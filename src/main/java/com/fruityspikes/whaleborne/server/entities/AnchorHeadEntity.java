package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AnchorHeadEntity extends Entity {
    protected Vec3 wantedPos;
    public AnchorHeadEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.wantedPos = this.position();
    }
    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        this.kill();
        return super.interact(player, hand);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    public void setWantedPos(Vec3 wantedPos) {
        this.wantedPos = wantedPos;
    }

    public Vec3 getWantedPos() {
        return wantedPos;
    }

    @Override
    public void tick() {
        super.tick();
        //if(this.wantedPos.distanceTo(this.position()) > 0.1)
        //    this.moveTo(new Vec3(Mth.lerp(1, this.position().x, wantedPos.x), Mth.lerp(1, this.position().y, wantedPos.y), Mth.lerp(1, this.position().z, wantedPos.z)));
        //this.moveTo(new Vec3(wantedPos.x, wantedPos.y, wantedPos.z));
    }
}
