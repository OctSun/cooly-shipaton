package com.octsun.cooly.platform

import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun currentLanguageCode(): String =
    NSLocale.currentLocale().languageCode ?: "en"
