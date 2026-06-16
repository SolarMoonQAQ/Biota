package cn.solarmoon.biota.fp.serialization

import cn.solarmoon.kbehaviortree.node.TreeNode
import cn.solarmoon.biota.fp.entity.ActivateFlyWhileFallingTask
import cn.solarmoon.biota.fp.entity.FloatTask
import cn.solarmoon.biota.fp.entity.FlyToSpotWanderTask
import cn.solarmoon.biota.fp.entity.GroundWanderTask
import cn.solarmoon.biota.fp.entity.IsFallingTask
import cn.solarmoon.biota.fp.entity.IsFlyingTask
import cn.solarmoon.biota.fp.entity.LandTask
import cn.solarmoon.biota.fp.entity.LookAtPlayerTask
import cn.solarmoon.biota.fp.entity.OnGroundTask
import cn.solarmoon.biota.fp.entity.PanicFlyTask
import cn.solarmoon.biota.fp.entity.PeafowlTask
import cn.solarmoon.biota.fp.entity.RandomLookAroundTask
import cn.solarmoon.biota.fp.entity.SlowDescentTask
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import net.minecraft.world.phys.Vec3
import kotlin.reflect.KClass

val serializeModule = SerializersModule {
    polymorphic(TreeNode::class) {
        registerAuto(IsFlyingTask::class)
        registerAuto(OnGroundTask::class)
        registerAuto(LookAtPlayerTask::class)
        registerAuto(RandomLookAroundTask::class)
        registerAuto(FloatTask::class)
        registerAuto(GroundWanderTask::class)
        registerAuto(PanicFlyTask::class)
        registerAuto(FlyToSpotWanderTask::class)
        registerAuto(LandTask::class)
        registerAuto(SlowDescentTask::class)
        registerAuto(IsFallingTask::class)
        registerAuto(ActivateFlyWhileFallingTask::class)
        registerAuto(PeafowlTask.IsDisplaying::class)
        registerAuto(PeafowlTask.DisplayTask::class)
        registerAuto(PeafowlTask.UndisplayTask::class)
    }
}

@OptIn(InternalSerializationApi::class)
fun <T : TreeNode> PolymorphicModuleBuilder<TreeNode>.registerAuto(
    clazz: KClass<T>,
    serializer: KSerializer<T> = clazz.serializer()
) {
    val className = clazz.simpleName ?: return

    // 自动规则：去掉末尾的 "Task" 或 "Node"（如果有的话）
    val cleanName = className.removeSuffix("Task").removeSuffix("Node")

    // 自动规则：驼峰转蛇形 (PanicFly -> panic_fly)
    val snakeName = cleanName.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

    // 注册带有新名字的序列化器
    subclass(clazz, AutoNamedSerializer(serializer, snakeName))
}

class AutoNamedSerializer<T>(
    private val delegate: KSerializer<T>,
    private val customName: String
) : KSerializer<T> {
    @OptIn(SealedSerializationApi::class)
    override val descriptor: SerialDescriptor = object : SerialDescriptor by delegate.descriptor {
        override val serialName: String = customName // 覆盖原本冗长的包名
    }
    override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): T = delegate.deserialize(decoder)
}

object Vec3Serializer : KSerializer<Vec3> {
    // 1. 复用 DoubleArray 的结构描述符，这样在 JSON 中就会体现为 [1.0, 2.0, 3.0]
    override val descriptor: SerialDescriptor = DoubleArraySerializer().descriptor

    // 2. 序列化：将 Vec3 拆解为 doubleArray 写入 JSON
    override fun serialize(encoder: Encoder, value: Vec3) {
        val array = doubleArrayOf(value.x, value.y, value.z)
        encoder.encodeSerializableValue(DoubleArraySerializer(), array)
    }

    // 3. 反序列化：从 JSON 读取双精度数组，并重新装配成 Vec3
    override fun deserialize(decoder: Decoder): Vec3 {
        val array = decoder.decodeSerializableValue(DoubleArraySerializer())
        require(array.size == 3) { "Vec3 必须包含且仅包含 3 个元素 (x, y, z)，当前长度为: ${array.size}" }
        return Vec3(array[0], array[1], array[2])
    }
}