package com.example.data.local

import com.example.data.model.Receipt
import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val receiptDao: ReceiptDao) {
    val allReceipts: Flow<List<Receipt>> = receiptDao.getAllReceipts()

    suspend fun insertReceipt(receipt: Receipt): Long {
        return receiptDao.insertReceipt(receipt)
    }

    suspend fun updateReceipt(receipt: Receipt) {
        receiptDao.updateReceipt(receipt)
    }

    suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt)
    }

    suspend fun deleteReceiptById(id: Int) {
        receiptDao.deleteReceiptById(id)
    }

    suspend fun getReceiptById(id: Int): Receipt? {
        return receiptDao.getReceiptById(id)
    }
}
