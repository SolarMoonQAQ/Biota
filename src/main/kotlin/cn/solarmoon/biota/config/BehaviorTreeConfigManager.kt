package cn.solarmoon.biota.fp.serialization

import cn.solarmoon.kbehaviortree.node.TreeNode
import cn.solarmoon.biota.Biota
import cn.solarmoon.biota.fp.entity.peafowlBehaviorTree
import net.neoforged.fml.loading.FMLPaths
import java.io.File

object BehaviorTreeConfigManager {
    // 💡 1. 准确定位到：游戏根目录/config/behavior_trees 文件夹
    private val configFolder: File = FMLPaths.CONFIGDIR.get().resolve(Biota.MOD_ID).resolve("behavior-trees").toFile()

    // 💡 2. 运行时行为树注册表：通过文件名（不含后缀）映射到内存中的行为树对象
    private val treeRegistry = mutableMapOf<String, TreeNode>()

    /**
     * 外部调用的初始化入口
     */
    fun init() {
        if (!configFolder.exists()) {
            configFolder.mkdirs()
        }

        // 检查该目录下是否有任何 yaml 配置文件
        val yamlFiles = configFolder.listFiles { _, name -> name.endsWith(".yaml") || name.endsWith(".yml") }
        if (yamlFiles.isNullOrEmpty()) {
            println("[BehaviorTree] 检测到行为树配置为空，开始生成默认模板...")
            generateDefaultConfigs()
        }

        // 执行全局载入
        reloadAll()
    }

    /**
     * 3. 自动生成默认配置文件（当文件夹为空时触发，作为范本供玩家/你参考修改）
     */
    private fun generateDefaultConfigs() {
        // 利用你写好的 DSL 快速拼装出一颗默认的孔雀行为树对象
        val defaultPeafowlTree = peafowlBehaviorTree

        // 保存为 "peafowl.yaml"
        saveTree("peafowl", defaultPeafowlTree)
    }

    /**
     * 将树结构序列化并保存为 YAML 文件
     */
    fun saveTree(name: String, tree: TreeNode) {
        val file = File(configFolder, "$name.yaml")
        try {
            // 使用前面定制过的 YAML 实例直接导出
            val yamlText = YAML.encodeToString(TreeNode.serializer(), tree)
            file.writeText(yamlText)
            println("[BehaviorTree] 已生成默认配置文件: ${file.absolutePath}")
        } catch (e: Exception) {
            System.err.println("[BehaviorTree] 序列化/保存 $name.yaml 失败！")
            e.printStackTrace()
        }
    }

    /**
     * 4. 核心：扫描并读取文件夹下所有的 YAML 配置文件（支持游戏内热重载）
     */
    fun reloadAll() {
        treeRegistry.clear()
        val files = configFolder.listFiles { _, name -> name.endsWith(".yaml") || name.endsWith(".yml") } ?: return

        for (file in files) {
            val treeName = file.nameWithoutExtension // 获取不带后缀的文件名，作为 Key
            try {
                val content = file.readText()

                // 💡 解码：动态识别你在 YAML 里填写的 type: panic_fly
                val tree = YAML.decodeFromString(TreeNode.serializer(), content)

                treeRegistry[treeName] = tree
                println("[BehaviorTree] 成功载入行为树配置: $treeName")
            } catch (e: Exception) {
                System.err.println("[BehaviorTree] 配置文件格式有误，载入失败: ${file.name}")
                e.printStackTrace() // 打印报错日志，方便在控制台排查是哪个节点的参数填错了
            }
        }
    }

    /**
     * 5. 提供给 Entity AI 调用的获取接口
     */
    fun getTree(name: String): TreeNode? {
        return treeRegistry[name]
    }
}