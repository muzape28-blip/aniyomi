@file:Suppress("UNUSED_PARAMETER")
package android.webkit

import android.content.Context

@Suppress("EmptyFunctionBlock", "UnusedPrivateProperty")
class WebView(private val context: Context) {
    fun clearCache(includeDiskFiles: Boolean) {}

    fun clearFormData() {}
    fun clearHistory() {}

    fun stopLoading() {}

    fun destroy() {}

    fun loadUrl(url: String, additionalHeaders: Map<String, String>) {}

    fun loadUrl(url: String) {}

    fun loadData(data: String, mimeType: String?, encoding: String?) {}

    fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?,
    ) {}

    fun addJavascriptInterface(obj: Any, name: String) {}

    fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {}
}
