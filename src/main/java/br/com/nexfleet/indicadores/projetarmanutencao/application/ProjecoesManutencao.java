package br.com.nexfleet.indicadores.projetarmanutencao.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public interface ProjecoesManutencao {
  boolean registrarSeAusente(UUID eventoId, UUID manutencaoId, UUID veiculoId, String tipo,
      LocalDate realizadaEm, long quilometragem, String descricao, String oficina,
      BigDecimal valor, LocalDate proximaManutencao, Instant recebidoEm);
}
