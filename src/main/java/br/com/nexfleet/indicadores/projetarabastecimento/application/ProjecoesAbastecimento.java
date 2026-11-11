package br.com.nexfleet.indicadores.projetarabastecimento.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface ProjecoesAbastecimento {
  boolean registrarSeAusente(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      long quilometragem,
      Instant recebidoEm);
}
