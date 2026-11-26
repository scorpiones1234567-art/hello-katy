# I-A-M MEZCLADOR MUSICAL

Mezclador multipista para Android, con una interfaz web local integrada en una aplicación WebView.

## Funciones de esta primera versión

- Importar varios archivos de audio en una sola sesión.
- Reproducir las pistas sincronizadas desde el inicio.
- Control individual de volumen, panorama izquierda/derecha y silencio.
- Modo SOLO para escuchar una pista aislada.
- Ecualización individual de graves, medios y agudos.
- Ajuste individual de tono por semitonos.
- Retraso de inicio por pista para alinear las partes.
- Control maestro de volumen, velocidad y tono.
- Exportar la mezcla estéreo como WAV de 16 bits.

## Uso

1. Abre I-A-M y pulsa **SUBIR PISTAS**.
2. Selecciona varios instrumentos o voces.
3. Ajusta el volumen, panorama, ecualización, tono y retraso de cada pista.
4. Usa SOLO o SILENCIAR para comparar pistas.
5. Reproduce la mezcla y exporta el resultado WAV.

Los archivos originales no se sobrescriben. El procesamiento de audio se realiza en el dispositivo; no se suben canciones a un servidor.

## Limitaciones conocidas

- La exportación de esta primera versión es WAV; MP3 todavía no está incorporado.
- El control de velocidad cambia también el tono (no es time-stretch independiente).
- La alineación es manual mediante el control de retraso; no hay detección automática de BPM ni sincronización inteligente.
- El motor trabaja con los formatos que Android/WebView pueda decodificar.
- Los proyectos no se guardan todavía como sesiones editables al cerrar la aplicación.

## Compilación Android

GitHub Actions construye el APK de depuración con Java 17, Android SDK 35 y Gradle 8.9. En el repositorio, abre **Actions**, selecciona el flujo **Construir I-A-M MEZCLADOR MUSICAL APK** y descarga el artefacto `IAM-MEZCLADOR-MUSICAL-APK`.

