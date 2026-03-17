package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.server.entities.goals.hullback.HullbackBreathAirGoal;
import com.fruityspikes.whaleborne.client.menus.HullbackMenu;
import com.fruityspikes.whaleborne.network.HullbackHurtPayload;
import com.fruityspikes.whaleborne.server.data.HullbackDirtManager;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackAIManager;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackEquipmentManager;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackInteractionManager;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackPartManager;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackDirt;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackSeatManager;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackControlManager;
import com.fruityspikes.whaleborne.server.registries.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class HullbackEntity extends AbstractWhale implements HasCustomInventoryScreen, PlayerRideableJumping, Saddleable {

    // ─── Constants ───────────────────────────────────────────────
    private static final float HEALING_AMOUNT = 0.25f;
    private static final int HEALING_INTERVAL_TICKS = 80;
    private static final int SEAT_VALIDATION_INTERVAL_TICKS = 20;
    private static final int ARMOR_SYNC_INTERVAL_TICKS = 40;
    private static final int MOUTH_CLOSE_INTERVAL_TICKS = 80;
    private static final float MOUTH_OPEN_SPEED = 0.8f;
    private static final float SPEED_THRESHOLD_MOUTH_OPEN = 0.3f;
    private static final float ARMOR_EJECT_THRESHOLD = 0.45f;
    private static final float PLATFORM_HEIGHT_HELM = 4.5F; 
    private static final float PLATFORM_HEIGHT_STATIONARY = 4.5F;
    private static final float PLATFORM_HEIGHT_LERP_SPEED = 0.1F;
    // DIRT_TICK_CHANCE / DIRT_TICK_DENOMINATOR removed — random gate now lives
    // inside HullbackDirt.randomTick(), matching the original 1.20.1 structure.
    private static final int POST_LOAD_VALIDATION_DELAY_TICKS = 5;
    private static final int STATIONARY_TICKS_PLAYER_ABOVE = 60;
    private static final int STATIONARY_TICKS_DISMOUNT = 200;
    private static final int STATIONARY_TICKS_INITIAL = 60;
    private static final int STATIONARY_MINIMUM_THRESHOLD = 30;
    private static final int PLAYER_ABOVE_COOLDOWN_TICKS = 20;
    private static final int DIRT_INITIAL_SYNC_TICK = 10;
    private static final int EARLY_TICK_THRESHOLD = 20;
    private static final double SPEED_THRESHOLD_MOUTH_OPEN_SQR = SPEED_THRESHOLD_MOUTH_OPEN * SPEED_THRESHOLD_MOUTH_OPEN;
    private static final double PARTICLE_SPEED_THRESHOLD_SQR = 0.03 * 0.03;

    // ─── Buoyancy / Depth Constants (see Config for runtime values) ───
    private static final double BUOYANCY_DEADZONE = 0.05;
    private static final double BUOYANCY_FORCE_FACTOR = 0.2;
    private static final double BUOYANCY_FORCE_MAX = 0.1;
    private static final double WILD_BUOYANCY_UP_FACTOR = 0.02;
    private static final double WILD_BUOYANCY_UP_MAX = 0.03;
    private static final double WILD_SINK_FORCE = -0.05;
    private static final float MAX_RIDING_PITCH = 25f;
    private static final double LAND_FRICTION = 0.3;
    private static final double ICE_FRICTION = 0.05;
    private static final double LAND_SPEED_SCALE = 0.1;
    private static final int[] SIDES = {-1, 1};

    // ─── Static fields ───────────────────────────────────────────
    private static final ResourceLocation SAIL_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "sail_speed_modifier");

    // ─── Synched Entity Data ─────────────────────────────────────
    public static final EntityDataAccessor<ItemStack> DATA_CROWN_ID = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> DATA_ARMOR = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> DATA_MOUTH_PROGRESS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_0 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_1 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_2 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_3 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_4 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_5 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_6 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Boolean> DATA_VECTOR_CONTROL = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STATIONARY_TICKS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.INT);

    // ─── Inventory Slots ────────────────────────────────────────
    public static final int INV_SLOT_CROWN = 0;
    public static final int INV_SLOT_SADDLE = 1;
    public static final int INV_SLOT_ARMOR = 2;

    // ─── Sub-Entity Parts ───────────────────────────────────────
    public final HullbackPartEntity head;
    public final HullbackPartEntity nose;
    public final HullbackPartEntity body;
    public final HullbackPartEntity tail;
    public final HullbackPartEntity fluke;
    private final HullbackPartEntity[] subEntities;



    // ─── Component Managers ──────────────────────────────────────
    public final HullbackPartManager partManager;
    public final HullbackDirt hullbackDirt;
    public final HullbackSeatManager hullbackSeatManager;
    public final HullbackAIManager aiManager;
    public final HullbackEquipmentManager equipmentManager;
    public final HullbackInteractionManager interactionManager;
    public final HullbackControlManager controlManager;
    public final HullbackControlManager.HullbackBodyRotationControl bodyControl;

    // ─── State Fields ───────────────────────────────────────────
    private boolean isFreshSpawn = true;
    private boolean validatedAfterLoad = false;
    private float leftEyeYaw, rightEyeYaw, eyePitch;
    private IItemHandler itemHandler;
    private Vec3 currentTarget;
    public int stationaryTicks;
    public boolean HAS_MOBIUS_SPAWNED = false;
    public int ticksSinceSpawn = 0;
    public float AttributeSpeedModifier = 1;
    public float newRotY = this.getYRot();
    private float mouthOpenProgress;
    private float mouthTarget;
    private boolean isBreaching;
    // Smoothed animation speed to avoid per-tick jitter from entity tracking position lerps
    private float smoothedAnimSpeed = 0f;
    private int lastAnimSpeedTick = -1;
    private boolean pitchLocked = false;
    private boolean wasPilotControlled = false;
    private int playerAboveCooldown = 0;  // Cooldown to prevent rapid detection
    private boolean platformsStable = false;
    private boolean isApproachingPlayer = false;
    private float targetPlatformHeight;
    private float currentPlatformHeight = PLATFORM_HEIGHT_STATIONARY;

    @Override
    public void setXRot(float xRot) {
        if (pitchLocked) {
            super.setXRot(0f);
        } else {
            super.setXRot(xRot);
        }
    }

    public HullbackDirt getWhaleDirt() {
        return this.hullbackDirt;
    }

    public boolean isPitchLocked() {
        return pitchLocked;
    }

    public void setBreaching(boolean breaching) {
        this.isBreaching = breaching;
    }

    public boolean isBreaching() {
        return this.isBreaching;
    }

    public void setApproachingPlayer(boolean approaching) {
        this.isApproachingPlayer = approaching;
    }

    public boolean isApproachingPlayer() {
        return this.isApproachingPlayer;
    }

    public static ResourceLocation getSailSpeedModifierId() {
        return SAIL_SPEED_MODIFIER_ID;
    }

    public HullbackEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);


        this.inventory.addListener(this);
        this.itemHandler = new InvWrapper(this.inventory);

        this.moveControl = new HullbackControlManager.StationaryAwareMoveControl(this, 1, 2, 0.1F, 0.1F, true);
        this.lookControl = new HullbackControlManager.StationaryAwareLookControl(this, 1);
        this.bodyControl = (HullbackControlManager.HullbackBodyRotationControl) createBodyControl();


        this.nose = new HullbackPartEntity(this, "nose", 5.0F, 5.0F);
        this.head = new HullbackPartEntity(this, "head", 5.0F, 5.0F);
        this.body = new HullbackPartEntity(this, "body", 5.0F, 5.0F);
        this.tail = new HullbackPartEntity(this, "tail", 2.5F, 2.5F);
        this.fluke = new HullbackPartEntity(this, "fluke", 4.0F, 0.6F);



        this.subEntities = new HullbackPartEntity[]{this.nose, this.head, this.body, this.tail, this.fluke};

        this.setId(ENTITY_COUNTER.getAndAdd(this.subEntities.length + 1) + 1);

        this.partManager = new HullbackPartManager(this, this.subEntities);
        this.partManager.init();

        this.hullbackDirt = new HullbackDirt(this);
        this.hullbackDirt.init();

        this.hullbackSeatManager = new HullbackSeatManager(this, 
            DATA_SEAT_0, DATA_SEAT_1, DATA_SEAT_2, DATA_SEAT_3, 
            DATA_SEAT_4, DATA_SEAT_5, DATA_SEAT_6);

        this.aiManager = new HullbackAIManager(this);
        this.equipmentManager = new HullbackEquipmentManager(this);
        this.interactionManager = new HullbackInteractionManager(this);
        this.controlManager = new HullbackControlManager(this);
        this.moveControl = new HullbackControlManager.HullbackMoveControl(this);
        
        // Manual registration since registerGoals() is called by super() before aiManager is init
        this.aiManager.registerGoals();

        this.mouthOpenProgress = 0.0f;
        this.currentTarget = this.position();
        
        // Manual inventory update since the one in super() was skipped due to null manager
        this.updateContainerEquipment();

        this.stationaryTicks = STATIONARY_TICKS_INITIAL;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnReason) {
        return checkHullbackSpawnRules((EntityType<HullbackEntity>) this.getType(), level, spawnReason, this.blockPosition(), level.getRandom());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        if (spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            this.setPersistenceRequired();
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    public static boolean checkHullbackSpawnRules(EntityType<HullbackEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        if (!hasSpawnSpace(level, pos)) {
            return false;
        }

        if (hasReachedSpawnCap(level, pos)) {
            return false;
        }

        double spawnChance;
        try {
            spawnChance = Config.HULLBACK_SPAWN_CHANCE.get();
        } catch (Exception e) {
            spawnChance = 0.001;
        }

        if (random.nextDouble() >= spawnChance) {
            return false;
        }

        return true;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    private static boolean hasSpawnSpace(LevelAccessor level, BlockPos pos) {
        // Reduced check area to allow more spawns
        int checkRadius = 5;   // Reduced from 10 to 5
        int heightCheck = 4;   // Reduced from 6 to 4

        AABB spawnCheckArea = new AABB(pos).inflate(checkRadius, heightCheck, checkRadius);

        int solidCount = 0;
        int maxSolidAllowed = 20;  // Allow up to 20 solid blocks (tolerance)

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = (int)spawnCheckArea.minX; x <= spawnCheckArea.maxX; x++) {
            for (int y = (int)spawnCheckArea.minY; y <= spawnCheckArea.maxY; y++) {
                for (int z = (int)spawnCheckArea.minZ; z <= spawnCheckArea.maxZ; z++) {
                    mutablePos.set(x, y, z);
                    BlockState state = level.getBlockState(mutablePos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) continue;

                    if (state.isSolid() && state.getCollisionShape(level, mutablePos) != Shapes.empty()) {
                        solidCount++;
                        if (solidCount > maxSolidAllowed) {
                            return false;  // Too obstructed
                        }
                    }
                }
            }
        }

        return true;  // Spawn allowed
    }

    private static boolean hasReachedSpawnCap(LevelAccessor level, BlockPos pos) {
        int spawnCap;
        try {
            spawnCap = Config.HULLBACK_SPAWN_CAP.get();
        } catch (Exception e) {
            spawnCap = 1;
        }

        // We define the search radius. 64 is a good balance between performance and safety.
        int radius = 64; 
        
        BlockPos minPos = pos.offset(-radius, -32, -radius);
        BlockPos maxPos = pos.offset(radius, 32, radius);

        // SMART LOAD CHECK:
        // If not all chunks containing our bounding box are loaded, the 'getEntitiesOfClass' method
        // would be inaccurate. Because of this, we abort the spawning attempt returning 'true' (cap reached).
        if (!level.hasChunksAt(minPos, maxPos)) {
            return true; 
        }

        // Now that we know that the area is safe and 100% visible to the code, we proceed counting.
        AABB checkArea = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
        
        int nearbyCount = level.getEntitiesOfClass(
                HullbackEntity.class,
                checkArea,
                e -> e.isAlive() && !e.isTamed()
        ).size();

        return nearbyCount >= spawnCap;
    }

    @Override
    public void checkDespawn() {
        // Custom checkDespawn: only resets noActionTime when a player is within simulation distance.
        // Distance-based despawn is fully disabled — lifespan is controlled by our tick() timer.
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            int simDistBlocks = serverLevel.getServer().getPlayerList().getSimulationDistance() * 16;
            double simDistSq = (double) simDistBlocks * simDistBlocks;
            net.minecraft.world.entity.player.Player nearest = this.level().getNearestPlayer(this, -1.0);
            if (nearest != null && nearest.distanceToSqr(this) < simDistSq) {
                this.setNoActionTime(0);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.005)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(NeoForgeMod.SWIM_SPEED, 1.2);
    }

    protected int getInventorySize() {
        return 3;
    }
    protected int getSaddleSlot() {
        return INV_SLOT_SADDLE;
    }
    // Inventory created in AbstractWhale
    public void updateContainerEquipment() {
        if (equipmentManager != null) {
            equipmentManager.updateContainerEquipment();
        }
    }
    public void containerChanged(Container invBasic) {
        if (equipmentManager != null) {
            equipmentManager.containerChanged(invBasic);
        }
    }

    @Override
    public void onInsideBubbleColumn(boolean downwards) { }

    public ItemStack getArmor() {
        return equipmentManager.getArmor();
    }
    public ItemStack getCrown() {
        return equipmentManager.getCrown();
    }


    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        equipmentManager.equipSaddle(stack, source);
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(DATA_SEAT_0, Optional.empty());
        builder.define(DATA_SEAT_1, Optional.empty());
        builder.define(DATA_SEAT_2, Optional.empty());
        builder.define(DATA_SEAT_3, Optional.empty());
        builder.define(DATA_SEAT_4, Optional.empty());
        builder.define(DATA_SEAT_5, Optional.empty());
        builder.define(DATA_SEAT_6, Optional.empty());

        builder.define(DATA_CROWN_ID, ItemStack.EMPTY);
        builder.define(DATA_ARMOR, ItemStack.EMPTY);
        builder.define(DATA_MOUTH_PROGRESS, 0f);
        builder.define(DATA_VECTOR_CONTROL, false);
        builder.define(DATA_STATIONARY_TICKS, 0);
    }

    public int getStationaryTicks() {
        return this.entityData.get(DATA_STATIONARY_TICKS);
    }
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ListTag listtag = new ListTag();

        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                Tag savedTag = itemstack.save(this.registryAccess());
                if (savedTag instanceof CompoundTag itemTag) {
                    itemTag.putByte("Slot", (byte) i);
                    listtag.add(itemTag);
                }
            }
        }
        compound.put("Items", listtag);


        compound.putBoolean("HasMobiusSpawned", HAS_MOBIUS_SPAWNED);
        compound.putInt("TicksSinceSpawn", ticksSinceSpawn);

        for (int i = 0; i < 7; i++) {
            Optional<UUID> occupant = this.entityData.get(getSeatAccessor(i));
            String seatKey = "Seat_" + i;

            if (occupant.isPresent()) {
                compound.putUUID(seatKey, occupant.get());
            }
        }
        this.hullbackDirt.addAdditionalSaveData(compound);
    }
    public void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    this.isFreshSpawn = false;

        ListTag items = compound.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < this.inventory.getContainerSize()) {
                ItemStack stack = ItemStack.parse(this.registryAccess(), itemTag).orElse(ItemStack.EMPTY);
                this.inventory.setItem(slot, stack);
            }
        }

        HAS_MOBIUS_SPAWNED = compound.getBoolean("HasMobiusSpawned");
        ticksSinceSpawn = compound.getInt("TicksSinceSpawn");

        for (int i = 0; i < 7; i++) {
            String seatKey = "Seat_" + i;
            if (compound.hasUUID(seatKey)) {
                UUID uuid = compound.getUUID(seatKey);
                this.entityData.set(getSeatAccessor(i), Optional.of(uuid));
            } else {
                this.entityData.set(getSeatAccessor(i), Optional.empty());
            }
        }

        this.hullbackDirt.readAdditionalSaveData(compound);
        this.updateContainerEquipment();
    }

    public EntityDataAccessor<Optional<UUID>> getSeatAccessor(int seatIndex) {
        return hullbackSeatManager.getSeatAccessor(seatIndex);
    }

    public float getArmorProgress() {
        return (float) this.entityData.get(DATA_ARMOR).getCount() / 64f;
    }
    public float getLeftEyeYaw() { return leftEyeYaw; }
    public float getRightEyeYaw() { return rightEyeYaw; }
    public float getEyePitch() { return eyePitch; }
    // Override to suppress default air supply tick — prevents the entity from
    // trying to surface for air, which would fight the custom buoyancy in travel().
    protected void handleAirSupply(int airSupply) { }

    public int getMaxAirSupply() {
        return 10000;
    }
    protected int increaseAirSupply(int currentAir) {
        return this.getMaxAirSupply();
    }

    public boolean isPushable() {
        return false;
    }
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.subEntities.length; i++) {
            this.subEntities[i].setId(id + i + 1);
        }
    }
    public HullbackPartEntity[] getSubEntities() {
        return this.subEntities;
    }
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    public boolean isPickable() {
        return false;
    }

    public HullbackPartManager getPartManager() {
        return this.partManager;
    }



    public boolean isMultipartEntity() {
        return true;
    }

    public HullbackPartEntity[] getParts() {
        return this.subEntities;
    }

    private boolean scanPlayerAbove() {
        // Do not scan if in cooldown
        if (playerAboveCooldown > 0) {
            return false;
        }

        for (HullbackPartEntity part : getSubEntities()) {
            AABB box = part.getBoundingBox();
            
            // MODIFICATION: Height reduced to 0.5 during approach for rigidity
            double heightCheck = this.isApproachingPlayer() ? 0.5 : 3.0;

            AABB topSurface = new AABB(
                    box.minX,
                    box.maxY,
                    box.minZ,
                    box.maxX,
                    box.maxY + heightCheck, // Increased from 2.0 to 3.0
                    box.maxZ
            ).inflate(1.5, 0.0, 1.5); // Increased from 1.0 to 1.5

            List<Entity> entities = level().getEntitiesOfClass(Entity.class, topSurface,
                    entity -> entity instanceof Player &&
                            !entity.isSpectator() &&
                            !entity.isPassenger()
            );

            if (!entities.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void playAmbientSound() {
        if (this.random.nextFloat() < 0.5) return;

        mouthTarget = 0.2f;


        for (int side : SIDES) {
            Vec3 particlePos = partManager.partPosition[1].add(new Vec3(3.5*side, 2, 0).yRot(-partManager.partYRot[1]));
            double x = particlePos.x;
            double y = particlePos.y;
            double z = particlePos.z;

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        WBParticleRegistry.SMOKE.get(),
                        x,
                        y,
                        z,
                        20,
                        0.2,
                        0.2,
                        0.2,
                        0.02
                );
            }
        }

        playSound(WBSoundRegistry.HULLBACK_AMBIENT.get(), 3.0f, 1);
    }

    @Override
    protected void playSwimSound(float volume) {
        super.playSwimSound(2);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return WBSoundRegistry.HULLBACK_SWIM.get();
    }
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return WBSoundRegistry.HULLBACK_HURT.get();
    }
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return WBSoundRegistry.HULLBACK_DEATH.get();
    }
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return WBSoundRegistry.HULLBACK_AMBIENT.get();
    }

    protected void registerGoals() {
        if (aiManager != null) {
            aiManager.registerGoals();
        }
    }

    public void moveEntitiesOnTop(int index) {
        partManager.moveEntitiesOnTop(index, platformsStable);
    }


    private List<HullbackDirtManager.HullbackDirtEntry> getCandidates(boolean bottom, String partName) {
        return interactionManager.getCandidates(bottom, partName);
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return interactionManager.mobInteract(player, hand);
    }

    public InteractionResult interactDebug(Player player, InteractionHand hand){
        return interactionManager.interactDebug(player, hand);
    }
    public InteractionResult interactRide(Player player, InteractionHand hand, int seatIndex, @Nullable EntityType<?> entityType) {
        return interactionManager.interactRide(player, hand, seatIndex, entityType);
    }

    public InteractionResult interactClean(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        return interactionManager.interactClean(player, hand, part, top);
    }

    public InteractionResult interactArmor(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        return interactionManager.interactArmor(player, hand, part, top);
    }
 
    public void setMouthTarget(float target) {
        this.mouthTarget = target;
    }

    @Override
    protected void tickDeath() {
        discardAllPlatforms();
        List<HullbackWalkableEntity> list = level().getEntitiesOfClass(HullbackWalkableEntity.class, this.getBoundingBox().inflate(6));
        for (HullbackWalkableEntity entity : list) entity.discard();

        super.tickDeath();
    }

    public void sendHurtSyncPacket() {
        if (!this.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(this, new HullbackHurtPayload(
                    this.getId(),
                    this.inventory.getItem(INV_SLOT_ARMOR),
                    this.inventory.getItem(INV_SLOT_CROWN),
                    this.entityData.get(DATA_ID_FLAGS)
            ));
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        
        // Initialize part positions immediately
        if (partManager.prevPartPositions[0] == null) {
            for (int i = 0; i < partManager.prevPartPositions.length; i++) {
                partManager.prevPartPositions[i] = position();
                partManager.partPosition[i] = position();
                partManager.oldPartPosition[i] = position();
            }
        }

        // Calculate initial part positions
        partManager.updatePartPositions();

        // Calculate initial seats
        if (isTamed()) {
            partManager.calculateSeats();
        }
        
        // NEW: Sync equipment when adding to world
        if (!this.level().isClientSide) {
            // Ensure inventory is correct
            ItemStack armor = this.inventory.getItem(INV_SLOT_ARMOR);
            ItemStack crown = this.inventory.getItem(INV_SLOT_CROWN);
            ItemStack saddle = this.inventory.getItem(INV_SLOT_SADDLE);
            
            if (!armor.isEmpty()) {
                this.entityData.set(DATA_ARMOR, armor.copy());
            }
            if (!crown.isEmpty()) {
                this.entityData.set(DATA_CROWN_ID, crown.copy());
            }
            
            // Update saddle flag
            this.setFlag(4, !saddle.isEmpty());
            
            // Force synchronization
            this.updateContainerEquipment();
        }
    }

    @Override
    public void tick() {
    // --- POST-SPAWN VALIDATION (LOGIN DESYNC FIX AND MULTIPLAYER-SAFE) ---
    // Only fresh spawned entities will do this check, and only once (at tick 40).
    if (!this.level().isClientSide && this.isFreshSpawn && !this.isPersistenceRequired() && this.tickCount == 40) {
        int spawnCap;
        try {
            spawnCap = Config.HULLBACK_SPAWN_CAP.get();
        } catch (Exception e) {
            spawnCap = 1;
        }

        AABB checkArea = this.getBoundingBox().inflate(128, 64, 128);
        List<HullbackEntity> nearbyHullbacks = this.level().getEntitiesOfClass(
                HullbackEntity.class,
                checkArea,
                e -> e.isAlive() && !e.isTamed()
        );

        int higherPriorityCount = 0;

        for (HullbackEntity other : nearbyHullbacks) {
            if (other == this) continue;

            // Old entities (read from disk) always bear absolute priority over fresh ones.
            if (!other.isFreshSpawn) {
                higherPriorityCount++;
            } 
            // If the other entity is also new (competing), we use the UUID to decide who stays.
            // compareTo < 0 ensures that only one counts the other, creating a mathematically perfect queue.
            else if (other.getUUID().compareTo(this.getUUID()) < 0) {
                higherPriorityCount++;
            }
        }

        // If the number of Hullbacks with higher priority already fills the cap, this entity deletes itself.
        if (higherPriorityCount >= spawnCap) {
            this.discard();
            return;
        }
    }

        if (!this.level().isClientSide && this.isAlive()) {
            // Grace zone: pause despawn timer if any player is within the configured radius
            int graceRadius = com.fruityspikes.whaleborne.Config.hullbackDespawnGraceRadius;
            boolean inGraceZone = false;
            if (graceRadius > 0) {
                double graceDistSq = (double) graceRadius * graceRadius;
                net.minecraft.world.entity.player.Player nearestPlayer = this.level().getNearestPlayer(this, -1.0);
                inGraceZone = nearestPlayer != null && nearestPlayer.distanceToSqr(this) < graceDistSq;
            }
            if (!inGraceZone) {
                ticksSinceSpawn++;
            }
            if (!this.isPersistenceRequired() && !this.isTamed() && !this.hasCustomName()) {
                long maxTicks = (long) com.fruityspikes.whaleborne.Config.hullbackDespawnTimeTicks * com.fruityspikes.whaleborne.Config.hullbackDespawnTimeMultiplier;
                if (ticksSinceSpawn >= maxTicks) {
                    this.discard();
                    return;
                }
            }
        }

        if (this.level().isClientSide && this.getStationaryTicks() > 0) {
             // Preserve Y velocity so gravity still works (whale won't float mid-air after breach)
             this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        if (!this.level().isClientSide && this.tickCount == 20) {
            this.hullbackDirt.syncDirtToClients();
        }

        // 1. Capture rotation snapshot BEFORE AI and movement processing
        RotationSnapshot snapshot = captureRotationSnapshot();

        // 2. Handle core stationary logic and base entity tick
        handleStationaryState();
        updatePlatformHeight();
        super.tick();

        // 3. Re-enforce the stationary orientation if applicable (Hard Lock)
        applyHardLock(snapshot);

        // 4. Handle client-side control prediction and state
        if (this.level().isClientSide) {
            controlManager.clientHandleControlState();
        }

        // 5. Manage equipment validation, synchronization and client restoration
        manageEquipmentAndSync();

        // 6. Update multipart positions, passenger rotation and seat points
        manageMultipartPhysics();

        // 7. Handle various passive behaviors and entity logic
        managePassiveBehaviors();
    }

    /** Helper record to store rotation data for the Hard Lock. */
    private record RotationSnapshot(float yaw, float bodyYaw, float headYaw) {}

    private RotationSnapshot captureRotationSnapshot() {
        return new RotationSnapshot(this.getYRot(), this.yBodyRot, this.yHeadRot);
    }

    private void applyHardLock(RotationSnapshot snapshot) {
        // BREATHING COMPATIBILITY: Do NOT lock pitch if the whale is trying to breach/breathe!
        boolean isBreachingAction = false;
        if (this.aiManager != null) {
            isBreachingAction = this.aiManager.isBreaching();
        }

        if ((this.getStationaryTicks() > 0 || this.hasAnchorDown()) && !isBreachingAction) {
            this.setYRot(snapshot.yaw());
            this.setYBodyRot(snapshot.bodyYaw());
            this.setYHeadRot(snapshot.headYaw());
            this.yRotO = snapshot.yaw();

            // CRITICAL FIX: Lock pitch to prevent vertical tilting when anchored/stationary
            this.pitchLocked = true;
            super.setXRot(0f); // Force immediate 0 to prevent one-tick jitters
            this.xRotO = 0f;
            
            // Set delta movement to zero to stop wagging tail due to swimCycle
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else {
            this.pitchLocked = false;
        }
    }

    private void manageEquipmentAndSync() {
        validateEquipmentAfterLoad();
        syncArmorPeriodically();
        restoreClientEquipment();

        if (this.tickCount % SEAT_VALIDATION_INTERVAL_TICKS == 0) {
            validateAssignments();
        }

        if (this.tickCount == DIRT_INITIAL_SYNC_TICK) {
            hullbackDirt.syncDirtToClients();
        }
    }

    private void manageMultipartPhysics() {
        // Update physical part positions
        partManager.setOldPosAndRots();
        partManager.updatePartPositions();

        // Align passengers with their respective platforms/seats
        hullbackSeatManager.rotatePassengers();

        // Handle head rotation offset when not controlled by AI
        if (this.getStationaryTicks() <= 0) {
            this.yHeadRot = this.yBodyRot + (partManager.partYRot[0] - partManager.partYRot[4]) * 1.5f;
        }

        // Refresh seat coordinates for riding entities
        if (this.isTamed() && partManager.partPosition != null && partManager.partYRot != null && partManager.partXRot != null) {
            partManager.calculateSeats();
        }
    }

    private void managePassiveBehaviors() {
        handlePassengerEjection();
        handleHealing();
        handleMouthAnimation();
        handleDirtTicks();
        updateModifiers();
        tickSubEntitiesAndParticles();
        updatePlatformHeight();
        updateStationaryPlatforms();
    }


    /** One-time validation after loading from NBT to re-sync equipment with clients. */
    private void validateEquipmentAfterLoad() {
        if (!this.level().isClientSide && !validatedAfterLoad && this.tickCount > POST_LOAD_VALIDATION_DELAY_TICKS) {
            validatedAfterLoad = true;

            ItemStack armor = this.inventory.getItem(INV_SLOT_ARMOR);
            ItemStack crown = this.inventory.getItem(INV_SLOT_CROWN);
            ItemStack saddle = this.inventory.getItem(INV_SLOT_SADDLE);

            this.entityData.set(DATA_ARMOR, armor.copy());
            this.entityData.set(DATA_CROWN_ID, crown.copy());
            this.setFlag(4, !saddle.isEmpty());

            sendHurtSyncPacket();
            hullbackDirt.syncDirtToClients();
        }
    }

    /** Periodically syncs armor data to ensure client-server consistency. */
    private void syncArmorPeriodically() {
        if (tickCount % ARMOR_SYNC_INTERVAL_TICKS == 0) {
            ItemStack current = this.inventory.getItem(INV_SLOT_ARMOR);
            if (!ItemStack.matches(this.entityData.get(DATA_ARMOR), current)) {
                this.entityData.set(DATA_ARMOR, current);
            }
        }
    }

    /** On client side, restores inventory from synched entity data if the local inventory is empty. */
    private void restoreClientEquipment() {
        if (this.level().isClientSide) {
            ItemStack armorInInventory = this.inventory.getItem(INV_SLOT_ARMOR);
            ItemStack armorInData = this.entityData.get(DATA_ARMOR);

            if (armorInInventory.isEmpty() && !armorInData.isEmpty()) {
                this.inventory.setItem(INV_SLOT_ARMOR, armorInData.copy());
            }

            ItemStack crownInInventory = this.inventory.getItem(INV_SLOT_CROWN);
            ItemStack crownInData = this.entityData.get(DATA_CROWN_ID);

            if (crownInInventory.isEmpty() && !crownInData.isEmpty()) {
                this.inventory.setItem(INV_SLOT_CROWN, crownInData.copy());
            }

            boolean hasSaddleFlag = this.getFlag(4);
            ItemStack saddleInInventory = this.inventory.getItem(INV_SLOT_SADDLE);

            if (hasSaddleFlag && saddleInInventory.isEmpty()) {
                this.inventory.setItem(INV_SLOT_SADDLE, new ItemStack(Items.SADDLE));
            }
        }

        if (this.level().isClientSide && getArmorProgress() > 0 && this.inventory.getItem(INV_SLOT_ARMOR).getItem().asItem() == Items.AIR.asItem()) {
            this.inventory.setItem(INV_SLOT_ARMOR, getArmor());
        }
    }

    /** Manages walkable platform spawning/removal and player-above detection. */
    private void handleStationaryState() {
        // Maximum priority: If breaching to breathe, skip all stationary logic
        if (this.isBreaching()) return;

        LivingEntity currentPilot = getControllingPassenger();
        
        // Manage pilot -> dismounted transition
        if (currentPilot != null) {
            this.stationaryTicks = 0; // Fix: Immediate acceleration when mounted
            this.wasPilotControlled = true;
            this.platformsStable = false;
            this.playerAboveCooldown = 0;
        } else if (this.wasPilotControlled) {
            // Increase initial time after dismount
            this.stationaryTicks = STATIONARY_TICKS_DISMOUNT;
            this.wasPilotControlled = false;
            this.platformsStable = false;
        }

        // Decrement cooldown
        if (playerAboveCooldown > 0) {
            playerAboveCooldown--;
        }

        // Main logic
        if (stationaryTicks > 0) {
            stopMoving();
            stationaryTicks--;
            
            // Mark platforms as stable after threshold
            if (stationaryTicks < (STATIONARY_TICKS_DISMOUNT - STATIONARY_MINIMUM_THRESHOLD)) {
                platformsStable = true;
            }
        } else {
            platformsStable = false;
        }

        // Player above detection - with cooldown and hysteresis
        // Skip detection while breaching or out of water to prevent stationaryTicks
        // from freezing the whale mid-air after a breach jump
        if (playerAboveCooldown == 0 && !this.isBreaching && this.isInWater()) {
            if (scanPlayerAbove()) {
                // MODIFICATION: Only stabilizes if horizontal speed is negligible
                double speed = this.getDeltaMovement().horizontalDistance();
                if (speed < 0.2 || !this.isApproachingPlayer) {
                    if (stationaryTicks < STATIONARY_TICKS_PLAYER_ABOVE) {
                        stationaryTicks = STATIONARY_TICKS_PLAYER_ABOVE;
                    }
                    playerAboveCooldown = PLAYER_ABOVE_COOLDOWN_TICKS;
                }
            }
        }

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_STATIONARY_TICKS, stationaryTicks);
        }

        // Manage platforms with hysteresis
        if (stationaryTicks == 0 && platformsStable) {
            discardAllPlatforms();
        } else if (stationaryTicks > STATIONARY_MINIMUM_THRESHOLD && !platformsStable) {
            // Wait until truly stable before creating platforms
            if (stationaryTicks < (STATIONARY_TICKS_DISMOUNT - STATIONARY_MINIMUM_THRESHOLD)) {
                platformsStable = true;
            }
        }
    }



    /** Ejects passengers when unsaddled or when armor is too damaged. */
    private void handlePassengerEjection() {
        if (!isSaddled() && !level().isClientSide && tickCount > EARLY_TICK_THRESHOLD) {
            if (!getPassengers().isEmpty()) {
                if (!getInventory().getItem(INV_SLOT_CROWN).isEmpty()) {
                    spawnAtLocation(getInventory().getItem(INV_SLOT_CROWN));
                    getInventory().setItem(INV_SLOT_CROWN, ItemStack.EMPTY);
                }
                ejectPassengers();
            }
        } else if (tickCount > EARLY_TICK_THRESHOLD) {
            if (getArmorProgress() < ARMOR_EJECT_THRESHOLD && getInventory().getItem(INV_SLOT_ARMOR).getCount() < 64 && !level().isClientSide) {
                if (!getInventory().getItem(INV_SLOT_CROWN).isEmpty()) {
                    spawnAtLocation(getInventory().getItem(INV_SLOT_CROWN));
                    getInventory().setItem(INV_SLOT_CROWN, ItemStack.EMPTY);
                }
                ejectPassengers();
            }
        }
    }

    /** Passive healing when the head is submerged in water. */
    private void handleHealing() {
        if (this.getSubEntities()[1].isEyeInFluidType(Fluids.WATER.getFluidType()) && this.tickCount % HEALING_INTERVAL_TICKS == 0) {
            this.heal(HEALING_AMOUNT);
        }
    }

    /** Controls mouth open/close animation based on water state and movement speed. */
    private void handleMouthAnimation() {
        if (!this.isEyeInFluidType(Fluids.WATER.getFluidType())) {
            mouthTarget = 1;
        }

        if (this.getDeltaMovement().lengthSqr() > SPEED_THRESHOLD_MOUTH_OPEN_SQR) {
            mouthTarget = MOUTH_OPEN_SPEED;
        }

        updateMouthOpening();

        if (this.tickCount % MOUTH_CLOSE_INTERVAL_TICKS == 0) {
            mouthTarget = 0;
        }
    }

    /** Randomly grows dirt on the whale's body; clears top dirt when tamed.
     *  Matches the original 1.20.1 structure: no outer random gate here —
     *  each randomTick call has its own independent random check inside. */
    private void handleDirtTicks() {
        if (!level().isClientSide) {
            // Bottom dirt always ticks (each call rolls independently inside randomTick)
            this.hullbackDirt.randomTick(this.head.name, true);
            this.hullbackDirt.randomTick(this.body.name, true);
            this.hullbackDirt.randomTick(this.tail.name, true);
            this.hullbackDirt.randomTick(this.fluke.name, true);

            // Top dirt ticks only if NOT tamed
            if (!isTamed()) {
                this.hullbackDirt.randomTick(this.head.name, false);
                this.hullbackDirt.randomTick(this.body.name, false);
            } else {
                hullbackDirt.clearTopDirt();
            }
        }
    }

    /** Ticks all sub-entities and spawns wake particles on body/fluke when swimming. */
    private void tickSubEntitiesAndParticles() {
        for (int i = 0; i < subEntities.length; i++) {
            subEntities[i].tick();

            if (i == 2 || i == 4) {
                float offset = 0;
                if (this.level().isClientSide && this.isInWater() && this.getDeltaMovement().lengthSqr() > PARTICLE_SPEED_THRESHOLD_SQR) {
                    for (int side : SIDES) {
                        if (i == 2) {
                            offset = 4;
                        }
                        Vec3 particlePos = partManager.partPosition[i].add(new Vec3((offset + subEntities[i].getBbWidth() / 2) * side, 0, subEntities[i].getBbWidth() / 2).yRot(partManager.partYRot[i]));
                        double x = particlePos.x;
                        double y = particlePos.y;
                        double z = particlePos.z;

                        for (int j = 0; j < 4; ++j) {
                            this.level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.1, 0.0);
                            this.level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.1, 0.0);
                        }
                    }
                }
            }
        }
    }

    public boolean hasAnchorDown() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof AnchorEntity anchor) {
                if (anchor.isDown()) return true;
                break;
            }
        }
        return false;
    }



    /** Discards all walkable platform entities and nullifies references. */
    private void discardAllPlatforms() {
        partManager.discardAllPlatforms();
    }

    public void stopMoving() {
        this.getNavigation().stop();

        // Create platforms only if truly stable
        if (platformsStable) {
            if (this.partManager.moving_nose == null) {
                this.partManager.moving_nose = partManager.spawnPlatform(0);
                if (this.partManager.moving_nose != null) this.partManager.moving_nose.setPos(this.partManager.moving_nose.getX(), this.getY() + currentPlatformHeight, this.partManager.moving_nose.getZ());
            }
            if (this.partManager.moving_head == null) {
                this.partManager.moving_head = partManager.spawnPlatform(1);
                 if (this.partManager.moving_head != null) this.partManager.moving_head.setPos(this.partManager.moving_head.getX(), this.getY() + currentPlatformHeight, this.partManager.moving_head.getZ());
            }
            if (this.partManager.moving_body == null) {
                this.partManager.moving_body = partManager.spawnPlatform(2);
                 if (this.partManager.moving_body != null) this.partManager.moving_body.setPos(this.partManager.moving_body.getX(), this.getY() + currentPlatformHeight, this.partManager.moving_body.getZ());
            }
        }

        // Preserve vertical movement if in water (allows sinking to neutral depth)
        // Only freeze fully if on land or if explicitly anchored
        if (this.isInWater() || this.hasAnchorDown()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else {
            // Preserve Y velocity so gravity still pulls the whale down when above water
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        // Also stabilize pitch when stopping
        if (this.hasAnchorDown() || this.getStationaryTicks() > STATIONARY_MINIMUM_THRESHOLD) {
            this.setXRot(Mth.rotLerp(0.5f, this.getXRot(), 0f));
            this.xRotO = this.getXRot();
        }
    }

    private void updateMouthOpening() {
        mouthOpenProgress = (float) Mth.lerp(0.3, mouthOpenProgress, mouthTarget);
        if(!this.level().isClientSide){
            this.entityData.set(DATA_MOUTH_PROGRESS, mouthOpenProgress);
        }
    }

    public float getMouthOpenProgress() {
        return this.entityData.get(DATA_MOUTH_PROGRESS);
    }

    /**
     * Returns the smoothed horizontal swim speed for animation purposes.
     * Uses position delta (getX()-xo) which works on ALL clients, including non-pilot observers,
     * because vanilla entity tracking syncs positions to every client each tick.
     * Smoothed via exponential moving average to prevent jitter from entity tracking lerps.
     */
    public float getAnimationSwimSpeed() {
        if (this.tickCount != lastAnimSpeedTick) {
            lastAnimSpeedTick = this.tickCount;
            double dx = this.getX() - this.xo;
            double dz = this.getZ() - this.zo;
            float raw = (float) Math.sqrt(dx * dx + dz * dz);
            smoothedAnimSpeed = Mth.lerp(0.3f, smoothedAnimSpeed, raw);
        }
        return smoothedAnimSpeed;
    }

    public Vec3 getPartPos(int i){
        return partManager.partPosition[i];
    }

    public float getPartYRot(int i){
        return partManager.partYRot[i];
    }
    public float getPartXRot(int i){
        return partManager.partXRot[i];
    }

    public Vec3 getOldPartPos(int i){
        return partManager.oldPartPosition[i];
    }
    public float getOldPartYRot(int i){
        return partManager.oldPartYRot[i];
    }
    public float getOldPartXRot(int i){
        return partManager.oldPartXRot[i];
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new HullbackControlManager.HullbackBodyRotationControl(this);
    }




    @Override
    public void onPlayerJump(int i) { }

    @Override
    public boolean canJump() { return true; }

    @Override
    public void handleStartJump(int i) { }

    @Override
    public void handleStopJump() { }

    // PASSENGERS
    @Override
    public boolean canAddPassenger(Entity passenger) {
        return (getPassengers().size() < 7);
    }

    // Improve positionRider to handle uninitialized seats
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;

        int seatIndex = getSeatByEntity(passenger);
        if (seatIndex == -1) {
            return;
        }

        float yOffset = 0;
        if(this.getArmorProgress() == 0)
            yOffset = 0.5F;

        // Verify if seats were calculated
        if (seatIndex < partManager.seats.length && partManager.seats[seatIndex] != null) {
            callback.accept(passenger,
                    partManager.seats[seatIndex].x,
                    partManager.seats[seatIndex].y - yOffset,
                    partManager.seats[seatIndex].z);
        } else {
            // Fallback: Use entity's main position if seats were not calculated
            callback.accept(passenger,
                    this.getX(),
                    this.getY() + 5.0 - yOffset,
                    this.getZ());
        }
    }



    public static Holder<Attribute> getSwimSpeed() {
        return NeoForgeMod.SWIM_SPEED;
    }

    private void updateModifiers() {
        double speedModifier = 0.0;

        for (Entity passenger : getPassengers()) {
            if (passenger instanceof com.fruityspikes.whaleborne.server.entities.SailEntity sail) {
                speedModifier += sail.getSpeedModifier();
            }
        }

        // CONDITIONAL SPEED: Only apply sail speed if there is a pilot.
        // This makes docking easier as the whale slows down immediately when the helm is dismounted.
        if (this.getControllingPassenger() == null) {
            speedModifier = 0.0;
        }

        AttributeInstance inst = this.getAttribute(getSwimSpeed());
        if (inst != null) {
            AttributeModifier old = inst.getModifier(SAIL_SPEED_MODIFIER_ID);
            if (old != null) {
                inst.removeModifier(SAIL_SPEED_MODIFIER_ID);
            }
            if (speedModifier != 0.0) {
                inst.addPermanentModifier(new AttributeModifier(
                        SAIL_SPEED_MODIFIER_ID,
                        speedModifier,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
        }
    }

    public int getSeatByEntity(Entity entity){
        return hullbackSeatManager.getSeatByEntity(entity);
    }

    private void validateAssignments() {
        hullbackSeatManager.validateAssignments();
        updateModifiers();
    }
    public void assignSeat(int seatIndex, @Nullable Entity passenger) {
        hullbackSeatManager.assignSeat(seatIndex, passenger);
    }

    public Optional<Entity> getPassengerForSeat(int seatIndex) {
        return hullbackSeatManager.getPassengerForSeat(seatIndex);
    }
    private boolean isPassengerAssigned(Entity passenger) {
        return hullbackSeatManager.isPassengerAssigned(passenger);
    }
    private int findFreeSeat() {
        return hullbackSeatManager.findFreeSeat();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        // Sync immediately
        if (this.level() instanceof ServerLevel) {
             ((ServerLevel) this.level()).getChunkSource().broadcast(this, new ClientboundSetPassengersPacket(this));
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if(passenger instanceof Player && !this.isBreaching())
            stationaryTicks = STATIONARY_TICKS_DISMOUNT;
        if(passenger.isRemoved())
            for (int i = 0; i < 7; i++) {
                Optional<UUID> occupant = this.entityData.get(getSeatAccessor(i));
                if (occupant.isPresent() && occupant.get().equals(passenger.getUUID())) {
                    this.entityData.set(getSeatAccessor(i), Optional.empty());
                }
            }
        super.removePassenger(passenger);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int seatIndex = getSeatByEntity(passenger);
        if (seatIndex != -1) {
            for (int i = 0; i < 7; i++) {
                Optional<UUID> occupant = this.entityData.get(getSeatAccessor(i));
                if (occupant.isPresent() && occupant.get().equals(passenger.getUUID())) {
                    this.entityData.set(getSeatAccessor(i), Optional.empty());
                }
            }

            if (seatIndex >= 0 && seatIndex < partManager.seats.length) {
                Vec3 seatPos = partManager.seats[seatIndex];
                return new Vec3(seatPos.x, seatPos.y, seatPos.z);
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (int i = 0; i < 7; i++) {
            Optional<UUID> seatOccupant = this.entityData.get(getSeatAccessor(i));
            if (seatOccupant.isPresent()) {
                Entity entity = getEntityByUUID(seatOccupant.get());

                if (entity instanceof HelmEntity helm) {
                    LivingEntity controller = helm.getControllingPassenger();
                    if (controller != null) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }

    public Entity getEntityByUUID(UUID uuid) {
        for (Entity entity : getPassengers()) {

            if(entity.getUUID().equals(uuid))
                return entity;
        }
        return null;
    }

    private void syncMovement() {
        if (!this.level().isClientSide) {
            ((ServerLevel) this.level()).getChunkSource().broadcast(this, new ClientboundSetEntityMotionPacket(this.getId(), this.getDeltaMovement()));
        }
    }


    @Override
    public void travel(Vec3 travelVector) {
        // Stop horizontal movement if anchor is down or stationary (unless approaching a player)
        if ((this.hasAnchorDown() || (this.getStationaryTicks() > 0 && !this.isApproachingPlayer())) && this.isHullbackInWater()) {
            travelVector = new Vec3(0, 0, 0);
        }

        // Aggressive land speed reduction: scale input vector before vanilla physics
        if (!this.isHullbackInWater()) {
            travelVector = travelVector.multiply(LAND_SPEED_SCALE, 1.0, LAND_SPEED_SCALE);
        }

        // Buoyancy: target specific depth from sea level.
        // Must run even when ridden and regardless of EffectiveAI.
        boolean isBreachingAction = this.isBreaching;
        
        // Robust submerged checks
        boolean noseSubmerged = this.getPartManager() != null && 
                               this.getPartManager().subEntities != null && 
                               this.getPartManager().subEntities.length > 0 && 
                               this.getPartManager().subEntities[0].isEyeInFluidType(Fluids.WATER.getFluidType());
        boolean bodySubmerged = this.getFluidTypeHeight(Fluids.WATER.getFluidType()) > 0.1;
        boolean inWater = this.isInWater() || bodySubmerged;

        if (inWater && !isBreachingAction) {
            boolean isAtHelm = this.getControllingPassenger() != null && this.getControllingPassenger().getVehicle() instanceof HelmEntity;
            boolean shouldBeAtBoardingLevel = this.hasAnchorDown() || this.getStationaryTicks() > 0 || (this.isTamed() && !isAtHelm);

            if (isAtHelm) {
                applyBuoyancy(Config.hullbackDepthSailing);
            } else if (shouldBeAtBoardingLevel) {
                applyBuoyancy(Config.hullbackDepthBoarding);
            } else {
                // Wild / Active: gentle upward pull, sink if nose is out of water
                double targetY = this.level().getSeaLevel() + Config.hullbackDepthWild;
                double currentY = this.getY();
                double diff = targetY - currentY;
                
                if (currentY < targetY) {
                    double verticalForce = Mth.clamp(diff * WILD_BUOYANCY_UP_FACTOR, 0, WILD_BUOYANCY_UP_MAX); 
                    this.setDeltaMovement(this.getDeltaMovement().add(0, verticalForce, 0));
                } else if (!noseSubmerged) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, WILD_SINK_FORCE, 0));
                }
            }
        }

        // Pitch stabilization: prevent freezing in a tilted state when stopping
        if (!this.level().isClientSide && !isBreachingAction && (this.getControllingPassenger() == null || this.hasAnchorDown() || this.getStationaryTicks() > 0)) {
            if (this.hasAnchorDown() || this.getStationaryTicks() > 0) {
                this.pitchLocked = true;
                this.setXRot(0f);
                this.xRotO = 0f;
            } else {
                 float lerpSpeed = 0.08f;
                 this.setXRot(Mth.rotLerp(lerpSpeed, this.getXRot(), 0f));
            }
        } else {
             if (isBreachingAction) this.pitchLocked = false;
        }

        super.travel(travelVector);

        // Extra friction on land (compensate for vanilla travel logic)
        if (!this.isHullbackInWater() && this.onGround()) {
            BlockState surface = this.getBlockStateOn();
            double friction = LAND_FRICTION;
            
            if (surface.is(net.minecraft.world.level.block.Blocks.ICE) || 
                surface.is(net.minecraft.world.level.block.Blocks.PACKED_ICE) || 
                surface.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)) {
                friction = ICE_FRICTION;
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 1.0, friction));
        }

        // Final anchor lock (prevents drift from super.travel)
        if (this.hasAnchorDown() && this.isInWater()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }
    }

    /** Applies buoyancy targeting a specific depth offset from sea level. */
    private void applyBuoyancy(double depthOffset) {
        double targetY = this.level().getSeaLevel() + depthOffset;
        double currentY = this.getY();
        double diff = targetY - currentY;
        
        if (Math.abs(diff) < BUOYANCY_DEADZONE) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
            this.setPos(this.getX(), targetY, this.getZ());
        } else {
            double verticalForce = Mth.clamp(diff * BUOYANCY_FORCE_FACTOR, -BUOYANCY_FORCE_MAX, BUOYANCY_FORCE_MAX);
            this.setDeltaMovement(this.getDeltaMovement().add(0, verticalForce, 0));
        }
    }

    public boolean isHullbackInWater() {
        // Require significant depth (0.4m) to count as "swimming"
        return this.getFluidTypeHeight(Fluids.WATER.getFluidType()) > 0.4;
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) (this.isHullbackInWater() ? this.getAttributeValue(getSwimSpeed()) : this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override
    public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
        return true;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        openHullbackMenu(player);
    }
    private void openHullbackMenu(Player player) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer && serverPlayer.getRootVehicle() instanceof HullbackEntity) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return HullbackEntity.this.getDisplayName();
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    return new HullbackMenu(windowId, playerInventory, HullbackEntity.this);
                }
            }, buf -> buf.writeInt(this.getId()));
        }
    }
    
    public void setVectorControl(boolean val) {
         this.entityData.set(DATA_VECTOR_CONTROL, val);
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        // FIX: On other players' clients the pilot's xxa/zza are always zero,
        // causing false braking that makes the tail/seats jitter.  Only the
        // server and the pilot's own client should compute ridden input.
        if (this.level().isClientSide && !player.isLocalPlayer()) {
            return Vec3.ZERO;
        }

        // Limit pitch during swimming to prevent excessive tilting
        if (this.isInWater()) {
            float currentPitch = this.getXRot();

            if (Math.abs(currentPitch) > MAX_RIDING_PITCH) {
                this.setXRot(Mth.clamp(currentPitch, -MAX_RIDING_PITCH, MAX_RIDING_PITCH));
                this.xRotO = this.getXRot();
            }
        }
        return controlManager.getRiddenInput(player, travelVector);
    }

    // ─── Platform Height Logic ───────────────────────────────────

    private float getTargetPlatformHeight() {
        boolean isAtHelm = this.getControllingPassenger() != null && this.getControllingPassenger().getVehicle() instanceof HelmEntity;
        if (isAtHelm) {
            return PLATFORM_HEIGHT_HELM;
        }
        return PLATFORM_HEIGHT_STATIONARY;
    }

    private void updatePlatformHeight() {
        this.targetPlatformHeight = getTargetPlatformHeight();
        this.currentPlatformHeight = Mth.lerp(PLATFORM_HEIGHT_LERP_SPEED, this.currentPlatformHeight, this.targetPlatformHeight);
    }

    private void updateStationaryPlatforms() {
        partManager.updateStationaryPlatforms(currentPlatformHeight, this.getDeltaMovement());
    }

    // ─── Pitch Stability Fix ──────────────────────────────────────

    private void applyEnhancedPitchControl(RotationSnapshot snapshot) {
        // 1. Determine if we are performing an action that requires pitch freedom
        boolean isBreachingAction = false;
        for (net.minecraft.world.entity.ai.goal.WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof HullbackBreathAirGoal breachGoal && breachGoal.isBreaching()) {
                isBreachingAction = true;
                break;
            }
        }

        // 2. Determine if we should lock pitch
        // Lock if: Anchored OR (Stationary AND Not Approaching Player) OR (Not Controlled AND Not Breaching)
        // We do NOT lock if controlled by a player (unless anchored), allowing them to aim up/down effectively.
        // We do NOT lock if breaching.
        boolean shouldLock = (this.hasAnchorDown() || (this.getStationaryTicks() > 0 && !this.isApproachingPlayer()));
        
        // Should we assume "Free Swim" with no input means level out?
        if (!shouldLock && this.getControllingPassenger() == null && !isBreachingAction) {
             // If wild and not breaching, tend towards 0 pitch
             this.setXRot(Mth.rotLerp(0.1f, this.getXRot(), 0f));
             // But do not HARD LOCK
             this.pitchLocked = false;
        } else if (shouldLock && !isBreachingAction) {
            // HARD LOCK
            this.setYRot(snapshot.yaw());
            this.setYBodyRot(snapshot.bodyYaw());
            this.setYHeadRot(snapshot.headYaw());
            this.yRotO = snapshot.yaw();
            this.yBodyRotO = snapshot.bodyYaw();
            this.yHeadRotO = snapshot.headYaw();

            this.pitchLocked = true;
            this.setXRot(0f); // Calls override which forces super.setXRot(0)
            this.xRotO = 0f;
            
            // Stop vertical momentum if anchored
             if (this.hasAnchorDown() && this.isInWater()) {
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            }
        } else {
            // Free movement / Controlled
            this.pitchLocked = false;
        }
    }


}