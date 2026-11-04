// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.compat;

import com.november.mcphone.api.MCphoneApi;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.LoggerFactory;

/** Read-only migration bridge until MCphone exposes server purchase lookup in its public API. */
public final class LegacyPurchaseReader {
    private static final AtomicBoolean warned = new AtomicBoolean();
    private static volatile Lookup lookup;
    private record Lookup(Method view, Method purchases, Method has) {}
    private LegacyPurchaseReader() {}
    public static boolean hasPurchased(ServerPlayer player, Object appId) {
        try {
            Lookup methods = lookup;
            if (methods == null) {
                Class<?> data = Class.forName("com.november.mcphone.core.PhonePlayerData", false,
                        MCphoneApi.class.getClassLoader());
                Method view = data.getMethod("of", Player.class);
                Method purchases = view.getReturnType().getMethod("purchasedApps");
                methods = new Lookup(view, purchases, purchases.getReturnType().getMethod("has", appId.getClass()));
                lookup = methods;
            }
            Object view = methods.view().invoke(null, player);
            Object purchases = methods.purchases().invoke(view);
            // Read the explicit paid set, never a client install flag or a "free App" access fallback.
            return Boolean.TRUE.equals(methods.has().invoke(purchases, appId));
        } catch (ReflectiveOperationException | LinkageError | SecurityException error) {
            if (warned.compareAndSet(false, true)) {
                LoggerFactory.getLogger("mcphone_workshop").warn(
                        "Cannot read legacy MCphone purchases; existing Workshop receipts remain authoritative.", error);
            }
            return false;
        }
    }
}
