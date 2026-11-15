package com.hisaab.diary.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Our privacy policy page. Opened in the phone's browser; the app itself never uses the internet. */
const val PRIVACY_POLICY_URL = "https://sites.google.com/view/hisaabdiary/home"

/** Opens a web page in another app (browser). Returns false when no app can open it. */
fun openWebPage(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (ignored: ActivityNotFoundException) {
    false
}

/** Opens this app's Play Store page, or the web page when the Play Store app is missing. */
fun openPlayStore(context: Context): Boolean {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
    return try {
        context.startActivity(market)
        true
    } catch (ignored: ActivityNotFoundException) {
        openWebPage(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
    }
}
