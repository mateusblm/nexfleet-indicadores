package br.com.nexfleet.indicadores.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;

class CorrelationIdsTest {
  @Test
  void leCorrelationIdDoCabecalhoRabbit() {
    String correlationId = UUID.randomUUID().toString();
    var message = MessageBuilder.withBody(new byte[0])
        .setHeader(CorrelationIds.HEADER, correlationId)
        .setMessageId(UUID.randomUUID().toString())
        .build();

    assertEquals(correlationId, CorrelationIds.from(message));
  }

  @Test
  void usaMessageIdEmEventoAntigoSemCabecalho() {
    String messageId = UUID.randomUUID().toString();
    var message = MessageBuilder.withBody(new byte[0]).setMessageId(messageId).build();

    assertEquals(messageId, CorrelationIds.from(message));
  }
}
