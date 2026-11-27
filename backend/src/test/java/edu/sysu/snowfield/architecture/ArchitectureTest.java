package edu.sysu.snowfield.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 架构边界测试：把 {@code docs/ARCHITECTURE.md} §1.2 的依赖规则（R1~R13 中的可表达子集）
 * 下沉为构建期自动检查，违规即失败，不依赖人工 Review 发现。
 *
 * <p>S1 阶段仅有 {@code common} 模块，业务模块的四层规则会在各业务模块创建后自动生效。</p>
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("edu.sysu.snowfield");

    // ---- 通用编码规则（立即生效） ----

    @Test
    void noClassesShouldAccessStandardStreams() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(CLASSES);
    }

    @Test
    void noClassesShouldThrowGenericExceptions() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(CLASSES);
    }

    @Test
    void noClassesShouldUseJavaUtilLogging() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(CLASSES);
    }

    // ---- 模块内分层规则（R5/R7/R8/R9） ----

    @Test
    void apiMustNotAccessPersistence() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void domainMustNotAccessInfrastructure() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void domainMustNotAccessApi() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void domainMustNotDependOnFrameworks() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "org.mybatis..", "jakarta.servlet..", "java.sql..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    // ---- 模块间依赖规则（消除循环） ----

    @Test
    void modulesMustBeFreeOfCycles() {
        slices().matching("edu.sysu.snowfield.(*)..")
                .should().beFreeOfCycles()
                .check(CLASSES);
    }
}
