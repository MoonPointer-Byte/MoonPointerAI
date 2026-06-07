package com.englishai.translator.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.englishai.translator.R
import com.englishai.translator.WebAppViewModel
import com.englishai.translator.bridge.MoonPointerAndroidBridge
import com.englishai.translator.speech.AndroidTtsController
import com.englishai.translator.ui.theme.MoonPointerTheme
import com.englishai.translator.web.MoonPointerWebAssets

private const val WEB_LOG_TAG = "MoonPointerWeb"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(viewModel: WebAppViewModel) {
    MoonPointerTheme(darkTheme = viewModel.isDarkTheme) {
        WebViewScreenContent(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewScreenContent(viewModel: WebAppViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    var apiDraft by remember(viewModel.apiServerUrl) { mutableStateOf(viewModel.apiServerUrl) }
    var webDraft by remember(viewModel.webAppUrl) { mutableStateOf(viewModel.webAppUrl) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val assetLoader = remember { MoonPointerWebAssets.createAssetLoader(context.applicationContext) }
    val resolvedWebUrl = remember(viewModel.webAppUrl) {
        MoonPointerWebAssets.resolveLoadUrl(viewModel.webAppUrl)
    }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val ttsController = remember { AndroidTtsController(context.applicationContext) }

    DisposableEffect(ttsController) {
        onDispose { ttsController.shutdown() }
    }

    BackHandler(enabled = webViewRef.value?.canGoBack() == true) {
        webViewRef.value?.goBack()
    }

    val colors = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        key(viewModel.reloadToken, viewModel.webAppUrl) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowFileAccessFromFileURLs = true
                            allowUniversalAccessFromFileURLs = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString MoonPointerAndroid/1.0"
                        }
                        ttsController.onUtteranceDone = { utteranceId ->
                            mainHandler.post {
                                val safeId = utteranceId.replace("'", "\\'")
                                evaluateJavascript(
                                    "window.__moonTtsDone&&window.__moonTtsDone('$safeId')",
                                    null
                                )
                            }
                        }
                        addJavascriptInterface(
                            MoonPointerAndroidBridge(
                                apiServerUrl = { viewModel.apiServerUrl },
                                webAppUrl = { viewModel.webAppUrl },
                                themeMode = { viewModel.themeMode },
                                onThemeChange = { viewModel.setTheme(it) },
                                ttsController = ttsController
                            ),
                            "MoonPointerAndroid"
                        )
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request.url)
                                    ?: super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                mainHandler.post {
                                    isLoading = true
                                    loadError = null
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                mainHandler.post { isLoading = false }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    mainHandler.post {
                                        isLoading = false
                                        loadError = error?.description?.toString() ?: "无法加载页面"
                                    }
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.d(
                                    WEB_LOG_TAG,
                                    "${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                                )
                                return true
                            }
                        }
                        webViewRef.value = this
                        Log.i(WEB_LOG_TAG, "Loading $resolvedWebUrl")
                        loadUrl(resolvedWebUrl)
                    }
                },
                update = { webView ->
                    webViewRef.value = webView
                }
            )
        }

        if (isLoading && loadError == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF818CF8)
            )
        }

        if (loadError != null) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color(0xFF16161F), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text("无法加载 MoonPointer", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(loadError ?: "", color = Color(0xFFA1A1AA), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.web_load_hint),
                    color = Color(0xFF71717A),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    loadError = null
                    isLoading = true
                    viewModel.resetToDefaults()
                }) {
                    Text("使用内置界面")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        loadError = null
                        isLoading = true
                        viewModel.reload()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重试")
                }
            }
        }

        FloatingActionButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 68.dp),
            containerColor = colors.primary
        ) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
    }

    if (showSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("连接设置", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiDraft,
                    onValueChange = { apiDraft = it },
                    label = { Text(stringResource(R.string.api_server_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = webDraft,
                    onValueChange = { webDraft = it },
                    label = { Text(stringResource(R.string.web_app_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.web_setup_hint),
                    color = Color(0xFF71717A),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.resetToDefaults(); apiDraft = viewModel.apiServerUrl; webDraft = viewModel.webAppUrl },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复默认（内置 Web）")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.useDevWebServer(); apiDraft = viewModel.apiServerUrl; webDraft = viewModel.webAppUrl },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("使用开发服务器 :3000")
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.updateUrls(apiDraft, webDraft)
                        showSettings = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("  保存并刷新", modifier = Modifier.padding(start = 4.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
