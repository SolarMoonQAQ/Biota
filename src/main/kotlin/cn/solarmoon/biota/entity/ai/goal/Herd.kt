package cn.solarmoon.biota.entity.ai.goal

sealed interface HerdRole {
    data object Solo : HerdRole                     // 自由身
    data object Leader : HerdRole                   // 首领
    data class Follower(val leader: Herd) : HerdRole // 跟随者（强制绑定首领实体）
}

interface Herd {
    var herdRole: HerdRole
}