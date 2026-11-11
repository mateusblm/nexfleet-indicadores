package br.com.nexfleet.indicadores.servicebus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import br.com.nexfleet.indicadores.servicebus.infrastructure.ServiceBusEventoListener;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class ServiceBusEventoListenerTest {
  private static final Instant RECEBIDO_EM = Instant.parse("2026-11-08T18:10:00Z");
  private final ProjetarAbastecimento abastecimentos = mock(ProjetarAbastecimento.class);
  private final ProjetarManutencao manutencoes = mock(ProjetarManutencao.class);
  private final ServiceBusEventoListener listener = new ServiceBusEventoListener(
      abastecimentos,
      manutencoes,
      new ObjectMapper(),
      Clock.fixed(RECEBIDO_EM, ZoneOffset.UTC));

  @Test
  void encaminhaAbastecimentoParaCasoDeUso() {
    UUID eventoId = UUID.randomUUID();
    when(abastecimentos.executar(any())).thenReturn(ProjetarAbastecimento.Resultado.PROJETADO);

    listener.consumir(
        "AbastecimentoRegistrado.v1",
        """
        {"eventoId":"%s","abastecimentoId":"%s","veiculoId":"%s",\
        "motoristaId":"%s","realizadoEm":"2026-11-08T18:00:00Z",\
        "combustivel":"DIESEL","litros":40.0,"valorTotal":250.0,"quilometragem":1500}
        """.formatted(eventoId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
        UUID.randomUUID().toString());

    var captor = ArgumentCaptor.forClass(ProjetarAbastecimento.Entrada.class);
    verify(abastecimentos).executar(captor.capture());
    assertEquals(eventoId, captor.getValue().eventoId());
    assertEquals(RECEBIDO_EM, captor.getValue().recebidoEm());
    verifyNoInteractions(manutencoes);
  }

  @Test
  void encaminhaManutencaoParaCasoDeUso() {
    UUID eventoId = UUID.randomUUID();
    when(manutencoes.executar(any())).thenReturn(ProjetarManutencao.Resultado.PROJETADO);

    listener.consumir(
        "ManutencaoRegistrada.v1",
        """
        {"eventoId":"%s","manutencaoId":"%s","veiculoId":"%s",\
        "tipo":"PREVENTIVA","realizadaEm":"2026-11-08","quilometragem":1500,\
        "descricao":"Troca de oleo","oficina":"Oficina Central","valor":450.0,\
        "proximaManutencao":"2027-05-08"}
        """.formatted(eventoId, UUID.randomUUID(), UUID.randomUUID()),
        UUID.randomUUID().toString());

    var captor = ArgumentCaptor.forClass(ProjetarManutencao.Entrada.class);
    verify(manutencoes).executar(captor.capture());
    assertEquals(eventoId, captor.getValue().eventoId());
    assertEquals(RECEBIDO_EM, captor.getValue().recebidoEm());
    verifyNoInteractions(abastecimentos);
  }

  @Test
  void rejeitaTipoNaoContratadoSemExecutarCasosDeUso() {
    assertThrows(
        IllegalArgumentException.class,
        () -> listener.consumir("EventoDesconhecido.v1", "{}", UUID.randomUUID().toString()));

    verifyNoInteractions(abastecimentos, manutencoes);
  }
}
