package br.com.nexfleet.indicadores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
