package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import br.com.nexfleet.indicadores.persistencia.InsercaoIdempotente;
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
}
