package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FinanceTransaction
import com.example.data.model.Folio
import com.example.data.model.FolioCharge
import kotlinx.coroutines.flow.Flow

@Dao
interface FolioDao {

    @Query("SELECT * FROM folios WHERE folioId = :folioId LIMIT 1")
    suspend fun getById(folioId: String): Folio?

    @Query("SELECT * FROM folios WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getByBookingId(bookingId: String): Folio?

    @Query("SELECT * FROM folios WHERE bookingId = :bookingId LIMIT 1")
    fun observeByBookingId(bookingId: String): Flow<Folio?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folio: Folio)

    @Update
    suspend fun update(folio: Folio)

    /**
     * Optimistic-lock update.
     *
     * Returns 1 only when the expected version still matches.
     */
    @Query("""
        UPDATE folios
        SET status = :status,
            version = version + 1,
            lastModifiedTimestamp = :lastModifiedTimestamp,
            updatedAt = :updatedAt
        WHERE folioId = :folioId
          AND version = :expectedVersion
    """)
    suspend fun updateStatusOptimistic(
        folioId: String,
        expectedVersion: Int,
        status: String,
        lastModifiedTimestamp: Long,
        updatedAt: Long
    ): Int

    @Query("SELECT * FROM folios")
    suspend fun getAllDirect(): List<Folio>
}

@Dao
interface FolioChargeDao {

    @Query("""
        SELECT * FROM folio_charges
        WHERE folioId = :folioId
        ORDER BY occurredAt ASC, createdAt ASC
    """)
    fun getForFolio(folioId: String): Flow<List<FolioCharge>>

    @Query("""
        SELECT * FROM folio_charges
        WHERE folioId = :folioId
        ORDER BY occurredAt ASC, createdAt ASC
    """)
    suspend fun getForFolioDirect(folioId: String): List<FolioCharge>

    @Query("""
        SELECT * FROM folio_charges
        WHERE bookingId = :bookingId
        ORDER BY occurredAt ASC, createdAt ASC
    """)
    suspend fun getForBookingDirect(bookingId: String): List<FolioCharge>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(charge: FolioCharge)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(charges: List<FolioCharge>)

    @Query("SELECT * FROM folio_charges")
    suspend fun getAllDirect(): List<FolioCharge>
}

@Dao
interface FinanceTransactionDao {

    @Query("""
        SELECT * FROM finance_transactions
        WHERE transactionId = :transactionId
        LIMIT 1
    """)
    suspend fun getById(transactionId: String): FinanceTransaction?

    @Query("""
        SELECT * FROM finance_transactions
        WHERE paymentId = :paymentId
        LIMIT 1
    """)
    suspend fun getByPaymentId(paymentId: String): FinanceTransaction?

    @Query("""
        SELECT * FROM finance_transactions
        WHERE bookingId = :bookingId
        ORDER BY transactionTimestamp DESC
    """)
    fun observeForBooking(bookingId: String): Flow<List<FinanceTransaction>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: FinanceTransaction)

    @Query("SELECT * FROM finance_transactions")
    suspend fun getAllDirect(): List<FinanceTransaction>
}
