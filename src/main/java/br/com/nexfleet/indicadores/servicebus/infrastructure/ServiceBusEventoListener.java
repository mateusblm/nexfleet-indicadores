package br.com.nexfleet.indicadores.servicebus.infrastructure;

import br.com.nexfleet.indicadores.observability.CorrelationIds;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarabastecimento.infrastructure.EventosAbastecimento;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import br.com.nexfleet.indicadores.projetarmanutencao.infrastructure.EventosManutencao;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Recebe da assinatura {@code indicadores-v1} os eventos v1 de abastecimento e manutenção. */
@Component
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "servicebus")
public class ServiceBusEventoListener {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceBusEventoListener.class);
  private static final String SUFIXO_V1 = ".v1";

  private final EventosAbastecimento abastecimentos;
  private final EventosManutencao manutencoes;

  public ServiceBusEventoListener(
      ProjetarAbastecimento projetarAbastecimento,
      ProjetarManutencao projetarManutencao,
      ObjectMapper json,
      Clock relogio) {
    this.abastecimentos = new EventosAbastecimento(projetarAbastecimento, json, relogio);
    this.manutencoes = new EventosManutencao(projetarManutencao, json, relogio);
  }

  @Transactional
  public void consumir(String eventType, String body, String correlationId) {
    try (var ignored = CorrelationIds.open(CorrelationIds.resolve(correlationId))) {
      String tipo = eventType != null && eventType.endsWith(SUFIXO_V1)
          ? eventType.substring(0, eventType.length() - SUFIXO_V1.length())
          : "";
      byte[] corpo = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
      UUID eventoId;
      if (EventosAbastecimento.suporta(tipo)) {
        eventoId = abastecimentos.consumir(tipo, corpo);
      } else if (EventosManutencao.suporta(tipo)) {
        eventoId = manutencoes.consumir(tipo, corpo);
      } else {
        throw new IllegalArgumentException("Tipo de evento não suportado: " + eventType);
      }
      LOG.info("event_consumed eventId={} eventType={} broker=servicebus", eventoId, eventType);
    }
  }
}
