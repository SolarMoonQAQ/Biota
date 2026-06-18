package cn.solarmoon.biota.config

import cn.solarmoon.kbehaviortree.node.TreeNode
import cn.solarmoon.biota.Biota
import cn.solarmoon.biota.fp.entity.peafowlBehaviorTree
import cn.solarmoon.biota.fp.entity.waterBuffaloBehaviorTree
import cn.solarmoon.biota.fp.serialization.YAML
import cn.solarmoon.biota.registry.common.BiotaEntities
import net.neoforged.fml.loading.FMLPaths
import java.io.File

object BehaviorTreeConfigManager {

    private val configFolder: File = FMLPaths.CONFIGDIR.get().resolve(Biota.MOD_ID).resolve("behavior-trees").toFile()
    private val treeRegistry = mutableMapOf<String, TreeNode>()

    /**
     * 1. 静态映射：把你预期的【树名称】和【代码里的默认树对象】绑定在一起
     */
    private val defaultTrees: Map<String, TreeNode> by lazy {
        mapOf(
            BiotaEntities.PEAFOWL.id.path to peafowlBehaviorTree,
            BiotaEntities.WATER_BUFFALO.id.path to waterBuffaloBehaviorTree
        )
    }

    fun init() {
        if (!configFolder.exists()) {
            configFolder.mkdirs()
        }

        // 执行全局载入
        reloadAll()
    }

    /**
     * 3. 核心修正：谁丢了就单独补谁，实现“非破坏性”的默认生成
     */
    private fun checkAndGenerateDefaultConfigs() {
        defaultTrees.forEach { (treeName, defaultTree) ->
            val targetFile = File(configFolder, "$treeName.yaml")

            // 如果这个特定的配置文件不存在，则为它生成代码里的默认模板
            if (!targetFile.exists()) {
                println("[BehaviorTree] 未检测到 [$treeName] 的配置文件，正在恢复默认模板...")
                saveTree(treeName, defaultTree)
            }
        }
    }

    /**
     * 将树结构序列化并保存为 YAML 文件
     */
    fun saveTree(name: String, tree: TreeNode) {
        val file = File(configFolder, "$name.yaml")
        try {
            val yamlText = YAML.encodeToString(TreeNode.serializer(), tree)
            file.writeText(yamlText)
            println("[BehaviorTree] 已生成/更新配置文件: ${file.absolutePath}")
        } catch (e: Exception) {
            System.err.println("[BehaviorTree] 序列化/保存 $name.yaml 失败！")
            e.printStackTrace()
        }
    }

    /**
     * 扫描并读取文件夹下所有的 YAML 配置文件
     */
    fun reloadAll() {
        checkAndGenerateDefaultConfigs()
        treeRegistry.clear()
        val files = configFolder.listFiles { _, name -> name.endsWith(".yaml") || name.endsWith(".yml") } ?: return

        for (file in files) {
            val treeName = file.nameWithoutExtension
            try {
                val content = file.readText()
                val tree = YAML.decodeFromString(TreeNode.serializer(), content)
                treeRegistry[treeName] = tree
                println("[BehaviorTree] 成功载入行为树配置: $treeName")
            } catch (e: Exception) {
                System.err.println("[BehaviorTree] 配置文件格式有误，载入失败: ${file.name}")
                e.printStackTrace()
            }
        }
    }

    fun getTree(name: String): TreeNode? {
        return treeRegistry[name]
    }
}