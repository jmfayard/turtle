package com.lordcodes.turtle

import org.zeroturnaround.exec.ProcessExecutor
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    // Proof of concept for executing a pipe
    val folder = File("/").canonicalFile.also { println("folder: $it") }
    val pipeline = Command("find", folder.path) pipe Command("grep", "/bin/")
    println("Executing $pipeline")
    println(pipeline.execute())

    val inifiniteCommand = Command("yes", "say yes indefinitely")
    val pipeline2 = inifiniteCommand `|` Command("head", "-5")
    println(
        """
        |
        |Executing $pipeline2
        |${pipeline2.execute()}
    """.trimMargin()
    )

}

/**
 * See https://github.com/zeroturnaround/zt-exec
 * See https://square.github.io/okio/3.x/okio/okio/okio/-buffer/index.html
 * TODO: return a Sequence<String> instead because the process might run forever
 */
fun Pipeline.execute(
    timeout: Duration? = null
): String {
    require(commands.size == 2) { "Only two commands are supported in this proof of concept" }
    val (first, second) = commands

    // not sure of okio is necessary,
    // I don't know how to have a buffer that act as both inputstream and outputstream
    val pipelineBuffer = okio.Buffer()

    val firstExecutor = ProcessExecutor()
        .command(first.arguments)
        .redirectOutput(pipelineBuffer.outputStream())

    val secondExecutor = ProcessExecutor()
        .command(second.arguments)
        .readOutput(true)
        .redirectInput(pipelineBuffer.inputStream())
    if (timeout != null) {
        secondExecutor.timeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    firstExecutor.start()
    var output: String
    val miliseconds = measureTimeMillis {
        output = secondExecutor.execute().outputUTF8()
    }
    println("w: pipeline executed in ${miliseconds.milliseconds}")
    return output
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
    val arguments: List<String>,
    internal val redirect: CommandRedirect? = null
) {
    val command = arguments.firstOrNull()
        ?: error("Command.command is empty")

    fun withRedirect(redirect: CommandRedirect): Command =
        copy(redirect = redirect)

    infix fun `|`(nextCommand: Command): Pipeline =
        Pipeline(listOf(this, nextCommand), redirect = null)

    infix fun pipe(nextCommand: Command): Pipeline =
        Pipeline(listOf(this, nextCommand), redirect = null)
}

fun Command(vararg args: String, redirect: CommandRedirect? = null): Command =
    Command(args.toList(), redirect)

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

