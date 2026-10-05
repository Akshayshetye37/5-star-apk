package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.AppSection
import com.example.data.model.BackupMetadata
import com.example.data.model.Booking
import com.example.data.model.BookingGuest
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.FoodItem
import com.example.data.model.FoodOrder
import com.example.data.model.HotelSettings
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Payment
import com.example.data.model.Reservation
import com.example.data.model.RoomEntity
import com.example.data.repository.FullDatabaseExport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object BackupRestoreManager {

    fun exportToJson(data: FullDatabaseExport): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("timestamp", System.currentTimeMillis())

        // 1. Settings
        val s = JSONObject().apply {
            put("hotelName", data.settings.hotelName)
            put("address", data.settings.address)
            put("phone", data.settings.phone)
            put("whatsapp", data.settings.whatsapp)
            put("email", data.settings.email)
            put("gstNumber", data.settings.gstNumber)
            put("logoUri", data.settings.logoUri)
                put("slogan", data.settings.slogan)
                put("manualQrUri", data.settings.manualQrUri)
                put("localBackupEnabled", data.settings.localBackupEnabled)
                put("driveBackupEnabled", data.settings.driveBackupEnabled)
                put("driveFolderUri", data.settings.driveFolderUri)
                put("backupSensitiveData", data.settings.backupSensitiveData)
            put("upiId", data.settings.upiId)
            put("upiPayeeName", data.settings.upiPayeeName)
            put("currencySymbol", data.settings.currencySymbol)
            put("currencyCode", data.settings.currencyCode)
            put("invoicePrefix", data.settings.invoicePrefix)
            put("invoiceFooter", data.settings.invoiceFooter)
            put("termsAndConditions", data.settings.termsAndConditions)
            put("taxPercentage", data.settings.taxPercentage)
        }
        root.put("settings", s)

        // 2. Customers
        val custArr = JSONArray()
        data.customers.forEach {
            custArr.put(JSONObject().apply {
                put("customerId", it.customerId)
                put("name", it.name)
                put("idType", it.idType)
                put("idNumber", if (data.settings.backupSensitiveData) it.idNumber else "")
                put("phone", it.phone)
                put("whatsapp", it.whatsapp)
                put("address", it.address)
                put("notes", it.notes)
                put("idPhotoUri", if (data.settings.backupSensitiveData) it.idPhotoUri else "")
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
                put("lastVisit", it.lastVisit)
                put("totalVisits", it.totalVisits)
            })
        }
        root.put("customers", custArr)

        // 3. Rooms
        val roomArr = JSONArray()
        data.rooms.forEach {
            roomArr.put(JSONObject().apply {
                put("roomNumber", it.roomNumber)
                put("roomType", it.roomType)
                put("floor", it.floor)
                put("rate", it.rate)
                put("status", it.status)
                put("notes", it.notes)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("rooms", roomArr)

        // 4. Reservations
        val resArr = JSONArray()
        data.reservations.forEach {
            resArr.put(JSONObject().apply {
                put("reservationId", it.reservationId)
                put("customerId", it.customerId)
                put("customerName", it.customerName)
                put("phone", it.phone)
                put("roomNumber", it.roomNumber)
                put("roomType", it.roomType)
                put("checkInDate", it.checkInDate)
                put("checkInTime", it.checkInTime)
                put("checkOutDate", it.checkOutDate)
                put("checkOutTime", it.checkOutTime)
                put("checkInTimestamp", it.checkInTimestamp)
                put("checkOutTimestamp", it.checkOutTimestamp)
                put("guests", it.guests)
                put("advance", it.advance)
                put("notes", it.notes)
                put("status", it.status)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("reservations", resArr)

        // 5. Bookings
        val bkArr = JSONArray()
        data.bookings.forEach {
            bkArr.put(JSONObject().apply {
                put("bookingId", it.bookingId)
                put("customerId", it.customerId)
                put("customerName", it.customerName)
                put("idType", it.idType)
                put("idNumber", if (data.settings.backupSensitiveData) it.idNumber else "")
                put("contactNumber", it.contactNumber)
                put("whatsappNumber", it.whatsappNumber)
                put("checkInDate", it.checkInDate)
                put("checkInTime", it.checkInTime)
                put("checkOutDate", it.checkOutDate)
                put("checkOutTime", it.checkOutTime)
                put("checkInTimestamp", it.checkInTimestamp)
                put("checkOutTimestamp", it.checkOutTimestamp)
                put("numberOfGuests", it.numberOfGuests)
                put("roomNumber", it.roomNumber)
                put("roomType", it.roomType)
                put("roomCharges", it.roomCharges)
                put("advance", it.advance)
                put("paid", it.paid)
                put("discount", it.discount)
                put("mineralWater", it.mineralWater)
                put("ghavaneChatney", it.ghavaneChatney)
                put("tea", it.tea)
                put("kandePohe", it.kandePohe)
                put("otherCharges", it.otherCharges)
                put("notes", it.notes)
                put("idPhotoUri", if (data.settings.backupSensitiveData) it.idPhotoUri else "")
                put("status", it.status)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("bookings", bkArr)

        // 5b. Booking Guests
        val guestArr = JSONArray()
        data.bookingGuests.forEach {
            guestArr.put(JSONObject().apply {
                put("guestId", it.guestId)
                put("bookingId", it.bookingId)
                put("sequence", it.sequence)
                put("name", it.name)
                put("idType", it.idType)
                put("idNumber", if (data.settings.backupSensitiveData) it.idNumber else "")
                put("phone", it.phone)
                put("whatsapp", it.whatsapp)
                if (data.settings.backupSensitiveData) {
                    put("idPhotoUri", it.idPhotoUri)
                } else {
                    put("idPhotoUri", "")
                }
                put("vehicleNumbers", it.vehicleNumbers)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("bookingGuests", guestArr)

        // 6. Invoices & Items
        val invArr = JSONArray()
        data.invoices.forEach {
            invArr.put(JSONObject().apply {
                put("invoiceNumber", it.invoiceNumber)
                put("bookingId", it.bookingId)
                put("invoiceDate", it.invoiceDate)
                put("customerId", it.customerId)
                put("customerName", it.customerName)
                put("customerContact", it.customerContact)
                put("roomNumber", it.roomNumber)
                put("roomType", it.roomType)
                put("checkInDate", it.checkInDate)
                put("checkOutDate", it.checkOutDate)
                put("guests", it.guests)
                put("roomCharges", it.roomCharges)
                put("foodCharges", it.foodCharges)
                put("otherCharges", it.otherCharges)
                put("discount", it.discount)
                put("tax", it.tax)
                put("grandTotal", it.grandTotal)
                put("advance", it.advance)
                put("paid", it.paid)
                put("pending", it.pending)
                put("paymentMethod", it.paymentMethod)
                put("paymentStatus", it.paymentStatus)
                put("notes", it.notes)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("invoices", invArr)

        val invItemArr = JSONArray()
        data.invoiceItems.forEach {
            invItemArr.put(JSONObject().apply {
                put("invoiceNumber", it.invoiceNumber)
                put("description", it.description)
                put("quantity", it.quantity)
                put("unitPrice", it.unitPrice)
                put("totalPrice", it.totalPrice)
            })
        }
        root.put("invoiceItems", invItemArr)

        // 7. Payments
        val payArr = JSONArray()
        data.payments.forEach {
            payArr.put(JSONObject().apply {
                put("paymentId", it.paymentId)
                put("bookingId", it.bookingId)
                put("invoiceId", it.invoiceId)
                put("customerId", it.customerId)
                put("customerName", it.customerName)
                put("amount", it.amount)
                put("date", it.date)
                put("paymentMethod", it.paymentMethod)
                put("referenceNumber", it.referenceNumber)
                put("notes", it.notes)
                put("createdAt", it.createdAt)
            })
        }
        root.put("payments", payArr)

        // 8. Food Items & Orders
        val foodArr = JSONArray()
        data.foodItems.forEach {
            foodArr.put(JSONObject().apply {
                put("name", it.name)
                put("category", it.category)
                put("price", it.price)
                put("description", it.description)
                put("isActive", it.isActive)
            })
        }
        root.put("foodItems", foodArr)

        val foodOrderArr = JSONArray()
        data.foodOrders.forEach {
            foodOrderArr.put(JSONObject().apply {
                put("bookingId", it.bookingId)
                put("roomNumber", it.roomNumber)
                put("foodName", it.foodName)
                put("quantity", it.quantity)
                put("price", it.price)
                put("total", it.total)
                put("orderTimestamp", it.orderTimestamp)
                put("notes", it.notes)
            })
        }
        root.put("foodOrders", foodOrderArr)

        // 9. Expenses
        val expArr = JSONArray()
        data.expenses.forEach {
            expArr.put(JSONObject().apply {
                put("expenseId", it.expenseId)
                put("date", it.date)
                put("category", it.category)
                put("description", it.description)
                put("amount", it.amount)
                put("paymentMethod", it.paymentMethod)
                put("notes", it.notes)
                put("createdAt", it.createdAt)
            })
        }
        root.put("expenses", expArr)

        // 10. App Sections
        val secArr = JSONArray()
        data.appSections.forEach {
            secArr.put(JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("iconName", it.iconName)
                put("orderIndex", it.orderIndex)
                put("isEnabled", it.isEnabled)
                put("isCore", it.isCore)
            })
        }
        root.put("appSections", secArr)

        return root.toString(2)
    }

    fun parseJsonToData(jsonString: String): FullDatabaseExport {
        val root = JSONObject(jsonString)

        // 1. Settings
        val sObj = root.optJSONObject("settings") ?: JSONObject()
        val settings = HotelSettings(
            id = 1,
            hotelName = sObj.optString("hotelName", "HOTEL BILLING SYSTEM"),
            address = sObj.optString("address", ""),
            phone = sObj.optString("phone", ""),
            whatsapp = sObj.optString("whatsapp", ""),
            email = sObj.optString("email", ""),
            gstNumber = sObj.optString("gstNumber", ""),
            logoUri = sObj.optString("logoUri", ""),
            slogan = sObj.optString("slogan", ""),
            upiId = sObj.optString("upiId", ""),
            upiPayeeName = sObj.optString("upiPayeeName", ""),
            manualQrUri = sObj.optString("manualQrUri", ""),
            localBackupEnabled = sObj.optBoolean("localBackupEnabled", true),
            driveBackupEnabled = sObj.optBoolean("driveBackupEnabled", false),
            driveFolderUri = sObj.optString("driveFolderUri", ""),
            backupSensitiveData = sObj.optBoolean("backupSensitiveData", true),
            currencySymbol = sObj.optString("currencySymbol", "₹"),
            currencyCode = sObj.optString("currencyCode", "INR"),
            invoicePrefix = sObj.optString("invoicePrefix", "INV-"),
            invoiceFooter = sObj.optString("invoiceFooter", "Thank you!"),
            termsAndConditions = sObj.optString("termsAndConditions", ""),
            taxPercentage = sObj.optDouble("taxPercentage", 0.0)
        )

        // 2. Customers
        val customers = mutableListOf<Customer>()
        val custArr = root.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until custArr.length()) {
            val o = custArr.getJSONObject(i)
            customers.add(
                Customer(
                    customerId = o.getString("customerId"),
                    name = o.getString("name"),
                    idType = o.optString("idType", "Aadhaar Card"),
                    idNumber = o.optString("idNumber", ""),
                    phone = o.optString("phone", ""),
                    whatsapp = o.optString("whatsapp", ""),
                    address = o.optString("address", ""),
                    notes = o.optString("notes", ""),
                    idPhotoUri = o.optString("idPhotoUri", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    lastVisit = o.optLong("lastVisit", System.currentTimeMillis()),
                    totalVisits = o.optInt("totalVisits", 1)
                )
            )
        }

        // 3. Rooms
        val rooms = mutableListOf<RoomEntity>()
        val roomArr = root.optJSONArray("rooms") ?: JSONArray()
        for (i in 0 until roomArr.length()) {
            val o = roomArr.getJSONObject(i)
            rooms.add(
                RoomEntity(
                    roomNumber = o.getString("roomNumber"),
                    roomType = o.getString("roomType"),
                    floor = o.optString("floor", "1st Floor"),
                    rate = o.optDouble("rate", 0.0),
                    status = o.optString("status", "AVAILABLE"),
                    notes = o.optString("notes", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 4. Reservations
        val reservations = mutableListOf<Reservation>()
        val resArr = root.optJSONArray("reservations") ?: JSONArray()
        for (i in 0 until resArr.length()) {
            val o = resArr.getJSONObject(i)
            reservations.add(
                Reservation(
                    reservationId = o.getString("reservationId"),
                    customerId = o.optString("customerId", ""),
                    customerName = o.getString("customerName"),
                    phone = o.optString("phone", ""),
                    roomNumber = o.getString("roomNumber"),
                    roomType = o.getString("roomType"),
                    checkInDate = o.getString("checkInDate"),
                    checkInTime = o.optString("checkInTime", "12:00 PM"),
                    checkOutDate = o.getString("checkOutDate"),
                    checkOutTime = o.optString("checkOutTime", "11:00 AM"),
                    checkInTimestamp = o.optLong("checkInTimestamp", System.currentTimeMillis()),
                    checkOutTimestamp = o.optLong("checkOutTimestamp", System.currentTimeMillis() + 86400000L),
                    guests = o.optInt("guests", 1),
                    advance = o.optDouble("advance", 0.0),
                    notes = o.optString("notes", ""),
                    status = o.optString("status", "PENDING"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 5. Bookings
        val bookings = mutableListOf<Booking>()
        val bkArr = root.optJSONArray("bookings") ?: JSONArray()
        for (i in 0 until bkArr.length()) {
            val o = bkArr.getJSONObject(i)
            bookings.add(
                Booking(
                    bookingId = o.getString("bookingId"),
                    customerId = o.optString("customerId", ""),
                    customerName = o.getString("customerName"),
                    idType = o.optString("idType", "Aadhaar Card"),
                    idNumber = o.optString("idNumber", ""),
                    contactNumber = o.optString("contactNumber", ""),
                    whatsappNumber = o.optString("whatsappNumber", ""),
                    checkInDate = o.getString("checkInDate"),
                    checkInTime = o.optString("checkInTime", "12:00 PM"),
                    checkOutDate = o.getString("checkOutDate"),
                    checkOutTime = o.optString("checkOutTime", "11:00 AM"),
                    checkInTimestamp = o.optLong("checkInTimestamp", System.currentTimeMillis()),
                    checkOutTimestamp = o.optLong("checkOutTimestamp", System.currentTimeMillis() + 86400000L),
                    numberOfGuests = o.optInt("numberOfGuests", 1),
                    roomNumber = o.getString("roomNumber"),
                    roomType = o.getString("roomType"),
                    roomCharges = o.optDouble("roomCharges", 0.0),
                    advance = o.optDouble("advance", 0.0),
                    paid = o.optDouble("paid", 0.0),
                    discount = o.optDouble("discount", 0.0),
                    mineralWater = o.optDouble("mineralWater", 0.0),
                    ghavaneChatney = o.optDouble("ghavaneChatney", 0.0),
                    tea = o.optDouble("tea", 0.0),
                    kandePohe = o.optDouble("kandePohe", 0.0),
                    otherCharges = o.optDouble("otherCharges", 0.0),
                    notes = o.optString("notes", ""),
                    idPhotoUri = o.optString("idPhotoUri", ""),
                    status = o.optString("status", "ACTIVE"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 5b. Booking Guests
        val bookingGuests = mutableListOf<BookingGuest>()
        val guestArr = root.optJSONArray("bookingGuests") ?: JSONArray()
        for (i in 0 until guestArr.length()) {
            val o = guestArr.getJSONObject(i)
            bookingGuests.add(
                BookingGuest(
                    guestId = o.getString("guestId"),
                    bookingId = o.getString("bookingId"),
                    sequence = o.optInt("sequence", i + 1),
                    name = o.optString("name", ""),
                    idType = o.optString("idType", "Aadhaar Card"),
                    idNumber = o.optString("idNumber", ""),
                    phone = o.optString("phone", ""),
                    whatsapp = o.optString("whatsapp", ""),
                    idPhotoUri = o.optString("idPhotoUri", ""),
                    vehicleNumbers = o.optString("vehicleNumbers", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 6. Invoices
        val invoices = mutableListOf<Invoice>()
        val invArr = root.optJSONArray("invoices") ?: JSONArray()
        for (i in 0 until invArr.length()) {
            val o = invArr.getJSONObject(i)
            invoices.add(
                Invoice(
                    invoiceNumber = o.getString("invoiceNumber"),
                    bookingId = o.getString("bookingId"),
                    invoiceDate = o.optLong("invoiceDate", System.currentTimeMillis()),
                    customerId = o.optString("customerId", ""),
                    customerName = o.getString("customerName"),
                    customerContact = o.optString("customerContact", ""),
                    roomNumber = o.getString("roomNumber"),
                    roomType = o.getString("roomType"),
                    checkInDate = o.getString("checkInDate"),
                    checkOutDate = o.getString("checkOutDate"),
                    guests = o.optInt("guests", 1),
                    roomCharges = o.optDouble("roomCharges", 0.0),
                    foodCharges = o.optDouble("foodCharges", 0.0),
                    otherCharges = o.optDouble("otherCharges", 0.0),
                    discount = o.optDouble("discount", 0.0),
                    tax = o.optDouble("tax", 0.0),
                    grandTotal = o.optDouble("grandTotal", 0.0),
                    advance = o.optDouble("advance", 0.0),
                    paid = o.optDouble("paid", 0.0),
                    pending = o.optDouble("pending", 0.0),
                    paymentMethod = o.optString("paymentMethod", "Cash"),
                    paymentStatus = o.optString("paymentStatus", "PENDING"),
                    notes = o.optString("notes", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val invoiceItems = mutableListOf<InvoiceItem>()
        val invItemArr = root.optJSONArray("invoiceItems") ?: JSONArray()
        for (i in 0 until invItemArr.length()) {
            val o = invItemArr.getJSONObject(i)
            invoiceItems.add(
                InvoiceItem(
                    invoiceNumber = o.getString("invoiceNumber"),
                    description = o.getString("description"),
                    quantity = o.optInt("quantity", 1),
                    unitPrice = o.optDouble("unitPrice", 0.0),
                    totalPrice = o.optDouble("totalPrice", 0.0)
                )
            )
        }

        // 7. Payments
        val payments = mutableListOf<Payment>()
        val payArr = root.optJSONArray("payments") ?: JSONArray()
        for (i in 0 until payArr.length()) {
            val o = payArr.getJSONObject(i)
            payments.add(
                Payment(
                    paymentId = o.getString("paymentId"),
                    bookingId = o.optString("bookingId", ""),
                    invoiceId = o.optString("invoiceId", ""),
                    customerId = o.optString("customerId", ""),
                    customerName = o.optString("customerName", ""),
                    amount = o.getDouble("amount"),
                    date = o.optLong("date", System.currentTimeMillis()),
                    paymentMethod = o.optString("paymentMethod", "Cash"),
                    referenceNumber = o.optString("referenceNumber", ""),
                    notes = o.optString("notes", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // 8. Food Items & Orders
        val foodItems = mutableListOf<FoodItem>()
        val foodArr = root.optJSONArray("foodItems") ?: JSONArray()
        for (i in 0 until foodArr.length()) {
            val o = foodArr.getJSONObject(i)
            foodItems.add(
                FoodItem(
                    name = o.getString("name"),
                    category = o.optString("category", "Breakfast"),
                    price = o.optDouble("price", 0.0),
                    description = o.optString("description", ""),
                    isActive = o.optBoolean("isActive", true)
                )
            )
        }

        val foodOrders = mutableListOf<FoodOrder>()
        val orderArr = root.optJSONArray("foodOrders") ?: JSONArray()
        for (i in 0 until orderArr.length()) {
            val o = orderArr.getJSONObject(i)
            foodOrders.add(
                FoodOrder(
                    bookingId = o.getString("bookingId"),
                    roomNumber = o.getString("roomNumber"),
                    foodName = o.getString("foodName"),
                    quantity = o.optInt("quantity", 1),
                    price = o.optDouble("price", 0.0),
                    total = o.optDouble("total", 0.0),
                    orderTimestamp = o.optLong("orderTimestamp", System.currentTimeMillis()),
                    notes = o.optString("notes", "")
                )
            )
        }

        // 9. Expenses
        val expenses = mutableListOf<Expense>()
        val expArr = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expArr.length()) {
            val o = expArr.getJSONObject(i)
            expenses.add(
                Expense(
                    expenseId = o.getString("expenseId"),
                    date = o.optLong("date", System.currentTimeMillis()),
                    category = o.getString("category"),
                    description = o.getString("description"),
                    amount = o.getDouble("amount"),
                    paymentMethod = o.optString("paymentMethod", "Cash"),
                    notes = o.optString("notes", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // 10. App Sections
        val appSections = mutableListOf<AppSection>()
        val secArr = root.optJSONArray("appSections") ?: JSONArray()
        for (i in 0 until secArr.length()) {
            val o = secArr.getJSONObject(i)
            appSections.add(
                AppSection(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    iconName = o.optString("iconName", "Settings"),
                    orderIndex = o.optInt("orderIndex", i),
                    isEnabled = o.optBoolean("isEnabled", true),
                    isCore = o.optBoolean("isCore", true)
                )
            )
        }

        return FullDatabaseExport(
            settings = settings,
            customers = customers,
            rooms = rooms,
            reservations = reservations,
            bookings = bookings,
            bookingGuests = bookingGuests,
            invoices = invoices,
            invoiceItems = invoiceItems,
            payments = payments,
            foodItems = foodItems,
            foodOrders = foodOrders,
            expenses = expenses,
            appSections = appSections,
            backups = emptyList()
        )
    }

    fun saveBackupToFile(context: Context, jsonString: String): File {
        val backupsDir = File(context.filesDir, "backups").apply { mkdirs() }
        val backupFile = File(backupsDir, "hotel_backup_${System.currentTimeMillis()}.json")
        FileWriter(backupFile).use { it.write(jsonString) }
        return backupFile
    }

    fun readBackupFromFile(file: File): String {
        return FileReader(file).use { it.readText() }
    }

    fun writeBackupToTree(context: Context, treeUri: Uri, jsonString: String): Boolean {
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            val name = "hotel_backup_${System.currentTimeMillis()}.json"
            val file = tree.createFile("application/json", name) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { output ->
                output.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareBackupFile(context: Context, backupFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hotel Billing Backup - ${backupFile.name}")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Share Database Backup"))
    }
}
