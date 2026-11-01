package com.basketapp.bcmserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "bcm.database.verification.enabled=false",
        "spring.flyway.enabled=false"
})
class BcmServerApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        assertNotNull(dataSource, "DataSource should be provided in application context");
    }
}
