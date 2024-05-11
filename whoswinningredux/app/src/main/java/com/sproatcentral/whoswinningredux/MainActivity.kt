package com.sproatcentral.whoswinningredux

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sproatcentral.whoswinningredux.ui.theme.WhoswinningreduxTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhoswinningreduxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScoreList(innerPadding = innerPadding)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScoreList(innerPadding: PaddingValues) {
        val currentGame = remember { mutableStateOf(GameScores()) }
        val activePlayerIndex = remember { mutableStateOf(-1) }
        val playersPlusAddCount = remember { mutableIntStateOf(0) }
        val listExpanded = remember { mutableStateOf(false) }
        val currentScoreList = remember { mutableStateOf(listOf<Int>()) }

        val showConfirmClose = remember { mutableStateOf(false) }
        val showSaveGame = remember { mutableStateOf(false) }
        val saveGameName = remember { mutableStateOf("") }
        val saveGame = remember { mutableStateOf(false) }
        val shareGame = remember { mutableStateOf(false) }

        val showRemove = remember { mutableStateOf(false) }
        val removeUser = remember { mutableStateOf(GamePlayer()) }
        val removeScore = remember { mutableStateOf(-1) }

        val nameFocusRequester = remember { FocusRequester() }
        val scoreFocusRequester = remember { FocusRequester() }

        var gameSaveImage = remember {
            mutableStateOf(ImageBitmap(500, 300))
        }

        fun updateImage() {
            gameSaveImage.value = ImageBitmap(500, 800)
            val canvas = Canvas(gameSaveImage.value.asAndroidBitmap())
            canvas.drawColor(Color.Gray.toArgb())
            val textPaint = Paint()
            textPaint.setColor(Color.Black.toArgb())
            textPaint.setTextSize(180f)
            canvas.drawText(saveGameName.value, 100f, 100f, textPaint)
        }

        fun addScore(playerIndex: Int, newScore: Int) {
            currentGame.value.players[playerIndex].addScore(newScore)
            currentScoreList.value = currentGame.value.players[playerIndex].scoreList
            listExpanded.value = true
            activePlayerIndex.value = -1
            Handler(Looper.getMainLooper()).postDelayed({
                activePlayerIndex.value = playerIndex
            }, 100L)
        }

        fun addPlayer(newName: String) {
            var newPlayer = GamePlayer()
            newPlayer.name = newName
            currentGame.value.players.add(newPlayer)
            activePlayerIndex.value =
                currentGame.value.players.size - 1
            playersPlusAddCount.value =
                currentGame.value.players.size
            currentScoreList.value = listOf<Int>()
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
                                Text(getString(R.string.save_game))
                                //Spacer(Modifier.weight(0.1f))
                                Row(horizontalArrangement = Arrangement.Center) {
                                    //Spacer(Modifier.weight(0.3f))
                                    Button(content = { Text(getString(R.string.yes)) },
                                        onClick = {
                                            saveGame.value = true
                                        }
                                    )
                                    Spacer(Modifier.weight(0.1f))
                                    Button(content = { Text(getString(R.string.no)) },
                                        onClick = {
                                            showConfirmClose.value = false
                                        }
                                    )
                                    //Spacer(Modifier.weight(0.1f))
                                }
                            } else {
                                Text(getString(R.string.save_game_name))
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
                                    Text(getString(R.string.share_game))
                                }
                                if (shareGame.value) {
                                    Image(
                                        gameSaveImage.value, "",
                                        modifier = Modifier.size(
                                            (500 / resources.getDisplayMetrics().density).dp,
                                            (800 / resources.getDisplayMetrics().density).dp
                                        )
                                    )

                                }
                                Button(
                                    onClick = {
                                        // save to database
                                        showConfirmClose.value = false
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
                            Text(getString(R.string.confirm_end))
                            //Spacer(Modifier.weight(0.1f))
                            Row(horizontalArrangement = Arrangement.Center) {
                                //Spacer(Modifier.weight(0.3f))
                                Button(content = { Text(getString(R.string.yes)) },
                                    onClick = {
                                        showSaveGame.value = true
                                    }
                                )
                                Spacer(Modifier.weight(0.1f))
                                Button(content = { Text(getString(R.string.no)) },
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

        if (showRemove.value) {
            AlertDialog(
                onDismissRequest = {
                    showRemove.value = false
                },
                title = { Text(getString(R.string.confirm_remove_score)) },
                dismissButton = {
                    Button(content = {
                        Text(getString(R.string.no))
                    },
                        onClick = {
                            showRemove.value = false
                        }
                    )
                },
                confirmButton = {
                    Button(content = {
                        Text(getString(R.string.yes))
                    },
                        onClick = {
                            removeUser.value.removeScore(removeScore.value)
                            currentScoreList.value = removeUser.value.scoreList
                            showRemove.value = false
                            listExpanded.value = true
                            val currentIndex = activePlayerIndex.value
                            activePlayerIndex.value = -1
                            Handler(Looper.getMainLooper()).postDelayed({
                                activePlayerIndex.value = currentIndex
                            }, 100L)
                        }
                    )
                }
            )
        }

        LazyRow(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxWidth(1.0f)
            //.border(2.dp, Color.Red)
        ) {
            items(playersPlusAddCount.value) { playerIndex ->
                if (playerIndex < currentGame.value.players.size) {
                    Column(
                        modifier = Modifier
                            .fillParentMaxWidth(
                                if (activePlayerIndex.value == playerIndex)
                                    0.35f
                                else
                                    0.15f
                            )//.border(2.dp, Color.Blue)
                            .padding(7.dp)
                    ) {
                        // show player
                        Text(
                            String.format(
                                getString(R.string.playerNameFormat),
                                if (currentGame.value.winningIndex.contains(playerIndex))
                                    "*" else "",
                                currentGame.value.players[playerIndex].name
                            ),
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                activePlayerIndex.value = playerIndex
                                listExpanded.value = false
                                currentScoreList.value =
                                    currentGame.value.players[activePlayerIndex.value].scoreList
                                Handler(Looper.getMainLooper()).postDelayed({
                                    scoreFocusRequester.requestFocus()
                                }, 100L)
                            }
                            //.border(2.dp, Color.Green)
                        )
                        if (listExpanded.value &&
                            activePlayerIndex.value == playerIndex
                        ) {
                            LazyColumn {
                                items(currentScoreList.value.size) { scoreIndex ->
                                    Text(
                                        currentScoreList.value[scoreIndex].toString(),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier
                                            .fillMaxWidth(1.0f)
                                            .clickable {
                                                removeUser.value =
                                                    currentGame.value.players[playerIndex]
                                                removeScore.value = scoreIndex
                                                showRemove.value = true
                                            }
                                    )
                                }
                            }
                        }
                        Text(currentGame.value.players[playerIndex].currentScore.toString(),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .clickable {
                                    if (activePlayerIndex.value == playerIndex) {
                                        listExpanded.value = listExpanded.value.not()
                                    } else {
                                        activePlayerIndex.value = playerIndex
                                        listExpanded.value = true
                                        currentScoreList.value =
                                            currentGame.value.players[activePlayerIndex.value].scoreList
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            scoreFocusRequester.requestFocus()
                                        }, 1000L)
                                    }
                                }
                                .fillMaxWidth(1.0f)
                            //.border(2.dp, Color.Yellow)
                        )
                        if (activePlayerIndex.value == playerIndex) {
                            var newScoreString =
                                remember { mutableStateOf(TextFieldValue("0")) }
                            val newScore = remember { mutableIntStateOf(0) }

                            TextField(newScoreString.value,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                                onValueChange = {
                                    if (it.text.toIntOrNull() != null) {
                                        newScore.value = it.text.toInt()
                                        newScoreString.value = newScoreString.value.copy(
                                            newScore.value.toString()
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
                                                newScore.value += 1
                                                newScoreString.value =
                                                    newScoreString.value.copy(
                                                        newScore.value.toString(),
                                                        selection = TextRange(
                                                            0,
                                                            newScore.value.toString().length
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
                                                newScore.value -= 1
                                                newScoreString.value =
                                                    newScoreString.value.copy(
                                                        newScore.value.toString(),
                                                        selection = TextRange(
                                                            0,
                                                            newScore.value.toString().length
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
                                        if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                                            addScore(
                                                playerIndex,
                                                newScore.value
                                            )
                                            newScore.value = 0
                                            newScoreString.value = newScoreString.value.copy(
                                                newScore.value.toString()
                                            )
                                            true
                                        }
                                        false
                                    }
                                    .focusRequester(scoreFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            val text = newScoreString.value.toString()
                                            newScoreString.value = newScoreString.value.copy(
                                                selection = TextRange(0, text.length)
                                            )
                                        }
                                    }
                            )
                            Button(
                                onClick = {
                                    addScore(playerIndex, newScore.value)
                                    newScore.value = 0
                                    newScoreString.value = newScoreString.value.copy(
                                        newScore.value.toString()
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
                Column() {
                    if (activePlayerIndex.value != -1) {
                        Icon(imageVector = Icons.Default.Settings,
                            contentDescription = "",
                            modifier = Modifier.clickable {
                                activePlayerIndex.value = -1
                                Handler(Looper.getMainLooper()).postDelayed({
                                    nameFocusRequester.requestFocus()
                                }, 100L)
                            }
                        )
                    } else {
                        BasicTextField(
                            newName.value,
                            onValueChange = { currentName: String ->
                                newName.value = currentName
                                if (currentGame.value.players.firstOrNull {
                                        it.name.startsWith(currentName)
                                    } != null) {
                                    repeatWarning.value = true
                                } else {
                                    repeatWarning.value = false
                                }
                            },
                            modifier = Modifier
                                .padding(5.dp)
                                .width(IntrinsicSize.Min)
                                .onKeyEvent {
                                    if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
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
                                placeholder = { Text(getString(R.string.name_hint)) },
                                innerTextField = it,
                                singleLine = true,
                                interactionSource = MutableInteractionSource(),
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
                                getString(R.string.name_exists),
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
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        var firstPlayer = GamePlayer()
        firstPlayer.name = "jerry"
        val currentGame = remember {
            mutableStateOf(GameScores())
        }
        currentGame.value.players.add(firstPlayer)

        WhoswinningreduxTheme {
            Scaffold {
                ScoreList(innerPadding = it)
            }
        }
    }
}




