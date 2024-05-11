package com.sproatcentral.whoswinningredux

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.time.Instant
import java.time.LocalDateTime

class GameScores {

    var gameName = ""
    var highScoreWinner = true
    var gameSaved = false
    var gameDate = LocalDateTime.now()

    var players = mutableListOf<GamePlayer>()

    var winningIndex : List<Int> = listOf<Int>()
        get () {
            var winners = mutableListOf<Int>()
            var winningScore = 0
            var scoreAdjustment = if(highScoreWinner) 1 else -1

            for(playerIndex in 0..< players.size) {
                if(players[playerIndex].currentScore * scoreAdjustment > winningScore) {
                    winners.clear()
                    winners.add(playerIndex)
                    winningScore = players[playerIndex].currentScore * scoreAdjustment
                } else if(players[playerIndex].currentScore * scoreAdjustment == winningScore) {
                    winners.add(playerIndex)
                }
            }

            return winners
        }
}

class GamePlayer {
    var name = ""
    val scoreList by mutableStateOf(mutableListOf<Int>())

    fun addScore(newScore: Int) {
        scoreList.add(newScore)
    }

    fun removeScore(scoreIndex: Int) {
        scoreList.removeAt(scoreIndex)
    }

    var currentScore : Int = 0
        get() {
            var scoreTotal = 0

            for (score in scoreList) {
                scoreTotal += score
            }

            return scoreTotal
        }
}