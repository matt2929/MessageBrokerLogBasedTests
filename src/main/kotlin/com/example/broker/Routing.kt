package com.example.broker


import com.example.broker.model.Message
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.locks.ReentrantLock

fun Application.configureRouting() {
    routing {
        staticResources("static", "static")

        route("/messages/{group}/{id}") {
            val lock = ReentrantLock()
            get {
                val group = call.parameters["group"]!!
                val id = call.parameters["id"]!!
                val mbb = MessageBrokerBuffer(group, lock)
                call.respond(mbb.pollMessage()?:"")
            }
            post {
                try {
                    val group = call.parameters["group"]!!
                    val id = call.parameters["id"]!!
                    val mbb = MessageBrokerBuffer(group, lock)
                    val message = call.receive<Message>()
                    mbb.appendToTopicFile(message)
                    call.respond(HttpStatusCode.Created)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}