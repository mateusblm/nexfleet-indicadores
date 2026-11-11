package br.com.nexfleet.indicadores.servicebus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import br.com.nexfleet.indicadores.servicebus.infrastructure.ServiceBusEventoListener;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

/** Os quatro eventos de edição e exclusão chegam pelo Service Bus aos casos de uso. */
class ServiceBusEventosVersionadosTest {
  private static final Instant RECEBIDO_EM = Instant.parse("2026-11-10T18:10:00Z");
  private final ProjetarAbastecimento abastecimentos = mock(ProjetarAbastecimento.class);
  private final ProjetarManutencao manutencoes = mock(ProjetarManutencao.class);
  private final ServiceBusEventoListener listener = new ServiceBusEventoListener(
      abastecimentos, manutencoes, new ObjectMapper(), Clock.fixed(RECEBIDO_EM, ZoneOffset.UTC));
  private final UUID eventoId = UUID.randomUUID();
  private final UUID agregadoId = UUID.randomUUID();
  private final UUID veiculoId = UUID.randomUUID();

  @Test
  void abastecimentoAtualizadoChegaComVersao() {
    when(abastecimentos.atualizar(any())).thenReturn(ProjetarAbastecimento.Resultado.PROJETADO);

    listener.consumir("AbastecimentoAtualizado.v1", """
        {"eventoId":"%s","abastecimentoId":"%s","veiculoId":"%s",\
        "motoristaId":"%s","realizadoEm":"2026-11-08T18:00:00Z",\
        "combustivel":"GASOLINA","litros":42.5,"valorTotal":260.1,"quilometragem":1510,\
        "versao":3,"atualizadoPor":"gestor@nexfleet.dev","correlationId":"%s"}
        """.formatted(eventoId, agregadoId, veiculoId, UUID.randomUUID(), UUID.randomUUID()),
        UUID.randomUUID().toString());

    var captor = ArgumentCaptor.forClass(ProjetarAbastecimento.Atualizacao.class);
    verify(abastecimentos).atualizar(captor.capture());
    var atualizacao = captor.getValue();
    assertEquals(eventoId, atualizacao.eventoId());
    assertEquals(agregadoId, atualizacao.abastecimentoId());
    assertEquals(new BigDecimal("260.1"), atualizacao.valorTotal());
    assertEquals(3, atualizacao.versao());
    assertEquals(RECEBIDO_EM, atualizacao.recebidoEm());
    verifyNoInteractions(manutencoes);
  }

  @Test
  void abastecimentoExcluidoChegaComVersaoEMomentoDaExclusao() {
    when(abastecimentos.excluir(any())).thenReturn(ProjetarAbastecimento.Resultado.EXCLUIDO);

    listener.consumir("AbastecimentoExcluido.v1", """
        {"eventoId":"%s","abastecimentoId":"%s","veiculoId":"%s","versao":4,\
        "excluidoPor":"gestor@nexfleet.dev","excluidoEm":"2026-11-10T18:00:00Z",\
        "correlationId":"%s"}
        """.formatted(eventoId, agregadoId, veiculoId, UUID.randomUUID()),
        UUID.randomUUID().toString());

    var captor = ArgumentCaptor.forClass(ProjetarAbastecimento.Exclusao.class);
    verify(abastecimentos).excluir(captor.capture());
    assertEquals(new ProjetarAbastecimento.Exclusao(eventoId, agregadoId, veiculoId, 4,
        Instant.parse("2026-11-10T18:00:00Z"), RECEBIDO_EM), captor.getValue());
    verifyNoInteractions(manutencoes);
  }

  @Test
  void manutencaoAtualizadaSemVersaoChegaSemVersao() {
    when(manutencoes.atualizar(any())).thenReturn(ProjetarManutencao.Resultado.PROJETADO);

    listener.consumir("ManutencaoAtualizada.v1", """
        {"eventoId":"%s","manutencaoId":"%s","veiculoId":"%s",\
        "tipo":"CORRETIVA","realizadaEm":"2026-11-08","quilometragem":1500,\
        "descricao":"Troca de pastilhas","oficina":"Oficina Norte","valor":480.0,\
        "proximaManutencao":"2027-05-08","atualizadoPor":"gestor@nexfleet.dev"}
        """.formatted(eventoId, agregadoId, veiculoId),
        UUID.randomUUID().toString());

    var captor = ArgumentCaptor.forClass(ProjetarManutencao.Atualizacao.class);
    verify(manutencoes).atualizar(captor.capture());
    var atualizacao = captor.getValue();
    assertEquals(eventoId, atualizacao.eventoId());
    assertEquals(agregadoId, atualizacao.manutencaoId());
    assertEquals("CORRETIVA", atualizacao.tipo());
    assertEquals(LocalDate.parse("2027-05-08"), atualizacao.proximaManutencao());
    assertNull(atualizacao.versao());
    assertEquals(RECEBIDO_EM, atualizacao.recebidoEm());
    verifyNoInteractions(abastecimentos);
  }

  @Test
  void manutencaoExcluidaChegaComVersaoEMomentoDaExclusao() {
    when(manutencoes.excluir(any())).thenReturn(ProjetarManutencao.Resultado.EXCLUIDO);

    listener.consumir("ManutencaoExcluida.v1", """
        {"eventoId":"%s","manutencaoId":"%s","veiculoId":"%s","versao":2,\
        "excluidoPor":"gestor@nexfleet.dev","excluidoEm":"2026-11-10T17:00:00Z",\
        "correlationId":"%s"}
        """.formatted(eventoId, agregadoId, veiculoId, UUID.randomUUID()),
        UUID.randomUUID().toString());

    var captor = ArgumentCaptor.forClass(ProjetarManutencao.Exclusao.class);
    verify(manutencoes).excluir(captor.capture());
    assertEquals(new ProjetarManutencao.Exclusao(eventoId, agregadoId, veiculoId, 2,
        Instant.parse("2026-11-10T17:00:00Z"), RECEBIDO_EM), captor.getValue());
    verifyNoInteractions(abastecimentos);
  }
}
