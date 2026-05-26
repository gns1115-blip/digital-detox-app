package com.example.instagramdetector.detection

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility node tree / event 기반으로 Reels·Shorts 화면 여부를 판별합니다.
 */
object ShortsReelsDetector {

    fun isShortsOrReelsScreen(
        packageName: String,
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
    ): Boolean {
        return when {
            MonitoredApps.isInstagram(packageName) -> isInstagramReelsScreen(event, root)
            MonitoredApps.isYouTube(packageName) -> isYouTubeShortsScreen(event, root)
            else -> false
        }
    }

    private fun isInstagramReelsScreen(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
    ): Boolean {
        if (isReelsClassName(event.className?.toString())) return true
        if (root == null) return false
        return containsIndicator(
            root = root,
            keywords = listOf("reels", "reel", "릴스"),
            viewIdHints = listOf(
                "clips_viewer",
                "reel_viewer",
                "reels_viewer",
                "clips_tab",
                "reel_viewer_fragment",
                "reels_viewer_fragment",
            ),
            viewIdMustContainAny = listOf("tab", "viewer", "clips", "reel"),
        )
    }

    private fun isYouTubeShortsScreen(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
    ): Boolean {
        if (isShortsClassName(event.className?.toString())) return true
        if (root == null) return false
        return containsIndicator(
            root = root,
            keywords = listOf("shorts", "short", "쇼츠"),
            viewIdHints = listOf(
                "shorts",
                "reel_player",
                "shorts_player",
                "shorts_video",
                "reel_watch_fragment",
                "shorts_fragment",
            ),
            viewIdMustContainAny = listOf("short", "reel", "shorts"),
        )
    }

    private fun isReelsClassName(className: String?): Boolean {
        if (className.isNullOrBlank()) return false
        val normalized = className.lowercase()
        return normalized.contains("reel") &&
            (normalized.contains("instagram") || normalized.contains("clips"))
    }

    private fun isShortsClassName(className: String?): Boolean {
        if (className.isNullOrBlank()) return false
        val normalized = className.lowercase()
        return normalized.contains("short") && normalized.contains("youtube")
    }

    private fun containsIndicator(
        root: AccessibilityNodeInfo,
        keywords: List<String>,
        viewIdHints: List<String>,
        viewIdMustContainAny: List<String>,
        depth: Int = 0,
    ): Boolean {
        if (depth > 14) return false

        val viewId = root.viewIdResourceName?.lowercase().orEmpty()
        if (viewIdHints.any { hint -> viewId.contains(hint) }) return true

        val combinedText = buildString {
            append(root.text?.toString().orEmpty())
            append(' ')
            append(root.contentDescription?.toString().orEmpty())
        }.lowercase()

        if (combinedText.isNotBlank() && keywords.any { combinedText.contains(it) }) {
            if (viewIdMustContainAny.any { viewId.contains(it) }) return true
        }

        for (index in 0 until root.childCount) {
            val child = root.getChild(index) ?: continue
            try {
                if (containsIndicator(child, keywords, viewIdHints, viewIdMustContainAny, depth + 1)) {
                    return true
                }
            } finally {
                child.recycle()
            }
        }
        return false
    }
}
