package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjecoesManutencao;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjecoesManutencao implements ProjecoesManutencao {
  private final JdbcTemplate jdbc;
  public JdbcProjecoesManutencao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public boolean registrarSeAusente(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Instant recebidoEm) {
    return jdbc.update("INSERT INTO indicadores.manutencao_projecao(evento_id,manutencao_id,"
        + "veiculo_id,tipo,realizada_em,quilometragem,descricao,oficina,valor,"
        + "proxima_manutencao,recebido_em) VALUES (?,?,?,?,?,?,?,?,?,?,?) "
        + "ON CONFLICT DO NOTHING", eventoId, manutencaoId, veiculoId, tipo, realizadaEm,
        quilometragem, descricao, oficina, valor, proximaManutencao,
        Timestamp.from(recebidoEm)) == 1;
  }
}
