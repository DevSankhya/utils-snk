package br.com.sankhya.ce.jape;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

public final class Jape {

    private Jape() {
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static <T> Result<T> secureTransaction(ThrowingSupplier<T> block) {
        try {
            T result;

            if (hasActiveTransaction()) {
                result = execEnsuringTXWithResult(block);
            } else {
                result = openAndExecute(true, block);
            }

            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static <T> Result<T> withSession(ThrowingSupplier<T> block) {
        try {
            T result;

            if (hasActiveSession()) {
                result = block.get();
            } else {
                result = openAndExecute(false, block);
            }

            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(e);
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

    private static <T> T openAndExecute(boolean runOnTransaction, ThrowingSupplier<T> block) throws Exception {
        JapeSession.SessionHandle hnd = JapeSession.open();

        try {
            final Holder<T> holder = new Holder<>();

            hnd.setCanTimeout(false);

            if (!runOnTransaction) {
                setSessionProperties();

                holder.value = block.get();
                return holder.value;
            }

            hnd.execWithTX(() -> {
                try {
                    holder.value = block.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

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

    private static boolean hasActiveSession() {
        try {
            return JapeSession.getCurrentSession() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setSessionProperties() {
        AuthenticationInfo auth = new AuthenticationInfo("SUP", BigDecimal.ZERO, BigDecimal.ZERO, 1);

        auth.makeCurrent();

        ServiceContext sctx = new ServiceContext((HttpServletRequest) null);

        sctx.setAutentication(AuthenticationInfo.getCurrent());

        Element bodyElem = new Element("serviceRequest");
        Element requestBody = new Element("requestBody");

        bodyElem.addContent(requestBody);

        sctx.setRequestBody(bodyElem);

        sctx.makeCurrent();

        try {
            SPBeanUtils.setupContext(sctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Holder<T> {
        T value;
    }
}
