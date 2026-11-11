package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import br.com.nexfleet.indicadores.observability.CorrelationIds;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
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
 * Fila de manutenções: registro, atualização e exclusão chegam pela mesma fila e se
 * distinguem pelo cabeçalho {@code eventoTipo} (ausente = registro, como antes da v1.1.0).
 */
@Component
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "rabbit", matchIfMissing = true)
public class RabbitManutencaoListener {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitManutencaoListener.class);
  static final String CABECALHO_TIPO = "eventoTipo";

  private final EventosManutencao eventos;

  public RabbitManutencaoListener(ProjetarManutencao casoDeUso, ObjectMapper json, Clock relogio) {
    this.eventos = new EventosManutencao(casoDeUso, json, relogio);
  }

  @RabbitListener(queues = "${nexfleet.eventos.manutencao-indicadores-queue}")
  @Transactional
  public void consumir(Message mensagem) {
    String correlationId = CorrelationIds.from(mensagem);
    try (var ignored = CorrelationIds.open(correlationId)) {
      Object cabecalho = mensagem.getMessageProperties().getHeader(CABECALHO_TIPO);
      String tipo = cabecalho == null ? EventosManutencao.REGISTRADA : cabecalho.toString();
      UUID eventoId = eventos.consumir(tipo, mensagem.getBody());
      LOG.info("event_consumed eventId={} eventType={}", eventoId, tipo);
    }
  }
}
