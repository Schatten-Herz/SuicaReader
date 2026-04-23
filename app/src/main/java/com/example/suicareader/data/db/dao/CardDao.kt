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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: TransitCard)

    @androidx.room.Update
    suspend fun updateCard(card: TransitCard)

    @Query("SELECT * FROM trip_records WHERE cardIdm = :idm ORDER BY timestamp DESC, id DESC")
    fun getTripsForCard(idm: String): Flow<List<TripRecord>>

    @Query("SELECT * FROM trip_records WHERE cardIdm = :idm ORDER BY timestamp ASC, id ASC")
    suspend fun getTripsListForCard(idm: String): List<TripRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrips(trips: List<TripRecord>)

    @androidx.room.Update
    suspend fun updateTrips(trips: List<TripRecord>)

    @Query("DELETE FROM transit_cards WHERE idm = :idm")
    suspend fun deleteCard(idm: String)

    @Query("DELETE FROM trip_records WHERE cardIdm = :idm")
    suspend fun deleteTripsForCard(idm: String)
}
