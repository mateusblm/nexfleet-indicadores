CREATE TABLE indicadores.abastecimento_projecao (
  evento_id UUID PRIMARY KEY,
  abastecimento_id UUID NOT NULL,
  veiculo_id UUID NOT NULL,
  motorista_id UUID NOT NULL,
  realizado_em TIMESTAMPTZ NOT NULL,
  combustivel VARCHAR(40) NOT NULL,
  litros NUMERIC(12,2) NOT NULL CHECK (litros > 0),
  valor_total NUMERIC(12,2) NOT NULL CHECK (valor_total > 0),
  quilometragem BIGINT NOT NULL CHECK (quilometragem > 0),
  recebido_em TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_indicadores_abastecimento
  ON indicadores.abastecimento_projecao (abastecimento_id);

CREATE INDEX ix_indicadores_abastecimento_veiculo_data
  ON indicadores.abastecimento_projecao (veiculo_id, realizado_em DESC);
