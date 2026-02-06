package br.com.sankhya.ce.jape;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
