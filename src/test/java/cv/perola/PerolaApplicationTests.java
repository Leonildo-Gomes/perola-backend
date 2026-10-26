package cv.perola;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@ActiveProfiles("dev")
class PerolaApplicationTests {

    @Autowired
    private Flyway flyway;

    @Test
    void contextLoads() {
        org.junit.jupiter.api.Assertions.assertNotNull(flyway, "Flyway bean should be initialized");
    }
}
