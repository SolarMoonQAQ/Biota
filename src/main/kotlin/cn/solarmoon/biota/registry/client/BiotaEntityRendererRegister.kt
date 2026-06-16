package cn.solarmoon.biota.registry.client

import cn.solarmoon.biota.entity.peafowl.PeafowlRenderer
import cn.solarmoon.biota.registry.common.BiotaEntities
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent

object BiotaEntityRendererRegister {

    private fun reg(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(BiotaEntities.PEAFOWL.get(), ::PeafowlRenderer)
    }

    fun register(bus: IEventBus) {
        bus.addListener(::reg)
    }

}