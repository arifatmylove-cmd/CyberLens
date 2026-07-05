package com.cyberlens.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberlens.app.data.remote.SauceNaoApiService
import com.cyberlens.app.domain.model.ImageMatch
import com.cyberlens.app.domain.model.ImageReverseResult
import com.cyberlens.app.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ImageReverseViewModel @Inject constructor(
    private val sauceNaoService: SauceNaoApiService,
    @ApplicationContext private val context: Context,
    @Named("saucenao_api_key") private val apiKey: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ImageReverseResult>>(UiState.Idle)
    val uiState: StateFlow<UiState<ImageReverseResult>> = _uiState

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput

    fun onUriSelected(uri: Uri) { _selectedUri.value = uri }
    fun onUrlChange(url: String) { _urlInput.value = url }

    fun searchByUrl() {
        val url = _urlInput.value.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = runCatching {
                val resp = sauceNaoService.searchByUrl(apiKey, url)
                val body = resp.body() ?: throw Exception("Empty response from SauceNAO")
                val matches = body.results?.mapNotNull { result ->
                    val sim = result.header?.similarity?.toDoubleOrNull() ?: return@mapNotNull null
                    val srcUrl = result.data?.ext_urls?.firstOrNull() ?: result.data?.source ?: return@mapNotNull null
                    ImageMatch(
                        similarity = sim,
                        sourceUrl = srcUrl,
                        thumbnail = result.header.thumbnail,
                        title = result.data?.title,
                        indexName = result.header.index_name,
                        authorName = result.data?.author_name
                    )
                }?.sortedByDescending { it.similarity } ?: emptyList()
                UiState.Success(
                    ImageReverseResult(
                        totalMatches = matches.size,
                        matches = matches,
                        apiLimitRemaining = body.header?.shortRemaining
                    )
                )
            }.getOrElse { UiState.Error(it.message ?: "Search failed") }
        }
    }

    fun searchByFile() {
        val uri = _selectedUri.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Cannot read image file")
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
                val resp = sauceNaoService.searchByFile(apiKey, file = part)
                val body = resp.body() ?: throw Exception("Empty response from SauceNAO")
                val matches = body.results?.mapNotNull { result ->
                    val sim = result.header?.similarity?.toDoubleOrNull() ?: return@mapNotNull null
                    val srcUrl = result.data?.ext_urls?.firstOrNull() ?: result.data?.source ?: return@mapNotNull null
                    ImageMatch(
                        similarity = sim,
                        sourceUrl = srcUrl,
                        thumbnail = result.header.thumbnail,
                        title = result.data?.title,
                        indexName = result.header.index_name,
                        authorName = result.data?.author_name
                    )
                }?.sortedByDescending { it.similarity } ?: emptyList()
                UiState.Success(
                    ImageReverseResult(
                        totalMatches = matches.size,
                        matches = matches,
                        apiLimitRemaining = body.header?.shortRemaining
                    )
                )
            }.getOrElse { UiState.Error(it.message ?: "Search failed") }
        }
    }

    fun reset() { _uiState.value = UiState.Idle }
}
