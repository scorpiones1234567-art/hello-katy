from pathlib import Path

path = Path("app/src/main/java/com/rm/mp3tomidi/ui/PlayScreen.kt")
source = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global source
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"{label}: se esperaba 1 coincidencia, encontradas {count}")
    source = source.replace(old, new, 1)


replace_once(
    "import androidx.compose.foundation.layout.Arrangement\n",
    "import androidx.compose.foundation.layout.Arrangement\nimport androidx.compose.foundation.layout.Box\n",
    "import Box",
)
replace_once(
    "import androidx.compose.material3.MaterialTheme\n",
    "import androidx.compose.material3.DropdownMenu\nimport androidx.compose.material3.DropdownMenuItem\nimport androidx.compose.material3.MaterialTheme\n",
    "imports del menú",
)
replace_once(
    "import com.rm.mp3tomidi.R\n",
    "import com.rm.mp3tomidi.R\nimport com.rm.mp3tomidi.audio.SoundEngine\nimport com.rm.mp3tomidi.midi.GmInstrument\n",
    "imports de audio",
)
replace_once(
    "import kotlinx.coroutines.launch\n",
    "import kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\n",
    "import delay",
)
replace_once(
    "    var soundFontDownloadProgress by remember { mutableStateOf<Float?>(null) }\n",
    "    var soundFontDownloadProgress by remember { mutableStateOf<Float?>(null) }\n"
    "    var soundFontReady by remember { mutableStateOf(false) }\n"
    "    var selectedInstrument by remember { mutableStateOf(GmInstrument.ACOUSTIC_GRAND_PIANO) }\n"
    "    var instrumentMenuExpanded by remember { mutableStateOf(false) }\n",
    "estado del catálogo",
)
replace_once(
    "        viewModel.soundEngine.ensureLoaded { fraction -> soundFontDownloadProgress = fraction }\n"
    "        soundFontDownloadProgress = null\n",
    "        viewModel.soundEngine.ensureLoaded { fraction -> soundFontDownloadProgress = fraction }\n"
    "        soundFontDownloadProgress = null\n"
    "        soundFontReady = true\n",
    "estado del banco",
)

ui_anchor = """                    if (usingCustomSoundFont) {
                        OutlinedActionButton(
                            text = stringResource(R.string.reset_to_default_soundfont),
                            icon = Icons.Filled.Refresh,
                            onClick = {
                                scope.launch {
                                    soundFontDownloadProgress = 0f
                                    viewModel.soundEngine.resetToDefault({ f -> soundFontDownloadProgress = f }) { }
                                    soundFontDownloadProgress = null
                                }
                            },
                        )
                    }
                }
"""
ui_addition = """                    if (usingCustomSoundFont) {
                        OutlinedActionButton(
                            text = stringResource(R.string.reset_to_default_soundfont),
                            icon = Icons.Filled.Refresh,
                            onClick = {
                                scope.launch {
                                    soundFontDownloadProgress = 0f
                                    viewModel.soundEngine.resetToDefault({ f -> soundFontDownloadProgress = f }) { }
                                    soundFontDownloadProgress = null
                                }
                            },
                        )
                    }
                }

                SectionCard(title = "Catálogo de instrumentos") {
                    Text(
                        "Explora los 128 instrumentos General MIDI del banco FluidR3 y escucha una muestra antes de usarlos en tus pistas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Box {
                        OutlinedActionButton(
                            text = GmInstrument.NAMES[selectedInstrument],
                            icon = Icons.Filled.LibraryMusic,
                            onClick = { instrumentMenuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = instrumentMenuExpanded,
                            onDismissRequest = { instrumentMenuExpanded = false },
                        ) {
                            GmInstrument.NAMES.forEachIndexed { program, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedInstrument = program
                                        instrumentMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedActionButton(
                        text = "ESCUCHAR INSTRUMENTO",
                        icon = Icons.Filled.PlayArrow,
                        enabled = soundFontReady,
                        onClick = {
                            scope.launch {
                                viewModel.soundEngine.noteOn(
                                    SoundEngine.MELODIC_BANK,
                                    selectedInstrument,
                                    60,
                                    0.85f,
                                )
                                delay(700)
                                viewModel.soundEngine.noteOff(
                                    SoundEngine.MELODIC_BANK,
                                    selectedInstrument,
                                    60,
                                )
                            }
                        },
                    )
                    OutlinedActionButton(
                        text = "PROBAR BATERÍA",
                        icon = Icons.Filled.PlayArrow,
                        enabled = soundFontReady,
                        onClick = {
                            scope.launch {
                                val bank = SoundEngine.PERCUSSION_BANK
                                val hits = listOf(36, 38, 42, 38, 36, 42)
                                for (note in hits) {
                                    viewModel.soundEngine.noteOn(bank, 0, note, 0.9f)
                                    delay(110)
                                    viewModel.soundEngine.noteOff(bank, 0, note)
                                    delay(90)
                                }
                            }
                        },
                    )
                    Text(
                        "Los sonidos se cargan desde el banco de muestras FluidR3 GM. La primera descarga requiere Internet; después queda guardado en el teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
"""
if source.count(ui_anchor) != 1:
    raise SystemExit(f"bloque SoundFont: se esperaba 1 coincidencia, encontradas {source.count(ui_anchor)}")
source = source.replace(ui_anchor, ui_addition, 1)

path.write_text(source, encoding="utf-8")
print("Catálogo de 128 instrumentos y prueba de batería añadidos.")
