package cn.solarmoon.biota.entity.ai.goal

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.util.DefaultRandomPos
import java.util.EnumSet

class FleeFromKillerGoal(
    private val mob: PathfinderMob,
    private val speedModifier: Double,
    private val fleeDistance: Double = 24.0, // 只要凶手在这个距离内，就继续跑
    //private val fleeDurationTicks: Long = 200L // 逃跑状态持续多久（200 tick = 10秒）
) : Goal() {

    private var targetX: Double = 0.0
    private var targetY: Double = 0.0
    private var targetZ: Double = 0.0

    init {
        // 独占移动控制权
        this.flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        val fleeStartTime = mob.persistentData.getLong("flee_start_time")

        // 1. 如果没有被注入逃跑时间，或者时间已经过去 10 秒了，恢复平静
        if (fleeStartTime == 0L /*|| mob.level().gameTime - fleeStartTime > fleeDurationTicks*/) {
            return false
        }

        // 2. 检查体内是否存了凶手的 UUID
        if (!mob.persistentData.hasUUID("herd_killer_uuid")) return false
        val killerUuid = mob.persistentData.getUUID("herd_killer_uuid")

        val level = mob.level() as? ServerLevel ?: return false

        // 3. 通过 UUID 在当前世界寻找凶手
        val killer: Entity? = level.getEntity(killerUuid)

        // 如果凶手已经下线/死了，或者凶手离我们已经足够远了（>24格），就不跑了
        if (killer == null || !killer.isAlive || mob.distanceToSqr(killer) > fleeDistance * fleeDistance) {

            // --- 彻底清空内心阴影，回归大自然 ---
            mob.persistentData.remove("flee_start_time")
            mob.persistentData.remove("herd_killer_uuid")

            return false
        }

        // 4. 核心：计算一个“远离凶手”的安全坐标 (最多跑出 16 格)
        val safePos = DefaultRandomPos.getPosAway(mob, 16, 7, killer.position()) ?: return false

        targetX = safePos.x
        targetY = safePos.y
        targetZ = safePos.z

        return true
    }

    override fun start() {
        // 以较高的速度逃跑
        mob.navigation.moveTo(targetX, targetY, targetZ, speedModifier)
    }

    override fun canContinueToUse(): Boolean {
        // 只要还没跑到目标点，并且依然在 10 秒的逃跑有效期内，就继续跑
        //val fleeStartTime = mob.persistentData.getLong("flee_start_time")
        return !mob.navigation.isDone
                //&& (mob.level().gameTime - fleeStartTime <= fleeDurationTicks)
    }

    override fun tick() {
        super.tick()
        mob.target = null
    }

    override fun stop() {
        mob.navigation.stop()
    }
}