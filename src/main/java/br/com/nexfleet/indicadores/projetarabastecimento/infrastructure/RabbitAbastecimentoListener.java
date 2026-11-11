package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import br.com.nexfleet.indicadores.observability.CorrelationIds;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import java.time.Clock;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Fila de abastecimentos: registro, atualização e exclusão chegam pela mesma fila e se
 * distinguem pelo cabeçalho {@code eventoTipo} (ausente = registro, como antes da v1.1.0).
 */
@Component
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "rabbit", matchIfMissing = true)
public class RabbitAbastecimentoListener {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitAbastecimentoListener.class);
  static final String CABECALHO_TIPO = "eventoTipo";

  private final EventosAbastecimento eventos;

  public RabbitAbastecimentoListener(
      ProjetarAbastecimento casoDeUso,
      ObjectMapper json,
      Clock relogio) {
    this.eventos = new EventosAbastecimento(casoDeUso, json, relogio);
  }

  @RabbitListener(queues = "${nexfleet.eventos.indicadores-queue}")
  @Transactional
  public void consumir(Message mensagem) {
    String correlationId = CorrelationIds.from(mensagem);
    try (var ignored = CorrelationIds.open(correlationId)) {
      Object cabecalho = mensagem.getMessageProperties().getHeader(CABECALHO_TIPO);
      String tipo = cabecalho == null ? EventosAbastecimento.REGISTRADO : cabecalho.toString();
      UUID eventoId = eventos.consumir(tipo, mensagem.getBody());
      LOG.info("event_consumed eventId={} eventType={}", eventoId, tipo);
    }
  }
}
