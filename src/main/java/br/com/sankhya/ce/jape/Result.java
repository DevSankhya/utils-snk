package br.com.sankhya.ce.jape;

public abstract class Result<T> {

    private Result() {
    }

    /* ---------- Subtipos ---------- */

    public static final class Ok<T> extends Result<T> {
        private final T data;

        public Ok(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }
    }

    public static final class Error<T> extends Result<T> {
        private final Exception exception;

        public Error(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }
    }

    /* ---------- Operações ---------- */

    public <R> Result<R> map(Mapper<T, R> mapper) {
        if (this instanceof Ok) {
            Ok<T> ok = (Ok<T>) this;
            return new Ok<>(mapper.apply(ok.data));
        }
        Error<T> error = (Error<T>) this;
        return new Error<>(error.exception);
    }

    public T getOrNull() {
        if (this instanceof Ok) {
            return ((Ok<T>) this).data;
        }
        return null;
    }

    public T getOrDefault(T defaultValue) {
        if (this instanceof Ok) {
            T data = ((Ok<T>) this).data;
            return data != null ? data : defaultValue;
        }
        return defaultValue;
    }

    public T getOrThrow() throws Exception {
        if (this instanceof Ok) {
            T data = ((Ok<T>) this).data;
            if (data == null) {
                throw new NullPointerException("Resultado nulo");
            }
            return data;
        }
        throw ((Error<T>) this).exception;
    }

    public Exception getExceptionOrNull() {
        if (this instanceof Error) {
            return ((Error<T>) this).exception;
        }
        return null;
    }

    public boolean isSuccess() {
        return this instanceof Ok;
    }

    public boolean isError() {
        return this instanceof Error;
    }

    /* ---------- Helpers ---------- */

    public Result<T> onSuccess(Consumer<T> action) {
        if (this instanceof Ok) {
            action.accept(((Ok<T>) this).data);
        }
        return this;
    }

    public Result<T> onError(Consumer<Exception> action) {
        if (this instanceof Error) {
            action.accept(((Error<T>) this).exception);
        }
        return this;
    }

    /* ---------- Functional interfaces ---------- */

    @FunctionalInterface
    public interface Mapper<T, R> {
        R apply(T value);
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T value);
    }
}
