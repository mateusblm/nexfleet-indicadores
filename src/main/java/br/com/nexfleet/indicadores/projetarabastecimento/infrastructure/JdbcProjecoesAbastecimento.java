package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjecoesAbastecimento;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjecoesAbastecimento implements ProjecoesAbastecimento {
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
    int inseridos = jdbc.update(
        "INSERT INTO indicadores.abastecimento_projecao"
            + "(evento_id,abastecimento_id,veiculo_id,motorista_id,realizado_em,"
            + "combustivel,litros,valor_total,quilometragem,recebido_em) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
        eventoId,
        abastecimentoId,
        veiculoId,
        motoristaId,
        Timestamp.from(realizadoEm),
        combustivel,
        litros,
        valorTotal,
        quilometragem,
        Timestamp.from(recebidoEm));
    return inseridos == 1;
  }
}
