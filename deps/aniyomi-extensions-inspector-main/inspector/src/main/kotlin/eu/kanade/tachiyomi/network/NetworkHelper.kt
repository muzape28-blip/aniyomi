package eu.kanade.tachiyomi.network

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Suppress("UNUSED_PARAMETER")
class NetworkHelper(context: Context) {

    val cookieManager = MemoryCookieJar()

    val client by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .build()
    }

    /*
     * @deprecated Since extension-lib 15
     */
    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    val cloudflareClient: OkHttpClient = client

    private val defaultUserAgent by lazy {
        System.getProperty("http.agent").orEmpty()
    }

    fun defaultUserAgentProvider() = defaultUserAgent
}
