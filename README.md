# I-A-M Studio

Estudio de producción musical multipista para Android (WebView) y navegador compatible.

## Funciones implementadas en esta versión

- Interfaz rediseñada como consola de audio táctil para usar el celular en horizontal; la APK solicita orientación horizontal.
- Canales tipo hardware con faders verticales y perillas giratorias táctiles para graves, medios, agudos y panorama.
- Espectrómetro gráfico central conectado al analizador de audio real del master (frecuencias en tiempo real, no una animación decorativa).
- Acceso rápido desde la consola a importar audio, agregar instrumento, automatizar, guardar y exportar.
- Control master general.

- Biblioteca musical que abre el selector de archivos del celular para elegir canciones y pistas de audio.
- Reproductor de referencia independiente de la consola, con lista de pistas importadas y controles propios; el master exportado también queda cargado para escucharlo.
- Importar varias pistas de audio y verlas en una línea de tiempo multipista.
- Reproducir y detener la mezcla sincronizada.
- Consola con volumen, panorama, graves, medios y agudos por pista.
- Mute y Solo por pista.
- Grabación de automatización: mientras la canción reproduce, los cambios de volumen, panorama y ecualización se guardan con su posición temporal.
- Instrumentos virtuales básicos (piano/seno, órgano, bajo, cuerdas y sintetizador) tocables desde un teclado en pantalla.
- Eventos de notas internos al tocar el teclado con la grabación armada y la reproducción activa.
- Control de tempo (BPM), loop y zoom de línea de tiempo.
- Guardar sesión .iam y restaurar la última sesión local del dispositivo mediante IndexedDB/localStorage.
- Exportar mezcla WAV estéreo sin pérdida: PCM de 24 bits o flotante de 32 bits, a 48 kHz o 96 kHz.
- Revisión del master final en el reproductor independiente.
- Procesamiento local; no se envían las canciones a un servidor.

## Uso rápido

1. Pulsa Importar audio y selecciona una o varias pistas.
2. Pulsa Reproducir para escuchar la mezcla.
3. Activa Grabar automatización y mueve los faders o la ecualización mientras suena.
4. Detén la grabación; los movimientos quedan asociados al segundo en que los hiciste.
5. Agrega un instrumento virtual, selecciona su sonido y toca las teclas en pantalla.
6. Usa la biblioteca y su reproductor para escuchar las pistas de referencia sin detener la consola.
7. En Exportación Master, elige 24-bit PCM para entrega de alta calidad o 32-bit flotante para conservar margen de masterización; selecciona 48 kHz o 96 kHz según el destino.
8. Exporta el WAV y escúchalo desde el reproductor independiente.

## Guardado de proyectos

El archivo .iam conserva la estructura de pistas, controles, notas y automatizaciones. Las muestras de audio se almacenan localmente en el dispositivo. Para abrir el proyecto en otro dispositivo, los archivos de audio originales deberán importarse allí; el archivo .iam no incorpora dentro de sí todos los audios pesados.

## Alcance y limitaciones conocidas

- Esta es una primera versión funcional del estudio, no sustituye todavía a una DAW profesional.
- Los instrumentos virtuales son sintetizadores básicos; todavía no incluyen bibliotecas de muestras realistas de piano, batería, guitarra o instrumentos acústicos.
- La grabación de notas del teclado es una secuencia interna simplificada; no es todavía un editor MIDI completo.
- La automatización registra y aplica movimientos de volumen, panorama y ecualización durante la reproducción y en el render WAV.
- La biblioteca abre el selector de archivos de Android; por seguridad, el usuario elige los archivos que la app puede leer, no se escanea todo el almacenamiento automáticamente.
- La exportación a 96 kHz no recupera detalle que no exista en los archivos originales; para la mayoría de entregas musicales, 48 kHz / 24-bit PCM es una opción de alta calidad.
- No incluye aún grabación de micrófono, plugins VST/AU, separación de stems, time-stretch independiente, cuantización avanzada ni exportación MP3.
- Los formatos de audio que se pueden importar dependen del decodificador disponible en Android WebView.
- La cantidad de pistas y duración máxima dependen de la memoria disponible en el dispositivo.

## Compilación Android

GitHub Actions construye el APK de depuración con Java 17, Android SDK 35 y Gradle 8.9. Abre Actions, selecciona Construir I-A-M MEZCLADOR MUSICAL APK y descarga el artefacto IAM-MEZCLADOR-MUSICAL-APK.
