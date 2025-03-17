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

    @OptIn(InternalAPI::class)
    fun appendToTopicFile(payload: Message) {
        lock.withLock {
            val jsonPayload = Json.encodeToString(payload)
            println("saving payload json $jsonPayload")
            raf.seek(raf.length())
            raf.writeUTF(jsonPayload)
            raf.channel.force(true)
        }
    }


    @OptIn(InternalAPI::class)
    fun pollMessage(): Message? {
        lock.withLock {
        raf.seek(offset)
        var message: Message? = null
        try {
            message = Json.decodeFromString<Message>(raf.readUTF())
            offset = raf.filePointer
        } catch (e: Exception) {
            println("Failure to poll message $e")
        }
        println("Polled $message")
        return message
        }
    }

}