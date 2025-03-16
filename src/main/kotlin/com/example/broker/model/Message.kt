package com.example.broker.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val message: String
)