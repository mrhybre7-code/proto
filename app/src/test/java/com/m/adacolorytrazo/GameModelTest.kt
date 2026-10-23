package com.m.adacolorytrazo

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class GameModelTest {
    @Test
    fun onlySunAndRainAreAvailable() {
        assertEquals(setOf(Weather.SOLEADO, Weather.LLUVIA), Weather.entries.toSet())
    }

    @Test
    fun bothWeatherOptionsShowTheSameSixLargeClothingChoices() {
        val expected = setOf(
            Item.POLERA, Item.SHORT, Item.CHALAS,
            Item.CHAQUETA, Item.BOTAS, Item.PARAGUAS
        )
        Weather.entries.forEach { weather ->
            assertEquals(expected, clothingChoicesFor(weather).toSet())
        }
    }

    @Test
    fun sunRequiresOnlyShirtShortsAndSandals() {
        val required = setOf(Item.POLERA, Item.SHORT, Item.CHALAS)
        assertEquals(required, Recommended.getValue(Weather.SOLEADO))
        assertTrue(outfitComplete(Weather.SOLEADO, required))
        assertFalse(outfitComplete(Weather.SOLEADO, required - Item.CHALAS))
    }

    @Test
    fun rainRequiresOnlyJacketBootsAndUmbrella() {
        val required = setOf(Item.CHAQUETA, Item.BOTAS, Item.PARAGUAS)
        assertEquals(required, Recommended.getValue(Weather.LLUVIA))
        assertTrue(outfitComplete(Weather.LLUVIA, required))
    }

    @Test
    fun allWeatherRulesHaveThreeCorrectChoicesAndThreeDistractors() {
        Weather.entries.forEach { weather ->
            val rule = ruleFor(weather)
            assertEquals(3, rule.required.size)
            assertEquals(3, rule.choices.count { it in rule.suitable })
            assertEquals(3, rule.choices.count { it !in rule.suitable })
        }
    }

    @Test
    fun shortsAndSandalsReplaceBasePantsAndShoes() {
        val outfit = effectiveOutfit(setOf(Item.SHORT, Item.CHALAS))
        assertEquals(Item.SHORT, outfit[ClothingSlot.BOTTOM])
        assertNotEquals(Item.PANTALON_NEGRO, outfit[ClothingSlot.BOTTOM])
        assertEquals(Item.CHALAS, outfit[ClothingSlot.FOOTWEAR])
        assertNotEquals(Item.ZAPATILLAS_ROJAS, outfit[ClothingSlot.FOOTWEAR])
    }

    @Test
    fun sameSlotGarmentsReplaceOneAnotherAndDifferentSlotsStaySeparate() {
        val outfit = replaceInSlot(setOf(Item.BOTAS, Item.CHAQUETA), Item.CHALAS)
        assertFalse(Item.BOTAS in outfit)
        assertTrue(Item.CHALAS in outfit)
        assertTrue(Item.CHAQUETA in outfit)
    }

    @Test
    fun rainHintSuggestsOnlyTheMissingUmbrella() {
        val text = helperForChoice(Weather.LLUVIA, setOf(Item.BOTAS, Item.CHAQUETA))
        assertTrue(text.contains("paraguas"))
        assertFalse(text.contains("botas"))
        assertFalse(text.contains("chaqueta"))
    }

    @Test
    fun wrongSunnyWeatherDropIsRejectedWithoutChangingOutfit() {
        val game = GameViewModel(SavedStateHandle())
        val spoken = mutableListOf<String>()
        game.setWeather(Weather.SOLEADO) { spoken += it }
        val before = game.state.selected

        assertFalse(game.dropItem(Item.BOTAS) { spoken += it })
        assertEquals(before, game.state.selected)
        assertTrue(spoken.last().contains("Intenta otra vez"))
    }

    @Test
    fun sunnyWeatherDropCompletesWithShirtShortsAndSandals() {
        val game = GameViewModel(SavedStateHandle())
        val spoken = mutableListOf<String>()
        game.setWeather(Weather.SOLEADO) { spoken += it }
        assertTrue(spoken.last().contains("sol"))

        game.speakItem(Item.SHORT) { spoken += it }
        assertEquals("short", spoken.last())
        assertTrue(game.dropItem(Item.POLERA) { spoken += it })
        assertFalse(outfitComplete(Weather.SOLEADO, game.state.selected))
        assertTrue(game.dropItem(Item.SHORT) { spoken += it })
        assertTrue(game.dropItem(Item.CHALAS) { spoken += it })
        assertTrue(outfitComplete(Weather.SOLEADO, game.state.selected))
        assertTrue(spoken.last().contains("lista"))
    }

    @Test
    fun rainDropCompletesWithJacketBootsAndUmbrella() {
        val game = GameViewModel(SavedStateHandle())
        val spoken = mutableListOf<String>()
        game.setWeather(Weather.LLUVIA) { spoken += it }
        assertTrue(spoken.last().contains("lloviendo"))

        assertTrue(game.dropItem(Item.CHAQUETA) { spoken += it })
        assertTrue(game.dropItem(Item.BOTAS) { spoken += it })
        assertTrue(game.dropItem(Item.PARAGUAS) { spoken += it })
        assertTrue(outfitComplete(Weather.LLUVIA, game.state.selected))
        assertTrue(spoken.last().contains("lista"))
    }

    @Test
    fun weatherStateCanBeRestored() {
        val handle = SavedStateHandle()
        val first = GameViewModel(handle)
        first.setCharacter(Character.TOBIAS)
        first.setWeather(Weather.SOLEADO) {}
        assertTrue(first.dropItem(Item.POLERA) {})
        val restored = GameViewModel(handle)
        assertEquals(first.state, restored.state)
        assertEquals(Character.TOBIAS, restored.state.character)
        assertEquals(setOf(Item.POLERA), restored.state.selected)
    }
}
