package com.neoplay.tv.models

import java.io.Serializable

data class Channel @JvmOverloads constructor(
    val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val categoryName: String,
    var tvgId: String = ""
) : Serializable
