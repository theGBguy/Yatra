package com.example.rider.model

data class YatraRequest(
    val requestId: String? = "",
    val initiatorId: String? = "",
    val acceptorId: String? = "",
    val arrivalLocation: String? = "",
    val arrivalDate: String? = "",
    val arrivalTime: String? = "",
    val departureLocation: String? = "",
    val departureDate: String? = "",
    val departureTime: String? = "",
    val peopleCount: Int? = -1,
    val weight: Int? = -1,
    val name: String? = "",
    val msg: String? = "",
    val isAccepted: Boolean? = false
)