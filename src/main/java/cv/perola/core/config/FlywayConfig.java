package cv.perola.core.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations.split(","))
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlywayPostProcessor() {
        return beanFactory -> {
            for (String name : beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false)) {
                var bd = beanFactory.getBeanDefinition(name);
                String[] existing = bd.getDependsOn();
                if (existing == null || existing.length == 0) {
                    bd.setDependsOn("flyway");
                } else if (!Arrays.asList(existing).contains("flyway")) {
                    String[] updated = Arrays.copyOf(existing, existing.length + 1);
                    updated[existing.length] = "flyway";
                    bd.setDependsOn(updated);
                }
            }
        };
    }
}
