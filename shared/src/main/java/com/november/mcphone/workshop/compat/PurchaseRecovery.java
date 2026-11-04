// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.compat;

import java.util.function.BooleanSupplier;

/** A missing receipt alone never grants access; migration requires an explicit server purchase. */
public final class PurchaseRecovery {
    private PurchaseRecovery() {}
    public static void recover(BooleanSupplier hasReceipt, BooleanSupplier receiptAvailable,
                               BooleanSupplier purchasedOnServer, Runnable grant) {
        if (!hasReceipt.getAsBoolean() && receiptAvailable.getAsBoolean() && purchasedOnServer.getAsBoolean()) {
            grant.run();
        }
    }
}
