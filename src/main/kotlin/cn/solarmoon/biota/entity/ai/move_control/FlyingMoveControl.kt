package cn.solarmoon.biota.entity.ai.move_control // 替换为你的包名

import net.minecraft.util.Mth
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import kotlin.math.abs
import kotlin.math.sqrt

class FlyingMoveControl(
    mob: Mob,
    private val maxTurnYaw: Int,   // 控制左右转身的平滑度 (推荐 10-20)
    private val maxTurnPitch: Int, // 控制上下抬头的平滑度 (推荐 10-20)
    private val hoversInPlace: Boolean
) : MoveControl(mob) {

    override fun tick() {
        if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT
            this.mob.isNoGravity = true

            val d0 = this.wantedX - this.mob.x
            val d1 = this.wantedY - this.mob.y
            val d2 = this.wantedZ - this.mob.z
            val d3 = d0 * d0 + d1 * d1 + d2 * d2

            // 距离目标极近时停止运动
            if (d3 < 2.5000003E-7) {
                this.mob.yya = 0.0f
                this.mob.zza = 0.0f
                return
            }

            // --- 修复 1：使用传入的 maxTurnYaw 来平滑水平转向 ---
            val targetYaw = (Mth.atan2(d2, d0).toFloat() * 180.0f / Math.PI.toFloat()) - 90.0f
            this.mob.yRot = this.rotlerp(this.mob.yRot, targetYaw, maxTurnYaw.toFloat())

            val speedModifier = if (this.mob.onGround()) {
                (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)).toFloat()
            } else {
                (this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED)).toFloat()
            }

            this.mob.speed = speedModifier

            val horizontalDistance = sqrt(d0 * d0 + d2 * d2)

            if (abs(d1) > 1.0E-5 || abs(horizontalDistance) > 1.0E-5) {
                // 处理上下抬头平滑度
                val targetPitch = -(Mth.atan2(d1, horizontalDistance).toFloat() * 180.0f / Math.PI.toFloat())
                this.mob.xRot = this.rotlerp(this.mob.xRot, targetPitch, maxTurnPitch.toFloat())

                // --- 修复 2：根据垂直距离按比例缓动升降速度 ---
                // 当高度差小于 2 个方块时，逐渐减速，防止上下抖动
                val yEasing = Mth.clamp(abs(d1) / 2.0, 0.1, 1.0).toFloat()
                this.mob.yya = if (d1 > 0.0) (speedModifier * yEasing) else -(speedModifier * yEasing)
            }
        } else {
            if (!this.hoversInPlace) {
                this.mob.isNoGravity = false
            }
            this.mob.yya = 0.0f
            this.mob.zza = 0.0f
        }
    }
}