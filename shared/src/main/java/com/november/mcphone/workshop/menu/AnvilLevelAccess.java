// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.menu;

import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

/** Suppresses world effects only during result pickup; native close/return still uses the real context. */
final class AnvilLevelAccess implements ContainerLevelAccess {
    private final ContainerLevelAccess delegate;
    private boolean suppress;
    private boolean suppressedAction;
    AnvilLevelAccess(ContainerLevelAccess delegate) { this.delegate = delegate; }
    @Override public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> action) {
        if (suppress) {
            suppressedAction = true;
            return Optional.empty();
        }
        return delegate.evaluate(action);
    }
    boolean withoutWorldEffects(Runnable action) {
        boolean previousSuppress = suppress;
        boolean previousAction = suppressedAction;
        suppress = true;
        suppressedAction = false;
        try {
            action.run();
            return suppressedAction;
        } finally {
            boolean didSuppress = suppressedAction;
            suppress = previousSuppress;
            suppressedAction = previousAction || (previousSuppress && didSuppress);
        }
    }
}
