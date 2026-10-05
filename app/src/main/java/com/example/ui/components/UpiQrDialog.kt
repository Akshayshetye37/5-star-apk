package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.HotelSettings
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.util.CurrencyFormatter
import com.example.util.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream

@Composable
fun UpiQrDialog(
    settings: HotelSettings,
    initialAmount: Double = 0.0,
    title: String = "Scan to Pay via UPI / Google Pay",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(if (initialAmount > 0) String.format("%.2f", initialAmount) else "") }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0
    val upiId = settings.upiId.trim()
    val payeeName = settings.upiPayeeName.ifBlank { settings.hotelName }

    val upiUrl = remember(upiId, payeeName, currentAmount) {
        if (upiId.isBlank()) "" else QrCodeGenerator.buildUpiUrl(
            upiId = upiId,
            payeeName = payeeName,
            amount = if (currentAmount > 0) currentAmount else null,
            transactionNote = "Hotel Bill Payment"
        )
    }

    val qrBitmap = remember(upiUrl) {
        if (upiUrl.isBlank()) null else QrCodeGenerator.generateBitmap(upiUrl, 360)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = HotelNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = settings.hotelName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelNavy,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Payee: $payeeName | UPI: $upiId",
                    fontSize = 12.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Image
                if (qrBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI QR Code",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                } else if (settings.manualQrUri.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = settings.manualQrUri,
                            contentDescription = "Hotel payment QR",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Text("Fixed hotel QR", fontSize = 11.sp, color = Slate500)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Set your hotel UPI ID in Settings before generating an amount QR.",
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFF9A3412),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount input field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${settings.currencySymbol})") },
                    placeholder = { Text("Enter amount or leave empty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upi_qr_amount_input"),
                    singleLine = true
                )

                if (currentAmount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total to Pay: ${CurrencyFormatter.format(currentAmount, settings.currencySymbol)}",
                        fontWeight = FontWeight.Bold,
                        color = HotelGold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Compatible with Google Pay, PhonePe, Paytm & any BHIM UPI app.",
                    fontSize = 11.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    qrBitmap?.let { shareQrCode(context, it, payeeName, currentAmount, settings.currencySymbol) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                modifier = Modifier.testTag("share_qr_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share QR")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun shareQrCode(
    context: Context,
    bitmap: Bitmap,
    payeeName: String,
    amount: Double,
    symbol: String
) {
    try {
        val file = File(context.cacheDir, "upi_qr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            val amtText = if (amount > 0) " of $symbol $amount" else ""
            putExtra(Intent.EXTRA_TEXT, "Scan to pay $payeeName$amtText via UPI.")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share UPI QR"))
    } catch (e: Exception) {
        // Ignored
    }
}
