package ru.netology.diplom.model

import com.google.gson.annotations.SerializedName

data class AuthModel (
    @SerializedName("id")
    val userId: Long?,
    @SerializedName("token")
    val token: String?
)