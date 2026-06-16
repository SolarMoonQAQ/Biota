package cn.solarmoon.biota.registry.common

import cn.solarmoon.biota.Biota
import cn.solarmoon.biota.entity.peafowl.Peafowl
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object BiotaEntities {
    val ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Biota.MOD_ID)

    fun register(bus: IEventBus) {
        ENTITY_TYPES.register(bus)
        bus.addListener(::onEntityAttributes)
    }

    val PEAFOWL = ENTITY_TYPES.register("peafowl", Supplier {
        EntityType.Builder.of(::Peafowl, MobCategory.CREATURE)
            .sized(1f, 1.5f)
            .eyeHeight(1.25f)
            .build("")
    })

    private fun onEntityAttributes(event: EntityAttributeCreationEvent) {
        event.put(
            PEAFOWL.get(),
            Peafowl.createAttributes().build()
        )
    }

}