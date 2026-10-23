# Ada, Color y Trazo

Aplicación educativa Android en Kotlin y Jetpack Compose. Esta versión conserva las actividades y la estética pastel del proyecto original, y corrige recursos de personajes, interacción, animaciones, guardado y ciclo de vida.

## Personajes didácticos

Los nombres propios se mantienen tanto en la interfaz como en la voz. El identificador del dibujo es independiente del nombre mostrado: Tobías usa el perro; Confeti, el gato; Wormi, el gusano; Cuco, el pollito; Flica, el caballo; Gingi, el pato; y Spunky, el cerdo. Son correspondencias adoptadas para reutilizar las ilustraciones existentes; se pueden ajustar en `ui/AnimalsFlow.kt` sin cambiar los nombres de los personajes.

## Correcciones incluidas

El confeti se sincroniza con cuadros de animación y ya no ejecuta un bucle ocupado en el hilo principal. Las animaciones decorativas del menú leen su estado durante dibujo o en capas gráficas, evitando recalcular el layout completo en cada cuadro.

El dibujo libre actualiza trazos mediante estado observable. El coloreado carga imágenes y rellena regiones fuera del hilo principal. La rasterización, compresión y escritura de PNG también se ejecutan fuera del hilo de interfaz.

Las dos pantallas usan un guardado común, con mensajes de resultado y limpieza de archivos o entradas incompletas. En Android 7–9 se solicita permiso de escritura cuando el usuario guarda; en Android 10 o posterior se usa `MediaStore` con publicación mediante `IS_PENDING`.

Se conserva la pantalla activa, el personaje/dibujo seleccionado y las sesiones de edición al rotar. Memorice cancela trabajos de partidas anteriores y normaliza un par incorrecto pendiente al restaurar. El botón Atrás vuelve al menú y la música se reactiva al regresar del segundo plano.

El menú y las paletas admiten desplazamiento. Las piezas del cuerpo parten dentro de una bandeja calculada según el tamaño de la ventana y su arrastre tiene límites. El armado continúa siendo libre, sin evaluar si las piezas quedaron en posiciones correctas.

Las ilustraciones originales de coloreado se mantienen en `res/drawable-nodpi`; se añadieron además 33 ilustraciones fijas de apariencia 3D para animales, frutas, verduras y transportes. Los selectores y reconocedores usan esas nuevas ilustraciones, y Memorice las prefiere cuando están disponibles. El editor sigue usando los dibujos lineales originales para poder rellenar regiones.

## Bienvenida de Ada

Al iniciar, se conserva la bienvenida animada de Ada en la banca; al terminar, continúa al menú principal. El ícono redondo del launcher se mantiene independiente de esa escena.

La bienvenida usa el video existente y mantiene la alternativa prevista por `SplashCute` cuando no se puede reproducir. El poster también se reutiliza para presentar a Ada dentro del menú.

## Rediseño visual inspirado en la referencia

El TikTok compartido de un rompecabezas infantil inspira la interacción táctil, los elementos grandes y el tono de jardín; no se copia su arte ni sus personajes. `ui/PlayfulGameUi.kt` incorpora cielo y colinas, encabezados compactos, paneles redondeados con relieve, objetivos con ilustración flotante, tarjetas grandes y estados de acierto/error.

El lenguaje visual se extendió a los tres reconocedores (animales, frutas/verduras y transportes), los selectores de dibujos, Colores, Memorice, armado del cuerpo, el juego del clima y los editores de dibujo/coloreado. Se quitó la barra superior redundante donde las pantallas ya ofrecen navegación propia; el menú ahora deja más espacio a sus actividades y destaca que funcionan sin Wi-Fi.

En los reconocedores, la respuesta correcta permanece visible y se bloquean nuevos toques hasta avanzar a una consigna distinta; el panel y la voz ya no se cruzan con el objetivo siguiente. Colores también conserva el acierto antes de cambiar de ronda. Se mantuvieron las reglas y recursos de juego existentes.

Los nombres visuales pueden conservar su emoji, pero la voz y las descripciones accesibles usan solo el nombre. Así TextToSpeech no vuelve a decir «moto» al leer el emoji de moto; el mismo ajuste se aplica a animales, frutas, verduras y pares de Memorice.

Memorice usa la foto completa de Ada en la banca con sus animales como reverso y no imprime consignas ni nombres sobre las cartas. Al girar aparece el dibujo; la voz da la instrucción y nombra los pares encontrados.

En **Armar el cuerpo**, la cara y el cabello son algo más grandes y los ojos y la boca se reducen; la pista desaparece al tocar la primera pieza y se conserva el área de agarre para arrastrar.

En **Vestir a Ada**, las elecciones de clima y prendas se presentan con símbolos y reciben guía hablada. Los lápices de la paleta de coloreado y los cuatro botones visuales de grosor funcionan también en el lienzo; Dibujo libre tiene trazos más gruesos y una goma con tamaño táctil.

**Límite visual:** los personajes de juego siguen siendo imágenes WebP estáticas con apariencia 3D. El pequeño movimiento añadido es una animación de interfaz; no equivale a modelos 3D interactivos. La bienvenida conserva su video 3D prerenderizado. Para dar el siguiente paso harían falta modelos/rigs 3D reales y una decisión sobre su integración en Android.

Los cambios están guardados en la copia local del repositorio y todavía no se han publicado en GitHub. La compilación se intentó, pero Gradle no encuentra un Android SDK (`ANDROID_HOME` y `ANDROID_SDK_ROOT` están sin configurar); por eso no llegó a compilar Kotlin ni se validó en un teléfono. Para probarlo, abre la raíz del proyecto con Android Studio y configura el SDK Android 34.

## Compilar y probar

Se requiere un JDK completo compatible con AGP 8.5.2 —se recomienda JDK 17—, el SDK Android 34 y Build Tools 34.0.0. Android Studio puede instalar estos componentes y crear `local.properties`; ese archivo es local y no se distribuye en el repositorio.

Abre **esta carpeta**, `AdaColoryTrazo`, como proyecto en Android Studio y sincroniza Gradle. Desde terminal, con el JDK y SDK configurados:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

En Windows se utiliza `gradlew.bat` en lugar de `./gradlew`.

El APK de prueba se genera en `app/build/outputs/apk/debug/app-debug.apk`. Es una compilación debug para probar, no una publicación en Google Play ni una versión firmada con una clave de producción.

## Límites y pruebas en dispositivo

El éxito de compilación y las pruebas unitarias no demuestran la fluidez en un celular concreto. Hay que probar arrastre, coloreado, guardado, permisos, rotación, retorno del segundo plano y tamaños de pantalla en dispositivos representativos.

La sesión editable de dibujo o coloreado se mantiene durante una recreación de configuración, pero no se persiste automáticamente en disco ante la muerte del proceso. Para conservar el resultado fuera de la sesión se debe guardar el PNG. El editor conserva el dibujo activo; al elegir otro dibujo se inicia una sesión de coloreado nueva para mantener el consumo de memoria acotado.

La referencia de Instagram se usó como orientación visual general; no se certifica una equivalencia 1:1 ni una tasa de cuadros concreta. La mejora de animaciones y tarjetas no sustituye la creación de modelos 3D reales.
