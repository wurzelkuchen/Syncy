package com.example.data.system

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.core.content.ContextCompat
import com.example.data.model.AddressBookEntity
import com.example.data.model.ContactEntity

object SystemContactsSync {

    fun hasContactsPermissions(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    fun removeSystemContacts(context: Context, accountUsername: String): Boolean {
        if (!hasContactsPermissions(context)) return false
        val accountName = accountUsername.ifBlank { "ownCloud" }
        return try {
            context.contentResolver.delete(
                RawContacts.CONTENT_URI,
                "${RawContacts.SYNC1} = ? OR ${RawContacts.ACCOUNT_NAME} = ?",
                arrayOf(accountName, accountName)
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun syncContactsToSystem(
        context: Context,
        accountUsername: String,
        addressBook: AddressBookEntity,
        contacts: List<ContactEntity>,
        traceBuilder: StringBuilder? = null
    ): Boolean {
        if (!hasContactsPermissions(context)) {
            traceBuilder?.appendLine("  └ [SystemContacts] Skipping system contacts export: READ/WRITE_CONTACTS permissions not granted.")
            return false
        }

        val contentResolver = context.contentResolver
        val accountName = accountUsername.ifBlank { "ownCloud" }

        if (!addressBook.syncEnabled) {
            removeSystemContacts(context, accountUsername)
            traceBuilder?.appendLine("  └ [SystemContacts] Address book '${addressBook.effectiveName}' sync is disabled. Cleared system contacts.")
            return true
        }

        return try {
            // Delete old raw contacts for this account
            val deleted = contentResolver.delete(
                RawContacts.CONTENT_URI,
                "${RawContacts.SYNC1} = ? OR ${RawContacts.ACCOUNT_NAME} = ?",
                arrayOf(accountName, accountName)
            )
            traceBuilder?.appendLine("  └ [SystemContacts] Cleared $deleted old raw contact(s) for account '$accountName'")

            var exportedCount = 0
            // Process contacts in batches of max 50 to avoid TransactionTooLargeException
            val batches = contacts.chunked(50)

            for (batch in batches) {
                val ops = ArrayList<ContentProviderOperation>()

                for (contact in batch) {
                    val rawContactIndex = ops.size

                    // Create raw contact attached as device local contact with SYNC1 tag
                    ops.add(
                        ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                            .withValue(RawContacts.ACCOUNT_NAME, null)
                            .withValue(RawContacts.ACCOUNT_TYPE, null)
                            .withValue(RawContacts.SYNC1, accountName)
                            .withValue(RawContacts.SOURCE_ID, contact.uid)
                            .build()
                    )

                    // Structured Name (Display Name + Given & Family name for Android indexing)
                    val name = contact.fullName.trim()
                    if (name.isNotBlank()) {
                        val nameParts = name.split("\\s+".toRegex(), limit = 2)
                        val givenName = nameParts.getOrNull(0) ?: ""
                        val familyName = nameParts.getOrNull(1) ?: ""

                        ops.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
                                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(StructuredName.DISPLAY_NAME, name)
                                .withValue(StructuredName.GIVEN_NAME, givenName)
                                .withValue(StructuredName.FAMILY_NAME, familyName)
                                .build()
                        )
                    }

                    // Phone Number
                    if (contact.phoneNumber.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
                                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                .withValue(Phone.NUMBER, contact.phoneNumber)
                                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                                .build()
                        )
                    }

                    // Email Address
                    if (contact.email.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
                                .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                                .withValue(Email.ADDRESS, contact.email)
                                .withValue(Email.TYPE, Email.TYPE_WORK)
                                .build()
                        )
                    }

                    // Organization
                    if (contact.organization.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
                                .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                                .withValue(Organization.COMPANY, contact.organization)
                                .build()
                        )
                    }

                    // Notes
                    if (contact.notes.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
                                .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                                .withValue(Note.NOTE, contact.notes)
                                .build()
                        )
                    }

                    exportedCount++
                }

                if (ops.isNotEmpty()) {
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                }
            }

            try {
                contentResolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
            } catch (_: Exception) {}

            traceBuilder?.appendLine("  └ [SystemContacts] Successfully exported $exportedCount contact(s) to Device Contacts Provider!")
            true
        } catch (e: Exception) {
            traceBuilder?.appendLine("  └ [SystemContacts] Error syncing contacts to system: ${e.message}")
            false
        }
    }
}
