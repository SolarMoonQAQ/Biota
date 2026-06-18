package cn.solarmoon.biota.fp.entity

import cn.solarmoon.kbehaviortree.parallelTree
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.common.Tags

val waterBuffaloBehaviorTree = parallelTree {

    // 1. 目标锁定逻辑（大脑层面，持续并行检测）
    task(HurtByTargetTask(true))

    // 2. 行为执行逻辑（身体层面，互斥执行）
    selector {
        // 逃命永远是第一位的（身体动作优先级 1）
        task(FleeFromKillerTask())

        // 只要有目标（无论是自己锁定的，还是别人传过来的），就干架（身体动作优先级 2）
        task(MeleeAttackTask())

        // --- 下面是和平时期的日常行为 ---
        task(BreedTask())
        task(TemptTask(listOf(Ingredient.of(Items.GOLDEN_APPLE)), canScare = false))
        task(BoidsWanderTask())
        task(GroundWanderTask())
        task(LookAtPlayerTask())
        task(RandomLookAroundTask)
    }

    // 3. 游泳逻辑（本能层面，并行检测）
    task(FloatTask)
}