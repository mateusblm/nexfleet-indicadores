package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
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

/** A fila de abastecimentos do RabbitMQ distingue os eventos pelo cabeçalho eventoTipo. */
class RabbitAbastecimentoListenerTest {
  private static final Instant RECEBIDO_EM = Instant.parse("2026-11-10T18:10:00Z");
  private final ProjetarAbastecimento casoDeUso = mock(ProjetarAbastecimento.class);
  private final RabbitAbastecimentoListener listener = new RabbitAbastecimentoListener(
      casoDeUso, new ObjectMapper(), Clock.fixed(RECEBIDO_EM, ZoneOffset.UTC));
  private final UUID eventoId = UUID.randomUUID();
  private final UUID abastecimentoId = UUID.randomUUID();
  private final UUID veiculoId = UUID.randomUUID();

  @Test
  void mensagemSemTipoContinuaSendoRegistro() {
    when(casoDeUso.executar(any())).thenReturn(ProjetarAbastecimento.Resultado.PROJETADO);

    listener.consumir(mensagem(null, registro()));

    var captor = ArgumentCaptor.forClass(ProjetarAbastecimento.Entrada.class);
    verify(casoDeUso).executar(captor.capture());
    assertEquals(eventoId, captor.getValue().eventoId());
  }

  @Test
  void abastecimentoAtualizadoVaiParaAtualizacao() {
    when(casoDeUso.atualizar(any())).thenReturn(ProjetarAbastecimento.Resultado.PROJETADO);

    listener.consumir(mensagem("AbastecimentoAtualizado", """
        {"eventoId":"%s","abastecimentoId":"%s","veiculoId":"%s",\
        "motoristaId":"%s","realizadoEm":"2026-11-08T18:00:00Z",\
        "combustivel":"GASOLINA","litros":42.5,"valorTotal":260.1,"quilometragem":1510,\
        "versao":2,"atualizadoPor":"gestor@nexfleet.dev"}
        """.formatted(eventoId, abastecimentoId, veiculoId, UUID.randomUUID())));

    var captor = ArgumentCaptor.forClass(ProjetarAbastecimento.Atualizacao.class);
    verify(casoDeUso).atualizar(captor.capture());
    assertEquals(eventoId, captor.getValue().eventoId());
    assertEquals(2, captor.getValue().versao());
    assertEquals(RECEBIDO_EM, captor.getValue().recebidoEm());
  }

  @Test
  void abastecimentoExcluidoVaiParaExclusao() {
    when(casoDeUso.excluir(any())).thenReturn(ProjetarAbastecimento.Resultado.EXCLUIDO);

    listener.consumir(mensagem("AbastecimentoExcluido", """
        {"eventoId":"%s","abastecimentoId":"%s","veiculoId":"%s","versao":3,\
        "excluidoPor":"gestor@nexfleet.dev","excluidoEm":"2026-11-10T18:00:00Z"}
        """.formatted(eventoId, abastecimentoId, veiculoId)));

    var captor = ArgumentCaptor.forClass(ProjetarAbastecimento.Exclusao.class);
    verify(casoDeUso).excluir(captor.capture());
    assertEquals(new ProjetarAbastecimento.Exclusao(eventoId, abastecimentoId, veiculoId, 3,
        Instant.parse("2026-11-10T18:00:00Z"), RECEBIDO_EM), captor.getValue());
  }

  @Test
  void tipoDesconhecidoEhRejeitadoSemExecutarCasoDeUso() {
    assertThrows(IllegalArgumentException.class,
        () -> listener.consumir(mensagem("ManutencaoRegistrada", registro())));

    verifyNoInteractions(casoDeUso);
  }

  private String registro() {
    return """
        {"eventoId":"%s","abastecimentoId":"%s","veiculoId":"%s",\
        "motoristaId":"%s","realizadoEm":"2026-11-08T18:00:00Z",\
        "combustivel":"DIESEL","litros":40.0,"valorTotal":250.0,"quilometragem":1500}
        """.formatted(eventoId, abastecimentoId, veiculoId, UUID.randomUUID());
  }

  private static Message mensagem(String tipo, String corpo) {
    var builder = MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json");
    if (tipo != null) builder.setHeader("eventoTipo", tipo);
    return builder.build();
  }
}
