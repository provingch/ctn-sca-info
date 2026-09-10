package py.edu.ctn.sca.padres.core

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Minimal persistent [CookieJar]. The backend delivers the refresh token as an
 * httpOnly cookie (`SCA_SESSION` / `SCA_REMEMBER`); a native client is free to
 * store and replay it — httpOnly only fences off JavaScript. Cookies are kept in
 * the encrypted [TokenStore] so they survive process death.
 */
class PersistentCookieJar(private val store: TokenStore) : CookieJar {

    private val cache = linkedMapOf<String, Cookie>()

    init {
        store.cookies.forEach { serialized ->
            runCatching { deserialize(serialized) }.getOrNull()?.let { cache[it.name] = it }
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        var changed = false
        for (cookie in cookies) {
            if (cookie.expiresAt < System.currentTimeMillis()) {
                changed = cache.remove(cookie.name) != null || changed
            } else {
                cache[cookie.name] = cookie
                changed = true
            }
        }
        if (changed) persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val expired = cache.values.filter { it.expiresAt < now }
        if (expired.isNotEmpty()) {
            expired.forEach { cache.remove(it.name) }
            persist()
        }
        return cache.values.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        store.cookies = emptySet()
    }

    private fun persist() {
        store.cookies = cache.values.map { serialize(it) }.toSet()
    }

    // Cookies cannot contain newlines (RFC 6265), so '\n' is a safe field separator.
    private fun serialize(c: Cookie): String = listOf(
        c.name, c.value, c.expiresAt.toString(), c.domain, c.path,
        c.secure.toString(), c.httpOnly.toString(), c.hostOnly.toString(),
    ).joinToString("\n")

    private fun deserialize(s: String): Cookie {
        val p = s.split("\n")
        val builder = Cookie.Builder()
            .name(p[0])
            .value(p[1])
            .expiresAt(p[2].toLong())
            .path(p[4])
        if (p[7].toBoolean()) builder.hostOnlyDomain(p[3]) else builder.domain(p[3])
        if (p[5].toBoolean()) builder.secure()
        if (p[6].toBoolean()) builder.httpOnly()
        return builder.build()
    }
}
