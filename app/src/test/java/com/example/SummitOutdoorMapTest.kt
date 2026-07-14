package com.example

import com.example.ui.OutdoorMapMode
import com.example.ui.MapFollowMode
import com.example.ui.MapLoadState
import com.example.ui.OutdoorMapStateManager
import com.example.ui.mapSportToOutdoorMapMode
import com.example.ui.applyStyleForMode
import org.junit.Assert.*
import org.junit.Test
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer

class SummitOutdoorMapTest {

    @Test
    fun `test activity type to OutdoorMapMode mapping`() {
        assertEquals(OutdoorMapMode.HIKE, mapSportToOutdoorMapMode("hike"))
        assertEquals(OutdoorMapMode.RUN, mapSportToOutdoorMapMode("run"))
        assertEquals(OutdoorMapMode.WALK, mapSportToOutdoorMapMode("walk"))
        assertEquals(OutdoorMapMode.CYCLE, mapSportToOutdoorMapMode("ride"))
        
        // Default fallbacks
        assertEquals(OutdoorMapMode.TREK, mapSportToOutdoorMapMode("swim"))
        assertEquals(OutdoorMapMode.TREK, mapSportToOutdoorMapMode("unknown"))
        assertEquals(OutdoorMapMode.HIKE, mapSportToOutdoorMapMode("HIKE"))
    }

    @Test
    fun `test initial state and transitions`() {
        val stateManager = OutdoorMapStateManager(OutdoorMapMode.HIKE, MapFollowMode.FOLLOWING)
        
        assertEquals(OutdoorMapMode.HIKE, stateManager.mode)
        assertEquals(MapFollowMode.FOLLOWING, stateManager.followMode)
        
        // 1. User manual interaction sets to FREE_EXPLORE
        stateManager.handleUserInteraction()
        assertEquals(MapFollowMode.FREE_EXPLORE, stateManager.followMode)
        
        // 2. Recentering action restores FOLLOWING
        stateManager.handleRecenter()
        assertEquals(MapFollowMode.FOLLOWING, stateManager.followMode)
    }

    @Test
    fun `test mode updates`() {
        val stateManager = OutdoorMapStateManager(OutdoorMapMode.HIKE, MapFollowMode.FOLLOWING)
        
        stateManager.setMapMode(OutdoorMapMode.CYCLE)
        assertEquals(OutdoorMapMode.CYCLE, stateManager.mode)
    }

    @Test
    fun `test map load state enums`() {
        val loading = MapLoadState.LOADING
        val ready = MapLoadState.READY
        val missingConfig = MapLoadState.MISSING_CONFIGURATION
        val styleFailed = MapLoadState.STYLE_LOAD_FAILED
        val networkUnavailable = MapLoadState.NETWORK_UNAVAILABLE

        assertEquals("LOADING", loading.name)
        assertEquals("READY", ready.name)
        assertEquals("MISSING_CONFIGURATION", missingConfig.name)
        assertEquals("STYLE_LOAD_FAILED", styleFailed.name)
        assertEquals("NETWORK_UNAVAILABLE", networkUnavailable.name)
    }

    @Test
    fun `test applyStyleForMode adds custom emphasis layers and cleans up previous layers`() {
        val style = Style()
        
        // Call applyStyleForMode for HIKE
        applyStyleForMode(style, OutdoorMapMode.HIKE)
        
        // Verify that a new hike layer is added to style
        val addedLayer = style.addedLayers.firstOrNull() as? LineLayer
        assertNotNull(addedLayer)
        assertEquals("summit-hike-emphasis", addedLayer?.id)
        assertEquals("outdoor", addedLayer?.sourceId)
        assertEquals("trail", addedLayer?.sourceLayer)
        assertEquals("#E65100", addedLayer?.properties?.get("line-color"))
    }

    @Test
    fun `test applyStyleForMode removes previous Summit layers if they exist`() {
        val style = Style()
        val oldLayer = LineLayer("summit-cycle-emphasis", "outdoor")
        style.layers.add(oldLayer)
        
        applyStyleForMode(style, OutdoorMapMode.HIKE)
        
        // Verify old layer was removed
        assertTrue(style.removedLayers.contains(oldLayer))
        assertFalse(style.layers.contains(oldLayer))
    }

    @Test
    fun `test applyStyleForMode adds layer below Road labels if present`() {
        val style = Style()
        val roadLabels = SymbolLayer("Road labels", "maptiler_planet")
        style.layers.add(roadLabels)
        
        applyStyleForMode(style, OutdoorMapMode.HIKE)
        
        // Verify that addLayerBelow is called to insert under labels
        val addedBelow = style.addedLayersBelow.firstOrNull()
        assertNotNull(addedBelow)
        assertEquals("summit-hike-emphasis", addedBelow?.first?.id)
        assertEquals("Road labels", addedBelow?.second)
    }

    @Test
    fun `test applyStyleForMode trek mode adjusts Outdoor POI layer properties`() {
        val style = Style()
        val outdoorLayer = SymbolLayer("Outdoor", "outdoor")
        style.layers.add(outdoorLayer)
        
        applyStyleForMode(style, OutdoorMapMode.TREK)
        
        // Verify that properties were updated on mockOutdoorLayer to scale shelters/huts
        assertNotNull(outdoorLayer.properties["icon-size"])
    }
}
