package cn.solarmoon.biota.entity.peafowl

import cn.solarmoon.kbehaviortree.Status
import cn.solarmoon.kbehaviortree.node.Task
import cn.solarmoon.kbehaviortree.node.execute
import cn.solarmoon.biota.entity.ai.LandGoal
import cn.solarmoon.biota.entity.ai.PanicFlyGoal
import cn.solarmoon.biota.entity.ai.debug
import cn.solarmoon.biota.entity.ai.goal.FlyToSpotWanderGoal
import cn.solarmoon.biota.entity.ai.toTask
import cn.solarmoon.biota.fp.entity.ActivateFlyWhileFallingTask
import cn.solarmoon.biota.fp.entity.FloatTask
import cn.solarmoon.biota.fp.entity.FlyToSpotWanderTask
import cn.solarmoon.biota.fp.entity.GroundWanderTask
import cn.solarmoon.biota.fp.entity.IsFallingTask
import cn.solarmoon.biota.fp.entity.IsFlyingTask
import cn.solarmoon.biota.fp.entity.LandTask
import cn.solarmoon.biota.fp.entity.LookAtPlayerTask
import cn.solarmoon.biota.fp.entity.OnGroundTask
import cn.solarmoon.biota.fp.entity.PanicFlyTask
import cn.solarmoon.biota.fp.entity.PeafowlTask
import cn.solarmoon.biota.fp.entity.RandomLookAroundTask
import cn.solarmoon.biota.fp.entity.SlowDescentTask
import cn.solarmoon.biota.fp.entity.runGoalTask
import cn.solarmoon.biota.fp.serialization.BehaviorTreeConfigManager
import com.tpcly.behaviourtree.node.*
import com.tpcly.behaviourtree.node.composite.ParallelPolicy
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.util.HoverRandomPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3

class PeafowlBehaviorTreeGoal(val peafowl: Peafowl): Goal() {

    val goalMemory = mutableMapOf<Task, Goal>()

    val taskExecutor = { task: Task ->
        when (task) {
            is PanicFlyTask -> runGoalTask(task, goalMemory) {
                object : PanicFlyGoal(peafowl, task.speed) {
                    override fun start() {
                        if (peafowl.onGround()) peafowl.triggerAnim("trigger", "takeoff")
                        peafowl.flyIntent = true
                        super.start()
                    }
                }
            }
            is FlyToSpotWanderTask -> runGoalTask(task, goalMemory) {
                object : FlyToSpotWanderGoal(peafowl, task.probability, task.takeoffMovement, task.speed, task.minDistance, task.maxDistance, task.minHeight, task.maxHeight, task.prepareTime) {
                    override fun canUse(): Boolean {
                        return super.canUse() && !peafowl.isDisplaying
                    }

                    override fun onPrepare() {
                        peafowl.triggerAnim("trigger", "takeoff")
                        peafowl.lookAt(EntityAnchorArgument.Anchor.FEET, flySpot!!)
                    }

                    override fun onTakeoff() {
                        peafowl.flyIntent = true
                    }

                    override fun stop() {
                        super.stop()
                        peafowl.flyIntent = false
                    }
                }
            }
            is LandTask -> runGoalTask(task, goalMemory) {
                object : LandGoal(peafowl, task.speed) {
                    override fun stop() {
                        super.stop()
                        peafowl.flyIntent = false
                    }
                }
            }
            is SlowDescentTask -> {
                peafowl.deltaMovement = peafowl.deltaMovement.multiply(1.0, 0.9, 1.0)
                Status.SUCCESS
            }
            is GroundWanderTask -> runGoalTask(task, goalMemory) {
                RandomStrollGoal(peafowl, task.speed)
            }
            is LookAtPlayerTask -> runGoalTask(task, goalMemory) {
                LookAtPlayerGoal(peafowl, Player::class.java, task.distance, task.probability)
            }
            is RandomLookAroundTask -> runGoalTask(task, goalMemory) {
                RandomLookAroundGoal(peafowl)
            }
            is FloatTask -> runGoalTask(task, goalMemory) {
                FloatGoal(peafowl)
            }
            is IsFlyingTask -> Status.condition(peafowl.isFlying)
            is OnGroundTask -> Status.condition(peafowl.onGround())
            is IsFallingTask -> Status.condition(!peafowl.onGround() && !peafowl.isFlying)
            is ActivateFlyWhileFallingTask -> {
                val groundY = peafowl.level().getHeight(Heightmap.Types.MOTION_BLOCKING, peafowl.onPos.x, peafowl.onPos.z)
                val distance = peafowl.y - groundY
                if (distance > 4) {
                    peafowl.flyIntent = true
                    Status.SUCCESS
                } else Status.FAILURE
            }

            is PeafowlTask.IsDisplaying -> Status.condition(peafowl.isDisplaying)
            is PeafowlTask.DisplayTask -> runGoalTask(task, goalMemory) {
                object : Goal() {
                    var prepareTick = 0

                    override fun canUse(): Boolean {
                        return !peafowl.isDisplaying && peafowl.random.nextDouble() < task.probability && peafowl.displayCoolDown == 0
                    }

                    override fun start() {
                        prepareTick = 0
                        peafowl.triggerAnim("trigger", "display")
                        peafowl.navigation.stop()
                    }

                    override fun canContinueToUse(): Boolean {
                        return prepareTick < 40
                    }

                    override fun tick() {
                        prepareTick++
                    }

                    override fun stop() {
                        peafowl.isDisplaying = true
                        peafowl.displayCoolDown = 3600
                    }

                    override fun requiresUpdateEveryTick(): Boolean {
                        return true
                    }
                }
            }
            is PeafowlTask.UndisplayTask -> runGoalTask(task, goalMemory) {
                object : Goal() {
                    var prepareTick = 0

                    override fun canUse(): Boolean {
                        return peafowl.isDisplaying && peafowl.displayTicks > task.requireDisplayedTime && peafowl.random.nextDouble() < task.probability
                    }

                    override fun start() {
                        prepareTick = 0
                        peafowl.triggerAnim("trigger", "undisplay")
                        peafowl.navigation.stop()
                    }

                    override fun canContinueToUse(): Boolean {
                        return prepareTick < 80
                    }

                    override fun tick() {
                        prepareTick++
                    }

                    override fun stop() {
                        peafowl.isDisplaying = false
                    }

                    override fun requiresUpdateEveryTick(): Boolean {
                        return true
                    }
                }
            }

            else -> Status.FAILURE
        }
    }

    override fun canUse(): Boolean {
        return true
    }

    override fun tick() {
        //behaviorTree.execute()
        BehaviorTreeConfigManager.getTree("peafowl")!!.execute(taskExecutor)
    }

    override fun requiresUpdateEveryTick(): Boolean {
        return true
    }

}