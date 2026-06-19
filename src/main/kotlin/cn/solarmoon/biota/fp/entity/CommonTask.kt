package cn.solarmoon.biota.fp.entity

import cn.solarmoon.biota.fp.serialization.IngredientSerializer
import cn.solarmoon.kbehaviortree.node.Task
import cn.solarmoon.biota.fp.serialization.Vec3Serializer
import kotlinx.serialization.Serializable
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.phys.Vec3

@Serializable
object IsFlyingTask : Task
@Serializable
object OnGroundTask : Task
@Serializable
data class LookAtPlayerTask(val distance: Float = 6.0f, val probability: Float = 0.01f) : Task
@Serializable
object RandomLookAroundTask : Task
@Serializable
object FloatTask : Task
@Serializable
data class GroundWanderTask(val speed: Double = 0.6) : Task
@Serializable
data class PanicTask(val speed: Double = 2.0) : Task
@Serializable
data class PanicFlyTask(val speed: Double = 2.0) : Task
@Serializable
data class FlyToSpotWanderTask(
    val probability: Double = 0.005,
    @Serializable(Vec3Serializer::class)
    val takeoffMovement: Vec3 = Vec3(0.7, 0.5, 0.7),
    val speed: Double = 2.0,
    val minDistance: Int = 6,
    val maxDistance: Int = 24,
    val minHeight: Int = 1,
    val maxHeight: Int = 12,
    val prepareTime: Int = 10
) : Task
@Serializable
data class LandTask(val speed: Double = 1.0) : Task
@Serializable
data class SlowDescentTask(val multiplier: Double = 0.9) : Task
@Serializable
object IsFallingTask : Task
@Serializable
data class ActivateFlyWhileFallingTask(val distanceFromGround: Double = 4.0) : Task
@Serializable
data class TemptTask(
    val items: List<@Serializable(with = IngredientSerializer::class) Ingredient>,
    val speed: Double = 1.0,
    val canScare: Boolean = true
) : Task
@Serializable
data class BreedTask(val speed: Double = 1.0) : Task
@Serializable
data class BoidsWanderTask(val speed: Double = 0.6) : Task
@Serializable
data class HerdPanicTask(val speed: Double = 2.0) : Task
@Serializable
data class HurtByTargetTask(val alertOthers: Boolean = false) : Task
@Serializable
data class MeleeAttackTask(val speed: Double = 1.5, val followTargetEventIfNotSeen: Boolean = false) : Task
@Serializable
data class FleeFromKillerTask(val speedModifier: Double = 1.5, val fleeDistance: Double = 24.0, val fleeDurationTicks: Long = 200L) : Task
@Serializable
data class FollowParentTask(val speed: Double = 1.0) : Task
