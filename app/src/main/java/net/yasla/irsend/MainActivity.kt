package net.yasla.irsend

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
import android.hardware.ConsumerIrManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import kotlin.system.exitProcess

private lateinit var cirManager: ConsumerIrManager
private const val IR_FREQUENCY = 56000 // частота передачи: 38000 или 56000 (работает с обеими)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение сервиса ИК-порта
        cirManager = getSystemService(CONSUMER_IR_SERVICE) as ConsumerIrManager
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
fun Greeting(modifier: Modifier = Modifier) {
    val playerId = remember { mutableIntStateOf(1) } // 0 - 127
    val teamColor = remember { mutableIntStateOf(2) } // 0 - 3
    val damageValue = remember { mutableIntStateOf(9) } // 0 - 15

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { sendStartGame() }) {
            Text("Новая игра")
        }
        Button(onClick = { sendDamage(playerId.intValue, teamColor.intValue, damageValue.intValue) }) {
            Text("Выстрел")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = playerId.intValue.toString(),
                onValueChange = { input ->
                    val newValue = input.toIntOrNull()
                    if (newValue != null && newValue in 0..127) {
                        playerId.intValue = newValue
                    }
                },
                label = { Text("ID") },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Button(onClick = {
                if (playerId.intValue > 0) playerId.intValue--
            }) {
                Text("-")
            }
            Button(onClick = {
                if (playerId.intValue < 127) playerId.intValue++
            }) {
                Text("+")
            }
        }
        Row {
            Button(onClick = {
                if (teamColor.intValue == 3) teamColor.intValue = 0 else teamColor.intValue++
            }) {
                Text("цвет: ${teamColor.intValue}")
            }
            Button(onClick = {
                if (damageValue.intValue == 15) damageValue.intValue = 0 else damageValue.intValue++
            }) {
                Text("урон: ${damageValue.intValue}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IrsendTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Greeting(
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

        // Байт 2 (0x05 - новая игра, b00000101)
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        600, 600,
        1200, 600,
        600, 600,
        1200, 600,

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

// цвет команды (2 бита)
fun createArrayShiftTeam(number: Int): IntArray {
    if (number !in 0..3) {
        throw IllegalArgumentException("Число должно быть от 0 до 3")
    }

    val values = intArrayOf(600, 1200)

    return intArrayOf(
        values[number shr 1], 600, // Сдвигаем вправо на 1 бит
        values[number and 1], 600, // Берем младший бит
    )
}

// наносимый урон (4 бит)
fun createArrayShiftDamage(number: Int): IntArray {
    if (number !in 0..15) {
        throw IllegalArgumentException("Число должно быть от 0 до 15")
    }

    val values = intArrayOf(600, 1200)

    return intArrayOf(
        values[number shr 3], 600, // 4-й бит
        values[number shr 2 and 1], 600, // 3-й бит
        values[number shr 1 and 1], 600, // 2-й бит
        values[number and 1], 600, // 1-й бит
    )
}

// ID игрока (7 бит)
fun createArrayShiftPlayerId(number: Int): IntArray {
    if (number !in 0..127) {
        throw IllegalArgumentException("Число должно быть от 0 до 127")
    }

    val values = intArrayOf(600, 1200)

    return intArrayOf(
        values[number shr 6], 600, // 7-й бит
        values[number shr 5 and 1], 600, // 6-й бит
        values[number shr 4 and 1], 600, // 5-й бит
        values[number shr 3 and 1], 600, // 4-й бит
        values[number shr 2 and 1], 600, // 3-й бит
        values[number shr 1 and 1], 600, // 2-й бит
        values[number and 1], 600, // 1-й бит
    )
}

private fun sendDamage(playerId: Int, team: Int, damage: Int) {
    val pattern = intArrayOf(
        // Заголовок (2400 микросекунд)
        2400, 600,

        // 0 для выстрела
        600, 600,
    ) + createArrayShiftPlayerId(playerId) + createArrayShiftTeam(team) + createArrayShiftDamage(damage)
    sendCommand(pattern)
}
