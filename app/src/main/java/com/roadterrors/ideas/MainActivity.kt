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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roadterrors.ideas.ui.theme.IDEASTheme
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

enum class ViewMode { Login, Main, Race, Debug, Test }
enum class Tests { Photo }

class MainActivity : ComponentActivity() {
    private var connection: Connection = Connection("192.168.221.123", 4000)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IDEASTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoverScreen(connection)
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
    private var canConnect = false

    private val messageQueue: BlockingQueue<String> = LinkedBlockingQueue()
    private val receivedMessageQueue: BlockingQueue<String> = LinkedBlockingQueue()

    @Synchronized
    override fun run() {
        while (true) {
            if (canConnect) {
                break
            }
        }

        try {
            socket = Socket(ip, port)
            input = socket.getInputStream()
            output = socket.getOutputStream()
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        connected = check("CONNECT")

        while (connected) {
            val messageToSend = messageQueue.poll()
            if (messageToSend != null) {
                writeToOutput(messageToSend)
            }

            val receivedMessage = readFromInput()
            if (receivedMessage.isNotEmpty()) {
                receivedMessageQueue.offer(receivedMessage)
            }

            connected = isConnected()
        }
    }

    @Synchronized
    fun connect() {
        canConnect = true
        while (true) {
            if (connected) {
                return
            }
        }
    }

    @Synchronized
    fun check(connectString: String): Boolean {
        writeToOutput(connectString)
        return readFromInput() == connectString
    }

    @Synchronized
    fun close() {
        connected = false
        canConnect = false
        socket.close()
    }

    @Synchronized
    fun isConnected(): Boolean {
        return connected
    }

    @Synchronized
    fun send(message: String) {
        messageQueue.offer(message)
    }

    @Synchronized
    fun recv(): String {
        return receivedMessageQueue.poll() ?: ""
    }

    @Synchronized
    fun sendRecv(message: String): String {
        send(message)
        while (true) {
            val response = recv()
            if (response.isNotEmpty()) {
                return response
            }
        }
    }

    @Synchronized
    private fun writeToOutput(data: String) {
        try {
            val bytes = data.toByteArray()
            output.write(bytes)
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun readFromInput(): String {
        try {
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            return if (bytesRead > 0) {
                String(buffer, 0, bytesRead)
            } else {
                ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
}

@Composable
fun RoverScreen(connection: Connection) {
    var viewMode: ViewMode by remember { mutableStateOf(ViewMode.Login) }
    val setViewMode: (ViewMode) -> Unit = { viewMode = it }

    var printErrorMessage: String? by remember { mutableStateOf(null) }

    val connect: () -> Boolean = { connection.connect(); connection.isConnected()}
    val exit: () -> Boolean = { connection.close(); true }
    val test: () -> Boolean = { true }
    val printError: (String) -> Unit = { printErrorMessage = it }

    if (viewMode != ViewMode.Login) {
        if (!connection.isConnected()) {
            connection.close()
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
            RoverControl(send = { connection.send(it) },
                sendRecv = { connection.sendRecv(it) },
                test = test,
                printError = printError,
                debug = false,
                setViewMode = setViewMode
            )
        }

        ViewMode.Debug -> {
            RoverControl(send = { connection.send(it) },
                sendRecv = { connection.sendRecv(it) },
                test = test,
                printError = printError,
                debug = true,
                setViewMode = setViewMode
            )
        }

        ViewMode.Test -> {
            TestMenu(send = { connection.send(it) },
                setViewMode = setViewMode
            )
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
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = modifier.padding(horizontal = 5.dp)
) {
    Column {
        Button(onClick = { setViewMode(ViewMode.Race) }, modifier = buttonModifier) {
            Text(text = "RACE")
        }
        Button(onClick = { setViewMode(ViewMode.Debug) }, modifier = buttonModifier) {
            Text(text = "DEBUG")
        }
        Button(onClick = {
            if (logout()) {
                setViewMode(ViewMode.Login)
            } else {
                printError("Stop rover before disconnecting")
            }
        }, modifier = buttonModifier) {
            Text(text = "EXIT")
        }
    }
}

@Composable
fun RoverControl(
    send: (String) -> Unit,
    sendRecv: (String) -> String,
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
        RoverData(sendRecv = sendRecv, debug = debug)
    }
}

@Composable
fun TestMenu(
    send: (String) -> Unit,
    setViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = modifier.padding(horizontal = 5.dp)
) {
    var currentTest: Tests? by remember { mutableStateOf(null) }
    val setCurrentTest: (Tests?) -> Unit = { currentTest = it }

    when (currentTest) {
        null -> {
            Column {
                Button(onClick = { setViewMode(ViewMode.Debug) }, modifier = buttonModifier) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                }
                Button(onClick = { setCurrentTest(Tests.Photo) }, modifier = buttonModifier) {
                    Text(text = "PHOTO")
                }
            }
        }

        Tests.Photo -> {
            PhotoTest(send = send, setCurrentTest = setCurrentTest)
        }
    }
}

@Composable
fun PhotoTest(
    send: (String) -> Unit, setCurrentTest: (Tests?) -> Unit, modifier: Modifier = Modifier
) {
    var running: Boolean by remember { mutableStateOf(false) }

    DisposableEffect(running) {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                send("PHOTO")
            }
        }

        if (running) {
            timer.schedule(timerTask, 1000)
        }

        onDispose {
            timer.cancel()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { setCurrentTest(null) }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }
        if (!running) {
            Button(onClick = { running = true }) {
                Text(text = "START")
            }
        } else {
            Button(onClick = { running = false }) {
                Text(text = "STOP")
            }
        }
    }
}

@Composable
fun RoverData(
    sendRecv: (String) -> String,
    modifier: Modifier = Modifier,
    textPatting: Int = 10,
    cardPatting: Int = 25,
    debug: Boolean = false
) {
    var roverData: String by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                roverData = sendRecv(if (debug) {"DEBUG"} else {"DATA"})
            }
        }

        timer.schedule(timerTask, 1000)

        onDispose {
            timer.cancel()
        }
    }

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
    buttonModifier: Modifier = modifier.padding(horizontal = 5.dp)
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = { setViewMode(ViewMode.Main) }, modifier = buttonModifier
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
                }, modifier = buttonModifier
            ) {
                Text(text = "TEST")
            }
        }
        Button(
            onClick = { send("START") }, modifier = buttonModifier
        ) {
            Text(text = "START")
        }
        Button(
            onClick = { send("STOP") }, modifier = buttonModifier
        ) {
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