package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Receipt
import com.example.data.model.ReceiptOrderItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listMyType = Types.newParameterizedType(List::class.java, ReceiptOrderItem::class.java)
    private val adapter = moshi.adapter<List<ReceiptOrderItem>>(listMyType)

    fun exportToExcel(context: Context, receipts: List<Receipt>): Uri? {
        try {
            val cachePath = File(context.cacheDir, "shared_files")
            cachePath.mkdirs()
            val file = File(cachePath, "Rekap_Pengeluaran_NotaScan.csv")
            val outputStream = FileOutputStream(file)
            
            // UTF-8 BOM to force Excel to open with proper UTF-8 encoding
            outputStream.write(0xEF)
            outputStream.write(0xBB)
            outputStream.write(0xBF)
            
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            
            // Explicit delimiter for Excel
            writer.write("sep=,\n")
            
            // CSV Header
            writer.write("ID Transaksi,Nama Toko/Merchant,Tanggal Transaksi,Nama Barang / Jasa,Kuantitas,Harga Satuan,Total Harga Barang,Total Nota,Waktu Scan\n")
            
            val doubleQuote = "\""
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            for (receipt in receipts) {
                val items = try {
                    adapter.fromJson(receipt.itemsJson) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                
                val merchantEscaped = receipt.merchantName.replace(doubleQuote, "\"\"")
                val scanTime = dateFormat.format(Date(receipt.createdAt))
                
                if (items.isEmpty()) {
                    writer.write("${receipt.id},\"$merchantEscaped\",${receipt.transactionDate},-,0,0.0,0.0,${receipt.totalAmount},\"$scanTime\"\n")
                } else {
                    for ((index, item) in items.withIndex()) {
                        val itemNameEscaped = item.name.replace(doubleQuote, "\"\"")
                        // Place total amount on original receipt line only so spreadsheet math matches exactly
                        val lineTotal = if (index == 0) receipt.totalAmount.toString() else "0.0"
                        
                        writer.write(
                            "${receipt.id},\"$merchantEscaped\",${receipt.transactionDate}," +
                            "\"$itemNameEscaped\",${item.quantity},${item.price},${item.totalPrice}," +
                            "$lineTotal,\"$scanTime\"\n"
                        )
                    }
                }
            }
            
            writer.flush()
            writer.close()
            outputStream.close()
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("ExcelExporter", "Error exporting to excel: ${e.message}", e)
            return null
        }
    }
    
    fun shareExcelFile(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Pengeluaran NotaScan")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Bagikan Rekap Excel (.csv)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
