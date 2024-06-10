package se.warting.signaturecore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface EventDao {

    @Query("SELECT * FROM DBEvent where signatureId = :signatureId order by id ASC")
    fun getAll(signatureId: UUID): Flow<List<DBEvent>>

    @Query("SELECT * FROM DBEvent WHERE id IN (:eventIds)")
    suspend fun loadAllByIds(eventIds: IntArray): List<DBEvent>

    @Insert
    suspend fun insertAll(users: List<DBEvent>)

    @Insert
    suspend fun insert(event: DBEvent)

    @Delete
    suspend fun delete(user: DBEvent)

    @Query("DELETE FROM DBEvent")
    suspend fun clear()
}
