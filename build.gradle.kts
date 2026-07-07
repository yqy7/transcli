plugins {
    application
    kotlin("jvm") version "2.3.21"
    kotlin("kapt") version "2.3.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
    id("org.graalvm.buildtools.native") version "1.1.3"

}

group = "io.github.yqy7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("info.picocli:picocli:4.7.7")
    kapt("info.picocli:picocli-codegen:4.7.7")
    // AI
    val langchain4jVersion = "1.16.3"
    implementation("dev.langchain4j:langchain4j:${langchain4jVersion}")
    implementation("dev.langchain4j:langchain4j-open-ai:${langchain4jVersion}")
    implementation("dev.langchain4j:langchain4j-kotlin:1.16.3-beta26")
    // 日志
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.5.37")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

val appMainClass = "io.github.yqy7.transcli.MainKt"
application {
    mainClass.set(appMainClass)    // 替换为你的实际主类

    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
    )
}

tasks.named<JavaExec>("run") {
    // 继承标准输入输出。虽然 System.console()  still 返回null，但是够覆盖需要需要反射的类了
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}

// 这个任务可以直接生成需要的 reachability-metadata.json
// ./gradlew runTracingAgent
tasks.register<JavaExec>("runTracingAgent") {
    mainClass.set(appMainClass)
    classpath = sourceSets.main.get().runtimeClasspath

    val metadataFile = projectDir.resolve("src/main/resources/META-INF/native-image/io.github.yqy7.transcli").absolutePath
    jvmArgs = listOf("-agentlib:native-image-agent=config-output-dir=${metadataFile}")

    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}

graalvmNative {
    binaries {
        // 用于生成 reachability-metadata.json
        agent {
            // 1.需要执行 ./gradlew -Pagent run 生成的文件在 ${buildDir}/native/agent-output/${taskName} 目录中
            enabled.set(true)
            defaultMode = "standard"

            // 2.需要执行 ./gradlew metadataCopy
            metadataCopy {
                inputTaskNames.add("run") // 指定之前使用代理执行的任务
                outputDirectories.add("src/main/resources/META-INF/native-image/io.github.yqy7.transcli")
                mergeWithExisting.set(false)
            }
        }

        named("main") {
            mainClass.set(appMainClass)
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
        }
    }
}