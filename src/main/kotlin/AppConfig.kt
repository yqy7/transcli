package io.github.yqy7.transcli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

@Serializable
data class AppConfig(
    val systemPrompt: String = "",
    val usePager: Boolean = true,
    val llm: List<LLMConfig> = listOf()
)

@Serializable
data class LLMConfig(
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val type: String = "",
    val model: String = "",
    val models: List<String> = emptyList(),
)

fun configDirPath(): Path {
    val homePath = Path(System.getProperty("user.home"))
    val configDirPath = homePath.resolve(".config").resolve("transcli")
    if (Files.notExists(configDirPath)) {
        Files.createDirectories(configDirPath)
    }
    return configDirPath
}

fun configFilePath(): Path {
    val configFilePath =  configDirPath().resolve("transcli.json")
    if (Files.notExists(configFilePath)) {
        Files.writeString(configFilePath, """
            {
              "systemPrompt": "你是一个翻译器，把其他语言的文本翻译成中文，如果是中文就翻译成英文，尽量文本维持原来的格式，不要输出多余信息",
              "llm": []
            }
        """.trimIndent())
    }
    return configFilePath
}

fun logFilePath(): Path {
    return configDirPath().resolve("app.log")
}

 fun loadConfig(): AppConfig {
    val jsonStr = Files.readString(configFilePath())
    return Json.decodeFromString<AppConfig>(jsonStr)
}

lateinit var appConfig: AppConfig

fun initApp() {
    System.setProperty("log.file", logFilePath().absolutePathString())

    appConfig = loadConfig()
}