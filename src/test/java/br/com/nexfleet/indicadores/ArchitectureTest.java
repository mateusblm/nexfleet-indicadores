package br.com.nexfleet.indicadores;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("br.com.nexfleet.indicadores");

  @Test
  void naoImportaInternosDeOutrosServicos() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "br.com.nexfleet.identidade..",
            "br.com.nexfleet.frota..",
            "br.com.nexfleet.operacoes..",
            "br.com.nexfleet.bff..",
            "br.com.nexfleet.gateway..")
        .check(classes);
  }

  @Test
  void aplicacaoNaoDependeDeFrameworkOuAdaptadores() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "..infrastructure..", "..api..")
        .check(classes);
  }

  @Test
  void apiNaoAcessaPersistenciaDiretamente() {
    noClasses()
        .that()
        .resideInAPackage("..api..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infrastructure..", "org.springframework.jdbc..", "jakarta.persistence..")
        .check(classes);
  }

  @Test
  void persistenciaFicaSoEmInfrastructure() {
    noClasses()
        .that()
        .resideOutsideOfPackage("..infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.jdbc..",
            "org.springframework.dao..",
            "jakarta.persistence..",
            "java.sql..",
            "javax.sql..")
        .check(classes);
  }

  @Test
  void codigoFicaNasCamadasDasFatiasOuEmPacotesTecnicosConhecidos() {
    // Um pacote de persistência solto na raiz (fora de infrastructure/) quebra esta regra.
    classes()
        .should()
        .resideInAnyPackage(
            "..api..",
            "..application..",
            "..domain..",
            "..infrastructure..",
            "br.com.nexfleet.indicadores.config..",
            "br.com.nexfleet.indicadores.observability..",
            "br.com.nexfleet.indicadores")
        .check(classes);
  }

  @Test
  void dominioNaoDependeDeOutrasCamadasNemDeFramework() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..infrastructure..",
            "..api..",
            "..config..",
            "org.springframework..",
            "jakarta..",
            "javax.sql..",
            "java.sql..",
            "com.fasterxml.jackson..",
            "tools.jackson..")
        .check(classes);
  }
}
