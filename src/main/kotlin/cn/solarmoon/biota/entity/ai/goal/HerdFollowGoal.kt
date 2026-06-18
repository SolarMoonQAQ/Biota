package cn.solarmoon.biota.entity.ai.goal

import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.util.DefaultRandomPos
import java.util.*

class HerdFollowGoal<E>(
    private val mob: E,
    private val speedModifier: Double,
    private val maxDistance: Double = 16.0,
    private val minDistance: Double = 12.0,
    private val maxHerdSize: Int = 10 // 新增：群落的最大容量（包含首领）
) : Goal() where E : PathfinderMob, E : Herd {

    private var targetX: Double = 0.0
    private var targetY: Double = 0.0
    private var targetZ: Double = 0.0
    private var leader: Herd? = null

    init {
        this.flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (mob.herdRole is HerdRole.Leader) return false

        val neighbors = mob.level().getEntitiesOfClass(mob::class.java, mob.boundingBox.inflate(maxDistance * 2)) { it != mob }
            .filterIsInstance<Herd>()

        if (neighbors.isEmpty()) return false

        var currentLeader: Herd? = null

        // 1. 优先检查自己是否已经加入了某个群，防止反复横跳
        val myCurrentRole = mob.herdRole
        if (myCurrentRole is HerdRole.Follower && (myCurrentRole.leader as? PathfinderMob)?.isAlive == true) {
            currentLeader = myCurrentRole.leader
        } else {
            // 2. 如果自己是自由身，找出周围所有的现存首领
            val existingLeaders = neighbors.mapNotNull {
                when(val role = it.herdRole) {
                    is HerdRole.Leader -> it
                    is HerdRole.Follower -> role.leader
                    else -> null
                }
            }.distinct()

            // 3. 遍历首领，找一个没满员的加入
            for (potentialLeader in existingLeaders) {
                // 统计附近有多少小弟属于这个首领
                val followerCount = neighbors.count {
                    val role = it.herdRole
                    role is HerdRole.Follower && role.leader == potentialLeader
                }

                // 队伍总人数 = 小弟数量 + 首领自己 (1)
                if (followerCount + 1 < maxHerdSize) {
                    currentLeader = potentialLeader
                    break
                }
            }
        }

        // 4. 如果周围没有群，或者所有的群都满了
        if (currentLeader == null) {
            // 自立为王：自己变成新群落的首领，并中止当前的跟随 Goal
            mob.herdRole = HerdRole.Leader
            return false
        }

        val leaderMob = currentLeader as? PathfinderMob ?: return false
        if (!leaderMob.isAlive) return false

        // 确立跟随关系
        this.leader = currentLeader
        mob.herdRole = HerdRole.Follower(currentLeader)

        // 5. 性能优化：如果距离没有超过最大距离，不需要跟随，直接返回 false 让游荡 Goal 接管
        if (mob.distanceToSqr(leaderMob) <= (maxDistance * maxDistance)) {
            return false
        }

        // 6. 只有确实离群了，才开始找点和间距检测
        val targetPos = DefaultRandomPos.getPos(leaderMob, maxDistance.toInt(), 4) ?: return false

        if (neighbors.any { it is PathfinderMob && it.distanceToSqr(targetPos) < (minDistance * minDistance) }) {
            return false // 落脚点太拥挤，放弃本次移动
        }

        targetX = targetPos.x
        targetY = targetPos.y
        targetZ = targetPos.z

        return true
    }

    override fun start() {
        mob.navigation.moveTo(targetX, targetY, targetZ, speedModifier)
    }

    override fun canContinueToUse(): Boolean {
        return mob.navigation.isInProgress
    }

    override fun stop() {
        mob.navigation.stop()
    }
}