package nl.quintor.soqqer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@Modulithic(sharedModules = "common")
public class SoqqerApplication {

	static void main(String[] args) {
		SpringApplication.run(SoqqerApplication.class, args);
	}

}
