package cn.solarmoon.biota.entity.water_buffalo

import cn.solarmoon.biota.Biota
import cn.solarmoon.biota.registry.common.BiotaEntities
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class WaterBuffaloModel : DefaultedEntityGeoModel<WaterBuffalo>(BiotaEntities.WATER_BUFFALO.id, "AllHead") {

    private data class EntityResourceSet(
        val model: ResourceLocation,
        val texture: ResourceLocation,
        val animation: ResourceLocation
    )

    companion object {
        private val PATH = BiotaEntities.WATER_BUFFALO.id.path

        // 💡 将 Key 改为 Triple<是否驯服, 是否是幼崽, 变种>
        private val RESOURCE_CACHE: Map<Triple<Boolean, Boolean, WaterBuffaloVariant>, EntityResourceSet> = buildMap {
            for (isTame in listOf(true, false)) {
                for (isBaby in listOf(true, false)) {
                    for (variant in WaterBuffaloVariant.entries) {
                        val variantName = variant.name.lowercase()

                        // 👈 2. 组合前缀逻辑：如果是 baby 则在 wild_/tamed_ 前面再叠加 baby_
                        val prefix = if (isBaby) "baby_" else if (isTame) "tamed_" else "wild_"

                        val modelLoc = ResourceLocation.fromNamespaceAndPath(Biota.MOD_ID, "geo/entity/$PATH/$prefix$PATH.geo.json")
                        val textureLoc = ResourceLocation.fromNamespaceAndPath(Biota.MOD_ID, "textures/entity/$PATH/$prefix${PATH}_$variantName.png")
                        val animLoc = ResourceLocation.fromNamespaceAndPath(Biota.MOD_ID, "animations/entity/$PATH/$prefix$PATH.animation.json")

                        put(Triple(isTame, isBaby, variant), EntityResourceSet(modelLoc, textureLoc, animLoc))
                    }
                }
            }
        }

        private fun getResourcesFor(animatable: WaterBuffalo): EntityResourceSet {
            // 💡 3. 获取时传入实体的 isBaby 状态（注：Minecraft 原生方法一般为 isBaby）
            val key = Triple(animatable.isTame, animatable.isBaby, animatable.variant)
            return RESOURCE_CACHE[key] ?: throw IllegalArgumentException("No resources for $animatable")
        }
    }

    override fun getTextureResource(animatable: WaterBuffalo): ResourceLocation {
        return getResourcesFor(animatable).texture
    }

    override fun getModelResource(animatable: WaterBuffalo): ResourceLocation {
        return getResourcesFor(animatable).model
    }

    override fun getAnimationResource(animatable: WaterBuffalo): ResourceLocation {
        return getResourcesFor(animatable).animation
    }
}