package com.example.suicareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.suicareader.data.db.entity.TransitCard
import com.example.suicareader.data.db.entity.TripRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM transit_cards ORDER BY lastUpdated DESC")
    fun getAllCards(): Flow<List<TransitCard>>

    @Query("SELECT * FROM transit_cards ORDER BY lastUpdated DESC")
    suspend fun getAllCardsList(): List<TransitCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: TransitCard)

    @Query("SELECT * FROM transit_cards WHERE idm = :idm LIMIT 1")
    suspend fun getCardByIdm(idm: String): TransitCard?

    @androidx.room.Update
    suspend fun updateCard(card: TransitCard)

    @Query("SELECT * FROM trip_records WHERE cardIdm = :idm ORDER BY timestamp DESC, id DESC")
    fun getTripsForCard(idm: String): Flow<List<TripRecord>>

    @Query("SELECT * FROM trip_records WHERE id = :tripId LIMIT 1")
    fun getTripById(tripId: Long): Flow<TripRecord?>

    @Query("SELECT * FROM trip_records WHERE cardIdm = :idm ORDER BY timestamp ASC, id ASC")
    suspend fun getTripsListForCard(idm: String): List<TripRecord>

    @Query("SELECT COUNT(*) FROM trip_records WHERE cardIdm = :idm")
    suspend fun getTripCountForCard(idm: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrips(trips: List<TripRecord>)

    @androidx.room.Update
    suspend fun updateTrips(trips: List<TripRecord>)

    @androidx.room.Update
    suspend fun updateTrip(trip: TripRecord)

    @Query("DELETE FROM transit_cards WHERE idm = :idm")
    suspend fun deleteCard(idm: String)

    @Query("DELETE FROM trip_records WHERE cardIdm = :idm")
    suspend fun deleteTripsForCard(idm: String)
}
