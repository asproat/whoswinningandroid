package com.sproatcentral.whoswinningredux

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sproatcentral.whoswinningredux.ui.theme.WhoswinningreduxTheme
import io.realm.Realm
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    lateinit var realm: Realm

    var fromHistory = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
            return true
        } else if (item.itemId == R.id.history) {
            val intent = Intent(this, HistoryActivity::class.java)
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            resultLauncher.launch(intent)
            return true
        } else {
            return super.onMenuItemSelected(featureId, item)
        }
    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK &&
                result.data != null &&
                result.data!!.hasExtra("gameScore")
            ) {

                val data = result.data!!
                fromHistory = true
                setContent {
                    WhoswinningreduxTheme {
                        CompositionLocalProvider(
                            LocalTextStyle provides LocalTextStyle.current.merge(
                                TextStyle(fontSize = 25.sp)
                            )
                        )
                        {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                ScoreList(
                                    innerPadding = PaddingValues(0.dp),
                                    Json.decodeFromString<GameScores>(data.getStringExtra("gameScore")!!)
                                )
                            }
                        }
                    }
                }
                //doSomeOperations()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Realm.init(this)
        realm = Realm.getDefaultInstance()
        var currentGame: GameScores? = null

        val prefs = this.getSharedPreferences("whosWinning", MODE_PRIVATE)

        if (prefs.contains("currentGame")) {
            try {
                currentGame = Json.decodeFromString<GameScores>(
                    prefs.getString("currentGame", "") ?: ""
                )
            } catch (se: IllegalArgumentException) {
                // never mind
            }
        }

        actionBar?.title = getString(R.string.app_name)

        setContent {
            WhoswinningreduxTheme {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(
                        TextStyle(fontSize = 25.sp)
                    )
                )
                {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ScoreList(innerPadding = innerPadding, currentGame)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScoreList(innerPadding: PaddingValues, startingGame: GameScores? = null) {
        val currentGame = remember { mutableStateOf(startingGame ?: GameScores()) }
        val winnerHighScore = remember { mutableStateOf(true) }
        val activePlayerIndex = remember { mutableIntStateOf(-1) }
        val playersPlusAddCount = remember { mutableIntStateOf(currentGame.value.players.size) }
        val listExpanded = remember { mutableStateOf(false) }
        val currentScoreList = remember { mutableStateOf(listOf<GamePlayerScore>()) }

        val showConfirmClose = remember { mutableStateOf(false) }
        val showSaveGame = remember { mutableStateOf(false) }
        val saveGameName = remember { mutableStateOf("") }
        val saveGame = remember { mutableStateOf(false) }
        val shareGame = remember { mutableStateOf(false) }

        val showStandings = remember { mutableStateOf(false) }

        val showRemoveUser = remember { mutableStateOf(false) }
        val showRemoveScore = remember { mutableStateOf(false) }
        val removePlayer = remember { mutableIntStateOf(-1) }
        val removeScore = remember { mutableIntStateOf(-1) }

        val columnHeight = remember { mutableStateOf(-1.dp) }
        val localDensity = LocalDensity.current

        val nameFocusRequester = remember { FocusRequester() }
        val scoreFocusRequester = remember { FocusRequester() }

        val gameSaveImage = remember {
            mutableStateOf(ImageBitmap(500, 300))
        }

        SideEffect {
            Log.i("MainActivity", "currentGame Date: ${currentGame.value.gameDate}")
        }

        fun updateImage() {
            gameSaveImage.value = ImageBitmap(500, 800)
            val canvas = Canvas(gameSaveImage.value.asAndroidBitmap())
            canvas.drawColor(Color.Gray.toArgb())
            val textPaint = Paint()
            textPaint.color = Color.Black.toArgb()
            textPaint.textSize = 180f
            canvas.drawText(saveGameName.value, 100f, 100f, textPaint)
        }

        fun addScore(playerIndex: Int, newScore: Int) {
            currentGame.value.players[playerIndex].addScore(newScore)
            currentGame.value.saveToPrefs(this)
            currentScoreList.value = currentGame.value.players[playerIndex].scoreList
            listExpanded.value = true
            activePlayerIndex.intValue = -1
            Handler(Looper.getMainLooper()).postDelayed({
                activePlayerIndex.intValue = playerIndex
            }, 100L)
        }

        fun addPlayer(newName: String) {
            val newPlayer = GamePlayer()
            newPlayer.gameDate = currentGame.value.gameDate
            newPlayer.name = newName
            currentGame.value.players.add(newPlayer)
            currentGame.value.saveToPrefs(this)
            activePlayerIndex.intValue =
                currentGame.value.players.size - 1
            playersPlusAddCount.intValue =
                currentGame.value.players.size
            currentScoreList.value = listOf()
            listExpanded.value = false
        }

        fun showNextPlayer(i: Int = 0, lastPlayer: Int = -1) {
            var current = i
            val playerCount = currentGame.value.players.count()
            val last = if (lastPlayer != -1) lastPlayer else (
                    (playerCount * 3..playerCount * 6).random()
                    )
            Handler(Looper.getMainLooper()).postDelayed({
                current++
                activePlayerIndex.intValue =
                    current % currentGame.value.players.count()
                if (current < last) {
                    showNextPlayer(current, last)
                }
            }, 100L)
        }

        fun resetCurrentGame(clearGame: Boolean = true) {
            if (clearGame) {
                // not using apply in background because it may not finish before app is closed
                this.getSharedPreferences("whosWinning", MODE_PRIVATE).edit().remove("currentGame")
                    .commit()
                currentGame.value = GameScores()
            } else {
                currentGame.value =
                    Json.decodeFromString<GameScores>(
                        this.getSharedPreferences("whosWinning", MODE_PRIVATE)
                            .getString("currentGame", "") ?: ""
                    )
            }
            fromHistory = false
            activePlayerIndex.intValue = -1
            playersPlusAddCount.intValue = currentGame.value.players.size
            currentScoreList.value = listOf()
            listExpanded.value = false
        }

        fun saveGameNow() = GlobalScope.async {
            currentGame.value.gameName = saveGameName.value
            Log.d(
                "Room",
                "saving ${currentGame.value.gameName} with ${currentGame.value.players.size} players"
            )
            val dao = currentGame.value.prepRoom(this@MainActivity)
                .gameScoresDao()
            dao.insertAll(currentGame.value)
            for (player in currentGame.value.players) {
                Log.d("Room", "saving ${player.name} with ${player.scoreList.size} scores")
                val playerId = dao.insertGamePlayer(player)
                for (score in player.scoreList) {
                    Log.d("Room", "saving score with value ${score.score}")
                    score.gamePlayerId = playerId.toInt()
                    dao.insertGamePlayerScore(score)
                }
            }

            // clear current game
            resetCurrentGame()
            showConfirmClose.value = false
        }

        if (showConfirmClose.value) {
            Dialog(
                onDismissRequest = {
                    showConfirmClose.value = false
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .border(
                                3.dp, Color.Black,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .fillMaxWidth(0.9f)
                            //.fillMaxHeight(0.9f)
                            .clip(shape = RoundedCornerShape(15.dp))
                            .background(Color.White)
                            .padding(20.dp)
                    ) {
                        //Spacer(Modifier.weight(0.1f))
                        if (showSaveGame.value) {
                            if (!saveGame.value) {
                                Text(stringResource(R.string.save_game))
                                //Spacer(Modifier.weight(0.1f))
                                Row(horizontalArrangement = Arrangement.Center) {
                                    //Spacer(Modifier.weight(0.3f))
                                    Button(content = { Text(stringResource(R.string.yes)) },
                                        onClick = {
                                            saveGame.value = true
                                        }
                                    )
                                    Spacer(Modifier.weight(0.1f))
                                    Button(content = { Text(stringResource(R.string.no)) },
                                        onClick = {
                                            resetCurrentGame()
                                            showConfirmClose.value = false
                                        }
                                    )
                                    //Spacer(Modifier.weight(0.1f))
                                }
                            } else {
                                Text(stringResource(R.string.save_game_name))
                                TextField(
                                    value = saveGameName.value,
                                    onValueChange = { newValue ->
                                        saveGameName.value = newValue
                                        updateImage()
                                    },
                                    trailingIcon = {
                                        CompositionLocalProvider(
                                            LocalMinimumInteractiveComponentEnforcement provides false,
                                        ) {
                                            IconButton(onClick = {
                                                saveGameName.value = ""
                                            }) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = ""
                                                )

                                            }
                                        }
                                    }
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = shareGame.value,
                                        onCheckedChange = {
                                            shareGame.value = it
                                        })
                                    Text(stringResource(R.string.share_game))
                                }
                                if (shareGame.value) {
                                    Image(
                                        gameSaveImage.value, "",
                                        modifier = Modifier.size(
                                            (500 / resources.displayMetrics.density).dp,
                                            (800 / resources.displayMetrics.density).dp
                                        )
                                    )

                                }
                                Button(
                                    onClick = {
                                        if (saveGameName.value.isNotEmpty()) {
                                            // save to database
                                            saveGameNow()
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                R.string.no_name,
                                                Toast.LENGTH_SHORT
                                            )
                                        }

                                    },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = ""
                                        )
                                    }
                                )
                                Button(
                                    onClick = {
                                        showSaveGame.value = false
                                        resetCurrentGame()
                                        showConfirmClose.value = false
                                    },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = ""
                                        )
                                    }
                                )
                            }
                        } else {
                            Text(stringResource(R.string.confirm_end))
                            //Spacer(Modifier.weight(0.1f))
                            Row(horizontalArrangement = Arrangement.SpaceAround) {
                                //Spacer(Modifier.weight(0.3f))
                                Button(content = { Text(stringResource(R.string.yes)) },
                                    onClick = {
                                        resetCurrentGame()
                                        showConfirmClose.value = false
                                        // showSaveGame.value = true
                                    }
                                )
                                Spacer(Modifier.weight(0.1f))
                                Button(content = { Text(stringResource(R.string.no)) },
                                    onClick = {
                                        showConfirmClose.value = false
                                    }
                                )
                                //Spacer(Modifier.weight(0.3f))
                            } // end game button row
                            //Spacer(Modifier.weight(0.1f))
                        } // else show close game
                    } // dialog column
                } // dialog content
            )
        }

        if(showStandings.value) {
            BasicAlertDialog (
                onDismissRequest = {
                    showStandings.value = false
                },
                content = { Text(currentGame.value.standings(applicationContext)) },
                modifier = Modifier
                    .border(
                        3.dp, Color.Black,
                        shape = RoundedCornerShape(15.dp)
                    )
                    .clip(shape = RoundedCornerShape(15.dp))
                    .background(Color.White)
                    .padding(10.dp)
            )
        }

        if (showRemoveUser.value) {
            AlertDialog(
                onDismissRequest = {
                    showRemoveUser.value = false
                },
                title = { Text(stringResource(R.string.confirm_remove_player)) },
                dismissButton = {
                    Button(content = {
                        Text(stringResource(R.string.no))
                    },
                        onClick = {
                            showRemoveUser.value = false
                        }
                    )
                },
                confirmButton = {
                    Button(content = {
                        Text(stringResource(R.string.yes))
                    },
                        onClick = {
                            currentGame.value.removePlayer(removePlayer.intValue)
                            currentGame.value.saveToPrefs(this)
                            showRemoveUser.value = false
                            var currentIndex = activePlayerIndex.intValue
                            activePlayerIndex.intValue = -1
                            currentIndex = if (currentIndex > 0) currentIndex - 1 else
                                (if (currentGame.value.players.size > 0) 0 else -1)
                            if (currentIndex != -1) {
                                currentScoreList.value =
                                    currentGame.value.players[currentIndex].scoreList
                            }
                            Handler(Looper.getMainLooper()).postDelayed({
                                activePlayerIndex.intValue = currentIndex
                            }, 100L)
                        }
                    )
                }
            )
        }

        if (showRemoveScore.value) {
            AlertDialog(
                onDismissRequest = {
                    showRemoveScore.value = false
                },
                title = { Text(stringResource(R.string.confirm_remove_score)) },
                dismissButton = {
                    Button(content = {
                        Text(stringResource(R.string.no))
                    },
                        onClick = {
                            showRemoveScore.value = false
                        }
                    )
                },
                confirmButton = {
                    Button(content = {
                        Text(stringResource(R.string.yes))
                    },
                        onClick = {
                            currentGame.value.players[removePlayer.intValue].removeScore(removeScore.intValue)
                            currentGame.value.saveToPrefs(this)
                            currentScoreList.value =
                                currentGame.value.players[removePlayer.intValue].scoreList
                            showRemoveScore.value = false
                            listExpanded.value = true
                            val currentIndex = activePlayerIndex.intValue
                            activePlayerIndex.intValue = -1
                            Handler(Looper.getMainLooper()).postDelayed({
                                activePlayerIndex.intValue = currentIndex
                            }, 100L)
                        }
                    )
                }
            )
        }

        LazyRow(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(all = 5.dp)
        )
        {
            items(playersPlusAddCount.intValue) { playerIndex ->
                if (playerIndex >= 0 &&
                    playerIndex < currentGame.value.players.size
                ) {
                    Column(
                        modifier = Modifier
                            .offset(
                                0.dp,
                                if (activePlayerIndex.intValue == playerIndex)
                                    20.dp
                                else
                                    0.dp
                            )
                            .fillMaxHeight(0.9f)
                            .fillParentMaxWidth(
                                animateFloatAsState(
                                    targetValue =
                                    if (activePlayerIndex.intValue == playerIndex)
                                        0.45f
                                    else
                                        0.20f,
                                    animationSpec = tween(durationMillis = 500)
                                ).value
                            )
                            .padding(7.dp)
                            .onGloballyPositioned { coordinates ->
                                if (columnHeight.value == -1.dp) {
                                    columnHeight.value =
                                        with(localDensity) { coordinates.size.height.toDp() }
                                }
                            }
                            .drawBehind {
                                if (activePlayerIndex.intValue == playerIndex) {
                                    // right shadow
                                    drawRect(
                                        brush =
                                        Brush.horizontalGradient(
                                            listOf(Color(0x99333333), Color(0x00FFFFFF)),
                                            size.width + 20f, size.width + 38f,
                                            TileMode.Decal
                                        ),
                                        Offset(size.width + 4f, 0f),
                                        Size(50.0f, this.size.height + 20f)
                                    )
                                }
                                // bottom shadow
                                drawRect(
                                    brush =
                                    Brush.verticalGradient(
                                        listOf(Color(0x99333333), Color(0x00FFFFFF)),
                                        this.size.height + 20f,
                                        this.size.height + 45f,
                                        TileMode.Decal
                                    ),
                                    Offset(
                                        this.center.x - (this.size.width / 2f),
                                        this.size.height + 20f),
                                    Size(
                                        this.size.width +
                                         if (activePlayerIndex.intValue - 1 == playerIndex ||
                                             (activePlayerIndex.intValue == -1 &&
                                              playerIndex == currentGame.value.players.size - 1)
                                             )
                                                15f
                                             else
                                                33f,
                                        25.0f
                                    )
                                )
                                // right
                                drawLine(
                                    Color.Black,
                                    Offset(size.width + 17f, -17f),
                                    Offset(size.width + 17f, size.height + 15f),
                                    3.dp.toPx()
                                )
                                // top
                                drawLine(
                                    Color.Black,
                                    Offset(-15f, -17f),
                                    Offset(size.width + 17f, -17f),
                                    3.dp.toPx()
                                )
                                // left
                                drawLine(
                                    Color.Black,
                                    Offset(-15f, -17f), Offset(-15f, size.height + 17f),
                                    3.dp.toPx()
                                )
                                // bottom
                                drawLine(
                                    Color.Black,
                                    Offset(-15f, size.height + 17f),
                                    Offset(size.width + 17f, size.height + 17f),
                                    3.dp.toPx()
                                )

                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // show player
                        Text(
                            String.format(
                                stringResource(R.string.playerNameFormat),
                                if (currentGame.value.winningIndex()
                                        .contains(playerIndex)
                                )
                                    "*" else "",
                                currentGame.value.players[playerIndex].name
                            ),
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                activePlayerIndex.intValue = playerIndex
                                listExpanded.value = false
                                currentScoreList.value =
                                    currentGame.value.players[activePlayerIndex.intValue].scoreList
                                if (!fromHistory) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        scoreFocusRequester.requestFocus()
                                    }, 100L)
                                }
                            }
                        )
                        if (listExpanded.value &&
                            activePlayerIndex.intValue == playerIndex
                        ) {
                            LazyColumn(
                                userScrollEnabled = true,
                                modifier = Modifier.heightIn(
                                    min = 0.dp,
                                    max = (columnHeight.value.value * 0.75f).dp
                                )
                            ) {
                                items(currentScoreList.value.size) { scoreIndex ->
                                    Text(
                                        currentScoreList.value[scoreIndex].score.toString(),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier
                                            .fillMaxWidth(1.0f)
                                            .clickable {
                                                removePlayer.intValue = playerIndex
                                                removeScore.intValue = scoreIndex
                                                showRemoveScore.value = true
                                            }
                                    )
                                }
                            }
                        }
                        Text(currentGame.value.players[playerIndex].currentScore()
                            .toString(),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .clickable {
                                    if (activePlayerIndex.intValue == playerIndex) {
                                        listExpanded.value = listExpanded.value.not()
                                    } else {
                                        activePlayerIndex.intValue = playerIndex
                                        listExpanded.value = true
                                        currentScoreList.value =
                                            currentGame.value.players[activePlayerIndex.intValue].scoreList
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            scoreFocusRequester.requestFocus()
                                        }, 1000L)
                                    }
                                }
                                .fillMaxWidth(1.0f)
                        )
                        if (activePlayerIndex.intValue == playerIndex &&
                            !this@MainActivity.fromHistory
                        ) {
                            val newScoreString =
                                remember { mutableStateOf(TextFieldValue("0")) }
                            val newScore = remember { mutableIntStateOf(0) }
                            TextField(newScoreString.value,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                                onValueChange = {
                                    newScoreString.value = it
                                    newScore.intValue = it.text.trim().toInt()
                                },
                                /*
                                    if (it.text.toIntOrNull() != null) {
                                        newScore.intValue = it.text.toInt()
                                        newScoreString.value =
                                            newScoreString.value.copy(
                                                newScore.intValue.toString()
                                            )
                                    }
                                },
                                 */
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                leadingIcon = {
                                    IconButton(onClick = {
                                        newScore.intValue += 1
                                        newScoreString.value =
                                            newScoreString.value.copy(
                                                newScore.intValue.toString(),
                                                selection = TextRange(
                                                    0,
                                                    newScore.intValue.toString().length
                                                )
                                            )
                                    }
                                    ) {
                                        Icon(Icons.Default.Add, "")
                                    }
                                },
                                trailingIcon = {
                                    Icon(painterResource(R.drawable.minus),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .clickable {
                                                newScore.intValue -= 1
                                                newScoreString.value =
                                                    newScoreString.value.copy(
                                                        newScore.intValue.toString(),
                                                        selection = TextRange(
                                                            0,
                                                            newScore.intValue.toString().length
                                                        )
                                                    )
                                            }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth(1.0f)
                                    .onKeyEvent {
                                        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                                            addScore(
                                                playerIndex,
                                                newScore.intValue
                                            )
                                            newScore.intValue = 0
                                            newScoreString.value =
                                                newScoreString.value.copy(
                                                    newScore.intValue.toString()
                                                )
                                            showStandings.value = true
                                            false
                                        }
                                        true
                                    }
                                    .focusRequester(scoreFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            val text = newScoreString.value.toString()
                                            newScoreString.value =
                                                newScoreString.value.copy(
                                                    selection = TextRange(
                                                        0,
                                                        text.length
                                                    )
                                                )
                                        }
                                    }
                            )
                            Button(
                                onClick = {
                                    addScore(playerIndex, newScore.intValue)
                                    newScore.intValue = 0
                                    newScoreString.value = newScoreString.value.copy(
                                        newScore.intValue.toString()
                                    )
                                    showStandings.value = true
                                },
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = ""
                                    )
                                }
                            )

                            Spacer(Modifier.weight(1.0f))

                            Button(
                                onClick = {
                                    removePlayer.intValue = playerIndex
                                    showRemoveUser.value = true
                                },
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = ""
                                    )
                                }
                            )
                        }
                    }
                }
            }
            item {
                // new player
                val newName = remember { mutableStateOf("") }
                val repeatWarning = remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .offset(
                            0.dp,
                            if (activePlayerIndex.intValue < 0)
                                20.dp
                            else
                                0.dp
                        )
                        .fillMaxHeight(0.9f)
                        .fillParentMaxWidth(
                            animateFloatAsState(
                                targetValue =
                                if (activePlayerIndex.intValue == -1)
                                    0.40f
                                else
                                    0.15f,
                                animationSpec = tween(durationMillis = 500)
                            ).value
                        )
                        .padding(all = 5.dp)
                        .drawBehind {
                            // right shadow
                            drawRect(
                                brush =
                                Brush.horizontalGradient(
                                    listOf(Color(0x99333333), Color(0x00FFFFFF)),
                                    size.width + 20f, size.width + 38f,
                                    TileMode.Decal
                                ),
                                Offset(size.width + 4f, 0f),
                                Size(50.0f, this.size.height + 20f)
                            )
                            // bottom shadow
                            drawRect(
                                brush =
                                Brush.verticalGradient(
                                    listOf(Color(0x00FFFFFF), Color(0x99333333)),
                                    0f, 25f, TileMode.Mirror
                                ),
                                Offset(5f, this.size.height + 20f),
                                Size(this.size.width + 7f, 25.0f)
                            )
                            // corner shadow
                            drawRect(
                                brush =
                                Brush.radialGradient(
                                    listOf(Color(0x99333333), Color(0x00FFFFFF)),
                                    Offset(this.size.width + 15f, this.size.height + 15f),
                                    30f,
                                    TileMode.Decal
                                ),
                                Offset(this.size.width + 13f, this.size.height + 20f),
                                Size(30f, 30f)
                            )
                            // top
                            drawLine(
                                Color.Black,
                                Offset(-15f, -15f), Offset(size.width + 17f, -15f),
                                3.dp.toPx()
                            )
                            // right
                            drawLine(
                                Color.Black,
                                Offset(size.width + 15f, -15f),
                                Offset(size.width + 15f, size.height + 15f),
                                3.dp.toPx()
                            )
                            // bottom
                            drawLine(
                                Color.Black,
                                Offset(-15f, size.height + 15f),
                                Offset(size.width + 17f, size.height + 15f),
                                3.dp.toPx()
                            )
                            // left
                            drawLine(
                                Color.Black,
                                Offset(-15f, -15f), Offset(-15f, size.height + 15f),
                                3.dp.toPx()
                            )

                        },

                    content = {
                        if (activePlayerIndex.intValue != -1) {
                            Image(
                                painterResource(id = R.drawable.user),
                                "",
                                modifier = Modifier.clickable {
                                    activePlayerIndex.intValue = -1
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        if (!this@MainActivity.fromHistory) {
                                            nameFocusRequester.requestFocus()
                                        }
                                    }, 100L)
                                }
                            )
                        } else {
                            if (!this@MainActivity.fromHistory) {

                                Text(
                                    text = stringResource(R.string.winner_label),
                                    modifier = Modifier.padding(5.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.low_score),
                                        style = TextStyle(fontSize = 14.sp),
                                        modifier = Modifier.padding(5.dp)
                                    )
                                    Switch(checked = winnerHighScore.value,
                                        onCheckedChange = {
                                            winnerHighScore.value = it
                                            currentGame.value.highScoreWinner =
                                                winnerHighScore.value
                                            currentGame.value.saveToPrefs(this@MainActivity)
                                            // reset winner
                                            if (currentGame.value.players.size > 0) {
                                                activePlayerIndex.intValue = 0
                                                Handler(Looper.getMainLooper()).postDelayed(
                                                    {
                                                        activePlayerIndex.intValue = -1
                                                    },
                                                    100L
                                                )
                                            }
                                        }
                                    )
                                    Text(
                                        text = stringResource(R.string.high_score),
                                        style = TextStyle(fontSize = 14.sp),
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                                BasicTextField(
                                    newName.value,
                                    textStyle = LocalTextStyle.current.merge(
                                        TextStyle(fontSize = 25.sp)
                                    ),
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        capitalization = KeyboardCapitalization.Words
                                    ),
                                    onValueChange = { currentName: String ->
                                        newName.value = currentName
                                        repeatWarning.value =
                                            currentGame.value.players.firstOrNull {
                                                it.name.startsWith(currentName)
                                            } != null
                                    },
                                    modifier = Modifier
                                        .width(IntrinsicSize.Min)
                                        .onKeyEvent {
                                            if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                                                addPlayer(newName.value)
                                            }
                                            false
                                        }
                                        .focusRequester(nameFocusRequester)
                                )

                                {
                                    TextFieldDefaults.DecorationBox(
                                        contentPadding = PaddingValues(3.dp),
                                        value = newName.value,
                                        enabled = true,
                                        placeholder = {
                                            Text(
                                                stringResource(R.string.name_hint),
                                                style = LocalTextStyle.current.merge(
                                                    TextStyle(fontSize = 25.sp)
                                                )
                                            )
                                        },
                                        innerTextField = it,
                                        singleLine = true,
                                        interactionSource = remember { MutableInteractionSource() },
                                        visualTransformation = VisualTransformation.None,
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                newName.value = ""
                                            }) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = ""
                                                )
                                            }
                                        }
                                    )
                                }
                                if (repeatWarning.value) {
                                    Text(
                                        stringResource(R.string.name_exists),
                                        modifier = Modifier.width(IntrinsicSize.Min)
                                    )
                                }
                                if (newName.value.isNotEmpty()) {
                                    Button(
                                        {
                                            addPlayer(newName.value)
                                        },
                                        content = {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = ""
                                            )
                                        },
                                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    showStandings.value = true
                                },
                                content = {
                                    Text(stringResource(R.string.standings))
                                },
                                modifier = Modifier
                                    .align(alignment = Alignment.CenterHorizontally)
                                    .padding(10.dp)
                            )

                            if (!this@MainActivity.fromHistory) {
                                Button(
                                    onClick = {
                                        if (currentGame.value.players.firstOrNull() != null) {
                                            // pick random number between 3 times and 6 times number of players
                                            activePlayerIndex.intValue = 0
                                            showNextPlayer()
                                        }
                                    },
                                    content = {
                                        Text(stringResource(R.string.choose_first))
                                    },
                                    modifier = Modifier
                                        .align(alignment = Alignment.CenterHorizontally)
                                        .padding(10.dp)
                                )
                            }

                            Spacer(Modifier.weight(1.0F))

                            Button(
                                onClick = {
                                    if (this@MainActivity.fromHistory) {
                                        resetCurrentGame(false)
                                    } else {

                                        if (currentGame.value.players.firstOrNull() != null) {
                                            showConfirmClose.value = true
                                            showSaveGame.value = false
                                            saveGameName.value = ""
                                            saveGame.value = false
                                            shareGame.value = false
                                            updateImage()
                                        }
                                    }
                                },
                                content = {
                                    Image(painterResource(R.drawable.finish), "")
                                },
                                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                            )

                        }
                    })
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        val currentGame = remember {
            mutableStateOf(GameScores())
        }
        val firstPlayer = GamePlayer()
        firstPlayer.gameDate = currentGame.value.gameDate
        firstPlayer.name = "jerry"
        currentGame.value.players.add(firstPlayer)

        WhoswinningreduxTheme {
            Scaffold {
                ScoreList(innerPadding = it, currentGame.value)
            }
        }
    }
}




