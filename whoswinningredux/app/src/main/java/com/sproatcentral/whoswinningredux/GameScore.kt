package com.sproatcentral.whoswinningredux

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.realm.RealmObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Entity
@Serializable
class GameScores : RealmObject() {

    var gameName = ""
    var highScoreWinner = true
    @PrimaryKey
    @io.realm.annotations.PrimaryKey
    var gameDate = LocalDateTime.now().toEpochSecond(ZonedDateTime.now().offset)

    var players = mutableListOf<GamePlayer>()

    fun removePlayer(userIndex: Int) {
        players.removeAt(userIndex)
    }

    fun winningIndex(): List<Int> {
            var winners = mutableListOf<Int>()
            var winningScore = 0
            var scoreAdjustment = if (highScoreWinner) 1 else -1

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

        prefs.apply()
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
class GamePlayer {
    var name = ""
    var scoreList = mutableListOf<Int>()

    fun addScore(newScore: Int) {
        scoreList.add(newScore)
    }

    fun removeScore(scoreIndex: Int) {
        scoreList.removeAt(scoreIndex)
    }

    fun currentScore(): Int {
        var scoreTotal = 0

        for (score in scoreList) {
            scoreTotal += score
        }

        return scoreTotal
    }
}