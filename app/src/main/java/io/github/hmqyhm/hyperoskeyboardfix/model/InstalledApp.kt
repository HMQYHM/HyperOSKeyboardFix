package io.github.hmqyhm.hyperoskeyboardfix.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean,
)
