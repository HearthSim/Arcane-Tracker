package net.mbonnin.arcanetracker.room

import android.arch.persistence.room.*
import io.reactivex.Flowable
import net.mbonnin.arcanetracker.ArcaneTrackerApplication


@Entity
data class RDeck(
    @PrimaryKey
    var id: String = "",

    var wins: Int = 0,
    var losses: Int = 0
)

@Database(entities = arrayOf(RDeck::class), version = 1)
abstract class RDatabase : RoomDatabase() {
    abstract fun deckDao(): RDeckDao
}

@Dao
interface RDeckDao {
    @Query("SELECT * FROM rdeck")
    fun getAll(): List<RDeck>

    @Update
    fun update(rDeck: RDeck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rDeck: RDeck)

    @Query("SELECT * FROM rdeck WHERE id = :id LIMIT 1")
    fun findById(id: String): Flowable<List<RDeck>>

    @Delete
    fun delete(rDeck: RDeck)
}

object RDatabaseSingleton {
    val instance = Room.databaseBuilder(ArcaneTrackerApplication.get(), RDatabase::class.java, "db").build()
}