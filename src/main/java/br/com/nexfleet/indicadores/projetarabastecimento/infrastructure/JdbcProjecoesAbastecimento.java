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

  private static OffsetDateTime utc(Instant instante) {
    return instante.atOffset(ZoneOffset.UTC);
  }
}
