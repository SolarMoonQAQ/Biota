package cn.solarmoon.biota.fp

import cn.solarmoon.kbehaviortree.Status
import cn.solarmoon.kbehaviortree.node.Task

infix fun ((Task) -> Status?).orElse(fallback: (Task) -> Status?): (Task) -> Status? {
    return { task -> this(task) ?: fallback(task) }
}

infix fun ((Task) -> Status?).orElse(defaultStatus: Status): (Task) -> Status {
    return { task -> this(task) ?: defaultStatus }
}

infix fun ((Task) -> Status?).orThrow(message: String? = null): (Task) -> Status {
    return { task ->
        this(task) ?: throw IllegalStateException(
            message ?: "未找到能够执行任务 [${task::class.simpleName}] 的 Executor！请检查行为树配置与 TaskExecutor 的匹配逻辑。"
        )
    }
}