package com.cyberlens.app.domain.model

data class ImageMatch(
    val similarity: Double,
    val sourceUrl: String,
    val thumbnail: String?,
    val title: String?,
    val indexName: String?,
    val authorName: String?
)

data class ImageReverseResult(
    val totalMatches: Int,
    val matches: List<ImageMatch>,
    val searchEngine: String = "SauceNAO",
    val apiLimitRemaining: Int? = null
)
