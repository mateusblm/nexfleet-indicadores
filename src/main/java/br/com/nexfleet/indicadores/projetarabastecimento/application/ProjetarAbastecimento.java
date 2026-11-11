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

  /** Aplica a atualização só se a versão for maior que a projetada e não houver exclusão. */
  public Resultado atualizar(Atualizacao atualizacao) {
    if (atualizacao == null) {
      throw new IllegalArgumentException("Evento de abastecimento incompleto.");
    }
    validar(new Entrada(
        atualizacao.eventoId(),
        atualizacao.abastecimentoId(),
        atualizacao.veiculoId(),
        atualizacao.motoristaId(),
        atualizacao.realizadoEm(),
        atualizacao.combustivel(),
        atualizacao.litros(),
        atualizacao.valorTotal(),
        atualizacao.quilometragem(),
        atualizacao.recebidoEm()));
    boolean aplicada = projecoes.aplicarSeMaisNova(
        atualizacao.eventoId(),
        atualizacao.abastecimentoId(),
        atualizacao.veiculoId(),
        atualizacao.motoristaId(),
        atualizacao.realizadoEm(),
        atualizacao.combustivel(),
        atualizacao.litros(),
        atualizacao.valorTotal(),
        atualizacao.quilometragem(),
        versao(atualizacao.versao()),
        atualizacao.recebidoEm());
    return aplicada ? Resultado.PROJETADO : Resultado.IGNORADO;
  }

  /** Marca o abastecimento como excluído; a marca vence registros e atualizações atrasados. */
  public Resultado excluir(Exclusao exclusao) {
    if (exclusao == null
        || exclusao.eventoId() == null
        || exclusao.abastecimentoId() == null
        || exclusao.veiculoId() == null
        || exclusao.recebidoEm() == null) {
      throw new IllegalArgumentException("Evento de exclusão de abastecimento incompleto.");
    }
    boolean marcada = projecoes.marcarExcluido(
        exclusao.eventoId(),
        exclusao.abastecimentoId(),
        exclusao.veiculoId(),
        versao(exclusao.versao()),
        exclusao.excluidoEm() == null ? exclusao.recebidoEm() : exclusao.excluidoEm(),
        exclusao.recebidoEm());
    return marcada ? Resultado.EXCLUIDO : Resultado.IGNORADO;
  }

  /** Eventos sem {@code versao} (anteriores à v1.1.0) valem 1. */
  private static int versao(Integer versao) {
    if (versao == null) return 1;
    if (versao < 1) throw new IllegalArgumentException("Versão do abastecimento inválida.");
    return versao;
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
