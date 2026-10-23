package com.m.adacolorytrazo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class GameViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    var state by mutableStateOf(
        GameState(
            character = savedState.get<String>("character")?.let { name ->
                Character.entries.firstOrNull { it.name == name }
            },
            weather = savedState.get<String>("weather")?.let { name ->
                Weather.entries.firstOrNull { it.name == name }
            },
            selected = savedState.get<ArrayList<String>>("selected")
                .orEmpty().mapNotNull { name -> Item.entries.firstOrNull { it.name == name } }
                .filter { it.selectable }
                .toSet(),
            helperText = savedState["helperText"] ?: ""
        )
    )
        private set

    private fun update(newState: GameState) {
        state = newState
        savedState["character"] = newState.character?.name
        savedState["weather"] = newState.weather?.name
        savedState["selected"] = ArrayList(newState.selected.map { it.name })
        savedState["helperText"] = newState.helperText
    }

    fun setCharacter(character: Character) {
        update(state.copy(character = character, helperText = ""))
    }

    fun setWeather(weather: Weather, say: (String) -> Unit) {
        val question = ruleFor(weather).question
        update(state.copy(weather = weather, selected = emptySet(), helperText = question))
        say(question)
    }

    /** Spoken when the child taps a garment; it does not equip the garment. */
    fun speakItem(item: Item, say: (String) -> Unit) {
        if (item.selectable) say(spokenItemName(item))
    }

    /** Called only after the garment has been dragged onto Ada. */
    fun dropItem(item: Item, say: (String) -> Unit): Boolean {
        val weather = state.weather ?: return false
        if (item !in clothingChoicesFor(weather)) return false

        if (!suitableForWeather(weather, item)) {
            val message = "Esa prenda no es para este clima. Intenta otra vez."
            update(state.copy(helperText = "Intenta otra vez."))
            say(message)
            return false
        }

        val selected = replaceInSlot(state.selected, item)
        val complete = outfitComplete(weather, selected)
        val message = if (complete) {
            "¡Muy bien! Ada ya está lista. ¡Lo hiciste genial!"
        } else {
            "¡Muy bien! ${spokenItemName(item)}. Busquemos otra prenda."
        }
        update(state.copy(selected = selected, helperText = message))
        say(message)
        return true
    }
}
