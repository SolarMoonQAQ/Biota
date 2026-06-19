package cn.solarmoon.biota.fp.entity

import cn.solarmoon.kbehaviortree.parallelTree
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.common.Tags

val waterBuffaloBehaviorTree = parallelTree {
    task(HurtByTargetTask(true))
    selector {
        task(FleeFromKillerTask())

        // 只要有目标（无论是自己锁定的，还是别人传过来的），就干架
        task(MeleeAttackTask())

        // --- 下面是和平时期的日常行为 ---
        task(BreedTask())
        task(TemptTask(listOf(Ingredient.of(Items.GOLDEN_APPLE)), canScare = false))
        task(FollowParentTask())
        task(BoidsWanderTask())
        task(GroundWanderTask())
        task(LookAtPlayerTask())
        task(RandomLookAroundTask)
    }
    task(FloatTask)
}