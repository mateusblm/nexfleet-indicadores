package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import br.com.nexfleet.indicadores.observability.CorrelationIds;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "rabbit", matchIfMissing = true)
public class RabbitAbastecimentoListener {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitAbastecimentoListener.class);
  private record Payload(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      Long quilometragem) {}

  private final ProjetarAbastecimento casoDeUso;
  private final ObjectMapper json;
  private final Clock relogio;

  public RabbitAbastecimentoListener(
      ProjetarAbastecimento casoDeUso,
      ObjectMapper json,
      Clock relogio) {
    this.casoDeUso = casoDeUso;
    this.json = json;
    this.relogio = relogio;
  }

  @RabbitListener(queues = "${nexfleet.eventos.indicadores-queue}")
  @Transactional
  public void consumir(Message mensagem) {
    String correlationId = CorrelationIds.from(mensagem);
    try (var ignored = CorrelationIds.open(correlationId)) {
      Payload payload = json.readValue(mensagem.getBody(), Payload.class);
      casoDeUso.executar(new ProjetarAbastecimento.Entrada(
          payload.eventoId(),
          payload.abastecimentoId(),
          payload.veiculoId(),
          payload.motoristaId(),
          payload.realizadoEm(),
          payload.combustivel(),
          payload.litros(),
          payload.valorTotal(),
          payload.quilometragem(),
          Instant.now(relogio)));
      LOG.info("event_consumed eventId={} eventType=AbastecimentoRegistrado",
          payload.eventoId());
    } catch (RuntimeException falha) {
      throw falha;
    } catch (Exception falha) {
      throw new IllegalArgumentException("Evento de abastecimento inválido.", falha);
    }
  }
}
