package com.codingwitharul.myupialert.repository

import com.codingwitharul.myupialert.db.TransactionDao
import com.codingwitharul.myupialert.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {

    suspend fun insert(transaction: PaymentTransaction) {
        withContext(Dispatchers.IO) {
            transactionDao.insert(transaction)
        }
    }

    fun getAllTransactions(): Flow<List<PaymentTransaction>> = transactionDao.getAllTransactions()

    fun getTransactions(
        appName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<PaymentTransaction>> {
        return if (appName == "all" && startTimestamp == 0L) {
            transactionDao.getAllTransactions()
        } else if (appName == "all") {
            transactionDao.getTransactionsByDate(startTimestamp, endTimestamp)
        } else if (startTimestamp == 0L) {
            transactionDao.getTransactionsByApp(appName)
        } else {
            transactionDao.getTransactionsByAppAndDate(appName, startTimestamp, endTimestamp)
        }
    }

    suspend fun findTransactionByRef(refNo: String, sinceTimestamp: Long): PaymentTransaction? {
        return withContext(Dispatchers.IO) {
            transactionDao.findTransactionByRef(refNo, sinceTimestamp)
        }
    }

    suspend fun findRecentByAmount(amount: String, normalizedAmount: String, sinceTimestamp: Long): PaymentTransaction? {
        return withContext(Dispatchers.IO) {
            transactionDao.findRecentByAmount(amount, normalizedAmount, sinceTimestamp)
        }
    }

    suspend fun update(transaction: PaymentTransaction) {
        withContext(Dispatchers.IO) {
            transactionDao.update(transaction)
        }
    }
}
