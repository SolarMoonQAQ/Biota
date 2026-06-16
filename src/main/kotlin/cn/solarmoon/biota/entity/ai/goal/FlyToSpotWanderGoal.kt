package cn.solarmoon.biota.entity.ai.goal

import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.util.HoverRandomPos
import net.minecraft.world.phys.Vec3

open class FlyToSpotWanderGoal(
    val mob: PathfinderMob,
    val probability: Double,
    val takeoffMovement: Vec3,
    val speed: Double,
    val minDistance: Int,
    val maxDistance: Int,
    val minHeight: Int,
    val maxHeight: Int,
    val prepareTime: Int
) : Goal() {
    var flySpot: Vec3? = null
    var prepareTick: Int = 0
    var preparing = false

    override fun canUse(): Boolean {
        if (mob.random.nextDouble() > probability || !mob.onGround()) return false
        repeat(10) {
            val pos = HoverRandomPos.getPos(
                mob,
                maxDistance,
                maxDistance,
                mob.x,
                mob.z,
                1.5707964f,
                maxHeight,
                minHeight,
            ) ?: return@repeat
            if (mob.distanceToSqr(pos) >= minDistance*minDistance) {
                flySpot = pos
                return true
            }
        }
        return false
    }

    override fun start() {
        prepareTick = 0
        preparing = true
        onPrepare()
    }

    override fun canContinueToUse(): Boolean {
        return mob.navigation.isInProgress || preparing
    }

    override fun tick() {
        if (preparing) prepareTick++

        if (prepareTick > prepareTime) {
            preparing = false
            prepareTick = 0
            onTakeoff()

            val flySpot = flySpot!!
            val currentPos = mob.position()
            val horizontalDir = Vec3(flySpot.x, currentPos.y, flySpot.z)
                .subtract(currentPos)
                .normalize()
            mob.navigation.moveTo(flySpot.x, flySpot.y, flySpot.z, speed)
            mob.deltaMovement = mob.deltaMovement.add(
                horizontalDir.x * takeoffMovement.x,
                takeoffMovement.y,
                horizontalDir.z * takeoffMovement.z
            )
        }
    }

    override fun stop() {
        preparing = false
        prepareTick = 0
    }

    open fun onPrepare() {}

    open fun onTakeoff() {}
}