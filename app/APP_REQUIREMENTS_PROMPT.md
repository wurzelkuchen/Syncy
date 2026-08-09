# Master App Requirements & Specification Prompt

> **Instructions for Starting from Scratch:**
> Copy and paste the prompt below into Google AI Studio to recreate or reboot this app from scratch with all discovered features, architecture patterns, and system integrations.

---

```markdown
Build a production-ready Android application in Kotlin using Jetpack Compose and Material Design 3 for syncing ownCloud / Nextcloud / WebDAV (CalDAV & CardDAV) calendars and contacts with the Android native System Calendar and Contacts Providers.

---

### Key Requirements & Functional Overview

#### 1. Architecture & Core Tech Stack
- **UI Framework:** Jetpack Compose with Material Design 3 (M3) components, `Scaffold`, and dynamic light/dark theme.
- **State Management:** `ViewModel`, `StateFlow`, and `collectAsStateWithLifecycle`.
- **Database:** Room Database with `AccountEntity`, `CalendarEntity`, `AddressBookEntity`, `CalendarEventEntity`, `ContactEntity`, and `SyncLogEntity`.
- **Concurrency & I/O:** Kotlin Coroutines and Flow for non-blocking asynchronous WebDAV/CalDAV/CardDAV network calls.
- **Permissions:** Runtime permission handling for `READ_CALENDAR`, `WRITE_CALENDAR`, `READ_CONTACTS`, and `WRITE_CONTACTS`.

---

#### 2. Features & Modules

##### A. Account Setup & WebDAV Server Connection
- Server URL, Username, and Password input configuration.
- "Test & Save Connection" action that validates DAV endpoint reachability and saves credentials securely in Room DB.
- Connection status indicators displaying last sync timestamp and overall synchronization state.

##### B. CalDAV & CardDAV Discovery Engine
- Automatic discovery of user Calendars via CalDAV `PROPFIND` requests.
- Automatic discovery of Address Books via CardDAV `PROPFIND` requests.
- iCalendar (.ics / VEVENT) parser extracting Event UID, Summary, Description, Location, Start Time, and End Time.
- vCard (.vcf / VCARD) parser extracting Contact UID, Full Name, Given Name, Family Name, Email, and Phone Number.

##### C. Sync Customization & Selective Management
- **Per-Calendar & Per-AddressBook Toggles (`syncEnabled`):** Allow users to individually enable or disable sync for specific calendars or address books.
- **Custom Name Overrides (`customName` / `effectiveName`):** Allow users to rename calendars and address books locally. Custom names are preserved during server sync re-discoveries and exported to the Android system calendar/contacts provider.
- **Automatic Cleanup:** Disabling sync for a calendar or address book immediately removes its corresponding events or contacts from both the local Room database and the Android System Providers.

##### D. Native Android System Provider Integration
- **Calendar Provider (`CalendarContract`):**
  - Export synced CalDAV events into local device system calendars created under the user's account name.
  - Set title, description, location, timezone, start/end times, and sync flags.
  - Trigger `contentResolver.notifyChange` upon sync completion.
- **Contacts Provider (`ContactsContract`):**
  - Export CardDAV contacts into native Android Device Contacts using `ContentProviderOperation` batches.
  - Write `StructuredName` entries (`DISPLAY_NAME`, `GIVEN_NAME`, `FAMILY_NAME`) for proper indexing in Android system search and dialer apps.
  - Tag raw contacts with `SYNC1` metadata to manage updates cleanly without duplicating entries.

##### E. User Interface Layout (3 Main Tabs)
1. **Sync & Account Tab:**
   - Server credentials card with edit/save functions.
   - "Sync Now" button with execution progress indicators and real-time trace logging.
   - System permissions status checker card.
2. **Data & Customization Viewer Tab:**
   - List of discovered Calendars & Address Books with `syncEnabled` switches and rename (edit custom name) dialogs.
   - Search bar filtering events and contacts in real time.
   - Card lists showing preview details of synced events and contacts.
3. **Logs & Diagnostics Tab:**
   - Historical list of all sync execution runs with status tags (Success, Warning, Error).
   - Expandable detailed trace logs viewer for troubleshooting CalDAV/CardDAV HTTP responses.

---

#### 3. Database Schema Reference (Room)

- **`accounts`**: `id`, `serverUrl`, `username`, `password`, `lastSyncTimestamp`, `status`
- **`calendars`**: `id` (URL), `accountId`, `displayName`, `customName`, `color`, `url`, `syncEnabled`, `eventCount`, `lastSyncTimestamp`
- **`address_books`**: `id` (URL), `accountId`, `displayName`, `customName`, `url`, `syncEnabled`, `contactCount`, `lastSyncTimestamp`
- **`calendar_events`**: `id`, `calendarId`, `uid`, `summary`, `description`, `location`, `startTime`, `endTime`
- **`contacts`**: `id`, `addressBookId`, `uid`, `fullName`, `givenName`, `familyName`, `email`, `phone`
- **`sync_logs`**: `id`, `timestamp`, `status`, `summary`, `trace`
```
