package com.octsun.cooly.platform

import java.util.Locale

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun currentLanguageCode(): String = Locale.getDefault().language
