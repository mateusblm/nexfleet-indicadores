package br.com.nexfleet.indicadores.projetarabastecimento.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import br.com.nexfleet.indicadores.SqlServerIntegracao;
import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento.Resultado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Atualizações e exclusões versionadas de abastecimento, observadas pelo dashboard. */
class ProjecaoAbastecimentoVersionadaIT extends SqlServerIntegracao {
  private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");

  @Autowired ProjetarAbastecimento projetar;
  @Autowired LeituraDashboard leitura;
  @Autowired JdbcTemplate jdbc;

  private final UUID abastecimento = UUID.randomUUID();
  private final UUID veiculo = UUID.randomUUID();

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM indicadores.abastecimento_projecao");
  }

  @Test
  void atualizacaoComVersaoMaiorSubstituiOValorNoDashboard() {
    projetar.executar(registro("100.00"));

    assertEquals(Resultado.PROJETADO, projetar.atualizar(atualizacao("150.00", 2)));

    assertDashboard("150.00", 1);
  }

  @Test
  void atualizacaoComVersaoMenorOuIgualEhIgnorada() {
    projetar.executar(registro("100.00"));
    projetar.atualizar(atualizacao("300.00", 3));

    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao("200.00", 2)));
    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao("999.00", 3)));

    assertDashboard("300.00", 1);
  }

  @Test
  void atualizacaoSemVersaoValeUmEEhIgnoradaDepoisDoRegistro() {
    projetar.executar(registro("100.00"));

    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao("500.00", null)));

    assertDashboard("100.00", 1);
  }

  @Test
  void atualizacaoQueChegaAntesDoRegistroValeERegistroAtrasadoEhIgnorado() {
    assertEquals(Resultado.PROJETADO, projetar.atualizar(atualizacao("180.00", 2)));

    assertEquals(Resultado.DUPLICATA_IGNORADA, projetar.executar(registro("100.00")));

    assertDashboard("180.00", 1);
  }

  @Test
  void mesmaAtualizacaoEntregueDuasVezesEhAplicadaUmaVez() {
    projetar.executar(registro("100.00"));
    var atualizacao = atualizacao("150.00", 2);

    assertEquals(Resultado.PROJETADO, projetar.atualizar(atualizacao));
    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao));

    assertDashboard("150.00", 1);
  }

  @Test
  void exclusaoTiraOAbastecimentoDoDashboard() {
    projetar.executar(registro("100.00"));
    projetar.executar(registroDeOutroAbastecimento("40.00"));

    assertEquals(Resultado.EXCLUIDO, projetar.excluir(exclusao(2)));

    assertDashboard("40.00", 1);
  }

  @Test
  void mesmaExclusaoEntregueDuasVezesEhAplicadaUmaVez() {
    projetar.executar(registro("100.00"));
    var exclusao = exclusao(2);

    assertEquals(Resultado.EXCLUIDO, projetar.excluir(exclusao));
    assertEquals(Resultado.IGNORADO, projetar.excluir(exclusao));

    assertDashboard("0", 0);
  }

  @Test
  void atualizacaoAtrasadaNaoFazOExcluidoReaparecer() {
    projetar.executar(registro("100.00"));
    projetar.excluir(exclusao(3));

    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao("150.00", 2)));

    assertDashboard("0", 0);
  }

  @Test
  void exclusaoQueChegaAntesDeTudoImpedeRegistroEAtualizacaoAtrasados() {
    assertEquals(Resultado.EXCLUIDO, projetar.excluir(exclusao(3)));

    assertEquals(Resultado.DUPLICATA_IGNORADA, projetar.executar(registro("100.00")));
    assertEquals(Resultado.IGNORADO, projetar.atualizar(atualizacao("150.00", 2)));

    assertDashboard("0", 0);
  }

  private void assertDashboard(String gasto, long quantidade) {
    var totais = leitura.consultarTotais();
    assertEquals(0, new BigDecimal(gasto).compareTo(totais.gastoAbastecimentos()),
        "gasto esperado " + gasto + " mas foi " + totais.gastoAbastecimentos());
    assertEquals(quantidade, totais.quantidadeAbastecimentos());
  }

  private ProjetarAbastecimento.Entrada registro(String valor) {
    return new ProjetarAbastecimento.Entrada(UUID.randomUUID(), abastecimento, veiculo,
        UUID.randomUUID(), Instant.parse("2026-11-05T10:00:00Z"), "DIESEL",
        new BigDecimal("40.00"), new BigDecimal(valor), 12_000L, AGORA);
  }

  private ProjetarAbastecimento.Entrada registroDeOutroAbastecimento(String valor) {
    return new ProjetarAbastecimento.Entrada(UUID.randomUUID(), UUID.randomUUID(), veiculo,
        UUID.randomUUID(), Instant.parse("2026-11-06T10:00:00Z"), "DIESEL",
        new BigDecimal("20.00"), new BigDecimal(valor), 12_500L, AGORA);
  }

  private ProjetarAbastecimento.Atualizacao atualizacao(String valor, Integer versao) {
    return new ProjetarAbastecimento.Atualizacao(UUID.randomUUID(), abastecimento, veiculo,
        UUID.randomUUID(), Instant.parse("2026-11-05T10:00:00Z"), "GASOLINA",
        new BigDecimal("42.00"), new BigDecimal(valor), 12_100L, versao, AGORA);
  }

  private ProjetarAbastecimento.Exclusao exclusao(Integer versao) {
    return new ProjetarAbastecimento.Exclusao(UUID.randomUUID(), abastecimento, veiculo,
        versao, Instant.parse("2026-11-09T09:00:00Z"), AGORA);
  }
}
