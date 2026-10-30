package org.dx340.remote.host

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowInsetsController
import android.widget.TextView

/** Keep required instructions readable using the incumbent platform theme roles. */
internal fun TextView.useCompanionPrimaryText() {
    val values = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
    try { values.getColorStateList(0)?.let { setTextColor(it) } } finally { values.recycle() }
}

internal fun Activity.matchCompanionStatusBar() {
    window.decorView.post {
        val light = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        window.insetsController?.setSystemBarsAppearance(if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
    }
}
