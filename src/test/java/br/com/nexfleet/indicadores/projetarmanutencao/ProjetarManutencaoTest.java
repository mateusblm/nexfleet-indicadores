package br.com.nexfleet.indicadores.projetarmanutencao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.nexfleet.indicadores.projetarmanutencao.application.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjetarManutencaoTest {
  final ProjecoesManutencao projecoes = mock(ProjecoesManutencao.class);
  final ProjetarManutencao caso = new ProjetarManutencao(projecoes);

  @Test
  void criaProjecaoEIgnoraReentrega() {
    var entrada = entrada();
    when(registrar(entrada)).thenReturn(true, false);
    assertEquals(ProjetarManutencao.Resultado.PROJETADO, caso.executar(entrada));
    assertEquals(ProjetarManutencao.Resultado.DUPLICATA_IGNORADA, caso.executar(entrada));
  }

  @Test
  void recusaProximaDataInvalida() {
    var base = entrada();
    var invalida = new ProjetarManutencao.Entrada(base.eventoId(), base.manutencaoId(),
        base.veiculoId(), base.tipo(), base.realizadaEm(), base.quilometragem(),
        base.descricao(), base.oficina(), base.valor(), base.realizadaEm(), base.recebidoEm());
    assertThrows(IllegalArgumentException.class, () -> caso.executar(invalida));
    verifyNoInteractions(projecoes);
  }

  private boolean registrar(ProjetarManutencao.Entrada e) {
    return projecoes.registrarSeAusente(e.eventoId(), e.manutencaoId(), e.veiculoId(), e.tipo(),
        e.realizadaEm(), e.quilometragem(), e.descricao(), e.oficina(), e.valor(),
        e.proximaManutencao(), e.recebidoEm());
  }

  private ProjetarManutencao.Entrada entrada() {
    return new ProjetarManutencao.Entrada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "PREVENTIVA", LocalDate.of(2026, 11, 6), 15000L, "Revisão", "Central",
        new BigDecimal("450.00"), LocalDate.of(2026, 12, 6),
        Instant.parse("2026-11-06T12:00:00Z"));
  }
}
