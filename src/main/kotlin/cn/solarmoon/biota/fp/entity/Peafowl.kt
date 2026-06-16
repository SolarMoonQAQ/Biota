package cn.solarmoon.biota.fp.entity

import cn.solarmoon.kbehaviortree.node.Task
import cn.solarmoon.kbehaviortree.selectorTree
import kotlinx.serialization.Serializable

@Serializable
sealed interface PeafowlTask : Task {
    @Serializable
    object IsDisplaying : PeafowlTask
    @Serializable
    data class DisplayTask(val probability: Double = 0.005) : PeafowlTask
    @Serializable
    data class UndisplayTask(val probability: Double = 0.005, val requireDisplayedTime: Int = 200) : PeafowlTask
}

val peafowlBehaviorTree = selectorTree {
    task(PanicFlyTask())
    sequence {
        task(IsFlyingTask)
        selector {
            task(LandTask())
            task(SlowDescentTask())
        }
    }
    sequence {
        task(IsFallingTask)
        selector {
            task(ActivateFlyWhileFallingTask())
        }
    }
    sequence {
        task(OnGroundTask)
        parallel(cn.solarmoon.kbehaviortree.node.ParallelPolicy.OR) {
            selector {
                sequence {
                    task(PeafowlTask.IsDisplaying)
                    selector {
                        task(PeafowlTask.UndisplayTask())
                    }
                }
                sequence {
                    invert { task(PeafowlTask.IsDisplaying) }
                    selector {
                        task(PeafowlTask.DisplayTask())
                    }
                }
                task(GroundWanderTask())
            }
            selector {
                task(LookAtPlayerTask())
                task(RandomLookAroundTask)
            }
        }
    }
    task(FlyToSpotWanderTask())
}