CREATE TABLE indicadores.manutencao_projecao (
  evento_id UUID PRIMARY KEY,
  manutencao_id UUID NOT NULL UNIQUE,
  veiculo_id UUID NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  realizada_em DATE NOT NULL,
  quilometragem BIGINT NOT NULL,
  descricao VARCHAR(500) NOT NULL,
  oficina VARCHAR(150) NOT NULL,
  valor NUMERIC(12,2) NOT NULL,
  proxima_manutencao DATE,
  recebido_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_manutencao_projecao_veiculo_data
  ON indicadores.manutencao_projecao (veiculo_id, realizada_em DESC);
