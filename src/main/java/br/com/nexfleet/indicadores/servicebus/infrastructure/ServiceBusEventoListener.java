package br.com.nexfleet.indicadores.servicebus.infrastructure;

import br.com.nexfleet.indicadores.observability.CorrelationIds;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "nexfleet.messaging.provider", havingValue = "servicebus")
public class ServiceBusEventoListener {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceBusEventoListener.class);

  private record AbastecimentoPayload(
      UUID eventoId, UUID abastecimentoId, UUID veiculoId, UUID motoristaId,
      Instant realizadoEm, String combustivel, BigDecimal litros,
      BigDecimal valorTotal, Long quilometragem) {}

  private record ManutencaoPayload(
      UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, Long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao) {}

  private final ProjetarAbastecimento projetarAbastecimento;
  private final ProjetarManutencao projetarManutencao;
  private final ObjectMapper json;
  private final Clock relogio;

  public ServiceBusEventoListener(
      ProjetarAbastecimento projetarAbastecimento,
      ProjetarManutencao projetarManutencao,
      ObjectMapper json,
      Clock relogio) {
    this.projetarAbastecimento = projetarAbastecimento;
    this.projetarManutencao = projetarManutencao;
    this.json = json;
    this.relogio = relogio;
  }

  @Transactional
  public void consumir(String eventType, String body, String correlationId) {
    try (var ignored = CorrelationIds.open(CorrelationIds.resolve(correlationId))) {
      switch (eventType) {
        case "AbastecimentoRegistrado.v1" -> consumirAbastecimento(body);
        case "ManutencaoRegistrada.v1" -> consumirManutencao(body);
        default -> throw new IllegalArgumentException("Tipo de evento não suportado: " + eventType);
      }
    }
  }

  private void consumirAbastecimento(String body) {
    try {
      AbastecimentoPayload p = json.readValue(body, AbastecimentoPayload.class);
      projetarAbastecimento.executar(new ProjetarAbastecimento.Entrada(
          p.eventoId(), p.abastecimentoId(), p.veiculoId(), p.motoristaId(),
          p.realizadoEm(), p.combustivel(), p.litros(), p.valorTotal(),
          p.quilometragem(), Instant.now(relogio)));
      LOG.info("event_consumed eventId={} eventType=AbastecimentoRegistrado.v1 broker=servicebus",
          p.eventoId());
    } catch (RuntimeException falha) {
      throw falha;
    } catch (Exception falha) {
      throw new IllegalArgumentException("Evento de abastecimento inválido.", falha);
    }
  }

  private void consumirManutencao(String body) {
    try {
      ManutencaoPayload p = json.readValue(body, ManutencaoPayload.class);
      projetarManutencao.executar(new ProjetarManutencao.Entrada(
          p.eventoId(), p.manutencaoId(), p.veiculoId(), p.tipo(), p.realizadaEm(),
          p.quilometragem(), p.descricao(), p.oficina(), p.valor(),
          p.proximaManutencao(), Instant.now(relogio)));
      LOG.info("event_consumed eventId={} eventType=ManutencaoRegistrada.v1 broker=servicebus",
          p.eventoId());
    } catch (RuntimeException falha) {
      throw falha;
    } catch (Exception falha) {
      throw new IllegalArgumentException("Evento de manutenção inválido.", falha);
    }
  }
}
