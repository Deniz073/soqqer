package nl.quintor.soqqer;

import nl.quintor.soqqer.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.core.ApplicationModules;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SoqqerApplicationTests {

	@Test
	@SuppressWarnings({"java:S2699", "EmptyMethod"})
	void contextLoads() { // only verifies that the application context can be loaded without exceptions
	}

	@Test
	void verifiesModularStructure() {
		var modules = ApplicationModules.of(SoqqerApplication.class);
		modules.verify();
	}

}
