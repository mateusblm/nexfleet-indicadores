-- Indicadores no Azure SQL / SQL Server (schema indicadores, criado pelo bootstrap do admin).
-- Recomeco limpo em T-SQL: substitui as migracoes V1..V3 do PostgreSQL.
-- O evento_id (PK) de cada projecao e o registro de eventos processados: um evento
-- ja aplicado nao e projetado de novo.

EXEC sys.sp_addextendedproperty
  @name = N'MS_Description', @value = N'NexFleet: dados exclusivos do servico indicadores',
  @level0type = N'SCHEMA', @level0name = N'indicadores';
GO

CREATE TABLE indicadores.abastecimento_projecao (
  evento_id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_abastecimento_projecao PRIMARY KEY,
  abastecimento_id UNIQUEIDENTIFIER NOT NULL,
  veiculo_id UNIQUEIDENTIFIER NOT NULL,
  motorista_id UNIQUEIDENTIFIER NOT NULL,
  realizado_em DATETIMEOFFSET(6) NOT NULL,
  combustivel NVARCHAR(40) NOT NULL,
  litros NUMERIC(12,2) NOT NULL CONSTRAINT ck_abastecimento_projecao_litros CHECK (litros > 0),
  valor_total NUMERIC(12,2) NOT NULL
    CONSTRAINT ck_abastecimento_projecao_valor CHECK (valor_total > 0),
  quilometragem BIGINT NOT NULL
    CONSTRAINT ck_abastecimento_projecao_quilometragem CHECK (quilometragem > 0),
  recebido_em DATETIMEOFFSET(6) NOT NULL
);
GO

CREATE UNIQUE INDEX uq_indicadores_abastecimento
  ON indicadores.abastecimento_projecao (abastecimento_id);
GO

CREATE INDEX ix_indicadores_abastecimento_veiculo_data
  ON indicadores.abastecimento_projecao (veiculo_id, realizado_em DESC);
GO

CREATE TABLE indicadores.manutencao_projecao (
  evento_id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_manutencao_projecao PRIMARY KEY,
  manutencao_id UNIQUEIDENTIFIER NOT NULL CONSTRAINT uq_manutencao_projecao_manutencao UNIQUE,
  veiculo_id UNIQUEIDENTIFIER NOT NULL,
  tipo NVARCHAR(30) NOT NULL,
  realizada_em DATE NOT NULL,
  quilometragem BIGINT NOT NULL,
  descricao NVARCHAR(500) NOT NULL,
  oficina NVARCHAR(150) NOT NULL,
  valor NUMERIC(12,2) NOT NULL,
  proxima_manutencao DATE NULL,
  recebido_em DATETIMEOFFSET(6) NOT NULL
);
GO

CREATE INDEX ix_manutencao_projecao_veiculo_data
  ON indicadores.manutencao_projecao (veiculo_id, realizada_em DESC);
GO
