package br.com.nexfleet.indicadores.projetarmanutencao.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public class ProjetarManutencao {
  public enum Resultado { PROJETADO, DUPLICATA_IGNORADA, EXCLUIDO, IGNORADO }
  public record Entrada(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, Long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Instant recebidoEm) {}
  /** {@code ManutencaoAtualizada.v1}; {@code versao} ausente vale 1. */
  public record Atualizacao(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, Long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Integer versao, Instant recebidoEm) {}
  /** {@code ManutencaoExcluida.v1}; {@code versao} ausente vale 1. */
  public record Exclusao(UUID eventoId, UUID manutencaoId, UUID veiculoId, Integer versao,
      Instant excluidoEm, Instant recebidoEm) {}

  private final ProjecoesManutencao projecoes;
  public ProjetarManutencao(ProjecoesManutencao projecoes) { this.projecoes = projecoes; }

  public Resultado executar(Entrada entrada) {
    validar(entrada);
    boolean inserido = projecoes.registrarSeAusente(entrada.eventoId(), entrada.manutencaoId(),
        entrada.veiculoId(), entrada.tipo(), entrada.realizadaEm(), entrada.quilometragem(),
        entrada.descricao(), entrada.oficina(), entrada.valor(), entrada.proximaManutencao(),
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
    if (entrada == null || entrada.eventoId() == null || entrada.manutencaoId() == null
        || entrada.veiculoId() == null || entrada.tipo() == null || entrada.tipo().isBlank()
        || entrada.realizadaEm() == null || entrada.quilometragem() == null
        || entrada.quilometragem() < 0 || entrada.descricao() == null
        || entrada.descricao().isBlank() || entrada.oficina() == null
        || entrada.oficina().isBlank() || entrada.valor() == null
        || entrada.valor().signum() < 0 || entrada.recebidoEm() == null)
      throw new IllegalArgumentException("Evento de manutenção incompleto.");
    if (entrada.proximaManutencao() != null
        && !entrada.proximaManutencao().isAfter(entrada.realizadaEm()))
      throw new IllegalArgumentException("Próxima manutenção inválida.");
  }
}
