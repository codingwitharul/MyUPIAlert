package com.codingwitharul.myupialert.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codingwitharul.myupialert.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PaymentTransaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsByDate(startTimestamp: Long, endTimestamp: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions WHERE sender = :appName ORDER BY timestamp DESC")
    fun getTransactionsByApp(appName: String): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions WHERE sender = :appName AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsByAppAndDate(appName: String, startTimestamp: Long, endTimestamp: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions WHERE body LIKE '%' || :refNo || '%' AND timestamp >= :sinceTimestamp LIMIT 1")
    suspend fun findTransactionByRef(refNo: String, sinceTimestamp: Long): PaymentTransaction?

    @androidx.room.Update
    suspend fun update(transaction: PaymentTransaction)

    @Query("SELECT * FROM transactions WHERE (amount = :amount OR amount = :normalizedAmount) AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentByAmount(amount: String, normalizedAmount: String, sinceTimestamp: Long): PaymentTransaction?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
