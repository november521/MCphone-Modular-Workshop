// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

/** Tick-based only: never catches up from wall time after login. */
public final class ProcessingTimer {
    private ProcessingTimer() {}
    public static int cooking(int progress, boolean burning, boolean canCook) {
        return burning ? (canCook ? progress + 1 : 0) : Math.max(0, progress - 2);
    }
    public static boolean accepts(int existing, int produced, int capacity) {
        return produced > 0 && existing >= 0 && produced <= capacity && existing <= capacity - produced;
    }
}
