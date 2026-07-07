package io.github.yqy7.transcli

import picocli.CommandLine

fun main(args: Array<String>) {
    initApp()

    CommandLine(TransCliCommand::class.java).execute(*args)
}
