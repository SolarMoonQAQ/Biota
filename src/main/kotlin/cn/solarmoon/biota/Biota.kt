package cn.solarmoon.biota

import cn.solarmoon.biota.config.BehaviorTreeConfigManager
import cn.solarmoon.biota.registry.client.BiotaEntityRendererRegister
import cn.solarmoon.biota.registry.common.BiotaCommandRegister
import cn.solarmoon.biota.registry.common.BiotaEntities
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import org.slf4j.LoggerFactory

@Mod(Biota.MOD_ID)
class Biota(
    val bus: IEventBus,
    val modContainer: ModContainer
) {

    companion object {
        const val MOD_ID = "biota"
        val LOGGER = LoggerFactory.getLogger(MOD_ID)
    }

    init {
        if (FMLEnvironment.dist.isClient) {
            BiotaEntityRendererRegister.register(bus)
        }

        BiotaEntities.register(bus)
        BiotaCommandRegister.register()

        bus.addListener(::onCommonSetup)
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            BehaviorTreeConfigManager.init()
        }
    }

}