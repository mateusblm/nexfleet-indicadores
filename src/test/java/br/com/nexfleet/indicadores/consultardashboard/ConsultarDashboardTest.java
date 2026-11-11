package br.com.nexfleet.indicadores.consultardashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.nexfleet.indicadores.consultardashboard.application.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarDashboardTest {
  @Test
  void consolidaGastosValidosEProximasManutencoes() {
    var leitura = mock(LeituraDashboard.class);
    var hoje = LocalDate.of(2026, 11, 6);
    var proxima =
        new LeituraDashboard.ProximaManutencao(
            UUID.randomUUID(), "PREVENTIVA", LocalDate.of(2026, 12, 6));
    when(leitura.consultarTotais())
        .thenReturn(new LeituraDashboard.Totais(new BigDecimal("250.50"), new BigDecimal("400.00"), 2, 1));
    when(leitura.consultarProximasManutencoes(hoje, 5)).thenReturn(List.of(proxima));

    var resultado =
        new ConsultarDashboard(
                leitura, Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC))
            .executar();

    assertEquals(new BigDecimal("650.50"), resultado.gastoTotal());
    assertEquals(2, resultado.quantidadeAbastecimentos());
    assertEquals(List.of(proxima), resultado.proximasManutencoes());
  }
}
