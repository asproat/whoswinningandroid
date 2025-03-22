package com.sproatcentral.whoswinningredux

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZonedDateTime


@Database(entities = [GameScores::class, GamePlayer::class, GamePlayerScore::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameScoresDao(): GameScoresDao
}

@Dao
interface GameScoresDao {
    @Query("SELECT * FROM GameScores ORDER BY gameDate DESC")
    fun getAll(): List<GameScores>

    @Transaction
    @Query("SELECT * FROM GameScores")
    fun getWithPlayers(): List<GameScoreWithPlayers>

    @Transaction
    @Query("SELECT * FROM GameScores")
    fun getWithPlayersAndScores(): List<GameScoreWithPlayersAndScores>

    @Transaction
    @Query("SELECT * FROM GamePlayer")
    fun getPlayersWithScores(): List<GamePlayerWithScores>

    @Query("SELECT * FROM GameScores WHERE gameDate = :gameDate")
    fun findByDate(gameDate: Long): GameScores

    @Insert
    fun insertAll(vararg gameScores: GameScores)

    @Insert
    fun insertGamePlayer(gamePlayer: GamePlayer): Long

    @Insert
    fun insertGamePlayerScore(gamePlayerScore: GamePlayerScore): Long

    @Delete
    fun delete(gameScores: GameScores)
}

@Serializable
@Entity
class GameScores : RealmObject() {

    var gameName = ""
    var highScoreWinner = true

    @PrimaryKey
    @io.realm.annotations.PrimaryKey
    var gameDate = LocalDateTime.now().toEpochSecond(ZonedDateTime.now().offset)

    @Transient
    @Ignore
    var readOnly = false

    @Ignore
    var players = mutableListOf<GamePlayer>()

    fun removePlayer(userIndex: Int) {
        players.removeAt(userIndex)
    }

    fun standings(context: Context): String {
        var standings = "${context.getString(R.string.standings_title)}"

        // default sort is ascending, high score should be descending
        val scoreAdjustment = if (highScoreWinner) -1 else 1
        val sortedPlayers = players.sortedBy { it.currentScore() * scoreAdjustment }

        for (standingPlayer in sortedPlayers) {
            standings = standings.plus("${standingPlayer.name}: ${standingPlayer.currentScore()}\n")
        }

        return standings

    }

    fun winningIndex(): List<Int> {
        val winners = mutableListOf<Int>()
        var winningScore = if (highScoreWinner) 0 else Int.MIN_VALUE
        val scoreAdjustment = if (highScoreWinner) 1 else -1

        for (playerIndex in 0..<players.size) {
            if (players[playerIndex].currentScore() * scoreAdjustment > winningScore) {
                winners.clear()
                winners.add(playerIndex)
                winningScore = players[playerIndex].currentScore() * scoreAdjustment
            } else if (players[playerIndex].currentScore() * scoreAdjustment == winningScore) {
                winners.add(playerIndex)
            }
        }

        return winners
    }

    fun saveToPrefs(context: Context) {
        val prefs = context.getSharedPreferences("whosWinning", Context.MODE_PRIVATE).edit()

        prefs.putString(
            "currentGame",
            Json.encodeToString(this)
        )

        // not using apply in background to aovid losing data if the app closes suddenly
        prefs.commit()
    }

    fun prepRoom(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()
    }

    fun prepRealm(context: Context) {
        Realm.init(context)
        val realmConfig = RealmConfiguration.Builder()
            .name("whoswinning.realm")
            .schemaVersion(1)
            .build()
        Realm.setDefaultConfiguration(realmConfig)
    }

    /*
    fun dateString(epochSeconds: Long) : String {
        val epochLocalInstant = Instant.ofEpochSecond(epochSeconds).atZone(ZonedDateTime.now().zone)

        return (String.format("%04d%02d%02d%02d%02d%02d",
                        epochLocalInstant.year,
                        epochLocalInstant.month,
                        epochLocalInstant.dayOfMonth,
                        epochLocalInstant.hour,
                        epochLocalInstant.minute,
                        epochLocalInstant.second,
                    )
                )
    }
     */
}

@Serializable
@Entity(
    foreignKeys = [ForeignKey(
        entity = GameScores::class,
        parentColumns = ["gameDate"],
        childColumns = ["gameDate"],
        onDelete = ForeignKey.CASCADE
    )]
)
class GamePlayer : RealmObject() {

    @PrimaryKey(autoGenerate = true)
    @Transient
    var _id = 0

    @Transient
    var gameDate = 0L
    var name = ""

    @Ignore
    var scoreList = mutableListOf<GamePlayerScore>()

    fun addScore(newScore: Int) {
        val score = GamePlayerScore()
        score.setInitialScore(newScore)
        scoreList.add(score)
    }

    fun removeScore(scoreIndex: Int) {
        scoreList.removeAt(scoreIndex)
    }

    fun currentScore(): Int {
        var scoreTotal = 0

        for (score in scoreList) {
            scoreTotal += score.score
        }

        return scoreTotal
    }
}

class GameScoreWithPlayers {
    @Embedded
    var gameScores: GameScores? = null

    @Relation(parentColumn = "gameDate", entityColumn = "gameDate", entity = GamePlayer::class)
    var players: List<GamePlayer>? = null
}

class GameScoreWithPlayersAndScores {
    @Embedded
    var gameScores: GameScores? = null

    @Relation(parentColumn = "gameDate", entityColumn = "gameDate", entity = GamePlayer::class)
    var players: List<GamePlayerWithScores>? = null
}

@Serializable
@Entity(
    foreignKeys = [ForeignKey(
        entity = GamePlayer::class,
        parentColumns = ["_id"],
        childColumns = ["gamePlayerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class GamePlayerScore : RealmObject() {
    @PrimaryKey(autoGenerate = true)
    @Transient
    var _id = 0

    @Transient
    var gamePlayerId = 0
    var score = 0

    fun setInitialScore(newScore: Int) {
        score = newScore
    }
}

class GamePlayerWithScores {
    @Embedded
    var gamePlayer: GamePlayer? = null

    @Relation(parentColumn = "_id", entityColumn = "gamePlayerId", entity = GamePlayerScore::class)
    var scores: List<GamePlayerScore>? = null
}

