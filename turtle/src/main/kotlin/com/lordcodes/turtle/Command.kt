package com.lordcodes.turtle

import java.io.File

fun main() {
    // Functional core
    val pipeline = measureKotlinFun()
    println(pipeline)
    /*
Pipeline(
    Command(command=find, args=[., -type, f], redirect=null),
    Command(command=grep, args=[.kt], redirect=null),
    Command(command=xargs, args=[grep, fun ], redirect=CommandRedirect(stdin=null, stdout=null, stderr=null, stdoutIgnore=false, stderrIgnore=true, stderrToStdout=false)),
    Command(command=wc, args=[-l], redirect=null),
    redirect = null
)
     */

    // imperative shell
    val output = File("fun.txt")
    pipeline.withRedirect(CommandRedirect(stdout = output))
        .executeCommand()
}

/**
 * Pure function that represents but doesn't execute this command:
 *  $ find . -type f | grep '.kt' | xargs grep 'fun ' 2> /dev/null | wc -l
 */
fun measureKotlinFun(): Pipeline {
    val findFiles = Command("find", ".", "-type", "f")
    val onlyKotlinFiles = Command("grep", ".kt")
    val foreachSearchFun = Command("xargs", "grep", "fun ")
        .withRedirect(CommandRedirect(stderrIgnore = true))
    val countLines = Command("wc", "-l")
    val pipeline = findFiles `|` onlyKotlinFiles `|` foreachSearchFun `|` countLines
    return pipeline
}

data class Command(
    val command: String,
    val args: List<String>,
    internal val redirect: CommandRedirect? = null
) {
    fun withRedirect(redirect: CommandRedirect): Command =
        copy(redirect = redirect)

    infix fun `|`(nextCommand: Command): Pipeline =
        Pipeline(listOf(this, nextCommand), redirect = null)

    infix fun pipe(nextCommand: Command): Pipeline =
        Pipeline(listOf(this, nextCommand), redirect = null)
}

fun Command(command: String, vararg args: String): Command =
    Command(command, args.toList(), redirect = null)

data class Pipeline(
    val commands: List<Command>,
    internal val redirect: CommandRedirect? = null
) {
    infix fun `|`(command: Command): Pipeline =
        copy(commands = commands + command)

    infix fun pipe(command: Command): Pipeline =
        copy(commands = commands + command)

    fun withRedirect(redirect: CommandRedirect): Pipeline =
        copy(redirect = redirect)

    fun executeCommand() {
        TODO("Not yet implemented")
    }

    override fun toString() = """
Pipeline(
    ${commands.joinToString(",\n    ")},
    redirect = $redirect
)
    """.trim()
}

data class CommandRedirect(
    val stdin: File? = null,
    val stdout: File? = null,
    val stderr: File? = null,
    val stdoutIgnore: Boolean = false,
    val stderrIgnore: Boolean = false,
    val stderrToStdout: Boolean = false,
)

