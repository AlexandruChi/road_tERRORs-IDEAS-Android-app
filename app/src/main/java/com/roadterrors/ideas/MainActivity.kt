package com.roadterrors.ideas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roadterrors.ideas.ui.theme.IDEASTheme
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

enum class ViewMode {Login, Main, Race, Debug, Test}

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
    var viewMode: ViewMode by remember{ mutableStateOf(ViewMode.Login) }
    val setViewMode: (ViewMode) -> Unit = {viewMode = it}

    var socket: Socket? = null
    var input: InputStream? = null
    var output: OutputStream? = null
    val connect: () -> Boolean = {
        socket = Socket("192.168.0.1", 4000)
        input = socket!!.getInputStream()
        output = socket!!.getOutputStream()
        true
    }
    val disconnect: () -> Boolean = {socket?.close(); true}
    val send: (String) -> Unit = { output?.write(it.toByteArray()) }
    val recv: () -> String = { input?.readBytes().toString() }

    when (viewMode) {
        ViewMode.Login -> { LoginMenu(login = connect, setViewMode = setViewMode) }
        ViewMode.Main -> { MainMenu(logout = disconnect, setViewMode = setViewMode) }
        ViewMode.Race -> {
            RoverControl(send = send, recv = recv, debug = false, setViewMode = setViewMode)
        }
        ViewMode.Debug -> {
            RoverControl(send = send, recv = recv, debug = true, setViewMode = setViewMode)
        }
        ViewMode.Test -> { TestMenu(setViewMode = setViewMode) }
    }
}

@Composable
fun LoginMenu(
    login: () -> Boolean,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            if (login()) {
                setViewMode(ViewMode.Main)
            } else {
                printError("Can not connect")
            }
        }) {
            Text(text = "CONNECT")
        }
    }
}

@Composable
fun MainMenu(
    logout: () -> Boolean,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        Button(onClick = { setViewMode(ViewMode.Race) }) {
            Text(text = "RACE")
        }
        Button(onClick = { setViewMode(ViewMode.Debug) }) {
            Text(text = "DEBUG")
        }
        Button(onClick = {
            if (logout()) {
                setViewMode(ViewMode.Login)
            } else {
                printError("Stop rover before disconnecting")
            }
        }) {
            Text(text = "EXIT")
        }
    }
}

@Composable
fun RoverControl(
    send: (String) -> Unit,
    recv: () -> String,
    debug: Boolean,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column (modifier = modifier.fillMaxSize()) {
        ActionMenu(send = send, debug = debug, setViewMode = setViewMode)
        Row (modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = if (debug) {"debug"} else {"race"})
        }
        RoverData(debug = debug)
    }
}

@Composable
fun TestMenu(setViewMode: (ViewMode) -> Unit, modifier: Modifier = Modifier) {
    Column {
        Button(onClick = { setViewMode(ViewMode.Debug) }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }
    }
}

@Composable
fun RoverData(debug: Boolean, modifier: Modifier = Modifier) {

}

@Composable
fun ActionMenu(
    send: (String) -> Unit,
    debug: Boolean,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    buttonPatting: Int = 5
) {
    Row (modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = { setViewMode(ViewMode.Main) },
            modifier.padding(buttonPatting.dp)
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }
        if (debug) {
            Button(
                onClick = {
                    if (false) {
                        setViewMode(ViewMode.Test)
                    } else {
                        printError("Rover must be stopped")
                    }},
                modifier.padding(buttonPatting.dp)
            ) {
                Text(text = "TEST")
            }
        }
        Button(onClick = { /*TODO*/ }, modifier.padding(buttonPatting.dp)) {
            Text(text = "START")
        }
        Button(onClick = { /*TODO*/ }, modifier.padding(buttonPatting.dp)) {
            Text(text = "STOP")
        }
    }
}

@Composable
fun PrintRoverData(debug: Boolean, modifier: Modifier = Modifier) {
    
}

fun printError(message: String) {

}