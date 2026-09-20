package com.knowledgeops;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.*;

@AnalyzeClasses(packages = "com.knowledgeops", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule controllers_do_not_access_repositories =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule future_modules_do_not_implement_phase_one =
      noClasses()
          .that()
          .resideInAnyPackage("..ticket..", "..knowledge..", "..ai..")
          .should()
          .haveSimpleNameEndingWith("Controller");
}
