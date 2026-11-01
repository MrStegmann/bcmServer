# BCM (Basket Club Manager) - Data Models & Schema Contracts

Este documento define el estado de la base de datos PostgreSQL existente. Cualquier entidad de dominio o entidad JPA generada en el proyecto Spring Boot debe mapear directamente contra estas definiciones.

## 1. Relaciones Principales (Entity Graph)
- **Team** `1 : N` **Player** (Un equipo tiene muchos jugadores)
- **Team** `1 : N` **Game** (Un equipo juega muchos partidos)
- **Game** `1 : N` **PlayerStats** (Un partido tiene muchas estadísticas de jugadores)
- **Player** `1 : N` **PlayerStats** (Un jugador tiene muchas estadísticas por partido)
- **Game** `1 : 1` **MatchCache** (Un partido en vivo tiene un estado en caché)

---

## 2. Entidades de Base de Datos

### `users`
Tabla para la gestión de acceso al sistema.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `username` | TEXT | NOT NULL, UNIQUE |
| `password` | TEXT | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### `teams`
Entidad principal que agrupa jugadores y partidos.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `name` | TEXT | NOT NULL |
| `category` | TEXT | NULLABLE |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### `players`
Jugadores pertenecientes a un equipo.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `team_id` | INTEGER | NOT NULL, FK `teams(id)`, ON DELETE CASCADE |
| `first_name` | TEXT | NOT NULL |
| `last_name` | TEXT | NOT NULL |
| `number` | TEXT | NOT NULL |
| `jersey_name` | TEXT | NULLABLE |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### `games`
Registro de partidos disputados o programados.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `team_id` | INTEGER | NOT NULL, FK `teams(id)`, ON DELETE CASCADE |
| `opponent` | TEXT | NOT NULL |
| `round` | TEXT | NOT NULL, DEFAULT '1' |
| `date` | TEXT | NOT NULL (ISO-8601 o custom string format) |
| `status` | TEXT | NOT NULL, DEFAULT 'pending' (pending, live, finished) |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| **Scores (Own & Opponent)** | | |
| `result_c1` a `result_c4` | INTEGER | DEFAULT 0 (Cuartos equipo local) |
| `result_extra` | INTEGER | DEFAULT 0 (Prórroga equipo local) |
| `result_c1_opponent` a `..._c4_opponent`| INTEGER | DEFAULT 0 (Cuartos rival) |
| `result_extra_opponent` | INTEGER | DEFAULT 0 (Prórroga rival) |
| **Fouls (Own & Opponent)** | | |
| `fouls_c1` a `fouls_c4` | INTEGER | DEFAULT 0 (Faltas equipo local por cuarto) |
| `fouls_extra` | INTEGER | DEFAULT 0 (Faltas equipo local prórroga) |
| `fouls_c1_opponent` a `..._c4_opponent` | INTEGER | DEFAULT 0 (Faltas rival por cuarto) |
| `fouls_extra_opponent`| INTEGER | DEFAULT 0 (Faltas rival prórroga) |

### `player_stats`
Estadísticas granulares de un jugador en un partido específico.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `game_id` | INTEGER | NOT NULL, FK `games(id)`, ON DELETE CASCADE |
| `player_id` | INTEGER | NOT NULL, FK `players(id)`, ON DELETE CASCADE |
| `minutes` / `seconds` | INTEGER | DEFAULT 0 (Tiempo de juego) |
| `t1a` / `t1i` | INTEGER | DEFAULT 0 (Tiros libres: intentados / anotados) |
| `t2a` / `t2i` | INTEGER | DEFAULT 0 (Tiros 2pt: intentados / anotados) |
| `t3a` / `t3i` | INTEGER | DEFAULT 0 (Tiros 3pt: intentados / anotados) |
| `oreb` / `dreb` | INTEGER | DEFAULT 0 (Rebotes of. / def.) |
| `asis` | INTEGER | DEFAULT 0 (Asistencias) |
| `rec` | INTEGER | DEFAULT 0 (Robos / Recuperaciones) |
| `per` | INTEGER | DEFAULT 0 (Pérdidas) |
| `falt` | INTEGER | DEFAULT 0 (Faltas personales) |
| `valuation` | INTEGER | DEFAULT 0 (Valoración PIR) |


## Not Include to Server: App Local Storage
### `notifications`
Registro de notificaciones push o in-app enviadas a los clientes.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `external_id` | TEXT | NULLABLE (Ej: ID de Firebase/OneSignal) |
| `title` / `body` | TEXT | NOT NULL |
| `data_json` | JSONB | NULLABLE (Payload extra de la notificación) |
| `received_at` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `is_read` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `suppressed_reason` | TEXT | NOT NULL, DEFAULT 'none' |

### `notification_settings`
Tabla Singleton (solo debería tener la fila `id=1`) para preferencias globales del dispositivo/usuario.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY, DEFAULT 1 |
| `mute_in_app` | BOOLEAN | NOT NULL, DEFAULT TRUE |
| `mute_in_match` | BOOLEAN | NOT NULL, DEFAULT TRUE |
| `sound_enabled` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### `match_cache`
Estado efímero/rápido de un partido en vivo, cacheado en base de datos.
| Campo | Tipo PostgreSQL | Restricciones / Notas |
| :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY |
| `match_id` | INTEGER | NOT NULL, UNIQUE, FK `games(id)`, ON DELETE CASCADE |
| `state_json` | JSONB | NOT NULL (Estado completo del partido serializado) |
| `version` | INTEGER | NOT NULL, DEFAULT 1 (Para control de concurrencia optimista) |
| `last_updated` | TIMESTAMPTZ | NOT NULL, DEFAULT CURRENT_TIMESTAMP |