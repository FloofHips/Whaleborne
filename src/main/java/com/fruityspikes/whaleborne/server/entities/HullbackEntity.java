package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.menus.HullbackMenu;
import com.fruityspikes.whaleborne.network.SyncHullbackDirtPacket;
import com.fruityspikes.whaleborne.network.WhaleborneNetwork;
import com.fruityspikes.whaleborne.server.registries.WBBlockRegistry;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HullbackEntity extends WaterAnimal implements ContainerListener, HasCustomInventoryScreen, PlayerRideableJumping, Saddleable {
    private static final UUID SAIL_SPEED_MODIFIER_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private boolean validatedAfterLoad = false;
    private float leftEyeYaw, rightEyeYaw, eyePitch;
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();
    private boolean immobile;
    private boolean tamedCoolDown;
    private Vec3 currentTarget;
    public int stationaryTicks;
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
    private static final EntityDataAccessor<ItemStack> DATA_CROWN_ID = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ARMOR_COUNT = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_MOUTH_PROGRESS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_0 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_1 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_2 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_3 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_4 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_5 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SEAT_6 = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public final HullbackPartEntity head;
    public final HullbackPartEntity nose;
    public final HullbackPartEntity body;
    public final HullbackPartEntity tail;
    public final HullbackPartEntity fluke;
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

    public static UUID getSailSpeedModifierUuid() {
        return SAIL_SPEED_MODIFIER_UUID;
    }

    public float AttributeSpeedModifier = 1;
    public BlockState[][] headDirt; // 8 x 5
    public BlockState[][] headTopDirt; // 8 x 5
    public BlockState[][] bodyDirt; // 5 x 5
    public BlockState[][] bodyTopDirt; // 5 x 5
    public BlockState[][] tailDirt; // 2 x 2
    public BlockState[][] flukeDirt; // 4 x 4
    public BlockState[] possibleBottomBlocks = {
            Blocks.SEAGRASS.defaultBlockState(),
            Blocks.KELP.defaultBlockState(),
            Blocks.TALL_SEAGRASS.defaultBlockState(),
            Blocks.KELP_PLANT.defaultBlockState(),
            WBBlockRegistry.WHALE_BARNACLE_0.get().defaultBlockState(),
            WBBlockRegistry.WHALE_BARNACLE_1.get().defaultBlockState()
    };
    public BlockState[] possibleFreshBlocks = {
            Blocks.SEAGRASS.defaultBlockState(),
            Blocks.KELP.defaultBlockState(),
            WBBlockRegistry.WHALE_BARNACLE_0.get().defaultBlockState(),
            Blocks.MOSS_CARPET.defaultBlockState()
    };
    public BlockState[] possibleTopBlocks = {
            Blocks.MOSS_CARPET.defaultBlockState(),
            WBBlockRegistry.WHALE_BARNACLE_0.get().defaultBlockState(),
            WBBlockRegistry.WHALE_BARNACLE_1.get().defaultBlockState()
    };
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

        this.moveControl = new SmoothSwimmingMoveControl(this, 1, 2, 0.1F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 1);

        this.nose = new HullbackPartEntity(this, "nose", 5.0F, 5.0F);
        this.head = new HullbackPartEntity(this, "head", 5.0F, 5.0F);
        this.body = new HullbackPartEntity(this, "body", 5.0F, 5.0F);
        this.tail = new HullbackPartEntity(this, "tail", 2.5F, 2.5F);
        this.fluke = new HullbackPartEntity(this, "fluke", 4.0F, 0.6F);

        this.subEntities = new HullbackPartEntity[]{this.nose, this.head, this.body, this.tail, this.fluke};
        this.setId(ENTITY_COUNTER.getAndAdd(this.subEntities.length + 1) + 1);

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
        //createInventory();
        if(!level.isClientSide)
            initDirt();
        else
            initClientDirt();
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

        fillDirtArray(headDirt, true);
        fillDirtArray(headTopDirt, false);
        fillDirtArray(bodyDirt, true);
        fillDirtArray(bodyTopDirt, false);
        fillDirtArray(tailDirt, true);
        fillDirtArray(flukeDirt, true);

        syncDirtToClients();
    }

    private void fillDirtArray(BlockState[][] array, boolean bottom) {
        if(bottom){
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    array[x][y] = Blocks.AIR.defaultBlockState();
                    if (random.nextDouble() < 0.5) {
                        array[x][y] = possibleBottomBlocks[getWeightedIndex(possibleBottomBlocks.length, false)];
                    }
                }
            }
        } else {
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    array[x][y] = Blocks.AIR.defaultBlockState();
                    if (random.nextDouble() < 0.3) {
                        array[x][y] = possibleTopBlocks[getWeightedIndex(possibleTopBlocks.length, false)];;
                    }
                }
            }
        }
        syncDirtToClients();
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

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 1.2000000476837158).add(Attributes.ATTACK_DAMAGE, 3.0);
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
        this.updateContainerEquipment();
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this.inventory));
    }
    protected void updateContainerEquipment() {
        //if (!this.level().isClientSide) {
            //if (this.inventory != null) {
                this.entityData.set(DATA_CROWN_ID, this.inventory.getItem(INV_SLOT_CROWN));
                this.entityData.set(DATA_ARMOR_COUNT, this.inventory.getItem(INV_SLOT_ARMOR).getCount());
                this.setFlag(4, !this.inventory.getItem(INV_SLOT_SADDLE).isEmpty());
            //}
        //}
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

    private ItemStack getArmor() {
        return this.inventory.getItem(INV_SLOT_ARMOR);
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
        byte b0 = (Byte)this.entityData.get(DATA_ID_FLAGS);
        if (value) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 | flagId));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 & ~flagId));
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
        this.entityData.define(DATA_ARMOR_COUNT, 0);
        this.entityData.define(DATA_MOUTH_PROGRESS, 0f);
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

        ItemStack crown = this.inventory.getItem(INV_SLOT_CROWN);
        ItemStack armor = this.inventory.getItem(INV_SLOT_ARMOR);
        this.entityData.set(DATA_CROWN_ID, crown);
        this.entityData.set(DATA_ARMOR_COUNT, armor.getCount());

        compound.putByte("Flags", this.entityData.get(DATA_ID_FLAGS));

        CompoundTag hasMobiusSpawned = new CompoundTag();
        hasMobiusSpawned.putBoolean("HasMobiusSpawned", HAS_MOBIUS_SPAWNED);

        //System.out.println("Saving seat data:");

        for (int i = 0; i < 7; i++) {
            Optional<UUID> occupant = this.entityData.get(getSeatAccessor(i));
            String seatKey = "Seat_" + i;

            //System.out.println("Seat " + i + ": " + occupant);

            if (occupant.isPresent()) {
                compound.putUUID(seatKey, occupant.get());

                //if (!compound.hasUUID(seatKey)) {
                //    System.out.println("Failed to write UUID for seat: " + i);
                //}
            }
        }
        compound.put("HeadDirt", saveDirtArray(headDirt));
        compound.put("HeadTopDirt", saveDirtArray(headTopDirt));

        compound.put("BodyDirt", saveDirtArray(bodyDirt));
        compound.put("BodyTopDirt", saveDirtArray(bodyTopDirt));

        compound.put("tailDirt", saveDirtArray(tailDirt));
        compound.put("flukeDirt", saveDirtArray(flukeDirt));
    }
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.createInventory();
        ListTag items = compound.getList("Items", 10);

        for(int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < this.inventory.getContainerSize()) {
                this.inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }

        this.entityData.set(DATA_ID_FLAGS, compound.getByte("Flags"));

        HAS_MOBIUS_SPAWNED = compound.getBoolean("HasMobiusSpawned");

        boolean hasAnySeatData = IntStream.range(0, 7)
                .anyMatch(i -> compound.hasUUID("Seat_" + i));

        //System.out.println("Contains seat data: " + hasAnySeatData);

        for (int i = 0; i < 7; i++) {
            String seatKey = "Seat_" + i;
            if (compound.hasUUID(seatKey)) {
                UUID uuid = compound.getUUID(seatKey);
                //System.out.println("Loading " + seatKey + " with UUID " + uuid);
                this.entityData.set(getSeatAccessor(i), Optional.of(uuid));
            } else {
                //System.out.println("No data for: "+ seatKey);
                this.entityData.set(getSeatAccessor(i), Optional.empty());
            }
        }

        //System.out.println("Post-load seat data:");
        //for (int i = 0; i < 7; i++) {
        //    System.out.println("Seat "+ i +": " + this.entityData.get(getSeatAccessor(i)));
        //}

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

        syncDirtToClients();
        this.updateContainerEquipment();
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
        return (float) this.entityData.get(DATA_ARMOR_COUNT) /64;
    }
    public float getLeftEyeYaw() { return leftEyeYaw; }
    public float getRightEyeYaw() { return rightEyeYaw; }
    public float getEyePitch() { return eyePitch; }
    public boolean canBreatheUnderwater() {
        return isVehicle();
    }
    protected void handleAirSupply(int airSupply) {
    }
    public int getMaxAirSupply() {
        return 10000;
    }
    protected int increaseAirSupply(int currentAir) {
        return this.getMaxAirSupply();
    }

    public boolean isPushable() {
        return false;
    }
    public void setId(int p_20235_) {
        super.setId(p_20235_);

        for(int i = 0; i < this.subEntities.length; ++i) {
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

    //    private void updateWalkerPositions() {
//        for ( HullbackPartEntity part : getSubEntities()) {
//            AABB boundingBox = part.getBoundingBoxForCulling().move(0, 0.5f, 0);
//
//            List<Entity> riders = level().getEntities(part, boundingBox);
//            riders.removeIf(rider -> rider.isPassenger());
//
//            for (Entity entity : riders) {
//                if (entity instanceof HullbackEntity || entity instanceof HullbackPartEntity)
//                    return;
//                Vec3 offset = new Vec3
//            }
//        }
//    }

    private boolean scanPlayerAbove() {
        for (HullbackPartEntity part : getSubEntities()) {
            AABB box = part.getBoundingBox();
            AABB topSurface = new AABB(
                    box.minX,
                    box.maxY,
                    box.minZ,
                    box.maxX,
                    box.maxY + 2.0,
                    box.maxZ
            );

            List<Entity> entities = level().getEntitiesOfClass(Entity.class, topSurface,
                    entity -> entity instanceof Player &&
                            !entity.isSpectator() &&
                            !entity.isPassenger()
            );

            if (!entities.isEmpty()) {
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME);
                return true;
            }
        }
        return false;
    }

    @Override
    public void playAmbientSound() {
        mouthTarget = 0.2f;

        for (int side : new int[]{-1, 1}) {
            Vec3 particlePos = partPosition[1].add(new Vec3(3.5*side, 2, 0).yRot(-partYRot[1]));
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

        playSound(WBSoundRegistry.HULLBACK_AMBIENT.get(), 5, 1);
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
        this.goalSelector.addGoal(0, new HullbackBreathAirGoal(this));
        this.goalSelector.addGoal(1, new HullbackTryFindWaterGoal(this, true));
        this.goalSelector.addGoal(2, new HullbackTryFindWaterGoal(this, false));
        this.goalSelector.addGoal(2, new HullbackRandomSwimGoal(this, 1.0, 10));
        this.goalSelector.addGoal(0, new HullbackApproachPlayerGoal(this, 0.01f));
        this.goalSelector.addGoal(0, new HullbackArmorPlayerGoal(this, 0.005f));
        this.goalSelector.addGoal(3, new FollowBoatGoal(this));
    }

    public void moveEntitiesOnTop(int index) {
        HullbackPartEntity part = getSubEntities()[index];
        Vec3 offset = partPosition[index].subtract(oldPartPosition[index]);
        if (offset.length() <= 0) return;
        for (Entity entity : this.level().getEntities(part, part.getBoundingBox().inflate(0F, 0.01F, 0F), EntitySelector.NO_SPECTATORS.and((entity) -> (!entity.isPassenger())))) {
            if (!entity.noPhysics && !(entity instanceof HullbackPartEntity) && !(entity instanceof HullbackEntity)) {
                double gravity = entity.isNoGravity() ? 0 : 0.08D;
                if (entity instanceof LivingEntity living) {
                    AttributeInstance attribute = living.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
                    gravity = attribute.getValue();
                }
                float f2 = 1.0F;
                entity.move(MoverType.SHULKER, new Vec3((double) (f2 * (float) offset.x), (double) (f2 * (float) offset.y), (double) (f2 * (float) offset.z)));
            }
        }
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isVehicle()) {
            return super.mobInteract(player, hand);
        }
        return super.mobInteract(player, hand);
    }

    public InteractionResult interactDebug(Player player, InteractionHand hand){
        if(!isTamed()){
            setTamed(true);
            //equipSaddle();
            return InteractionResult.SUCCESS;
        }

        //System.out.println(level() + ": Seat Data: ");
        //for(int i = 0; i<7 ; i++){
        //    System.out.println("Seat " + i + ": " + this.entityData.get(getSeatAccessor(i)));
        //}

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
        ItemStack heldItem = player.getItemInHand(hand);
        mouthTarget = 0.2f;
        if((handleVegetationRemoval(player, hand, part, top, heldItem.getItem() instanceof ShearsItem)) == InteractionResult.PASS) {

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
                Vec3 particlePos = partPosition[1].add(new Vec3(4*side, 2, 0).yRot(partYRot[1]));
                double x = particlePos.x;
                double y = particlePos.y;
                double z = particlePos.z;

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HEART,
                            x,
                            y,
                            z,
                            10,
                            0.5,
                            0.5,
                            0.5,
                            0.02
                    );
                }
            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
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
            }
            else if (heldItem.is(Items.DARK_OAK_PLANKS)) {
                if (!isSaddled()) {
                    mouthTarget = 0.3f;
                    playSound(WBSoundRegistry.HULLBACK_MAD.get());
                    return InteractionResult.PASS;
                }
                ItemStack currentArmor = this.inventory.getItem(INV_SLOT_ARMOR);

                if (currentArmor.getCount() < 64) {
                    if (currentArmor.isEmpty()) {
                        this.inventory.setItem(INV_SLOT_ARMOR, new ItemStack(Items.DARK_OAK_PLANKS, 1));
                    } else {
                        currentArmor.grow(1);
                    }

                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WOOD_PLACE,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                            SoundSource.PLAYERS, 1.0F, 0.5f + ((float) currentArmor.getCount() /64));
                    if(currentArmor.getCount()==64){
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
    private InteractionResult handleVegetationRemoval(Player player, InteractionHand hand, HullbackPartEntity part, boolean top, boolean isShears) {
        BlockState[][] dirtArray = getDirtArrayForPart(part, top);
        BlockState removedState;

        for (int x = 0; x < dirtArray.length; x++) {
            for (int y = 0; y < dirtArray[x].length; y++) {
                if(isRemovableVegetation(dirtArray[x][y], isShears)){
                    removedState = dirtArray[x][y];
                    if (!this.level().isClientSide) {
                        dirtArray[x][y] = Blocks.AIR.defaultBlockState();
                        ItemStack drop = getDropForBlock(removedState);
                        if (!drop.isEmpty()) {
                            ItemEntity itemEntity = new ItemEntity(
                                    this.level(),
                                    part.getX() + y - part.getSize().width / 2,
                                    part.getY() + (top ? part.getSize().height + 0.5f : - 0.5f),
                                    part.getZ() - x + part.getSize().width / 2,
                                    drop
                            );
                            this.level().addFreshEntity(itemEntity);
                        }

                        if (!player.isCreative()) {
                            player.getItemInHand(hand).hurtAndBreak(1, player,
                                    (p) -> p.broadcastBreakEvent(hand));
                        }

                        this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                isShears ? SoundEvents.SHEEP_SHEAR : SoundEvents.AXE_STRIP,
                                SoundSource.PLAYERS, 1.0F, 1.0f);

                        mouthTarget = 1.0f;
                        syncDirtToClients();
                        return InteractionResult.SUCCESS;
                    } else {
                        dirtArray[x][y] = Blocks.AIR.defaultBlockState();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
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

    private boolean isRemovableVegetation(BlockState state, boolean isShears) {
        Block block = state.getBlock();
        if (isShears) {
            return block == Blocks.SEAGRASS || block == Blocks.TALL_SEAGRASS ||
                    block == Blocks.KELP || block == Blocks.KELP_PLANT ||
                    block == Blocks.MOSS_CARPET;
        } else {
            return block == WBBlockRegistry.WHALE_BARNACLE_0.get() || block == WBBlockRegistry.WHALE_BARNACLE_1.get() ||
                    block == WBBlockRegistry.WHALE_BARNACLE_2.get();
        }
    }

    private ItemStack getDropForBlock(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.SEAGRASS) {
            return new ItemStack(Items.SEAGRASS);
        } else if (block == Blocks.KELP) {
            return new ItemStack(Items.KELP);
        } else if (block == Blocks.MOSS_CARPET) {
            return new ItemStack(Items.MOSS_CARPET);
        } else if (block == WBBlockRegistry.WHALE_BARNACLE_0.get()) {
            return new ItemStack(WBItemRegistry.BARNACLE.get());
        } else if (block == WBBlockRegistry.WHALE_BARNACLE_1.get()) {
            return new ItemStack(WBItemRegistry.BARNACLE.get(), 2);
        } else if (block == WBBlockRegistry.WHALE_BARNACLE_2.get()) {
            return new ItemStack(WBItemRegistry.BARNACLE.get(), 5);
        } else if (block == Blocks.KELP_PLANT) {
            return new ItemStack(Items.KELP, 2);
        } else if (block == Blocks.TALL_SEAGRASS) {
            return new ItemStack(Items.SEAGRASS, 2);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
//        if(level().isClientSide){
//            return super.hurt(source, amount);
//        }
        ItemStack armorStack = this.inventory.getItem(INV_SLOT_ARMOR);

        if (!armorStack.isEmpty()) {
            mouthTarget = 0.8f;
            int originalCount = armorStack.getCount();
            int armorDamage = Math.min(originalCount, (int)Math.ceil(amount));

            armorStack.shrink(armorDamage);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);

            updateContainerEquipment();

            playSound(WBSoundRegistry.HULLBACK_MAD.get());

            float remainingDamage = amount - armorDamage;
            if (remainingDamage > 0) {
                //this.unRide();
                return super.hurt(source, remainingDamage);
            }
            return super.hurt(source, remainingDamage);
        }

        mouthTarget = 0;
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if(stationaryTicks>0){
            stopMoving();
            stationaryTicks--;
        }


        if(!this.isEyeInFluidType(Fluids.WATER.getFluidType()))
            mouthTarget = 1;

        if(!isSaddled()){
            if(!getPassengers().isEmpty())
                ejectPassengers();
        }
        else {
            if(this.getArmorProgress() < 0.5 && !getPassengers().isEmpty())
                ejectPassengers();
        }

        setOldPosAndRots();

        if(!hasAnchorDown()){
            updatePartPositions();
            rotatePassengers();
        }

        if(this.getSubEntities()[1].isEyeInFluidType(Fluids.WATER.getFluidType()) && this.tickCount % 80 == 0)
            this.heal(0.25f);

        if (this.getDeltaMovement().length() > 0.3) {
            mouthTarget = 0.8f;
        }

//        for (int i = 0; i < getSubEntities().length; i++) {
//            //if (part.getDeltaMovement().length() > 0) {
//            moveEntitiesOnTop(i);
//            //}
//        }

        updateMouthOpening();
//        if (!level().isClientSide && getControllingPassenger() instanceof Player player) {
//            setYRot(Mth.rotLerp(0.8f, getYRot(), getYRot() - player.xxa));
//        }
        if (getControllingPassenger() instanceof Player player) {
            if(player.xxa != 0)
                setYRot(Mth.rotLerp(0.8f, getYRot(), getYRot() - player.xxa));
        }
//        if(this.getControllingPassenger() instanceof Player){
//            //if(this.level().isClientSide){
//               // System.out.println(newRotY);
//                //System.out.println(this.entityData.get(DATA_Y_ROT));
//                this.setYRot(Mth.rotLerp(0.8f, this.getYRot(), newRotY));
//            //}
//        }
        if (this.tickCount % 80 == 0)
            mouthTarget = 0;

        if (this.tickCount % 20 == 0)
            validateAssignments();

        if (tickCount == 10)
            syncDirtToClients();

        yHeadRot = yBodyRot + (partYRot[0] - partYRot[4]) * 1.5f;

        if (isTamed() && partPosition!=null && partYRot!=null && partXRot!=null){
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

        if(!level().isClientSide){
            randomTickDirt(headDirt, true);
            randomTickDirt(bodyDirt, true);
            randomTickDirt(tailDirt, true);
            randomTickDirt(flukeDirt, true);

            if(!isTamed()){
                randomTickDirt(headTopDirt, false);
                randomTickDirt(bodyTopDirt, false);
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

        for (int i=0; i < subEntities.length; i++) {
            subEntities[i].tick();

            if(i == 2 || i == 4) {
                float offset = 0;
                if (this.level().isClientSide && this.isInWater() && this.getDeltaMovement().length() > 0.03) {
                    for (int side : new int[]{-1, 1}) {
                        if(i == 2)
                            offset = 4;
                        Vec3 particlePos = partPosition[i].add(new Vec3((offset + subEntities[i].getSize().width / 2)*side, 0, subEntities[i].getSize().width / 2).yRot(partYRot[i]));
                        double x = particlePos.x;
                        double y = particlePos.y;
                        double z = particlePos.z;

                        for(int j = 0; j < 4; ++j) {
                            this.level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.1, 0.0);
                            this.level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.1, 0.0);
                        }
                    }
                }
            }
        }
    }

    private boolean hasAnchorDown() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof AnchorEntity anchor) {
                if (anchor.isDown()) return true;
                break;
            }
        }
        return false;
    }

    public void stopMoving(){
        this.getNavigation().stop();

        this.setPos(this.xo, this.yo, this.zo);
        this.setYRot(yRotO);
        this.setXRot(xRotO);
        //this.setXxa(0.0F);
        //this.setYya(0.0F);
    }

//    private void updateWalkerPositions() {
//        for ( HullbackPartEntity part : getSubEntities()) {
//            AABB boundingBox = part.getBoundingBoxForCulling().move(0, 0.5f, 0);
//
//            List<Entity> riders = level().getEntities(part, boundingBox);
//            riders.removeIf(rider -> rider.isPassenger());
//
//            for (Entity entity : riders) {
//                if (entity instanceof HullbackEntity || entity instanceof HullbackPartEntity)
//                    return;
//                Vec3 offset = new Vec3
//            }
//        }
//    }

    private void randomTickDirt(BlockState[][] array, boolean bottom) {
        if (bottom) {
            if (this.random.nextInt(10000) <= 10) {
                int x = getWeightedIndex(array.length, true);
                int y = getWeightedIndex(array[x].length, true);

                BlockState currentState = array[x][y];

                if (currentState == Blocks.SEAGRASS.defaultBlockState()) {
                    array[x][y] = Blocks.TALL_SEAGRASS.defaultBlockState();
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (currentState == Blocks.KELP.defaultBlockState()) {
                    array[x][y] = Blocks.KELP_PLANT.defaultBlockState();
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (currentState == WBBlockRegistry.WHALE_BARNACLE_0.get().defaultBlockState()) {
                    array[x][y] = WBBlockRegistry.WHALE_BARNACLE_1.get().defaultBlockState();
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (currentState == WBBlockRegistry.WHALE_BARNACLE_1.get().defaultBlockState()) {
                    array[x][y] = WBBlockRegistry.WHALE_BARNACLE_2.get().defaultBlockState();
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (currentState == Blocks.AIR.defaultBlockState()) {
                    int index = getWeightedIndex(possibleFreshBlocks.length, false);
                    array[x][y] = possibleFreshBlocks[index];
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
            }
        } else {
            if (this.random.nextInt(10000) <= 5) {

                int x = getWeightedIndex(array.length, true);
                int y = getWeightedIndex(array[x].length, true);

                BlockState currentState = array[x][y];

                if (currentState == Blocks.AIR.defaultBlockState()) {
                    int index = getWeightedIndex(possibleTopBlocks.length, false);
                    array[x][y] = possibleTopBlocks[index];
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (currentState == WBBlockRegistry.WHALE_BARNACLE_0.get().defaultBlockState()) {
                    array[x][y] = WBBlockRegistry.WHALE_BARNACLE_1.get().defaultBlockState();
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (currentState == WBBlockRegistry.WHALE_BARNACLE_1.get().defaultBlockState()) {
                    array[x][y] = WBBlockRegistry.WHALE_BARNACLE_2.get().defaultBlockState();
                    syncDirtToClients();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
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
        if(!this.level().isClientSide){
            this.entityData.set(DATA_MOUTH_PROGRESS, mouthOpenProgress);
        }
    }

    public float getMouthOpenProgress() {
        return this.entityData.get(DATA_MOUTH_PROGRESS);
    }
    public Vec3 getPartPos(int i){
        return partPosition[i];
    }

    public float getPartYRot(int i){
        return partYRot[i];
    }
    public float getPartXRot(int i){
        return partXRot[i];
    }

    public Vec3 getOldPartPos(int i){
        return oldPartPosition[i];
    }
    public float getOldPartYRot(int i){
        return oldPartYRot[i];
    }
    public float getOldPartXRot(int i){
        return oldPartXRot[i];
    }

    public void setOldPosAndRots(){
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
            }
        }

        float swimCycle = (float) (Mth.sin(this.tickCount * 0.1f) * this.getDeltaMovement().length());
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
            }
            prevPartPositions[i] = baseOffsets[i];
        }

        this.partPosition[0] = prevPartPositions[0];
        this.partYRot[0] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[0] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        this.nose.moveTo(prevPartPositions[0].x, prevPartPositions[0].y, prevPartPositions[0].z,
                partYRot[0],
                partXRot[0]);

        this.partPosition[1] = new Vec3(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * 2, prevPartPositions[1].z);
        this.partYRot[1] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[1] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        this.head.moveTo(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * 2, prevPartPositions[1].z,
                partYRot[1],
                partXRot[1]);

        this.partPosition[2] = new Vec3(prevPartPositions[2].x, prevPartPositions[2].y + swimCycle * 2, prevPartPositions[2].z);
        this.partYRot[2] = calculateYaw(prevPartPositions[1], prevPartPositions[2]);
        this.partXRot[2] = calculatePitch(prevPartPositions[1], prevPartPositions[2]);
        this.body.moveTo(prevPartPositions[2].x,
                prevPartPositions[2].y + swimCycle * 2,
                prevPartPositions[2].z,
                partYRot[2],
                partXRot[2]);

        this.partPosition[3] = new Vec3(prevPartPositions[3].x, prevPartPositions[3].y + swimCycle * 8, prevPartPositions[3].z);
        this.partYRot[3] = calculateYaw(prevPartPositions[2], prevPartPositions[3]);
        this.partXRot[3] = calculatePitch(prevPartPositions[2], prevPartPositions[3]) * 1.5f - swimCycle * 20f;
        this.tail.moveTo(prevPartPositions[3].x,
                prevPartPositions[3].y + swimCycle * 8,
                prevPartPositions[3].z,
                partYRot[3],
                partXRot[3]);

        float flukeDistance = 4.0f;
        Vec3 flukeOffset = new Vec3(0, 0, -flukeDistance)
                .yRot(-tail.getYRot() * Mth.DEG_TO_RAD)
                .xRot(tail.getXRot() * Mth.DEG_TO_RAD);

        Vec3 flukeTarget = new Vec3(
                tail.getX() + flukeOffset.x,
                tail.getY() + flukeOffset.y + swimCycle * 10,
                tail.getZ() + flukeOffset.z
        );

        float flukeYaw = calculateYaw(tail.position(), flukeTarget);
        float flukePitch = calculatePitch(tail.position(), flukeTarget);

        flukeYaw = Mth.rotLerp(partDragFactors[4], oldPartYRot[4], flukeYaw);

        this.partPosition[4] = flukeTarget;
        this.partYRot[4] = flukeYaw;
        this.partXRot[4] = flukePitch * 1.5f + swimCycle * 30f;
        this.fluke.moveTo(
                flukeTarget.x,
                flukeTarget.y,
                flukeTarget.z,
                flukeYaw,
                (flukePitch * 1.5f + swimCycle * 30f)
        );
    }
    private float calculateYaw(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float)(Mth.atan2(dz, dx) * (180F / Math.PI)) + 90F;
    }

    private float calculatePitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        return -(float)(Mth.atan2(dy, horizontalDistance) * (180F / Math.PI));
    }
    protected BodyRotationControl createBodyControl() {
        return new HullbackBodyRotationControl(this);
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
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;

        int seatIndex = getSeatByEntity(passenger);
        if (seatIndex == -1) {
            return;
            //getEntityByUUID(this.entityData.get(getSeatAccessor(-1)).get()).unRide();
//            seatIndex = findFreeSeat();
//            if (seatIndex != -1) {
//                this.entityData.set(getSeatAccessor(seatIndex), Optional.of(passenger.getUUID()));
//            } else {
//                passenger.unRide();
//                return;
//            }
        }

        float yOffset = 0;
        if(this.getArmorProgress() == 0)
            yOffset = 0.5F;

        if (seatIndex < seats.length) {
            if(seats[seatIndex] != null)
                callback.accept(passenger,
                        seats[seatIndex].x,
                        seats[seatIndex].y - yOffset + passenger.getMyRidingOffset(),
                        seats[seatIndex].z);
        }
    }

    private void updateModifiers() {
        double speedModifier = 0.0;

        for (Entity passenger : getPassengers()) {
            if (passenger instanceof SailEntity sail) {
                speedModifier += sail.getSpeedModifier();
            }
        }

        AttributeInstance inst = this.getAttribute(getSwimSpeed());
        if (inst != null) {
            AttributeModifier old = inst.getModifier(SAIL_SPEED_MODIFIER_UUID);
            if (old != null) {
                inst.removeModifier(old);
            }
            if (speedModifier != 0.0) {
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

    public int getSeatByEntity(Entity entity){
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

        updateModifiers();
        for (int seatIndex = 0; seatIndex < 7; seatIndex++) {
            EntityDataAccessor<Optional<UUID>> seatAccessor = getSeatAccessor(seatIndex);
            Optional<UUID> assignedUUID = this.entityData.get(seatAccessor);

            if (assignedUUID.isPresent()) {
                UUID uuid = assignedUUID.get();

                if (!currentPassengerUUIDs.contains(uuid)) {
                    //Whaleborne.LOGGER.debug("Clearing seat assignment: seat {} for {}", seatIndex, uuid);
                    this.entityData.set(seatAccessor, Optional.empty());
                }

                else {
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
        this.entityData.set(getSeatAccessor(seatIndex), Optional.of(passenger.getUUID()));
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
//        if (!seatAssignments.containsValue(passenger.getUUID())) {
//
//            int availableSeat = IntStream.range(0, seats.length)
//                    .filter(i -> !seatAssignments.containsKey(i))
//                    .findFirst()
//                    .orElse(0);
//
//            seatAssignments.put(availableSeat, passenger.getUUID());
//            syncSeatAssignments();
//        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if(passenger instanceof Player)
            stationaryTicks = 100;
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

            if (seatIndex >= 0 && seatIndex < seats.length) {
                Vec3 seatPos = seats[seatIndex];
                return new Vec3(seatPos.x, seatPos.y, seatPos.z);
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    public void rotatePassengers(){
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
                if(!(passenger instanceof CannonEntity cannonEntity && cannonEntity.isVehicle())){
                    if(passenger instanceof SailEntity){
                        passenger.setYRot(Mth.rotLerp((float) (0.05 + 0.1 * partIndex), passenger.getYRot(), partYRot[partIndex]) + offset);
                        passenger.setXRot(Mth.rotLerp((float) (0.05 + 0.1 * partIndex), passenger.getXRot(), partXRot[partIndex]));
                    }
                    else {
                        passenger.yRotO = passenger.getYRot();
                        passenger.xRotO = passenger.getXRot();
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

            if(entity.getUUID().equals(uuid))
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

    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if(Mth.abs(player.xxa) > 0){
            if (hasAnchorDown()){
                if (tickCount % 10 == 0)
                    this.playSound(SoundEvents.WOOD_HIT);
                return Vec3.ZERO;
            }
            if (tickCount % 2 == 0)
                this.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON);
            //newRotY = this.getYRot() - player.xxa;

            if(getControllingPassenger().getVehicle() != null && getControllingPassenger().getVehicle() instanceof HelmEntity helmEntity){
                helmEntity.setWheelRotation(helmEntity.getWheelRotation() + player.xxa / 10);
            }
        } else{
            if(getControllingPassenger().getVehicle() != null && getControllingPassenger().getVehicle() instanceof HelmEntity helmEntity){
                helmEntity.setPrevWheelRotation(helmEntity.getWheelRotation());
            }
        }

        float f = player.xxa * 0.5F;
        float f1 = player.zza;
        if (f1 <= 0.0F) {
            f1 *= 0.25F;
        }
        float f3 = 0;
        if(this.nose.isEyeInFluidType(Fluids.WATER.getFluidType()))
            f3 = 1;

        return new Vec3(0, f3, f1);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
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

    class HullbackBodyRotationControl extends BodyRotationControl {
        public HullbackBodyRotationControl(HullbackEntity hullBack) {
            super(hullBack);
        }

        public void clientTick() {
            HullbackEntity.this.setYBodyRot(HullbackEntity.this.getYRot());
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
                if (this.mob.getNoActionTime() >= 100) {
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
                mob.getNavigation().moveTo(currentTarget.x, currentTarget.y, currentTarget.z, mob.isSaddled() ? 0.2f : speedModifier);
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

            if(!mob.getSubEntities()[0].isEyeInFluidType(Fluids.WATER.getFluidType()))
                mob.setDeltaMovement(0, -0.1, 0);

            if (currentTarget != null && mob.getNavigation().isDone() && mob.position().distanceTo(currentTarget) > MIN_DISTANCE) {
                mob.getNavigation().moveTo(currentTarget.x, currentTarget.y, currentTarget.z, speedModifier);
            }
            if(this.mob.getDeltaMovement().length() > 0.5) mouthTarget = 1;
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
                Vec3 direction = lookAngle.yRot((float)Math.toRadians(angle));

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
        private static final int BREACH_HEIGHT = 5; // Blocks above surface to breach
        private static final float BREACH_SPEED = 1.2f;
        private static final float ROTATION_SPEED = 10f;

        private final HullbackEntity hullback;
        private int breachCooldown = 0;
        private boolean isBreaching = false;
        private Vec3 initialPos;

        public HullbackBreathAirGoal(HullbackEntity hullback) {
            this.hullback = hullback;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {

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
            this.isBreaching = true;
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

            if (this.hullback.level().isClientSide) {
                this.hullback.mouthTarget = 0.7f;
                this.hullback.playSound(SoundEvents.ALLAY_HURT, 1.0f, 0.3f);
            }
        }

        @Override
        public void tick() {
            super.tick();

            float targetXRot = -60f;
            this.hullback.setXRot(Mth.rotLerp(ROTATION_SPEED * 0.1f, this.hullback.getXRot(), targetXRot));

            if (this.hullback.isInWater()) {
                this.hullback.setDeltaMovement(new Vec3(0.3, 0.8, 0).yRot(this.hullback.getYRot())
                );
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
            this.isBreaching = false;
            this.breachCooldown = 200;

            this.hullback.setAirSupply(this.hullback.getMaxAirSupply());

            if (this.hullback.level().isClientSide) {
                for (int i = 0; i < 20; i++) {
                    this.hullback.level().addParticle(ParticleTypes.BUBBLE,
                            this.hullback.partPosition[2].x,
                            this.hullback.partPosition[2].y,
                            this.hullback.partPosition[2].z,
                            (this.hullback.getRandom().nextFloat() - 0.5f) * 0.5f,
                            this.hullback.getRandom().nextFloat() * 0.5f,
                            (this.hullback.getRandom().nextFloat() - 0.5f) * 0.5f);
                }
                this.hullback.mouthTarget = 0.0f;
            }

            Vec3 particlePos = partPosition[1].add(new Vec3(0, 7, 0));
            double x = particlePos.x;
            double y = particlePos.y;
            double z = particlePos.z;

            if (this.hullback.level() instanceof ServerLevel serverLevel) {
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

            this.hullback.playSound(WBSoundRegistry.HULLBACK_BREATHE.get(), 6, 1);
            this.hullback.setXRot(Mth.rotLerp(0.1f, this.hullback.getXRot(), 0));
        }

        public boolean isBreaching() {
            return this.isBreaching;
        }
    }

    public class HullbackArmorPlayerGoal extends Goal {
        private static final float APPROACH_DISTANCE = 8.0f;
        private static final float SIDE_OFFSET = 5.0f;
        private static final float ROTATION_SPEED = 0.8f;
        private static Ingredient TEMPT_PLANKS = Ingredient.of(Items.DARK_OAK_PLANKS);
        private static Ingredient TEMPT_WIDGETS = Ingredient.of(WBItemRegistry.SAIL.get(), WBItemRegistry.ANCHOR.get(), WBItemRegistry.MAST.get(), WBItemRegistry.HELM.get(), WBItemRegistry.CANNON.get());
        private final HullbackEntity hullback;
        private final float speedModifier;
        private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
        private final TargetingConditions targetingConditions;
        private Player targetPlayer;
        private int repositionCooldown;
        private boolean approachFromRight;
        private Vec3 targetPosition;

        public HullbackArmorPlayerGoal(HullbackEntity hullback, float speedModifier) {
            this.hullback = hullback;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.repositionCooldown = 200 + hullback.getRandom().nextInt(200);
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

            if (!isTamed())
                return false;

            if (hullback.hasAnchorDown())
                return false;

            this.targetPlayer = this.hullback.level().getNearestPlayer(this.targetingConditions, this.hullback, 30, 50, 30);
            if (this.targetPlayer == null) return false;
            if (this.targetPlayer.isPassenger() && (this.targetPlayer.getVehicle().is(this.hullback) || (this.targetPlayer.getVehicle().isPassenger() && this.targetPlayer.getVehicle().getVehicle().is(this.hullback)))) return false;

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            super.start();

            Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
            Vec3 whaleRight = Vec3.directionFromRotation(0, hullback.getYRot() + 90);
            this.approachFromRight = toPlayer.dot(whaleRight) > 0;


            this.hullback.setTarget(this.targetPlayer);
            Vec3 playerLook = this.targetPlayer.getLookAngle();

            Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();

            Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
            this.targetPosition = this.targetPlayer.position()
                    .add(sideOffset)
                    .add(playerLook.scale(-APPROACH_DISTANCE));

            playSound(WBSoundRegistry.HULLBACK_HAPPY.get());
            this.hullback.mouthTarget = 0.1f;
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.targetPosition = null;
            this.hullback.setTarget(null);
            this.repositionCooldown = 100 + hullback.getRandom().nextInt(200);
            this.hullback.getNavigation().stop();
            this.hullback.mouthTarget = 0.0f;
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) return;

            this.hullback.mouthTarget = 0.6f;
            if(hullback.tickCount % 200 == 0){
                Vec3 playerLook = this.targetPlayer.getLookAngle();
                Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();
                Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
                this.targetPosition = this.targetPlayer.position()
                        .add(sideOffset)
                        .add(playerLook.scale(-APPROACH_DISTANCE));
                this.hullback.getNavigation().moveTo(
                        targetPosition.x,
                        targetPosition.y,
                        targetPosition.z,
                        this.speedModifier
                );

                Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
                float desiredYaw = (float) Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90f;
                float sideYawOffset = approachFromRight ? -90f : 90f;
                float targetYaw = desiredYaw + sideYawOffset;
//            if (this.hullback.tickCount % 200 == 0){
                this.hullback.setYRot(Mth.rotLerp(0.1f, this.hullback.getYRot(), targetYaw));
                this.hullback.yBodyRot = this.hullback.getYRot();
//            }
            }
        }
    }
    public class HullbackApproachPlayerGoal extends Goal {
        private static final float APPROACH_DISTANCE = 8.0f;
        private static final float SIDE_OFFSET = 5.0f;
        private static final float ROTATION_SPEED = 0.8f;

        private static Ingredient TEMPT_SADDLE = Ingredient.of(Items.SADDLE);
        private static Ingredient TEMPT_ITEMS = Ingredient.of(Items.SHEARS);
        private static Ingredient TEMPT_AXES = Ingredient.of(ItemTags.AXES);
        private final HullbackEntity hullback;
        private final float speedModifier;
        private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
        private final TargetingConditions targetingConditions;
        private Player targetPlayer;
        private int repositionCooldown;
        private boolean approachFromRight;
        private Vec3 targetPosition;

        public HullbackApproachPlayerGoal(HullbackEntity hullback, float speedModifier) {
            this.hullback = hullback;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.repositionCooldown = 200 + hullback.getRandom().nextInt(200);
            this.targetingConditions = TEMP_TARGETING.copy().selector(this::shouldFollow);
        }

        private boolean shouldFollow(LivingEntity entity) {
            if( isTamed() && !isSaddled())
                return TEMPT_SADDLE.test(entity.getMainHandItem()) || TEMPT_SADDLE.test(entity.getOffhandItem()) || TEMPT_ITEMS.test(entity.getMainHandItem()) || TEMPT_ITEMS.test(entity.getOffhandItem()) || TEMPT_AXES.test(entity.getMainHandItem()) || TEMPT_AXES.test(entity.getOffhandItem());
            return TEMPT_ITEMS.test(entity.getMainHandItem()) || TEMPT_ITEMS.test(entity.getOffhandItem()) || TEMPT_AXES.test(entity.getMainHandItem()) || TEMPT_AXES.test(entity.getOffhandItem());
        }

        @Override
        public boolean canUse() {

            if (hullback.hasAnchorDown())
                return false;

            this.targetPlayer = this.hullback.level().getNearestPlayer(this.targetingConditions, this.hullback, 30, 50, 30);
            if (this.targetPlayer == null) return false;
            if (this.targetPlayer.isPassenger() && (this.targetPlayer.getVehicle().is(this.hullback) || (this.targetPlayer.getVehicle().isPassenger() && this.targetPlayer.getVehicle().getVehicle().is(this.hullback)))) return false;

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            super.start();

            Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
            Vec3 whaleRight = Vec3.directionFromRotation(0, hullback.getYRot() + 90);
            this.approachFromRight = toPlayer.dot(whaleRight) > 0;


            this.hullback.setTarget(this.targetPlayer);
            Vec3 playerLook = this.targetPlayer.getLookAngle();

            Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();
            //TODO FIX
            Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
            this.targetPosition = this.targetPlayer.position()
                    .add(sideOffset)
                    .add(playerLook.scale(-APPROACH_DISTANCE));

            playSound(WBSoundRegistry.HULLBACK_HAPPY.get());
            this.hullback.mouthTarget = 0.2f;
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.targetPosition = null;
            this.hullback.setTarget(null);
            this.repositionCooldown = 100 + hullback.getRandom().nextInt(200);
            this.hullback.getNavigation().stop();
            this.hullback.mouthTarget = 0.0f;
        }

        @Override
        public void tick() {
            this.hullback.mouthTarget = 0.6f;
            if (this.targetPlayer == null) return;
            if(hullback.tickCount % 200 == 0) {
                Vec3 playerLook = this.targetPlayer.getLookAngle();
                Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();
                Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
                this.targetPosition = this.targetPlayer.position()
                        .add(sideOffset)
                        .add(playerLook.scale(-APPROACH_DISTANCE));
                this.hullback.getNavigation().moveTo(
                        targetPosition.x,
                        targetPosition.y,
                        targetPosition.z,
                        this.speedModifier
                );

                Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
                float desiredYaw = (float) Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90f;
                float sideYawOffset = approachFromRight ? -90f : 90f;
                float targetYaw = desiredYaw + sideYawOffset;

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
            if(isBeached)
                return mob.tickCount > 20 && !this.mob.isEyeInFluidType(Fluids.WATER.getFluidType());
            return mob.tickCount > 20 && !mob.level().getFluidState(mob.blockPosition().below()).is(FluidTags.WATER);
        }

        public void start() {
            BlockPos blockpos = null;

                Iterator var2 = BlockPos.betweenClosed(Mth.floor(this.mob.getX() - (isBeached ? 20.0 : 5)), Mth.floor(this.mob.getY() - 2.0), Mth.floor(this.mob.getZ() - (isBeached ? 20.0 : 5)), Mth.floor(this.mob.getX() + (isBeached ? 20.0 : 5)), this.mob.getBlockY(), Mth.floor(this.mob.getZ() + (isBeached ? 20.0 : 5))).iterator();

            while(var2.hasNext()) {
                BlockPos blockpos1 = (BlockPos)var2.next();
                if (this.mob.level().getFluidState(blockpos1).is(FluidTags.WATER)) {
                    blockpos = blockpos1;
                    break;
                }
            }

            if (blockpos != null) {
                this.mob.getMoveControl().setWantedPosition((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), 1.0);
                this.mob.getLookControl().setLookAt(mob.getMoveControl().getWantedX(), mob.getMoveControl().getWantedY(), mob.getMoveControl().getWantedZ());
            }
        }

        @Override
        public void tick() {
            super.tick();

            Vec3 target = new Vec3(mob.getMoveControl().getWantedX(),
                    mob.getMoveControl().getWantedY(),
                    mob.getMoveControl().getWantedZ());
            float targetYRot = (float)Math.toDegrees(Math.atan2(target.z - mob.getZ(), target.x - mob.getX())) - 90;

            mob.setYRot(Mth.rotLerp(0.01f, mob.getYRot(), targetYRot));


            if(mob.tickCount % 10 == 0)
                mob.playSound(WBSoundRegistry.HULLBACK_MAD.get());

            if(mob.tickCount % 100 == 0 && !mob.level().getBlockState(mob.blockPosition().below()).isAir()){

                ((HullbackEntity) mob).mouthTarget = 0;

                mob.getLookControl().setLookAt(target);
                mob.setYRot(Mth.rotLerp(0.1f, mob.getYRot(), targetYRot));
                mob.yBodyRot = mob.getYRot();
                Vec3 direction = target.subtract(mob.position()).normalize();

                double lungePower = 1;
                Vec3 velocity = direction.scale(lungePower).add(0, 0.5, 0);
                if(isBeached) {
                    mob.playSound(WBSoundRegistry.ORGAN.get(), 2, 2f);
                    mob.playSound(WBSoundRegistry.ORGAN.get(), 2, 1f);
                    mob.playSound(WBSoundRegistry.HULLBACK_HURT.get(), 3, 0.2f);
                    mob.playSound(WBSoundRegistry.HULLBACK_SWIM.get(), 3, 0.5f);
                    pushEntities();
                }
                mob.setDeltaMovement(velocity);

                mob.setYRot(Mth.rotLerp(0.2f, mob.getYRot(), targetYRot));
                mob.yBodyRot = mob.getYRot();
            }
        }

        public void pushEntities(){
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
            if(isBeached) {
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

                this.mob.playSound(WBSoundRegistry.HULLBACK_BREATHE.get(), 6, 1);
            }
        }
    }
}
