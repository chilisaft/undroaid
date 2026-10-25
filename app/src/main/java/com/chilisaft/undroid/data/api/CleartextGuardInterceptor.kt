package com.chilisaft.undroid.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/** Refused to send the API key over plain HTTP to a server outside the local network. */
class InsecureConnectionException(host: String) : IOException("Refusing plain HTTP to non-local host $host")

/**
 * Plain HTTP is only allowed to servers on a private network - the API key (and, when starting an
 * encrypted array, its passphrase) travels in every request, readable by anyone on the path.
 *
 * Registered as a *network* interceptor so it runs after the connection is made but before the
 * request is written: it judges the IP address actually connected to, not the hostname. A name
 * like `unraid.example.com` that resolves to a LAN address is fine; one that resolves to a public
 * address isn't - however the URL was written.
 */
class CleartextGuardInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isHttps) {
            val address = chain.connection()?.socket()?.inetAddress
            if (address == null || !address.isPrivateNetworkAddress()) {
                throw InsecureConnectionException(request.url.host)
            }
        }
        return chain.proceed(request)
    }
}

/**
 * Loopback, RFC 1918 (10/8, 172.16/12, 192.168/16), link-local, CGNAT 100.64/10 (what Tailscale
 * assigns - its tunnel is already encrypted), and IPv6 unique-local fc00::/7.
 */
internal fun InetAddress.isPrivateNetworkAddress(): Boolean {
    if (isLoopbackAddress || isSiteLocalAddress || isLinkLocalAddress) return true
    val bytes = address
    return when (this) {
        is Inet4Address -> bytes[0].toInt() and 0xFF == 100 && (bytes[1].toInt() and 0xC0) == 0x40
        is Inet6Address -> (bytes[0].toInt() and 0xFE) == 0xFC
        else -> false
    }
}
