package com.example.broker

import com.example.broker.model.Message
import io.ktor.utils.io.*
import io.ktor.utils.io.locks.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

class MessageBrokerBuffer(
    topic: String,
    val lock: ReentrantLock
) {
    var offset = 0L
    val file = File(topic)
    val raf = RandomAccessFile(file, "rw")
    val counter = AtomicInteger()

    @OptIn(InternalAPI::class)
    fun appendToTopicFile(payload: Message) {
        lock.withLock {
            raf.seek(raf.length())
            raf.writeUTF(Json.encodeToString(payload))
            raf.channel.force(true)
            val queueSize = counter.incrementAndGet()
            println("Queue Size $queueSize")
        }
    }


    @OptIn(InternalAPI::class)
    fun pollMessage(): Message? {
        lock.withLock {
        raf.seek(offset)
        var message: Message? = null
        try {
            val messageStr = raf.readUTF()
            val queueSize = counter.decrementAndGet()
            offset = raf.filePointer
            println("Queue Size $queueSize")
            message = Json.decodeFromString<Message>(messageStr)
        } catch (_: Exception) {

        }
        println("Polled $message")
        return message
        }
    }

}