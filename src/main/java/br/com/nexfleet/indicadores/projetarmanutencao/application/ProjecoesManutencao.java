package br.com.nexfleet.indicadores.projetarmanutencao.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public interface ProjecoesManutencao {
  boolean registrarSeAusente(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Instant recebidoEm);

  /**
   * Substitui a projeção se {@code versao} for maior que a projetada, ou a cria se ainda não
   * existe. Nunca altera uma manutenção marcada como excluída.
   *
   * @return {@code true} se a projeção mudou
   */
  boolean aplicarSeMaisNova(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, int versao, Instant recebidoEm);

  /**
   * Marca a manutenção como excluída, criando a marca se ela ainda não foi projetada.
   *
   * @return {@code true} se a marca foi aplicada agora, {@code false} se já existia
   */
  boolean marcarExcluido(UUID eventoId, UUID manutencaoId, UUID veiculoId, int versao,
      Instant excluidoEm, Instant recebidoEm);
}
