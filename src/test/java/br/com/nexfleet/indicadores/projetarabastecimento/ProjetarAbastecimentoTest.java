package br.com.nexfleet.indicadores.projetarabastecimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjecoesAbastecimento;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjetarAbastecimentoTest {
  private final ProjecoesAbastecimento projecoes = mock(ProjecoesAbastecimento.class);
  private final ProjetarAbastecimento caso = new ProjetarAbastecimento(projecoes);

  @Test
  void criaProjecaoNaPrimeiraEntrega() {
    var entrada = entrada();
    when(registrar(entrada)).thenReturn(true);

    assertEquals(ProjetarAbastecimento.Resultado.PROJETADO, caso.executar(entrada));
  }

  @Test
  void ignoraEntregaDuplicada() {
    var entrada = entrada();
    when(registrar(entrada)).thenReturn(false);

    assertEquals(
        ProjetarAbastecimento.Resultado.DUPLICATA_IGNORADA,
        caso.executar(entrada));
  }

  private boolean registrar(ProjetarAbastecimento.Entrada entrada) {
    return projecoes.registrarSeAusente(
        entrada.eventoId(),
        entrada.abastecimentoId(),
        entrada.veiculoId(),
        entrada.motoristaId(),
        entrada.realizadoEm(),
        entrada.combustivel(),
        entrada.litros(),
        entrada.valorTotal(),
        entrada.quilometragem(),
        entrada.recebidoEm());
  }

  private ProjetarAbastecimento.Entrada entrada() {
    return new ProjetarAbastecimento.Entrada(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-11-05T15:00:00Z"),
        "DIESEL",
        new BigDecimal("40.00"),
        new BigDecimal("250.00"),
        1500L,
        Instant.parse("2026-11-05T15:00:01Z"));
  }
}
