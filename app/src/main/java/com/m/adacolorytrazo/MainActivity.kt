package com.m.adacolorytrazo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.m.adacolorytrazo.audio.BackgroundMusic
import com.m.adacolorytrazo.ui.AnimalsFlow
import com.m.adacolorytrazo.ui.BodyAssembleScreen
import com.m.adacolorytrazo.ui.ColorsFlow
import com.m.adacolorytrazo.ui.FreeDrawScreen
import com.m.adacolorytrazo.ui.FruitsFlow
import com.m.adacolorytrazo.ui.HomeMenuScreen
import com.m.adacolorytrazo.ui.MemoriceScreen
import com.m.adacolorytrazo.ui.RecognizeAnimalsScreen
import com.m.adacolorytrazo.ui.RecognizeProduceScreen
import com.m.adacolorytrazo.ui.RecognizeTransportScreen
import com.m.adacolorytrazo.ui.SplashCute
import com.m.adacolorytrazo.ui.TransportFlow

private enum class Screen {
    Splash, Menu, Animals, Fruits, Transport, Colors, FreeDraw,
    RecognizeAnimals, RecognizeProduce, RecognizeTransport,
    BodyAssemble, Memorice, WeatherGame
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                // Save the active section, but let a new launch begin with Ada's intro.
                var screen by rememberSaveable { mutableStateOf(Screen.Splash) }
                val stateHolder = rememberSaveableStateHolder()
                val context = LocalContext.current.applicationContext
                val lifecycleOwner = LocalLifecycleOwner.current
                val backToMenu = { screen = Screen.Menu }
                val isSplash = screen == Screen.Splash

                BackHandler(enabled = screen != Screen.Menu && screen != Screen.Splash) {
                    backToMenu()
                }

                DisposableEffect(lifecycleOwner, context, isSplash) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                if (isSplash) {
                                    BackgroundMusic.stop()
                                } else {
                                    BackgroundMusic.start(context)
                                }
                            }
                            Lifecycle.Event.ON_STOP -> BackgroundMusic.stop()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    if (isSplash) {
                        BackgroundMusic.stop()
                    } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        BackgroundMusic.start(context)
                    }
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        BackgroundMusic.stop()
                    }
                }

                if (screen == Screen.Splash) {
                    // SplashCute owns the edge-to-edge video surface and advances when
                    // the banca clip finishes (or when its accessible fallback is used).
                    SplashCute(onFinish = backToMenu)
                } else {
                    Scaffold(
                        topBar = {
                            if (screen == Screen.BodyAssemble) {
                                TopAppBar(
                                    title = {
                                        Text("Armar el cuerpo", fontWeight = FontWeight.Bold)
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = backToMenu) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Volver al menú"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            stateHolder.SaveableStateProvider(screen.name) {
                                when (screen) {
                                    Screen.Splash -> Unit
                                    Screen.Menu -> HomeMenuScreen(
                                        onAnimals = { screen = Screen.Animals },
                                        onFruits = { screen = Screen.Fruits },
                                        onTransport = { screen = Screen.Transport },
                                        onColors = { screen = Screen.Colors },
                                        onFreeDraw = { screen = Screen.FreeDraw },
                                        onRecognizeAnimals = { screen = Screen.RecognizeAnimals },
                                        onRecognizeFruits = { screen = Screen.RecognizeProduce },
                                        onRecognizeTransport = { screen = Screen.RecognizeTransport },
                                        onBodyAssemble = { screen = Screen.BodyAssemble },
                                        onWeatherGame = { screen = Screen.WeatherGame },
                                        onMemorice = { screen = Screen.Memorice }
                                    )
                                    Screen.Animals -> AnimalsFlow(onBack = backToMenu)
                                    Screen.Fruits -> FruitsFlow(onBack = backToMenu)
                                    Screen.Transport -> TransportFlow(onBack = backToMenu)
                                    Screen.Colors -> ColorsFlow(onBack = backToMenu)
                                    Screen.FreeDraw -> FreeDrawScreen(onBack = backToMenu)
                                    Screen.RecognizeAnimals -> RecognizeAnimalsScreen(onBack = backToMenu)
                                    Screen.RecognizeProduce -> RecognizeProduceScreen(onBack = backToMenu)
                                    Screen.RecognizeTransport -> RecognizeTransportScreen(onBack = backToMenu)
                                    Screen.BodyAssemble -> BodyAssembleScreen(onBack = backToMenu)
                                    Screen.WeatherGame -> GameScreen(onBack = backToMenu)
                                    Screen.Memorice -> MemoriceScreen(onBack = backToMenu)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
