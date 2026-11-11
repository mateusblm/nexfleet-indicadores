package br.com.nexfleet.indicadores.consultardashboard.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import br.com.nexfleet.indicadores.SqlServerIntegracao;
import br.com.nexfleet.indicadores.consultardashboard.application.ConsultarDashboard;
import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard;
import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard.ProximaManutencao;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DashboardSqlServerIT extends SqlServerIntegracao {
  private static final Clock HOJE =
      Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);

  @Autowired ProjetarAbastecimento abastecimentos;
  @Autowired ProjetarManutencao manutencoes;
  @Autowired LeituraDashboard leitura;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM indicadores.abastecimento_projecao");
    jdbc.update("DELETE FROM indicadores.manutencao_projecao");
  }

  @Test
  void dashboardVazioRetornaZeros() {
    var resultado = new ConsultarDashboard(leitura, HOJE).executar();

    assertEquals(0, resultado.gastoAbastecimentos().compareTo(BigDecimal.ZERO));
    assertEquals(0, resultado.gastoManutencoes().compareTo(BigDecimal.ZERO));
    assertEquals(0, resultado.quantidadeAbastecimentos());
    assertEquals(0, resultado.quantidadeManutencoes());
    assertEquals(List.of(), resultado.proximasManutencoes());
  }

  @Test
  void somaGastosERetornaAsCincoProximasManutencoesDaUltimaDeCadaVeiculo() {
    abastecer("250.50");
    abastecer("100.25");
    UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(),
        d = UUID.randomUUID(), e = UUID.randomUUID(), f = UUID.randomUUID(),
        g = UUID.randomUUID(), h = UUID.randomUUID();
    manter(a, "2026-09-01", "2026-11-10", "100.00");
    manter(a, "2026-10-01", "2027-03-01", "200.00");
    manter(b, "2026-10-15", "2026-11-06", "50.00");
    manter(c, "2026-10-01", "2026-11-05", "10.00");
    manter(d, "2026-10-02", null, "20.00");
    manter(e, "2026-10-03", "2026-12-15", "30.00");
    manter(f, "2026-10-04", "2027-01-20", "40.00");
    manter(g, "2026-10-05", "2027-02-10", "60.00");
    manter(h, "2026-10-06", "2027-04-01", "70.00");

    var resultado = new ConsultarDashboard(leitura, HOJE).executar();

    assertEquals(new BigDecimal("350.75"), resultado.gastoAbastecimentos());
    assertEquals(new BigDecimal("580.00"), resultado.gastoManutencoes());
    assertEquals(new BigDecimal("930.75"), resultado.gastoTotal());
    assertEquals(2, resultado.quantidadeAbastecimentos());
    assertEquals(9, resultado.quantidadeManutencoes());
    assertEquals(List.of(
            new ProximaManutencao(b, "PREVENTIVA", LocalDate.parse("2026-11-06")),
            new ProximaManutencao(e, "PREVENTIVA", LocalDate.parse("2026-12-15")),
            new ProximaManutencao(f, "PREVENTIVA", LocalDate.parse("2027-01-20")),
            new ProximaManutencao(g, "PREVENTIVA", LocalDate.parse("2027-02-10")),
            new ProximaManutencao(a, "PREVENTIVA", LocalDate.parse("2027-03-01"))),
        resultado.proximasManutencoes());
  }

  private void abastecer(String valor) {
    abastecimentos.executar(new ProjetarAbastecimento.Entrada(UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        Instant.parse("2026-11-01T10:00:00Z"), "DIESEL", new BigDecimal("30.00"),
        new BigDecimal(valor), 15_000L, Instant.parse("2026-11-01T10:00:01Z")));
  }

  private void manter(UUID veiculo, String realizadaEm, String proxima, String valor) {
    manutencoes.executar(new ProjetarManutencao.Entrada(UUID.randomUUID(), UUID.randomUUID(),
        veiculo, "PREVENTIVA", LocalDate.parse(realizadaEm), 10_000L, "Revisão",
        "Oficina Central", new BigDecimal(valor),
        proxima == null ? null : LocalDate.parse(proxima), Instant.parse("2026-11-01T12:00:00Z")));
  }
}
