package net.mbonnin.arcanetracker.room

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import io.reactivex.Flowable
import io.reactivex.Maybe
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.BnetGameType
import net.mbonnin.arcanetracker.GameType


@Entity
data class RDeck(
    @PrimaryKey
    var id: String = "",

    var wins: Int = 0,
    var losses: Int = 0
)

@Entity
data class RGame(
        val deck_id: String?,
        val victory: Boolean,
        val player_class: String,
        val opponent_class: String,
        val coin: Boolean,
        val rank: Int? = null,
        val game_type: String,
        val format_type: String,
        val hs_replay_url: String? = null,
        val date: Long? = null,
        val deck_name: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}


@Database(entities = arrayOf(RDeck::class, RGame::class), version = 2)
abstract class RDatabase : RoomDatabase() {
    abstract fun deckDao(): RDeckDao
    abstract fun gameDao(): RGameDao
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

@Dao
interface RGameDao {
    @Query("UPDATE rgame SET hs_replay_url = :hs_replay_url WHERE id = :id")
    fun update(id: Long, hs_replay_url: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rGame: RGame): Long

    @Query("SELECT COUNT(*) FROM rgame WHERE opponent_class = :opponent_class")
    fun totalPlayedAgainst(opponent_class: String): Maybe<Int>

    @Query("SELECT COUNT(*) FROM rgame WHERE opponent_class = :opponent_class AND victory = 1")
    fun totalVictoriesAgainst(opponent_class: String): Maybe<Int>
}


object RDatabaseSingleton {
    val instance = Room.databaseBuilder(ArcaneTrackerApplication.get(), RDatabase::class.java, "db")
            .fallbackToDestructiveMigration()
            .build()
}