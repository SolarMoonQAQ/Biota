package cn.solarmoon.biota.registry.common

import cn.solarmoon.biota.Biota
import cn.solarmoon.biota.config.BehaviorTreeConfigManager
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

object BiotaCommandRegister {

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(
            Commands.literal(Biota.MOD_ID)
                .then(
                    Commands.literal("reload_bt")
                        .requires { it.hasPermission(2) }
                        .executes { context ->
                            // 💡 核心：清除缓存并重新扫盘读取最新的 YAML
                            BehaviorTreeConfigManager.reloadAll()

                            // 向控制台/聊天框发送反馈
                            context.source.sendSuccess({
                                Component.literal("§a[SiedeFlora] 所有的行为树配置文件已成功热重载！")
                            }, true)
                            1
                        }
                )
        )
    }

    fun register() {
        NeoForge.EVENT_BUS.register(this)
    }

}