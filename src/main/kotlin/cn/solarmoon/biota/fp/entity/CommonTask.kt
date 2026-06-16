package cn.solarmoon.biota.fp.entity

import cn.solarmoon.kbehaviortree.Status
import cn.solarmoon.kbehaviortree.node.Task
import cn.solarmoon.biota.fp.serialization.Vec3Serializer
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.ai.goal.Goal
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

/**
 * 3. 核心通用高阶函数：代理并管理所有 Minecraft Goal 的严格生命周期
 * 确保整个运行周期内，start/tick/stop 调用的都是【同一个 Goal 实例】
 */
inline fun runGoalTask(
    task: Task,
    memory: MutableMap<Task, Goal>,
    goalFactory: () -> Goal
): Status {
    // 从内存中尝试获取正在运行的旧实例
    val runningGoal = memory[task]

    return if (runningGoal == null) {
        // 如果没在运行，则创建新实例并尝试启动
        val newGoal = goalFactory()
        if (newGoal.canUse()) {
            newGoal.start()
            memory[task] = newGoal // 启动成功，将其放入内存锁定
            Status.RUNNING
        } else {
            Status.FAILURE
        }
    } else {
        // 如果已经在运行了，严格复用内存中的同一个旧实例！
        if (runningGoal.canContinueToUse()) {
            runningGoal.tick()
            Status.RUNNING
        } else {
            runningGoal.stop()
            memory.remove(task) // 运行结束，将其从内存中释放
            Status.SUCCESS
        }
    }
}
