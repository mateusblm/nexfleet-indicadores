package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import br.com.nexfleet.indicadores.SqlServerIntegracao;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao.Resultado;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ProjecaoManutencaoIdempotenteIT extends SqlServerIntegracao {
  @Autowired ProjetarManutencao projetar;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM indicadores.manutencao_projecao");
  }

  @Test
  void eventoDuplicadoTemEfeitoRegistradoUmaUnicaVez() {
    var entrada = entrada(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(Resultado.PROJETADO, projetar.executar(entrada));
    assertEquals(Resultado.DUPLICATA_IGNORADA, projetar.executar(entrada));
    assertEquals(1, linhas(entrada.eventoId()));
  }

  @Test
  void mesmaManutencaoComOutroEventoEhIgnorada() {
    var manutencao = UUID.randomUUID();

    assertEquals(Resultado.PROJETADO, projetar.executar(entrada(UUID.randomUUID(), manutencao)));
    assertEquals(Resultado.DUPLICATA_IGNORADA,
        projetar.executar(entrada(UUID.randomUUID(), manutencao)));
    assertEquals(1, jdbc.queryForObject(
        "SELECT COUNT(*) FROM indicadores.manutencao_projecao WHERE manutencao_id = ?",
        Integer.class, manutencao));
  }

  @RepeatedTest(5)
  void mesmoEventoEmDuasThreadsTemEfeitoRegistradoUmaUnicaVez() throws Exception {
    var entrada = entrada(UUID.randomUUID(), UUID.randomUUID());
    var largada = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      Callable<Resultado> tarefa = () -> {
        largada.await();
        return projetar.executar(entrada);
      };
      var futuros = List.of(executor.submit(tarefa), executor.submit(tarefa));
      largada.countDown();
      var resultados = List.of(
          futuros.get(0).get(30, TimeUnit.SECONDS), futuros.get(1).get(30, TimeUnit.SECONDS));

      assertEquals(1, resultados.stream().filter(Resultado.PROJETADO::equals).count());
      assertEquals(1, resultados.stream().filter(Resultado.DUPLICATA_IGNORADA::equals).count());
      assertEquals(1, linhas(entrada.eventoId()));
    } finally {
      executor.shutdownNow();
    }
  }

  private int linhas(UUID eventoId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM indicadores.manutencao_projecao WHERE evento_id = ?",
        Integer.class, eventoId);
  }

  private static ProjetarManutencao.Entrada entrada(UUID eventoId, UUID manutencaoId) {
    return new ProjetarManutencao.Entrada(eventoId, manutencaoId, UUID.randomUUID(),
        "PREVENTIVA", LocalDate.of(2026, 11, 1), 10_000L, "Troca de óleo", "Oficina Central",
        new BigDecimal("400.00"), LocalDate.of(2026, 12, 1),
        Instant.parse("2026-11-01T12:00:00Z"));
  }
}
