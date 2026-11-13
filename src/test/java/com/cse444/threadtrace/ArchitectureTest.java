package com.cse444.threadtrace;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.cse444.threadtrace", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..infrastructure..", "..web..");

    @ArchTest
    static final ArchRule passport_should_only_read_via_application_ports =
            noClasses()
                    .that().resideInAPackage("..passport..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..lineage.domain..",
                            "..lineage.infrastructure.."
                    );
}
