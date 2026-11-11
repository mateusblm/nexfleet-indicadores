package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import br.com.nexfleet.indicadores.observability.CorrelationIds;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import java.math.BigDecimal;
import java.time.*;
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
public class RabbitManutencaoListener {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitManutencaoListener.class);
  private record Payload(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, Long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao) {}

  private final ProjetarManutencao casoDeUso;
  private final ObjectMapper json;
  private final Clock relogio;
  public RabbitManutencaoListener(ProjetarManutencao casoDeUso, ObjectMapper json, Clock relogio) {
    this.casoDeUso = casoDeUso;
    this.json = json;
    this.relogio = relogio;
  }

  @RabbitListener(queues = "${nexfleet.eventos.manutencao-indicadores-queue}")
  @Transactional
  public void consumir(Message mensagem) {
    String correlationId = CorrelationIds.from(mensagem);
    try (var ignored = CorrelationIds.open(correlationId)) {
      Payload p = json.readValue(mensagem.getBody(), Payload.class);
      casoDeUso.executar(new ProjetarManutencao.Entrada(p.eventoId(), p.manutencaoId(),
          p.veiculoId(), p.tipo(), p.realizadaEm(), p.quilometragem(), p.descricao(),
          p.oficina(), p.valor(), p.proximaManutencao(), Instant.now(relogio)));
      LOG.info("event_consumed eventId={} eventType=ManutencaoRegistrada", p.eventoId());
    } catch (RuntimeException falha) { throw falha; }
    catch (Exception falha) {
      throw new IllegalArgumentException("Evento de manutenção inválido.", falha);
    }
  }
}
