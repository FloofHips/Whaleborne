package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HullbackEntity extends WaterAnimal implements ContainerListener, PlayerRideableJumping, Saddleable {
    private float leftEyeYaw, rightEyeYaw, eyePitch;
    private LazyOptional<?> itemHandler = null;
    protected SimpleContainer inventory;
    public static final int INV_SLOT_ARMOR = 0;
    public static final int INV_SLOT_CROWN = 1;
    public static final int INV_SLOT_SADDLE = 2;
    private static final int FLAG_SADDLE = 4;
    public static final int SEAT_HEAD_SAIL = 0;
    public static final int SEAT_HEAD_CAPTAIN = 1;
    public static final int SEAT_BODY_RIGHT_FRONT = 2;
    public static final int SEAT_BODY_LEFT_FRONT = 3;
    public static final int SEAT_BODY_RIGHT_BACK = 4;
    public static final int SEAT_BODY_LEFT_BACK = 5;
    public static final int SEAT_FLUKE = 6;
    private static final EntityDataAccessor<Boolean> DATA_SADDLE_ID = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ARMOR_ID = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<CompoundTag> DATA_SEAT_ASSIGNMENTS = SynchedEntityData.defineId(HullbackEntity.class, EntityDataSerializers.COMPOUND_TAG);
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
    private float mouthOpenProgress;
    private boolean isOpening;
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
            Blocks.ACACIA_LOG.defaultBlockState()
    };
    public BlockState[] possibleFreshBlocks = {
            Blocks.SEAGRASS.defaultBlockState(),
            Blocks.KELP.defaultBlockState(),
            Blocks.ACACIA_LOG.defaultBlockState(),
            Blocks.MOSS_CARPET.defaultBlockState()
    };
    public BlockState[] possibleTopBlocks = {
            Blocks.ACACIA_LOG.defaultBlockState(),
            Blocks.MOSS_CARPET.defaultBlockState()
    };
    public final Vec3[] seatOffsets = {
            //head
            new Vec3(0, 5f, 3.35), //sail
            new Vec3(0, 5f, 0.35), //captain

            //body
            new Vec3(1.5, 5f, 0.3),
            new Vec3(-1.5, 5f, 0.3),
            new Vec3(1.5, 5f, -1.75),
            new Vec3(-1.5, 5f, -1.75),

            //fluke
            new Vec3(0, 1.1f, -0.8)
    };

    public Vec3[] seats = new Vec3[7];
    public Vec3[] oldSeats = new Vec3[7];
    private final Map<UUID, Integer> seatAssignments = new HashMap<>();

    public HullbackEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new SmoothSwimmingMoveControl(this, 1, 2, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 180);

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

        Arrays.fill(partPosition, Vec3.ZERO);

        this.oldPartPosition = new Vec3[5];
        this.oldPartYRot = new float[5];
        this.oldPartXRot = new float[5];

        this.mouthOpenProgress = 0.0f;
        initDirt();
    }

    private void initDirt() {
        headDirt = new BlockState[8][5];
        fillDirtArray(headDirt, true);

        headTopDirt = new BlockState[8][5];
        fillDirtArray(headTopDirt, false);

        bodyDirt = new BlockState[6][5];
        fillDirtArray(bodyDirt, true);

        bodyTopDirt = new BlockState[6][5];
        fillDirtArray(bodyTopDirt, false);

        tailDirt = new BlockState[4][2];
        fillDirtArray(tailDirt, true);

        flukeDirt = new BlockState[3][5];
        fillDirtArray(flukeDirt, true);
    }

    private void fillDirtArray(BlockState[][] array, boolean bottom) {
        if(bottom){
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    array[x][y] = Blocks.AIR.defaultBlockState();
                    if (random.nextDouble() < 0.5) {
                        array[x][y] = possibleBottomBlocks[random.nextInt(possibleBottomBlocks.length)];
                    }
                }
            }
        } else {
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    array[x][y] = Blocks.AIR.defaultBlockState();
                    if (random.nextDouble() < 0.5) {
                        array[x][y] = possibleTopBlocks[random.nextInt(possibleTopBlocks.length)];
                    }
                }
            }
        }
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

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 1.2000000476837158).add(Attributes.ATTACK_DAMAGE, 3.0);
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
        this.itemHandler = LazyOptional.of(() -> {
            return new InvWrapper(this.inventory);
        });
    }
    protected void updateContainerEquipment() {
        if (!this.level().isClientSide) {
            this.setFlag(4, !this.inventory.getItem(0).isEmpty());
        }
    }
    public void containerChanged(Container invBasic) {
        boolean flag = this.isSaddled();
        this.updateContainerEquipment();
        if (this.tickCount > 20 && !flag && this.isSaddled()) {
            this.playSound(this.getSaddleSoundEvent(), 0.5F, 1.0F);
        }

    }
    public boolean isTamed() {
        return this.getFlag(2);
    }
    public void setTamed(boolean tamed) {
        this.setFlag(2, tamed);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public boolean isSaddleable() {
        return this.isAlive();// && this.isTamed();
    }

    public void equipSaddle(@Nullable SoundSource source) {
        this.inventory.setItem(INV_SLOT_SADDLE, new ItemStack(Items.SADDLE));
        this.entityData.set(DATA_SADDLE_ID, true);
    }

    public void equipArmor() {
        this.entityData.set(DATA_ARMOR_ID, 64);
    }
    public boolean isWearingArmor() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_ARMOR_ID) == 64;
        }
        return !this.inventory.getItem(INV_SLOT_ARMOR).isEmpty();
    }

    public boolean isSaddled() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_SADDLE_ID);
        }
        return !this.inventory.getItem(INV_SLOT_SADDLE).isEmpty();
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
        this.entityData.define(DATA_SADDLE_ID, false);
        this.entityData.define(DATA_ARMOR_ID, 0);
        this.entityData.define(DATA_ID_FLAGS, (byte)0);
        this.entityData.define(DATA_SEAT_ASSIGNMENTS, new CompoundTag());
    }
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ListTag listtag = new ListTag();

        for(int i = 2; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)i);
                itemstack.save(compoundtag);
                listtag.add(compoundtag);
            }
        }
        compound.put("Items", listtag);

        compound.putBoolean("Saddled", this.isSaddled());
        compound.putInt("Armor", this.entityData.get(DATA_ARMOR_ID));
        compound.putByte("Flags", this.entityData.get(DATA_ID_FLAGS));

        CompoundTag seatsTag = new CompoundTag();
        for (int i = 0; i < seats.length; i++) {
            UUID occupant = findUUIDForSeat(i);
            seatsTag.putUUID("Seat_" + i, occupant != null ? occupant : new UUID(0, 0));
        }
        compound.put("SeatAssignments", seatsTag);

//        if (this.headDirt != null) {
//            compound.put("HeadDirt", saveDirtArray(headDirt));
//        }
    }
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.createInventory();
        ListTag listtag = compound.getList("Items", 10);

        for(int i = 0; i < listtag.size(); ++i) {
            CompoundTag compoundtag = listtag.getCompound(i);
            int j = compoundtag.getByte("Slot") & 255;
            if (j >= 2 && j < this.inventory.getContainerSize()) {
                this.inventory.setItem(j, ItemStack.of(compoundtag));
            }
        }

        this.entityData.set(DATA_SADDLE_ID, compound.getBoolean("Saddled"));
        this.entityData.set(DATA_ARMOR_ID, compound.getInt("Armor"));
        this.entityData.set(DATA_ID_FLAGS, compound.getByte("Flags"));

        seatAssignments.clear();
        if (compound.contains("SeatAssignments")) {
            CompoundTag seatsTag = compound.getCompound("SeatAssignments");
            for (int i = 0; i < seats.length; i++) {
                UUID occupant = seatsTag.getUUID("Seat_" + i);
                if (!occupant.equals(new UUID(0, 0))) {
                    seatAssignments.put(occupant, i);
                }
            }
        }

//        if (compound.contains("HeadDirt")) {
//            this.headDirt = loadDirtArray(compound.getCompound("HeadDirt"), this.level().holderLookup(Registries.BLOCK));
//        }

        if (compound.contains("Passengers")) {
            ListTag passengers = compound.getList("Passengers", 10);
            for (int i = 0; i < passengers.size(); i++) {
                CompoundTag passengerTag = passengers.getCompound(i);
                UUID passengerUUID = passengerTag.getUUID("UUID");

                if (!seatAssignments.containsKey(passengerUUID)) {
                    int seat = IntStream.range(0, seats.length)
                            .filter(idx -> !seatAssignments.containsValue(idx))
                            .findFirst()
                            .orElse(0);
                    seatAssignments.put(passengerUUID, seat);
                }
            }
        }

        updateSeatAssignmentsSync();

        this.updateContainerEquipment();
    }

    private void validateAfterLoad() {
        getPassengers().forEach(passenger -> {
            if (!seatAssignments.containsKey(passenger.getUUID())) {
                int seat = IntStream.range(0, seats.length)
                        .filter(i -> !seatAssignments.containsValue(i))
                        .findFirst()
                        .orElse(0);
                seatAssignments.put(passenger.getUUID(), seat);
            }
        });

        Set<UUID> passengerUUIDs = getPassengers().stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());

        seatAssignments.keySet().removeIf(uuid -> !passengerUUIDs.contains(uuid));
    }

    private UUID findUUIDForSeat(int seatIndex) {
        return seatAssignments.entrySet().stream()
                .filter(entry -> entry.getValue() == seatIndex)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
//    private CompoundTag saveDirtArray(BlockState[][] array) {
//        CompoundTag tag = new CompoundTag();
//        for (int x = 0; x < array.length; x++) {
//            ListTag column = new ListTag();
//            for (int y = 0; y < array[x].length; y++) {
//                CompoundTag blockTag = NbtUtils.writeBlockState(array[x][y]);
//                column.add(blockTag);
//            }
//            tag.put("X" + x, column);
//        }
//        return tag;
//    }
//
//    private BlockState[][] loadDirtArray(CompoundTag tag, HolderGetter<Block> blockGetter) {
//        BlockState[][] array = new BlockState[8][5];
//        for (int x = 0; x < array.length; x++) {
//            if (tag.contains("X" + x)) {
//                ListTag column = tag.getList("X" + x, 10);
//                for (int y = 0; y < Math.min(column.size(), array[x].length); y++) {
//                    array[x][y] = NbtUtils.readBlockState(blockGetter, column.getCompound(y));
//                }
//            } else {
//                Arrays.fill(array[x], Blocks.AIR.defaultBlockState());
//            }
//        }
//        return array;
//    }
    public float getArmorProgress() {
        float progress = (float) this.entityData.get(DATA_ARMOR_ID) / 64;
        progress = (float) (Math.round(progress * Math.pow(10, 3))
                        / Math.pow(10, 3));

        return progress;
    }
    public float getLeftEyeYaw() { return leftEyeYaw; }
    public float getRightEyeYaw() { return rightEyeYaw; }
    public float getEyePitch() { return eyePitch; }

    public void setLeftEyeYaw(float yaw) { this.leftEyeYaw = Mth.wrapDegrees(yaw); }
    public void setRightEyeYaw(float yaw) { this.rightEyeYaw = Mth.wrapDegrees(yaw); }
    public void setEyePitch(float pitch) { this.eyePitch = Mth.wrapDegrees(pitch); }

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

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreathAirGoal(this));
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(4, new HullbackRandomSwimGoal(this, 0.8, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        //this.goalSelector.addGoal(1, new HullbackApproachPlayerGoal(this, 0.4f));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2000000476837158, true));
        this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal(this, Guardian.class, 8.0F, 1.0, 1.0));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, new Class[]{Guardian.class})).setAlertOthers(new Class[0]));
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isVehicle()) {
            return super.mobInteract(player, hand);
        }

        return super.mobInteract(player, hand);
    }
    public InteractionResult interactRide(Player player, InteractionHand hand, int seatIndex) {
        seatAssignments.remove(player.getUUID());

        System.out.println(seatIndex);

        if (this.level().isClientSide) {
            seatAssignments.put(player.getUUID(), seatIndex);
            return InteractionResult.SUCCESS;
        }

        if (seatAssignments.containsValue(seatIndex)) {
            return InteractionResult.PASS;
        }

        player.startRiding(this);
        seatAssignments.put(player.getUUID(), seatIndex);
        updateSeatAssignmentsSync();
        this.positionRider(player, (e, x, y, z) -> e.teleportTo(x, y, z));

        return InteractionResult.CONSUME;
    }
    public InteractionResult interactClean(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        ItemStack heldItem = player.getItemInHand(hand);
        return handleVegetationRemoval(player, hand, part, top, heldItem.getItem() instanceof ShearsItem);
    }
    public InteractionResult interactArmor(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!this.level().isClientSide) {
            if (heldItem.getItem() instanceof SaddleItem) {
                if (!this.isSaddled()) {
                    this.equipSaddle(SoundSource.PLAYERS);
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.HORSE_SADDLE,
                            SoundSource.PLAYERS, 1.0F, 0.1F);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            else if (heldItem.is(Items.DARK_OAK_PLANKS)) {
                ItemStack currentArmor = this.inventory.getItem(INV_SLOT_ARMOR);
                int toAdd = Math.min(heldItem.getCount(), 64 - currentArmor.getCount());

                if (toAdd > 0) {
                    if (currentArmor.isEmpty()) {
                        this.inventory.setItem(INV_SLOT_ARMOR, new ItemStack(Items.DARK_OAK_PLANKS, toAdd));
                    } else {
                        currentArmor.grow(toAdd);
                    }

                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(toAdd);
                    }
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.WOOD_PLACE,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                            SoundSource.PLAYERS, 1.0F, 0.5f + ((float) currentArmor.getCount() /64));
                    if(currentArmor.getCount()==64){
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                        this.equipArmor();

                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }
    private InteractionResult handleVegetationRemoval(Player player, InteractionHand hand, HullbackPartEntity part, boolean top, boolean isShears) {
        BlockState[][] dirtArray = getDirtArrayForPart(part, top);
        BlockState removedState = Blocks.AIR.defaultBlockState();

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

                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                isShears ? SoundEvents.SHEEP_SHEAR : SoundEvents.AXE_STRIP,
                                SoundSource.PLAYERS, 1.0F, 1.0f);

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
            return block == Blocks.ACACIA_LOG;
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
        } else if (block == Blocks.ACACIA_LOG) {
            return new ItemStack(Items.ACACIA_LOG);
        } else if (block == Blocks.KELP_PLANT) {
            return new ItemStack(Items.KELP, 2);
        } else if (block == Blocks.TALL_SEAGRASS) {
            return new ItemStack(Items.SEAGRASS, 2);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        setOldPosAndRots();
        super.tick();

        updatePartPositions();
        updateMouthOpening();
        if(level().isClientSide)
            if (seatAssignments.isEmpty())
                onSeatAssignmentsUpdated();
        else
            if (seatAssignments.isEmpty())
                onSeatAssignmentsUpdated();

        if (partPosition!=null && partYRot!=null && partXRot!=null){
            seats[0] = partPosition[1].add((seatOffsets[0]).xRot(partXRot[1] * Mth.DEG_TO_RAD).yRot(-partYRot[1] * Mth.DEG_TO_RAD));
            seats[1] = partPosition[1].add((seatOffsets[1]).xRot(partXRot[1] * Mth.DEG_TO_RAD).yRot(-partYRot[1] * Mth.DEG_TO_RAD));
            seats[2] = partPosition[2].add((seatOffsets[2]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[3] = partPosition[2].add((seatOffsets[3]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[4] = partPosition[2].add((seatOffsets[4]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[5] = partPosition[2].add((seatOffsets[5]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
            seats[6] = partPosition[4].add((seatOffsets[6]).xRot(partXRot[4] * Mth.DEG_TO_RAD).yRot(-partYRot[4] * Mth.DEG_TO_RAD));


            oldSeats[0] = oldPartPosition[1].add((seatOffsets[0]).xRot(oldPartXRot[1] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[1] * Mth.DEG_TO_RAD));
            oldSeats[1] = oldPartPosition[1].add((seatOffsets[1]).xRot(oldPartXRot[1] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[1] * Mth.DEG_TO_RAD));
            oldSeats[2] = oldPartPosition[2].add((seatOffsets[2]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[3] = oldPartPosition[2].add((seatOffsets[3]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[4] = oldPartPosition[2].add((seatOffsets[4]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[5] = oldPartPosition[2].add((seatOffsets[5]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
            oldSeats[6] = oldPartPosition[4].add((seatOffsets[6]).xRot(oldPartXRot[4] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[4] * Mth.DEG_TO_RAD));
        }

        if(this.inventory!=null && !this.inventory.getItem(INV_SLOT_ARMOR).isEmpty())
            this.entityData.set(DATA_ARMOR_ID, this.inventory.getItem(INV_SLOT_ARMOR).getCount());

        randomTickDirt(headDirt, true);
        randomTickDirt(bodyDirt, true);
        randomTickDirt(tailDirt, true);
        randomTickDirt(flukeDirt, true);

        if(!isSaddled()){
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

        for ( HullbackPartEntity part : getSubEntities()
             ) {
            part.tick();
        }
    }

    private void randomTickDirt(BlockState[][] array, boolean bottom) {
        if(bottom){
            if (this.random.nextInt(100) <= 10) {
                int x = this.random.nextInt(array.length);
                if (array[x][this.random.nextInt(array[x].length)] == Blocks.SEAGRASS.defaultBlockState()) {
                    array[x][this.random.nextInt(array[x].length)] = Blocks.TALL_SEAGRASS.defaultBlockState();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (array[x][this.random.nextInt(array[x].length)] == Blocks.KELP.defaultBlockState()) {
                    array[x][this.random.nextInt(array[x].length)] = Blocks.KELP_PLANT.defaultBlockState();
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
                if (array[x][this.random.nextInt(array[x].length)] == Blocks.AIR.defaultBlockState()) {
                    array[x][this.random.nextInt(array[x].length)] = possibleFreshBlocks[this.random.nextInt(possibleFreshBlocks.length)];
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
            }
        }
        else{
            if (this.random.nextInt(100) <= 5) {
                int x = this.random.nextInt(array.length);
                if (array[x][this.random.nextInt(array[x].length)] == Blocks.AIR.defaultBlockState()) {
                    array[x][this.random.nextInt(array[x].length)] = possibleTopBlocks[this.random.nextInt(possibleTopBlocks.length)];
                    playSound(SoundEvents.BONE_MEAL_USE);
                    return;
                }
            }
        }
    }

    private void updateMouthOpening() {
        if(isOpening){
            mouthOpenProgress+= 0.1f;
            if(mouthOpenProgress>=1)
                isOpening=false;
        }
        else{
            mouthOpenProgress-= 0.05f;
            if(mouthOpenProgress<=0)
                isOpening=true;
        }
    }

    public float getMouthOpenProgress() {
        return mouthOpenProgress;
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
        float[] partDragFactors = new float[]{1f, 0.9f, 0.5f, 0.3f, 0.07f};
        Vec3[] baseOffsets = {
                new Vec3(0, 0, 6),   // Nose
                new Vec3(0, 0, 2.5), // Head
                new Vec3(0, 0, -2.25),  // Body
                new Vec3(0, 0, -7),  // Tail base
                new Vec3(0, 0, -11)  // Fluke tip
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
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(travelVector);
        }

    }
    protected BodyRotationControl createBodyControl() {
        return new HullbackBodyRotationControl(this);
    }

    @Override
    public void onPlayerJump(int i) {
        
    }

    @Override
    public boolean canJump() {
        return false;
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
        return this.getPassengers().size() < seats.length;
    }
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        if (seatAssignments.isEmpty()) return;

        Integer seatIndex = seatAssignments.get(passenger.getUUID());
        if (seatIndex == null || seatIndex < 0 || seatIndex >= seats.length) {
            seatIndex = 0;
            seatAssignments.put(passenger.getUUID(), seatIndex);
        }
        if (seatIndex < seats.length) {
            if(seats[seatIndex] != null)
                callback.accept(passenger,
                        seats[seatIndex].x,
                        seats[seatIndex].y,
                        seats[seatIndex].z);
        }
    }
    private void updateSeatAssignmentsSync() {
        CompoundTag tag = new CompoundTag();
        seatAssignments.forEach((uuid, seat) -> {
            tag.putInt(uuid.toString(), seat);
        });
        this.entityData.set(DATA_SEAT_ASSIGNMENTS, tag);
    }

    private void onSeatAssignmentsUpdated() {
        CompoundTag tag = this.entityData.get(DATA_SEAT_ASSIGNMENTS);

        seatAssignments.clear();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                seatAssignments.put(uuid, tag.getInt(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);

        if (!seatAssignments.containsKey(passenger.getUUID())) {
            if (passenger instanceof Player) {
                return;
            }

            int availableSeat = IntStream.range(0, seats.length)
                    .filter(i -> !seatAssignments.containsValue(i))
                    .findFirst()
                    .orElse(0);

            seatAssignments.put(passenger.getUUID(), availableSeat);
            updateSeatAssignmentsSync();
        }
    }
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        updateSeatAssignmentsSync();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (!seatAssignments.isEmpty()){
            int seatIndex = seatAssignments.get(passenger.getUUID());
            seatAssignments.remove(passenger.getUUID());
            if (seatIndex >= 0 && seatIndex < seats.length) {
                Vec3 seatPos = seats[seatIndex];
                return new Vec3(
                        seatPos.x,
                        seatPos.y,
                        seatPos.z);
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }
    class HullbackMoveControl extends MoveControl {
        private static final float WHALE_TURN_SPEED = 1.5F;
        private static final float WHALE_PITCH_SPEED = 0.8F;
        private static final float MAX_PITCH_ANGLE = 30.0F;
        private static final float BUOYANCY_FORCE = 0.002F;

        private final HullbackEntity whale;
        private float targetYaw;
        private float targetPitch;

        public HullbackMoveControl(HullbackEntity whale) {
            super(whale);
            this.whale = whale;
            this.targetYaw = whale.getYRot();
            this.targetPitch = whale.getXRot();
        }

        @Override
        public void tick() {
            if (whale.isInWater()) {
                whale.setDeltaMovement(whale.getDeltaMovement().add(0, BUOYANCY_FORCE, 0));
            }

            if (this.operation == Operation.MOVE_TO) {
                double dx = this.wantedX - whale.getX();
                double dy = this.wantedY - whale.getY();
                double dz = this.wantedZ - whale.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > 0.1) {
                    this.targetYaw = (float)(Mth.atan2(dz, dx) * (180F / (float)Math.PI) - 90F);
                    this.targetPitch = (float)(-Mth.atan2(dy, distance) * (180F / (float)Math.PI));
                    this.targetPitch = Mth.clamp(this.targetPitch, -MAX_PITCH_ANGLE, MAX_PITCH_ANGLE);

                    whale.setYRot(this.newrotlerp(whale.getYRot(), targetYaw, WHALE_TURN_SPEED));
                    whale.setXRot(this.newrotlerp(whale.getXRot(), targetPitch, WHALE_PITCH_SPEED));

                    float speed = (float)(this.speedModifier * whale.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    float adjustedSpeed = speed * 0.1F;

                    float f4 = Mth.cos(whale.getXRot() * ((float)Math.PI / 180F));
                    float f5 = Mth.sin(whale.getXRot() * ((float)Math.PI / 180F));
                    whale.zza = f4 * adjustedSpeed;
                    whale.yya = -f5 * adjustedSpeed;

                    whale.xxa = Mth.sin(whale.tickCount * 0.1F) * 0.05F * adjustedSpeed;
                } else {
                    whale.setSpeed(0);
                    whale.setZza(0);
                    whale.setYya(0);
                    whale.setXxa(0);
                }
            } else {
                whale.setSpeed(0);
                whale.setZza(0);
                whale.setYya(0);
                whale.setXxa(0);

                if (whale.tickCount % 40 == 0) {
                    this.targetYaw += (whale.getRandom().nextFloat() - 0.5F) * 30F;
                    this.targetPitch += (whale.getRandom().nextFloat() - 0.5F) * 10F;
                    this.targetPitch = Mth.clamp(this.targetPitch, -15F, 15F);
                }

                whale.setYRot(this.newrotlerp(whale.getYRot(), targetYaw, WHALE_TURN_SPEED * 0.3F));
                whale.setXRot(this.newrotlerp(whale.getXRot(), targetPitch, WHALE_PITCH_SPEED * 0.3F));
            }
        }

        float newrotlerp(float current, float target, float maxStep) {
            float delta = Mth.wrapDegrees(target - current);
            delta = Mth.clamp(delta, -maxStep, maxStep);
            return current + delta * 0.2F;
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

    class HullbackLookControl extends LookControl {
        private final HullbackEntity hullback;
        private static final float EYE_SIDEWAYS_ANGLE = 90F;

        public HullbackLookControl(HullbackEntity mob) {
            super(mob);
            this.hullback = mob;
        }

        @Override
        public void tick() {
            if (this.lookAtCooldown > 0) {
                this.lookAtCooldown--;

                double dx = this.wantedX - this.mob.getX();
                double dz = this.wantedZ - this.mob.getZ();
                double dy = this.wantedY - this.mob.getEyeY();
                double distanceXZ = Math.sqrt(dx * dx + dz * dz);

                float pitch = (float) -Math.toDegrees(Math.atan2(dy, distanceXZ));
                hullback.setEyePitch(pitch);

                float bodyYaw = this.mob.getYRot();
                float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
                float angleToTarget = Mth.wrapDegrees(targetYaw - bodyYaw);

                float leftEyeYaw = bodyYaw - EYE_SIDEWAYS_ANGLE;
                float rightEyeYaw = bodyYaw + EYE_SIDEWAYS_ANGLE;

                if (angleToTarget > 0) {
                    rightEyeYaw += Mth.clamp(angleToTarget * 0.5F, 0, 45F);
                } else {
                    leftEyeYaw += Mth.clamp(angleToTarget * 0.5F, -45F, 0);
                }

                hullback.setLeftEyeYaw(leftEyeYaw);
                hullback.setRightEyeYaw(rightEyeYaw);
            } else {
                hullback.setLeftEyeYaw(this.mob.getYRot() - EYE_SIDEWAYS_ANGLE);
                hullback.setRightEyeYaw(this.mob.getYRot() + EYE_SIDEWAYS_ANGLE);
                hullback.setEyePitch(0);
            }
        }
    }

    class HullbackRandomSwimGoal extends RandomSwimmingGoal{
        public HullbackRandomSwimGoal(PathfinderMob p_25753_, double p_25754_, int p_25755_) {
            super(p_25753_, p_25754_, p_25755_);
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            return BehaviorUtils.getRandomSwimmablePos(this.mob, 50, 20);
        }
    }
    public class HullbackApproachPlayerGoal extends Goal {
        private static final float APPROACH_DISTANCE = 8.0f;
        private static final float SIDE_OFFSET = 5.0f;
        private static final float ROTATION_SPEED = 0.8f; // Degrees per tick

        private final HullbackEntity whale;
        private final float speedModifier;
        private Player targetPlayer;
        private int repositionCooldown;
        private boolean approachFromRight;

        public HullbackApproachPlayerGoal(HullbackEntity whale, float speedModifier) {
            this.whale = whale;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.repositionCooldown > 0) {
                this.repositionCooldown--;
                return false;
            }

            this.targetPlayer = this.whale.level().getNearestPlayer(this.whale, 15.0);
            if (this.targetPlayer == null) return false;

            // Alternate approach sides for variety
            this.approachFromRight = whale.getRandom().nextBoolean();
            this.repositionCooldown = 200 + whale.getRandom().nextInt(200);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPlayer != null
                    && this.targetPlayer.isAlive()
                    && this.whale.distanceToSqr(this.targetPlayer) < 225.0; // 15^2
        }

        @Override
        public void start() {
            super.start();
            playSound(SoundEvents.AMETHYST_BLOCK_BREAK);
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.whale.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) return;

            Vec3 playerLook = this.targetPlayer.getViewVector(1.0f);
            Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();

            Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
            Vec3 targetPos = this.targetPlayer.position()
                    .add(sideOffset)
                    .add(playerLook.scale(-APPROACH_DISTANCE));

            this.whale.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);

            Vec3 toPlayer = this.targetPlayer.position().subtract(this.whale.position());
            float desiredYaw = (float)Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90f;

            float angleToPlayer = Mth.wrapDegrees(desiredYaw - this.whale.getYRot());

            float sideAwareYaw = desiredYaw + (approachFromRight ? -90f : 90f);

            float newYRot = Mth.rotLerp(0.05f, this.whale.getYRot(), sideAwareYaw);
            this.whale.setYRot(newYRot);
            this.whale.yBodyRot = newYRot;

            float eyeBaseAngle = approachFromRight ? 90f : -90f;
            float eyeFollowAmount = Mth.clamp(Math.abs(angleToPlayer) * 0.5f, 0, 45f);

            if (approachFromRight) {
                this.whale.setRightEyeYaw(eyeBaseAngle + eyeFollowAmount);
                this.whale.setLeftEyeYaw(-90f);
            } else {
                this.whale.setLeftEyeYaw(eyeBaseAngle - eyeFollowAmount);
                this.whale.setRightEyeYaw(90f);
            }

            double yDiff = this.targetPlayer.getEyeY() - this.whale.getEyeY();
            double horizontalDist = new Vec3(toPlayer.x, 0, toPlayer.z).length();
            float pitch = (float)-Math.toDegrees(Math.atan2(yDiff, horizontalDist));
            this.whale.setEyePitch(pitch);
        }
    }
}
