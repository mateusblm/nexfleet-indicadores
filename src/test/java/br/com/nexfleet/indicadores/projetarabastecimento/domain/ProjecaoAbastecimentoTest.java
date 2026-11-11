package br.com.nexfleet.indicadores.projetarabastecimento.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjecaoAbastecimentoTest {
  private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
  private static final UUID ID = UUID.randomUUID();

  @Test
  void versaoAusenteValeUmEMenorQueUmEhInvalida() {
    assertEquals(1, ProjecaoAbastecimento.versao(null));
    assertEquals(3, ProjecaoAbastecimento.versao(3));
    assertThrows(IllegalArgumentException.class, () -> ProjecaoAbastecimento.versao(0));
  }

  @Test
  void exclusaoSemDataUsaORecebimento() {
    var excluido = AGORA.minusSeconds(60);
    assertEquals(AGORA, ProjecaoAbastecimento.momentoExclusao(null, AGORA));
    assertEquals(excluido, ProjecaoAbastecimento.momentoExclusao(excluido, AGORA));
  }

  @Test
  void dadosPrecisamEstarCompletosEPositivos() {
    assertDoesNotThrow(() -> validar(BigDecimal.ONE, 1L));
    assertEquals(ProjecaoAbastecimento.INCOMPLETO,
        assertThrows(IllegalArgumentException.class, () -> validar(BigDecimal.ONE, null))
            .getMessage());
    assertEquals("Valores do abastecimento devem ser positivos.",
        assertThrows(IllegalArgumentException.class, () -> validar(BigDecimal.ZERO, 1L))
            .getMessage());
  }

  @Test
  void exclusaoPrecisaIdentificarOAbastecimento() {
    assertThrows(IllegalArgumentException.class,
        () -> ProjecaoAbastecimento.validarExclusao(ID, null, ID, AGORA));
    assertDoesNotThrow(() -> ProjecaoAbastecimento.validarExclusao(ID, ID, ID, AGORA));
  }

  private static void validar(BigDecimal litros, Long km) {
    ProjecaoAbastecimento.validarDados(ID, ID, ID, ID, AGORA, "DIESEL", litros, BigDecimal.TEN,
        km, AGORA);
  }
}
