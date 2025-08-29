package net.yasla.irsend

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.yasla.irsend.ui.theme.IrsendTheme
import android.hardware.ConsumerIrManager;
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlin.system.exitProcess

private lateinit var cirManager: ConsumerIrManager
private val IR_FREQUENCY = 56000 // частота передачи: 38000 или 56000 (работает с обеими)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение сервиса ИК-порта
        cirManager = getSystemService(Context.CONSUMER_IR_SERVICE) as ConsumerIrManager
        // Проверка наличия ИК-порта
        if (!cirManager.hasIrEmitter()) {
            showErrorAndExit("ИК-порт недоступен")
            return
        }

        enableEdgeToEdge()
        setContent {
            IrsendTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Привет, ${name}!")
        Button(onClick = { sendStartGame() }) {
            Text("Старт игры")
        }
        Button(onClick = { sendDamage(0x10, 0x1001) }) {
            Text("Выстрел")
        }
    }
    /*Text(
        text = "Hello $name!",
        modifier = modifier
    )*/
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IrsendTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Greeting(
                name = "Android",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        }
    }
}

// Обработка ошибок
private fun showErrorAndExit(message: String) {
    // Здесь можно добавить отображение сообщения пользователю
    println("Ошибка: $message")
    exitProcess(0)
}

// Функция отправки сигнала
private fun sendCommand(pattern: IntArray) {
    if (cirManager.hasIrEmitter()) {
        cirManager.transmit(IR_FREQUENCY, pattern)
    } else {
        showErrorAndExit("Не удалось отправить сигнал")
    }
}

// Базовые команды
private fun sendStartGame() {
    val pattern = intArrayOf(
        // Заголовок (2400 микросекунд)
        2400, 600,

        // Байт 1 (0x83 - команда, b10000011)
        1200, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        1200, 600,
        1200, 600,

        // Байт 2 (0x02 - старт игры, b00000010)
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        1200, 600,
        600, 600,

        // Байт 3 (0xE8 - конец пакета, b11101000)
        1200, 600,
        1200, 600,
        1200, 600,
        600, 600,
        1200, 600,
        600, 600,
        600, 600,
        600, 600,
    )
    sendCommand(pattern)
}

private fun sendDamage(team: Int, damage: Int) {
    val pattern = intArrayOf(
        //2400, 600, team, 600, damage, 600, 0xE8, 600
        // Заголовок (2400 микросекунд)
        2400, 600,

        // 0 для выстрела
        600, 600,

        // ID игрока (7 бит)
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        1200, 600,

        // цвет команды (2 бита)
        1200, 600,
        600, 600,

        // наносимый урон (4 бит)
        1200, 600,
        600, 600,
        600, 600,
        1200, 600,
    )
    sendCommand(pattern)
}
