package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

/**
 * Traduz os eventos de abastecimento (v1) para o caso de uso, qualquer que seja o broker.
 * O tipo chega sem o sufixo de versão: {@code AbastecimentoRegistrado},
 * {@code AbastecimentoAtualizado} ou {@code AbastecimentoExcluido}.
 */
public final class EventosAbastecimento {
  public static final String REGISTRADO = "AbastecimentoRegistrado";
  public static final String ATUALIZADO = "AbastecimentoAtualizado";
  public static final String EXCLUIDO = "AbastecimentoExcluido";
  private static final Set<String> TIPOS = Set.of(REGISTRADO, ATUALIZADO, EXCLUIDO);

  private record Payload(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      Long quilometragem,
      Integer versao,
      Instant excluidoEm) {}

  private final ProjetarAbastecimento casoDeUso;
  private final ObjectMapper json;
  private final Clock relogio;

  public EventosAbastecimento(ProjetarAbastecimento casoDeUso, ObjectMapper json, Clock relogio) {
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
      case REGISTRADO -> casoDeUso.executar(new ProjetarAbastecimento.Entrada(
          p.eventoId(), p.abastecimentoId(), p.veiculoId(), p.motoristaId(), p.realizadoEm(),
          p.combustivel(), p.litros(), p.valorTotal(), p.quilometragem(), recebidoEm));
      case ATUALIZADO -> casoDeUso.atualizar(new ProjetarAbastecimento.Atualizacao(
          p.eventoId(), p.abastecimentoId(), p.veiculoId(), p.motoristaId(), p.realizadoEm(),
          p.combustivel(), p.litros(), p.valorTotal(), p.quilometragem(), p.versao(),
          recebidoEm));
      default -> casoDeUso.excluir(new ProjetarAbastecimento.Exclusao(
          p.eventoId(), p.abastecimentoId(), p.veiculoId(), p.versao(), p.excluidoEm(),
          recebidoEm));
    }
    return p.eventoId();
  }

  private Payload ler(byte[] corpo) {
    try {
      return json.readValue(corpo, Payload.class);
    } catch (RuntimeException falha) {
      throw new IllegalArgumentException("Evento de abastecimento inválido.", falha);
    }
  }
}
