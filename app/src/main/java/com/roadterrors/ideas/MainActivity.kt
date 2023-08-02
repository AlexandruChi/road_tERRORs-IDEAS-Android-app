package com.roadterrors.ideas

import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.textInputServiceFactory
import androidx.compose.ui.unit.dp
import com.roadterrors.ideas.ui.theme.IDEASTheme
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

enum class ViewMode { Login, Main, Race, Debug, Test }

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

class Connection(private val ip: String, private val port: Int) : Thread() {
    private lateinit var socket: Socket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private var connected = false

    @Synchronized
    override fun run() {
        try {
            socket = Socket(ip, port)
            input = socket.getInputStream()
            output = socket.getOutputStream()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        connected = true
        while (connected) {
            connected = socket.isConnected
        }
    }

    @Synchronized
    fun close() {
        connected = false;
        socket.close()
    }

    @Synchronized
    fun isConnected(): Boolean {
        return connected
    }

    @Synchronized
    fun send(message: String) {
        output.write(message.toByteArray())
    }

    @Synchronized
    fun recv(): String {
        return input.readBytes().toString()
    }
}

@Composable
fun RoverScreen(modifier: Modifier = Modifier) {
    var viewMode: ViewMode by remember { mutableStateOf(ViewMode.Login) }
    val setViewMode: (ViewMode) -> Unit = { viewMode = it }

    var printErrorMessage: String? by remember { mutableStateOf(null) }

    var connection: Connection? by remember { mutableStateOf(null) }
    val connect: () -> Boolean = {
        connection =
            Connection("192.168.0.1", 4000); connection!!.start(); connection!!.isConnected()
    }
    val exit: () -> Boolean = { connection?.close(); connection = null; true }
    val test: () -> Boolean = { true }
    val printError: (String) -> Unit = { printErrorMessage = it }

    if (viewMode != ViewMode.Login) {
        if (!connection!!.isConnected()) {
            connection!!.close()
            connection = null
            viewMode = ViewMode.Login
            printErrorMessage = "Connection lost"
        }
    }

    when (viewMode) {
        ViewMode.Login -> {
            LoginMenu(
                login = connect, printError = printError, setViewMode = setViewMode
            )
        }

        ViewMode.Main -> {
            MainMenu(logout = exit, printError = printError, setViewMode = setViewMode)
        }

        ViewMode.Race -> {
            RoverControl(send = { connection?.send(it) },
                recv = { connection?.recv() ?: "" },
                test = test,
                printError = printError,
                debug = false,
                setViewMode = setViewMode
            )
        }

        ViewMode.Debug -> {
            RoverControl(send = { connection?.send(it) },
                recv = { connection?.recv() ?: "" },
                test = test,
                printError = printError,
                debug = true,
                setViewMode = setViewMode
            )
        }

        ViewMode.Test -> {
            TestMenu(setViewMode = setViewMode)
        }
    }

    if (printErrorMessage != null) {
        PrintError(message = printErrorMessage!!, dismiss = { printErrorMessage = null })
    }
}

@Composable
fun LoginMenu(
    login: () -> Boolean,
    setViewMode: (ViewMode) -> Unit,
    printError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
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
    printError: (String) -> Unit,
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
    test: () -> Boolean,
    printError: (String) -> Unit,
    debug: Boolean,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    spacerPatting: Int = 10
) {
    Column(modifier = modifier.fillMaxSize()) {
        ActionMenu(
            send = send,
            test = test,
            debug = debug,
            printError = printError,
            setViewMode = setViewMode
        )
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = if (debug) {
                    "debug"
                } else {
                    "race"
                }, modifier = modifier.padding(spacerPatting.dp)
            )
        }
        RoverData(send = send, recv = recv, debug = debug)
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
fun RoverData(
    send: (String) -> Unit,
    recv: () -> String,
    modifier: Modifier = Modifier,
    textPatting: Int = 10,
    cardPatting: Int = 25,
    debug: Boolean = false
) {
    var roverData: String by remember { mutableStateOf("") }

    send(
        if (debug) {
            "DEBUG"
        } else {
            "DATA"
        }
    )
    roverData = recv()

    Column(modifier = modifier.fillMaxSize()) {
        Card(
            modifier = modifier
                .padding(horizontal = cardPatting.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = roverData, modifier = modifier
                    .fillMaxWidth()
                    .padding(textPatting.dp)
            )
        }
    }
}

@Composable
fun ActionMenu(
    send: (String) -> Unit,
    test: () -> Boolean,
    debug: Boolean,
    printError: (String) -> Unit,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    buttonPatting: Int = 5
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = { setViewMode(ViewMode.Main) },
            modifier.padding(horizontal = buttonPatting.dp)
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }
        if (debug) {
            Button(
                onClick = {
                    if (test()) {
                        setViewMode(ViewMode.Test)
                    } else {
                        printError("Rover must be stopped")
                    }
                }, modifier.padding(horizontal = buttonPatting.dp)
            ) {
                Text(text = "TEST")
            }
        }
        Button(onClick = { send("START") }, modifier.padding(horizontal = buttonPatting.dp)) {
            Text(text = "START")
        }
        Button(onClick = { send("STOP") }, modifier.padding(horizontal = buttonPatting.dp)) {
            Text(text = "STOP")
        }
    }
}

@Composable
fun PrintError(message: String, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss,
        confirmButton = { Button(onClick = dismiss) { Text(text = "OK") } },
        text = { Text(text = message) })
}