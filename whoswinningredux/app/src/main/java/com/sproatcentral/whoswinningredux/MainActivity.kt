package com.sproatcentral.whoswinningredux

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sproatcentral.whoswinningredux.ui.theme.WhoswinningreduxTheme
import io.realm.Realm
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    lateinit var realm: Realm

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
        } else {
            return super.onMenuItemSelected(featureId, item)
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
        val currentScoreList = remember { mutableStateOf(listOf<Int>()) }

        val showConfirmClose = remember { mutableStateOf(false) }
        val showSaveGame = remember { mutableStateOf(false) }
        val saveGameName = remember { mutableStateOf("") }
        val saveGame = remember { mutableStateOf(false) }
        val shareGame = remember { mutableStateOf(false) }

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
                                            currentGame.value = GameScores()
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
                                        Icon(Icons.Default.Clear,
                                            contentDescription = "",
                                            modifier = Modifier.clickable {
                                                saveGameName.value = ""
                                            })
                                    },
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

                                            // clear current game
                                            currentGame.value = GameScores()
                                            showConfirmClose.value = false
                                        }

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
                        } else {
                            Text(stringResource(R.string.confirm_end))
                            //Spacer(Modifier.weight(0.1f))
                            Row(horizontalArrangement = Arrangement.Center) {
                                //Spacer(Modifier.weight(0.3f))
                                Button(content = { Text(stringResource(R.string.yes)) },
                                    onClick = {
                                        showSaveGame.value = true
                                    }
                                )
                                Spacer(Modifier.weight(0.1f))
                                Button(content = { Text(stringResource(R.string.no)) },
                                    onClick = {
                                        showConfirmClose.value = false
                                    }
                                )
                                //Spacer(Modifier.weight(0.3f))
                            }
                            //Spacer(Modifier.weight(0.1f))
                        }
                    }
                }

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
                            if(currentIndex != -1) {
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
                            currentGame.value.players[removePlayer.value].removeScore(removeScore.intValue)
                            currentGame.value.saveToPrefs(this)
                            currentScoreList.value =
                                currentGame.value.players[removePlayer.value].scoreList
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
            //.border(2.dp, Color.Red)
        ) {
            items(playersPlusAddCount.intValue) { playerIndex ->
                if (playerIndex < currentGame.value.players.size) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, Color.Black)
                            .fillMaxHeight(0.9f)
                            .fillParentMaxWidth(
                                animateFloatAsState(
                                    targetValue =
                                    if (activePlayerIndex.intValue == playerIndex)
                                        0.35f
                                    else
                                        0.15f,
                                    animationSpec = tween(durationMillis = 500)
                                ).value
                            )//.border(2.dp, Color.Blue)
                            .padding(7.dp)
                            .onGloballyPositioned { coordinates ->
                                if (columnHeight.value == -1.dp) {
                                    columnHeight.value =
                                        with(localDensity) { coordinates.size.height.toDp() }
                                }
                            }
                            .drawBehind {
                                drawLine(
                                    Color(153, 153, 153, 102),
                                    Offset(0f, this.size.height + 22f),
                                    Offset(
                                        this.size.width + 25f,
                                        this.size.height + 22f
                                    ),
                                    10f
                                )
                                if (playerIndex == activePlayerIndex.value) {
                                    drawLine(
                                        Color(153, 153, 153, 102),
                                        Offset(this.size.width + 33f, 5f),
                                        Offset(
                                            this.size.width + 33f,
                                            this.size.height + 15f
                                        ),
                                        10f
                                    )
                                }
                            }
                    ) {
                        // show player
                        Row(
                            modifier = Modifier.fillMaxWidth(1.0f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        scoreFocusRequester.requestFocus()
                                    }, 100L)
                                }
                            )
                            if (activePlayerIndex.intValue == playerIndex) {
                                Icon(Icons.Default.Clear,
                                    "",
                                    modifier = Modifier.clickable {
                                        removePlayer.value = playerIndex
                                        showRemoveUser.value = true
                                    }
                                )
                            }
                        }
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
                                        currentScoreList.value[scoreIndex].toString(),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier
                                            .fillMaxWidth(1.0f)
                                            .clickable {
                                                removePlayer.value = playerIndex
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
                            //.border(2.dp, Color.Yellow)
                        )
                        if (activePlayerIndex.intValue == playerIndex) {
                            val newScoreString =
                                remember { mutableStateOf(TextFieldValue("0")) }
                            val newScore = remember { mutableIntStateOf(0) }

                            TextField(newScoreString.value,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                                onValueChange = {
                                    if (it.text.toIntOrNull() != null) {
                                        newScore.intValue = it.text.toInt()
                                        newScoreString.value =
                                            newScoreString.value.copy(
                                                newScore.intValue.toString()
                                            )
                                    }
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = "",
                                        modifier = Modifier
                                            .clickable {
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
                                            .scale(0.5f)
                                            .width(IntrinsicSize.Min)
                                    )
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
                                            .scale(0.5f)
                                            .width(IntrinsicSize.Min)
                                    )
                                },
                                modifier = Modifier
                                    .width(180.dp)
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
                                        }
                                        false
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
                }
            }
            item {
                // new player
                val newName = remember { mutableStateOf("") }
                val repeatWarning = remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .border(3.dp, Color.Black)
                        .fillMaxHeight(0.9f)
                        .fillParentMaxWidth(
                            animateFloatAsState(
                                targetValue =
                                if (activePlayerIndex.intValue == -1)
                                    0.50f
                                else
                                    0.10f,
                                animationSpec = tween(durationMillis = 500)
                            ).value
                        )
                        .padding(all = 5.dp)
                        .drawBehind {
                            drawLine(
                                Color(153, 153, 153, 102),
                                Offset(0f, this.size.height + 19f),
                                Offset(this.size.width + 22f, this.size.height + 19f),
                                10f
                            )
                            drawLine(
                                Color(153, 153, 153, 102),
                                Offset(this.size.width + 18f, 5f),
                                Offset(this.size.width + 18f, this.size.height + 14f),
                                10f
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
                                        nameFocusRequester.requestFocus()
                                    }, 100L)
                                }
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.winner_label),
                                modifier = Modifier.padding(5.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.low_score),
                                    style = TextStyle(fontSize = 12.sp),
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
                                    style = TextStyle(fontSize = 16.sp),
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                            BasicTextField(
                                newName.value,
                                textStyle = LocalTextStyle.current.merge(
                                    TextStyle(fontSize = 25.sp)
                                ),
                                onValueChange = { currentName: String ->
                                    newName.value = currentName
                                    repeatWarning.value =
                                        currentGame.value.players.firstOrNull {
                                            it.name.startsWith(currentName)
                                        } != null
                                },
                                modifier = Modifier
                                    .padding(5.dp)
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
                                        Icon(Icons.Default.Clear,
                                            contentDescription = "",
                                            modifier = Modifier.clickable {
                                                newName.value = ""
                                            })
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
                            } else {
                                Button(
                                    onClick = {
                                        if (currentGame.value.players.firstOrNull() != null) {
                                            showConfirmClose.value = true
                                            showSaveGame.value = false
                                            saveGameName.value = ""
                                            saveGame.value = false
                                            shareGame.value = false
                                            updateImage()
                                        }
                                    },
                                    content = {
                                        Image(painterResource(R.drawable.finish), "")
                                    },
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                                )
                            }
                        }
                    })
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        val firstPlayer = GamePlayer()
        firstPlayer.name = "jerry"
        val currentGame = remember {
            mutableStateOf(GameScores())
        }
        currentGame.value.players.add(firstPlayer)

        WhoswinningreduxTheme {
            Scaffold {
                ScoreList(innerPadding = it, currentGame.value)
            }
        }
    }
}




