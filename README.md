# I-A-M REAL MUSIC STUDIO

Aplicación Android para estudiar canciones completas, separar fuentes de audio, transcribir partes instrumentales a MIDI y reproducir MIDI con SoundFonts.

## Motor integrado

La compilación utiliza el proyecto abierto MP3toMIDI como motor de separación/transcripción, fijado al commit:

`6b86c32ad60641c940e524134bc198d9fac6a205`

Repositorio: https://github.com/roge-rm/MP3toMIDI

La aplicación puede procesar seis fuentes Demucs: drums, bass, other, vocals, guitar y piano; detectar notas y golpes de batería; estimar tempo; revisar instrumentos detectados; exportar MIDI y reproducirlo con un SoundFont.

## Primer uso y requisitos

- Android 8.1 o superior (minSdk 27).
- El primer procesamiento necesita Internet para descargar modelos (Demucs aprox. 235 MB y YAMNet aprox. 16 MB).
- El reproductor descarga su SoundFont predeterminado (aprox. 148 MB) cuando se usa por primera vez.
- Después de descargar los recursos, el procesamiento puede realizarse sin conexión.
- El procesamiento puede consumir cientos de MB de RAM y tardar varios minutos, según la canción y el teléfono.

## Precisión

La separación de pistas y la transcripción MIDI son estimaciones automáticas. Las notas extraídas pueden contener errores, especialmente en mezclas densas. El flujo permite revisar resultados antes de exportarlos; no garantiza una reconstrucción idéntica ni que todos los instrumentos sean identificados correctamente.

La voz se conserva como una pista separada dentro del resultado; no se modifica al generar las pistas MIDI.

## Compilación

GitHub Actions descarga el proyecto de motor en el commit indicado, aplica el identificador y nombre I-A-M y construye el APK de depuración.

Consulta `THIRD_PARTY_NOTICES.md` para las licencias y atribuciones.


Compilación de motor integrada: revisión inicial del workflow Android.

Configuración de Android SDK actualizada para la compilación automatizada.

El flujo de compilación utiliza la plataforma Android estable 36 en el entorno de CI.
