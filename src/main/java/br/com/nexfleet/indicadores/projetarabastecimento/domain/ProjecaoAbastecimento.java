package br.com.nexfleet.indicadores.projetarabastecimento.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Regras para um evento de abastecimento entrar na projeção. A aplicação da versão maior e a
 * marca de exclusão (tombstone) são garantidas atomicamente pelo adaptador de persistência.
 */
public final class ProjecaoAbastecimento {
  /** Versão dos eventos publicados sem {@code versao} (anteriores à v1.1.0). */
  public static final int VERSAO_SEM_CAMPO = 1;

  /** Mensagem de evento sem algum dado obrigatório (ou ausente). */
  public static final String INCOMPLETO = "Evento de abastecimento incompleto.";

  private ProjecaoAbastecimento() {}

  /** Registro e atualização trazem todos os dados, com litros, valor e km positivos. */
  public static void validarDados(UUID eventoId, UUID abastecimentoId, UUID veiculoId,
      UUID motoristaId, Instant realizadoEm, String combustivel, BigDecimal litros,
      BigDecimal valorTotal, Long quilometragem, Instant recebidoEm) {
    if (eventoId == null
        || abastecimentoId == null
        || veiculoId == null
        || motoristaId == null
        || realizadoEm == null
        || combustivel == null
        || combustivel.isBlank()
        || litros == null
        || valorTotal == null
        || quilometragem == null
        || recebidoEm == null) {
      throw new IllegalArgumentException(INCOMPLETO);
    }
    if (litros.signum() <= 0 || valorTotal.signum() <= 0 || quilometragem <= 0) {
      throw new IllegalArgumentException("Valores do abastecimento devem ser positivos.");
    }
  }

  /** A exclusão identifica o evento, o abastecimento, o veículo e o recebimento. */
  public static void validarExclusao(UUID eventoId, UUID abastecimentoId, UUID veiculoId,
      Instant recebidoEm) {
    if (eventoId == null || abastecimentoId == null || veiculoId == null || recebidoEm == null) {
      throw new IllegalArgumentException("Evento de exclusão de abastecimento incompleto.");
    }
  }

  /** Versão ausente vale {@value #VERSAO_SEM_CAMPO}; menor que 1 é inválida. */
  public static int versao(Integer versao) {
    if (versao == null) return VERSAO_SEM_CAMPO;
    if (versao < 1) throw new IllegalArgumentException("Versão do abastecimento inválida.");
    return versao;
  }

  /** Sem data de exclusão no evento, vale o momento do recebimento. */
  public static Instant momentoExclusao(Instant excluidoEm, Instant recebidoEm) {
    return excluidoEm == null ? recebidoEm : excluidoEm;
  }
}
