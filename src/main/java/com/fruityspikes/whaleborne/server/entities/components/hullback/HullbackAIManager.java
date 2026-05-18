package com.fruityspikes.whaleborne.server.entities.components.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.goals.hullback.*;
import net.minecraft.world.entity.ai.goal.FollowBoatGoal;

/** AI goal registration and management for the Hullback. */
public class HullbackAIManager {
    private final HullbackEntity hullback;

    public HullbackAIManager(HullbackEntity hullback) {
        this.hullback = hullback;
    }

    /** Registers all AI goals; called during entity initialization. */
    public void registerGoals() {
        // Priority 0: Critical survival and interaction goals
        hullback.goalSelector.addGoal(0, new HullbackBreathAirGoal(hullback));
        hullback.goalSelector.addGoal(0, new HullbackApproachPlayerGoal(hullback, 1.0f));
        hullback.goalSelector.addGoal(0, new HullbackArmorPlayerGoal(hullback, 0.6f));

        // Priority 1-2: Water finding and swimming
        hullback.goalSelector.addGoal(1, new HullbackTryFindWaterGoal(hullback, true));
        hullback.goalSelector.addGoal(2, new HullbackTryFindWaterGoal(hullback, false));
        hullback.goalSelector.addGoal(2, new HullbackRandomSwimGoal(hullback, 1.0, 10));

        // Priority 3: Follow boats
        hullback.goalSelector.addGoal(3, new FollowBoatGoal(hullback));
    }

    /** Clears all goals from the Hullback. */
    public void clearGoals() {
        hullback.goalSelector.getAvailableGoals().clear();
        hullback.targetSelector.getAvailableGoals().clear();
    }

    /** Removes a specific goal by class type. */
    public void removeGoal(Class<?> goalClass) {
        hullback.goalSelector.getAvailableGoals().removeIf(
            wrappedGoal -> goalClass.isInstance(wrappedGoal.getGoal())
        );
    }

    /** Whether a specific goal class is registered. */
    public boolean hasGoal(Class<?> goalClass) {
        return hullback.goalSelector.getAvailableGoals().stream()
            .anyMatch(wrappedGoal -> goalClass.isInstance(wrappedGoal.getGoal()));
    }

    /** Whether the Hullback is currently breaching/breathing. */
    public boolean isBreaching() {
        for (net.minecraft.world.entity.ai.goal.WrappedGoal goal : hullback.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof HullbackBreathAirGoal breachGoal) {
                if (breachGoal.isBreaching()) return true;
            }
        }
        return false;
    }
}
