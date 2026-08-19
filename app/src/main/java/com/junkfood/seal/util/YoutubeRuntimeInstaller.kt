package com.junkfood.seal.util

import android.content.Context
import java.io.File

object YoutubeRuntimeInstaller {
    private const val ASSET_ROOT = "ytdlp"
    private const val RUNTIME_DIR = "runtime"
    private const val EJS_DIR = "ejs"

    fun install(context: Context): RuntimeStatus {
        val ytdlpDir = File(context.filesDir, ASSET_ROOT).apply { mkdirs() }
        val runtimeDir = File(ytdlpDir, RUNTIME_DIR).apply { mkdirs() }
        val ejsDir = File(ytdlpDir, EJS_DIR).apply { mkdirs() }
        copyAssetDirectoryIfPresent(
            context = context,
            assetPath = "$ASSET_ROOT/$RUNTIME_DIR",
            targetDir = runtimeDir,
        )
        copyAssetDirectoryIfPresent(
            context = context,
            assetPath = "$ASSET_ROOT/$EJS_DIR",
            targetDir = ejsDir,
        )
        runtimeDir
            .walkTopDown()
            .filter { it.isBundledAsset() }
            .forEach { it.setExecutable(true, true) }
        return getStatus(context)
    }

    fun getStatus(context: Context): RuntimeStatus {
        val ytdlpDir = File(context.filesDir, ASSET_ROOT)
        val runtimeDir = File(ytdlpDir, RUNTIME_DIR)
        val ejsDir = File(ytdlpDir, EJS_DIR)
        val runtime =
            runtimeDir
                .walkTopDown()
                .firstOrNull { file -> file.isBundledAsset() && file.canExecute() }
        return RuntimeStatus(
            runtimePath = runtime?.absolutePath.orEmpty(),
            isRuntimeAvailable = runtime != null,
            isEjsAvailable = ejsDir.exists() && ejsDir.walkTopDown().any { it.isBundledAsset() },
        )
    }

    private fun File.isBundledAsset(): Boolean = isFile && name != ".gitkeep"

    private fun copyAssetDirectoryIfPresent(context: Context, assetPath: String, targetDir: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) return
        children.forEach { child ->
            val childAssetPath = "$assetPath/$child"
            val childTarget = File(targetDir, child)
            val grandChildren = context.assets.list(childAssetPath).orEmpty()
            if (grandChildren.isEmpty()) {
                childTarget.outputStream().use { output ->
                    context.assets.open(childAssetPath).use { input -> input.copyTo(output) }
                }
            } else {
                childTarget.mkdirs()
                copyAssetDirectoryIfPresent(context, childAssetPath, childTarget)
            }
        }
    }

    data class RuntimeStatus(
        val runtimePath: String,
        val isRuntimeAvailable: Boolean,
        val isEjsAvailable: Boolean,
    )
}
