package cn.solarmoon.biota.entity.ai

import com.tpcly.behaviourtree.Status
import com.tpcly.behaviourtree.TreeNodeResult
import com.tpcly.behaviourtree.node.TreeNode
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.util.HoverRandomPos
import net.minecraft.world.entity.ai.util.LandRandomPos
import net.minecraft.world.phys.Vec3

fun TreeNode.debug(content: String = "", match: Status = Status.SUCCESS) = object : TreeNode {
    override val name: String = this@debug.name
    override fun execute(): TreeNodeResult {
        val result = this@debug.execute()
        if (result.status == match) {
            println(content)
        }
        return result
    }
}

open class PanicFlyGoal(mob: PathfinderMob, speedModifier: Double) : PanicGoal(mob, speedModifier) {
    override fun findRandomPosition(): Boolean {
        val damageSource = mob.lastDamageSource!!
        val sourcePos = damageSource.sourcePosition ?: damageSource.directEntity?.position() ?: damageSource.entity?.position()
        val target = PathFinder.scatterTarget(mob.position(), sourcePos!!, 8.0 + mob.random.nextInt(10), 150.0, mob.random)
        posX = target.x
        posY = target.y
        posZ = target.z
        return true
    }
}

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

open class ShortFlightRelocateGoal(
    val mob: PathfinderMob,
    val takeoffHeight: Double,
    val takeoffSpeed: Double,
    val speed: Double
) : Goal() {
    var targetX = 0.0
    var targetY = 0.0
    var targetZ = 0.0

    private val horizontalRadius = 24
    private val verticalRadius = 12
    private val minDistance = 8.0

    override fun canUse(): Boolean {
        if (!mob.onGround()) return false

        val minDistanceSqr = minDistance * minDistance
        var finalPos: Vec3? = null

        for (i in 0 until 10) {
            val pos = HoverRandomPos.getPos(
                mob,
                horizontalRadius,
                verticalRadius,
                mob.x,
                mob.z,
                1.5707964f,
                10,
                1
            ) ?: continue

            if (mob.distanceToSqr(pos) >= minDistanceSqr) {
                finalPos = pos
                break
            }
        }

        if (finalPos == null) return false

        targetX = finalPos.x
        targetY = finalPos.y
        targetZ = finalPos.z
        return true
    }

    override fun start() {
        val currentPos = mob.position()
        val horizontalDir = Vec3(targetX, currentPos.y, targetZ)
            .subtract(currentPos)
            .normalize()

        mob.navigation.moveTo(targetX, targetY, targetZ, speed)
        mob.deltaMovement = mob.deltaMovement.add(
            horizontalDir.x * takeoffSpeed,
            takeoffHeight,
            horizontalDir.z * takeoffSpeed
        )
    }

    override fun canContinueToUse(): Boolean {
        return !mob.navigation.isDone
    }
}


