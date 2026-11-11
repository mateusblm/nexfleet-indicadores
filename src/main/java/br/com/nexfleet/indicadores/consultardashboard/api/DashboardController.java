package br.com.nexfleet.indicadores.consultardashboard.api;

import br.com.nexfleet.indicadores.consultardashboard.application.ConsultarDashboard;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
  private final ConsultarDashboard dashboard;

  public DashboardController(ConsultarDashboard dashboard) {
    this.dashboard = dashboard;
  }

  @GetMapping("/api/v1/indicadores/dashboard")
  ResponseEntity<ConsultarDashboard.Resultado> consultar() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(dashboard.executar());
  }
}
