package cn.solarmoon.biota.entity.ai.goal

import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.PanicGoal

open class HerdPanicGoal(mob: PathfinderMob, speed: Double) : PanicGoal(mob, speed) {

    override fun shouldPanic(): Boolean {
        // 1. 本源恐慌（自己被打或着火），拥有最高优先级
        if (super.shouldPanic()) return true

        // --- 以下是传染恐慌逻辑 ---

        // 2. 冷却机制：如果自己刚跑完停下来不到  秒，不要立刻再次被传染，防止左脚踩右脚
        val lastPanicTime = mob.persistentData.getLong("panic_start_time")
        if (lastPanicTime != 0L &&(mob.level().gameTime - lastPanicTime < 160)) return false

        // 3. 概率控制，产生错落感
        if (mob.random.nextDouble() > 0.1) return false

        // 4. 寻找周围同类
        val neighbors = mob.level().getEntitiesOfClass(mob::class.java, mob.boundingBox.inflate(8.0)) { it != mob && it.isAlive }

        // 5. 核心逻辑：不查 Boolean，查时间戳！
        val panickingNeighbor = neighbors.firstOrNull {
            val neighborPanicStart = it.persistentData.getLong("panic_start_time")
            // 只要邻居是在最近的 40 tick (2秒) 内刚刚开始恐慌的，才会被传染
            neighborPanicStart > 0L && (mob.level().gameTime - neighborPanicStart) < 40
        }

        return panickingNeighbor != null
    }

    override fun start() {
        super.start()
        // 启动时，打上当前世界的时间戳
        mob.persistentData.putLong("panic_start_time", mob.level().gameTime)
    }
}