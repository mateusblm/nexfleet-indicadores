package br.com.nexfleet.indicadores.consultardashboard.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeituraDashboard {
  record Totais(
      BigDecimal gastoAbastecimentos,
      BigDecimal gastoManutencoes,
      long quantidadeAbastecimentos,
      long quantidadeManutencoes) {}

  record ProximaManutencao(UUID veiculoId, String tipo, LocalDate data) {}

  Totais consultarTotais();

  List<ProximaManutencao> consultarProximasManutencoes(LocalDate hoje, int limite);
}
