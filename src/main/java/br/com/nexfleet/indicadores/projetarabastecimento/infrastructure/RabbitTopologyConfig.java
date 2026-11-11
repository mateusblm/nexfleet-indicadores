package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "rabbit", matchIfMissing = true)
public class RabbitTopologyConfig {
  @Bean
  RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  TopicExchange eventosExchange(@Value("${nexfleet.eventos.exchange}") String nome) {
    return new TopicExchange(nome, true, false);
  }

  @Bean
  Queue abastecimentosQueue(
      @Value("${nexfleet.eventos.indicadores-queue}") String nome,
      @Value("${nexfleet.eventos.dlx}") String deadLetterExchange,
      @Value("${nexfleet.eventos.indicadores-dlq-routing-key}") String deadLetterRoutingKey) {
    return QueueBuilder.durable(nome)
        .deadLetterExchange(deadLetterExchange)
        .deadLetterRoutingKey(deadLetterRoutingKey)
        .build();
  }

  @Bean
  DirectExchange deadLetterExchange(@Value("${nexfleet.eventos.dlx}") String nome) {
    return new DirectExchange(nome, true, false);
  }

  @Bean
  Queue abastecimentosDeadLetterQueue(
      @Value("${nexfleet.eventos.indicadores-dlq}") String nome) {
    return QueueBuilder.durable(nome).build();
  }

  @Bean
  Binding abastecimentosDeadLetterBinding(
      DirectExchange deadLetterExchange,
      @Qualifier("abastecimentosDeadLetterQueue") Queue abastecimentosDeadLetterQueue,
      @Value("${nexfleet.eventos.indicadores-dlq-routing-key}") String routingKey) {
    return BindingBuilder.bind(abastecimentosDeadLetterQueue)
        .to(deadLetterExchange)
        .with(routingKey);
  }

  @Bean
  Binding abastecimentoRegistradoBinding(
      TopicExchange eventosExchange,
      @Qualifier("abastecimentosQueue") Queue abastecimentosQueue,
      @Value("${nexfleet.eventos.abastecimento-routing-key}") String routingKey) {
    return BindingBuilder.bind(abastecimentosQueue)
        .to(eventosExchange)
        .with(routingKey);
  }

  @Bean
  Queue manutencoesQueue(
      @Value("${nexfleet.eventos.manutencao-indicadores-queue}") String nome,
      @Value("${nexfleet.eventos.dlx}") String deadLetterExchange,
      @Value("${nexfleet.eventos.manutencao-indicadores-dlq-routing-key}") String routingKey) {
    return QueueBuilder.durable(nome).deadLetterExchange(deadLetterExchange)
        .deadLetterRoutingKey(routingKey).build();
  }

  @Bean
  Queue manutencoesDeadLetterQueue(
      @Value("${nexfleet.eventos.manutencao-indicadores-dlq}") String nome) {
    return QueueBuilder.durable(nome).build();
  }

  @Bean
  Binding manutencoesDeadLetterBinding(DirectExchange deadLetterExchange,
      @Qualifier("manutencoesDeadLetterQueue") Queue fila,
      @Value("${nexfleet.eventos.manutencao-indicadores-dlq-routing-key}") String routingKey) {
    return BindingBuilder.bind(fila).to(deadLetterExchange).with(routingKey);
  }

  @Bean
  Binding manutencaoRegistradaBinding(TopicExchange eventosExchange,
      @Qualifier("manutencoesQueue") Queue fila,
      @Value("${nexfleet.eventos.manutencao-routing-key}") String routingKey) {
    return BindingBuilder.bind(fila).to(eventosExchange).with(routingKey);
  }
}
