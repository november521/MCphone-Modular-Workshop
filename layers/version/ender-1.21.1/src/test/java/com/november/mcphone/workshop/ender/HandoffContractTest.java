package com.november.mcphone.workshop.ender;
import com.november.mcphone.api.server.PortableAppAccess;
public final class HandoffContractTest {
    private static void check(boolean v) { if (!v) throw new AssertionError(); }
    public static void main(String[] args) {
        check(!EnderChestAccess.active());
        PortableAppAccess.owner = true;
        check(EnderChestAccess.active());
        check(!EnderChestAccess.purchased(null));
        PortableAppAccess.paid = true;
        check(EnderChestAccess.purchased(null));
        check(!EnderChestAccess.device(null, null));
        PortableAppAccess.device = true;
        check(EnderChestAccess.device(null, null));
        PortableAppAccess.failure = true;
        try { EnderChestAccess.purchased(null); throw new AssertionError("Failed lookup must not become unpaid"); }
        catch (IllegalStateException expected) {}
        PortableAppAccess.owner = false;
        check(!EnderChestAccess.active());
        System.out.println("Proposed handoff contract checks passed (test fixture, not host integration).");
    }
}
