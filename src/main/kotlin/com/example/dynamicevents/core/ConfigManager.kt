package com.example.dynamicevents.core

import com.example.dynamicevents.DynamicEventsPlugin
import com.example.dynamicevents.events.chests.ChestRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: DynamicEventsPlugin) {
    private lateinit var config: FileConfiguration

    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
    }

    fun getSpawnInterval(): Long = config.getLong("spawn-interval", 30) * 60 * 20L
    fun getOpenTime(): Int = config.getInt("chest-settings.open-time", 600)
    fun getPvpZoneRadius(): Double = config.getDouble("chest-settings.pvp-zone-radius", 250.0)
    fun getBossbarRadius(): Double = config.getDouble("chest-settings.bossbar-radius", 500.0)
    fun getChestLootTable(rarity: ChestRarity) = config.getConfigurationSection("loot.${rarity.name}")

    fun getEnabledWorlds(): List<String> = config.getStringList("worlds.enabled-worlds")

    fun getWorldSpawnSettings(worldName: String): WorldSpawnSettings {
        val path = "worlds.spawn-settings.$worldName"
        return WorldSpawnSettings(
            minX = config.getInt("$path.min-x", -1000),
            maxX = config.getInt("$path.max-x", 1000),
            minZ = config.getInt("$path.min-z", -1000),
            maxZ = config.getInt("$path.max-z", 1000),
            minY = config.getInt("$path.min-y", 60),
            maxY = config.getInt("$path.max-y", 120)
        )
    }

    fun getRarityChances(): Map<ChestRarity, Double> {
        return mapOf(
            ChestRarity.LEGENDARY to config.getDouble("rarity-chances.LEGENDARY", 0.05),
            ChestRarity.EPIC to config.getDouble("rarity-chances.EPIC", 0.15),
            ChestRarity.RARE to config.getDouble("rarity-chances.RARE", 0.30),
            ChestRarity.COMMON to config.getDouble("rarity-chances.COMMON", 0.50)
        )
    }

    fun getMessage(key: String): String =
        config.getString("messages.$key", "§cСообщение не найдено: $key")!!

    fun getFormattedMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = getMessage(key)
        replacements.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    fun getParticleInterval(): Long = config.getLong("effects.particle-interval", 40)
    fun getSoundInterval(): Long = config.getLong("effects.sound-interval", 40)
    fun getParticleCount(type: String): Int = config.getInt("effects.particles.$type", 10)
    fun getSoundVolume(type: String): Float = config.getDouble("effects.sounds.$type", 1.0).toFloat()

    data class LootItem(
        val itemStack: ItemStack,
        val chance: Double,
        val amountRange: IntRange = itemStack.amount..itemStack.amount
    )

    data class WorldSpawnSettings(
        val minX: Int,
        val maxX: Int,
        val minZ: Int,
        val maxZ: Int,
        val minY: Int,
        val maxY: Int
    )
}