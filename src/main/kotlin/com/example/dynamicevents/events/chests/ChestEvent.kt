package com.example.dynamicevents.events.chests

import com.example.dynamicevents.DynamicEventsPlugin
import com.example.dynamicevents.core.ConfigManager.LootItem
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.util.Random

class ChestEvent(
    val location: Location,
    val rarity: ChestRarity,
    private val plugin: DynamicEventsPlugin
) {
    private val random = Random()
    private var bossBar: BossBar? = null
    var isOpen = false
        private set
    private var config = plugin.configManager
    private var timeLeft = config.getOpenTime()
    private var pvpTimeLeft = 300
    private var effectsTask: BukkitRunnable? = null
    private var timerTask: BukkitRunnable? = null
    private var pvpTimerTask: BukkitRunnable? = null

    fun start() {
        createClosedChest()
        startEffects()
        startTimer()
        announceChestSpawn()
    }


    private fun parseAmount(range: String): IntRange {
        return if ("..." in range) {
            val (min, max) = range.split("...").map { it.trim().toInt() }
            min..max
        } else {
            val value = range.trim().toInt()
            value..value
        }
    }

    fun getLootTable(rarity: ChestRarity): List<LootItem> {
        val section = config.getChestLootTable(rarity) ?: return emptyList()
        val items = mutableListOf<LootItem>()

        for (key in section.getKeys(false)) {
            val itemSection = section.getConfigurationSection(key) ?: continue
            try {
                val material = Material.valueOf(itemSection.getString("material") ?: "DIRT")
                val rawAmount = itemSection.getString("amount") ?: "1"
                val amount = parseAmount(rawAmount)
                val chance = itemSection.getDouble("chance", 0.1)

                val item = ItemStack(material, amount.random())
                val meta = item.itemMeta ?: continue

                itemSection.getString("display-name")?.let {
                    meta.setDisplayName(it)
                }

                if (itemSection.isList("lore")) {
                    val loreList = itemSection.getStringList("lore")
                    meta.lore = loreList
                }

                val enchantsSection = itemSection.getConfigurationSection("enchants")
                if (enchantsSection != null) {
                    for (enchantKey in enchantsSection.getKeys(false)) {
                        val enchantment = Enchantment.getByName(enchantKey.uppercase())
                        if (enchantment != null) {
                            val level = enchantsSection.getInt(enchantKey)
                            meta.addEnchant(enchantment, level, true)
                        } else {
                            plugin.logger.warning("Неверное имя зачарования: $enchantKey")
                        }
                    }
                }

                if (itemSection.contains("custom-model-data")) {
                    meta.setCustomModelData(itemSection.getInt("custom-model-data"))
                }

                item.itemMeta = meta
                items.add(LootItem(item, chance))
            } catch (e: Exception) {
                plugin.logger.warning("Ошибка загрузки предмета $key: ${e.message}")
            }
        }

        return items
    }


    private fun announceChestSpawn() {
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        val worldName = location.world?.name ?: "Unknown"

        val message = "§6§l[СОБЫТИЕ] §${rarity.color}§l${rarity.displayName} Сундук §7появился в мире §e$worldName §7на координатах §f$x, $y, $z§7!"

        // Отправляем сообщение всем игрокам на сервере
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(message)
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f)
        }

        // Логируем в консоль
        plugin.logger.info("Заспавнен ${rarity.name} сундук на $x, $y, $z в мире $worldName")
    }

    private fun createClosedChest() {
        location.block.type = Material.CHEST
        val chest = location.block.state as Chest
        chest.customName = "§${rarity.color}${rarity.displayName} Сундук"
        chest.update()
    }

    private fun startEffects() {
        effectsTask = object : BukkitRunnable() {
            override fun run() {
                if (isOpen) {
                    cancel()
                    return
                }

                location.world?.spawnParticle(
                    Particle.ENCHANTED_HIT,
                    location.clone().add(0.5, 1.0, 0.5),
                    20, 0.5, 0.5, 0.5, 0.1
                )

                location.world?.playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.0f)

                val particle = when (rarity) {
                    ChestRarity.LEGENDARY -> Particle.DRAGON_BREATH
                    ChestRarity.EPIC -> Particle.PORTAL
                    ChestRarity.RARE -> Particle.WITCH
                    ChestRarity.COMMON -> Particle.HAPPY_VILLAGER
                }

                location.world?.spawnParticle(
                    particle,
                    location.clone().add(0.5, 1.5, 0.5),
                    10, 0.3, 0.3, 0.3, 0.05
                )
            }
        }
        effectsTask?.runTaskTimer(plugin, 0L, 40L)
    }

    private fun startTimer() {
        bossBar = Bukkit.createBossBar(
            "§${rarity.color}${rarity.displayName} Сундук - Открытие через: ${formatTime(timeLeft)}",
            rarity.barColor,
            BarStyle.SOLID
        )

        timerTask = object : BukkitRunnable() {
            override fun run() {
                if (timeLeft <= 0) {
                    openChest()
                    cancel()
                    return
                }

                updateBossBar()
                timeLeft--
            }
        }
        timerTask?.runTaskTimer(plugin, 0L, 20L)
    }

    private fun updateBossBar() {
        val progress = if (isOpen) {
            pvpTimeLeft.toDouble() / 300.0
        } else {
            timeLeft.toDouble() / 600.0
        }

        val nearbyPlayers = getNearbyPlayers()
        val pvpRadius = plugin.configManager.getPvpZoneRadius()
        val bossbarRadius = plugin.configManager.getBossbarRadius()

        for (player in nearbyPlayers) {
            val distance = player.location.distance(location)
            val zone = if (distance <= pvpRadius) "§c⚔ ПВП ЗОНА" else "§a🛡 ПВЕ ЗОНА"

            val title = if (isOpen) {
                "§${rarity.color}${rarity.displayName} Сундук - Исчезнет через: ${formatTime(pvpTimeLeft)} | $zone"
            } else {
                "§${rarity.color}${rarity.displayName} Сундук - ${formatTime(timeLeft)} | $zone"
            }

            bossBar?.setTitle(title)
            bossBar?.progress = progress
            bossBar?.addPlayer(player)
        }

        bossBar?.players?.toList()?.forEach { player ->
            if (player.location.distance(location) > bossbarRadius) {
                bossBar?.removePlayer(player)
            }
        }
    }

    private fun startPvpTimer() {
        pvpTimerTask = object : BukkitRunnable() {
            override fun run() {
                if (pvpTimeLeft <= 0) {
                    removeChestAndZone()
                    cancel()
                    return
                }

                updateBossBar()
                pvpTimeLeft--
            }
        }
        pvpTimerTask?.runTaskTimer(plugin, 0L, 20L) // Каждую секунду
    }

    private fun openChest() {
        isOpen = true
        fillChestWithLoot()

        // Эффекты открытия
        location.world?.spawnParticle(
            Particle.EXPLOSION,
            location.clone().add(0.5, 1.0, 0.5),
            5, 0.0, 0.0, 0.0, 0.0
        )

        location.world?.playSound(location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.2f)
        location.world?.playSound(location, Sound.BLOCK_CHEST_OPEN, 1.0f, 0.8f)

        bossBar?.color = BarColor.GREEN

        startPvpTimer()
    }

    private fun removeChestAndZone() {
        location.world?.spawnParticle(
            Particle.CLOUD,
            location.clone().add(0.5, 1.0, 0.5),
            30, 1.0, 1.0, 1.0, 0.1
        )

        location.world?.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f)

        val nearbyPlayers = getNearbyPlayers()
        nearbyPlayers.forEach { player ->
            player.sendMessage("§6§l[СОБЫТИЕ] §${rarity.color}${rarity.displayName} Сундук §7исчез! ПВП зона деактивирована.")
        }

        location.block.type = Material.AIR

        bossBar?.removeAll()

        plugin.eventManager.removeChest(location)
    }

    private fun fillChestWithLoot() {
        val block = location.block
        if (block.type != Material.CHEST) {
            block.setType(Material.CHEST, false)
        }

        val state = block.state
        if (state !is Chest) {
            plugin.logger.warning("Не удалось получить сундук по адресу $location")
            return
        }

        val inventory = state.blockInventory
        inventory.clear()

        val lootTable = getLootTable(rarity)
        if (lootTable.isEmpty()) {
            plugin.logger.warning("Пустая таблица лута для редкости $rarity на $location")
            return
        }

        val size = inventory.size
        val usedSlots = mutableSetOf<Int>()

        for (lootItem in lootTable.shuffled()) {
            if (random.nextDouble() >= lootItem.chance) continue

            // Найдём случайный свободный слот
            val slot = (0 until size).filter { it !in usedSlots }.randomOrNull() ?: break
            usedSlots.add(slot)

            val item = lootItem.itemStack.clone().apply {
                amount = lootItem.amountRange.random().coerceAtMost(maxStackSize)
            }
            inventory.setItem(slot, item)
        }

        plugin.logger.info("Сундук по координатам $location открыт (${rarity.name})")
    }

    private fun getNearbyPlayers(): List<Player> {
        return location.world?.players?.filter {
            it.location.distance(location) <= 500
        } ?: emptyList()
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    fun cleanup() {
        effectsTask?.cancel()
        timerTask?.cancel()
        pvpTimerTask?.cancel()
        bossBar?.removeAll()
    }
}