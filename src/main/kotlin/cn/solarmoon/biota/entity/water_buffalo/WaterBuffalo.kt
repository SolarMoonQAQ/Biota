package cn.solarmoon.biota.entity.water_buffalo

import cn.solarmoon.biota.entity.ai.goal.Herd
import cn.solarmoon.biota.entity.ai.goal.HerdRole
import cn.solarmoon.biota.entity.ai.navigation.NaturalNavigateGround
import cn.solarmoon.biota.registry.common.BiotaEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.ItemTags
import net.minecraft.util.Mth
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.LookControl
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.event.EventHooks
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

open class WaterBuffalo(entityType: EntityType<out WaterBuffalo>, level: Level) : TamableAnimal(entityType, level), VariantHolder<WaterBuffaloVariant>, GeoEntity, Herd {

    companion object {
        val DATA_VARIANT = SynchedEntityData.defineId(WaterBuffalo::class.java, EntityDataSerializers.INT)
        val DATA_PLOW = SynchedEntityData.defineId(WaterBuffalo::class.java, EntityDataSerializers.ITEM_STACK)

        val WILD_DIMENSIONS = EntityDimensions(1.5f, 2.2f, 1.7f, EntityAttachments.createDefault(1.5f, 2.2f), false)
        val TAMED_DIMENSIONS = EntityDimensions(1.15f, 1.6f, 1.3f, EntityAttachments.createDefault(1.15f, 1.6f), false)

        val WALK_ANIM = RawAnimation.begin().thenLoop("walk")
        val IDLE_ANIM = RawAnimation.begin().thenLoop("idle")
        val RUN_ANIM = RawAnimation.begin().thenLoop("run")

        fun createAttributes() = createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.JUMP_STRENGTH, 0.6)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
    }

    private val animCache = GeckoLibUtil.createInstanceCache(this)

    var plow: ItemStack
        set(value) { entityData.set(DATA_PLOW, value) }
        get() = entityData.get(DATA_PLOW)

    override fun onAddedToLevel() {
        super.onAddedToLevel()
        refreshDimensions()
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnType: MobSpawnType,
        spawnGroupData: SpawnGroupData?
    ): SpawnGroupData? {
        variant = WaterBuffaloVariant.entries.random()
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData)
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(0, WaterBuffaloBehaviorTreeGoal(this))
    }

    override fun isFood(p0: ItemStack): Boolean {
        return p0.`is`(Tags.Items.CROPS_WHEAT) || p0.`is`(Items.APPLE) || p0.`is`(Items.GOLDEN_APPLE)
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val item = player.getItemInHand(hand)

        when {
            // 驯服
            !isTame && isFood(item) -> {
                if (random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, player)) {
                    tame(player)
                    target = null
                    navigation.stop()
                    level().broadcastEntityEvent(this, 7)
                    item.consume(1, player)
                    return InteractionResult.SUCCESS
                } else {
                    level().broadcastEntityEvent(this, 6)
                }
            }
            // 装犁
            isTame && item.`is`(ItemTags.HOES) && this.plow.isEmpty -> {
                if (!level().isClientSide) {
                    this.plow = item.copyWithCount(1)
                    item.consume(1, player)
                    level().playSound(null, onPos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS)
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide)
            }
            // 卸犁
            isTame && player.isCrouching && item.isEmpty && !this.plow.isEmpty -> {
                if (!level().isClientSide) {
                    if (!player.inventory.add(this.plow)) {
                        spawnAtLocation(this.plow, 0.5f)
                    }
                }
                this.plow = ItemStack.EMPTY
                return InteractionResult.sidedSuccess(player.level().isClientSide)
            }
            // 骑乘
            isTame && !player.isCrouching && !this.isVehicle -> {
                if (!level().isClientSide) {
                    player.startRiding(this)
                }
                return InteractionResult.sidedSuccess(level().isClientSide)
            }
        }

        return super.mobInteract(player, hand)
    }

    override fun getControllingPassenger(): LivingEntity? {
        // 确保第一个乘客是生物（比如玩家）才能控制
        return firstPassenger as? LivingEntity
    }

    override fun canRiderInteract(): Boolean {
        return true
    }

    override fun skipAttackInteraction(attacker: Entity): Boolean {
        // 如果攻击者是当前正骑在水牛身上的乘客，直接跳过/无视这次攻击
        if (this.hasPassenger(attacker)) {
            return true
        }
        return super.skipAttackInteraction(attacker)
    }

    override fun tickRidden(player: Player, travelVector: Vec3) {
        super.tickRidden(player, travelVector)

        val turnSpeed = -player.xxa.sign * 4f
        val isMoving = player.zza != 0f

        if (turnSpeed != 0f) {
            yRot += turnSpeed
            yHeadRot = yRot + turnSpeed * 1.5f
            yBodyRot = Mth.rotLerp(0.2f, yBodyRot, yHeadRot)
        } else if (isMoving) {
            yRot = Mth.rotLerp(0.1f, yBodyRot, player.yRot)
            yBodyRot = yRot
            yHeadRot = Mth.rotLerp(0.125f, yBodyRot, player.yRot)
        }
    }

    override fun getRiddenInput(player: Player, travelVector: Vec3): Vec3 {
        // 只获取前后移动 (W/S 键)
        var zza = player.zza

        if (zza <= 0.0f) {
            zza *= 0.25f
        }

        return Vec3(0.0, 0.0, zza.toDouble())
    }

    override fun getRiddenSpeed(player: Player): Float {
        return (0.25f) * this.getAttributeValue(Attributes.MOVEMENT_SPEED).toFloat()
    }

    override fun aiStep() {
        super.aiStep()

        val level = level()
        if (level.isClientSide || level !is ServerLevel || !isTame || plow.isEmpty) return


        // 1. 获取朝向基准
        val f = yRot * (Math.PI / 180.0).toFloat()
        val forward = Vec3(-sin(f.toDouble()), 0.0, cos(f.toDouble()))
        val right = Vec3(cos(f.toDouble()), 0.0, sin(f.toDouble()))

        // 2. 设定耕地平面范围 (假设身后 s 到 e 格范围，左右各 w 格)
        val depthStart = 2.0
        val depthEnd = 2.5
        val width = 1.5
        val centerOffset = forward.scale(2.25)
        val centerPoint = position().subtract(centerOffset)

        // 遍历矩形区域内的方块
        var z = depthStart
        while (z <= depthEnd) {
            var x = -width
            while (x <= width) {
                // 计算局部偏移点：pos = center - forward * z + right * x
                val scanPos = position().subtract(forward.scale(z)).add(right.scale(x))
                val blockPos = BlockPos.containing(scanPos).below()
                val aboveState = level.getBlockState(blockPos.above())

                if (aboveState.isAir && plow.useOn(UseOnContext(
                        level,
                        null,
                        InteractionHand.MAIN_HAND,
                        plow,
                        BlockHitResult(blockPos.center, Direction.UP, blockPos, false)
                    )) !in listOf(InteractionResult.FAIL, InteractionResult.PASS)
                ) {
                    plow.hurtAndBreak(1, level, this) {
                        level.playSound(null, centerPoint.x, centerPoint.y, centerPoint.z, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS)
                        level.sendParticles(ItemParticleOption(ParticleTypes.ITEM, ItemStack(it)), centerPoint.x, centerPoint.y, centerPoint.z, 50, random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.2)
                        plow = ItemStack.EMPTY
                    }
                }
                x += 1.0
            }
            z += 1.0
        }
    }

    override fun getBreedOffspring(
        level: ServerLevel,
        mate: AgeableMob
    ): AgeableMob? {
        if (mate is WaterBuffalo) {
            return BiotaEntities.WATER_BUFFALO.get().create(level)?.also { baby ->
                baby.isBaby = true
                baby.variant = listOf(this.variant, mate.variant).random()
            }
        }
        return null
    }

    override fun setVariant(variant: WaterBuffaloVariant) {
        entityData.set(DATA_VARIANT, variant.ordinal)
    }

    override fun getVariant(): WaterBuffaloVariant {
        return WaterBuffaloVariant.entries[entityData.get(DATA_VARIANT)]
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_VARIANT, 0)
        builder.define(DATA_PLOW, ItemStack.EMPTY)
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)

        if (key == DATA_FLAGS_ID) refreshDimensions()
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        variant = WaterBuffaloVariant.entries[compound.getInt("variant")]
        plow = ItemStack.parseOptional(registryAccess(), compound.getCompound("plow"))
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("variant", variant.ordinal)
        compound.put("plow", plow.saveOptional(this.registryAccess()))
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return (if (isTame || isBaby) TAMED_DIMENSIONS else WILD_DIMENSIONS).scale(ageScale)
    }

    override fun setTame(tame: Boolean, applyTamingSideEffects: Boolean) {
        super.setTame(tame, applyTamingSideEffects)
        refreshDimensions()
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController(this, "controller", 5) { state ->
                val speed = state.animatable.deltaMovement.horizontalDistance()
                val moving = speed > 0.0
                val running = speed > 0.15
                when {
                    running -> state.setAndContinue(RUN_ANIM)
                    moving -> state.setAndContinue(WALK_ANIM)
                    else -> state.setAndContinue(IDLE_ANIM)
                }
            }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return animCache
    }

    override fun createNavigation(level: Level): PathNavigation {
        return NaturalNavigateGround(this, level)
    }

    override var herdRole: HerdRole = HerdRole.Solo

    override fun die(damageSource: net.minecraft.world.damagesource.DamageSource) {
        super.die(damageSource)

        // 只有在服务端，且确实有凶手（比如是被玩家砍死，而不是摔死/火烧死）时才触发溃逃
        if (!level().isClientSide && damageSource.entity != null) {
            val killer = damageSource.entity!!

            // 寻找半径 16 格内的活体同伴
            val neighbors = level().getEntitiesOfClass(
                WaterBuffalo::class.java,
                boundingBox.inflate(16.0)
            ) { it != this && it.isAlive }

            val currentTime = level().gameTime

            for (neighbor in neighbors) {
                // 1. 清除同伴当前的攻击目标，强行打断它们的反击状态
                neighbor.target = null
                neighbor.lastHurtByMob = null

                // 2. 存入案发时间（用于控制逃跑持续多久）
                neighbor.persistentData.putLong("flee_start_time", currentTime)

                // 3. 存入凶手的 UUID！
                neighbor.persistentData.putUUID("herd_killer_uuid", killer.uuid)
            }
        }
    }

}