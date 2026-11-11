package br.com.nexfleet.indicadores.projetarmanutencao.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import br.com.nexfleet.indicadores.SqlServerIntegracao;
import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard;
import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard.ProximaManutencao;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao;
import br.com.nexfleet.indicadores.projetarmanutencao.application.ProjetarManutencao.Resultado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Atualizações e exclusões versionadas de manutenção, observadas pelo dashboard. */
class ProjecaoManutencaoVersionadaIT extends SqlServerIntegracao {
  private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
  private static final LocalDate HOJE = LocalDate.parse("2026-11-10");

  @Autowired ProjetarManutencao projetar;
  @Autowired LeituraDashboard leitura;
  @Autowired JdbcTemplate jdbc;

  private final UUID manutencao = UUID.randomUUID();
  private final UUID veiculo = UUID.randomUUID();

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM indicadores.manutencao_projecao");
  }

  @Test
  void atualizacaoComVersaoMaiorSubstituiValorEProximaManutencao() {
    projetar.executar(registro("2026-10-01", "100.00", "2027-01-10"));

    assertEquals(Resultado.PROJETADO,
        projetar.atualizar(atualizacao("2026-10-01", "250.00", "2027-02-20", 2)));

    assertDashboard("250.00", 1);
    assertEquals(
        List.of(new ProximaManutencao(veiculo, "CORRETIVA", LocalDate.parse("2027-02-20"))),
        leitura.consultarProximasManutencoes(HOJE, 5));
  }

  @Test
  void atualizacaoComVersaoMenorOuIgualEhIgnorada() {
    projetar.executar(registro("2026-10-01", "100.00", null));
    projetar.atualizar(atualizacao("2026-10-01", "300.00", null, 3));

    assertEquals(Resultado.IGNORADO,
        projetar.atualizar(atualizacao("2026-10-01", "200.00", null, 2)));
    assertEquals(Resultado.IGNORADO,
        projetar.atualizar(atualizacao("2026-10-01", "999.00", null, 3)));

    assertDashboard("300.00", 1);
  }

  @Test
  void atualizacaoSemVersaoValeUmEEhIgnoradaDepoisDoRegistro() {
    projetar.executar(registro("2026-10-01", "100.00", null));

    assertEquals(Resultado.IGNORADO,
        projetar.atualizar(atualizacao("2026-10-01", "500.00", null, null)));

    assertDashboard("100.00", 1);
  }

  @Test
  void atualizacaoQueChegaAntesDoRegistroValeERegistroAtrasadoEhIgnorado() {
    assertEquals(Resultado.PROJETADO,
        projetar.atualizar(atualizacao("2026-10-01", "180.00", null, 2)));

    assertEquals(Resultado.DUPLICATA_IGNORADA,
        projetar.executar(registro("2026-10-01", "100.00", null)));

    assertDashboard("180.00", 1);
  }

  @Test
  void mesmaAtualizacaoEntregueDuasVezesEhAplicadaUmaVez() {
    projetar.executar(registro("2026-10-01", "100.00", null));
    var atualizacao = atualizacao("2026-10-01", "150.00", null, 2);

    assertEquals(Resultado.PROJETADO, projetar.atualizar(atualizacao));
    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao));

    assertDashboard("150.00", 1);
  }

  @Test
  void exclusaoTiraAManutencaoDoDashboardEDasProximas() {
    projetar.executar(new ProjetarManutencao.Entrada(UUID.randomUUID(), UUID.randomUUID(),
        veiculo, "PREVENTIVA", LocalDate.parse("2026-09-01"), 9_000L, "Revisão",
        "Oficina Central", new BigDecimal("40.00"), LocalDate.parse("2026-12-01"), AGORA));
    projetar.executar(registro("2026-10-01", "100.00", "2027-01-10"));

    assertEquals(Resultado.EXCLUIDO, projetar.excluir(exclusao(2)));

    assertDashboard("40.00", 1);
    assertEquals(
        List.of(new ProximaManutencao(veiculo, "PREVENTIVA", LocalDate.parse("2026-12-01"))),
        leitura.consultarProximasManutencoes(HOJE, 5));
  }

  @Test
  void mesmaExclusaoEntregueDuasVezesEhAplicadaUmaVez() {
    projetar.executar(registro("2026-10-01", "100.00", null));
    var exclusao = exclusao(2);

    assertEquals(Resultado.EXCLUIDO, projetar.excluir(exclusao));
    assertEquals(Resultado.IGNORADO, projetar.excluir(exclusao));

    assertDashboard("0", 0);
  }

  @Test
  void atualizacaoAtrasadaNaoFazAExcluidaReaparecer() {
    projetar.executar(registro("2026-10-01", "100.00", null));
    projetar.excluir(exclusao(3));

    assertEquals(Resultado.IGNORADO,
        projetar.atualizar(atualizacao("2026-10-01", "150.00", "2027-01-10", 2)));

    assertDashboard("0", 0);
    assertEquals(List.of(), leitura.consultarProximasManutencoes(HOJE, 5));
  }

  @Test
  void exclusaoQueChegaAntesDeTudoImpedeRegistroEAtualizacaoAtrasados() {
    assertEquals(Resultado.EXCLUIDO, projetar.excluir(exclusao(3)));

    assertEquals(Resultado.DUPLICATA_IGNORADA,
        projetar.executar(registro("2026-10-01", "100.00", "2027-01-10")));
    assertEquals(Resultado.IGNORADO,
        projetar.atualizar(atualizacao("2026-10-01", "150.00", "2027-01-10", 2)));

    assertDashboard("0", 0);
    assertEquals(List.of(), leitura.consultarProximasManutencoes(HOJE, 5));
  }

  private void assertDashboard(String gasto, long quantidade) {
    var totais = leitura.consultarTotais();
    assertEquals(0, new BigDecimal(gasto).compareTo(totais.gastoManutencoes()),
        "gasto esperado " + gasto + " mas foi " + totais.gastoManutencoes());
    assertEquals(quantidade, totais.quantidadeManutencoes());
  }

  private ProjetarManutencao.Entrada registro(String data, String valor, String proxima) {
    return new ProjetarManutencao.Entrada(UUID.randomUUID(), manutencao, veiculo,
        "PREVENTIVA", LocalDate.parse(data), 10_000L, "Troca de óleo", "Oficina Central",
        new BigDecimal(valor), proxima == null ? null : LocalDate.parse(proxima), AGORA);
  }

  private ProjetarManutencao.Atualizacao atualizacao(
      String data, String valor, String proxima, Integer versao) {
    return new ProjetarManutencao.Atualizacao(UUID.randomUUID(), manutencao, veiculo,
        "CORRETIVA", LocalDate.parse(data), 10_050L, "Troca de pastilhas", "Oficina Norte",
        new BigDecimal(valor), proxima == null ? null : LocalDate.parse(proxima), versao, AGORA);
  }

  private ProjetarManutencao.Exclusao exclusao(Integer versao) {
    return new ProjetarManutencao.Exclusao(UUID.randomUUID(), manutencao, veiculo, versao,
        Instant.parse("2026-11-09T09:00:00Z"), AGORA);
  }
}
