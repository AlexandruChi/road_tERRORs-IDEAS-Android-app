package com.roadterrors.ideas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.roadterrors.ideas.ui.theme.IDEASTheme

enum class ViewMode {LoginMenu, MainMenu, Race, Debug}
var viewMode: ViewMode by mutableStateOf(ViewMode.MainMenu)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IDEASTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoverScreen()
                }
            }
        }
    }
}

@Composable
fun RoverScreen(modifier: Modifier = Modifier) {
    Column {
        if (viewMode == ViewMode.MainMenu) {
            MainMenu()
        } else {
            Row() {
                Button(onClick = { viewMode = ViewMode.MainMenu }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back")
                }
                RoverControl()
            }
            PrintRoverData(
                debug = when (viewMode) {
                    ViewMode.Race -> false
                    ViewMode.Debug -> true
                    else -> {false}
                }
            )
        }
    }
}

@Composable
fun MainMenu(modifier: Modifier = Modifier) {
    Column() {
        Button(onClick = { viewMode = ViewMode.Race}) {
            Text(text = "RACE")
        }
        Button(onClick = { viewMode = ViewMode.Debug }) {
            Text(text = "DEBUG")
        }
    }
}

@Composable
fun RoverControl(modifier: Modifier = Modifier) {
    Row() {
        Button(onClick = { /*TODO*/ }) {
            Text(text = "START")
        }
        Button(onClick = { /*TODO*/ }) {
            Text(text = "STOP")
        }
    }
}

@Composable
fun PrintRoverData(debug: Boolean, modifier: Modifier = Modifier) {
    
}