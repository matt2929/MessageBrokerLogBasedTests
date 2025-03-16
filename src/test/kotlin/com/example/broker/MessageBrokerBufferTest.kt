package com.example.broker

import com.example.broker.model.Message
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.StringGen
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.jupiter.api.Assertions
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.Test
import kotlin.test.assertEquals

@Param(name = "id", gen = StringGen::class)
@Param(name = "message", gen = StringGen::class)
class MessageBrokerBufferTest {

    fun testMultiThreadedWrite() {
        val tempFile: java.nio.file.Path? = Files.createTempFile("tempFile-", ".txt")
        println("Temporary file created at: ${tempFile!!.toAbsolutePath()}")
        val executorService = Executors.newFixedThreadPool(4)
        val testSize = 500
        val latch = CountDownLatch(testSize)
        var expectedOutput: MutableList<String> = mutableListOf()
        val lock = ReentrantLock()
        for (i in 1..testSize) {
            executorService.submit {
                val mbb = MessageBrokerBuffer(tempFile!!.toAbsolutePath().toString(), lock)
                val messageToSend = "a $i\n"
                mbb.appendToTopicFile(
                    Message(
                        message = messageToSend,
                        id = "id$i"
                    )
                )
                expectedOutput.add(messageToSend)
                latch.countDown()
            }
        }
        executorService.shutdown()
        latch.await(100, TimeUnit.SECONDS)
        println("Test completed.")
        for (d in 1..3) {
            val actualOutput: MutableList<Message> = mutableListOf()
            val mbb = MessageBrokerBuffer(tempFile!!.toAbsolutePath().toString(), lock)
            for (i in 1..testSize) {
                val offset = mbb.pollMessage()
                actualOutput.add(offset!!)
            }
            actualOutput.sortedWith(compareBy {
                val tokenized = it.message.split(" ")
                val num = tokenized[tokenized.size - 1].replace("\\s".toRegex(), "")
                Integer.parseInt(num)
            })
            for (i in 0..testSize - 1) {
                Assertions.assertEquals(expectedOutput[i], actualOutput[i].message)
            }
        }
    }

    fun testWriteReadWriteRead() {
        val tempFile: java.nio.file.Path? = Files.createTempFile("tempFile-", ".txt")
        val lock = ReentrantLock()
        val mbb = MessageBrokerBuffer(tempFile!!.toAbsolutePath().toString(), lock)
        mbb.appendToTopicFile(Message(id = "id", message = "foo"))
        assertEquals("foo", mbb.pollMessage()!!.message)
        mbb.appendToTopicFile(Message(id = "id", message = "baz"))
        assertEquals("baz", mbb.pollMessage()!!.message)
    }

    private val tempFile: java.nio.file.Path? = Files.createTempFile("tempFile-", ".txt")
    val lock = ReentrantLock()
    val mbb = MessageBrokerBuffer(tempFile!!.toAbsolutePath().toString(), lock)

    // Operations on the Counter
    @Operation
    fun poll() = mbb.pollMessage()

    @Operation
    fun sendMessage(@Param(name = "message") message: String, @Param(name = "id") id: String) =
        mbb.appendToTopicFile(Message(id, message))

    @Test
    fun stressTest(): Unit = StressOptions() // Stress testing options:
        .actorsBefore(2) // Number of operations before the parallel part
        .threads(3) // Number of threads in the parallel part
        .actorsPerThread(2) // Number of operations in each thread of the parallel part
        .actorsAfter(1) // Number of operations after the parallel part
        .iterations(10) // Generate 100 random concurrent scenarios
        .invocationsPerIteration(10) // Run each generated scenario 1000 times
        .check(this::class) // Run the test
}

