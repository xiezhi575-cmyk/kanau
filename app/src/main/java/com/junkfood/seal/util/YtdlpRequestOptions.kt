package com.junkfood.seal.util

import com.junkfood.seal.App
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getString
import com.yausername.youtubedl_android.YoutubeDLRequest

private const val YOUTUBE_EXTRACTOR_KEY = "youtube"

fun YoutubeDLRequest.applySharedYtdlpOptions(
    preferences: DownloadUtil.DownloadPreferences,
): YoutubeDLRequest =
    apply {
        with(preferences) {
            if (cookies) {
                addOption("--cookies", App.context.getCookiesFile().absolutePath)
                userAgentString.takeIf { it.isNotEmpty() }?.let {
                    addOption("--add-header", "User-Agent:$it")
                }
            }
            if (restrictFilenames) {
                addOption("--restrict-filenames")
            }
            if (proxy) {
                addOption("--proxy", proxyUrl)
            }
            if (forceIpv4) {
                addOption("-4")
            }
            if (youtubeEjs) {
                val runtimeStatus = YoutubeRuntimeInstaller.getStatus(App.context)
                check(runtimeStatus.isRuntimeAvailable) {
                    "YouTube EJS is enabled but no bundled JavaScript runtime is installed."
                }
                addOption("--js-runtimes", "$youtubeJsRuntime:${runtimeStatus.runtimePath}")
            }
            buildYoutubeExtractorArgs(preferences).takeIf { it.isNotEmpty() }?.let {
                addOption("--extractor-args", it)
            }
        }
    }

private fun buildYoutubeExtractorArgs(
    preferences: DownloadUtil.DownloadPreferences,
): String {
    val args = buildList {
        if (preferences.autoSubtitle && !preferences.autoTranslatedSubtitles) {
            add("skip=translated_subs")
        }
        preferences.youtubePlayerClient.takeIf { it.isNotBlank() }?.let {
            add("player_client=$it")
        }
    }
    return if (args.isEmpty()) "" else "$YOUTUBE_EXTRACTOR_KEY:${args.joinToString(";")}"
}

fun createYoutubeRuntimePreferences(): YoutubeRuntimePreferences =
    YoutubeRuntimePreferences(
        enableEjs = YOUTUBE_EJS.getBoolean(),
        playerClient = YOUTUBE_PLAYER_CLIENT.getString(),
        jsRuntime = YOUTUBE_JS_RUNTIME.getString(),
    )

data class YoutubeRuntimePreferences(
    val enableEjs: Boolean,
    val playerClient: String,
    val jsRuntime: String,
)
