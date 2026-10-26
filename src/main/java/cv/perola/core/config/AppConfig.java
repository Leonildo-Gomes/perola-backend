package cv.perola.core.config;

import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;
import jakarta.annotation.PostConstruct;

@Configuration
public class AppConfig {

    public static final String CAPE_VERDE_TIMEZONE = "Atlantic/Cape_Verde";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(CAPE_VERDE_TIMEZONE));
    }
}
