# Experiencia infantil de Ada (1–3 años)

## Implementado

- [x] Conservar la bienvenida animada de Ada en la banca y continuar al menú al terminar; mantener aparte el ícono del launcher.
- [x] Mostrar a Ada en el menú y usar su carta ilustrada como señal visual tanto para Memorice como para Vestir según el clima.
- [x] Hacer que el juego del clima muestre a Ada, superponga las prendas elegidas y dé ayuda hablada desde el inicio y durante la actividad.
- [x] Usar la foto de Ada en la banca con sus animales como reverso de Memorice, sin instrucciones o nombres escritos sobre las cartas; guía y nombres de pares por voz.
- [x] Separar el nombre hablado del emoji visual para evitar que TextToSpeech repita el vehículo, animal o fruta; aplicar la misma corrección a los tres reconocedores y Memorice.
- [x] Reducir ojos y boca y dar algo más de tamaño a cara/cabello en Armar el cuerpo; la pista desaparece con el primer toque a una pieza y sigue funcionando el arrastre.
- [x] Convertir las muestras de la paleta de coloreado en lápices visuales y ofrecer cuatro grosores reales de pincel.
- [x] Hacer más gruesos los trazos de Dibujo libre y aumentar la superficie táctil de la goma, pinceles y controles.
- [x] Aplicar una identidad visual lúdica y movimientos sutiles a menú, selectores, juegos y lienzos, sin añadir consignas textuales nuevas como elementos decorativos.

## Pendiente de verificación

- [ ] Compilar con un JDK completo y Android SDK 34; probar en pantalla pequeña y dispositivo real: voz, splash, rotación, arrastre, coloreado, goma, guardado y Memorice.
- [ ] Para 3D real durante la partida, preparar modelos/rigs e integrar un renderizador Android. Los recursos `*_icon_3d.webp` son ilustraciones estáticas; las animaciones Compose son movimientos de interfaz, no modelos 3D.
