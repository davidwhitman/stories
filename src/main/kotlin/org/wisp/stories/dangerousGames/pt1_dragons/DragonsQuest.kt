package org.wisp.stories.dangerousGames.pt1_dragons

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import org.wisp.stories.game
import wisp.questgiver.InteractionDefinition
import wisp.questgiver.QuestFacilitator
import wisp.questgiver.wispLib.*

/**
 * Bring some passengers to see dragon-like creatures on a dangerous adventure.
 * Part 1 - Bring passengers to planet.
 * Part 2 - Return them back home
 */
object DragonsQuest : QuestFacilitator() {
    private val DRAGON_PLANET_TYPES = listOf(
        "terran",
        "terran-eccentric",
        "jungle",
        "US_jungle" // Unknown Skies
    )

    val icon = InteractionDefinition.Portrait("wispStories_dragonriders", "icon")
    val intelDetailHeaderImage = InteractionDefinition.Illustration("wispStories_dragonriders", "intelPicture")
    val dragonPlanetImage = InteractionDefinition.Illustration("wispStories_dragonriders", "planetIllustration")

    const val rewardCredits: Int = 95000
    const val minimumDistanceFromPlayerInLightYearsToPlaceDragonPlanet = 5

    var stage: Stage by PersistentData(key = "dragonQuestStage", defaultValue = { Stage.NotStarted })
        private set

    var dragonPlanet: SectorEntityToken? by PersistentNullableData("dragonDestinationPlanet")
        private set

    var startingPlanet: SectorEntityToken? by PersistentNullableData("dragonStartingPlanet")
        private set

    /**
     * Find a planet with life somewhere near the center, excluding player's current location.
     */
    fun init(playersCurrentStarSystem: StarSystemAPI?) {
        findAndTagDragonPlanetIfNeeded(playersCurrentStarSystem)
    }

    override fun getBarEventInformation() = BarEventInformation(
        barEventCreator = DragonsPart1_BarEventCreator(),
        shouldOfferFromMarket = { market ->
            market.factionId.toLowerCase() !in listOf("luddic_church", "luddic_path")
                    && market.size > 3
        }
    )

    override fun isComplete() = stage >= Stage.FailedByAbandoning
    override fun hasBeenStarted() = stage == Stage.NotStarted

    override fun updateTextReplacements(text: Text) {
        text.globalReplacementGetters["dragonPlanet"] = { dragonPlanet?.name }
        text.globalReplacementGetters["dragonSystem"] = { dragonPlanet?.starSystem?.name }
        text.globalReplacementGetters["startPlanet"] = { startingPlanet?.name }
        text.globalReplacementGetters["startSystem"] = { startingPlanet?.starSystem?.name }
    }

    private fun findAndTagDragonPlanetIfNeeded(playersCurrentStarSystem: StarSystemAPI?) {
        if (dragonPlanet == null) {
            val planet = try {
                game.sector.starSystemsNotOnBlacklist
                    .filter { it.id != playersCurrentStarSystem?.id }
                    .filter { it.distanceFromPlayerInHyperspace > minimumDistanceFromPlayerInLightYearsToPlaceDragonPlanet }
                    .sortedBy { it.distanceFromCenterOfSector }
                    .flatMap { it.planets }
                    .filter { planet -> DRAGON_PLANET_TYPES.any { it == planet.typeId } }
                    .toList()
                    .getNonHostileOnlyIfPossible()
                    .run {
                        // Take all planets from the top third of the list,
                        // which is sorted by proximity to the center.
                        this.take((this.size / 3).coerceAtLeast(1))
                    }
                    .random()
            } catch (e: Exception) {
                // If no planets matching the criteria are found
                game.errorReporter.reportCrash(e)
                return
            }

            dragonPlanet = planet
        }
    }

    fun restartQuest() {
        game.logger.i { "Restarting Dragons quest." }

        dragonPlanet = null
        startingPlanet = null
        stage = Stage.NotStarted

        game.intelManager.findFirst(DragonsQuest_Intel::class.java)?.endImmediately()
        BarEventManager.getInstance().removeBarEventCreator(DragonsPart1_BarEventCreator::class.java)
        init(game.sector.playerFleet.starSystem)
    }

    fun startStage1(startLocation: SectorEntityToken) {
        stage = Stage.GoToPlanet
        startingPlanet = startLocation
        game.intelManager.addIntel(DragonsQuest_Intel(startLocation = startLocation, endLocation = dragonPlanet!!))
    }

    fun failQuestByLeavingToGetEatenByDragons() {
        stage = Stage.FailedByAbandoning
        game.intelManager.findFirst(DragonsQuest_Intel::class.java)
            ?.endAndNotifyPlayer()

    }

    fun startPart2() {
        stage = Stage.ReturnToStart
        game.intelManager.findFirst(DragonsQuest_Intel::class.java)
            ?.apply {
                flipStartAndEndLocations()
                sendUpdateIfPlayerHasIntel(null, false)
            }
    }

    fun finishStage2() {
        game.sector.playerFleet.cargo.credits.add(rewardCredits.toFloat())
        stage = Stage.Done
        game.intelManager.findFirst(DragonsQuest_Intel::class.java)
            ?.apply {
                endAfterDelay()
                sendUpdateIfPlayerHasIntel(null, false)
            }
    }

    /**
     * Where the player is in the quest.
     * Note: Should be in order of completion.
     */
    enum class Stage {
        NotStarted,
        GoToPlanet,
        ReturnToStart,
        FailedByAbandoning,
        Done
    }
}