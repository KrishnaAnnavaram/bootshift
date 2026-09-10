package com.bootshift.tests.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the ports-and-adapters boundaries (spec section 5).
 *
 * <p>These rules exist because the layering is load-bearing, not decorative: if the core domain can
 * reach a concrete tool, the harness stops being portable and the OSS boundary stops being
 * checkable.
 */
class ArchitectureTest {

    private static JavaClasses harness;

    @BeforeAll
    static void importClasses() {
        harness = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bootshift");
    }

    @Test
    @DisplayName("core must not depend on ports, adapters, stages or the CLI")
    void coreIsIndependent() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bootshift.ports..", "com.bootshift.adapters..",
                        "com.bootshift.stages..", "com.bootshift.cli..")
                .because("the core domain is the innermost layer and must stay portable");
        rule.check(harness);
    }

    @Test
    @DisplayName("core must not import concrete external tooling")
    void coreImportsNoTooling() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.eclipse.jgit..", "com.github.javaparser..",
                        "org.apache.maven..", "picocli..")
                .because("external technologies belong behind ports, never inside the domain (R8)");
        rule.check(harness);
    }

    @Test
    @DisplayName("ports must not depend on adapters, stages or the CLI")
    void portsAreInterfacesOverCore() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift.ports..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bootshift.adapters..", "com.bootshift.stages..",
                        "com.bootshift.cli..")
                .because("a port that knows its adapter is not a port");
        rule.check(harness);
    }

    @Test
    @DisplayName("adapters must not depend on stages or the CLI")
    void adaptersDoNotDependOnStages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift.adapters..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bootshift.stages..", "com.bootshift.cli..")
                .because("adapters implement ports and know nothing about pipeline sequencing");
        rule.check(harness);
    }

    @Test
    @DisplayName("the CLI must contain no migration semantics")
    void cliDelegates() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.bootshift.cli..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("com.bootshift..", "picocli..", "java..", "com.fasterxml..",
                        "org.slf4j..")
                .because("the CLI parses options and delegates; deleting it must not delete any "
                        + "stage logic (R24)");
        rule.check(harness);
    }

    @Test
    @DisplayName("only the mutation gateway may write application source")
    void onlyGatewayWritesSource() {
        // Transformation and repair produce proposals. Any filesystem write from those packages would
        // be a bypass of the single authorized writer (R13).
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.bootshift.stages.stage12..",
                        "com.bootshift.stages.stage13..", "com.bootshift.adapters.transform..")
                .should().callMethodWhere(new DescribedPredicate<JavaCall<?>>(
                        "a filesystem write, delete, move or copy") {
                    @Override
                    public boolean test(JavaCall<?> call) {
                        if (!call.getTargetOwner().getName().equals("java.nio.file.Files")) {
                            return false;
                        }
                        String method = call.getTarget().getName();
                        return method.startsWith("write") || method.startsWith("delete")
                                || method.equals("move") || method.equals("copy")
                                || method.equals("createFile");
                    }
                })
                .because("all repository mutations must flow through the FileMutationGateway (R13)");
        rule.check(harness);
    }

    @Test
    @DisplayName("no component may load a source-available Spring recipe estate")
    void noForbiddenRecipeEstate() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.openrewrite.java.spring..", "io.moderne..")
                .because("source-available Spring recipe packs are forbidden runtime dependencies "
                        + "under the strict-OSS policy (R8, R9)");
        rule.check(harness);
    }

    @Test
    @DisplayName("only the composition root may construct the run state adapter")
    void stagesUseContextForSharedPorts() {
        // StageContext and RunFactory are the composition root: wiring a concrete adapter is their
        // job. Any other stage reaching for the state adapter directly would defeat the port.
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift.stages..")
                .and().haveSimpleNameNotEndingWith("StageContext")
                .and().haveSimpleNameNotEndingWith("RunFactory")
                .should().dependOnClassesThat()
                .haveNameMatching("com\\.bootshift\\.nexus\\.adapters\\.state\\..*")
                .because("run state persistence is reached through StageContext so the backing store "
                        + "can be swapped without touching a stage");
        rule.check(harness);
    }

    @Test
    @DisplayName("the harness must not depend on the application under analysis")
    void noApplicationDependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bootshift..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.aura.vihanga..", "org.springframework..")
                .because("./src is input data; the harness core must not couple to the Spring Boot "
                        + "version being migrated");
        rule.check(harness);
    }
}
