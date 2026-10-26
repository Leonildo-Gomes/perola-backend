package cv.perola;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(PerolaApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
