package dev.ed.edhub.internal;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class EdProtectBridge {
    private EdProtectBridge() {
    }

    public static boolean guard(Class<?> anchor, Logger logger) {
        try {
            Class<?> rt = Class.forName("dev.ed.edprotect.core.RuntimeProtector");
            Method guard = rt.getMethod("guard", Class.class, Consumer.class, Consumer.class);
            Object res = guard.invoke(null, anchor,
                    (Consumer<String>) logger::warning,
                    (Consumer<String>) logger::severe);
            return "OK".equals(String.valueOf(res));
        } catch (ClassNotFoundException ignored) {
            logger.warning("EDProtect не найден: runtime-защита пропущена.");
            return true;
        } catch (Throwable t) {
            logger.severe("EDProtect guard failed: " + t.getMessage());
            return false;
        }
    }
}
