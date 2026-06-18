package cn.solarmoon.biota.entity.peafowl

import cn.solarmoon.kbehaviortree.Status
import cn.solarmoon.kbehaviortree.node.Task
import cn.solarmoon.kbehaviortree.node.execute
import cn.solarmoon.biota.entity.ai.goal.LandGoal
import cn.solarmoon.biota.entity.ai.goal.PanicFlyGoal
import cn.solarmoon.biota.entity.ai.goal.FlyToSpotWanderGoal
import cn.solarmoon.biota.entity.ai.mobTaskExecutor
import cn.solarmoon.biota.fp.entity.ActivateFlyWhileFallingTask
import cn.solarmoon.biota.fp.entity.FlyToSpotWanderTask
import cn.solarmoon.biota.fp.entity.LandTask
import cn.solarmoon.biota.fp.entity.PanicFlyTask
import cn.solarmoon.biota.fp.entity.PeafowlTask
import cn.solarmoon.biota.fp.orElse
import cn.solarmoon.biota.entity.ai.runGoalTask
import cn.solarmoon.biota.config.BehaviorTreeConfigManager
import cn.solarmoon.biota.fp.orThrow
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.level.levelgen.Heightmap

class PeafowlBehaviorTreeGoal(val peafowl: Peafowl): Goal() {

    val goalMemory = mutableMapOf<Task, Goal>()

    val peafowlTaskExecutor = { task: Task ->
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

            else -> null
        }
    }

    val taskExecutor = (peafowlTaskExecutor orElse mobTaskExecutor(peafowl, goalMemory)).orThrow()

    override fun canUse(): Boolean {
        return true
    }

    override fun tick() {
        BehaviorTreeConfigManager.getTree(BuiltInRegistries.ENTITY_TYPE.getKey(peafowl.type).path)!!.execute(taskExecutor)
    }

    override fun requiresUpdateEveryTick(): Boolean {
        return true
    }

}