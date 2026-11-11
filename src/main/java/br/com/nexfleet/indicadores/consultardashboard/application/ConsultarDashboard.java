package br.com.nexfleet.indicadores.consultardashboard.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class ConsultarDashboard {
  private static final int LIMITE_PROXIMAS_MANUTENCOES = 5;
  private final LeituraDashboard leitura;
  private final Clock relogio;

  public ConsultarDashboard(LeituraDashboard leitura, Clock relogio) {
    this.leitura = leitura;
    this.relogio = relogio;
  }

  public Resultado executar() {
    var totais = leitura.consultarTotais();
    var abastecimentos = valor(totais.gastoAbastecimentos());
    var manutencoes = valor(totais.gastoManutencoes());
    return new Resultado(
        abastecimentos,
        manutencoes,
        abastecimentos.add(manutencoes),
        totais.quantidadeAbastecimentos(),
        totais.quantidadeManutencoes(),
        leitura.consultarProximasManutencoes(
            LocalDate.now(relogio), LIMITE_PROXIMAS_MANUTENCOES));
  }

  private BigDecimal valor(BigDecimal valor) {
    return valor == null ? BigDecimal.ZERO : valor;
  }

  public record Resultado(
      BigDecimal gastoAbastecimentos,
      BigDecimal gastoManutencoes,
      BigDecimal gastoTotal,
      long quantidadeAbastecimentos,
      long quantidadeManutencoes,
      List<LeituraDashboard.ProximaManutencao> proximasManutencoes) {}
}
