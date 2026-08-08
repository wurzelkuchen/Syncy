package com.example.data.dav

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DavParser {

    /**
     * Parses WebDAV PROPFIND XML response to discover calendar and address book home-set URLs
     * and current-user-principal URLs.
     */
    fun parseHomeSetsXml(xml: String, baseUrl: String): DavHomeSets {
        if (xml.isBlank()) return DavHomeSets()

        val calHomeSets = mutableListOf<String>()
        val abHomeSets = mutableListOf<String>()
        val principals = mutableListOf<String>()

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inCalHomeSet = false
            var inAbHomeSet = false
            var inPrincipal = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name?.lowercase(Locale.ROOT) ?: ""
                val tagName = rawName.substringAfter(":")

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "calendar-home-set" -> inCalHomeSet = true
                            "addressbook-home-set" -> inAbHomeSet = true
                            "current-user-principal" -> inPrincipal = true
                            "href" -> {
                                val href = parser.nextText().trim()
                                if (href.isNotBlank()) {
                                    val fullUrl = resolveUrl(baseUrl, href)
                                    val cleanUrl = fullUrl.trimEnd('/') + "/"
                                    if (inCalHomeSet && !calHomeSets.contains(cleanUrl)) {
                                        calHomeSets.add(cleanUrl)
                                    } else if (inAbHomeSet && !abHomeSets.contains(cleanUrl)) {
                                        abHomeSets.add(cleanUrl)
                                    } else if (inPrincipal && !principals.contains(cleanUrl)) {
                                        principals.add(cleanUrl)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tagName) {
                            "calendar-home-set" -> inCalHomeSet = false
                            "addressbook-home-set" -> inAbHomeSet = false
                            "current-user-principal" -> inPrincipal = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        // Fallback regex scan for home-sets
        if (calHomeSets.isEmpty()) {
            val calMatches = Regex("(?i)<(?:\\w+:)?calendar-home-set>[\\s\\S]*?<(?:\\w+:)?href>([^<]+)").findAll(xml)
            for (match in calMatches) {
                val href = match.groupValues[1].trim()
                val cleanUrl = resolveUrl(baseUrl, href).trimEnd('/') + "/"
                if (!calHomeSets.contains(cleanUrl)) calHomeSets.add(cleanUrl)
            }
        }
        if (abHomeSets.isEmpty()) {
            val abMatches = Regex("(?i)<(?:\\w+:)?addressbook-home-set>[\\s\\S]*?<(?:\\w+:)?href>([^<]+)").findAll(xml)
            for (match in abMatches) {
                val href = match.groupValues[1].trim()
                val cleanUrl = resolveUrl(baseUrl, href).trimEnd('/') + "/"
                if (!abHomeSets.contains(cleanUrl)) abHomeSets.add(cleanUrl)
            }
        }
        if (principals.isEmpty()) {
            val pMatches = Regex("(?i)<(?:\\w+:)?current-user-principal>[\\s\\S]*?<(?:\\w+:)?href>([^<]+)").findAll(xml)
            for (match in pMatches) {
                val href = match.groupValues[1].trim()
                val cleanUrl = resolveUrl(baseUrl, href).trimEnd('/') + "/"
                if (!principals.contains(cleanUrl)) principals.add(cleanUrl)
            }
        }

        return DavHomeSets(
            calendarHomeSets = calHomeSets.distinct(),
            addressBookHomeSets = abHomeSets.distinct(),
            principalUrls = principals.distinct()
        )
    }

    /**
     * Parses WebDAV PROPFIND XML response to discover calendars.
     */
    fun parseCalendarsXml(xml: String, baseUrl: String): List<DiscoveredCalendar> {
        val list = mutableListOf<DiscoveredCalendar>()
        if (xml.isBlank()) return list

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentHref = ""
            var currentDisplayName = ""
            var currentColor = "#0288D1"
            var isCalendar = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name?.lowercase(Locale.ROOT) ?: ""
                val tagName = rawName.substringAfter(":")

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "href" -> {
                                if (currentHref.isBlank()) {
                                    currentHref = parser.nextText().trim()
                                }
                            }
                            "displayname" -> {
                                currentDisplayName = parser.nextText().trim()
                            }
                            "calendar-color" -> {
                                val colorText = parser.nextText().trim()
                                if (colorText.startsWith("#")) {
                                    currentColor = colorText
                                }
                            }
                            "calendar" -> {
                                isCalendar = true
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "response") {
                            if (currentHref.isNotBlank()) {
                                val fullUrl = resolveUrl(baseUrl, currentHref)
                                val cleanFullUrl = fullUrl.trimEnd('/') + "/"
                                val lastSegment = cleanFullUrl.trimEnd('/').substringAfterLast('/')

                                val isCalCollection = isCalendar || (
                                    cleanFullUrl.contains("/calendars/", ignoreCase = true) &&
                                    lastSegment.isNotBlank() &&
                                    !lastSegment.equals("calendars", ignoreCase = true)
                                )

                                if (isCalCollection) {
                                    val name = currentDisplayName.ifBlank { lastSegment }
                                    if (name.isNotBlank() && !list.any { it.url == cleanFullUrl }) {
                                        list.add(DiscoveredCalendar(cleanFullUrl, name, currentColor))
                                    }
                                }
                            }
                            currentHref = ""
                            currentDisplayName = ""
                            currentColor = "#0288D1"
                            isCalendar = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Fallback string matching if XML parsing failed
            if (xml.contains("calendar", ignoreCase = true)) {
                val hrefMatches = Regex("(?i)<(?:\\w+:)?href>([^<]+)").findAll(xml)
                for (match in hrefMatches) {
                    val href = match.groupValues[1].trim()
                    if (href.contains("calendar", ignoreCase = true) || href.contains("dav", ignoreCase = true)) {
                        val fullUrl = resolveUrl(baseUrl, href).trimEnd('/') + "/"
                        val name = fullUrl.trimEnd('/').substringAfterLast('/')
                        if (name.isNotEmpty() && !name.equals("calendars", ignoreCase = true) && !list.any { it.url == fullUrl }) {
                            list.add(DiscoveredCalendar(fullUrl, name))
                        }
                    }
                }
            }
        }
        return list
    }

    /**
     * Parses WebDAV PROPFIND XML response to discover address books.
     */
    fun parseAddressBooksXml(xml: String, baseUrl: String): List<DiscoveredAddressBook> {
        val list = mutableListOf<DiscoveredAddressBook>()
        if (xml.isBlank()) return list

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentHref = ""
            var currentDisplayName = ""
            var isAddressBook = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name?.lowercase(Locale.ROOT) ?: ""
                val tagName = rawName.substringAfter(":")

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "href" -> {
                                if (currentHref.isBlank()) {
                                    currentHref = parser.nextText().trim()
                                }
                            }
                            "displayname" -> {
                                currentDisplayName = parser.nextText().trim()
                            }
                            "addressbook" -> {
                                isAddressBook = true
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "response") {
                            if (currentHref.isNotBlank()) {
                                val fullUrl = resolveUrl(baseUrl, currentHref)
                                val cleanFullUrl = fullUrl.trimEnd('/') + "/"
                                val lastSegment = cleanFullUrl.trimEnd('/').substringAfterLast('/')

                                val isAbCollection = isAddressBook || (
                                    (cleanFullUrl.contains("/addressbooks/", ignoreCase = true) || cleanFullUrl.contains("/carddav/", ignoreCase = true)) &&
                                    lastSegment.isNotBlank() &&
                                    !lastSegment.equals("addressbooks", ignoreCase = true) &&
                                    !lastSegment.equals("users", ignoreCase = true)
                                )

                                if (isAbCollection) {
                                    val name = currentDisplayName.ifBlank { lastSegment }
                                    if (name.isNotBlank() && !list.any { it.url == cleanFullUrl }) {
                                        list.add(DiscoveredAddressBook(cleanFullUrl, name))
                                    }
                                }
                            }
                            currentHref = ""
                            currentDisplayName = ""
                            isAddressBook = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Regex fallback
            if (xml.contains("addressbook", ignoreCase = true) || xml.contains("contacts", ignoreCase = true)) {
                val hrefMatches = Regex("(?i)<(?:\\w+:)?href>([^<]+)").findAll(xml)
                for (match in hrefMatches) {
                    val href = match.groupValues[1].trim()
                    if (href.contains("contacts", ignoreCase = true) || href.contains("addressbook", ignoreCase = true)) {
                        val fullUrl = resolveUrl(baseUrl, href).trimEnd('/') + "/"
                        val name = fullUrl.trimEnd('/').substringAfterLast('/')
                        if (name.isNotEmpty() && !name.equals("addressbooks", ignoreCase = true) && !list.any { it.url == fullUrl }) {
                            list.add(DiscoveredAddressBook(fullUrl, name))
                        }
                    }
                }
            }
        }
        return list
    }

    /**
     * Parses iCalendar (.ics) format string or XML containing VEVENT blocks into event objects.
     */
    fun parseIcsEvents(icsContent: String): List<ParsedIcsEvent> {
        val events = mutableListOf<ParsedIcsEvent>()
        if (icsContent.isBlank()) return events

        val unescapedContent = unescapeXmlEntities(icsContent)
        val uppercase = unescapedContent.uppercase(Locale.ROOT)
        var searchIndex = 0

        while (searchIndex < unescapedContent.length) {
            val startIndex = uppercase.indexOf("BEGIN:VEVENT", searchIndex)
            if (startIndex == -1) break

            val endIndex = uppercase.indexOf("END:VEVENT", startIndex)
            val block = if (endIndex == -1) {
                unescapedContent.substring(startIndex)
            } else {
                unescapedContent.substring(startIndex, endIndex + "END:VEVENT".length)
            }

            val event = parseSingleIcsEvent(block)
            if (event != null) {
                events.add(event)
            }

            if (endIndex == -1) break
            searchIndex = endIndex + "END:VEVENT".length
        }

        return events
    }

    private fun parseSingleIcsEvent(block: String): ParsedIcsEvent? {
        val lines = block.lines()
        var uid = ""
        var summary = ""
        var location = ""
        var description = ""
        var dtStart: Long = System.currentTimeMillis()
        var dtEnd: Long = System.currentTimeMillis() + 3600000

        for (rawLine in lines) {
            val cleanLine = rawLine.replace(Regex("<[^>]+>"), "").trim()
            if (cleanLine.isBlank() || cleanLine.startsWith("BEGIN:VEVENT", ignoreCase = true) || cleanLine.startsWith("END:VEVENT", ignoreCase = true)) {
                continue
            }

            val parts = cleanLine.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].uppercase(Locale.ROOT)
                val value = unescapeIcsValue(parts[1])

                when {
                    key.startsWith("UID") -> uid = value
                    key.startsWith("SUMMARY") -> summary = value
                    key.startsWith("LOCATION") -> location = value
                    key.startsWith("DESCRIPTION") -> description = value
                    key.startsWith("DTSTART") -> dtStart = parseIcsDateTime(value)
                    key.startsWith("DTEND") -> dtEnd = parseIcsDateTime(value)
                }
            }
        }

        if (uid.isEmpty()) uid = "uid_" + System.currentTimeMillis() + "_" + (0..999).random()
        if (summary.isEmpty()) summary = "Untitled Event"

        return ParsedIcsEvent(uid, summary, dtStart, dtEnd, location, description)
    }

    /**
     * Parses vCard (.vcf) format string or XML containing VCARD blocks into contact objects.
     */
    fun parseVCards(vCardContent: String): List<ParsedVCardContact> {
        val contacts = mutableListOf<ParsedVCardContact>()
        if (vCardContent.isBlank()) return contacts

        val unescapedContent = unescapeXmlEntities(vCardContent)
        val uppercase = unescapedContent.uppercase(Locale.ROOT)
        var searchIndex = 0

        while (searchIndex < unescapedContent.length) {
            val startIndex = uppercase.indexOf("BEGIN:VCARD", searchIndex)
            if (startIndex == -1) break

            val endIndex = uppercase.indexOf("END:VCARD", startIndex)
            val block = if (endIndex == -1) {
                unescapedContent.substring(startIndex)
            } else {
                unescapedContent.substring(startIndex, endIndex + "END:VCARD".length)
            }

            val contact = parseSingleVCard(block)
            if (contact != null) {
                contacts.add(contact)
            }

            if (endIndex == -1) break
            searchIndex = endIndex + "END:VCARD".length
        }

        return contacts
    }

    private fun parseSingleVCard(block: String): ParsedVCardContact? {
        val lines = block.lines()
        var uid = ""
        var fn = ""
        var tel = ""
        var email = ""
        var org = ""
        var note = ""

        for (rawLine in lines) {
            val cleanLine = rawLine.replace(Regex("<[^>]+>"), "").trim()
            if (cleanLine.isBlank() || cleanLine.startsWith("BEGIN:VCARD", ignoreCase = true) || cleanLine.startsWith("END:VCARD", ignoreCase = true)) {
                continue
            }

            val parts = cleanLine.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].uppercase(Locale.ROOT)
                val value = parts[1].trim()

                when {
                    key.startsWith("UID") -> uid = value
                    key.startsWith("FN") -> fn = unescapeXmlEntities(value)
                    key.startsWith("N") && fn.isEmpty() -> {
                        val nameParts = value.split(";")
                        val lastName = nameParts.getOrNull(0)?.trim() ?: ""
                        val firstName = nameParts.getOrNull(1)?.trim() ?: ""
                        fn = unescapeXmlEntities("$firstName $lastName".trim())
                    }
                    key.startsWith("TEL") -> {
                        val cleanTel = unescapeXmlEntities(value)
                        if (tel.isEmpty()) tel = cleanTel else tel += ", $cleanTel"
                    }
                    key.startsWith("EMAIL") -> {
                        val cleanEmail = unescapeXmlEntities(value)
                        if (email.isEmpty()) email = cleanEmail else email += ", $cleanEmail"
                    }
                    key.startsWith("ORG") -> org = unescapeXmlEntities(value.replace(";", " ").trim())
                    key.startsWith("NOTE") -> note = unescapeXmlEntities(value)
                }
            }
        }

        if (uid.isEmpty()) uid = "card_" + System.currentTimeMillis() + "_" + (0..999).random()
        if (fn.isEmpty()) fn = "Unnamed Contact"

        return ParsedVCardContact(uid, fn, tel, email, org, note)
    }

    fun parseVcfHrefs(xml: String, baseUrl: String): List<String> {
        val hrefs = mutableListOf<String>()
        if (xml.isBlank()) return hrefs
        val matches = Regex("(?i)<(?:\\w+:)?href>([^<]+)").findAll(xml)
        for (match in matches) {
            val href = match.groupValues[1].trim()
            if (href.endsWith(".vcf", ignoreCase = true)) {
                val fullUrl = resolveUrl(baseUrl, href)
                if (!hrefs.contains(fullUrl)) hrefs.add(fullUrl)
            }
        }
        return hrefs
    }

    fun parseIcsHrefs(xml: String, baseUrl: String): List<String> {
        val hrefs = mutableListOf<String>()
        if (xml.isBlank()) return hrefs
        val matches = Regex("(?i)<(?:\\w+:)?href>([^<]+)").findAll(xml)
        for (match in matches) {
            val href = match.groupValues[1].trim()
            if (href.endsWith(".ics", ignoreCase = true)) {
                val fullUrl = resolveUrl(baseUrl, href)
                if (!hrefs.contains(fullUrl)) hrefs.add(fullUrl)
            }
        }
        return hrefs
    }

    private fun unescapeXmlEntities(text: String): String {
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&#13;", "")
            .replace("&#10;", "\n")
    }

    private fun parseIcsDateTime(value: String): Long {
        val raw = value.trim()
        val formats = listOf(
            "yyyyMMdd'T'HHmmss'Z'",
            "yyyyMMdd'T'HHmmss",
            "yyyyMMdd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                if (fmt.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(raw)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun unescapeIcsValue(valStr: String): String {
        return valStr.replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    private fun resolveUrl(baseUrl: String, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href

        val cleanBase = baseUrl.trimEnd('/')
        val host = if (cleanBase.contains("://")) {
            val scheme = cleanBase.substringBefore("://")
            val hostAndPath = cleanBase.substringAfter("://")
            val hostOnly = hostAndPath.substringBefore("/")
            "$scheme://$hostOnly"
        } else {
            "https://$cleanBase"
        }

        return if (href.startsWith("/")) {
            "$host$href"
        } else {
            "$cleanBase/$href"
        }
    }
}
