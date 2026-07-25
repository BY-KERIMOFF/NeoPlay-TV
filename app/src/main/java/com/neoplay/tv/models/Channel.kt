package com.neoplay.tv.models

import java.io.Serializable

data class Channel @JvmOverloads constructor(
    val id: String,
    val name: String,
    var logoUrl: String,
    val streamUrl: String,
    val categoryName: String,
    var tvgId: String = "",
    var catchupType: String = "", // "append", "shift", "default"
    var catchupDays: Int = 0,
    var catchupSource: String = ""
) : Serializable
