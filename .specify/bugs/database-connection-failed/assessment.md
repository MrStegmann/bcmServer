# Bug Assessment: Supabase Direct Database Connection Failure (08001 / UnknownHostException)

- **Slug**: database-connection-failed
- **Created**: 2026-11-01
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

```text
# Slug = database-connection-failed
## Mandatory Solution
The database connection MUST connect to supabase. LocalHost is NOT acceptable as a solution.
Focus only in the fix to connect to Supabase direct connection (defined in `.env`)

Error track:
SQL State  : 08001
Error Code : 0
Message    : El intento de conexión falló.

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: Failed to initialize dependency 'flywayInitializer' of LoadTimeWeaverAware bean 'entityManagerFactory': Error creating bean with name 'flywayInitializer' defined in class path resource [org/springframework/boot/autoconfigure/flyway/FlywayAutoConfiguration$FlywayConfiguration.class]: Unable to obtain connection from database: El intento de conexión falló.
Caused by: org.postgresql.util.PSQLException: El intento de conexión falló.
Caused by: java.net.UnknownHostException: db.ccthgmjmkdxhpqovbbvk.supabase.co
```

## Symptom

When launching `bcmServer` (e.g. via `.\mvnw.cmd spring-boot:run`), Spring Boot attempts to connect to the Supabase direct database connection host (`db.ccthgmjmkdxhpqovbbvk.supabase.co:5432`) configured in `.env`. Startup halts immediately with `BeanCreationException` during `flywayInitializer` and `entityManagerFactory` initialization because the PostgreSQL JDBC driver fails to establish a socket connection (`PSQLException` SQL State `08001`: `"El intento de conexión falló."`), caused by `java.net.UnknownHostException: db.ccthgmjmkdxhpqovbbvk.supabase.co` / network unreachability.

Per user requirements, local database (`localhost`) is explicitly rejected. The connection MUST connect to Supabase using the direct connection endpoint defined in `.env`.

## Reproduction

1. In `.env`, ensure the Supabase direct connection parameters are active:
   ```dotenv
   DB_HOST=db.ccthgmjmkdxhpqovbbvk.supabase.co
   DB_PORT=5432
   DB_NAME=postgres
   DB_USERNAME=postgres
   DB_PASSWORD=nU4goTgNHwsvTrvV
   DB_QUERY_PARAMS=?sslmode=require
   ```
2. Start the application on a host/network that lacks an active IPv6 internet gateway:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. Observe that HikariCP attempts to establish a connection pool to `jdbc:postgresql://db.ccthgmjmkdxhpqovbbvk.supabase.co:5432/postgres?sslmode=require`.
4. The connection fails during DNS name resolution and socket initialization:
   ```text
   Caused by: org.postgresql.util.PSQLException: El intento de conexión falló.
   ...
   Caused by: java.net.UnknownHostException: db.ccthgmjmkdxhpqovbbvk.supabase.co
   ```
   causing Flyway initialization to fail and aborting Spring Boot startup.

## Suspected Code Paths

- `.env:11-16` — Defines direct connection host `DB_HOST=db.ccthgmjmkdxhpqovbbvk.supabase.co`, `DB_PORT=5432`, `DB_USERNAME=postgres`, `DB_QUERY_PARAMS=?sslmode=require`.
- `src/main/resources/application.yml:7-18` — Configures HikariCP datasource with URL `jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:bcm_db}${DB_QUERY_PARAMS:}`.
- `src/main/resources/application-dev.yml:1-5` — Configures development profile datasource binding.
- `src/main/java/com/basketapp/bcmserver/infrastructure/config/DotenvEnvironmentPostProcessor.java:1-50` — Loads `.env` properties into Spring environment early during startup.

## Root Cause Hypothesis

Supabase direct connection hostnames (`db.<project-ref>.supabase.co`) publish only IPv6 (`AAAA`) DNS records (e.g. `db.ccthgmjmkdxhpqovbbvk.supabase.co` -> `2a05:d012:7a:e02:977e:ba49:7c3:2bef`) and do not publish standard IPv4 (`A`) records by default.

On the developer machine (`DESKTOP-1PL3O9D`), the primary network interface (`Ethernet 5`) is provisioned with IPv4 (`192.168.1.38/24`, gateway `192.168.1.1`) and only possesses link-local IPv6 (`fe80::`), without an IPv6 default gateway or public IPv6 unicast address. 

When the JVM calls Windows native `getaddrinfo` via `Inet6AddressImpl.lookupAllHostAddr`, the OS name resolution filters out `AAAA` records due to lack of a global IPv6 route, resulting in `java.net.UnknownHostException: db.ccthgmjmkdxhpqovbbvk.supabase.co`. Furthermore, direct socket connection to the literal IPv6 address (`2a05:d012:7a:e02:977e:ba49:7c3:2bef:5432`) fails with `java.net.SocketException: Network is unreachable`.

Confidence: high. (Verified via DNS query, PowerShell `Test-NetConnection`, and direct Java socket reproduction).

## Proposed Remediation

Per mandatory user instruction:
- **Localhost is NOT acceptable.**
- **The fix MUST establish connection to Supabase direct connection defined in `.env`.**

To enable direct connection to `db.ccthgmjmkdxhpqovbbvk.supabase.co:5432` from this environment:

**Preferred Remediation**:
1. **Enable IPv6 connectivity on the host environment**:
   - Activate IPv6 transit on the host machine or local router (e.g., enable IPv6 DHCP on the local network or use an IPv6 tunnel / Cloudflare WARP client to provide global IPv6 routing to the Windows OS).
   - Once global IPv6 routing is active, Windows and the JVM resolve `db.ccthgmjmkdxhpqovbbvk.supabase.co` and establish TCP connection to port `5432` seamlessly without changing `.env` credentials.
2. **Supabase Direct Connection IPv4 Add-on (Dashboard)**:
   - If the local network cannot support IPv6 natively, activate the Supabase **IPv4 Add-on** in the Supabase Project Dashboard (Project Settings > Add-ons > Dedicated IPv4).
   - This assigns a dedicated public IPv4 address and publishes an `A` record for `db.ccthgmjmkdxhpqovbbvk.supabase.co`, allowing direct connections over IPv4 without any change to `.env` or application code.
3. **Verify Supabase Project Status**:
   - Confirm in the Supabase dashboard that project `ccthgmjmkdxhpqovbbvk` is in an active (unpaused) state so that Postgres accepts incoming connections on port 5432.
4. **JVM Network Properties Configuration**:
   - If IPv6 is configured on the machine, ensure JVM options do not disable IPv6 (ensure `-Djava.net.preferIPv4Stack=false` and optionally `-Djava.net.preferIPv6Addresses=true` in `.mvn/jvm.config` or Maven execution args).

**Alternatives**:
- If native IPv6 routing or the IPv4 add-on is unavailable, a local IPv4-to-IPv6 forwarding tunnel (e.g. a lightweight proxy running on the host bridging IPv4 to the Supabase IPv6 target) can map direct connection requests to the Supabase direct endpoint.

**Files likely to change**:
- `.mvn/jvm.config` (if JVM IPv6 address preference properties need to be explicitly declared)
- `.env` (verify and keep direct connection properties intact)

**Tests to add or update**:
- Integration test or pre-flight connectivity check to validate that the configured `DB_HOST` is resolvable and reachable before Flyway migration runs.

## Risks & Considerations

- **ISP / Router IPv6 Support**: If the local ISP does not offer native IPv6, enabling IPv6 requires either a VPN/tunnel (such as Cloudflare WARP) or purchasing Supabase's IPv4 add-on.
- **Supabase Free Tier Inactivity**: Supabase pauses free-tier database instances after 7 days of inactivity. If paused, direct connection attempts will fail or time out until the project is resumed via the Supabase web dashboard.

## Open Questions

- Does the developer prefer enabling IPv6 on the host (e.g. via Cloudflare WARP / network settings) or purchasing the Supabase IPv4 Add-on on the Supabase dashboard?
