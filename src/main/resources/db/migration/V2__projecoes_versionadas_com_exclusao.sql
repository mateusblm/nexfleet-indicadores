-- Edicao e exclusao no Operacoes (v1.1.0): cada projecao guarda a versao do agregado e uma
-- marca de exclusao (tombstone). Migracao aditiva: a imagem anterior ignora as colunas novas.
-- A marca pode chegar antes do registro (ordem trocada), entao a linha marcada nao tem os
-- dados de negocio: as colunas passam a aceitar NULL, e um CHECK exige os dados completos
-- enquanto a linha nao estiver excluida.

ALTER TABLE indicadores.abastecimento_projecao ADD
  versao INT NOT NULL CONSTRAINT df_abastecimento_projecao_versao DEFAULT 1,
  excluido_em DATETIMEOFFSET(6) NULL;
GO

DROP INDEX ix_indicadores_abastecimento_veiculo_data ON indicadores.abastecimento_projecao;
ALTER TABLE indicadores.abastecimento_projecao DROP CONSTRAINT
  ck_abastecimento_projecao_litros,
  ck_abastecimento_projecao_valor,
  ck_abastecimento_projecao_quilometragem;
GO

ALTER TABLE indicadores.abastecimento_projecao ALTER COLUMN motorista_id UNIQUEIDENTIFIER NULL;
ALTER TABLE indicadores.abastecimento_projecao ALTER COLUMN realizado_em DATETIMEOFFSET(6) NULL;
ALTER TABLE indicadores.abastecimento_projecao ALTER COLUMN combustivel NVARCHAR(40) NULL;
ALTER TABLE indicadores.abastecimento_projecao ALTER COLUMN litros NUMERIC(12,2) NULL;
ALTER TABLE indicadores.abastecimento_projecao ALTER COLUMN valor_total NUMERIC(12,2) NULL;
ALTER TABLE indicadores.abastecimento_projecao ALTER COLUMN quilometragem BIGINT NULL;
GO

ALTER TABLE indicadores.abastecimento_projecao ADD
  CONSTRAINT ck_abastecimento_projecao_litros CHECK (litros > 0),
  CONSTRAINT ck_abastecimento_projecao_valor CHECK (valor_total > 0),
  CONSTRAINT ck_abastecimento_projecao_quilometragem CHECK (quilometragem > 0),
  CONSTRAINT ck_abastecimento_projecao_versao CHECK (versao >= 1),
  CONSTRAINT ck_abastecimento_projecao_dados CHECK (
    excluido_em IS NOT NULL OR (
      motorista_id IS NOT NULL AND realizado_em IS NOT NULL AND combustivel IS NOT NULL
      AND litros IS NOT NULL AND valor_total IS NOT NULL AND quilometragem IS NOT NULL));
CREATE INDEX ix_indicadores_abastecimento_veiculo_data
  ON indicadores.abastecimento_projecao (veiculo_id, realizado_em DESC);
GO

ALTER TABLE indicadores.manutencao_projecao ADD
  versao INT NOT NULL CONSTRAINT df_manutencao_projecao_versao DEFAULT 1,
  excluido_em DATETIMEOFFSET(6) NULL;
GO

DROP INDEX ix_manutencao_projecao_veiculo_data ON indicadores.manutencao_projecao;
GO

ALTER TABLE indicadores.manutencao_projecao ALTER COLUMN tipo NVARCHAR(30) NULL;
ALTER TABLE indicadores.manutencao_projecao ALTER COLUMN realizada_em DATE NULL;
ALTER TABLE indicadores.manutencao_projecao ALTER COLUMN quilometragem BIGINT NULL;
ALTER TABLE indicadores.manutencao_projecao ALTER COLUMN descricao NVARCHAR(500) NULL;
ALTER TABLE indicadores.manutencao_projecao ALTER COLUMN oficina NVARCHAR(150) NULL;
ALTER TABLE indicadores.manutencao_projecao ALTER COLUMN valor NUMERIC(12,2) NULL;
GO

ALTER TABLE indicadores.manutencao_projecao ADD
  CONSTRAINT ck_manutencao_projecao_versao CHECK (versao >= 1),
  CONSTRAINT ck_manutencao_projecao_dados CHECK (
    excluido_em IS NOT NULL OR (
      tipo IS NOT NULL AND realizada_em IS NOT NULL AND quilometragem IS NOT NULL
      AND descricao IS NOT NULL AND oficina IS NOT NULL AND valor IS NOT NULL));
CREATE INDEX ix_manutencao_projecao_veiculo_data
  ON indicadores.manutencao_projecao (veiculo_id, realizada_em DESC);
GO
