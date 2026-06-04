package br.com.sankhya.ce.sql.interfaces;

/**
 * Interface funcional que permite exceptions checked.
 *
 * @param <T> Tipo de entrada.
 * @param <R> Tipo de retorno.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;
}
