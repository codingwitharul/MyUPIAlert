---
name: transaction-storage-export
description: >-
  Use this skill when modifying Room database schema, adding DAO queries, altering transaction models,
  implementing transaction filters, or working on CSV/data export.
---

# Transaction Storage & Export Skill

This skill guides the agent in working with the local SQLite persistence layer (Room processed with KSP) and transaction export features in MyUPIAlert (Kotlin edition).

## Target Files
- `app/src/main/java/com/example/upipaymentalert/db/AppDatabase.kt`: Room database class with companion object singleton instantiation.
- `app/src/main/java/com/example/upipaymentalert/db/TransactionDao.kt`: Data Access Object interface for transactions.
- `app/src/main/java/com/example/upipaymentalert/model/PaymentTransaction.kt`: Room Entity and JSON/Map serialization.
- `app/src/main/java/com/example/upipaymentalert/MainActivity.kt`: UI filters and statement export logic.
- `app/src/main/java/com/example/upipaymentalert/PaymentAdapter.kt`: RecyclerView adapter for transactions.

---

## Key Workflows

### 1. Modifying the PaymentTransaction Entity
The table is named `transactions`:
```kotlin
@Keep
@Entity(tableName = "transactions")
data class PaymentTransaction(
    @PrimaryKey
    var id: String = "",
    var amount: String? = null,
    var type: String? = null, // "credit" or "debit"
    var sender: String? = null,
    var body: String? = null,
    var timestamp: Long = 0L
)
```
If adding a new field (e.g. `upiRefNumber`):
1. Add field to `PaymentTransaction.kt`.
2. Update `toMap()`, `fromMap()`, `toJson()`, `fromJson()`.
3. Increment database version in `AppDatabase.kt`:
   ```kotlin
   @Database(entities = [PaymentTransaction::class], version = 2, exportSchema = false)
   ```
4. If preserving user data is required, define a Room `Migration` object rather than relying on `fallbackToDestructiveMigration()`.

### 2. Adding Queries to TransactionDao.kt
All Room queries in `TransactionDao` run on a background thread (e.g. coroutines or `Executors.newSingleThreadExecutor()`).
Common query patterns:
```kotlin
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: PaymentTransaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): List<PaymentTransaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsByDate(start: Long, end: Long): List<PaymentTransaction>

    @Query("SELECT * FROM transactions WHERE sender = :appName ORDER BY timestamp DESC")
    fun getTransactionsByApp(appName: String): List<PaymentTransaction>

    @Query("DELETE FROM transactions")
    fun deleteAll()
}
```

### 3. Statement / CSV Export Workflow
In `MainActivity.kt`:
- Query transactions matching current filter (`app`, `date range`).
- Build formatted HTML / CSV statement.
- On Android 10+ (API 29+), write to `MediaStore.Downloads` via `ContentResolver`.
- On older Android versions, write to `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`.
- Notify user via `Toast`.

---

## Verification
- Test insertion from both `NotificationListener.kt` and `SmsListener.kt`.
- Confirm transaction appears instantly in `MainActivity.kt`'s `RecyclerView`.
- Verify export file produces valid columns and opens in standard viewers.
