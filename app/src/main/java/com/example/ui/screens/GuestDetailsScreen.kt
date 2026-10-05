package com.example.ui.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Booking
import com.example.data.model.BookingGuest
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.GuestRoomPdfGenerator
import com.example.util.WhatsAppMessageBuilder
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GuestDetailsScreen(
    viewModel: HotelViewModel,
    booking: Booking,
    onEdit: () -> Unit,
    onAddPayment: () -> Unit,
    onGenerateInvoice: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var customer by remember { mutableStateOf<Customer?>(null) }
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var bookingHistory by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var reservationId by remember { mutableStateOf("") }
    var guests by remember { mutableStateOf<List<BookingGuest>>(emptyList()) }
    var pdfFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(booking.bookingId) {
        val c = viewModel.getCustomerForBooking(booking)
        customer = c
        payments = viewModel.repository.getPaymentsForBookingDirect(booking.bookingId)
        bookingHistory = c?.customerId?.let { viewModel.repository.getBookingsForCustomerDirect(it) } ?: listOf(booking)
        reservationId = c?.customerId?.let { viewModel.repository.getLatestReservationForGuest(it, booking.roomNumber)?.reservationId }.orEmpty()
        guests = viewModel.getGuestsForBookingDirect(booking.bookingId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Guest / Person Details", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Room ${booking.roomNumber} shows all guests. Use Person PDF for one guest or Room PDF for everyone.", color = HotelNavy, fontSize = 12.sp)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Detail("Customer Name", booking.customerName)
                    Detail("Phone", booking.contactNumber.ifBlank { customer?.phone.orEmpty() })
                    Detail("WhatsApp", booking.whatsappNumber.ifBlank { customer?.whatsapp.orEmpty() })
                    Detail("ID Type", booking.idType.ifBlank { customer?.idType.orEmpty() })
                    Detail("ID Number", booking.idNumber.ifBlank { customer?.idNumber.orEmpty() })
                    Detail("Room Number", booking.roomNumber)
                    Detail("Room Type", booking.roomType)
                    Detail("Check-in", "${booking.checkInDate} ${booking.checkInTime}")
                    Detail("Check-out", "${booking.checkOutDate} ${booking.checkOutTime}")
                    Detail("Number of Guests", booking.numberOfGuests.toString())
                    Detail("Booking ID", booking.bookingId)
                    if (reservationId.isNotBlank()) Detail("Reservation ID", reservationId)
                    Detail("Status", booking.status)
                }
            }
        }
        if (guests.isNotEmpty()) {
            item {
                Text("Guests in Room ${booking.roomNumber}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            items(guests, key = { it.guestId }) { guest ->
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Guest ${guest.sequence}${if (guest.sequence == 1) " • Main Guest" else ""}: ${guest.name}", fontWeight = FontWeight.Bold)
                        Text("Contact: ${guest.phone.ifBlank { "—" }}", fontSize = 12.sp)
                        Text("WhatsApp: ${guest.whatsapp.ifBlank { "—" }}", fontSize = 12.sp)
                        Text("ID: ${guest.idType} • ${guest.idNumber.ifBlank { "—" }}", fontSize = 12.sp)
                        if (guest.vehicleNumbers.isNotBlank()) Text("Vehicles: ${guest.vehicleNumbers.replace("\n", ", ")}", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                if (guest.phone.isNotBlank()) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(guest.phone)}")))
                            }, modifier = Modifier.weight(1f)) { Text("Call") }
                            OutlinedButton(onClick = {
                                val n = guest.whatsapp.ifBlank { guest.phone }.filter { it.isDigit() }
                                if (n.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$n")))
                            }, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val file = GuestRoomPdfGenerator.generate(
                                        context, viewModel.settings.value, booking, customer, payments, reservationId, listOf(guest)
                                    )
                                    GuestRoomPdfGenerator.share(context, file)
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Person PDF") }
                        }
                    }
                }
            }
        }

        if (booking.idPhotoUri.isNotBlank() || !customer?.idPhotoUri.isNullOrBlank()) {
            item {
                Text("ID Photo", fontWeight = FontWeight.Bold)
                AsyncImage(
                    model = booking.idPhotoUri.ifBlank { customer?.idPhotoUri.orEmpty() },
                    contentDescription = "ID Photo",
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Charges", fontWeight = FontWeight.Bold)
                    Detail("Room Charges", "${booking.roomCharges}")
                    Detail("Food Charges", "${booking.mineralWater + booking.ghavaneChatney + booking.tea + booking.kandePohe}")
                    Detail("Other Charges", "${booking.otherCharges}")
                    Detail("Discount", "${booking.discount}")
                    Detail("Grand Total", "${booking.grandTotal}")
                    Detail("Advance", "${booking.advance}")
                    Detail("Paid", "${booking.paid}")
                    Detail("Pending", "${booking.pending}")
                    Detail("Notes", booking.notes)
                }
            }
        }
        item { Text("Booking History", fontWeight = FontWeight.Bold) }
        items(bookingHistory) { b ->
            ListItem(
                headlineContent = { Text("${b.bookingId} • Room ${b.roomNumber}") },
                supportingContent = { Text("${b.checkInDate} → ${b.checkOutDate} • ${b.status}") }
            )
        }
        item { Text("Payment History", fontWeight = FontWeight.Bold) }
        items(payments) { p ->
            ListItem(headlineContent = { Text("${p.amount} • ${p.paymentMethod}") }, supportingContent = { Text(p.notes) })
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit, Modifier.weight(1f)) { Text("Edit") }
                OutlinedButton(onClick = onAddPayment, Modifier.weight(1f)) { Text("Add Payment") }
                OutlinedButton(onClick = onGenerateInvoice, Modifier.weight(1f)) { Text("Invoice") }
            }
        }
        item {
            OutlinedButton(
                onClick = { viewModel.shareInvoicePdf(context, booking) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share Invoice PDF to Main Guest") }
        }
        item {
            OutlinedButton(
                onClick = {
                    val n = booking.whatsappNumber.ifBlank { booking.contactNumber }.filter { it.isDigit() }
                    if (n.isNotBlank()) {
                        val message = WhatsAppMessageBuilder.checkIn(booking, viewModel.settings.value.hotelName)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$n?text=${Uri.encode(message)}")))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send Check-in WhatsApp") }
        }
        if (booking.pending > 0) {
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val file = GuestRoomPdfGenerator.generate(
                                context, viewModel.settings.value, booking, customer, payments, reservationId, guests
                            )
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val number = booking.whatsappNumber.ifBlank { booking.contactNumber }.filter { it.isDigit() }
                            val text = WhatsAppMessageBuilder.pending(booking, viewModel.settings.value.currencySymbol)
                            try {
                                context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    setPackage("com.whatsapp")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            } catch (_: Exception) {
                                GuestRoomPdfGenerator.share(context, file)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pending Balance PDF + Main Guest WhatsApp") }
            }
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        val file = GuestRoomPdfGenerator.generate(context, viewModel.settings.value, booking, customer, payments, reservationId, guests)
                        pdfFile = file
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate PDF") }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = pdfFile != null,
                    onClick = { pdfFile?.let { GuestRoomPdfGenerator.share(context, it) } },
                    modifier = Modifier.weight(1f)
                ) { Text("Share PDF") }
                OutlinedButton(
                    enabled = pdfFile != null,
                    onClick = { pdfFile?.let { GuestRoomPdfGenerator.open(context, it) } },
                    modifier = Modifier.weight(1f)
                ) { Text("Open PDF") }
                OutlinedButton(
                    enabled = pdfFile != null,
                    onClick = { pdfFile?.let { GuestRoomPdfGenerator.print(context, it) } },
                    modifier = Modifier.weight(1f)
                ) { Text("Print") }
            }
        }
        item {
            Text("Room PDF includes all saved guests; Person PDF includes only the selected guest.", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Text(value.ifBlank { "—" })
    }
}
