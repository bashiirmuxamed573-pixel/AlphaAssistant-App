package com.kingalpha.kingalpha

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingalpha.kingalpha.security.AlphaDeviceAdminReceiver
import com.kingalpha.kingalpha.security.AlphaDevicePolicy
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        setContent {
            AlphaScreen(
                speak = { text ->
                    speakText(text)
                }
            )
        }
    }

    private fun speakText(text: String) {
        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "ALPHA_RESPONSE"
        )
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}

data class AlphaMessage(
    val text: String,
    val fromUser: Boolean
)

@androidx.compose.runtime.Composable
private fun AlphaScreen(
    speak: (String) -> Unit
) {
    val context = LocalContext.current

    val devicePolicy = remember {
        AlphaDevicePolicy(context)
    }

    var input by remember {
        mutableStateOf("")
    }

    val messages = remember {
        mutableStateListOf(
            AlphaMessage(
                text = "ALPHA is ready.",
                fromUser = false
            )
        )
    }

    val speechLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val spokenText =
                result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                input = spokenText
                executeAlphaCommand(
                    spokenText,
                    context,
                    devicePolicy,
                    messages,
                    speak
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {

        Text(
            text = "ALPHA",
            color = Color.Cyan,
            fontSize = 30.sp
        )

        Text(
            text = "READY",
            color = Color(0xFFFFD700),
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(messages) { message ->

                Text(
                    text = message.text,
                    color = if (message.fromUser) {
                        Color.White
                    } else {
                        Color.Cyan
                    },
                    fontSize = 16.sp
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Command")
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                onClick = {

                    if (input.isNotBlank()) {
                        executeAlphaCommand(
                            input,
                            context,
                            devicePolicy,
                            messages,
                            speak
                        )

                        input = ""
                    }
                }
            ) {
                Text("SEND")
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Button(
                onClick = {

                    val intent =
                        Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                        ).apply {

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                "so-SO"
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Speak to ALPHA"
                            )
                        }

                    speechLauncher.launch(intent)
                }
            ) {
                Text("SPEAK")
            }
        }
    }
}

private fun executeAlphaCommand(
    input: String,
    context: Context,
    devicePolicy: AlphaDevicePolicy,
    messages: MutableList<AlphaMessage>,
    speak: (String) -> Unit
) {

    messages.add(
        AlphaMessage(
            text = input,
            fromUser = true
        )
    )

    when (AlphaAI.processCommand(input)) {

        AlphaCommand.LOCK_DEVICE -> {

            if (devicePolicy.isAdminActive()) {

                val locked =
                    devicePolicy.lockDevice()

                if (locked) {
                    messages.add(
                        AlphaMessage(
                            text = "Telefoonka waa la xiray.",
                            fromUser = false
                        )
                    )

                    speak("Telefoonka waa la xiray.")
                }

            } else {

                messages.add(
                    AlphaMessage(
                        text = "ALPHA Security lama ogolaan. Fadlan Device Admin ka dhaqaaji.",
                        fromUser = false
                    )
                )

                speak(
                    "Fadlan marka hore ogolow ALPHA Security."
                )

                openDeviceAdminSettings(context)
            }
        }

        AlphaCommand.UNKNOWN -> {

            messages.add(
                AlphaMessage(
                    text = "Weli ma fahmin amarkaas.",
                    fromUser = false
                )
            )

            speak(
                "Weli ma fahmin amarkaas."
            )
        }
    }
}

private fun openDeviceAdminSettings(
    context: Context
) {

    val component =
        ComponentName(
            context,
            AlphaDeviceAdminReceiver::class.java
        )

    val intent =
        Intent(
            DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
        ).apply {

            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                component
            )

            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Allow ALPHA to lock the device."
            )
        }

    context.startActivity(intent)
}