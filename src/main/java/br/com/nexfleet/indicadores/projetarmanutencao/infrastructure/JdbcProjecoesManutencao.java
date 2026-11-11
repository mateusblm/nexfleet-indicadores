package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import br.com.nexfleet.indicadores.infrastructure.InsercaoIdempotente;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjecoesManutencao;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.*;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjecoesManutencao implements ProjecoesManutencao {
  private static final String INSERIR_SE_AUSENTE =
      "INSERT INTO indicadores.manutencao_projecao(evento_id,manutencao_id,"
          + "veiculo_id,tipo,realizada_em,quilometragem,descricao,oficina,valor,"
          + "proxima_manutencao,recebido_em) SELECT ?,?,?,?,?,?,?,?,?,?,? "
          + "WHERE NOT EXISTS (SELECT 1 FROM indicadores.manutencao_projecao "
          + "WITH (UPDLOCK, HOLDLOCK) WHERE evento_id = ?) "
          + "AND NOT EXISTS (SELECT 1 FROM indicadores.manutencao_projecao "
          + "WITH (UPDLOCK, HOLDLOCK) WHERE manutencao_id = ?)";

  /** Upsert por manutenção: só versão maior substitui, e linha excluída nunca muda. */
  private static final String APLICAR_SE_MAIS_NOVA =
      "MERGE indicadores.manutencao_projecao WITH (HOLDLOCK) AS alvo "
          + "USING (SELECT ? AS evento_id, ? AS manutencao_id, ? AS veiculo_id, ? AS tipo, "
          + "? AS realizada_em, ? AS quilometragem, ? AS descricao, ? AS oficina, ? AS valor, "
          + "? AS proxima_manutencao, ? AS versao, ? AS recebido_em) AS fonte "
          + "ON alvo.manutencao_id = fonte.manutencao_id "
          + "WHEN MATCHED AND alvo.excluido_em IS NULL AND alvo.versao < fonte.versao "
          + "THEN UPDATE SET evento_id = fonte.evento_id, veiculo_id = fonte.veiculo_id, "
          + "tipo = fonte.tipo, realizada_em = fonte.realizada_em, "
          + "quilometragem = fonte.quilometragem, descricao = fonte.descricao, "
          + "oficina = fonte.oficina, valor = fonte.valor, "
          + "proxima_manutencao = fonte.proxima_manutencao, versao = fonte.versao, "
          + "recebido_em = fonte.recebido_em "
          + "WHEN NOT MATCHED THEN INSERT (evento_id,manutencao_id,veiculo_id,tipo,realizada_em,"
          + "quilometragem,descricao,oficina,valor,proxima_manutencao,versao,recebido_em) "
          + "VALUES (fonte.evento_id, fonte.manutencao_id, fonte.veiculo_id, fonte.tipo, "
          + "fonte.realizada_em, fonte.quilometragem, fonte.descricao, fonte.oficina, "
          + "fonte.valor, fonte.proxima_manutencao, fonte.versao, fonte.recebido_em);";

  /** Marca (tombstone) que vence qualquer registro ou atualização que chegue depois. */
  private static final String MARCAR_EXCLUIDO =
      "MERGE indicadores.manutencao_projecao WITH (HOLDLOCK) AS alvo "
          + "USING (SELECT ? AS evento_id, ? AS manutencao_id, ? AS veiculo_id, "
          + "? AS versao, ? AS excluido_em, ? AS recebido_em) AS fonte "
          + "ON alvo.manutencao_id = fonte.manutencao_id "
          + "WHEN MATCHED AND alvo.excluido_em IS NULL "
          + "THEN UPDATE SET evento_id = fonte.evento_id, "
          + "versao = IIF(fonte.versao > alvo.versao, fonte.versao, alvo.versao), "
          + "excluido_em = fonte.excluido_em, recebido_em = fonte.recebido_em "
          + "WHEN NOT MATCHED THEN INSERT "
          + "(evento_id,manutencao_id,veiculo_id,versao,excluido_em,recebido_em) "
          + "VALUES (fonte.evento_id, fonte.manutencao_id, fonte.veiculo_id, "
          + "fonte.versao, fonte.excluido_em, fonte.recebido_em);";

  private final JdbcTemplate jdbc;
  public JdbcProjecoesManutencao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public boolean registrarSeAusente(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Instant recebidoEm) {
    return InsercaoIdempotente.inserir(() -> jdbc.update(INSERIR_SE_AUSENTE, eventoId,
        manutencaoId, veiculoId, tipo, realizadaEm, quilometragem, descricao, oficina, valor,
        new SqlParameterValue(Types.DATE, proximaManutencao),
        recebidoEm.atOffset(ZoneOffset.UTC), eventoId, manutencaoId));
  }

  @Override
  public boolean aplicarSeMaisNova(UUID eventoId, UUID manutencaoId, UUID veiculoId,
      String tipo, LocalDate realizadaEm, long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, int versao, Instant recebidoEm) {
    return InsercaoIdempotente.inserir(() -> jdbc.update(APLICAR_SE_MAIS_NOVA, eventoId,
        manutencaoId, veiculoId, tipo, realizadaEm, quilometragem, descricao, oficina, valor,
        new SqlParameterValue(Types.DATE, proximaManutencao), versao,
        recebidoEm.atOffset(ZoneOffset.UTC)));
  }

  @Override
  public boolean marcarExcluido(UUID eventoId, UUID manutencaoId, UUID veiculoId, int versao,
      Instant excluidoEm, Instant recebidoEm) {
    return InsercaoIdempotente.inserir(() -> jdbc.update(MARCAR_EXCLUIDO, eventoId,
        manutencaoId, veiculoId, versao, excluidoEm.atOffset(ZoneOffset.UTC),
        recebidoEm.atOffset(ZoneOffset.UTC)));
  }
}
