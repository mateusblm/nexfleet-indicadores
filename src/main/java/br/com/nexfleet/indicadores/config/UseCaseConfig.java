package br.com.nexfleet.indicadores.config;

import br.com.nexfleet.indicadores.consultarservico.application.ConsultarServico;
import br.com.nexfleet.indicadores.consultarservico.application.VerificarBanco;
import br.com.nexfleet.indicadores.consultardashboard.application.ConsultarDashboard;
import br.com.nexfleet.indicadores.consultardashboard.application.LeituraDashboard;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjecoesAbastecimento;
import br.com.nexfleet.indicadores.projetarabastecimento.application.ProjetarAbastecimento;
import br.com.nexfleet.indicadores.projetarmanutencao.application.*;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
  @Bean
  Clock relogio() {
    return Clock.systemUTC();
  }

  @Bean
  ProjetarAbastecimento projetarAbastecimento(ProjecoesAbastecimento projecoes) {
    return new ProjetarAbastecimento(projecoes);
  }

  @Bean
  ProjetarManutencao projetarManutencao(ProjecoesManutencao projecoes) {
    return new ProjetarManutencao(projecoes);
  }

  @Bean
  ConsultarDashboard consultarDashboard(LeituraDashboard leitura, Clock relogio) {
    return new ConsultarDashboard(leitura, relogio);
  }

  @Bean
  ConsultarServico consultarServico(VerificarBanco banco) {
    return new ConsultarServico(banco);
  }
}
