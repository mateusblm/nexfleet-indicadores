package br.com.nexfleet.indicadores.projetarabastecimento.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProjetarAbastecimento {
  public enum Resultado { PROJETADO, DUPLICATA_IGNORADA, EXCLUIDO, IGNORADO }

  public record Entrada(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      Long quilometragem,
      Instant recebidoEm) {}

  /** {@code AbastecimentoAtualizado.v1}; {@code versao} ausente vale 1. */
  public record Atualizacao(
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
      Instant recebidoEm) {}

  /** {@code AbastecimentoExcluido.v1}; {@code versao} ausente vale 1. */
  public record Exclusao(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      Integer versao,
      Instant excluidoEm,
      Instant recebidoEm) {}

  private final ProjecoesAbastecimento projecoes;

  public ProjetarAbastecimento(ProjecoesAbastecimento projecoes) {
    this.projecoes = projecoes;
  }

  public Resultado executar(Entrada entrada) {
    validar(entrada);
    boolean inserido = projecoes.registrarSeAusente(
        entrada.eventoId(),
        entrada.abastecimentoId(),
        entrada.veiculoId(),
        entrada.motoristaId(),
        entrada.realizadoEm(),
        entrada.combustivel(),
        entrada.litros(),
        entrada.valorTotal(),
        entrada.quilometragem(),
        entrada.recebidoEm());
    return inserido ? Resultado.PROJETADO : Resultado.DUPLICATA_IGNORADA;
  }

  public Resultado atualizar(Atualizacao atualizacao) {
    throw new UnsupportedOperationException("ainda não implementado");
  }

  public Resultado excluir(Exclusao exclusao) {
    throw new UnsupportedOperationException("ainda não implementado");
  }

  private void validar(Entrada entrada) {
    if (entrada == null
        || entrada.eventoId() == null
        || entrada.abastecimentoId() == null
        || entrada.veiculoId() == null
        || entrada.motoristaId() == null
        || entrada.realizadoEm() == null
        || entrada.combustivel() == null
        || entrada.combustivel().isBlank()
        || entrada.litros() == null
        || entrada.valorTotal() == null
        || entrada.quilometragem() == null
        || entrada.recebidoEm() == null) {
      throw new IllegalArgumentException("Evento de abastecimento incompleto.");
    }
    if (entrada.litros().signum() <= 0
        || entrada.valorTotal().signum() <= 0
        || entrada.quilometragem() <= 0) {
      throw new IllegalArgumentException("Valores do abastecimento devem ser positivos.");
    }
  }
}
