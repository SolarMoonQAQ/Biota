package cn.solarmoon.biota.entity.peafowl

import cn.solarmoon.biota.Biota
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class PeafowlModel:
    DefaultedEntityGeoModel<Peafowl>(ResourceLocation.fromNamespaceAndPath(Biota.MOD_ID, "peafowl"), "Head") {
    companion object {
        val TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(Biota.MOD_ID, "textures/entity/green_peafowl.png")
    }

    override fun getTextureResource(animatable: Peafowl): ResourceLocation {
        return TEXTURE_LOCATION
    }

    override fun setCustomAnimations(
        animatable: Peafowl,
        instanceId: Long,
        animationState: AnimationState<Peafowl>
    ) {
        val head = animationProcessor.getBone(this.headBone)
        val entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA)!!
        head.rotY = -entityData.headPitch() * Mth.DEG_TO_RAD
        head.rotZ = -entityData.netHeadYaw() * Mth.DEG_TO_RAD
    }
}