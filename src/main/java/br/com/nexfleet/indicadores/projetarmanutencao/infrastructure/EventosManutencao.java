package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

/**
 * Traduz os eventos de manutenção (v1) para o caso de uso, qualquer que seja o broker.
 * O tipo chega sem o sufixo de versão: {@code ManutencaoRegistrada},
 * {@code ManutencaoAtualizada} ou {@code ManutencaoExcluida}.
 */
public final class EventosManutencao {
  public static final String REGISTRADA = "ManutencaoRegistrada";
  public static final String ATUALIZADA = "ManutencaoAtualizada";
  public static final String EXCLUIDA = "ManutencaoExcluida";
  private static final Set<String> TIPOS = Set.of(REGISTRADA, ATUALIZADA, EXCLUIDA);

  private record Payload(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, Long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Integer versao, Instant excluidoEm) {}

  private final ProjetarManutencao casoDeUso;
  private final ObjectMapper json;
  private final Clock relogio;

  public EventosManutencao(ProjetarManutencao casoDeUso, ObjectMapper json, Clock relogio) {
    this.casoDeUso = casoDeUso;
    this.json = json;
    this.relogio = relogio;
  }

  public static boolean suporta(String tipo) {
    return TIPOS.contains(tipo);
  }

  /** Aplica o evento e devolve o seu {@code eventoId}. */
  public UUID consumir(String tipo, byte[] corpo) {
    if (!suporta(tipo)) {
      throw new IllegalArgumentException("Tipo de evento não suportado: " + tipo);
    }
    Payload p = ler(corpo);
    Instant recebidoEm = Instant.now(relogio);
    switch (tipo) {
      case REGISTRADA -> casoDeUso.executar(new ProjetarManutencao.Entrada(p.eventoId(),
          p.manutencaoId(), p.veiculoId(), p.tipo(), p.realizadaEm(), p.quilometragem(),
          p.descricao(), p.oficina(), p.valor(), p.proximaManutencao(), recebidoEm));
      case ATUALIZADA -> casoDeUso.atualizar(new ProjetarManutencao.Atualizacao(p.eventoId(),
          p.manutencaoId(), p.veiculoId(), p.tipo(), p.realizadaEm(), p.quilometragem(),
          p.descricao(), p.oficina(), p.valor(), p.proximaManutencao(), p.versao(),
          recebidoEm));
      default -> casoDeUso.excluir(new ProjetarManutencao.Exclusao(p.eventoId(),
          p.manutencaoId(), p.veiculoId(), p.versao(), p.excluidoEm(), recebidoEm));
    }
    return p.eventoId();
  }

  private Payload ler(byte[] corpo) {
    try {
      return json.readValue(corpo, Payload.class);
    } catch (RuntimeException falha) {
      throw new IllegalArgumentException("Evento de manutenção inválido.", falha);
    }
  }
}
