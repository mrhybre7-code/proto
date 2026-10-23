package com.m.adacolorytrazo

enum class Character { TOBIAS, CONFETI }
enum class Weather { SOLEADO, LLUVIA }

enum class ClothingSlot {
    TOP,
    BOTTOM,
    FOOTWEAR,
    OUTERWEAR,
    HEAD,
    EYES,
    NECK,
    HANDS,
    CARRY,
    SOCKS
}

enum class Item(
    val slot: ClothingSlot,
    val selectable: Boolean = true
) {
    // Ropa inicial: las prendas elegidas reemplazan la pieza de su mismo espacio.
    BASE_POLERA(ClothingSlot.TOP, selectable = false),
    PANTALON_NEGRO(ClothingSlot.BOTTOM, selectable = false),
    ZAPATILLAS_ROJAS(ClothingSlot.FOOTWEAR, selectable = false),

    GORRA(ClothingSlot.HEAD),
    LENTES(ClothingSlot.EYES),
    BOTAS(ClothingSlot.FOOTWEAR),
    CHAQUETA(ClothingSlot.OUTERWEAR),
    BUFANDA(ClothingSlot.NECK),
    GUANTES(ClothingSlot.HANDS),
    PARAGUAS(ClothingSlot.CARRY),
    POLERA(ClothingSlot.TOP),
    SHORT(ClothingSlot.BOTTOM),
    CHALAS(ClothingSlot.FOOTWEAR),
    CALCETA(ClothingSlot.SOCKS),
    GORRO(ClothingSlot.HEAD)
}

data class WeatherRule(
    val required: Set<Item>,
    val suitable: Set<Item>,
    val choices: List<Item>,
    val question: String,
    val spokenName: String
)

data class GameState(
    val character: Character? = null,
    val weather: Weather? = null,
    val selected: Set<Item> = emptySet(),
    val helperText: String = ""
)

private val sunnyClothes = setOf(Item.POLERA, Item.SHORT, Item.CHALAS)
private val rainyClothes = setOf(Item.CHAQUETA, Item.BOTAS, Item.PARAGUAS)
private val allClothingChoices = listOf(
    Item.POLERA,
    Item.CHAQUETA,
    Item.PARAGUAS,
    Item.SHORT,
    Item.BOTAS,
    Item.CHALAS
)

private val weatherRules: Map<Weather, WeatherRule> = mapOf(
    Weather.SOLEADO to WeatherRule(
        required = sunnyClothes,
        suitable = sunnyClothes,
        choices = allClothingChoices,
        question = "Hace sol y calor. ¿Qué ropa necesita Ada?",
        spokenName = "el sol"
    ),
    Weather.LLUVIA to WeatherRule(
        required = rainyClothes,
        suitable = rainyClothes,
        choices = allClothingChoices,
        question = "Está lloviendo. ¿Qué necesita Ada para salir?",
        spokenName = "la lluvia"
    )
)

val Recommended: Map<Weather, Set<Item>> = weatherRules.mapValues { it.value.required }

private val baseOutfit: Map<ClothingSlot, Item> = Item.entries
    .filterNot { it.selectable }
    .associateBy { it.slot }

fun ruleFor(weather: Weather): WeatherRule = weatherRules.getValue(weather)

fun clothingChoicesFor(weather: Weather): List<Item> = ruleFor(weather).choices

fun suitableForWeather(weather: Weather, item: Item): Boolean =
    item.selectable && item in ruleFor(weather).suitable

fun outfitComplete(weather: Weather, selected: Set<Item>): Boolean =
    ruleFor(weather).required.all { it in selected }

/** Adds a garment to its slot and removes any other selected garment in that same slot. */
fun replaceInSlot(current: Set<Item>, item: Item): Set<Item> {
    if (!item.selectable) return current
    return current.filterNot { it.selectable && it.slot == item.slot }.toSet() + item
}

/** Full visual outfit, including the base clothes that are replaced by chosen garments. */
fun effectiveOutfit(selected: Set<Item>): Map<ClothingSlot, Item> {
    val outfit = baseOutfit.toMutableMap()
    selected.filter { it.selectable }.forEach { item -> outfit[item.slot] = item }
    return outfit
}

fun spokenItemName(item: Item): String = when (item) {
    Item.GORRA -> "gorra"
    Item.LENTES -> "lentes"
    Item.BOTAS -> "botas"
    Item.CHAQUETA -> "chaqueta"
    Item.BUFANDA -> "bufanda"
    Item.GUANTES -> "guantes"
    Item.PARAGUAS -> "paraguas"
    Item.POLERA -> "polera"
    Item.SHORT -> "short"
    Item.CHALAS -> "chalas"
    Item.CALCETA -> "calceta"
    Item.GORRO -> "gorro"
    Item.BASE_POLERA -> "polera"
    Item.PANTALON_NEGRO -> "pantalón"
    Item.ZAPATILLAS_ROJAS -> "zapatillas"
}

fun helperForChoice(weather: Weather, selected: Set<Item>): String {
    if (outfitComplete(weather, selected)) {
        return "¡Listo! Ada está preparada para ${ruleFor(weather).spokenName}."
    }

    val missing = ruleFor(weather).required - selected
    val suggestion = missing.map(::spokenItemName).joinToString(", ")
    return "¡Vamos muy bien! Todavía falta $suggestion."
}
