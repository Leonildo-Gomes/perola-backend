package cv.perola.core.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.TimeZone;

@Configuration
public class AppConfig {

    public static final String CAPE_VERDE_TIMEZONE = "Atlantic/Cape_Verde";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(CAPE_VERDE_TIMEZONE));
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
