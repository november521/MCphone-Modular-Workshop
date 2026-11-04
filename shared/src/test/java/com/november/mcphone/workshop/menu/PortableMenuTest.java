// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.menu;

import com.november.mcphone.workshop.workstation.Workstation;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

/** Runs against each target's actual ContainerLevelAccess; no game bootstrap or test framework required. */
public final class PortableMenuTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        int[] callbacks = {0};
        ContainerLevelAccess delegate = new ContainerLevelAccess() {
            @Override public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> action) {
                callbacks[0]++;
                return Optional.of(action.apply(null, null));
            }
        };
        AnvilLevelAccess access = new AnvilLevelAccess(delegate);
        check(access.evaluate((level, pos) -> 7, 0) == 7, "Normal access must delegate");
        int before = callbacks[0];
        check(access.withoutWorldEffects(() -> access.execute((level, pos) -> {
            throw new AssertionError("Result pickup must never invoke world mutation");
        })), "Pickup world effect must be reported for sound");
        check(callbacks[0] == before, "Pickup must not call the real level context");
        access.execute((level, pos) -> {});
        check(callbacks[0] == before + 1, "Close/return context must be restored after pickup");
        check(!access.withoutWorldEffects(() -> {}), "Canceled native take must not play a success sound");
        try {
            access.withoutWorldEffects(() -> { throw new IllegalStateException("test"); });
            throw new AssertionError("Expected native failure to propagate");
        } catch (IllegalStateException expected) {
            check("test".equals(expected.getMessage()), "Unexpected exception");
        }
        check(access.evaluate((level, pos) -> 9, 0) == 9, "Failure must not disable later cleanup");
        before = callbacks[0];
        check(access.withoutWorldEffects(() -> {
            check(access.withoutWorldEffects(() -> access.execute((level, pos) -> {
                throw new AssertionError("Nested world effect must not run");
            })), "Inner suppression must report its action");
            access.execute((level, pos) -> { throw new AssertionError("Outer guard must remain active"); });
        }), "Outer suppression must retain nested actions");
        check(callbacks[0] == before, "Nested scopes must not reach the real context");
        check(access.evaluate((level, pos) -> 11, 0) == 11, "Nested scopes must restore cleanup");

        var appIds = new HashSet<String>();
        var receipts = new HashSet<String>();
        for (Workstation station : Workstation.values()) {
            check(Workstation.fromNetworkId(station.networkId()) == station, "Request must map to its own station");
            check(appIds.add(station.appId()), "Apps must have independent purchase IDs");
            check(receipts.add(station.receiptId()), "Purchases must have independent authorization receipts");
        }
        check(Workstation.CRAFTING.networkId() == 0, "Existing crafting selector must remain stable");
        check(Workstation.CRAFTING.receiptId().equals("mcphone_workshop:crafting_purchase"),
                "Upgrading must preserve existing crafting ownership");
        for (int id : new int[] {-1, 4, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            check(Workstation.fromNetworkId(id) == null, "Invalid station selectors must be rejected");
        }
        // Reproduce a paid App with a missing Workshop receipt, without trusting client installation.
        var paid = new HashSet<String>();
        var recovered = new HashSet<String>();
        paid.add(Workstation.CRAFTING.appId());
        for (Workstation station : Workstation.values()) {
            com.november.mcphone.workshop.compat.PurchaseRecovery.recover(
                    () -> recovered.contains(station.appId()), () -> true,
                    () -> paid.contains(station.appId()), () -> recovered.add(station.appId()));
        }
        check(recovered.equals(paid), "Recover only the station recorded as paid on the server");
        com.november.mcphone.workshop.compat.PurchaseRecovery.recover(
                () -> true, () -> { throw new AssertionError("Existing receipt must skip migration"); },
                () -> false, () -> { throw new AssertionError("Must not re-grant existing receipt"); });
        com.november.mcphone.workshop.compat.PurchaseRecovery.recover(
                () -> false, () -> false,
                () -> { throw new AssertionError("Missing advancement must skip host lookup"); },
                () -> { throw new AssertionError("Cannot grant a missing advancement"); });
        // Validate bridge method signatures against the actual MCphone dependency on every target.
        try {
            Class<?> data = Class.forName("com.november.mcphone.core.PhonePlayerData", false,
                    PortableMenuTest.class.getClassLoader());
            var view = data.getMethod("of", net.minecraft.world.entity.player.Player.class);
            var purchases = view.getReturnType().getMethod("purchasedApps");
            Class<?> identifier;
            try { identifier = Class.forName("net.minecraft.resources.ResourceLocation"); }
            catch (ClassNotFoundException renamed) { identifier = Class.forName("net.minecraft.resources.Identifier"); }
            check(purchases.getReturnType().getMethod("has", identifier).getReturnType() == boolean.class,
                    "Host paid lookup must return an explicit boolean");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Legacy purchase bridge does not match this target", error);
        }
        System.out.println("Portable menu checks passed: anvil effects, station identities, paid-only recovery and host lookup signatures.");
    }
}
