package io.github.yqy7.transcli

import dev.langchain4j.data.message.SystemMessage.systemMessage
import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.kotlin.model.chat.chat
import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.ValueLayout
import kotlin.io.path.absolutePathString

@Command(
    name = "transcli", mixinStandardHelpOptions = true,
    version = ["transcli v1.0"], description = ["使用 AI 翻译文本"]
)
class TransCliCommand : Runnable {
    @Option(names = ["-c", "--config"], description = ["编辑配置文件"])
    var editConfig: Boolean = false

    @Parameters(description = ["要翻译的文本"])
    var texts = listOf<String>()

    override fun run() = runBlocking {
        if (editConfig) {
            editConfig()
            return@runBlocking
        }

        if (appConfig.llm.isEmpty()) {
            val configFilePath = configFilePath().absolutePathString()
            throw ParameterException(CommandLine(this@TransCliCommand), """
                请编辑配置文件 $configFilePath，添加 LLM 配置，示例如下：
                "llm": [
                    {
                        "baseUrl": "https://api.deepseek.com",
                        "apiKey": "你的API_KEY",
                        "model": "deepseek-v4-flash"
                    }
                ]
            """.trimIndent())
        }

        val inputText = if (texts.isNotEmpty()) {
            texts.joinToString("\n")
        } else {
            generateSequence { readlnOrNull() }.joinToString()
        }

        if (inputText.isBlank()) {
            throw ParameterException(CommandLine(this@TransCliCommand), "输入文本为空")
        }

        val result = translate(inputText)
        if (isStdoutTTY() && appConfig.usePager) {
            usePager(result)
        } else {
            println(result)
        }
    }
}

fun editConfig() {
    val editor = System.getenv("EDITOR")
    if (editor != null) {
        val pb = ProcessBuilder(editor, configFilePath().absolutePathString())
        pb.inheritIO()
        val process = pb.start()
        process.waitFor()
    } else {
        println("请手动编辑配置文件 ${configFilePath().absolutePathString()}")
    }
}

fun usePager(result: String) {
    val pager = System.getenv("PAGER")
    if (pager != null) {
        val process = ProcessBuilder(pager)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.outputStream.write(result.toByteArray())
        process.outputStream.close()
        process.waitFor()
    }
}

fun isStdoutTTY(): Boolean {
    val linker = Linker.nativeLinker()
    val stdLib = linker.defaultLookup()

    val isattyAddr = stdLib.find("isatty").orElseThrow()
    val isattyDesc = FunctionDescriptor.of(
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_INT
    )

    val isatty = linker.downcallHandle(isattyAddr, isattyDesc)

    val result = isatty.invoke(1) as Int  // 1 = STDOUT_FILENO
    return result != 0
}

suspend fun translate(inputText: String): String {
    val appConfig = loadConfig()
    val llmConfig = appConfig.llm.first()
    val model = OpenAiChatModel.builder()
        .apiKey(llmConfig.apiKey)
        .baseUrl(llmConfig.baseUrl)
        .modelName(llmConfig.model)
        .build()

    val result = model.chat {
        messages += systemMessage(appConfig.systemPrompt)
        messages += userMessage(inputText)
    }
    return result.aiMessage().text()
}