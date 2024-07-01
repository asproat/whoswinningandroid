package com.sproatcentral.whoswinningredux

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.TimeZone

class HistoryActivity : FragmentActivity() {

    private lateinit var roomdb: AppDatabase
    private lateinit var historyDao: GameScoresDao
    private lateinit var historyList: List<GameScores>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // Back is pressed... Finishing the activity
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(
                this /* lifecycle owner */,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Back is pressed... Finishing the activity
                        finish()
                    }
                })
        }

        // get saved games
        roomdb = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "database-name"
        ).build()
        historyDao = roomdb.gameScoresDao()

        runBlocking {
            // get saved games
            getHistoryListNow().await()

            setContent {
                History(this@HistoryActivity, historyList, historyDao)
            }
        }
    }

    fun getHistoryListNow() = GlobalScope.async {
        historyList = historyDao.getAll().toList()
    }

}

@Composable
fun History(activity: HistoryActivity, historyList: List<GameScores>, historyDao: GameScoresDao) {

    val showDateSummary = remember { mutableStateOf(false) }
    val summaryGame = remember { mutableStateOf(GameScores()) }
    val summaryGameName = remember { mutableStateOf("") }
    val summaryGameDate = remember { mutableStateOf("") }
    val summaryGamePlayers = remember { mutableListOf<GamePlayer>() }

    fun getGameWithPlayersAndScores(game: GameScores) = GlobalScope.async {
        historyDao.getWithPlayersAndScores()
            .firstOrNull { it.gameScores!!.gameDate == game.gameDate }
    }

    suspend fun showGame(game: GameScores) {
        val gameWithPlayersAndScores = getGameWithPlayersAndScores(game).await()
        for (player in gameWithPlayersAndScores!!.players!!) {
            gameWithPlayersAndScores.gameScores!!.players.add(player.gamePlayer!!)
            for (score in player.scores!!) {
                gameWithPlayersAndScores.gameScores!!
                    .players[gameWithPlayersAndScores.gameScores!!.players.size - 1]
                    .addScore(score.score)
            }
        }
        summaryGame.value = gameWithPlayersAndScores.gameScores!!
        summaryGameDate.value = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(summaryGame.value.gameDate),
            TimeZone.getDefault().toZoneId()).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        )
        summaryGameName.value = summaryGame.value.gameName
        summaryGamePlayers.clear()
        summaryGamePlayers.addAll(summaryGame.value.players)
        showDateSummary.value = true
    }

    Column(
        verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxSize(1.0f)
    ) {

        if (showDateSummary.value) {
            AlertDialog(onDismissRequest = { showDateSummary.value },
                title = { Text(summaryGameName.value) },
                text = {
                    Column() {
                        Text(summaryGameDate.value)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.winner_label))
                        val winningPlayersIndexes = summaryGame.value.winningIndex()

                        LazyColumn() {
                            items(winningPlayersIndexes.size) { winnerIndex ->
                                val winningPlayer =
                                    summaryGamePlayers[winningPlayersIndexes[winnerIndex]]
                                Row() {
                                    Text(
                                        String.format(
                                            stringResource(R.string.summary_player_format),
                                            winningPlayer.name,
                                            winningPlayer.currentScore()
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val losingPlayers =
                            summaryGame.value.players.withIndex()
                                .filter { !winningPlayersIndexes.contains(it.index) }
                                .sortedByDescending { it.value.currentScore() }
                        LazyColumn() {
                            items(losingPlayers.size) { loserIndex ->
                                val losingPlayer =
                                    losingPlayers[loserIndex].value
                                Row() {
                                    Text(
                                        String.format(
                                            stringResource(R.string.summary_player_format),
                                            losingPlayer.name,
                                            losingPlayer.currentScore()
                                        )
                                    )
                                }
                            }
                        }
                        Button(onClick = {
                            val data = Intent()
                            data.putExtra("gameScore",
                                Json.encodeToString(summaryGame.value))

                            activity.setResult(Activity.RESULT_OK, data)
                            activity.finish()
                        }, content = {
                            Text(stringResource(R.string.show_details))
                        }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showDateSummary.value = false }) {
                        Text( stringResource(R.string.ok) )
                    }
                }
            )
        }
        Text("History")
        Button(onClick = {
            activity.finish()
        }) {
            Icon(imageVector = Icons.Default.Done, "")
        }

        LazyColumn() {
            items(historyList.size) { historyIndex ->
                Row(
                    modifier = Modifier.clickable {
                        runBlocking {
                            showGame(historyList[historyIndex])
                        }
                    }
                ) {
                    Text(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(historyList[historyIndex].gameDate),
                            TimeZone.getDefault().toZoneId()).format(
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                    Text(
                        historyList[historyIndex].gameName.toString(),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }
        }
    }
}