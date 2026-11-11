package br.com.nexfleet.indicadores;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

/** Base dos testes de integração: um SQL Server 2022 compartilhado pela suíte. */
@SpringBootTest(properties = {
    "nexfleet.messaging.provider=nenhum",
    "spring.rabbitmq.password=nao-usado"})
public abstract class SqlServerIntegracao {
  @ServiceConnection
  protected static final MSSQLServerContainer SQL_SERVER = iniciar();

  private static MSSQLServerContainer iniciar() {
    var container = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
        .acceptLicense()
        .withInitScript("sqlserver/bootstrap-indicadores.sql");
    container.start();
    return container;
  }
}
