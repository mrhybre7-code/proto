package com.m.adacolorytrazo.ui

fun prettyName(category: String, id: String): String {
    return when (category) {

        // 🍓 FRUTAS
        "frutas" -> when (id) {
            "platano" -> "Plátano 🍌"
            "frutilla" -> "Frutilla 🍓"
            "sandia" -> "Sandía 🍉"
            "pina" -> "Piña 🍍"
            "naranja" -> "Naranja 🍊"
            "cereza" -> "Cereza 🍒"
            "durazno" -> "Durazno 🍑"
            "manzana" -> "Manzana 🍎"
            "pera" -> "Pera 🍐"
            "uva" -> "Uva 🍇"
            else -> id.replaceFirstChar { it.uppercase() }
        }

        // 🥦 VERDURAS
        "verduras" -> when (id) {
            "zanahoria" -> "Zanahoria 🥕"
            "tomate" -> "Tomate 🍅"
            "brocoli" -> "Brócoli 🥦"
            "pepino" -> "Pepino 🥒"
            "papa" -> "Papa 🥔"
            else -> id.replaceFirstChar { it.uppercase() }
        }

        // 🚗 TRANSPORTES
        "transportes" -> when (id) {
            "bici" -> "Bicicleta 🚲"
            "auto" -> "Auto 🚗"
            "avion" -> "Avión ✈️"
            "barco" -> "Barco ⛴️"
            "moto" -> "Moto 🏍️"
            "ambulancia" -> "Ambulancia 🚑"
            "policia" -> "Policía 🚓"
            "tren" -> "Tren 🚆"
            else -> id.replaceFirstChar { it.uppercase() }
        }

        // 🐾 ANIMALES
        "animales" -> when (id) {
            "gato" -> "Gato 🐱"
            "pollito" -> "Pollito 🐥"
            "caballo" -> "Caballo 🐴"
            "pato" -> "Pato 🦆"
            "cerdo" -> "Cerdo 🐷"
            "perro" -> "Perro 🐶"
            "gusano" -> "Gusano 🪱"
            "vaca" -> "Vaca 🐮"
            "oveja" -> "Oveja 🐑"
            "conejo" -> "Conejo 🐰"
            else -> id.replaceFirstChar { it.uppercase() }
        }

        else -> id.replaceFirstChar { it.uppercase() }
    }
}

/** Nombre limpio para voz/TalkBack: los emojis quedan solo como apoyo visual. */
fun spokenName(category: String, id: String): String {
    val displayName = prettyName(category, id)
    val suffix = displayName.substringAfterLast(' ')
    return if (displayName.contains(' ') && suffix.none { it.isLetterOrDigit() }) {
        displayName.substringBeforeLast(' ')
    } else {
        displayName
    }
}
