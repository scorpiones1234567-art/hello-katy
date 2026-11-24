# Avisos de licencias y atribuciones

## Motor de separación y transcripción Android

Este proyecto utiliza como base de procesamiento el proyecto abierto **MP3toMIDI** de roge-rm:

- Código fuente: https://github.com/roge-rm/MP3toMIDI
- Commit utilizado por la compilación: `6b86c32ad60641c940e524134bc198d9fac6a205`
- Licencia del proyecto base: MIT
- Copyright indicado en su licencia: Copyright (c) 2026 dan

El motor incorpora los siguientes componentes y modelos, según los avisos del proyecto original:

- Demucs `htdemucs_6s`: separación de batería, bajo, otros, voz, guitarra y piano. Licencia MIT.
- Spotify Basic Pitch: transcripción de notas a MIDI. Licencia Apache 2.0.
- YAMNet: clasificación de sonidos/instrumentos. Licencia Apache 2.0.
- ONNX Runtime Mobile: ejecución de modelos. Licencia MIT.
- TinySoundFont y FluidR3 GM: reproducción de instrumentos virtuales; consultar los avisos y licencias incluidos por sus respectivos autores.
- Oboe: motor de audio nativo. Licencia Apache 2.0.

Los pesos de modelos y el banco de sonidos se descargan desde las fuentes configuradas por el proyecto base cuando se necesitan. No se incluyen dentro de este repositorio.

Se conservan los avisos y licencias de cada componente en el código fuente de origen. Esta nota no sustituye esos avisos ni concede derechos adicionales sobre modelos o bancos de sonidos.
