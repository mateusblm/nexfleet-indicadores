package br.com.nexfleet.indicadores.persistencia;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;

class InsercaoIdempotenteTest {
  @Test
  void linhaInseridaRetornaVerdadeiroELinhaJaExistenteRetornaFalso() {
    assertTrue(InsercaoIdempotente.inserir(() -> 1));
    assertFalse(InsercaoIdempotente.inserir(() -> 0));
  }

  @Test
  void violacaoDeChaveNaCorridaContaComoDuplicata() {
    assertFalse(InsercaoIdempotente.inserir(() -> {
      throw new DuplicateKeyException("Violation of PRIMARY KEY constraint");
    }));
  }

  @Test
  void vitimaDeDeadlockEhRepetidaAteConseguir() {
    var chamadas = new AtomicInteger();

    assertTrue(InsercaoIdempotente.inserir(() -> {
      if (chamadas.incrementAndGet() < 3) throw new CannotAcquireLockException("deadlock 1205");
      return 1;
    }));
    assertEquals(3, chamadas.get());
  }

  @Test
  void desisteDepoisDeTresDeadlocks() {
    var chamadas = new AtomicInteger();

    assertThrows(CannotAcquireLockException.class, () -> InsercaoIdempotente.inserir(() -> {
      chamadas.incrementAndGet();
      throw new CannotAcquireLockException("deadlock 1205");
    }));
    assertEquals(3, chamadas.get());
  }
}
