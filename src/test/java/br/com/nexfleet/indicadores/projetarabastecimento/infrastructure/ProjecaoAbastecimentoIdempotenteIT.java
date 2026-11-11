package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import br.com.nexfleet.indicadores.SqlServerIntegracao;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento.Resultado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ProjecaoAbastecimentoIdempotenteIT extends SqlServerIntegracao {
  @Autowired ProjetarAbastecimento projetar;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM indicadores.abastecimento_projecao");
  }

  @Test
  void eventoDuplicadoTemEfeitoRegistradoUmaUnicaVez() {
    var entrada = entrada(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(Resultado.PROJETADO, projetar.executar(entrada));
    assertEquals(Resultado.DUPLICATA_IGNORADA, projetar.executar(entrada));
    assertEquals(1, linhas(entrada.eventoId()));
  }

  @Test
  void mesmoAbastecimentoComOutroEventoEhIgnorado() {
    var abastecimento = UUID.randomUUID();

    assertEquals(Resultado.PROJETADO, projetar.executar(entrada(UUID.randomUUID(), abastecimento)));
    assertEquals(Resultado.DUPLICATA_IGNORADA,
        projetar.executar(entrada(UUID.randomUUID(), abastecimento)));
    assertEquals(1, jdbc.queryForObject(
        "SELECT COUNT(*) FROM indicadores.abastecimento_projecao WHERE abastecimento_id = ?",
        Integer.class, abastecimento));
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
        "SELECT COUNT(*) FROM indicadores.abastecimento_projecao WHERE evento_id = ?",
        Integer.class, eventoId);
  }

  private static ProjetarAbastecimento.Entrada entrada(UUID eventoId, UUID abastecimentoId) {
    return new ProjetarAbastecimento.Entrada(eventoId, abastecimentoId, UUID.randomUUID(),
        UUID.randomUUID(), Instant.parse("2026-11-05T10:00:00Z"), "DIESEL",
        new BigDecimal("40.00"), new BigDecimal("250.50"), 12_000L,
        Instant.parse("2026-11-05T10:00:05Z"));
  }
}
