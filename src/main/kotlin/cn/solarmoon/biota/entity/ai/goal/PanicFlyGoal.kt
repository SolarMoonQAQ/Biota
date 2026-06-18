package cn.solarmoon.biota.entity.ai.goal

import cn.solarmoon.biota.entity.ai.PathFinder
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.PanicGoal

open class PanicFlyGoal(mob: PathfinderMob, speedModifier: Double) : PanicGoal(mob, speedModifier) {
    override fun findRandomPosition(): Boolean {
        val damageSource = mob.lastDamageSource!!
        val sourcePos = damageSource.sourcePosition ?: damageSource.directEntity?.position() ?: damageSource.entity?.position()
        val target = PathFinder.scatterTarget(mob.position(), sourcePos ?: mob.position(), 8.0 + mob.random.nextInt(10), 150.0, mob.random)
        posX = target.x
        posY = target.y
        posZ = target.z
        return true
    }
}