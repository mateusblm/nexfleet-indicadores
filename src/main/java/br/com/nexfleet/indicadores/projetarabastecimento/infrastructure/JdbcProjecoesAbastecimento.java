package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import br.com.nexfleet.indicadores.infrastructure.InsercaoIdempotente;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjecoesAbastecimento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjecoesAbastecimento implements ProjecoesAbastecimento {
  private static final String INSERIR_SE_AUSENTE =
      "INSERT INTO indicadores.abastecimento_projecao"
          + "(evento_id,abastecimento_id,veiculo_id,motorista_id,realizado_em,"
          + "combustivel,litros,valor_total,quilometragem,recebido_em) "
          + "SELECT ?,?,?,?,?,?,?,?,?,? "
          + "WHERE NOT EXISTS (SELECT 1 FROM indicadores.abastecimento_projecao "
          + "WITH (UPDLOCK, HOLDLOCK) WHERE evento_id = ?) "
          + "AND NOT EXISTS (SELECT 1 FROM indicadores.abastecimento_projecao "
          + "WITH (UPDLOCK, HOLDLOCK) WHERE abastecimento_id = ?)";

  /** Upsert por abastecimento: só versão maior substitui, e linha excluída nunca muda. */
  private static final String APLICAR_SE_MAIS_NOVA =
      "MERGE indicadores.abastecimento_projecao WITH (HOLDLOCK) AS alvo "
          + "USING (SELECT ? AS evento_id, ? AS abastecimento_id, ? AS veiculo_id, "
          + "? AS motorista_id, ? AS realizado_em, ? AS combustivel, ? AS litros, "
          + "? AS valor_total, ? AS quilometragem, ? AS versao, ? AS recebido_em) AS fonte "
          + "ON alvo.abastecimento_id = fonte.abastecimento_id "
          + "WHEN MATCHED AND alvo.excluido_em IS NULL AND alvo.versao < fonte.versao "
          + "THEN UPDATE SET evento_id = fonte.evento_id, veiculo_id = fonte.veiculo_id, "
          + "motorista_id = fonte.motorista_id, realizado_em = fonte.realizado_em, "
          + "combustivel = fonte.combustivel, litros = fonte.litros, "
          + "valor_total = fonte.valor_total, quilometragem = fonte.quilometragem, "
          + "versao = fonte.versao, recebido_em = fonte.recebido_em "
          + "WHEN NOT MATCHED THEN INSERT (evento_id,abastecimento_id,veiculo_id,motorista_id,"
          + "realizado_em,combustivel,litros,valor_total,quilometragem,versao,recebido_em) "
          + "VALUES (fonte.evento_id, fonte.abastecimento_id, fonte.veiculo_id, "
          + "fonte.motorista_id, fonte.realizado_em, fonte.combustivel, fonte.litros, "
          + "fonte.valor_total, fonte.quilometragem, fonte.versao, fonte.recebido_em);";

  /** Marca (tombstone) que vence qualquer registro ou atualização que chegue depois. */
  private static final String MARCAR_EXCLUIDO =
      "MERGE indicadores.abastecimento_projecao WITH (HOLDLOCK) AS alvo "
          + "USING (SELECT ? AS evento_id, ? AS abastecimento_id, ? AS veiculo_id, "
          + "? AS versao, ? AS excluido_em, ? AS recebido_em) AS fonte "
          + "ON alvo.abastecimento_id = fonte.abastecimento_id "
          + "WHEN MATCHED AND alvo.excluido_em IS NULL "
          + "THEN UPDATE SET evento_id = fonte.evento_id, "
          + "versao = IIF(fonte.versao > alvo.versao, fonte.versao, alvo.versao), "
          + "excluido_em = fonte.excluido_em, recebido_em = fonte.recebido_em "
          + "WHEN NOT MATCHED THEN INSERT "
          + "(evento_id,abastecimento_id,veiculo_id,versao,excluido_em,recebido_em) "
          + "VALUES (fonte.evento_id, fonte.abastecimento_id, fonte.veiculo_id, "
          + "fonte.versao, fonte.excluido_em, fonte.recebido_em);";

  private final JdbcTemplate jdbc;

  public JdbcProjecoesAbastecimento(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean registrarSeAusente(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      long quilometragem,
      Instant recebidoEm) {
    return InsercaoIdempotente.inserir(() -> jdbc.update(
        INSERIR_SE_AUSENTE,
        eventoId,
        abastecimentoId,
        veiculoId,
        motoristaId,
        utc(realizadoEm),
        combustivel,
        litros,
        valorTotal,
        quilometragem,
        utc(recebidoEm),
        eventoId,
        abastecimentoId));
  }

  @Override
  public boolean aplicarSeMaisNova(
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
      Instant recebidoEm) {
    return InsercaoIdempotente.inserir(() -> jdbc.update(
        APLICAR_SE_MAIS_NOVA,
        eventoId,
        abastecimentoId,
        veiculoId,
        motoristaId,
        utc(realizadoEm),
        combustivel,
        litros,
        valorTotal,
        quilometragem,
        versao,
        utc(recebidoEm)));
  }

  @Override
  public boolean marcarExcluido(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      int versao,
      Instant excluidoEm,
      Instant recebidoEm) {
    return InsercaoIdempotente.inserir(() -> jdbc.update(
        MARCAR_EXCLUIDO,
        eventoId,
        abastecimentoId,
        veiculoId,
        versao,
        utc(excluidoEm),
        utc(recebidoEm)));
  }

  private static OffsetDateTime utc(Instant instante) {
    return instante.atOffset(ZoneOffset.UTC);
  }
}
