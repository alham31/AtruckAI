package com.example.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.text.SimpleDateFormat
import java.util.Date
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.ExtractedItem
import com.example.data.api.ExtractedReceipt
import com.example.data.model.Receipt
import com.example.data.model.ReceiptOrderItem
import com.example.viewmodel.ReceiptViewModel
import com.example.viewmodel.ScanState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.NumberFormat
import java.util.Locale

// Number formatter for rupiah currency
fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    // Remove trailing decimal if integer
    var res = format.format(amount)
        .replace("Rp", "Rp ")
        .replace(",00", "")
    return res
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ReceiptViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receipts by viewModel.receiptsState.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val apiKey by viewModel.userApiKey.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var selectedRecapMode by remember { mutableIntStateOf(0) } // 0 = Harian, 1 = Mingguan, 2 = Bulanan
    var expandedReceiptId by remember { mutableStateOf<Int?>(null) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    // Dialog for review scan result
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewReceiptData by remember { mutableStateOf<ExtractedReceipt?>(null) }

    // Launch Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.performLiveOcr(bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Monitor scanning state
    LaunchedEffect(scanState) {
        when (val state = scanState) {
            is ScanState.Success -> {
                reviewReceiptData = state.receipt
                showReviewDialog = true
            }
            is ScanState.Error -> {
                Toast.makeText(context, "OCR Info: ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NotaScan AI OCR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                actions = {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("api_key_settings_btn")
                    ) {
                        val isCustomKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Pengaturan API Key",
                            tint = if (isCustomKey) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.exportAndShare(context) },
                        modifier = Modifier.testTag("excel_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor ke Excel",
                            tint = Color(0xFF2E7D32) // Microsoft Excel green
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Add receipt manually Button
                OutlinedButton(
                    onClick = { showManualAddDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("add_manual_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nota Manual", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                
                // OCR Scan Button
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("scan_receipt_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pilih Gambar Struk"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Foto Struk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = Arrangement.spacedBy(16.dp).let {
                androidx.compose.foundation.layout.PaddingValues(16.dp)
            }
        ) {
            // Loading Overlay Area
            if (scanState is ScanState.Scanning) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Menghubungi AI Gemini untuk Transkripsi OCR...",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Quick Info Warning about API Key
            val defaultKey = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY"
            if (defaultKey) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Menggunakan Mode Simulator (Kunci API Kosong)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Gunakan resep nota contoh cepat atau klik ikon Kunci di atas untuk memasang opsi Gemini API Anda sendiri.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. Dashboard Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayVal = viewModel.getTodayTotal(receipts)
                    val weekVal = viewModel.getWeeklyTotal(receipts)
                    val monthVal = viewModel.getMonthlyTotal(receipts)

                    val itemsData = listOf(
                        Triple("Hari Ini", todayVal, MaterialTheme.colorScheme.primary),
                        Triple("Minggu Ini", weekVal, MaterialTheme.colorScheme.secondary),
                        Triple("Bulan Ini", monthVal, MaterialTheme.colorScheme.tertiary)
                    )

                    itemsData.forEach { (label, value, color) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = color.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatRupiah(value),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. Bar Chart Section with Recap Options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Chart Header with Switchers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grafik Pengeluaran",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(2.dp)
                            ) {
                                listOf("Hari", "Minggu", "Bulan").forEachIndexed { index, label ->
                                    val isSelected = selectedRecapMode == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                            .clickable { selectedRecapMode = index }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Retrieve active data list
                        val chartList = when (selectedRecapMode) {
                            0 -> viewModel.getDailyRecapList(receipts)
                            1 -> viewModel.getWeeklyRecapList(receipts)
                            else -> viewModel.getMonthlyRecapList(receipts)
                        }

                        // Render Bar Chart
                        if (chartList.isEmpty() || chartList.all { it.second == 0.0 }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Belum ada transaksi di periode ini",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val maxValue = chartList.maxOfOrNull { it.second } ?: 1.0
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                chartList.forEach { (label, value) ->
                                    // Scale bar size dynamically
                                    val barHeightRatio = if (maxValue == 0.0) 0f else (value / maxValue).toFloat()
                                    val heightAnimate by animateFloatAsState(targetValue = barHeightRatio)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (value > 0.0) {
                                            Text(
                                                text = if (value >= 1000000) "${String.format("%.1f", value/1000000)}jt" else if (value >= 1000) "${(value/1000).toInt()}rb" else value.toInt().toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                            )
                                        } else {
                                            Text("-", fontSize = 9.sp, color = Color.Transparent)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Bar view
                                        Box(
                                            modifier = Modifier
                                                .width(18.dp)
                                                .fillMaxHeight(0.75f * heightAnimate.coerceIn(0.01f, 1f))
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                        )
                                                    )
                                                )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Playground / Presets Segment (Extremely helpful for testing!)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💡 Uji Coba Cepat (Simulator Nota)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sebagai pengganti foto struk, klik tombol di bawah untuk menyimulasikan transkripsi AI secara otomatis.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presetsList = listOf(
                                "Indomaret" to 0,
                                "Starbucks" to 1,
                                "TB Makmur" to 2
                            )
                            
                            presetsList.forEach { (name, idx) ->
                                OutlinedButton(
                                    onClick = { viewModel.scanPresetReceipt(idx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("preset_${idx}_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Receipts History List Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Nota Transaksi (${receipts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (receipts.isNotEmpty()) {
                        Text(
                            text = "Sentuh untuk detail",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Receipts List Items
            if (receipts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Nota Belanja Tersimpan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih foto struk di galeri Anda atau klik salah satu nota uji coba di atas untuk memulai transkripsi OCR Otomatis.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                items(receipts) { receipt ->
                    val isExpanded = expandedReceiptId == receipt.id
                    ReceiptCard(
                        receipt = receipt,
                        isExpanded = isExpanded,
                        onExpandToggle = {
                            expandedReceiptId = if (isExpanded) null else receipt.id
                        },
                        onDelete = {
                            viewModel.deleteReceipt(receipt)
                            Toast.makeText(context, "Nota dihapus", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // --- DIALOGS SECTION ---

    // 1. Review & Edit OCR Result Dialog
    if (showReviewDialog && reviewReceiptData != null) {
        var merchant by remember { mutableStateOf(reviewReceiptData!!.merchantName) }
        var trxDate by remember { mutableStateOf(reviewReceiptData!!.transactionDate) }
        var totalAmountStr by remember { mutableStateOf(reviewReceiptData!!.totalAmount.toInt().toString()) }
        
        // Use mutableStateListOf to allow adding/editing/deleting items dynamically!
        val itemsList = remember { 
            mutableStateListOf<ExtractedItem>().apply {
                addAll(reviewReceiptData!!.items)
            }
        }

        Dialog(
            onDismissRequest = { 
                showReviewDialog = false
                viewModel.clearScanState()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verifikasi Hasil OCR AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { 
                            showReviewDialog = false
                            viewModel.clearScanState()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Silakan verifikasi atau ubah detail transaksi yang dibaca oleh sistem otomatis di bawah ini sebelum menyimpan.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Merchant & Date Fields
                        item {
                            OutlinedTextField(
                                value = merchant,
                                onValueChange = { merchant = it },
                                label = { Text("Nama Toko / Merchant") },
                                leadingIcon = { Icon(Icons.Default.Store, null) },
                                modifier = Modifier.fillMaxWidth().testTag("review_merchant_input")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = trxDate,
                                onValueChange = { trxDate = it },
                                label = { Text("Tanggal Transaksi (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                                modifier = Modifier.fillMaxWidth().testTag("review_date_input")
                            )
                        }

                        // Items Table title
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daftar Barang / Jasa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = {
                                        itemsList.add(ExtractedItem("Barang Baru", 1.0, 0.0, 0.0))
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah", fontSize = 11.sp)
                                }
                            }
                        }

                        // Render items inside scrollable
                        items(itemsList.size) { index ->
                            val item = itemsList[index]
                            var itemName by remember(item) { mutableStateOf(item.name) }
                            var itemQty by remember(item) { mutableStateOf(item.quantity.toString()) }
                            var itemPrice by remember(item) { mutableStateOf(item.price.toInt().toString()) }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Barang #${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { itemsList.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = itemName,
                                        onValueChange = { 
                                            itemName = it
                                            itemsList[index] = itemsList[index].copy(name = it)
                                        },
                                        label = { Text("Nama Barang") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = itemQty,
                                            onValueChange = {
                                                itemQty = it
                                                val qtyDouble = it.toDoubleOrNull() ?: 1.0
                                                val priceDouble = itemPrice.toDoubleOrNull() ?: 0.0
                                                itemsList[index] = itemsList[index].copy(
                                                    quantity = qtyDouble,
                                                    totalPrice = qtyDouble * priceDouble
                                                )
                                            },
                                            label = { Text("Kuantitas") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = itemPrice,
                                            onValueChange = {
                                                itemPrice = it
                                                val priceDouble = it.toDoubleOrNull() ?: 0.0
                                                val qtyDouble = itemQty.toDoubleOrNull() ?: 1.0
                                                itemsList[index] = itemsList[index].copy(
                                                    price = priceDouble,
                                                    totalPrice = qtyDouble * priceDouble
                                                )
                                            },
                                            label = { Text("Harga Satuan") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // Total bill
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Recalculate automatic sum
                            val automaticTotal = itemsList.sumOf { it.totalPrice }
                            LaunchedEffect(automaticTotal) {
                                totalAmountStr = automaticTotal.toInt().toString()
                            }
                            
                            OutlinedTextField(
                                value = totalAmountStr,
                                onValueChange = { totalAmountStr = it },
                                label = { Text("Total Pembayaran Akhir") },
                                prefix = { Text("Rp ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("review_total_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                showReviewDialog = false
                                viewModel.clearScanState()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                val totalVal = totalAmountStr.toDoubleOrNull() ?: 0.0
                                val receiptItems = itemsList.map {
                                    ReceiptOrderItem(
                                        name = it.name,
                                        quantity = it.quantity,
                                        price = it.price,
                                        totalPrice = it.totalPrice
                                    )
                                }
                                viewModel.insertReceipt(
                                    merchantName = merchant,
                                    transactionDate = trxDate,
                                    totalAmount = totalVal,
                                    items = receiptItems
                                )
                                showReviewDialog = false
                                viewModel.clearScanState()
                                Toast.makeText(context, "Nota berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.5f).testTag("save_review_receipt_btn")
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Nota")
                        }
                    }
                }
            }
        }
    }

    // 2. Manual Add Receipt Dialog
    if (showManualAddDialog) {
        var merchant by remember { mutableStateOf("") }
        var totalStr by remember { mutableStateOf("") }
        val dateToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var trxDate by remember { mutableStateOf(dateToday) }
        
        // Single manual item placeholder
        var itemName by remember { mutableStateOf("") }
        var qtyStr by remember { mutableStateOf("1") }
        var unitPriceStr by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showManualAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tambah Nota Manajerial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Nama Toko/Merchant") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_merchant_input")
                    )

                    OutlinedTextField(
                        value = trxDate,
                        onValueChange = { trxDate = it },
                        label = { Text("Tanggal Transaksi (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_date_input")
                    )

                    Text(
                        text = "Item Utama (Opsional)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Nama Barang") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitPriceStr,
                            onValueChange = {
                                unitPriceStr = it
                                // Auto set total if total is empty
                                val qty = qtyStr.toDoubleOrNull() ?: 1.0
                                val price = it.toDoubleOrNull() ?: 0.0
                                totalStr = (qty * price).toInt().toString()
                            },
                            label = { Text("Harga") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    OutlinedTextField(
                        value = totalStr,
                        onValueChange = { totalStr = it },
                        label = { Text("Total Tagihan / Pembayaran") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("manual_total_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showManualAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (merchant.isEmpty() || totalStr.isEmpty()) {
                                    Toast.makeText(context, "Sebutkan nama Toko & Total!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val total = totalStr.toDoubleOrNull() ?: 0.0
                                val itemsList = mutableListOf<ReceiptOrderItem>()
                                if (itemName.isNotEmpty()) {
                                    val qty = qtyStr.toDoubleOrNull() ?: 1.0
                                    val price = unitPriceStr.toDoubleOrNull() ?: total
                                    itemsList.add(ReceiptOrderItem(itemName, qty, price, qty * price))
                                }
                                viewModel.insertReceipt(
                                    merchantName = merchant,
                                    transactionDate = trxDate,
                                    totalAmount = total,
                                    items = itemsList
                                )
                                showManualAddDialog = false
                                Toast.makeText(context, "Nota manual berhasil disimpan", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.5f).testTag("save_manual_receipt_btn")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }

    // 3. API Key Settings Dialog
    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(apiKey) }

        Dialog(onDismissRequest = { showApiKeyDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pengaturan Gemini API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Aplikasi menggunakan model gemini-3.5-flash untuk melakukan OCR struk asli secara langsung melalui internet.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Kunci API Gemini") },
                        placeholder = { Text("Masukkan API Key Anda...") },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )

                    Text(
                        text = "Catatan: Jika kunci kosong atau bawaan, NotaScan akan otomatis menggunakan algoritma simulator cerdas yang cepat agar Anda tetap dapat menguji fungsionalitas aplikasi di emulator.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.updateApiKey("")
                                showApiKeyDialog = false 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                        Button(
                            onClick = {
                                viewModel.updateApiKey(keyInput.trim())
                                showApiKeyDialog = false
                                Toast.makeText(context, "API Key diperbarui!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.5f).testTag("save_api_key_btn")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

// Custom receipt card that can be expanded to display items
@Composable
fun ReceiptCard(
    receipt: Receipt,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val listMyType = Types.newParameterizedType(List::class.java, ReceiptOrderItem::class.java)
    val adapter = moshi.adapter<List<ReceiptOrderItem>>(listMyType)

    val items = remember(receipt.itemsJson) {
        try {
            adapter.fromJson(receipt.itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .testTag("receipt_card_${receipt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = receipt.merchantName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = receipt.transactionDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiah(receipt.totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${items.size} item",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded Area showing full receipt order lists
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    if (items.isEmpty()) {
                        Text(
                            text = "Tidak ada detail barang untuk nota ini.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${if(item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} x ${formatRupiah(item.price)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatRupiah(item.totalPrice),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_receipt_btn_${receipt.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Nota",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
