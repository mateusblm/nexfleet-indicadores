package br.com.nexfleet.indicadores.consultardashboard.infrastructure;

import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLeituraDashboard implements LeituraDashboard {
  private final JdbcTemplate jdbc;

  public JdbcLeituraDashboard(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Totais consultarTotais() {
    return jdbc.queryForObject(
        "SELECT "
            + "COALESCE((SELECT SUM(valor_total) FROM indicadores.abastecimento_projecao), 0), "
            + "COALESCE((SELECT SUM(valor) FROM indicadores.manutencao_projecao), 0), "
            + "(SELECT COUNT(*) FROM indicadores.abastecimento_projecao), "
            + "(SELECT COUNT(*) FROM indicadores.manutencao_projecao)",
        (resultado, linha) ->
            new Totais(
                resultado.getBigDecimal(1),
                resultado.getBigDecimal(2),
                resultado.getLong(3),
                resultado.getLong(4)));
  }

  @Override
  public List<ProximaManutencao> consultarProximasManutencoes(LocalDate hoje, int limite) {
    return jdbc.query(
        "WITH recentes AS ("
            + " SELECT veiculo_id, tipo, proxima_manutencao,"
            + " ROW_NUMBER() OVER (PARTITION BY veiculo_id"
            + " ORDER BY realizada_em DESC, recebido_em DESC) AS posicao"
            + " FROM indicadores.manutencao_projecao)"
            + " SELECT veiculo_id, tipo, proxima_manutencao FROM recentes"
            + " WHERE posicao = 1 AND proxima_manutencao >= ?"
            + " ORDER BY proxima_manutencao, veiculo_id LIMIT ?",
        (resultado, linha) ->
            new ProximaManutencao(
                resultado.getObject("veiculo_id", java.util.UUID.class),
                resultado.getString("tipo"),
                resultado.getObject("proxima_manutencao", LocalDate.class)),
        hoje,
        limite);
  }
}
