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
                                currentHref = parser.nextText().trim()
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
                            if (isCalendar && currentHref.isNotBlank()) {
                                val fullUrl = resolveUrl(baseUrl, currentHref)
                                val name = currentDisplayName.ifBlank {
                                    fullUrl.trimEnd('/').substringAfterLast('/')
                                }
                                if (!list.any { it.url == fullUrl }) {
                                    list.add(DiscoveredCalendar(fullUrl, name, currentColor))
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
            // Fallback string matching if XML is malformed or custom namespace
            if (xml.contains("calendar", ignoreCase = true)) {
                val hrefMatches = Regex("(?i)<(?:\\w+:)?href>([^<]+)").findAll(xml)
                for (match in hrefMatches) {
                    val href = match.groupValues[1].trim()
                    if (href.contains("calendar", ignoreCase = true) || href.contains("dav", ignoreCase = true)) {
                        val fullUrl = resolveUrl(baseUrl, href)
                        val name = fullUrl.trimEnd('/').substringAfterLast('/')
                        if (name.isNotEmpty() && !list.any { it.url == fullUrl }) {
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
                                currentHref = parser.nextText().trim()
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
                            if (isAddressBook && currentHref.isNotBlank()) {
                                val fullUrl = resolveUrl(baseUrl, currentHref)
                                val name = currentDisplayName.ifBlank {
                                    fullUrl.trimEnd('/').substringAfterLast('/')
                                }
                                if (!list.any { it.url == fullUrl }) {
                                    list.add(DiscoveredAddressBook(fullUrl, name))
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
                        val fullUrl = resolveUrl(baseUrl, href)
                        val name = fullUrl.trimEnd('/').substringAfterLast('/')
                        if (name.isNotEmpty() && !list.any { it.url == fullUrl }) {
                            list.add(DiscoveredAddressBook(fullUrl, name))
                        }
                    }
                }
            }
        }
        return list
    }

    /**
     * Parses iCalendar (.ics) format string into event objects.
     */
    fun parseIcsEvents(icsContent: String): List<ParsedIcsEvent> {
        val events = mutableListOf<ParsedIcsEvent>()
        if (icsContent.isBlank()) return events

        val lines = icsContent.lines()
        var inEvent = false
        var uid = ""
        var summary = ""
        var location = ""
        var description = ""
        var dtStart: Long = System.currentTimeMillis()
        var dtEnd: Long = System.currentTimeMillis() + 3600000

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                uid = ""
                summary = ""
                location = ""
                description = ""
                dtStart = System.currentTimeMillis()
                dtEnd = System.currentTimeMillis() + 3600000
            } else if (line.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent) {
                    if (uid.isEmpty()) uid = "uid_" + System.currentTimeMillis() + "_" + (0..999).random()
                    if (summary.isEmpty()) summary = "Untitled Event"
                    events.add(ParsedIcsEvent(uid, summary, dtStart, dtEnd, location, description))
                }
                inEvent = false
            } else if (inEvent) {
                val parts = line.split(":", limit = 2)
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
        }
        return events
    }

    /**
     * Parses vCard (.vcf) format string into contact objects.
     */
    fun parseVCards(vCardContent: String): List<ParsedVCardContact> {
        val contacts = mutableListOf<ParsedVCardContact>()
        if (vCardContent.isBlank()) return contacts

        val lines = vCardContent.lines()
        var inVCard = false
        var uid = ""
        var fn = ""
        var tel = ""
        var email = ""
        var org = ""
        var note = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.equals("BEGIN:VCARD", ignoreCase = true)) {
                inVCard = true
                uid = ""
                fn = ""
                tel = ""
                email = ""
                org = ""
                note = ""
            } else if (line.equals("END:VCARD", ignoreCase = true)) {
                if (inVCard) {
                    if (uid.isEmpty()) uid = "card_" + System.currentTimeMillis() + "_" + (0..999).random()
                    if (fn.isEmpty()) fn = "Unnamed Contact"
                    contacts.add(ParsedVCardContact(uid, fn, tel, email, org, note))
                }
                inVCard = false
            } else if (inVCard) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].uppercase(Locale.ROOT)
                    val value = parts[1].trim()

                    when {
                        key.startsWith("UID") -> uid = value
                        key.startsWith("FN") -> fn = value
                        key.startsWith("TEL") -> {
                            if (tel.isEmpty()) tel = value else tel += ", $value"
                        }
                        key.startsWith("EMAIL") -> {
                            if (email.isEmpty()) email = value else email += ", $value"
                        }
                        key.startsWith("ORG") -> org = value.replace(";", " ")
                        key.startsWith("NOTE") -> note = value
                    }
                }
            }
        }
        return contacts
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
