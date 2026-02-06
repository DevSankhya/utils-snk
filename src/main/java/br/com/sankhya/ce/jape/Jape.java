package br.com.sankhya.ce.jape;

import br.com.sankhya.jape.core.JapeSession;

public final class Jape {

    private Jape() {
        // util class
    }

    public static <T> Result<T> secureTransaction(ThrowingSupplier<T> block) {
        try {
            T result;
            if (hasActiveTransaction()) {
                result = execEnsuringTXWithResult(block);
            } else {
                result = openAndExecute(block);
            }
            return new Result.Ok<>(result);
        } catch (Exception e) {
            return new Result.Error<>(e);
        }
    }

    private static <T> T execEnsuringTXWithResult(ThrowingSupplier<T> block) throws Exception {
        final Holder<T> holder = new Holder<>();

        JapeSession.execEnsuringTX(() -> {
            try {
                holder.value = block.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        if (holder.value == null) {
            throw new IllegalStateException("Bloco executado sem retorno");
        }
        return holder.value;
    }

    private static <T> T openAndExecute(ThrowingSupplier<T> block) throws Exception {
        JapeSession.SessionHandle hnd = JapeSession.open();
        try {
            hnd.setCanTimeout(false);

            final Holder<T> holder = new Holder<>();

            hnd.execWithTX(() -> {
                try {
                    holder.value = block.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (holder.value == null) {
                throw new IllegalStateException("Resultado nulo inesperado");
            }

            return holder.value;
        } finally {
            JapeSession.close(hnd);
        }
    }

    private static boolean hasActiveTransaction() {
        try {
            return JapeSession.getCurrentSession().hasTransaction();
        } catch (Exception e) {
            return false;
        }
    }

    /** Simples holder mutável (equivalente ao var em Kotlin) */
    private static final class Holder<T> {
        T value;
    }
}
