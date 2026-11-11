package br.com.nexfleet.indicadores.observability;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

public final class CorrelationIds {
  public static final String HEADER = "X-Correlation-ID";
  public static final String MDC_KEY = "correlationId";

  private CorrelationIds() {}

  public static String resolve(String candidate) {
    if (candidate != null && candidate.length() == 36) {
      try {
        return UUID.fromString(candidate).toString();
      } catch (IllegalArgumentException ignored) {
        // Gera um valor seguro abaixo.
      }
    }
    return UUID.randomUUID().toString();
  }

  public static String from(Message message) {
    Object header = message.getMessageProperties().getHeaders().get(HEADER);
    String candidate = header == null ? null : header.toString();
    if (candidate == null) candidate = message.getMessageProperties().getMessageId();
    return resolve(candidate);
  }

  public static MDC.MDCCloseable open(String correlationId) {
    return MDC.putCloseable(MDC_KEY, correlationId);
  }
}
