package br.com.nexfleet.indicadores.servicebus.infrastructure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "servicebus")
public class ServiceBusConfig {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceBusConfig.class);

  @Bean(initMethod = "start", destroyMethod = "close")
  ServiceBusProcessorClient serviceBusProcessor(
      ServiceBusEventoListener listener,
      @Value("${nexfleet.servicebus.fully-qualified-namespace}") String namespace,
      @Value("${nexfleet.servicebus.topic}") String topic,
      @Value("${nexfleet.servicebus.subscription}") String subscription) {
    if (namespace == null || namespace.isBlank()) {
      throw new IllegalStateException("SERVICEBUS_FULLY_QUALIFIED_NAMESPACE é obrigatório.");
    }
    return new ServiceBusClientBuilder()
        .credential(namespace, new DefaultAzureCredentialBuilder().build())
        .processor()
        .topicName(topic)
        .subscriptionName(subscription)
        .disableAutoComplete()
        .processMessage(context -> {
          var message = context.getMessage();
          Map<String, Object> properties = message.getApplicationProperties();
          Object configuredType = properties.get("eventType");
          String eventType = configuredType == null ? message.getSubject() : configuredType.toString();
          String correlationId = message.getCorrelationId() == null
              ? message.getMessageId()
              : message.getCorrelationId();
          try {
            listener.consumir(eventType, message.getBody().toString(), correlationId);
            context.complete();
          } catch (RuntimeException falha) {
            LOG.warn("event_processing_failed eventType={} deliveryCount={} errorType={}",
                eventType, message.getDeliveryCount(), falha.getClass().getSimpleName());
            context.abandon();
          }
        })
        .processError(context -> LOG.error(
            "servicebus_processor_error namespace={} entity={} errorType={}",
            context.getFullyQualifiedNamespace(), context.getEntityPath(),
            context.getException().getClass().getSimpleName()))
        .buildProcessorClient();
  }
}
