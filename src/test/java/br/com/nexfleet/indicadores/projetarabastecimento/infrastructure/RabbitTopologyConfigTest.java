package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

class RabbitTopologyConfigTest {
  private final RabbitTopologyConfig config = new RabbitTopologyConfig();

  @Test
  void deveConfigurarFilaPrincipalComDestinoParaFalhas() {
    Queue queue = config.abastecimentosQueue(
        "fila-principal",
        "eventos.dlx",
        "evento.falhou");

    assertThat(queue.isDurable()).isTrue();
    assertThat(queue.getArguments())
        .containsEntry("x-dead-letter-exchange", "eventos.dlx")
        .containsEntry("x-dead-letter-routing-key", "evento.falhou");
  }

  @Test
  void deveVincularDlqAoDeadLetterExchange() {
    DirectExchange exchange = config.deadLetterExchange("eventos.dlx");
    Queue deadLetterQueue = config.abastecimentosDeadLetterQueue("fila.dlq");

    Binding binding = config.abastecimentosDeadLetterBinding(
        exchange,
        deadLetterQueue,
        "evento.falhou");

    assertThat(exchange.isDurable()).isTrue();
    assertThat(deadLetterQueue.isDurable()).isTrue();
    assertThat(binding.getExchange()).isEqualTo("eventos.dlx");
    assertThat(binding.getDestination()).isEqualTo("fila.dlq");
    assertThat(binding.getRoutingKey()).isEqualTo("evento.falhou");
  }

  @Test
  void deveSepararFilaDeManutencoesESeuDestinoDeFalhas() {
    Queue queue = config.manutencoesQueue("manutencoes", "eventos.dlx", "manutencao.falhou");
    Queue dlq = config.manutencoesDeadLetterQueue("manutencoes.dlq");
    Binding binding = config.manutencoesDeadLetterBinding(
        config.deadLetterExchange("eventos.dlx"), dlq, "manutencao.falhou");
    assertThat(queue.getArguments())
        .containsEntry("x-dead-letter-routing-key", "manutencao.falhou");
    assertThat(binding.getDestination()).isEqualTo("manutencoes.dlq");
  }
}
