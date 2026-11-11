package br.com.nexfleet.indicadores.persistencia;

import java.util.function.IntSupplier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;

/**
 * Executa um INSERT idempotente no SQL Server: chave já existente conta como duplicata e
 * vítima de deadlock (erro 1205) é repetida até {@value #TENTATIVAS} vezes.
 */
public final class InsercaoIdempotente {
  static final int TENTATIVAS = 3;

  private InsercaoIdempotente() {}

  /** Retorna {@code true} se a linha foi inserida, {@code false} se já existia. */
  public static boolean inserir(IntSupplier insercao) {
    for (int tentativa = 1; ; tentativa++) {
      try {
        return insercao.getAsInt() == 1;
      } catch (DuplicateKeyException duplicata) {
        return false;
      } catch (ConcurrencyFailureException concorrencia) {
        if (tentativa >= TENTATIVAS) throw concorrencia;
      }
    }
  }
}
