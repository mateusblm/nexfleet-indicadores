package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

/** A fila de manutenções do RabbitMQ distingue os eventos pelo cabeçalho eventoTipo. */
class RabbitManutencaoListenerTest {
  private static final Instant RECEBIDO_EM = Instant.parse("2026-11-10T18:10:00Z");
  private final ProjetarManutencao casoDeUso = mock(ProjetarManutencao.class);
  private final RabbitManutencaoListener listener = new RabbitManutencaoListener(
      casoDeUso, new ObjectMapper(), Clock.fixed(RECEBIDO_EM, ZoneOffset.UTC));
  private final UUID eventoId = UUID.randomUUID();
  private final UUID manutencaoId = UUID.randomUUID();
  private final UUID veiculoId = UUID.randomUUID();

  @Test
  void manutencaoRegistradaContinuaSendoRegistro() {
    when(casoDeUso.executar(any())).thenReturn(ProjetarManutencao.Resultado.PROJETADO);

    listener.consumir(mensagem("ManutencaoRegistrada", corpoCompleto("")));

    var captor = ArgumentCaptor.forClass(ProjetarManutencao.Entrada.class);
    verify(casoDeUso).executar(captor.capture());
    assertEquals(eventoId, captor.getValue().eventoId());
  }

  @Test
  void manutencaoAtualizadaVaiParaAtualizacao() {
    when(casoDeUso.atualizar(any())).thenReturn(ProjetarManutencao.Resultado.PROJETADO);

    listener.consumir(mensagem("ManutencaoAtualizada",
        corpoCompleto(",\"versao\":5,\"atualizadoPor\":\"gestor@nexfleet.dev\"")));

    var captor = ArgumentCaptor.forClass(ProjetarManutencao.Atualizacao.class);
    verify(casoDeUso).atualizar(captor.capture());
    assertEquals(eventoId, captor.getValue().eventoId());
    assertEquals(5, captor.getValue().versao());
    assertEquals(RECEBIDO_EM, captor.getValue().recebidoEm());
  }

  @Test
  void manutencaoExcluidaVaiParaExclusao() {
    when(casoDeUso.excluir(any())).thenReturn(ProjetarManutencao.Resultado.EXCLUIDO);

    listener.consumir(mensagem("ManutencaoExcluida", """
        {"eventoId":"%s","manutencaoId":"%s","veiculoId":"%s","versao":2,\
        "excluidoPor":"gestor@nexfleet.dev","excluidoEm":"2026-11-10T17:00:00Z"}
        """.formatted(eventoId, manutencaoId, veiculoId)));

    var captor = ArgumentCaptor.forClass(ProjetarManutencao.Exclusao.class);
    verify(casoDeUso).excluir(captor.capture());
    assertEquals(new ProjetarManutencao.Exclusao(eventoId, manutencaoId, veiculoId, 2,
        Instant.parse("2026-11-10T17:00:00Z"), RECEBIDO_EM), captor.getValue());
  }

  @Test
  void tipoDesconhecidoEhRejeitadoSemExecutarCasoDeUso() {
    assertThrows(IllegalArgumentException.class,
        () -> listener.consumir(mensagem("AbastecimentoExcluido", corpoCompleto(""))));

    verifyNoInteractions(casoDeUso);
  }

  private String corpoCompleto(String extras) {
    return """
        {"eventoId":"%s","manutencaoId":"%s","veiculoId":"%s",\
        "tipo":"PREVENTIVA","realizadaEm":"2026-11-08","quilometragem":1500,\
        "descricao":"Troca de oleo","oficina":"Oficina Central","valor":450.0,\
        "proximaManutencao":"2027-05-08"%s}
        """.formatted(eventoId, manutencaoId, veiculoId, extras);
  }

  private static Message mensagem(String tipo, String corpo) {
    return MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json")
        .setHeader("eventoTipo", tipo)
        .build();
  }
}
