package br.com.sankhya.ce.jape;

public abstract class Result<T> {

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

    public interface Mapper<T, R> {
        R map(T value);
    }

    public interface SuccessConsumer<T> {
        void accept(T value);
    }

    public interface ErrorConsumer {
        void accept(Exception exception);
    }

    public <R> Result<R> map(Mapper<T, R> mapper) {

        if (this instanceof Ok) {
            Ok<T> ok = (Ok<T>) this;
            return new Ok<>(mapper.map(ok.getData()));
        }

        Error<T> error = (Error<T>) this;

        return new Error<>(error.getException());
    }

    public T getOrNull() {

        if (this instanceof Ok) {
            return ((Ok<T>) this).getData();
        }

        return null;
    }

    public T getOrDefault(T defaultValue) {

        if (this instanceof Ok) {

            T value = ((Ok<T>) this).getData();

            return value != null ? value : defaultValue;
        }

        return defaultValue;
    }

    public T getOrThrow() throws Exception {

        if (this instanceof Ok) {
            return ((Ok<T>) this).getData();
        }

        throw ((Error<T>) this).getException();
    }

    public Exception getExceptionOrNull() {

        if (this instanceof Error) {
            return ((Error<T>) this).getException();
        }

        return null;
    }

    public boolean isSuccess() {
        return this instanceof Ok;
    }

    public boolean isError() {
        return this instanceof Error;
    }

    public Result<T> onSuccess(SuccessConsumer<T> action) {

        if (this instanceof Ok) {
            action.accept(((Ok<T>) this).getData());
        }

        return this;
    }

    public Result<T> onError(ErrorConsumer action) {

        if (this instanceof Error) {
            action.accept(((Error<T>) this).getException());
        }

        return this;
    }

    public static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    public static <T> Result<T> error(Exception exception) {
        return new Error<>(exception);
    }
}
