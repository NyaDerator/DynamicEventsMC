package com.example.dynamicevents

import com.example.dynamicevents.core.ConfigManager
import com.example.dynamicevents.core.EventManager
import com.example.dynamicevents.core.PvPManager
import com.example.dynamicevents.listeners.EventListener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class DynamicEventsPlugin : JavaPlugin() {
    lateinit var configManager: ConfigManager
    lateinit var eventManager: EventManager
    lateinit var pvpManager: PvPManager

    override fun onEnable() {
        configManager = ConfigManager(this)
        eventManager = EventManager(this)
        pvpManager = PvPManager(this)

        configManager.loadConfig()
        server.pluginManager.registerEvents(EventListener(this), this)
        getCommand("dynamicevents")?.setExecutor(CommandHandler(this))

        startEventScheduler()
        logger.info("DynamicEventsMC запущен!")
    }

    private fun startEventScheduler() {
        object : BukkitRunnable() {
            override fun run() {
                eventManager.spawnRandomChest()
            }
        }.runTaskTimer(this, 0L, configManager.getSpawnInterval()) // 30 минут
    }
}