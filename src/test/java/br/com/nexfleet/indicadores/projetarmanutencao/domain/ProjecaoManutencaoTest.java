package br.com.nexfleet.indicadores.projetarmanutencao.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjecaoManutencaoTest {
  private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
  private static final LocalDate DIA = LocalDate.parse("2026-11-01");
  private static final UUID ID = UUID.randomUUID();

  @Test
  void versaoAusenteValeUmEMenorQueUmEhInvalida() {
    assertEquals(1, ProjecaoManutencao.versao(null));
    assertEquals(2, ProjecaoManutencao.versao(2));
    assertThrows(IllegalArgumentException.class, () -> ProjecaoManutencao.versao(-1));
  }

  @Test
  void exclusaoSemDataUsaORecebimento() {
    assertEquals(AGORA, ProjecaoManutencao.momentoExclusao(null, AGORA));
  }

  @Test
  void dadosPrecisamEstarCompletosEProximaPosterior() {
    assertDoesNotThrow(() -> validar(0L, DIA.plusDays(1)));
    assertEquals(ProjecaoManutencao.INCOMPLETO,
        assertThrows(IllegalArgumentException.class, () -> validar(-1L, null)).getMessage());
    assertEquals("Próxima manutenção inválida.",
        assertThrows(IllegalArgumentException.class, () -> validar(0L, DIA)).getMessage());
  }

  @Test
  void exclusaoPrecisaIdentificarAManutencao() {
    assertThrows(IllegalArgumentException.class,
        () -> ProjecaoManutencao.validarExclusao(ID, ID, null, AGORA));
  }

  private static void validar(Long km, LocalDate proxima) {
    ProjecaoManutencao.validarDados(ID, ID, ID, "REVISAO", DIA, km, "Revisão", "Oficina",
        BigDecimal.ONE, proxima, AGORA);
  }
}
