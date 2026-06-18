package cn.solarmoon.biota.fp.serialization

import cn.solarmoon.biota.fp.entity.*
import cn.solarmoon.kbehaviortree.node.TreeNode
import com.mojang.serialization.JsonOps
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import net.minecraft.util.GsonHelper
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.phys.Vec3
import kotlin.reflect.KClass

val serializeModule = SerializersModule {
    polymorphic(TreeNode::class) {
        registerAuto(IsFlyingTask::class)
        registerAuto(OnGroundTask::class)
        registerAuto(LookAtPlayerTask::class)
        registerAuto(RandomLookAroundTask::class)
        registerAuto(FloatTask::class)
        registerAuto(PanicTask::class)
        registerAuto(GroundWanderTask::class)
        registerAuto(PanicFlyTask::class)
        registerAuto(FlyToSpotWanderTask::class)
        registerAuto(LandTask::class)
        registerAuto(SlowDescentTask::class)
        registerAuto(IsFallingTask::class)
        registerAuto(ActivateFlyWhileFallingTask::class)
        registerAuto(TemptTask::class)
        registerAuto(BreedTask::class)
        registerAuto(BoidsWanderTask::class)
        registerAuto(HerdPanicTask::class)
        registerAuto(HurtByTargetTask::class)
        registerAuto(MeleeAttackTask::class)
        registerAuto(FleeFromKillerTask::class)

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

object IngredientSerializer : KSerializer<Ingredient> {

    // 💡 核心魔法：向外界（包括 Kaml）声明，我们这个类的结构就是一个 List<String>
    private val delegateSerializer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Ingredient) {
        // 1. 用原版 Codec 序列化出 Gson 对象
        val gsonElement = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, value)
            .getOrThrow { java.lang.IllegalStateException("序列化 Ingredient 失败: $it") }

        // 2. 将 Gson 对象“压扁”成干净的 List<String>
        val stringList = extractStrings(gsonElement)

        // 3. 安全地按照 List<String> 格式交给任何 Encoder（彻底绕过 JsonEncoder 限制）
        encoder.encodeSerializableValue(delegateSerializer, stringList)
    }

    override fun deserialize(decoder: Decoder): Ingredient {
        // 1. 安全地从 YAML/JSON 中读出 List<String>
        val stringList = decoder.decodeSerializableValue(delegateSerializer)

        // 2. 重新组装成原版 Codec 认识的 JsonArray 结构
        val gsonArray = com.google.gson.JsonArray()
        for (str in stringList) {
            val cleanStr = str.trim()
            when {
                cleanStr.startsWith("{") -> {
                    // 应对极少数复杂的自定义对象直接手写 JSON 的情况
                    gsonArray.add(GsonHelper.parse(cleanStr))
                }
                cleanStr.startsWith("#") -> {
                    // 标签 (Tag) -> {"tag": "xxx"}
                    val obj = com.google.gson.JsonObject()
                    obj.addProperty("tag", cleanStr.substring(1))
                    gsonArray.add(obj)
                }
                else -> {
                    // 普通物品 (Item) -> {"item": "xxx"}
                    val obj = com.google.gson.JsonObject()
                    obj.addProperty("item", cleanStr)
                    gsonArray.add(obj)
                }
            }
        }

        // 3. 喂给原生 Codec 进行严谨反序列化
        return Ingredient.CODEC.parse(JsonOps.INSTANCE, gsonArray)
            .getOrThrow { java.lang.IllegalStateException("解析 Ingredient 失败: $it") }
    }

    /**
     * 辅助方法：从原生序列化结果中提取出干净的字符串
     */
    private fun extractStrings(element: com.google.gson.JsonElement): List<String> {
        val list = mutableListOf<String>()
        when {
            element.isJsonArray -> element.asJsonArray.forEach { list.addAll(extractStrings(it)) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                when {
                    obj.has("item") -> list.add(obj.get("item").asString)
                    obj.has("tag") -> list.add("#" + obj.get("tag").asString)
                    else -> list.add(element.toString()) // 遇到非常规自定义结构，保留原样
                }
            }
            element.isJsonPrimitive -> list.add(element.asString)
        }
        return list
    }
}