package br.com.nexfleet.indicadores.projetarmanutencao.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Regras para um evento de manutenção entrar na projeção. A aplicação da versão maior e a marca
 * de exclusão (tombstone) são garantidas atomicamente pelo adaptador de persistência.
 */
public final class ProjecaoManutencao {
  /** Versão dos eventos publicados sem {@code versao} (anteriores à v1.1.0). */
  public static final int VERSAO_SEM_CAMPO = 1;

  /** Mensagem de evento sem algum dado obrigatório (ou ausente). */
  public static final String INCOMPLETO = "Evento de manutenção incompleto.";

  private ProjecaoManutencao() {}

  /**
   * Registro e atualização trazem todos os dados, km e valor não negativos e próxima manutenção
   * posterior à realizada.
   */
  public static void validarDados(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, Long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Instant recebidoEm) {
    if (eventoId == null || manutencaoId == null
        || veiculoId == null || tipo == null || tipo.isBlank()
        || realizadaEm == null || quilometragem == null
        || quilometragem < 0 || descricao == null
        || descricao.isBlank() || oficina == null
        || oficina.isBlank() || valor == null
        || valor.signum() < 0 || recebidoEm == null)
      throw new IllegalArgumentException(INCOMPLETO);
    if (proximaManutencao != null && !proximaManutencao.isAfter(realizadaEm))
      throw new IllegalArgumentException("Próxima manutenção inválida.");
  }

  /** A exclusão identifica o evento, a manutenção, o veículo e o recebimento. */
  public static void validarExclusao(UUID eventoId, UUID manutencaoId, UUID veiculoId,
      Instant recebidoEm) {
    if (eventoId == null || manutencaoId == null || veiculoId == null || recebidoEm == null)
      throw new IllegalArgumentException("Evento de exclusão de manutenção incompleto.");
  }

  /** Versão ausente vale {@value #VERSAO_SEM_CAMPO}; menor que 1 é inválida. */
  public static int versao(Integer versao) {
    if (versao == null) return VERSAO_SEM_CAMPO;
    if (versao < 1) throw new IllegalArgumentException("Versão da manutenção inválida.");
    return versao;
  }

  /** Sem data de exclusão no evento, vale o momento do recebimento. */
  public static Instant momentoExclusao(Instant excluidoEm, Instant recebidoEm) {
    return excluidoEm == null ? recebidoEm : excluidoEm;
  }
}
