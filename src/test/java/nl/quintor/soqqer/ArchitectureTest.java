package nl.quintor.soqqer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("nl.quintor.soqqer");

    @Test
    void springFieldDependencyInjectionShouldNotBeUsed() {
        ArchRule rule = ArchRuleDefinition.noFields()
                .should().beAnnotatedWith(Autowired.class);
        rule.check(classes);
    }
}
