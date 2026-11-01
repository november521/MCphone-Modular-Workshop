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
        System.out.println("Portable menu checks passed: scoped anvil world effects and independent station identities.");
    }
}
