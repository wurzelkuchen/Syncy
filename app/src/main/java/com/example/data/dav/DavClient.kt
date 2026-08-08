package com.example.data.dav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DavClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val xmlMediaType = "application/xml; charset=utf-8".toMediaType()

    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String,
        traceBuilder: StringBuilder
    ): DavResult<String> = withContext(Dispatchers.IO) {
        val cleanUrl = normalizeUrl(serverUrl)
        traceBuilder.appendLine("➜ Normalizing host URL: $cleanUrl")
        traceBuilder.appendLine("➜ Authenticating as user: $username")

        val endpointsToTry = getPossibleTestEndpoints(serverUrl, username)
        var lastError: DavResult.Error? = null

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:">
               <d:prop>
                  <d:displayname/>
                  <d:resourcetype/>
               </d:prop>
            </d:propfind>
        """.trimIndent()

        for (endpoint in endpointsToTry) {
            traceBuilder.appendLine("➜ Probing DAV endpoint: $endpoint")
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("User-Agent", "ownCloud-Sync-Android/1.0")
                .addHeader("Depth", "1")
                .method("PROPFIND", propfindBody.toRequestBody(xmlMediaType))
                .build()

            try {
                val response = client.newCall(request).execute()
                val code = response.code

                if (response.isSuccessful || code == 207) {
                    traceBuilder.appendLine("✔ HTTP Status $code (${response.message}) on $endpoint")
                    traceBuilder.appendLine("✔ Authentication & DAV endpoint handshake successful!")
                    return@withContext DavResult.Success("Connected successfully to ownCloud DAV endpoint ($endpoint)")
                } else if (code == 401) {
                    traceBuilder.appendLine("✖ Authentication failed (HTTP 401 Unauthorized) on $endpoint. Please verify username/password or app password.")
                    return@withContext DavResult.Error("HTTP 401 Unauthorized - Check username/password or app password", code)
                } else if (code == 405) {
                    traceBuilder.appendLine("  └ HTTP 405 Method Not Allowed on $endpoint (Host root, probing next DAV path...)")
                    lastError = DavResult.Error("HTTP 405 Method Not Allowed", code)
                } else if (code == 404) {
                    traceBuilder.appendLine("  └ HTTP 404 Not Found on $endpoint (Probing next path...)")
                    lastError = DavResult.Error("HTTP 404 Not Found", code)
                } else {
                    traceBuilder.appendLine("  └ HTTP Status $code (${response.message}) on $endpoint")
                    lastError = DavResult.Error("Server returned HTTP $code (${response.message})", code)
                }
            } catch (e: Exception) {
                traceBuilder.appendLine("  └ Probe error on $endpoint: ${e.message}")
                lastError = DavResult.Error("Connection failed: ${e.message}", cause = e)
            }
        }

        traceBuilder.appendLine("✖ Unable to locate active DAV service on host.")
        lastError ?: DavResult.Error("Could not reach a valid ownCloud DAV endpoint. Please check host URL.")
    }

    suspend fun discoverCalendars(
        serverUrl: String,
        username: String,
        password: String,
        traceBuilder: StringBuilder
    ): List<DiscoveredCalendar> = withContext(Dispatchers.IO) {
        val urlsToTry = getPossibleDavEndpoints(serverUrl, username, "calendars")
        val resultList = mutableListOf<DiscoveredCalendar>()

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav" xmlns:a="http://apple.com/ns/ical/">
               <d:prop>
                  <d:displayname/>
                  <d:resourcetype/>
                  <a:calendar-color/>
               </d:prop>
            </d:propfind>
        """.trimIndent()

        for (url in urlsToTry) {
            traceBuilder.appendLine("➜ Probing CalDAV endpoint: $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Depth", "1")
                .method("PROPFIND", propfindBody.toRequestBody(xmlMediaType))
                .build()

            try {
                val response = client.newCall(request).execute()
                traceBuilder.appendLine("  └ Response HTTP ${response.code}")
                if (response.isSuccessful || response.code == 207) {
                    val xml = response.body?.string() ?: ""
                    val calendars = DavParser.parseCalendarsXml(xml, url)
                    if (calendars.isNotEmpty()) {
                        traceBuilder.appendLine("  └ Found ${calendars.size} calendar(s): ${calendars.joinToString { it.displayName }}")
                        resultList.addAll(calendars)
                        break
                    }
                }
            } catch (e: Exception) {
                traceBuilder.appendLine("  └ Probe error on $url: ${e.message}")
            }
        }
        resultList.distinctBy { it.url }
    }

    suspend fun discoverAddressBooks(
        serverUrl: String,
        username: String,
        password: String,
        traceBuilder: StringBuilder
    ): List<DiscoveredAddressBook> = withContext(Dispatchers.IO) {
        val urlsToTry = getPossibleDavEndpoints(serverUrl, username, "addressbooks")
        val resultList = mutableListOf<DiscoveredAddressBook>()

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
               <d:prop>
                  <d:displayname/>
                  <d:resourcetype/>
               </d:prop>
            </d:propfind>
        """.trimIndent()

        for (url in urlsToTry) {
            traceBuilder.appendLine("➜ Probing CardDAV endpoint: $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Depth", "1")
                .method("PROPFIND", propfindBody.toRequestBody(xmlMediaType))
                .build()

            try {
                val response = client.newCall(request).execute()
                traceBuilder.appendLine("  └ Response HTTP ${response.code}")
                if (response.isSuccessful || response.code == 207) {
                    val xml = response.body?.string() ?: ""
                    val books = DavParser.parseAddressBooksXml(xml, url)
                    if (books.isNotEmpty()) {
                        traceBuilder.appendLine("  └ Found ${books.size} address book(s): ${books.joinToString { it.displayName }}")
                        resultList.addAll(books)
                        break
                    }
                }
            } catch (e: Exception) {
                traceBuilder.appendLine("  └ Probe error on $url: ${e.message}")
            }
        }
        resultList.distinctBy { it.url }
    }

    suspend fun fetchCalendarEvents(
        calendarUrl: String,
        username: String,
        password: String,
        traceBuilder: StringBuilder
    ): ParsedCalendarFetchResult = withContext(Dispatchers.IO) {
        traceBuilder.appendLine("➜ Fetching calendar events from: $calendarUrl")

        // Try REPORT calendar-query first, fallback to GET or PROPFIND
        val reportBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
               <d:prop>
                  <d:getetag/>
                  <c:calendar-data/>
               </d:prop>
               <c:filter>
                  <c:comp-filter name="VCALENDAR">
                     <c:comp-filter name="VEVENT"/>
                  </c:comp-filter>
               </c:filter>
            </c:calendar-query>
        """.trimIndent()

        val request = Request.Builder()
            .url(calendarUrl)
            .addHeader("Authorization", Credentials.basic(username, password))
            .addHeader("Depth", "1")
            .method("REPORT", reportBody.toRequestBody(xmlMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            traceBuilder.appendLine("  └ REPORT status HTTP ${response.code}")

            if (response.isSuccessful || response.code == 207) {
                val xml = response.body?.string() ?: ""
                val events = DavParser.parseIcsEvents(xml)
                traceBuilder.appendLine("  └ Parsed ${events.size} event(s) from $calendarUrl")
                ParsedCalendarFetchResult(events, null)
            } else {
                // Try fallback GET
                val getRequest = Request.Builder()
                    .url(calendarUrl)
                    .addHeader("Authorization", Credentials.basic(username, password))
                    .get()
                    .build()
                val getResponse = client.newCall(getRequest).execute()
                val content = getResponse.body?.string() ?: ""
                val events = DavParser.parseIcsEvents(content)
                traceBuilder.appendLine("  └ GET Fallback status HTTP ${getResponse.code}, parsed ${events.size} event(s)")
                ParsedCalendarFetchResult(events, null)
            }
        } catch (e: Exception) {
            traceBuilder.appendLine("  └ Error fetching calendar events: ${e.message}")
            ParsedCalendarFetchResult(emptyList(), e.message)
        }
    }

    suspend fun fetchAddressBookContacts(
        addressBookUrl: String,
        username: String,
        password: String,
        traceBuilder: StringBuilder
    ): ParsedContactFetchResult = withContext(Dispatchers.IO) {
        traceBuilder.appendLine("➜ Fetching address book contacts from: $addressBookUrl")

        val reportBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <card:addressbook-query xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
               <d:prop>
                  <d:getetag/>
                  <card:address-data/>
               </d:prop>
            </card:addressbook-query>
        """.trimIndent()

        val request = Request.Builder()
            .url(addressBookUrl)
            .addHeader("Authorization", Credentials.basic(username, password))
            .addHeader("Depth", "1")
            .method("REPORT", reportBody.toRequestBody(xmlMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            traceBuilder.appendLine("  └ REPORT status HTTP ${response.code}")

            if (response.isSuccessful || response.code == 207) {
                val xml = response.body?.string() ?: ""
                val contacts = DavParser.parseVCards(xml)
                traceBuilder.appendLine("  └ Parsed ${contacts.size} contact(s) from $addressBookUrl")
                ParsedContactFetchResult(contacts, null)
            } else {
                val getRequest = Request.Builder()
                    .url(addressBookUrl)
                    .addHeader("Authorization", Credentials.basic(username, password))
                    .get()
                    .build()
                val getResponse = client.newCall(getRequest).execute()
                val content = getResponse.body?.string() ?: ""
                val contacts = DavParser.parseVCards(content)
                traceBuilder.appendLine("  └ GET Fallback status HTTP ${getResponse.code}, parsed ${contacts.size} contact(s)")
                ParsedContactFetchResult(contacts, null)
            }
        } catch (e: Exception) {
            traceBuilder.appendLine("  └ Error fetching contacts: ${e.message}")
            ParsedContactFetchResult(emptyList(), e.message)
        }
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean.trimEnd('/') + "/"
    }

    private fun getPossibleTestEndpoints(serverUrl: String, username: String): List<String> {
        val base = normalizeUrl(serverUrl)
        val host = if (base.contains("://")) {
            val scheme = base.substringBefore("://")
            val rest = base.substringAfter("://").substringBefore("/")
            "$scheme://$rest"
        } else base

        val isDirectDavPath = base.contains("remote.php") || base.contains("/dav")
        val candidates = mutableListOf<String>()

        if (isDirectDavPath) {
            candidates.add(base)
        }

        // Standard ownCloud / Nextcloud DAV endpoints
        candidates.add("${base}remote.php/dav/")
        candidates.add("${base}remote.php/dav/calendars/$username/")
        candidates.add("${base}remote.php/dav/addressbooks/users/$username/")
        candidates.add("${base}remote.php/dav/addressbooks/$username/")
        candidates.add("${base}remote.php/webdav/")
        candidates.add("${base}remote.php/caldav/")
        candidates.add("${base}remote.php/carddav/")

        if (!base.startsWith("$host/owncloud/")) {
            candidates.add("${host}/owncloud/remote.php/dav/")
        }
        if (!base.startsWith("$host/nextcloud/")) {
            candidates.add("${host}/nextcloud/remote.php/dav/")
        }

        if (!isDirectDavPath) {
            candidates.add(base)
        }

        return candidates.distinct()
    }

    private fun getPossibleDavEndpoints(serverUrl: String, username: String, type: String): List<String> {
        val base = normalizeUrl(serverUrl)
        val host = if (base.contains("://")) {
            val scheme = base.substringBefore("://")
            val rest = base.substringAfter("://").substringBefore("/")
            "$scheme://$rest"
        } else base

        val isCal = type == "calendars"
        val isDirectDavPath = base.contains("remote.php") || base.contains("/dav")
        val candidates = mutableListOf<String>()

        if (isDirectDavPath) {
            candidates.add(base)
        }

        if (isCal) {
            candidates.add("${base}remote.php/dav/calendars/$username/")
            candidates.add("${base}remote.php/dav/")
            candidates.add("${base}remote.php/dav/principals/users/$username/")
            candidates.add("${base}remote.php/caldav/calendars/$username/")
            candidates.add("${base}remote.php/caldav/")
            candidates.add("${host}/remote.php/dav/calendars/$username/")
            candidates.add("${host}/remote.php/dav/")
            candidates.add("${host}/owncloud/remote.php/dav/calendars/$username/")
            candidates.add("${host}/nextcloud/remote.php/dav/calendars/$username/")
        } else {
            candidates.add("${base}remote.php/dav/addressbooks/users/$username/")
            candidates.add("${base}remote.php/dav/addressbooks/$username/")
            candidates.add("${base}remote.php/dav/")
            candidates.add("${base}remote.php/dav/principals/users/$username/")
            candidates.add("${base}remote.php/carddav/addressbooks/$username/")
            candidates.add("${base}remote.php/carddav/")
            candidates.add("${host}/remote.php/dav/addressbooks/users/$username/")
            candidates.add("${host}/remote.php/dav/")
            candidates.add("${host}/owncloud/remote.php/dav/addressbooks/users/$username/")
            candidates.add("${host}/nextcloud/remote.php/dav/addressbooks/users/$username/")
        }

        if (!isDirectDavPath) {
            candidates.add(base)
        }

        return candidates.distinct()
    }
}

data class ParsedCalendarFetchResult(
    val events: List<ParsedIcsEvent>,
    val error: String?
)

data class ParsedContactFetchResult(
    val contacts: List<ParsedVCardContact>,
    val error: String?
)
