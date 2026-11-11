package br.com.nexfleet.indicadores;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlserver://127.0.0.1:1;databaseName=nexfleet;encrypt=false;loginTimeout=2",
    "spring.datasource.username=indicadores",
    "spring.datasource.password=indisponivel",
    "spring.flyway.enabled=false",
    "nexfleet.messaging.provider=nenhum",
    "spring.rabbitmq.password=nao-usado"})
@AutoConfigureMockMvc
class ProntidaoSemBancoTest {
  @Autowired MockMvc http;

  @Test
  void readinessFicaDownComSqlServerIndisponivel() throws Exception {
    http.perform(get("/actuator/health/readiness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @Test
  void infoDoServicoResponde503ComSqlServerIndisponivel() throws Exception {
    http.perform(get("/api/v1/indicadores/info"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }
}
