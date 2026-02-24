package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.menus.HullbackMenu;
import com.fruityspikes.whaleborne.network.HullbackHurtPacket;
import com.fruityspikes.whaleborne.network.SyncHullbackDirtPacket;
import com.fruityspikes.whaleborne.network.WhaleborneNetwork;
import com.fruityspikes.whaleborne.server.data.HullbackDirtManager;
import com.fruityspikes.whaleborne.server.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class HullbackEntity extends WaterAnimal implements ContainerListener, HasCustomInventoryScreen, PlayerRideableJumping, Saddleable {
    private static final UUID SAIL_SPEED_MODIFIER_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final double PARTICLE_SPEED_THRESHOLD_SQR = 0.01;
    private static final int[] SIDES = {-1, 1};
    private boolean validatedAfterLoad = false;
    private float leftEyeYaw, rightEyeYaw, eyePitch;
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();
    private boolean immobile;
    private boolean tamedCoolDown;
    private Vec3 currentTarget;
    public int stationaryTicks;
    private boolean isBreaching;

    public void setBreaching(boolean breaching) {
        this.isBreaching = breaching;
    }

    public boolean isBreaching() {
        return isBreaching;
    }
    public SimpleContainer inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
        }
    };
    public static final int INV_SLOT_CROWN = 0;
    public static final int INV_SLOT_SADDLE = 1;
    public static final int INV_SLOT_ARMOR = 2;
    public static boolean HAS_MOBIUS_SPAWNED = false;
    public static final EntityDataAccessor<ItemStack> DATA_CROWN_ID = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> DATA_ARMOR = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_MOUTH_PROGRESS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_0 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_1 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_2 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_3 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_4 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_5 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_6 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_VECTOR_CONTROL = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STATIONARY_TICKS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MOUTH_TARGET = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.FLOAT);

    // Performance cache for third-person mod detection
    private static Boolean IS_THIRD_PERSON_MOD_LOADED = null;
    public final HullbackPartEntity head;
    public final HullbackPartEntity nose;
    public final HullbackPartEntity body;
    public final HullbackPartEntity tail;
    public final HullbackPartEntity fluke;

    public HullbackWalkableEntity moving_head;
    public HullbackWalkableEntity moving_nose;
    public HullbackWalkableEntity moving_body;
    private final HullbackPartEntity[] subEntities;
    private Vec3[] prevPartPositions;
    private Vec3[] partPosition;
    private float[] partYRot;
    private float[] partXRot;
    private Vec3[] oldPartPosition;
    private float[] oldPartYRot;
    private float[] oldPartXRot;
    public float newRotY = this.getYRot();
    private float mouthOpenProgress;
    private float mouthTarget;
    private boolean wasPilotControlled = false;
    private boolean pitchLocked = false;
    private boolean platformsStable = false; // Added for Integrated Fix
    private boolean isApproachingPlayer = false; // Flag to apply movement during approach
    private float currentPlatformHeight = 4.55f;
    private float targetPlatformHeight = 4.55f;
    private float oldPlatformHeight = 4.55f; // Rastreia a altura do tick anterior
    private int platformHeightCooldown = 0;
    private boolean wasMounted = false;
    private static final float PLATFORM_HEIGHT_THRESHOLD = 0.002f;
    private int playerStableTimer = 0;
    private Vec3 lastPlayerPosition = Vec3.ZERO;
    private static final double STABLE_THRESHOLD = 0.01;

    public void setApproachingPlayer(boolean approaching) {
        this.isApproachingPlayer = approaching;
    }

    public boolean isApproachingPlayer() {
        return this.isApproachingPlayer;
    }

    @Override
    public void setXRot(float xRot) {
        if (pitchLocked) {
            super.setXRot(0f);
        } else {
            super.setXRot(xRot);
        }
    }

    public static UUID getSailSpeedModifierUuid() {
        return SAIL_SPEED_MODIFIER_UUID;
    }

    public float AttributeSpeedModifier = 1;

    public int ticksSinceSpawn = 0;

    public BlockState[][] headDirt; // 8 x 5
    public BlockState[][] headTopDirt; // 8 x 5
    public BlockState[][] bodyDirt; // 5 x 5
    public BlockState[][] bodyTopDirt; // 5 x 5
    public BlockState[][] tailDirt; // 2 x 2
    public BlockState[][] flukeDirt; // 4 x 4

    public final Vec3[] seatOffsets = {
            //head
            new Vec3(0, 5.5f, 0.0), //sail
            new Vec3(0, 5.5f, -3.0), //captain

            //body
            new Vec3(1.5, 5.5f, 0.3),
            new Vec3(-1.5, 5.5f, 0.3),
            new Vec3(1.5, 5.5f, -1.75),
            new Vec3(-1.5, 5.5f, -1.75),

            //fluke
            new Vec3(0, 1.6f, -0.8)
    };

    public Vec3[] seats = new Vec3[7];
    public Vec3[] oldSeats = new Vec3[7];
    public HullbackEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);

        this.inventory.addListener(this);
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this.inventory));

        this.moveControl = new StationaryAwareMoveControl(this);
        this.lookControl = new SmoothSwimmingLookControl(this, 1) {
            @Override
            public void tick() {
                if (HullbackEntity.this.stationaryTicks > 0) return;
                super.tick();
            }
        };

        this.nose = new HullbackPartEntity(this, "nose", 5.0F, 5.0F);
        this.head = new HullbackPartEntity(this, "head", 5.0F, 5.0F);
        this.body = new HullbackPartEntity(this, "body", 5.0F, 5.0F);
        this.tail = new HullbackPartEntity(this, "tail", 2.5F, 2.5F);
        this.fluke = new HullbackPartEntity(this, "fluke", 4.0F, 0.6F);

        this.moving_nose = null;
        this.moving_head = null;
        this.moving_body = null;

        this.subEntities = new HullbackPartEntity[]{this.nose, this.head, this.body, this.tail, this.fluke};
        
        this.prevPartPositions = new Vec3[5];
        this.partPosition = new Vec3[5];
        this.partYRot = new float[5];
        this.partXRot = new float[5];
        this.oldPartPosition = new Vec3[5];
        this.oldPartYRot = new float[5];
        this.oldPartXRot = new float[5];
        Arrays.fill(partPosition, Vec3.ZERO);
        this.mouthOpenProgress = 0.0f;
        this.currentTarget = this.position();
        
        if(!level.isClientSide) initDirt();
        else initClientDirt();

        this.stationaryTicks = 60;
    }


    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnReason) {
        return checkHullbackSpawnRules((EntityType<HullbackEntity>) this.getType(), level, spawnReason, this.blockPosition(), level.getRandom());
    }

    public static boolean checkHullbackSpawnRules(EntityType<HullbackEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        if (!hasSpawnSpace(level, pos)) {
            return false;
        }

        if (hasNearbyHullbacks(level, pos)) {
            return false;
        }

        return true;
    }

    private static boolean hasSpawnSpace(LevelAccessor level, BlockPos pos) {
        int checkRadius = 5; 
        int heightCheck = 4; 

        AABB spawnCheckArea = new AABB(pos).inflate(checkRadius, heightCheck, checkRadius);

        int solidCount = 0;
        int maxSolidAllowed = 20;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = (int)spawnCheckArea.minX; x <= spawnCheckArea.maxX; x++) {
            for (int y = (int)spawnCheckArea.minY; y <= spawnCheckArea.maxY; y++) {
                for (int z = (int)spawnCheckArea.minZ; z <= spawnCheckArea.maxZ; z++) {
                    mutablePos.set(x, y, z);
                    BlockState state = level.getBlockState(mutablePos);
                    if (state.isSolid() && state.getCollisionShape(level, mutablePos) != Shapes.empty()) {
                         solidCount++;
                         if (solidCount > maxSolidAllowed) {
                             return false; 
                         }
                    }
                }
            }
        }

        return true;
    }

    private static boolean hasNearbyHullbacks(LevelAccessor level, BlockPos pos) {
        AABB checkArea = new AABB(pos).inflate(16, 8, 16);
        return !level.getEntitiesOfClass(
                HullbackEntity.class,
                checkArea,
                e -> e.isAlive() && !e.isTamed()
        ).isEmpty();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void initClientDirt() {
        headDirt = new BlockState[8][5];
        headTopDirt = new BlockState[8][5];
        bodyDirt = new BlockState[6][5];
        bodyTopDirt = new BlockState[6][5];
        tailDirt = new BlockState[4][2];
        flukeDirt = new BlockState[3][5];

        for (BlockState[][] array : new BlockState[][][]{headDirt, headTopDirt, bodyDirt, bodyTopDirt, tailDirt, flukeDirt}) {
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    array[x][y] = Blocks.AIR.defaultBlockState();
                }
            }
        }
    }

    private void initDirt() {
        headDirt = new BlockState[8][5];
        headTopDirt = new BlockState[8][5];
        bodyDirt = new BlockState[6][5];
        bodyTopDirt = new BlockState[6][5];
        tailDirt = new BlockState[4][2];
        flukeDirt = new BlockState[3][5];

        for (BlockState[][] array : new BlockState[][][]{headDirt, headTopDirt, bodyDirt, bodyTopDirt, tailDirt, flukeDirt}) {
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    array[x][y] = Blocks.AIR.defaultBlockState();
                }
            }
        }

        fillDirtArray(headDirt, true, getPartName(head));
        fillDirtArray(headTopDirt, false, getPartName(head));
        fillDirtArray(bodyDirt, true, getPartName(body));
        fillDirtArray(bodyTopDirt, false, getPartName(body));
        fillDirtArray(tailDirt, true, getPartName(tail));
        fillDirtArray(flukeDirt, true, getPartName(tail));

        syncDirtToClients();
    }

    private void fillDirtArray(BlockState[][] array, boolean bottom, String partName) {
        List<HullbackDirtManager.HullbackDirtEntry> candidates = getCandidates(bottom, partName);

        for (int x = 0; x < array.length; x++) {
            for (int y = 0; y < array[x].length; y++) {
                array[x][y] = Blocks.AIR.defaultBlockState();
                if (candidates.isEmpty()) continue;
                List<HullbackDirtManager.HullbackDirtEntry> successful = new ArrayList<>();

                for (HullbackDirtManager.HullbackDirtEntry entry : candidates) {
                    if (random.nextDouble() < entry.placementChance()) successful.add(entry);
                }

                if (!successful.isEmpty()) {
                    HullbackDirtManager.HullbackDirtEntry chosen = successful.get(random.nextInt(successful.size()));
                    array[x][y] = applyProperties(chosen.block(), chosen.blockProperties());
                }
            }
        }
    }

    public static BlockState applyProperties(Block block, @Nullable Map<String, String> props) {
        BlockState state = block.defaultBlockState();
        if (props == null || props.isEmpty()) return state;

        for (Map.Entry<String, String> entry : props.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
            if (property == null) {
                Whaleborne.LOGGER.warn("Unknown blockstate property '{}' for block {}", name, block);
                continue;
            }

            state = setProperty(state, property, value);
        }

        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        if (parsed.isPresent()) return state.setValue(property, parsed.get());
        Whaleborne.LOGGER.warn("Invalid value '{}' for property '{}' (allowed: {})", value, property.getName(), property.getPossibleValues());
        return state;
    }

    public BlockState[][] getDirtArray(int index, boolean bottom){
        if(bottom){
            switch (index){
                case 2: return bodyDirt;
                case 3: return tailDirt;
                case 4: return flukeDirt;
                default: return headDirt;
            }
        }
        else {
            if (index == 0)
                return headTopDirt;
            else return bodyTopDirt;
        }
    }

    public void syncDirtToClients() {
        if (!level().isClientSide) {
            syncDirtArray(headDirt, 0, true);
            syncDirtArray(headTopDirt, 1, false);
            syncDirtArray(bodyDirt, 2, true);
            syncDirtArray(bodyTopDirt, 3, false);
            syncDirtArray(tailDirt, 4, true);
            syncDirtArray(flukeDirt, 5, true);
        }
    }

    public void syncDirtArray(BlockState[][] array, int arrayType, boolean isBottom) {
        if (!level().isClientSide) {
            WhaleborneNetwork.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> this),
                    new SyncHullbackDirtPacket(getId(), array, arrayType, isBottom)
            );
        }
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
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get(), 1.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new HullbackBreathAirGoal(this));
        this.goalSelector.addGoal(1, new HullbackApproachPlayerGoal(this, 1.2f));
        this.goalSelector.addGoal(2, new HullbackTryFindWaterGoal(this, true));
        this.goalSelector.addGoal(2, new HullbackArmorPlayerGoal(this, 1.2f)); 
        this.goalSelector.addGoal(3, new HullbackRandomSwimGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    protected int getInventorySize() {
        return 3;
    }
    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.moveControl = new StationaryAwareMoveControl(this);
        this.lookControl = new StationaryAwareLookControl(this);
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this.inventory));
    }
    public void updateContainerEquipment() {
        ItemStack crown = this.inventory.getItem(INV_SLOT_CROWN);
        ItemStack armor = this.inventory.getItem(INV_SLOT_ARMOR);
        boolean hasSaddle = !this.inventory.getItem(INV_SLOT_SADDLE).isEmpty();
        this.entityData.set(DATA_CROWN_ID, crown);
        this.entityData.set(DATA_ARMOR, armor);
        sendHurtSyncPacket();
        this.setFlag(4, hasSaddle);
    }
    public void containerChanged(Container invBasic) {
        ItemStack itemstack = this.getArmor();
        boolean flag = this.isSaddled();
        this.updateContainerEquipment();
        ItemStack itemstack1 = this.getArmor();
        if (this.tickCount > 20 && !flag && this.isSaddled()) {
            this.playSound(this.getSaddleSoundEvent(), 0.5F, 1.0F);
        }
        if (this.tickCount > 20 && itemstack != itemstack1) {
            this.playSound(SoundEvents.HORSE_ARMOR, 0.5F, 1.0F);
        }
    }

    @Override
    public void onInsideBubbleColumn(boolean downwards) {

    }

    public ItemStack getArmor() {
        return this.entityData.get(DATA_ARMOR);
    }
    public ItemStack getCrown() {
        return this.entityData.get(DATA_CROWN_ID);
    }
    public boolean isTamed() {
        return this.getFlag(2);
    }
    public void setTamed(boolean tamed) {

        this.setPersistenceRequired();
        this.setFlag(2, tamed);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public boolean isSaddleable() {
        return this.isAlive() && this.isTamed();
    }

    public void equipSaddle(@Nullable SoundSource source) {
        this.inventory.setItem(INV_SLOT_SADDLE, new ItemStack(Items.SADDLE));
    }
    public boolean isSaddled() {
        return this.getFlag(4);
    }

    protected boolean getFlag(int flagId) {
        return ((Byte)this.entityData.get(DATA_ID_FLAGS) & flagId) != 0;
    }

    protected void setFlag(int flagId, boolean value) {
        byte flags = this.entityData.get(DATA_ID_FLAGS);
        if (value) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(flags | flagId));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(flags & ~flagId));
        }
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_FLAGS, (byte)0);

        this.entityData.define(DATA_SEAT_0, Optional.empty());
        this.entityData.define(DATA_SEAT_1, Optional.empty());
        this.entityData.define(DATA_SEAT_2, Optional.empty());
        this.entityData.define(DATA_SEAT_3, Optional.empty());
        this.entityData.define(DATA_SEAT_4, Optional.empty());
        this.entityData.define(DATA_SEAT_5, Optional.empty());
        this.entityData.define(DATA_SEAT_6, Optional.empty());

        this.entityData.define(DATA_CROWN_ID, ItemStack.EMPTY);
        this.entityData.define(DATA_ARMOR, ItemStack.EMPTY);
        this.entityData.define(DATA_MOUTH_PROGRESS, 0f);
        this.entityData.define(DATA_VECTOR_CONTROL, false);
        this.entityData.define(DATA_STATIONARY_TICKS, 0);
    }
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ListTag listtag = new ListTag();

        for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)i);
                itemstack.save(compoundtag);
                listtag.add(compoundtag);
            }
        }
        compound.put("Items", listtag);
        compound.putByte("Flags", this.entityData.get(DATA_ID_FLAGS));

        CompoundTag hasMobiusSpawned = new CompoundTag();
        hasMobiusSpawned.putBoolean("HasMobiusSpawned", HAS_MOBIUS_SPAWNED);

        for (int i = 0; i < 7; i++) {
            Optional<UUID> occupant = this.entityData.get(getSeatAccessor(i));
            String seatKey = "Seat_" + i;

            if (occupant.isPresent()) {
                compound.putUUID(seatKey, occupant.get());
            }
        }
        compound.put("HeadDirt", saveDirtArray(headDirt));
        compound.put("HeadTopDirt", saveDirtArray(headTopDirt));

        compound.put("BodyDirt", saveDirtArray(bodyDirt));
        compound.put("BodyTopDirt", saveDirtArray(bodyTopDirt));

        compound.put("tailDirt", saveDirtArray(tailDirt));
        compound.put("flukeDirt", saveDirtArray(flukeDirt));

        compound.putInt("TicksSinceSpawn", ticksSinceSpawn);
    }
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ListTag items = compound.getList("Items", 10);

        for(int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < this.inventory.getContainerSize()) {
                this.inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }

        // this.createInventory(); // REMOVED to avoid resetting inventory
        this.entityData.set(DATA_ID_FLAGS, compound.getByte("Flags"));
        if (compound.contains("HasMobiusSpawned")) {
            HAS_MOBIUS_SPAWNED = compound.getBoolean("HasMobiusSpawned");
        }

        for (int i = 0; i < 7; i++) {
            String seatKey = "Seat_" + i;
            if (compound.hasUUID(seatKey)) {
                UUID uuid = compound.getUUID(seatKey);
                this.entityData.set(getSeatAccessor(i), Optional.of(uuid));
            } else {
                this.entityData.set(getSeatAccessor(i), Optional.empty());
            }
        }

        syncDirtToClients();
        this.updateContainerEquipment();

        if (compound.contains("HeadDirt")) {
            headDirt = loadDirtArray(compound.getCompound("HeadDirt"));
        }
        if (compound.contains("HeadTopDirt")) {
            headTopDirt = loadDirtArray(compound.getCompound("HeadTopDirt"));
        }
        if (compound.contains("BodyDirt")) {
            bodyDirt = loadDirtArray(compound.getCompound("BodyDirt"));
        }
        if (compound.contains("BodyTopDirt")) {
            bodyTopDirt = loadDirtArray(compound.getCompound("BodyTopDirt"));
        }
        if (compound.contains("TailDirt")) {
            tailDirt = loadDirtArray(compound.getCompound("TailDirt"));
        }
        if (compound.contains("FlukeDirt")) {
            flukeDirt = loadDirtArray(compound.getCompound("FlukeDirt"));
        }

        if (compound.contains("TicksSinceSpawn")) {
            ticksSinceSpawn = compound.getInt("TicksSinceSpawn");
        }

        syncDirtToClients();
        this.updateContainerEquipment();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_STATIONARY_TICKS.equals(key)) {
            this.stationaryTicks = this.entityData.get(DATA_STATIONARY_TICKS);
        }
    }

    public int getStationaryTicks() {
        return this.entityData.get(DATA_STATIONARY_TICKS);
    }


    private EntityDataAccessor<Optional<UUID>> getSeatAccessor(int seatIndex) {
        return switch (seatIndex) {

            case 0 -> DATA_SEAT_0;
            case 1 -> DATA_SEAT_1;
            case 2 -> DATA_SEAT_2;
            case 3 -> DATA_SEAT_3;
            case 4 -> DATA_SEAT_4;
            case 5 -> DATA_SEAT_5;
            default -> DATA_SEAT_6;
        };
    }

    private CompoundTag saveDirtArray(BlockState[][] array) {
        CompoundTag tag = new CompoundTag();
        for (int x = 0; x < array.length; x++) {
            ListTag column = new ListTag();
            for (int y = 0; y < array[x].length; y++) {
                column.add(NbtUtils.writeBlockState(array[x][y]));
            }
            tag.put("x" + x, column);
        }
        return tag;
    }

    private BlockState[][] loadDirtArray(CompoundTag tag) {
        BlockState[][] array = new BlockState[tag.size()][];
        for (int x = 0; x < array.length; x++) {
            String key = "x" + x;
            if (tag.contains(key)) {
                ListTag column = tag.getList(key, 10);
                array[x] = new BlockState[column.size()];
                for (int y = 0; y < column.size(); y++) {
                    array[x][y] = NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), column.getCompound(y));
                }
            }
        }
        return array;
    }

    public float getArmorProgress() {
        return (float) this.entityData.get(DATA_ARMOR).getCount() / 64f;
    }

    public float getLeftEyeYaw() {
        return leftEyeYaw;
    }

    public float getRightEyeYaw() {
        return rightEyeYaw;
    }

    public float getEyePitch() {
        return eyePitch;
    }

    public boolean canBreatheUnderwater() {
        return isVehicle();
    }

    // Override to suppress default air supply tick — prevents the entity from
    // trying to surface for air, which would fight the custom buoyancy in travel().
    @Override
    protected void handleAirSupply(int airSupply) { }

    @Override
    public int getMaxAirSupply() {
        return 10000;
    }

    @Override
    protected int increaseAirSupply(int currentAir) {
        return this.getMaxAirSupply();
    }



    public boolean isPushable() {
        return false;
    }

    public void setId(int p_20235_) {
        super.setId(p_20235_);

        for (int i = 0; i < this.subEntities.length; i++) {
            this.subEntities[i].setId(p_20235_ + i + 1);
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

    public boolean isMultipartEntity() {
        return true;
    }

    public PartEntity<?>[] getParts() {
        return this.subEntities;
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
    }

    private boolean scanPlayerAbove() {
        // If approaching, the check is RIGOROUS (0.5 blocks). 
        // You have to be literally stepping on it to count.
        double heightCheck = isApproachingPlayer() ? 0.5 : 2.0;

        for (int i = 0; i < 3; i++) { // Nose, Head, Body
            HullbackPartEntity part = getSubEntities()[i];
            AABB box = part.getBoundingBox();
            
            // Reduced inflation to avoid false lateral detection
            AABB topSurface = new AABB(
                    box.minX,
                    box.maxY,
                    box.minZ,
                    box.maxX,
                    box.maxY + heightCheck, 
                    box.maxZ
            ).inflate(0.5, 0.0, 0.5);

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
    public void playAmbientSound() {
        if (this.random.nextFloat() < 0.5) return;

        mouthTarget = 0.2f;

        for (int side : new int[]{-1, 1}) {
            Vec3 particlePos = partPosition[1].add(new Vec3(3.5 * side, 2, 0).yRot(-partYRot[1]));
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

        playSound(WBSoundRegistry.HULLBACK_AMBIENT.get(), Config.SOUND_DISTANCE.get().floatValue(), 1);
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

    public void moveEntitiesOnTop(int index, Set<Entity> movedThisTick) {
        HullbackPartEntity part = getSubEntities()[index];
        
        // Body movement calculation
        Vec3 bodyOffset = partPosition[index].subtract(oldPartPosition[index]);

        // NEW: Platform height delta calculation (Relative Height Fix)
        double heightDelta = (double)this.currentPlatformHeight - (double)this.oldPlatformHeight;

        // Total movement = Body Movement + Height Variation
        Vec3 totalOffset = bodyOffset.add(0, heightDelta, 0);

        // If platforms are stable, DO NOT move anyone
        if (this.platformsStable) {
            return;
        }

        // Tiny threshold to ensure micro-movements are processed (Elevator Problem)
        if (totalOffset.lengthSqr() < 1.0E-7) return; 

        if (index > 2) return; // Only main platforms (0, 1, 2) move entities

        // Search area expanded vertically (6.0F) to reach the deck
        for (Entity entity : this.level().getEntities(part, part.getBoundingBox().inflate(0.5F, 6.0F, 0.5F), 
             EntitySelector.NO_SPECTATORS.and((e) -> (!e.isPassenger())))) {
            
            if (!movedThisTick.contains(entity) && !entity.noPhysics && 
                !(entity instanceof HullbackPartEntity) && 
                !(entity instanceof HullbackEntity) && 
                !(entity instanceof HullbackWalkableEntity)) {

                // Checks if the entity is actually ON the platform visually
                if (entity.getY() < part.getY() + currentPlatformHeight - 0.5) continue;
                
                double motionMultiplier = 0.5;
                double entityMultiplier = motionMultiplier;
                
                if (entity instanceof Player player) {
                    entityMultiplier *= 0.8;
                    
                    // If the player has been stable for more than 1 second, reduces horizontal movement
                    if (playerStableTimer > 20) {
                        entityMultiplier *= 0.1;
                    }
                    
                    // --- ELEVATOR AND RELATIVE HEIGHT FIX ---
                    // For the Player, we apply 100% of the vertical variation to ensure smooth contact
                    Vec3 moveVec = new Vec3(totalOffset.x * entityMultiplier, totalOffset.y, totalOffset.z * entityMultiplier);
                    
                    entity.move(MoverType.SHULKER, moveVec);
                    
                    player.setOnGround(true);
                    player.fallDistance = 0; // Prevents fall damage accumulated from jitter
                } else {
                    Vec3 moveVec = totalOffset.scale(entityMultiplier);
                    entity.move(MoverType.SHULKER, moveVec);
                }
                
                entity.hurtMarked = true;
                movedThisTick.add(entity);
            }
        }
    }


    private List<HullbackDirtManager.HullbackDirtEntry> getCandidates(boolean bottom, String partName) {
        String key = partName + "_" + (bottom ? "bottom" : "top");
        return Whaleborne.PROXY.getHullbackDirtManager().get().stream()
                .filter(e -> e.placements().contains(key))
                .toList();
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isVehicle()) {
            return super.mobInteract(player, hand);
        }
        return super.mobInteract(player, hand);
    }

    public InteractionResult interactDebug(Player player, InteractionHand hand) {
        if (!isTamed()) {
            setTamed(true);
            this.setPersistenceRequired();
            //equipSaddle();
            return InteractionResult.SUCCESS;
        }

        System.out.println(level() + ": Seat Data: ");
        for (int i = 0; i < 7; i++) {
            System.out.println("Seat " + i + ": " + this.entityData.get(getSeatAccessor(i)));
        }

        System.out.println(getPassengers());

        return InteractionResult.SUCCESS;
    }

    public InteractionResult interactRide(Player player, InteractionHand hand, int seatIndex, @Nullable EntityType<?> entityType) {
        if (seatIndex < 0 || seatIndex >= 7) {
            return InteractionResult.FAIL;
        }

        if (getArmorProgress() < 0.5) {
            return InteractionResult.FAIL;
        }

        if (!isSaddled()) {
            mouthTarget = 1.0f;
            playSound(WBSoundRegistry.HULLBACK_MAD.get());
            return InteractionResult.PASS;
        }

        Optional<UUID> currentSeatOccupant = this.entityData.get(getSeatAccessor(seatIndex));
        if (currentSeatOccupant.isPresent()) {
            if (currentSeatOccupant.get().equals(player.getUUID())) {
                return InteractionResult.PASS;
            }
            return InteractionResult.FAIL;
        }

        if (entityType != null) {
            return placeEquipment(player, hand, seatIndex, entityType);
        }

        return mountPlayer(player, seatIndex);
        //return super.interact(player, hand);
    }

    private InteractionResult placeEquipment(Player player, InteractionHand hand, int seatIndex, EntityType<?> entityType) {
//        if (level().isClientSide) {
//            return InteractionResult.SUCCESS;
//        }

        Entity entity = entityType.create(this.level());
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        Vec3 seatPos = seats[seatIndex];
        entity.moveTo(seatPos.x, seatPos.y + 1, seatPos.z, this.getYRot(), 0);
        this.level().addFreshEntity(entity);

        if (entity.startRiding(this, true)) {
            assignSeat(seatIndex, entity);
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        //entity.discard();
        return InteractionResult.FAIL;
    }

    private InteractionResult mountPlayer(Player player, int seatIndex) {
        for (int i = 0; i < 7; i++) {
            Optional<UUID> occupant = this.entityData.get(getSeatAccessor(i));
            if (occupant.isPresent() && occupant.get().equals(player.getUUID())) {
                player.stopRiding();
            }
        }

        if (player.startRiding(this, true)) {
            assignSeat(seatIndex, player);
            return InteractionResult.SUCCESS;
        } else {
            //player.stopRiding();
            return InteractionResult.FAIL;
        }
    }

    public InteractionResult interactClean(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        mouthTarget = 0.2f;
        if ((handleVegetationRemoval(player, hand, part, top)) == InteractionResult.PASS) {

            for (BlockState[] states : headTopDirt) {
                for (BlockState state : states) {
                    if (state != Blocks.AIR.defaultBlockState()) {
                        mouthTarget = 0.5f;
                        playSound(WBSoundRegistry.HULLBACK_MAD.get());
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            for (BlockState[] blockStates : bodyTopDirt) {
                for (BlockState blockState : blockStates) {
                    if (blockState != Blocks.AIR.defaultBlockState()) {
                        mouthTarget = 0.5f;
                        playSound(WBSoundRegistry.HULLBACK_MAD.get());
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            setTamed(true);
            this.setPersistenceRequired();
            mouthTarget = 0.1f;
            playSound(WBSoundRegistry.HULLBACK_HAPPY.get());

            for (int side : new int[]{-1, 1}) {
                Vec3 particlePos = partPosition[1].add(new Vec3(4 * side, 2, 0).yRot(partYRot[1]));
                double x = particlePos.x;
                double y = particlePos.y;
                double z = particlePos.z;

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, x, y, z, 10, 0.5, 0.5, 0.5, 0.02);
                }
            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleVegetationRemoval(Player player, InteractionHand hand, HullbackPartEntity part, boolean top) {
        BlockState[][] dirtArray = getDirtArrayForPart(part, top);
        ItemStack held = player.getItemInHand(hand);

        for (int x = 0; x < dirtArray.length; x++) {
            for (int y = 0; y < dirtArray[x].length; y++) {
                BlockState state = dirtArray[x][y];
                if (state == null || state.isAir()) continue;

                HullbackDirtManager.HullbackDirtEntry entry = HullbackDirtManager.DATA.stream().filter(e -> e.matches(state)).findFirst().orElse(null);

                if (entry == null) continue;

                boolean removable = entry.removableWith().contains("any") || entry.removableWith().contains(getToolType(held));

                if (removable) {
                    if (level() instanceof ServerLevel serverLevel) {
                        float yOffset = top ? 5 : -1;

                        serverLevel.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, applyProperties(entry.block(), entry.blockProperties())),
                                position().x, position().y + yOffset, position().z,
                                60,
                                3, 0.2, -3
                                , 0);

                        dirtArray[x][y] = Blocks.AIR.defaultBlockState();

                        if (entry.drop() != Items.AIR) {
                            for (int i = 0; i < entry.dropAmount(); i++) {
                                ItemStack dropStack = new ItemStack(entry.drop());
                                double px = part.getX() + y - part.getSize().width / 2.0;
                                double py = part.getY() + (top ? part.getSize().height + 0.5f : -0.5f);
                                double pz = part.getZ() - x + part.getSize().width / 2.0;
                                ItemEntity itemEntity = new ItemEntity(this.level(), px, py, pz, dropStack);
                                this.level().addFreshEntity(itemEntity);
                            }
                        }

                        if (!player.isCreative()) {
                            player.getItemInHand(hand).hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                        }

                        if (entry.soundOnRemove() != null) {
                            this.level().playSound(null, player.getX(), player.getY(), player.getZ(), entry.soundOnRemove(), SoundSource.PLAYERS, 1.0F, 1.0f);
                        } else {
                            this.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0f);
                        }

                        mouthTarget = 1.0f;
                        syncDirtToClients();
                    } else {
                        dirtArray[x][y] = Blocks.AIR.defaultBlockState();
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    public InteractionResult interactArmor(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.getItem() instanceof SaddleItem) {
            if (!this.isSaddled()) {
                if (this.isTamed()) {
                    this.equipSaddle(SoundSource.PLAYERS);
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.HORSE_SADDLE,
                            SoundSource.PLAYERS, 1.0F, 0.1F);
                    return InteractionResult.SUCCESS;
                } else {
                    mouthTarget = 0.3f;
                    playSound(WBSoundRegistry.HULLBACK_MAD.get());
                    return InteractionResult.PASS;
                }
            }
            return InteractionResult.PASS;
        } else if (heldItem.is(WBTagRegistry.HULLBACK_EQUIPPABLE)) {
            if (!isSaddled()) {
                mouthTarget = 0.3f;
                playSound(WBSoundRegistry.HULLBACK_MAD.get());
                return InteractionResult.PASS;
            }

            ItemStack currentArmor = this.inventory.getItem(INV_SLOT_ARMOR);

            if (currentArmor.getCount() == currentArmor.getMaxStackSize()) {
                mouthTarget = 0.3f;
                playSound(WBSoundRegistry.HULLBACK_MAD.get());
                return InteractionResult.PASS;
            }

            if (currentArmor.getCount() < currentArmor.getMaxStackSize()) {
                if (currentArmor.isEmpty()) {
                    ItemStack newArmor = new ItemStack(heldItem.getItem(), 1);
                    this.inventory.setItem(INV_SLOT_ARMOR, newArmor);
                    updateContainerEquipment();
                } else {
                    if (heldItem.getItem() == currentArmor.getItem()) {
                        if (player.isCreative()) {
                            currentArmor.setCount(64);
                        } else {
                            currentArmor.grow(1);
                        }

                        this.entityData.set(DATA_ARMOR, currentArmor.copy());
                    } else {
                        mouthTarget = 0.3f;
                        playSound(WBSoundRegistry.HULLBACK_MAD.get());
                        return InteractionResult.PASS;
                    }
                }

                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }

                this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WOOD_PLACE,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                        SoundSource.PLAYERS, 1.0F, 0.5f + ((float) currentArmor.getCount() / 64));
                if (currentArmor.getCount() == 64) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.PLAYERS, 2.0F, 1.0F);
                    playSound(WBSoundRegistry.HULLBACK_TAME.get());

                }
                this.updateContainerEquipment();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    private String getPartName(HullbackPartEntity part) {
        if (part == this.head) return "head";
        if (part == this.body) return "body";
        if (part == this.tail) return "tail";
        if (part == this.fluke) return "fluke";
        if (part == this.nose) return "nose";
        return "body";
    }

    private String getToolType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "hand";
        Item item = stack.getItem();
        if (item instanceof ShearsItem || stack.is(Tags.Items.SHEARS)) return "shears";
        if (item instanceof AxeItem || stack.is(ItemTags.AXES)) return "axe";
        if (item instanceof PickaxeItem || stack.is(ItemTags.PICKAXES)) return "pickaxe";
        if (item instanceof HoeItem || stack.is(ItemTags.HOES)) return "hoe";
        if (item instanceof ShovelItem || stack.is(ItemTags.SHOVELS)) return "shovel";
        if (item instanceof SwordItem || stack.is(ItemTags.SWORDS)) return "sword";
        return "hand";
    }

    private BlockState[][] getDirtArrayForPart(HullbackPartEntity part, boolean top) {
        if (part == this.head) {
            return top ? this.headTopDirt : this.headDirt;
        } else if (part == this.body) {
            return top ? this.bodyTopDirt : this.bodyDirt;
        } else if (part == this.fluke) {
            return this.flukeDirt;
        } else if (part == this.nose) {
            return top ? this.headTopDirt : this.headDirt;
        }
        return this.tailDirt;
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        ItemStack armorStack = this.inventory.getItem(INV_SLOT_ARMOR);

        float resistance = 1;
        if (armorStack.getItem() instanceof BlockItem blockItem) {
            BlockState defaultState = blockItem.getBlock().defaultBlockState();
            resistance = defaultState.getDestroySpeed(null, null);
            if (resistance < 0) {
                resistance = 50f;
            }
        }

        if (!armorStack.isEmpty()) {
            float blockChance = resistance / 70f;
            if (this.random.nextFloat() < blockChance) {
                this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                this.playSound(WBSoundRegistry.HULLBACK_HAPPY.get(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                updateContainerEquipment();
                return super.hurt(source, 0); // Blocked completely (parity with Ref)
            }

            mouthTarget = 0.8f;
            int originalCount = armorStack.getCount();
            int armorDamage = Math.min(originalCount, (int) Math.ceil(amount));
            armorStack.shrink(armorDamage);
            this.inventory.setItem(INV_SLOT_ARMOR, armorStack);
            this.entityData.set(DATA_ARMOR, armorStack);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
            playSound(WBSoundRegistry.HULLBACK_MAD.get());
            float remainingDamage = Math.max(0, amount - armorDamage);
            updateContainerEquipment();
            return super.hurt(source, remainingDamage);
        }
        mouthTarget = 0;
        return super.hurt(source, amount);
    }

    @Override
    protected void tickDeath() {
        if (this.moving_nose != null) {
            this.moving_nose.discard();
            this.moving_nose = null;
        }
        if (this.moving_head != null) {
            this.moving_head.discard();
            this.moving_head = null;
        }
        if (this.moving_body != null) {
            this.moving_body.discard();
            this.moving_body = null;
        }
        List<HullbackWalkableEntity> list = level().getEntitiesOfClass(HullbackWalkableEntity.class, this.getBoundingBox().inflate(6));
        if (!list.isEmpty()) for (HullbackWalkableEntity entity : list) entity.discard();

        super.tickDeath();
    }

    private void sendHurtSyncPacket() {
        if (!this.level().isClientSide) {
            WhaleborneNetwork.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> this),
                    new HullbackHurtPacket(
                            this.getId(),
                            this.inventory.getItem(INV_SLOT_ARMOR),
                            this.inventory.getItem(INV_SLOT_CROWN),
                            this.entityData.get(DATA_ID_FLAGS)
                    )
            );
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        // ENSURE data-synchers match the inventory on load to prevent UI wipes
        this.updateContainerEquipment();
        this.entityData.set(DATA_ARMOR, this.inventory.getItem(INV_SLOT_ARMOR));
        this.entityData.set(DATA_CROWN_ID, this.inventory.getItem(INV_SLOT_CROWN));
    }

    private record RotationSnapshot(float yaw, float bodyYaw, float headYaw) {}

    @Override
    public void tick() {
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

        if (tickCount % 40 == 0 && !this.level().isClientSide) {
            this.entityData.set(DATA_ARMOR, this.inventory.getItem(INV_SLOT_ARMOR));
        }

        if (this.level().isClientSide && getArmorProgress() > 0 && this.inventory.getItem(INV_SLOT_ARMOR).getItem().asItem() == Items.AIR.asItem()) {
            this.inventory.setItem(INV_SLOT_ARMOR, getArmor());
        }

        // Capture rotation snapshot for Hard Lock (Ported from 1.21.1 setup)
        RotationSnapshot snapshot = new RotationSnapshot(this.getYRot(), this.yBodyRot, this.yHeadRot);

        updateModifiers(); // Update speed modifiers every tick for instant acceleration response
        handleStationaryState();

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_STATIONARY_TICKS, stationaryTicks);
        }

        // Core entity tick (ONLY ONCE)
        super.tick();

        // Apply Hard Lock if stationary to prevent snapping caused by AI combat/movement
        // BREATHING COMPATIBILITY: Do NOT lock pitch if the whale is trying to breach/breathe!
        boolean isBreachingAction = false;
        // Seek the goal if present
        for (net.minecraft.world.entity.ai.goal.WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof HullbackBreathAirGoal breachGoal) {
                if (breachGoal.isBreaching()) isBreachingAction = true;
            }
        }

        applyEnhancedPitchControl(snapshot, isBreachingAction);



        // Discard logic: Only if NOT stationary (moving freely) AND not approaching
        if (stationaryTicks == 0 && !scanPlayerAbove() && !isApproachingPlayer()) {
            if (this.moving_nose != null) { this.moving_nose.discard(); this.moving_nose = null; }
            if (this.moving_head != null) { this.moving_head.discard(); this.moving_head = null; }
            if (this.moving_body != null) { this.moving_body.discard(); this.moving_body = null; }
        }

        if (!this.isEyeInFluidType(Fluids.WATER.getFluidType()))
            mouthTarget = 1;

        if (!isSaddled() && !level().isClientSide) {
            if (!getPassengers().isEmpty()) {
                if (!getInventory().getItem(INV_SLOT_CROWN).isEmpty()) {
                    spawnAtLocation(getInventory().getItem(INV_SLOT_CROWN));
                    getInventory().setItem(INV_SLOT_CROWN, ItemStack.EMPTY);
                }
                ejectPassengers();
            }
        } else {
            if (getArmorProgress() < 0.45f && getInventory().getItem(INV_SLOT_ARMOR).getCount() < 64 && !level().isClientSide) {
                if (!getInventory().getItem(INV_SLOT_CROWN).isEmpty()) {
                    spawnAtLocation(getInventory().getItem(INV_SLOT_CROWN));
                    getInventory().setItem(INV_SLOT_CROWN, ItemStack.EMPTY);
                }
                ejectPassengers();
            }
        }

        setOldPosAndRots();

        updatePartPositions();
        rotatePassengers();

        if (this.getSubEntities()[1].isEyeInFluidType(Fluids.WATER.getFluidType()) && this.tickCount % 80 == 0)
            this.heal(0.25f);

        if (this.getDeltaMovement().length() > 0.3) {
            mouthTarget = 0.8f;
        }

        updatePlatformHeight();
        // REMOVED: Unconditional updateWalkablePlatforms() (it was forcing phantom spawns)
        updatePlayerStability();

        Set<Entity> movedThisTick = new HashSet<>();
        for (int i = 0; i < getSubEntities().length; i++) {
            moveEntitiesOnTop(i, movedThisTick);
        }

        updateMouthOpening();

        if (this.level().isClientSide && this.getControllingPassenger() instanceof Player player && player.isLocalPlayer()) {
            boolean shouldBeVector = isVectorControlActiveClient();
            if (this.entityData.get(DATA_VECTOR_CONTROL) != shouldBeVector) {
                WhaleborneNetwork.INSTANCE.sendToServer(new com.fruityspikes.whaleborne.network.ToggleControlPayload(shouldBeVector));
            }
        }

        if (this.tickCount % 80 == 0)
            mouthTarget = 0;

        // Updates the previous height for the delta calculation in the next tick
        this.oldPlatformHeight = this.currentPlatformHeight;
        if (this.tickCount % 20 == 0)
            validateAssignments();

        if (tickCount == 10)
            syncDirtToClients();

        yHeadRot = yBodyRot + (partYRot[0] - partYRot[4]) * 1.5f;

        if (isTamed() && partPosition != null && partYRot != null && partXRot != null) {
            seats[0] = partPosition[0].add((seatOffsets[0]).xRot(partXRot[1] * Mth.DEG_TO_RAD).yRot(-partYRot[1] * Mth.DEG_TO_RAD));
            seats[1] = partPosition[0].add((seatOffsets[1]).xRot(partXRot[1] * Mth.DEG_TO_RAD).yRot(-partYRot[1] * Mth.DEG_TO_RAD));
            seats[2] = partPosition[2].add((seatOffsets[2]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[3] = partPosition[2].add((seatOffsets[3]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[4] = partPosition[2].add((seatOffsets[4]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[5] = partPosition[2].add((seatOffsets[5]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[6] = partPosition[4].add((seatOffsets[6]).xRot(partXRot[4] * Mth.DEG_TO_RAD).yRot(-partYRot[4] * Mth.DEG_TO_RAD));


            oldSeats[0] = oldPartPosition[0].add((seatOffsets[0]).xRot(oldPartXRot[1] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[1] * Mth.DEG_TO_RAD));
            oldSeats[1] = oldPartPosition[0].add((seatOffsets[1]).xRot(oldPartXRot[1] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[1] * Mth.DEG_TO_RAD));
            oldSeats[2] = oldPartPosition[2].add((seatOffsets[2]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[3] = oldPartPosition[2].add((seatOffsets[3]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[4] = oldPartPosition[2].add((seatOffsets[4]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[5] = oldPartPosition[2].add((seatOffsets[5]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[6] = oldPartPosition[4].add((seatOffsets[6]).xRot(oldPartXRot[4] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[4] * Mth.DEG_TO_RAD));
        }

        if (!level().isClientSide) {
            randomTickDirt(headDirt, true, getPartName(head));
            randomTickDirt(bodyDirt, true, getPartName(body));
            randomTickDirt(tailDirt, true, getPartName(tail));
            randomTickDirt(flukeDirt, true, getPartName(fluke));

            if (!isTamed()) {
                randomTickDirt(headTopDirt, false, getPartName(head));
                randomTickDirt(bodyTopDirt, false, getPartName(body));
            } else {
                for (int x = 0; x < headTopDirt.length; x++) {
                    for (int y = 0; y < headTopDirt[x].length; y++) {
                        headTopDirt[x][y] = Blocks.AIR.defaultBlockState();
                    }
                }
                for (int x = 0; x < bodyTopDirt.length; x++) {
                    for (int y = 0; y < bodyTopDirt[x].length; y++) {
                        bodyTopDirt[x][y] = Blocks.AIR.defaultBlockState();
                    }
                }
            }
        }

        for (int i = 0; i < subEntities.length; i++) {
            subEntities[i].tick();

            if (i == 2 || i == 4) {
                float offset = 0;
                if (this.level().isClientSide && this.isInWater() && this.getDeltaMovement().length() > 0.03) {
                    for (int side : new int[]{-1, 1}) {
                        if (i == 2)
                            offset = 4;
                        Vec3 particlePos = partPosition[i].add(new Vec3((offset + subEntities[i].getSize().width / 2) * side, 0, subEntities[i].getSize().width / 2).yRot(partYRot[i]));
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

        // FINAL FIX: Removed the "12 blocks" check within tick().
        // Platform appearance now depends EXCLUSIVELY on stationaryTicks being > 0.
        // The stationaryTicks, in turn, is only set in handleStationaryState if the whale ACTUALLY stops or gets close.
        if (this.getStationaryTicks() > 0) {
            updateWalkablePlatforms();
        }
    }



    private void handleStationaryState() {
        // Maximum priority: If breathing, it doesn't stop.
        if (this.isBreaching()) return;

        LivingEntity currentPilot = getControllingPassenger();
        
        if (!wasPilotControlled && currentPilot != null) {
            this.isApproachingPlayer = false;
        }
        
        // Manage pilot -> dismounted transition (PORTED FROM 1.21.1)
        if (currentPilot != null) {
            this.stationaryTicks = 0; // Fix: Immediate acceleration when mounted (Fixes Speed Lag)
            this.wasPilotControlled = true;
            this.platformsStable = false;
        } else if (this.wasPilotControlled) {
            // Increase initial time after dismount
            stationaryTicks = 200; 
            this.wasPilotControlled = false;
            this.platformsStable = false;
        }
        wasPilotControlled = currentPilot != null;

        // CONFLICT LOGIC FIXED:
        boolean playerDetectedOnTop = scanPlayerAbove();
        
        if (currentPilot == null && playerDetectedOnTop) {
            if (this.isApproachingPlayer) {
                // If the approach AI (scissor) is active:
                // ONLY accepts turning into a statue if the whale has already arrived (nearly zero speed).
                // If it's still fast, IGNORE the player and continue swimming.
                if (this.getDeltaMovement().horizontalDistance() < 0.2) {
                    stationaryTicks = Math.max(this.stationaryTicks, 60);
                    this.setApproachingPlayer(false); // Arrived at destination, turns off approach.
                }
            } else {
                // Default behavior (no tool in hand)
                stationaryTicks = Math.max(this.stationaryTicks, 60);
            }
        }

        // Counter execution
        if (stationaryTicks > 0) {
            // If still marked as "approaching", it DOES NOT stop movement,
            // allows the AI to finish the path smoothly.
            if (!isApproachingPlayer) {
                stopMoving();
            }
            stationaryTicks--;
        }
        
        // Platform stabilization (Only activates if it actually stops)
        if (stationaryTicks > 10 && Math.abs(this.getDeltaMovement().y) < 0.005) { 
            this.platformsStable = true;
        } else {
            this.platformsStable = false;
        }
    }

    private void applyEnhancedPitchControl(RotationSnapshot snapshot, boolean isBreaching) {
        boolean shouldLockPitch = false;
          
        // Hard Lock Conditions: Anchored OR (Stationary AND Stable AND Not Approaching)
        if (this.hasAnchorDown()) {
            shouldLockPitch = true;
        } else if (this.getStationaryTicks() > 0 && this.platformsStable && !this.isApproachingPlayer) {
             shouldLockPitch = true;
        }
        
        if (shouldLockPitch && !isBreaching) {
            // Restore rotation from snapshot to prevent spinning
            this.setYRot(snapshot.yaw());
            this.setYBodyRot(snapshot.bodyYaw());
            this.setYHeadRot(snapshot.headYaw());
            this.yRotO = snapshot.yaw();
            this.yBodyRotO = snapshot.bodyYaw();
            this.yHeadRotO = snapshot.headYaw();
            
            this.pitchLocked = true;
            super.setXRot(0f);
            this.xRotO = 0f;
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else {
            this.pitchLocked = false;
        }
    }

    public void setMouthTarget(float mouthTarget) {
        this.mouthTarget = mouthTarget;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (HullbackEntity.this.stationaryTicks > 0) return;
                super.clientTick();
            }
        };
    }

    @Override
    public void onPlayerJump(int i) {

    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void handleStartJump(int i) {

    }

    @Override
    public void handleStopJump() {

    }

    // PASSENGERS
    @Override
    public boolean canAddPassenger(Entity passenger) {
        return (getPassengers().size() < 7);
    }

    private int getPartIndexForSeat(int seatIndex) {
        if (seatIndex == 0 || seatIndex == 1) {
            return 0; // Head
        } else if (seatIndex >= 2 && seatIndex <= 5) {
            return 2; // Body
        } else if (seatIndex == 6) {
            return 4; // Fluke
        }
        return 2; // Default to body if unknown
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;

        int seatIndex = getSeatByEntity(passenger);
        if (seatIndex == -1) {
            return;
        }

        float yOffset = 0;
        if (this.getArmorProgress() == 0)
            yOffset = 0.5F;

        if (seatIndex < seats.length) {
            if (seats[seatIndex] != null)
                callback.accept(passenger,
                        seats[seatIndex].x,
                        seats[seatIndex].y - yOffset + passenger.getMyRidingOffset(),
                        seats[seatIndex].z);
        }
    }

    private void updateModifiers() {
        double speedModifier = 0.0;

        // 1. Calculate potential speed from sails
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof SailEntity sail) {
                speedModifier += sail.getSpeedModifier();
            }
        }

        // 2. Enforce Pilot Requirement (HELM ONLY)
        // Check if the controller is actually at a HelmEntity to apply sail bonus
        LivingEntity pilot = this.getControllingPassenger();
        if (pilot == null || !(pilot.getVehicle() instanceof HelmEntity)) {
            speedModifier = 0.0;
        }

        AttributeInstance inst = this.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get());
        if (inst != null) {
            // 3. Always remove the old modifier first to ensure a clean state
            if (inst.getModifier(SAIL_SPEED_MODIFIER_UUID) != null) {
                inst.removeModifier(SAIL_SPEED_MODIFIER_UUID);
            }

            // 4. Only add the new modifier if it's non-zero
            if (speedModifier > 0.0) {
                inst.addPermanentModifier(new AttributeModifier(
                        SAIL_SPEED_MODIFIER_UUID,
                        Whaleborne.MODID + ":sail_speed_modifier",
                        speedModifier,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }
    }

    public static Attribute getSwimSpeed() {
        return ForgeMod.SWIM_SPEED.isPresent() ? ForgeMod.SWIM_SPEED.get() : Attributes.MOVEMENT_SPEED;
    }

    public int getSeatByEntity(Entity entity) {
        if (entity != null) {
            for (int seatIndex = 0; seatIndex < 7; seatIndex++) {
                Optional<UUID> seatOccupant = this.entityData.get(getSeatAccessor(seatIndex));
                if (seatOccupant.isPresent() && seatOccupant.get().equals(entity.getUUID())) {
                    return seatIndex;
                }
            }
        }
        return -1;
    }

    private void validateAssignments() {
        Set<UUID> currentPassengerUUIDs = this.getPassengers().stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());

        for (int seatIndex = 0; seatIndex < 7; seatIndex++) {
            EntityDataAccessor<Optional<UUID>> seatAccessor = getSeatAccessor(seatIndex);
            Optional<UUID> assignedUUID = this.entityData.get(seatAccessor);

            if (assignedUUID.isPresent()) {
                UUID uuid = assignedUUID.get();

                if (!currentPassengerUUIDs.contains(uuid)) {
                    boolean shouldClear = true;

                    // "Smart" Check & Active Recovery
                    if (this.level() instanceof ServerLevel serverLevel) {
                        Entity passengerEntity = serverLevel.getEntity(uuid);

                        if (passengerEntity == null) {
                            // Entity still loading or dead. Wait up to 60s.
                            if (this.tickCount < 1200) {
                                shouldClear = false;
                            }
                        } else {
                            if (passengerEntity.isAlive()) {
                                Vec3 seatPos = seats[seatIndex]; // Current seat position
                                // Teleport to sync position before mounting to avoid weird interpolation
                                passengerEntity.teleportTo(seatPos.x, seatPos.y, seatPos.z);

                                // Force re-mount
                                passengerEntity.startRiding(this, true);

                                // Do NOT clear the seat, we just fixed it.
                                shouldClear = false;
                            }
                        }
                    }

                    if (shouldClear) {
                        //Whaleborne.LOGGER.debug("Clearing seat assignment: seat {} for {}", seatIndex, uuid);
                        this.entityData.set(seatAccessor, Optional.empty());
                    }
                } else {
                    Entity passenger = getEntityByUUID(uuid);
                    if (passenger != null && getSeatByEntity(passenger) != seatIndex) {
                        //Whaleborne.LOGGER.debug("Correcting mismatched seat assignment: {} was in seat {} but belongs in {}",
                        //passenger, seatIndex, getSeatByEntity(passenger));
                        this.entityData.set(seatAccessor, Optional.empty());
                    }
                }
            }
        }
    }

    public void assignSeat(int seatIndex, @Nullable Entity passenger) {
        if (passenger == null) {
            this.entityData.set(getSeatAccessor(seatIndex), Optional.empty());
        } else {
            this.entityData.set(getSeatAccessor(seatIndex), Optional.of(passenger.getUUID()));
        }
        if (this.level() instanceof ServerLevel) {
            PacketDistributor.TRACKING_ENTITY.with(() -> this)
                    .send(new ClientboundSetPassengersPacket(this));
        }
    }

    public Optional<Entity> getPassengerForSeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= 7) return Optional.empty();

        return this.entityData.get(getSeatAccessor(seatIndex))
                .flatMap(uuid -> this.getPassengers().stream()
                        .filter(p -> p.getUUID().equals(uuid))
                        .findFirst());
    }

    private boolean isPassengerAssigned(Entity passenger) {
        for (int i = 0; i < 7; i++) {
            Optional<UUID> seatUUID = this.entityData.get(getSeatAccessor(i));
            if (seatUUID.isPresent() && seatUUID.get().equals(passenger.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private int findFreeSeat() {
        for (int i = 0; i < 7; i++) {
            if (this.entityData.get(getSeatAccessor(i)).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (this.level() instanceof ServerLevel) {
            PacketDistributor.TRACKING_ENTITY.with(() -> this)
                    .send(new ClientboundSetPassengersPacket(this));
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if (passenger instanceof Player)
            stationaryTicks = 100;
        if (passenger.isRemoved())
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

            if (seatIndex >= 0 && seatIndex < seats.length) {
                Vec3 seatPos = seats[seatIndex];
                return new Vec3(seatPos.x, seatPos.y, seatPos.z);
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    public void rotatePassengers() {
        for (Entity passenger : getPassengers()) {
            int seat = getSeatByEntity(passenger);
            int partIndex;
            float offset = 0;
            if (seat == 0 || seat == 1) {
                partIndex = 0;
            } else if (seat == 2 || seat == 4) {
                partIndex = 2;
                offset = -1;
            } else if (seat == 3 || seat == 5) {
                partIndex = 2;
                offset = 1;
            } else if (seat == 6) {
                partIndex = 4;
            } else {
                continue;
            }

            if (!(passenger instanceof Player)) {
                if (!(passenger instanceof CannonEntity cannonEntity && cannonEntity.isVehicle())) {
                    if (passenger instanceof SailEntity) {
                        passenger.yRotO = (Mth.rotLerp((float) (0.05 + 0.1 * partIndex), passenger.getYRot(), oldPartYRot[partIndex]) + offset);
                        passenger.xRotO = (Mth.rotLerp((float) (0.05 + 0.1 * partIndex), passenger.getXRot(), oldPartXRot[partIndex]));
                        passenger.setYRot(Mth.rotLerp((float) (0.05 + 0.1 * partIndex), passenger.getYRot(), partYRot[partIndex]) + offset);
                        passenger.setXRot(Mth.rotLerp((float) (0.05 + 0.1 * partIndex), passenger.getXRot(), partXRot[partIndex]));
                    } else {
                        passenger.yRotO = oldPartYRot[partIndex];
                        passenger.xRotO = oldPartXRot[partIndex];
                        passenger.setYRot((partYRot[partIndex]) + offset);
                        passenger.setXRot(partXRot[partIndex]);
                    }
                }
            }
        }
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

            if (entity.getUUID().equals(uuid))
                return entity;
        }
        return null;
    }

    private void syncMovement() {
        if (!this.level().isClientSide) {
            PacketDistributor.TRACKING_ENTITY.with(() -> this)
                    .send(new ClientboundSetEntityMotionPacket(this.getId(), this.getDeltaMovement()));
        }
    }


    public void setVectorControl(boolean active) {
        this.entityData.set(DATA_VECTOR_CONTROL, active);
    }


    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) (this.isHullbackInWater() ? this.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()) : this.getAttributeValue(Attributes.MOVEMENT_SPEED));
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
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
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

    protected class StationaryAwareMoveControl extends MoveControl {
        private final int maxTurnX = 1;
        private final int maxTurnY = 2;
        private final float inWaterSpeedModifier = 1.0F;
        private final float outsideWaterSpeedModifier = 1.0F;
        private final boolean applyGravity = true;

        public StationaryAwareMoveControl(HullbackEntity entity) {
            super(entity);
        }

        @Override
        public void tick() {
            // INTEGRATED FIX: Allow movement if approaching player, even if stationary
            if ((HullbackEntity.this.getStationaryTicks() > 0 && !HullbackEntity.this.isApproachingPlayer()) || HullbackEntity.this.pitchLocked) {
                this.mob.setSpeed(0.0F);
                this.mob.setZza(0.0F);
                this.mob.setYya(0.0F);
                // Also force XRot to 0 if locked, though setXRot override handles most of it
                if (HullbackEntity.this.pitchLocked) {
                    HullbackEntity.this.setXRot(0f);
                }
                return;
            }

            if (this.applyGravity && this.mob.isInWater()) {
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
            }

            if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
                double d0 = this.wantedX - this.mob.getX();
                double d1 = this.wantedY - this.mob.getY();
                double d2 = this.wantedZ - this.mob.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                if (d3 < 2.5000003E-7F) {
                    this.mob.setZza(0.0F);
                } else {
                    float f = (float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F;
                    this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, (float)this.maxTurnY));
                    this.mob.yBodyRot = this.mob.getYRot();
                    this.mob.yHeadRot = this.mob.getYRot();

                    // CRITICAL: Use SWIM_SPEED if in water, else MOVEMENT_SPEED
                    double baseSpeed = this.mob.isInWater() ?
                            this.mob.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()) :
                            this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);

                    float f1 = (float)(this.speedModifier * baseSpeed);

                    if (this.mob.isInWater()) {
                        this.mob.setSpeed(f1 * this.inWaterSpeedModifier);
                        double d4 = Math.sqrt(d0 * d0 + d2 * d2);
                        if (Math.abs(d1) > 1.0E-5F || Math.abs(d4) > 1.0E-5F) {
                            float f3 = -((float)(Mth.atan2(d1, d4) * 180.0F / (float)Math.PI));
                            
                            // Limit pitch angle during AI movement to prevent excessive tilting
                            f3 = Mth.clamp(f3, -20f, 20f);

                            f3 = Mth.clamp(Mth.wrapDegrees(f3), (float)(-this.maxTurnX), (float)this.maxTurnX);
                            this.mob.setXRot(this.rotlerp(this.mob.getXRot(), f3, 5.0F));
                        }

                        float f6 = Mth.cos(this.mob.getXRot() * (float) (Math.PI / 180.0));
                        float f4 = Mth.sin(this.mob.getXRot() * (float) (Math.PI / 180.0));
                        this.mob.zza = f6 * f1;
                        this.mob.yya = -f4 * f1;
                    } else {
                        float f5 = Math.abs(Mth.wrapDegrees(this.mob.getYRot() - f));
                        float f2 = getTurningSpeedFactor(f5);
                        this.mob.setSpeed(f1 * this.outsideWaterSpeedModifier * f2);
                    }
                }
            } else {
                this.mob.setSpeed(0.0F);
                this.mob.setXxa(0.0F);
                this.mob.setYya(0.0F);
                this.mob.setZza(0.0F);
            }
        }

        private float getTurningSpeedFactor(float p_249853_) {
            return 1.0F - Mth.clamp((p_249853_ - 10.0F) / 50.0F, 0.0F, 1.0F);
        }
    }

    protected class StationaryAwareLookControl extends LookControl {
        public StationaryAwareLookControl(HullbackEntity entity) {
            super(entity);
        }

        @Override
        public void tick() {
            if (HullbackEntity.this.getStationaryTicks() > 0) {
                return;
            }
            super.tick();
        }
    }

    class HullbackBodyRotationControl extends BodyRotationControl {
        private final HullbackEntity hullback;

        public HullbackBodyRotationControl(HullbackEntity hullback) {
            super(hullback);
            this.hullback = hullback;
        }

        @Override
        public void clientTick() {
            if (this.hullback.getStationaryTicks() > 0) return;
            super.clientTick();
        }
    }

    class HullbackRandomSwimGoal extends RandomSwimmingGoal {
        private static final int HORIZONTAL_RANGE = 10;
        private static final int VERTICAL_RANGE = 10;
        private static final float FRONT_ANGLE = 45.0f;
        private static final int STUCK_TIMEOUT = 100;
        private static final double MIN_DISTANCE = 2.0;

        private final HullbackEntity mob;
        private int stuckTimer = 0;
        private Vec3 lastPosition = Vec3.ZERO;
        private Vec3 currentTarget = null;

        public HullbackRandomSwimGoal(HullbackEntity mob, double speed, int interval) {
            super(mob, speed, interval);
            this.mob = mob;
        }

        @Override
        public boolean canUse() {

            if (mob.hasAnchorDown()) {
                return false;
            }

            if (!this.forceTrigger) {
                if (this.mob.getNoActionTime() >= 200) {
                    return false;
                }

                if (this.mob.getRandom().nextInt(reducedTickDelay(this.interval)) != 0) {
                    return false;
                }
            }

            Vec3 vec3 = this.getPosition();
            if (vec3 == null) {
                return false;
            } else {
                this.wantedX = vec3.x;
                this.wantedY = vec3.y;
                this.wantedZ = vec3.z;
                this.forceTrigger = false;
                this.currentTarget = getPosition();
                this.stuckTimer = 0;
                this.lastPosition = mob.position();
                this.mob.setNoActionTime(0);
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (currentTarget == null) {
                return false;
            }

            Vec3 currentPos = mob.position();
            if (currentPos.distanceTo(lastPosition) < 0.5) {
                stuckTimer++;
            } else {
                stuckTimer = 0;
            }
            lastPosition = currentPos;

            return stuckTimer < STUCK_TIMEOUT &&
                    currentPos.distanceTo(currentTarget) > MIN_DISTANCE &&
                    !mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (currentTarget != null) {
                mob.getNavigation().moveTo(currentTarget.x, currentTarget.y, currentTarget.z, speedModifier);
            }
        }

        @Override
        public void stop() {
            super.stop();
            currentTarget = null;
        }

        @Override
        public void tick() {
            super.tick();

            if (!((HullbackEntity)mob).nose.isEyeInFluidType(Fluids.WATER.getFluidType())) {
                 mob.setDeltaMovement(mob.getDeltaMovement().add(0, -0.1, 0));
            }

            if (currentTarget != null && mob.getNavigation().isDone() && mob.position().distanceTo(currentTarget) > MIN_DISTANCE) {
                mob.getNavigation().moveTo(currentTarget.x, currentTarget.y, currentTarget.z, speedModifier);
            }
            if (this.mob.getDeltaMovement().length() > 0.5) mouthTarget = 1;
        }

        protected Vec3 getPosition() {
            Vec3 target = Vec3.ZERO;
            target = mob.getRandom().nextFloat() < 0.7f ?
                    findPositionInFront() :
                    BehaviorUtils.getRandomSwimmablePos(mob, HORIZONTAL_RANGE, VERTICAL_RANGE);
            return target == null ? mob.position() : target;
        }

        private Vec3 findPositionInFront() {
            Vec3 lookAngle = mob.getLookAngle();
            Vec3 mobPos = mob.position();

            for (int i = 0; i < 10; i++) {
                float angle = mob.getRandom().nextFloat() * FRONT_ANGLE * 2 - FRONT_ANGLE;
                Vec3 direction = lookAngle.yRot((float) Math.toRadians(angle));

                float distance = HORIZONTAL_RANGE;
                Vec3 targetPos = mobPos.add(direction.scale(distance));

                targetPos = targetPos.add(0, mob.getRandom().nextFloat() * VERTICAL_RANGE * 2 - VERTICAL_RANGE, 0);

                if (isSwimmablePos(mob, targetPos)) {
                    return targetPos;
                }
            }
            return BehaviorUtils.getRandomSwimmablePos(mob, HORIZONTAL_RANGE, VERTICAL_RANGE);
        }

        private boolean isSwimmablePos(PathfinderMob entity, Vec3 targetPos) {
            return entity.level().getBlockState(BlockPos.containing(targetPos))
                    .isPathfindable(entity.level(), BlockPos.containing(targetPos), PathComputationType.WATER);
        }
    }

    public class HullbackBreathAirGoal extends Goal {
        private static final int BREACH_HEIGHT = 10; // Blocks above surface to breach
        private static final float BREACH_SPEED = 1.2f;
        private static final float ROTATION_SPEED = 10f;

        private final HullbackEntity hullback;
        private int breachCooldown = 0;
        private boolean isBreachingGoal = false; // Renamed to avoid confusion with field
        private Vec3 initialPos;

        public HullbackBreathAirGoal(HullbackEntity hullback) {
            this.hullback = hullback;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // FIX: Allows surfacing if air is critical, even if stationaryTicks > 0
            if(hullback.stationaryTicks > 0 && this.hullback.getAirSupply() > 100) return false;

            for (net.minecraft.world.entity.Entity passenger : hullback.getPassengers()) {
                if (passenger instanceof net.minecraft.world.entity.player.Player) {
                    return false;
                }
            }
            if (breachCooldown > 0) {
                breachCooldown--;
                return false;
            }
            return this.hullback.getAirSupply() < this.hullback.getMaxAirSupply() * 0.2;
        }

        @Override
        public boolean canContinueToUse() {
            return (this.hullback.getAirSupply() < this.hullback.getMaxAirSupply() ||
                    !this.hullback.isInWater());
        }

        @Override
        public void start() {
            this.isBreachingGoal = true;
            this.hullback.setBreaching(true);
            this.initialPos = this.hullback.position();
            this.hullback.getNavigation().stop();

            int surfaceY = this.hullback.level().getSeaLevel();
            Vec3 breachTarget = new Vec3(
                    this.hullback.getX(),
                    surfaceY + BREACH_HEIGHT,
                    this.hullback.getZ()
            );

            this.hullback.getMoveControl().setWantedPosition(
                    breachTarget.x,
                    breachTarget.y,
                    breachTarget.z,
                    BREACH_SPEED
            );
        }

        @Override
        public void tick() {
            super.tick();

            float targetXRot = -60f;
            this.hullback.setXRot(Mth.rotLerp(ROTATION_SPEED * 0.1f, this.hullback.getXRot(), targetXRot));

            if (this.hullback.isInWater()) {
                float yRotRad = this.hullback.getYRot() * (float) (Math.PI / 180.0);
                
                this.hullback.setDeltaMovement(new Vec3(0, 1.2, 0.8).yRot(-yRotRad));
            }

            if (this.hullback.getY() >= this.hullback.level().getSeaLevel() &&
                    this.hullback.level().isClientSide) {
                for (int i = 0; i < 5; i++) {
                    this.hullback.level().addParticle(ParticleTypes.SPLASH,
                            this.hullback.getX() + (this.hullback.getRandom().nextFloat() - 0.5f) * 3f,
                            this.hullback.level().getSeaLevel(),
                            this.hullback.getZ() + (this.hullback.getRandom().nextFloat() - 0.5f) * 3f,
                            0, 0.5, 0);
                }
            }
        }

        @Override
        public void stop() {
            this.isBreachingGoal = false;
            this.hullback.setBreaching(false);
            this.breachCooldown = 200;

            this.hullback.setAirSupply(this.hullback.getMaxAirSupply());

            if (this.hullback.level().isClientSide) {
                if (this.hullback.getPartPos(2) != null) {
                    for (int i = 0; i < 20; i++) {
                        this.hullback.level().addParticle(ParticleTypes.BUBBLE,
                                this.hullback.getPartPos(2).x,
                                this.hullback.getPartPos(2).y,
                                this.hullback.getPartPos(2).z,
                                (this.hullback.getRandom().nextFloat() - 0.5f) * 0.5f,
                                this.hullback.getRandom().nextFloat() * 0.5f,
                                (this.hullback.getRandom().nextFloat() - 0.5f) * 0.5f);
                    }
                }
                this.hullback.setMouthTarget(0.0f);
            }

            if (this.hullback.getPartPos(1) != null) {
                Vec3 particlePos = this.hullback.getPartPos(1).add(new Vec3(0, 7, 0));
                double x = particlePos.x;
                double y = particlePos.y;
                double z = particlePos.z;

                if (this.hullback.level() instanceof ServerLevel serverLevel) {
                      // Check registry existence to avoid error
                      if (com.fruityspikes.whaleborne.server.registries.WBParticleRegistry.SMOKE != null) {
                        serverLevel.sendParticles(
                                com.fruityspikes.whaleborne.server.registries.WBParticleRegistry.SMOKE.get(),
                                x,
                                y,
                                z,
                                50,
                                0.2,
                                0.2,
                                0.2,
                                0.02
                        );
                    }
                }
            }

            this.hullback.playSound(com.fruityspikes.whaleborne.server.registries.WBSoundRegistry.HULLBACK_BREATHE.get(), 3.0f, 1);
            this.hullback.setXRot(Mth.rotLerp(0.1f, this.hullback.getXRot(), 0));
        }

        public boolean isBreaching() {
            return this.isBreachingGoal;
        }
    }


    public class HullbackArmorPlayerGoal extends Goal {
        private static final float APPROACH_DISTANCE = 6.0f; // Slightly increased to avoid crude collision
        private static final float SIDE_OFFSET = 5.0f;
        private static final float ROTATION_SPEED = 0.8f;
        private static Ingredient TEMPT_PLANKS = Ingredient.of(WBTagRegistry.HULLBACK_EQUIPPABLE);
        private static Ingredient TEMPT_WIDGETS = Ingredient.of(WBItemRegistry.SAIL.get(), WBItemRegistry.ANCHOR.get(), WBItemRegistry.MAST.get(), WBItemRegistry.HELM.get(), WBItemRegistry.CANNON.get());
        private final HullbackEntity hullback;
        private final float speedModifier;
        private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(40.0).ignoreLineOfSight(); // Increased range
        private final TargetingConditions targetingConditions;
        private Player targetPlayer;
        private boolean approachFromRight;
        private Vec3 targetPosition;

        public HullbackArmorPlayerGoal(HullbackEntity hullback, float speedModifier) {
            this.hullback = hullback;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.targetingConditions = TEMP_TARGETING.copy().selector(this::shouldFollow);
        }

        private boolean shouldFollow(LivingEntity entity) {
            if (isSaddled()) {
                if (getArmorProgress() >= 0.5 && getArmorProgress() < 1)
                    return TEMPT_PLANKS.test(entity.getMainHandItem()) || TEMPT_PLANKS.test(entity.getOffhandItem()) || TEMPT_WIDGETS.test(entity.getMainHandItem()) || TEMPT_WIDGETS.test(entity.getOffhandItem());
                if (getArmorProgress() == 1)
                    return TEMPT_WIDGETS.test(entity.getMainHandItem()) || TEMPT_WIDGETS.test(entity.getOffhandItem());
                return TEMPT_PLANKS.test(entity.getMainHandItem()) || TEMPT_PLANKS.test(entity.getOffhandItem());
            }
            return false;
        }

        @Override
        public boolean canUse() {
            if (hullback.hasAnchorDown()) return false;
            this.targetPlayer = this.hullback.level().getNearestPlayer(this.targetingConditions, this.hullback, 40, 50, 40);
            if (this.targetPlayer == null) return false;
            if (this.targetPlayer.isPassenger() && (this.targetPlayer.getVehicle().is(this.hullback) || (this.targetPlayer.getVehicle().isPassenger() && this.targetPlayer.getVehicle().getVehicle().is(this.hullback))))
                return false;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.hullback.setApproachingPlayer(true);
            Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
            Vec3 whaleRight = Vec3.directionFromRotation(0, hullback.getYRot() + 90);
            this.approachFromRight = toPlayer.dot(whaleRight) > 0;
            playSound(WBSoundRegistry.HULLBACK_HAPPY.get());
            this.hullback.mouthTarget = 0.1f;
        }

        @Override
        public void stop() {
            this.hullback.setApproachingPlayer(false);
            this.targetPlayer = null;
            this.targetPosition = null;
            this.hullback.setTarget(null);
            this.hullback.getNavigation().stop();
            this.hullback.mouthTarget = 0.0f;
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) return;

            this.hullback.mouthTarget = 0.6f;

            // Constantly recalculates target for smoothness
            Vec3 playerLook = this.targetPlayer.getLookAngle();
            Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();
            Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
            this.targetPosition = this.targetPlayer.position()
                    .add(sideOffset)
                    .add(playerLook.scale(-APPROACH_DISTANCE));

            double distSqr = this.hullback.position().distanceToSqr(targetPosition);

            // HYBRID NAVIGATION SYSTEM
            // If standard navigation fails (common on surface) or is too close/far, we use brute force.
            boolean pathFound = this.hullback.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, this.speedModifier);

            if (!pathFound || this.hullback.getNavigation().isDone()) {
                // Manual Vector "Push"
                Vec3 moveDir = targetPosition.subtract(this.hullback.position()).normalize();
                // Applies smooth speed in the target direction
                double speed = this.speedModifier * 0.15; // Manual speed fine tuning
                this.hullback.setDeltaMovement(this.hullback.getDeltaMovement().add(moveDir.scale(speed)));
                
                // Manual Rotation (Smooth Look)
                double d0 = targetPosition.x - this.hullback.getX();
                double d2 = targetPosition.z - this.hullback.getZ();
                float targetYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.hullback.setYRot(Mth.rotLerp(0.1f, this.hullback.getYRot(), targetYaw));
                this.hullback.yBodyRot = this.hullback.getYRot();
            }
        }
    }

    public class HullbackApproachPlayerGoal extends Goal {
        private static final float APPROACH_DISTANCE = 6.0f; 
        private static final float SIDE_OFFSET = 5.0f;
        
        private static Ingredient TEMPT_SADDLE = Ingredient.of(Items.SADDLE);
        private static Ingredient TEMPT_ITEMS = Ingredient.of(Items.SHEARS);
        private static Ingredient TEMPT_AXES = Ingredient.of(ItemTags.AXES);
        private final HullbackEntity hullback;
        private final float speedModifier;
        private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(40.0).ignoreLineOfSight();
        private final TargetingConditions targetingConditions;
        private Player targetPlayer;
        private boolean approachFromRight;
        private Vec3 targetPosition;

        public HullbackApproachPlayerGoal(HullbackEntity hullback, float speedModifier) {
            this.hullback = hullback;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.targetingConditions = TEMP_TARGETING.copy().selector(this::shouldFollow);
        }

        private boolean shouldFollow(LivingEntity entity) {
            if (isTamed() && !isSaddled())
                return TEMPT_SADDLE.test(entity.getMainHandItem()) || TEMPT_SADDLE.test(entity.getOffhandItem()) || TEMPT_ITEMS.test(entity.getMainHandItem()) || TEMPT_ITEMS.test(entity.getOffhandItem()) || TEMPT_AXES.test(entity.getMainHandItem()) || TEMPT_AXES.test(entity.getOffhandItem());
            return TEMPT_ITEMS.test(entity.getMainHandItem()) || TEMPT_ITEMS.test(entity.getOffhandItem()) || TEMPT_AXES.test(entity.getMainHandItem()) || TEMPT_AXES.test(entity.getOffhandItem());
        }

        @Override
        public boolean canUse() {
            if (hullback.hasAnchorDown()) return false;
            this.targetPlayer = this.hullback.level().getNearestPlayer(this.targetingConditions, this.hullback, 40, 50, 40);
            if (this.targetPlayer == null) return false;
            if (this.targetPlayer.isPassenger() && (this.targetPlayer.getVehicle().is(this.hullback) || (this.targetPlayer.getVehicle().isPassenger() && this.targetPlayer.getVehicle().getVehicle().is(this.hullback))))
                return false;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.hullback.setApproachingPlayer(true);
            Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
            Vec3 whaleRight = Vec3.directionFromRotation(0, hullback.getYRot() + 90);
            this.approachFromRight = toPlayer.dot(whaleRight) > 0;
            this.hullback.setTarget(this.targetPlayer);
            playSound(WBSoundRegistry.HULLBACK_HAPPY.get());
            this.hullback.mouthTarget = 0.2f;
        }

        @Override
        public void stop() {
            this.hullback.setApproachingPlayer(false);
            this.targetPlayer = null;
            this.targetPosition = null;
            this.hullback.setTarget(null);
            this.hullback.getNavigation().stop();
            this.hullback.mouthTarget = 0.0f;
        }

        @Override
        public void tick() {
            this.hullback.mouthTarget = 0.6f;
            if (this.targetPlayer == null) return;

            Vec3 playerLook = this.targetPlayer.getLookAngle();
            Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();
            Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
            this.targetPosition = this.targetPlayer.position()
                    .add(sideOffset)
                    .add(playerLook.scale(-APPROACH_DISTANCE));

            // HYBRID SYSTEM: Tries to navigate, if fails, pushes.
            boolean pathFound = this.hullback.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, this.speedModifier);

            if (!pathFound || this.hullback.getNavigation().isDone()) {
                // Manual vector calculation to ensure movement even on the surface
                Vec3 moveDir = targetPosition.subtract(this.hullback.position()).normalize();
                
                // Forces movement (This overcomes Pathfinding limitation on the surface)
                double speed = this.speedModifier * 0.15;
                this.hullback.setDeltaMovement(this.hullback.getDeltaMovement().add(moveDir.scale(speed)));

                // Forces visual rotation
                double d0 = targetPosition.x - this.hullback.getX();
                double d2 = targetPosition.z - this.hullback.getZ();
                float targetYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                
                this.hullback.setYRot(Mth.rotLerp(0.1f, this.hullback.getYRot(), targetYaw));
                this.hullback.yBodyRot = this.hullback.getYRot();
            }
        }
    }

    public class HullbackTryFindWaterGoal extends Goal {
        private final PathfinderMob mob;
        private final boolean isBeached;

        public HullbackTryFindWaterGoal(PathfinderMob mob, boolean isBeached) {
            this.mob = mob;
            this.isBeached = isBeached;
        }

        public boolean canUse() {
            if (isBeached)
                return mob.tickCount > 20 && (!this.mob.isEyeInFluidType(Fluids.WATER.getFluidType()) || !this.mob.level().getFluidState(mob.blockPosition().above(3)).is(FluidTags.WATER));
            return mob.tickCount > 20 && !mob.level().getFluidState(mob.blockPosition().below()).is(FluidTags.WATER);
        }

        public void start() {
            BlockPos blockpos = null;

            Iterator var2 = BlockPos.betweenClosed(Mth.floor(this.mob.getX() - (isBeached ? 20.0 : 5)), Mth.floor(this.mob.getY() - 2.0), Mth.floor(this.mob.getZ() - (isBeached ? 20.0 : 5)), Mth.floor(this.mob.getX() + (isBeached ? 20.0 : 5)), this.mob.getBlockY(), Mth.floor(this.mob.getZ() + (isBeached ? 20.0 : 5))).iterator();

            while (var2.hasNext()) {
                BlockPos blockpos1 = (BlockPos) var2.next();
                if (this.mob.level().getFluidState(blockpos1).is(FluidTags.WATER)) {
                    blockpos = blockpos1;
                    break;
                }
            }

            if (blockpos != null) {
                this.mob.getMoveControl().setWantedPosition((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ(), 1.0);
                this.mob.getLookControl().setLookAt(mob.getMoveControl().getWantedX(), mob.getMoveControl().getWantedY(), mob.getMoveControl().getWantedZ());
            }
        }

        @Override
        public void tick() {
            super.tick();

            Vec3 target = new Vec3(mob.getMoveControl().getWantedX(),
                    mob.getMoveControl().getWantedY(),
                    mob.getMoveControl().getWantedZ());
            float targetYRot = (float) Math.toDegrees(Math.atan2(target.z - mob.getZ(), target.x - mob.getX())) - 90;

            mob.setYRot(Mth.rotLerp(0.01f, mob.getYRot(), targetYRot));


            if (mob.tickCount % 10 == 0)
                mob.playSound(WBSoundRegistry.HULLBACK_MAD.get());

            if (mob.tickCount % 100 == 0 && !mob.level().getBlockState(mob.blockPosition().below()).isAir()) {

                ((HullbackEntity) mob).mouthTarget = 0;

                mob.getLookControl().setLookAt(target);
                mob.setYRot(Mth.rotLerp(0.1f, mob.getYRot(), targetYRot));
                mob.yBodyRot = mob.getYRot();
                Vec3 direction = target.subtract(mob.position()).normalize();

                double lungePower = 1.5; // Increased from 1.0 for better mobility
                Vec3 velocity = direction.scale(lungePower).add(0, 1.0, 0); // Increased Y from 0.5 to 0.7
                if (isBeached) {
                    mob.playSound(WBSoundRegistry.ORGAN.get(), 2, 2f);
                    mob.playSound(WBSoundRegistry.ORGAN.get(), 2, 1f);
                    mob.playSound(WBSoundRegistry.HULLBACK_HURT.get(), Config.SOUND_DISTANCE.get().floatValue(), 0.2f);
                    mob.playSound(WBSoundRegistry.HULLBACK_SWIM.get(), 2, 0.5f);
                    pushEntities();
                }
                mob.setDeltaMovement(velocity);

                mob.setYRot(Mth.rotLerp(0.2f, mob.getYRot(), targetYRot));
                mob.yBodyRot = mob.getYRot();
            }
        }

        public void pushEntities() {
            AABB pushArea = mob.getBoundingBox().inflate(20);
            List<Entity> pushableEntities = this.mob.level().getEntities(mob, pushArea);
            pushableEntities.removeIf(entity -> !entity.isPushable() && entity.isPassenger());

            Vec3 center = mob.position();
            double pushStrength = 3;

            for (Entity entity : pushableEntities) {

                Vec3 pushDir = entity.position().subtract(center).normalize();
                entity.push(
                        pushDir.x * pushStrength,
                        0.3,
                        pushDir.z * pushStrength
                );

                if (this.mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            WBParticleRegistry.SMOKE.get(),
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            10,
                            0.5,
                            0.5,
                            0.5,
                            0.02
                    );
                }

                if (entity instanceof LivingEntity living) {
                    living.knockback(0.5f, pushDir.x, pushDir.z);
                    living.hurtMarked = true;
                }
            }
        }

        @Override
        public void stop() {
            super.stop();
            if (isBeached) {
                this.mob.setAirSupply(this.mob.getMaxAirSupply());

                if (this.mob.level().isClientSide) {
                    for (int i = 0; i < 20; i++) {
                        this.mob.level().addParticle(ParticleTypes.BUBBLE,
                                ((HullbackEntity) mob).partPosition[2].x,
                                ((HullbackEntity) mob).partPosition[2].y,
                                ((HullbackEntity) mob).partPosition[2].z,
                                (this.mob.getRandom().nextFloat() - 0.5f) * 0.5f,
                                this.mob.getRandom().nextFloat() * 0.5f,
                                (this.mob.getRandom().nextFloat() - 0.5f) * 0.5f);
                    }
                    ((HullbackEntity) mob).mouthTarget = 0.0f;
                }

                Vec3 particlePos = partPosition[1].add(new Vec3(0, 7, 0));
                double x = particlePos.x;
                double y = particlePos.y;
                double z = particlePos.z;

                if (this.mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            WBParticleRegistry.SMOKE.get(),
                            x,
                            y,
                            z,
                            50,
                            0.2,
                            0.2,
                            0.2,
                            0.02
                    );
                }

                this.mob.playSound(WBSoundRegistry.HULLBACK_BREATHE.get(), Config.SOUND_DISTANCE.get().floatValue() * 1.5f, 1);
            }
        }
    }

    // --- Ported Logic from 1.21.1 for 1.20.1 (Definitive Fix) ---

    private boolean isVectorControlActive() {
        if (this.level().isClientSide) {
            return isVectorControlActiveClient();
        }
        return this.entityData.get(DATA_VECTOR_CONTROL);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isVectorControlActiveClient() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || this.getControllingPassenger() != mc.player) {
            return this.entityData.get(DATA_VECTOR_CONTROL);
        }

        if (IS_THIRD_PERSON_MOD_LOADED == null) {
            IS_THIRD_PERSON_MOD_LOADED = net.minecraftforge.fml.ModList.get().isLoaded("leawind_third_person");
        }

        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
        return IS_THIRD_PERSON_MOD_LOADED && !isFirstPerson;
    }

    public boolean isHullbackInWater() {
        return this.getFluidTypeHeight(net.minecraftforge.common.ForgeMod.WATER_TYPE.get()) > 0.4;
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isAlive()) {
            boolean isHullbackInWater = this.isInWater() || this.getFluidTypeHeight(net.minecraftforge.common.ForgeMod.WATER_TYPE.get()) > 0.1;

            // Stop horizontal movement if anchor is down or stationary (UNLESS BREACHING)
            if ((this.hasAnchorDown() || (this.getStationaryTicks() > 0)) && isHullbackInWater && !this.isBreaching()) {
                pTravelVector = Vec3.ZERO;
            }

            // --- BUOYANCY LOGIC (Ported from 1.21.1) ---
            boolean isBreachingAction = false;
            // Seek the goal if present
            for (net.minecraft.world.entity.ai.goal.WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
                if (goal.getGoal() instanceof HullbackBreathAirGoal breachGoal) {
                    if (breachGoal.isBreaching()) isBreachingAction = true;
                    if (breachGoal.isBreaching()) this.setBreaching(true); // Update breaching state
                }
            }

            boolean noseSubmerged = this.nose != null && this.nose.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get());

            if (isHullbackInWater && !this.isBreaching()) {
                this.setNoGravity(false);
                boolean isAtHelm = this.getControllingPassenger() != null && this.getControllingPassenger().getVehicle() instanceof HelmEntity;

                if (isAtHelm || (this.getStationaryTicks() > 0) || this.isTamed() || this.hasAnchorDown()) {
                    // HELM, STATIONARY, TAMED, ANCHORED: Maintain target depth
                    double targetY = this.level().getSeaLevel() - (isAtHelm ? 4.55 : 5.0);
                    double diff = targetY - this.getY();

                    // FIX: Aggressive Deadzone (Snap)
                    if (Math.abs(diff) < 0.05) {
                        this.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
                        this.setPos(this.getX(), targetY, this.getZ()); // Hard Snap
                    } else {
                        double verticalSpeed = Mth.clamp(diff * 0.2, -0.1, 0.1);
                        this.setDeltaMovement(this.getDeltaMovement().x, verticalSpeed, this.getDeltaMovement().z);
                    }
                } else {
                    // WILD / STATIONARY (WILD): Only push down if surfacing
                    if (!noseSubmerged) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05, 0));
                    }
                }
            }

            // PITCH STABILIZATION: Reset XRot to 0 if not controlled or ANCHORED/STATIONARY
            // Apply on both client and server for smooth stabilization
            // PITCH STABILIZATION: Reset XRot to 0 if not controlled or ANCHORED/STATIONARY
            // Apply on both client and server for smooth stabilization
            if (!isBreachingAction && (this.getControllingPassenger() == null || this.hasAnchorDown() || this.getStationaryTicks() > 0)) {
                if (this.hasAnchorDown() || this.getStationaryTicks() > 0) {
                    this.pitchLocked = true;
                    this.setXRot(0f); // Calls override which forces super.setXRot(0)
                    this.xRotO = 0f;
                } else {
                    float lerpSpeed = 0.08f;
                    this.setXRot(Mth.rotLerp(lerpSpeed, this.getXRot(), 0f));
                }
            } else {
                // If breaching or moving freely, ensure lock is released (though tick() also manages this)
                if (isBreachingAction) this.pitchLocked = false;
            }

            if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
                Vec3 input = this.getRiddenInput(player, pTravelVector);
                super.travel(input);
            } else {
                super.travel(pTravelVector);
            }

            // WIPE VERTICAL DRIFT IF AT HELM OR TAMED/STATIONARY IN DEADZONE
            if ((this.getControllingPassenger() != null || this.isTamed() || this.hasAnchorDown() || this.getStationaryTicks() > 0) && this.isInWater()) {
                double targetY = this.getControllingPassenger() != null ? this.level().getSeaLevel() - 4.55 : this.level().getSeaLevel() - 5.0;
                if (Math.abs(targetY - this.getY()) < 0.051) {
                    this.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
                    this.setPos(this.getX(), targetY, this.getZ());
                }
            }

            // EXTRA FRICTION ON LAND
            if (!isHullbackInWater && this.onGround()) {
                BlockState surface = this.getBlockStateOn();
                double friction = 0.3; // Standard land: Heavy

                if (surface.is(Blocks.ICE) || surface.is(Blocks.PACKED_ICE) || surface.is(Blocks.BLUE_ICE)) {
                    friction = 0.05; // Ice: Extremely locked down
                }

                this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 1.0, friction));
            }

            // Final anchor lock
            if (this.hasAnchorDown() && isHullbackInWater) {
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            }
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        boolean hasInput = Mth.abs(player.xxa) > 0 || Mth.abs(player.zza) > 0;

        if (hasInput) {
            if (hasAnchorDown()) {
                if (tickCount % 10 == 0) this.playSound(SoundEvents.WOOD_HIT, 1, 1);
                return Vec3.ZERO;
            }

            if (tickCount % 2 == 0) this.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.5f, 1.0f);

            if (getControllingPassenger() != null && getControllingPassenger().getVehicle() instanceof HelmEntity helmEntity) {
                helmEntity.setWheelRotation(helmEntity.getWheelRotation() + player.xxa / 10);
            }
        } else {
            if (getControllingPassenger() != null && getControllingPassenger().getVehicle() instanceof HelmEntity helmEntity) {
                helmEntity.setPrevWheelRotation(helmEntity.getWheelRotation());
            }
        }

        boolean vectorControl = isVectorControlActive();

        float xxa = player.xxa;
        float zza = player.zza;

        if (this.isInWater()) {
            float currentPitch = this.getXRot();
            float maxPitch = 25f; // Maximum tilt angle in degrees

            if (Math.abs(currentPitch) > maxPitch) {
                this.setXRot(Mth.clamp(currentPitch, -maxPitch, maxPitch));
                this.xRotO = this.getXRot();
            }
        }

        if (vectorControl) {
            // --- VECTOR MODE (3rd Person) ---
            if (hasInput) {
                float targetYaw = player.getYRot() - (float) (Mth.atan2(player.xxa, player.zza) * (180D / Math.PI));

                this.setYRot(Mth.rotLerp(0.05f, this.getYRot(), targetYaw));
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();

                zza = Mth.sqrt(xxa * xxa + zza * zza);
                xxa = 0;
            } else {
                zza = 0;
            }
        } else {
            // --- TANK MODE (1st Person) ---
            if (zza <= 0.0F) {
                zza *= 0.25F;
            }

            // Lateral input becomes ROTATION
            if (player.xxa != 0) {
                this.setYRot(Mth.rotLerp(0.8F, this.getYRot(), this.getYRot() - player.xxa));
                this.yBodyRot = this.getYRot();
            }

            xxa = 0; // Prevent drift
        }

        // Buoyancy is handled EXCLUSIVELY in travel() to prevent "up and down" quivering (1.21.1 Parity)
        // Ensure we don't return vertical force here
        return new Vec3(0, 0, zza);
    }

    private void randomTickDirt(BlockState[][] array, boolean bottom, String partName) {
        if (bottom) {
            if (this.random.nextInt(30000) <= 5) {
                int x = getWeightedIndex(array.length, true);
                int y = getWeightedIndex(array[x].length, true);

                BlockState currentState = array[x][y];
                HullbackDirtManager.HullbackDirtEntry entry = HullbackDirtManager.DATA.stream().filter(e -> e.matches(currentState)).findFirst().orElse(null);
                if (entry != null && entry.growth().isPresent() && this.random.nextBoolean()) {
                    array[x][y] = applyProperties(entry.growth().get(), entry.growthProperties());
                    this.level().playSound(null, getX(), getY(), getZ(), entry.soundOnGrowth() != null ? entry.soundOnGrowth() : SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    if (!this.level().isClientSide) syncDirtToClients();
                    return;
                }

                if (currentState == null || currentState.isAir()) {
                    List<HullbackDirtManager.HullbackDirtEntry> candidates = getCandidates(true, partName);
                    List<HullbackDirtManager.HullbackDirtEntry> successful = new ArrayList<>();

                    for (HullbackDirtManager.HullbackDirtEntry candidate : candidates) {
                        if (random.nextDouble() < candidate.placementChance()) {
                            successful.add(candidate);
                        }
                    }

                    if (!successful.isEmpty()) {
                        HullbackDirtManager.HullbackDirtEntry chosen = successful.get(random.nextInt(successful.size()));
                        array[x][y] = applyProperties(chosen.block(), chosen.blockProperties());

                        if (!this.level().isClientSide) {
                            this.level().playSound(null, getX(), getY(), getZ(), chosen.soundOnGrowth() != null ? chosen.soundOnGrowth() : SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                            syncDirtToClients();
                        }
                    }
                }
            }

        } else {
            if (this.random.nextInt(30000) <= 5) {
                int x = getWeightedIndex(array.length, true);
                int y = getWeightedIndex(array[x].length, true);

                BlockState currentState = array[x][y];

                HullbackDirtManager.HullbackDirtEntry entry = HullbackDirtManager.DATA.stream().filter(e -> e.matches(currentState)).findFirst().orElse(null);

                if (entry != null && entry.growth().isPresent() && this.random.nextBoolean()) {

                    array[x][y] = applyProperties(entry.growth().get(), entry.growthProperties());
                    if (entry.soundOnGrowth() != null) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), entry.soundOnGrowth(), SoundSource.NEUTRAL, 1.0F, 1.0f);
                    } else {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 1.0F, 1.0f);
                    }
                    if (!this.level().isClientSide) syncDirtToClients();
                    return;
                }

                if (currentState == null || currentState.isAir()) {
                    List<HullbackDirtManager.HullbackDirtEntry> candidates = getCandidates(false, partName);
                    if (!candidates.isEmpty()) {
                        HullbackDirtManager.HullbackDirtEntry candidate = candidates.get(random.nextInt(candidates.size()));
                        if (random.nextDouble() < candidate.placementChance()) {
                            array[x][y] = applyProperties(candidate.block(), candidate.blockProperties());
                            if (!this.level().isClientSide) {
                                if (candidate.soundOnGrowth() != null) {
                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                            candidate.soundOnGrowth(), SoundSource.NEUTRAL, 1.0F, 1.0f);
                                } else {
                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                            SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 1.0F, 1.0f);
                                }
                                syncDirtToClients();
                            }
                        }
                    }
                }
            }
        }
    }

    private int getWeightedIndex(int length, boolean higherWeight) {
        double[] weights = new double[length];
        double totalWeight = 0;

        for (int i = 0; i < length; i++) {
            if (higherWeight) {
                weights[i] = i + 1;
            } else {
                weights[i] = length - i;
            }
            totalWeight += weights[i];
        }

        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        for (int i = 0; i < length; i++) {
            cumulativeWeight += weights[i];
            if (randomValue <= cumulativeWeight) {
                return i;
            }
        }

        return length - 1;
    }

    private void updateMouthOpening() {
        mouthOpenProgress = (float) Mth.lerp(0.3, mouthOpenProgress, mouthTarget);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_MOUTH_PROGRESS, mouthOpenProgress);
        }
    }

    public float getMouthOpenProgress() {
        return this.entityData.get(DATA_MOUTH_PROGRESS);
    }

    public Vec3 getPartPos(int i) {
        return partPosition[i];
    }

    public float getPartYRot(int i) {
        return partYRot[i];
    }

    public float getPartXRot(int i) {
        return partXRot[i];
    }

    public Vec3 getOldPartPos(int i) {
        return oldPartPosition[i];
    }

    public float getOldPartYRot(int i) {
        return oldPartYRot[i];
    }

    public float getOldPartXRot(int i) {
        return oldPartXRot[i];
    }

    public void setOldPosAndRots() {
        for (int i = 0; i < 5; i++) {
            this.oldPartPosition[i] = subEntities[i].position();
            this.oldPartYRot[i] = subEntities[i].getYRot();
            this.oldPartXRot[i] = subEntities[i].getXRot();
        }
    }

    private void updatePartPositions() {

        float[] partDragFactors = new float[]{1f, 0.9f, 0.2f, 0.1f, 0.09f};

        Vec3[] baseOffsets = {
                new Vec3(0, 0, 6),      // Nose
                new Vec3(0, 0, 2.5),    // Head
                new Vec3(0, 0, -2.25),  // Body
                new Vec3(0, 0, -7),     // Tail
                new Vec3(0, 0, -11)     // Fluke
        };

        if (prevPartPositions[0] == null) {
            for (int i = 0; i < prevPartPositions.length; i++) {
                prevPartPositions[i] = position();
                oldPartPosition[i] = position();
                partPosition[i] = position();
            }
        }

        // CRITICAL MODIFICATION: swimCycle = 0 when platforms are stable
        float swimCycle;
        if (this.pitchLocked || this.hasAnchorDown() || this.getStationaryTicks() > 0 || this.platformsStable) {
            swimCycle = 0f;
        } else {
             // BREATHING COMPATIBILITY: Force swimCycle to 0 if breaching to prevent visual glitches
             boolean isBreachingAction = false;
             for (net.minecraft.world.entity.ai.goal.WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
                 if (goal.getGoal() instanceof HullbackBreathAirGoal breachGoal) {
                     if (breachGoal.isBreaching()) isBreachingAction = true;
                 }
             }
             if (scanPlayerAbove()) {
                 swimCycle = (float) (Mth.sin(this.tickCount * 0.05f) * this.getDeltaMovement().horizontalDistance() * 0.3);
             } else if(isBreachingAction) {
                 swimCycle = 0f;
             } else {
                 swimCycle = (float) (Mth.sin(this.tickCount * 0.1f) * this.getDeltaMovement().horizontalDistance());
             }
        }
        float yawRad = -this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;

        for (int i = 0; i < baseOffsets.length; i++) {
            baseOffsets[i] = baseOffsets[i]
                    .yRot(yawRad)
                    .xRot(pitchRad);

            baseOffsets[i] = new Vec3(
                    this.getX() + baseOffsets[i].x,
                    this.getY() + baseOffsets[i].y,
                    this.getZ() + baseOffsets[i].z
            );

            if (i > 0) {
                baseOffsets[i] = new Vec3(
                        Mth.lerp(partDragFactors[i], prevPartPositions[i].x, baseOffsets[i].x),
                        Mth.lerp(partDragFactors[i], prevPartPositions[i].y, baseOffsets[i].y),
                        Mth.lerp(partDragFactors[i], prevPartPositions[i].z, baseOffsets[i].z)
                );

                // Chain Constraint: Prevent parts from disconnecting
                double maxDist = switch (i) {
                    case 1 -> 3.55;
                    case 2 -> 4.8;
                    case 3 -> 4.8;
                    case 4 -> 4.1;
                    default -> 10.0;
                };

                Vec3 parentPos = prevPartPositions[i - 1];
                double dist = baseOffsets[i].distanceTo(parentPos);
                if (dist > maxDist) {
                    baseOffsets[i] = parentPos.add(baseOffsets[i].subtract(parentPos).normalize().scale(maxDist));
                }
            }
            prevPartPositions[i] = baseOffsets[i];
        }

        // CRITICAL: Does not apply swimCycle if platforms are stable
        float swimOffset1 = this.platformsStable ? 0f : swimCycle * 2;
        float swimOffset2 = this.platformsStable ? 0f : swimCycle * 2;
        float swimOffset3 = this.platformsStable ? 0f : swimCycle * 8;
        float swimOffset4 = this.platformsStable ? 0f : swimCycle * 5.5f;

        this.partPosition[0] = prevPartPositions[0];
        this.partYRot[0] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[0] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        this.nose.moveTo(prevPartPositions[0].x, prevPartPositions[0].y, prevPartPositions[0].z,
                partYRot[0],
                partXRot[0]);

        this.partPosition[1] = new Vec3(prevPartPositions[1].x, prevPartPositions[1].y + swimOffset1, prevPartPositions[1].z);
        this.partYRot[1] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[1] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        this.head.moveTo(prevPartPositions[1].x, prevPartPositions[1].y + swimOffset1, prevPartPositions[1].z,
                partYRot[1],
                partXRot[1]);

        this.partPosition[2] = new Vec3(prevPartPositions[2].x, prevPartPositions[2].y + swimOffset2, prevPartPositions[2].z);
        this.partYRot[2] = calculateYaw(prevPartPositions[1], prevPartPositions[2]);
        this.partXRot[2] = calculatePitch(prevPartPositions[1], prevPartPositions[2]);
        this.body.moveTo(prevPartPositions[2].x,
                prevPartPositions[2].y + swimOffset2,
                prevPartPositions[2].z,
                partYRot[2],
                partXRot[2]);

        this.partPosition[3] = new Vec3(prevPartPositions[3].x, prevPartPositions[3].y + swimOffset3, prevPartPositions[3].z);
        this.partYRot[3] = calculateYaw(prevPartPositions[2], prevPartPositions[3]);
        this.partXRot[3] = calculatePitch(prevPartPositions[2], prevPartPositions[3]) * 1.5f - (this.platformsStable ? 0 : swimCycle * 20f);
        this.tail.moveTo(prevPartPositions[3].x,
                prevPartPositions[3].y + swimOffset3,
                prevPartPositions[3].z,
                partYRot[3],
                partXRot[3]);

        float flukeDistance = 4.0f;
        Vec3 flukeOffset = new Vec3(0, 0, -flukeDistance)
                .yRot(-tail.getYRot() * Mth.DEG_TO_RAD)
                .xRot(tail.getXRot() * Mth.DEG_TO_RAD);

        Vec3 flukeTarget = new Vec3(
                tail.getX() + flukeOffset.x,
                tail.getY() + flukeOffset.y + swimOffset4,
                tail.getZ() + flukeOffset.z
        );

        float flukeYaw = calculateYaw(tail.position(), flukeTarget);
        float flukePitch = calculatePitch(tail.position(), flukeTarget);

        flukeYaw = Mth.rotLerp(partDragFactors[4], oldPartYRot[4], flukeYaw);

        this.partPosition[4] = flukeTarget;
        this.partYRot[4] = flukeYaw;
        this.partXRot[4] = flukePitch * 1.5f + (this.platformsStable ? 0 : swimCycle * 30f);
        this.fluke.moveTo(
                flukeTarget.x,
                flukeTarget.y,
                flukeTarget.z,
                flukeYaw,
                (flukePitch * 1.5f + (this.platformsStable ? 0 : swimCycle * 30f))
        );

        // Particles only when NOT stable
        if (this.level().isClientSide && this.isInWater() && !this.platformsStable && this.getDeltaMovement().lengthSqr() > PARTICLE_SPEED_THRESHOLD_SQR) {
            for (int i : new int[]{2, 4}) { // Body and Fluke trail
                float offset = (i == 2) ? 4 : 0;
                for (int side : SIDES) {
                    Vec3 particlePos = partPosition[i].add(new Vec3((offset + subEntities[i].getBbWidth() / 2) * side, 0, subEntities[i].getBbWidth() / 2).yRot(-partYRot[i] * Mth.DEG_TO_RAD));
                    for (int j = 0; j < 2; ++j) {
                        this.level().addParticle(ParticleTypes.BUBBLE, particlePos.x, particlePos.y, particlePos.z, 0.0, 0.1, 0.0);
                    }
                }
            }
        }
    }

    private float calculateYaw(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) + 90F;
    }

    private float calculatePitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        return -(float) (Mth.atan2(dy, horizontalDistance) * (180F / Math.PI));
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

    public HullbackWalkableEntity spawnPlatform(int index) {
        if (!this.isDeadOrDying()) {
            HullbackWalkableEntity part = new HullbackWalkableEntity(WBEntityRegistry.HULLBACK_PLATFORM.get(), this.level());
            part.setPos(this.getSubEntities()[index].getX(), this.position().y + currentPlatformHeight, this.getSubEntities()[index].getZ());
            if (this.level().addFreshEntity(part)) {
                return part;
            }
        }
        return null;
    }

    public void stopMoving() {
        this.getNavigation().stop();

        // Ported from 1.21.1: Preserve vertical only when anchored (buoyancy active); otherwise full stop
        // Note: hasAnchorDown() check removed as it doesn't exist in 1.20.1 yet or isn't ported.
        // For now, enforcing full stop to match user request for stability.
        if (this.isInWater()) {
            // SMOOTH DESCENT FIX: Preserve vertical velocity (Y) so 'travel' can handle buoyancy smoothly.
            // Only zero X and Z.
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // Also stabilize pitch when stopping
        if (this.hasAnchorDown()) {
            this.setXRot(Mth.rotLerp(0.5f, this.getXRot(), 0f));
            this.xRotO = this.getXRot();
        }
    }

    private void updateWalkablePlatforms() {
        // Uses fixed and stable height
        double platformY = this.getY() + currentPlatformHeight;
        
        if (this.moving_nose == null) {
            this.moving_nose = spawnPlatform(0);
        } else {
            this.moving_nose.moveTo(this.nose.getX(), platformY, this.nose.getZ());
            this.moving_nose.setYRot(this.nose.getYRot());
            
            // CRITICAL: Delta movement ZERO when stable
            if (this.platformsStable) {
                this.moving_nose.setDeltaMovement(Vec3.ZERO);
            } else {
                this.moving_nose.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
            }
        }

        if (this.moving_head == null) {
            this.moving_head = spawnPlatform(1);
        } else {
            this.moving_head.moveTo(this.head.getX(), platformY, this.head.getZ());
            this.moving_head.setYRot(this.head.getYRot());
            
            if (this.platformsStable) {
                this.moving_head.setDeltaMovement(Vec3.ZERO);
            } else {
                this.moving_head.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
            }
        }

        if (this.moving_body == null) {
            this.moving_body = spawnPlatform(2);
        } else {
            this.moving_body.moveTo(this.body.getX(), platformY, this.body.getZ());
            this.moving_body.setYRot(this.body.getYRot());
            
            if (this.platformsStable) {
                this.moving_body.setDeltaMovement(Vec3.ZERO);
            } else {
                this.moving_body.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
            }
        }
    }


    private void updatePlatformHeight() {
        boolean isMounted = this.getControllingPassenger() != null;
        float target = isMounted ? 4.0f : 4.55f;

        // Detects state change
        if (wasMounted != isMounted) {
            platformHeightCooldown = 40; // 2 seconds
        }
        wasMounted = isMounted;

        this.targetPlatformHeight = target;
        
        // Waits for cooldown to finish before adjusting
        if (platformHeightCooldown > 0) {
            platformHeightCooldown--;
            if (platformHeightCooldown > 20) return; // Waits 1 full second before starting
        }
        
        // More aggressive Lerp to avoid infinite drift
        float lerpSpeed = 0.1f;
        float newHeight = (float) Mth.lerp(lerpSpeed, this.currentPlatformHeight, targetPlatformHeight);
        
        // Final snap to avoid infinite oscillation (Threshold increased to 0.01)
        if (Math.abs(newHeight - targetPlatformHeight) < 0.01f) {
            this.currentPlatformHeight = targetPlatformHeight;
        } else {
            this.currentPlatformHeight = newHeight;
        }
    }



    private void updatePlayerStability() {
        Player nearestPlayer = this.level().getNearestPlayer(this, 10);
        
        if (nearestPlayer != null && isPlayerOnPlatform(nearestPlayer)) {
            double distMoved = nearestPlayer.position().distanceTo(lastPlayerPosition);
            
            if (distMoved < STABLE_THRESHOLD) {
                playerStableTimer++;
            } else {
                playerStableTimer = 0;
            }
            
            lastPlayerPosition = nearestPlayer.position();
        } else {
            playerStableTimer = 0;
        }
    }

    private boolean isPlayerOnPlatform(Player player) {
        // Fix: Use a bounding box that actually encompasses the deck height
        AABB deckBox = this.getBoundingBox().inflate(5.0, 0.5, 12.0).move(0, currentPlatformHeight, 0);
        return deckBox.contains(player.position());
    }

}