package cn.solarmoon.biota.entity.ai.goal

import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.util.LandRandomPos
import net.minecraft.world.phys.Vec3

open class LandGoal(val mob: PathfinderMob, val speed: Double) : Goal() {
    private var targetPos: Vec3? = null

    override fun canUse(): Boolean {
        if (mob.onGround()) return false
        targetPos = LandRandomPos.getPos(mob, 15, 15)
        return targetPos != null
    }

    override fun start() {
        targetPos?.let {
            mob.navigation.moveTo(it.x, it.y, it.z, speed)
        }
    }

    override fun canContinueToUse(): Boolean {
        return !mob.navigation.isDone
    }
}