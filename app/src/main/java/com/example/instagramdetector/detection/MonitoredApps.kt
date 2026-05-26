package com.example.instagramdetector.detection

object MonitoredApps {
    const val INSTAGRAM = "com.instagram.android"
    const val INSTAGRAM_LITE = "com.instagram.lite"
    const val YOUTUBE = "com.google.android.youtube"

    val instagramPackages = setOf(INSTAGRAM, INSTAGRAM_LITE)

    val allPackages = setOf(INSTAGRAM, INSTAGRAM_LITE, YOUTUBE)

    /** 사용 시간 만료 후 재실행 차단 대상 */
    val relaunchBlockTargets = allPackages

    fun isInstagram(packageName: String): Boolean = packageName in instagramPackages

    fun isYouTube(packageName: String): Boolean = packageName == YOUTUBE

    fun isMonitored(packageName: String): Boolean = packageName in allPackages

    fun accessibilityPackageNames(): String = allPackages.joinToString(":")
}
