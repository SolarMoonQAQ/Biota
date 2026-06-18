package cn.solarmoon.biota.entity.water_buffalo

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer
import software.bernie.geckolib.util.RenderUtil

class WaterBuffaloRenderer(context: EntityRendererProvider.Context) : GeoEntityRenderer<WaterBuffalo>(context, WaterBuffaloModel()) {

    init {
        addRenderLayer(PlowLayer(this))
    }

    class PlowLayer(renderer: GeoEntityRenderer<WaterBuffalo>) : GeoRenderLayer<WaterBuffalo>(renderer) {

        private val plowModel = PlowModel()

        override fun render(
            poseStack: PoseStack,
            animatable: WaterBuffalo,
            bakedModel: BakedGeoModel,
            renderType: RenderType?,
            bufferSource: MultiBufferSource,
            buffer: VertexConsumer?,
            partialTick: Float,
            packedLight: Int,
            packedOverlay: Int
        ) {
            if (animatable.plow.isEmpty) return

            val bone = this.geoModel.getBone("FrontBody").orElse(null) ?: return

            // 1. 获取子模型的资源数据
            val modelResource = plowModel.getModelResource(animatable)
            val texture = plowModel.getTextureResource(animatable)
            val plowBakedModel = plowModel.getBakedModel(modelResource)

            poseStack.pushPose()

            // 2. 将矩阵对齐到骨骼
            RenderUtil.prepMatrixForBone(poseStack, bone)

            // 3. 【核心】直接调用 reRender，让主渲染器帮你处理所有渲染细节
            // 注意：RenderType.entitySolid 是最稳妥的，它会处理法线，保证不会全黑
            val renderType = RenderType.entitySolid(texture)

            this.renderer.reRender(
                plowBakedModel,
                poseStack,
                bufferSource,
                animatable,
                renderType,
                bufferSource.getBuffer(renderType),
                partialTick,
                packedLight,
                packedOverlay,
                16777215        // 传入白色，表示没有任何颜色乘数覆盖，显示贴图原本颜色
            )

            poseStack.popPose()
        }
    }

}