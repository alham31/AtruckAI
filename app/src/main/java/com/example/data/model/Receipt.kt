package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String,
    val transactionDate: String, // Format: YYYY-MM-DD
    val totalAmount: Double,
    val itemsJson: String, // Serialized List<ReceiptOrderItem>
    val createdAt: Long = System.currentTimeMillis()
)

data class ReceiptOrderItem(
    val name: String,
    val quantity: Double,
    val price: Double,
    val totalPrice: Double
)

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listMyType = Types.newParameterizedType(List::class.java, ReceiptOrderItem::class.java)
    private val adapter = moshi.adapter<List<ReceiptOrderItem>>(listMyType)

    @TypeConverter
    fun stringToList(value: String?): List<ReceiptOrderItem> {
        return if (value == null) emptyList() else adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun listToString(list: List<ReceiptOrderItem>?): String {
        return adapter.toJson(list ?: emptyList())
    }
}
