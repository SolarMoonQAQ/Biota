package cn.solarmoon.biota.entity.water_buffalo

import cn.solarmoon.biota.config.BehaviorTreeConfigManager
import cn.solarmoon.biota.entity.ai.goal.Herd
import cn.solarmoon.biota.entity.ai.goal.HerdRole
import cn.solarmoon.biota.entity.ai.mobTaskExecutor
import cn.solarmoon.biota.entity.ai.runGoalTask
import cn.solarmoon.biota.fp.entity.GroundWanderTask
import cn.solarmoon.biota.fp.entity.PanicTask
import cn.solarmoon.biota.fp.orElse
import cn.solarmoon.biota.fp.orThrow
import cn.solarmoon.kbehaviortree.node.Task
import cn.solarmoon.kbehaviortree.node.execute
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.util.DefaultRandomPos
import net.minecraft.world.entity.monster.piglin.Piglin
import net.minecraft.world.phys.Vec3

class WaterBuffaloBehaviorTreeGoal(val mob: WaterBuffalo) : Goal() {

    val goalMemory = mutableMapOf<Task, Goal>()

    val waterBuffaloTaskExecutor = { task: Task ->
        when (task) {
            is GroundWanderTask -> runGoalTask(task, goalMemory) {
                object : RandomStrollGoal(mob, task.speed) {
                    // 重写原版的找点逻辑
                    override fun getPosition(): Vec3? {
                        val minSpacing = 12 // 理想的同伴间距，可以根据水牛的碰撞箱改大点
                        val minSpacingSqr = minSpacing * minSpacing
                        val maxSearchDist = minSpacing * 2

                        // 获取周围一定范围内的同伴
                        val peers = mob.level().getEntitiesOfClass(
                            mob::class.java,
                            mob.boundingBox.inflate(16.0)
                        ) { it != mob && it.isAlive }

                        // 如果周围没有同伴，直接走原版逻辑，节省性能
                        if (peers.isEmpty()) {
                            return super.getPosition()
                        }

                        // 尝试最多 3 次，找一个周围不拥挤的目标点
                        for (i in 0..2) {
                            val candidate = DefaultRandomPos.getPos(mob, maxSearchDist, 7) ?: continue

                            val isCrowded = peers.any { peer ->
                                peer.distanceToSqr(candidate) < minSpacingSqr
                            }

                            if (!isCrowded) {
                                return candidate // 找到了好位置
                            }
                        }

                        // 如果几次都找不到宽敞的地方，返回 null。
                        // 这会让原版 RandomStrollGoal 的 canUse() 判定失败，水牛这回合就暂时站着不动。
                        return null
                    }
                }
            }
            else -> null
        }
    }

    val taskExecutor = (waterBuffaloTaskExecutor orElse mobTaskExecutor(mob, goalMemory)).orThrow()

    override fun canUse(): Boolean {
        return true
    }

    override fun tick() {
        BehaviorTreeConfigManager.getTree(BuiltInRegistries.ENTITY_TYPE.getKey(mob.type).path)!!.execute(taskExecutor)
    }

    override fun requiresUpdateEveryTick(): Boolean {
        return true
    }
}