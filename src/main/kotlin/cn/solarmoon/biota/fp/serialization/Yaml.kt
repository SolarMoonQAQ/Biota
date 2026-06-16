package cn.solarmoon.biota.fp.serialization

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

private val yamlConfig = YamlConfiguration(
    polymorphismStyle = PolymorphismStyle.Property
)

val YAML = Yaml(serializeModule, yamlConfig)

