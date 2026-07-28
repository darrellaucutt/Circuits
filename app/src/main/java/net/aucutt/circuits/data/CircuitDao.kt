package net.aucutt.circuits.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CircuitDao {
    @Query("SELECT * FROM circuits ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CircuitEntity>>

    @Query("SELECT * FROM circuits WHERE id = :id")
    suspend fun getById(id: Long): CircuitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(circuit: CircuitEntity): Long

    @Update
    suspend fun update(circuit: CircuitEntity)

    @Delete
    suspend fun delete(circuit: CircuitEntity)
}
