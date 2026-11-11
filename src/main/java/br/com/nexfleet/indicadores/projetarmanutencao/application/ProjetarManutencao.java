package br.com.nexfleet.indicadores.projetarmanutencao.application;

import br.com.nexfleet.indicadores.projetarmanutencao.domain.ProjecaoManutencao;
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

  /** Aplica a atualização só se a versão for maior que a projetada e não houver exclusão. */
  public Resultado atualizar(Atualizacao a) {
    if (a == null) throw new IllegalArgumentException(ProjecaoManutencao.INCOMPLETO);
    validar(new Entrada(a.eventoId(), a.manutencaoId(), a.veiculoId(), a.tipo(),
        a.realizadaEm(), a.quilometragem(), a.descricao(), a.oficina(), a.valor(),
        a.proximaManutencao(), a.recebidoEm()));
    boolean aplicada = projecoes.aplicarSeMaisNova(a.eventoId(), a.manutencaoId(),
        a.veiculoId(), a.tipo(), a.realizadaEm(), a.quilometragem(), a.descricao(),
        a.oficina(), a.valor(), a.proximaManutencao(), ProjecaoManutencao.versao(a.versao()), a.recebidoEm());
    return aplicada ? Resultado.PROJETADO : Resultado.IGNORADO;
  }

  /** Marca a manutenção como excluída; a marca vence registros e atualizações atrasados. */
  public Resultado excluir(Exclusao e) {
    if (e == null)
      throw new IllegalArgumentException("Evento de exclusão de manutenção incompleto.");
    ProjecaoManutencao.validarExclusao(e.eventoId(), e.manutencaoId(), e.veiculoId(),
        e.recebidoEm());
    boolean marcada = projecoes.marcarExcluido(e.eventoId(), e.manutencaoId(), e.veiculoId(),
        ProjecaoManutencao.versao(e.versao()),
        ProjecaoManutencao.momentoExclusao(e.excluidoEm(), e.recebidoEm()), e.recebidoEm());
    return marcada ? Resultado.EXCLUIDO : Resultado.IGNORADO;
  }

  private void validar(Entrada entrada) {
    if (entrada == null) throw new IllegalArgumentException(ProjecaoManutencao.INCOMPLETO);
    ProjecaoManutencao.validarDados(entrada.eventoId(), entrada.manutencaoId(),
        entrada.veiculoId(), entrada.tipo(), entrada.realizadaEm(), entrada.quilometragem(),
        entrada.descricao(), entrada.oficina(), entrada.valor(), entrada.proximaManutencao(),
        entrada.recebidoEm());
  }
}
