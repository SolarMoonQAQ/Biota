package cn.solarmoon.biota.entity.peafowl

import cn.solarmoon.biota.entity.ai.move_control.FlyingMoveControl
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.ItemTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.Tags
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.util.GeckoLibUtil

open class Peafowl(entityType: EntityType<out Peafowl>, level: Level)
    : Animal(entityType, level), GeoEntity, FlyingAnimal {

    companion object {
        val DATA_IS_DISPLAYING = SynchedEntityData.defineId(Peafowl::class.java, EntityDataSerializers.BOOLEAN)
        val DATA_FLY_INTENT = SynchedEntityData.defineId(Peafowl::class.java, EntityDataSerializers.BOOLEAN)
        val DATA_DISPLAY_COOLDOWN = SynchedEntityData.defineId(Peafowl::class.java, EntityDataSerializers.INT)

        val WALK_ANIM = RawAnimation.begin().thenLoop("walk")
        val IDLE_ANIM = RawAnimation.begin().thenLoop("idle")
        val READY_FLY_ANIM = RawAnimation.begin().thenPlay("takeoff")
        val FLY_IDLE_ANIM = RawAnimation.begin().thenLoop("fly_idle")
        val FLY_ANIM = RawAnimation.begin().thenLoop("flying")
        val DISPLAY_ANIM = RawAnimation.begin().thenPlay("displaying")
        val UNDISPLAY_ANIM = RawAnimation.begin().thenPlay("undisplaying")
        val DISPLAY_IDLE_ANIM = RawAnimation.begin().thenPlay("displaying_idle")
        val DISPLAY_WALK_ANIM = RawAnimation.begin().thenPlay("displaying_walk")

        fun createAttributes() = createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FLYING_SPEED, 1.0)
            .add(Attributes.JUMP_STRENGTH, 0.6)
    }

    private val animCache = GeckoLibUtil.createInstanceCache(this)
    private lateinit var flyNavigation: FlyingPathNavigation
    private lateinit var groundNavigation: PathNavigation
    private val flyMoveControl = FlyingMoveControl(this, 5, 5, false)
    private val groundMoveControl = MoveControl(this)

    var isDisplaying: Boolean
        set(value) { entityData.set(DATA_IS_DISPLAYING, value) }
        get() = entityData.get(DATA_IS_DISPLAYING)

    var displayTicks = 0
        private set
    var undisplayTicks = 0
        private set

    var flyIntent: Boolean
        set(value) {
            entityData.set(DATA_FLY_INTENT, value)
            if (value) {
                navigation = flyNavigation
                isDisplaying = false
                moveControl = flyMoveControl
            } else {
                navigation = groundNavigation
                moveControl = groundMoveControl
            }
        }
        get() = entityData.get(DATA_FLY_INTENT)

    var displayCoolDown: Int
        set(value) { entityData.set(DATA_DISPLAY_COOLDOWN, value) }
        get() = entityData.get(DATA_DISPLAY_COOLDOWN)

    override fun createNavigation(level: Level): PathNavigation {
        flyNavigation = FlyingPathNavigation(this, level).apply { setCanFloat(true) }
        groundNavigation = super.createNavigation(level)
        return groundNavigation
    }

    override fun causeFallDamage(fallDistance: Float, multiplier: Float, source: DamageSource): Boolean {
        return false
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, PeafowlBehaviorTreeGoal(this))
    }

    override fun tick() {
        super.tick()
        if (displayCoolDown > 0) displayCoolDown--
        if (isDisplaying) {
            displayTicks++
            undisplayTicks = 0
        } else {
            displayTicks = 0
            undisplayTicks++
        }
    }

    // 动画
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController(this, "controller", 5) { state ->
                val moving = state.animatable.deltaMovement.horizontalDistance() > 0.0
                when {
                    isFlying -> when {
                        moving -> state.setAndContinue(FLY_ANIM)
                        else -> state.setAndContinue(FLY_IDLE_ANIM)
                    }
                    isDisplaying -> when {
                        moving -> state.setAndContinue(DISPLAY_WALK_ANIM)
                        else -> state.setAndContinue(DISPLAY_IDLE_ANIM)
                    }
                    onGround() -> when {
                        moving -> state.setAndContinue(WALK_ANIM)
                        else -> state.setAndContinue(IDLE_ANIM)
                    }
                    else -> state.setAndContinue(IDLE_ANIM)
                }
            }
        )
        controllers.add(
            AnimationController(this, "trigger", 2) { PlayState.STOP }
                .triggerableAnim("takeoff", READY_FLY_ANIM)
                .triggerableAnim("display", DISPLAY_ANIM)
                .triggerableAnim("undisplay", UNDISPLAY_ANIM)
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return animCache
    }

    override fun isFood(stack: ItemStack): Boolean {
        return stack.`is`(Tags.Items.SEEDS)
    }

    override fun getBreedOffspring(
        level: ServerLevel,
        otherParent: AgeableMob
    ): AgeableMob? {
        return null
    }

    override fun isFlying(): Boolean {
        return flyIntent && !onGround()
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_IS_DISPLAYING, false)
        builder.define(DATA_FLY_INTENT, false)
        builder.define(DATA_DISPLAY_COOLDOWN, 0)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        isDisplaying = compound.getBoolean("is_displaying")
        flyIntent = compound.getBoolean("is_flying")
        displayCoolDown = compound.getInt("display_cool_down")
        displayTicks = compound.getInt("display_ticks")
        undisplayTicks = compound.getInt("undisplay_ticks")
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("is_displaying", isDisplaying)
        compound.putBoolean("is_flying", flyIntent)
        compound.putInt("display_cool_down", displayCoolDown)
        compound.putInt("display_ticks", displayTicks)
        compound.putInt("undisplay_ticks", undisplayTicks)
    }
}