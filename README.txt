HELLO KATY — PROTOTIPO ANDROID 1

Este proyecto contiene cuatro minijuegos (memoria, vestir a Katy, pastelería y preguntas), respuestas habladas con la voz española disponible en Android y reconocimiento de voz mediante el servicio de voz del teléfono. No usa la voz oficial ni personajes/arte oficiales de Sanrio.

CÓMO OBTENER EL APK SIN INSTALAR ANDROID STUDIO:
1. Crea un repositorio nuevo en GitHub llamado hello-katy.
2. Sube TODO el contenido de esta carpeta (incluida la carpeta .github).
3. Abre la pestaña Actions del repositorio.
4. En “Crear APK Hello Katy”, pulsa Run workflow.
5. Cuando termine en verde, abre la ejecución y descarga el artefacto “Hello-Katy-APK”.
6. Descomprime el ZIP y abre app-debug.apk en tu Android. Permite instalar desde esa fuente si Android lo solicita.

También puedes compilar localmente con Gradle 8.9, JDK 17 y Android SDK instalado: gradle assembleDebug.

NOTAS: el permiso de micrófono se solicita cuando tocas el botón. La voz depende de los servicios de voz instalados en el teléfono y puede requerir conexión. Este proyecto es una primera versión de prueba, no un producto oficial de Sanrio.
