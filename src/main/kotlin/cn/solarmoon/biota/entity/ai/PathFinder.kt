package cn.solarmoon.biota.entity.ai

import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PathFinder {
    /**
     * 计算鸟群被惊吓后呈扇形飞散的目标点
     * @param currentPos 当前鸟位置
     * @param sourcePos 惊吓源点
     * @param radius 飞散半径
     * @param maxSpreadDegrees 扇形半边最大开角（度数，如45.0表示总计90度的扇形）
     * @param random 随机源
     */
    fun scatterTarget(
        currentPos: Vec3,
        sourcePos: Vec3,
        radius: Double,
        maxSpreadDegrees: Double,
        random: RandomSource
    ): Vec3 {
        // 1. 计算基础远离向量
        var away = currentPos.subtract(sourcePos)

        // 防御边界：如果鸟和源完全重合，给一个随机的基础方向，防止计算出NaN
        if (away.x == 0.0 && away.z == 0.0) {
            val randomAngle = random.nextDouble() * Math.PI * 2
            away = Vec3(cos(randomAngle), 0.0, sin(randomAngle))
        }

        // 2. 计算水平面上的基础逃离夹角 (弧度值)
        // atan2(z, x) 可以精确算出这个向量在 XZ 平面上的绝对角度
        val baseYaw = atan2(away.z, away.x)

        // 3. 在 [-maxSpreadDegrees, +maxSpreadDegrees] 之间生成随机偏转角，并转为弧度
        val randomSpreadPercent = (random.nextDouble() - 0.5) * 2.0 // 生成 -1.0 到 1.0 的系数
        val spreadRadians = Math.toRadians(maxSpreadDegrees) * randomSpreadPercent

        // 最终的扇形散开角度
        val targetYaw = baseYaw + spreadRadians

        // 4. 根据新的角度，重新构建 X 和 Z 的水平方向向量
        val scatterX = cos(targetYaw)
        val scatterZ = sin(targetYaw)

        // 5. Y轴（上下高度）控制：让鸟在逃跑时产生轻微的起伏或斜向上飞
        // 这样扇形就有了 3D 的厚度，而不是死板地在绝对水平面飞
        val scatterY = if (away.y != 0.0) {
            // 如果原本就有垂直位移趋势（比如从下往上被打），保留并施加微弱随机扰动
            away.normalize().y + (random.nextDouble() - 0.5) * 0.2
        } else {
            // 如果是平地受惊，倾向于往上扬（0.0 到 0.25 的爬升力），看起来更像起飞
            random.nextDouble() * 0.25
        }

        // 6. 组合成最终的偏转方向向量，并重新归一化
        val scatterDir = Vec3(scatterX, scatterY, scatterZ).normalize()

        // 7. 最终目标点：当前位置 + 扇形方向延伸指定半径
        return currentPos.add(scatterDir.scale(radius))
    }
}