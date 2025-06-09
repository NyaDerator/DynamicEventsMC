package com.example.dynamicevents.core

import com.example.dynamicevents.DynamicEventsPlugin
import com.example.dynamicevents.events.chests.ChestEvent
import com.example.dynamicevents.events.chests.ChestRarity
import org.bukkit.*
import kotlin.random.Random

class EventManager(private val plugin: DynamicEventsPlugin) {
    private val activeChests = mutableMapOf<Location, ChestEvent>()
    private var config = plugin.configManager

    fun spawnRandomChest() {
        val worlds = config.getEnabledWorlds()
            .mapNotNull { Bukkit.getWorld(it) }

        if (worlds.isEmpty()) return

        val world = worlds.random()
        val location = getRandomLocation(world)
        val rarity = getRandomRarity()

        createChestEvent(location, rarity)
    }

    private fun getRandomLocation(world: World): Location {
        val settings = config.getWorldSpawnSettings(world.name)

        val x = Random.nextInt(settings.minX, settings.maxX + 1)
        val z = Random.nextInt(settings.minZ, settings.maxZ + 1)

        val y = when (world.environment) {
            World.Environment.NETHER,
            World.Environment.THE_END -> Random.nextInt(settings.minY, settings.maxY + 1)
            else -> world.getHighestBlockYAt(x, z) + 1
        }

        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    private fun getRandomRarity(): ChestRarity {
        val chances = config.getRarityChances()
        val orderedChances = listOf(
            ChestRarity.LEGENDARY,
            ChestRarity.EPIC,
            ChestRarity.RARE,
            ChestRarity.COMMON
        ).map { it to chances[it]!! }

        val rand = Random.nextDouble()
        var cumulative = 0.0

        for ((rarity, chance) in orderedChances) {
            cumulative += chance
            if (rand < cumulative) {
                return rarity
            }
        }
        return ChestRarity.COMMON
    }

    private fun createChestEvent(location: Location, rarity: ChestRarity) {
        val event = ChestEvent(location, rarity, plugin)
        activeChests[location] = event
        event.start()
    }

    fun removeChest(location: Location) {
        activeChests.remove(location)
    }

    fun removeChestEvent(location: Location) {
        val event = activeChests[location]
        event?.cleanup()
        location.block.type = Material.AIR
        activeChests.remove(location)
    }

    fun getChestEvent(location: Location): ChestEvent? = activeChests[location]
    fun getAllActiveChests(): Map<Location, ChestEvent> = activeChests.toMap()
}