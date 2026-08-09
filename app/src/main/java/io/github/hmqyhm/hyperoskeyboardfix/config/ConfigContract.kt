package io.github.hmqyhm.hyperoskeyboardfix.config

import android.net.Uri
import io.github.hmqyhm.hyperoskeyboardfix.BuildConfig

object ConfigContract {
    val authority: String = BuildConfig.CONFIG_PROVIDER_AUTHORITY
    val baseUri: Uri = Uri.parse("content://$authority")
    val changedUri: Uri = baseUri.buildUpon().appendPath("changed").build()

    const val METHOD_GET_CONFIG = "get_config"
    const val METHOD_GET_VERSION = "get_version"
    const val METHOD_PING = "ping"
    const val METHOD_NOTIFY_EVENT = "notify_event"
    const val METHOD_PUBLISH_CONFIG = "publish_config"

    const val EVENT_APP_OPEN = "app_open"
    const val EVENT_AUTO_IME = "auto_ime"
    const val EVENT_MACRO = "macro"
    const val EVENT_TEST = "test"

    const val BUNDLE_AVAILABLE = "available"
    const val BUNDLE_AUTHORITY = "authority"
    const val BUNDLE_CONFIG_VERSION = ConfigKeys.CONFIG_VERSION
    const val BUNDLE_MASTER_ENABLED = ConfigKeys.MASTER_ENABLED
    const val BUNDLE_WHITELIST = ConfigKeys.REMOTE_APP_WHITELIST
}
