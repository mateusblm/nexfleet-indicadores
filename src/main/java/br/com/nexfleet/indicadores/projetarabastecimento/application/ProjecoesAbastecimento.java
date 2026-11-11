package br.com.nexfleet.indicadores.projetarabastecimento.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface ProjecoesAbastecimento {
  boolean registrarSeAusente(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      long quilometragem,
      Instant recebidoEm);

  /**
   * Substitui a projeção se {@code versao} for maior que a projetada, ou a cria se ainda não
   * existe. Nunca altera um abastecimento marcado como excluído.
   *
   * @return {@code true} se a projeção mudou
   */
  boolean aplicarSeMaisNova(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      long quilometragem,
      int versao,
      Instant recebidoEm);

  /**
   * Marca o abastecimento como excluído, criando a marca se ele ainda não foi projetado.
   *
   * @return {@code true} se a marca foi aplicada agora, {@code false} se já existia
   */
  boolean marcarExcluido(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      int versao,
      Instant excluidoEm,
      Instant recebidoEm);
}
