package com.cyberlens.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SauceNaoResponseDto(
    val header: SauceNaoHeaderDto?,
    val results: List<SauceNaoResultDto>?
)

data class SauceNaoHeaderDto(
    val status: Int?,
    @SerializedName("results_returned") val resultsReturned: Int?,
    @SerializedName("long_remaining") val longRemaining: Int?,
    @SerializedName("short_remaining") val shortRemaining: Int?
)

data class SauceNaoResultDto(
    val header: SauceNaoResultHeaderDto?,
    val data: SauceNaoResultDataDto?
)

data class SauceNaoResultHeaderDto(
    val similarity: String?,
    val thumbnail: String?,
    val index_name: String?,
    val index_id: Int?
)

data class SauceNaoResultDataDto(
    val ext_urls: List<String>?,
    val title: String?,
    val author_name: String?,
    val source: String?
)
