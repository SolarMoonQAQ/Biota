package cn.solarmoon.biota.entity.ai

import cn.solarmoon.biota.entity.ai.goal.FleeFromKillerGoal
import cn.solarmoon.biota.entity.ai.goal.Herd
import cn.solarmoon.biota.entity.ai.goal.HerdFollowGoal
import cn.solarmoon.biota.entity.ai.goal.HerdPanicGoal
import cn.solarmoon.biota.fp.entity.*
import cn.solarmoon.kbehaviortree.Status
import cn.solarmoon.kbehaviortree.node.Task
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.entity.player.Player

fun mobTaskExecutor(entity: PathfinderMob, goalMemory: MutableMap<Task, Goal>) = { task: Task ->
    when (task) {
        is SlowDescentTask -> {
            entity.deltaMovement = entity.deltaMovement.multiply(1.0, 0.9, 1.0)
            Status.SUCCESS
        }
        is GroundWanderTask -> runGoalTask(task, goalMemory) {
            RandomStrollGoal(entity, task.speed)
        }
        is LookAtPlayerTask -> runGoalTask(task, goalMemory) {
            LookAtPlayerGoal(entity, Player::class.java, task.distance, task.probability)
        }
        is RandomLookAroundTask -> runGoalTask(task, goalMemory) {
            RandomLookAroundGoal(entity)
        }
        is FloatTask -> runGoalTask(task, goalMemory) {
            FloatGoal(entity)
        }
        is TemptTask -> runGoalTask(task, goalMemory) {
            TemptGoal(entity, task.speed, { held -> task.items.any { it.test(held) } }, task.canScare)
        }
        is BreedTask -> if (entity is Animal) {
            runGoalTask(task, goalMemory) {
                BreedGoal(entity, task.speed)
            }
        } else Status.FAILURE
        is BoidsWanderTask -> if (entity is Herd) {
            runGoalTask(task, goalMemory) {
                HerdFollowGoal(entity, task.speed)
            }
        } else Status.FAILURE
        is PanicTask -> runGoalTask(task, goalMemory) {
            PanicGoal(entity, task.speed)
        }
        is HerdPanicTask -> runGoalTask(task, goalMemory) {
            HerdPanicGoal(entity, task.speed)
        }
        is HurtByTargetTask -> runGoalTask(task, goalMemory) {
            object : HurtByTargetGoal(entity) {
                init {
                    if (task.alertOthers) setAlertOthers()
                }
            }
        }
        is MeleeAttackTask -> runGoalTask(task, goalMemory) {
            MeleeAttackGoal(entity, task.speed, task.followTargetEventIfNotSeen)
        }
        is FleeFromKillerTask -> runGoalTask(task, goalMemory) {
            FleeFromKillerGoal(entity, task.speedModifier, task.fleeDistance)
        }
        is IsFlyingTask -> Status.condition(entity is FlyingAnimal && entity.isFlying)
        is OnGroundTask -> Status.condition(entity.onGround())
        is IsFallingTask -> Status.condition(!entity.onGround() && entity is FlyingAnimal && !entity.isFlying)

        else -> null
    }
}

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

