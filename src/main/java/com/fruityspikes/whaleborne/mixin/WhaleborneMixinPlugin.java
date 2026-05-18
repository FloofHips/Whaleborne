package com.fruityspikes.whaleborne.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class WhaleborneMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("com.fruityspikes.whaleborne.mixin.NeatCompatMixin")) {
            // Check if class exists without loading it to avoid MixinTargetAlreadyLoadedException
            return this.getClass().getClassLoader().getResource("vazkii/neat/HealthBarRenderer.class") != null;
        }
        if (mixinClassName.equals("com.fruityspikes.whaleborne.mixin.HealthBarsCompatMixin")) {
             return this.getClass().getClassLoader().getResource("fuzs/healthbars/client/handler/InLevelRenderingHandler.class") != null;
        }
        if (mixinClassName.equals("com.fruityspikes.whaleborne.mixin.HealthBarsVisibilityMixin")) {
            return this.getClass().getClassLoader().getResource("fuzs/healthbars/client/helper/EntityVisibilityHelper.class") != null;
        }
        if (mixinClassName.equals("com.fruityspikes.whaleborne.mixin.WakesEffectRuleMixin")) {
            return this.getClass().getClassLoader().getResource("com/leclowndu93150/wakes/utils/WakesUtils.class") != null;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
