package com.example.dynamicevents.core

import com.example.dynamicevents.DynamicEventsPlugin
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import java.time.LocalDateTime

class PvPManager(private val plugin: DynamicEventsPlugin) {
    private val killLogs = mutableListOf<KillLog>()

    fun handlePlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer ?: return

        if (isInPvEZone(victim)) {
            logKill(killer, victim)
            notifyAdmins(killer, victim)
        }
    }

    fun handlePlayerDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        if (isInPvEZone(victim)) {
            event.isCancelled = true
            damager.sendMessage("§cВы не можете атаковать игроков в PvE зоне!")

            plugin.logger.warning(
                "PvE Attack Attempt: ${damager.name} попытался атаковать ${victim.name} в PvE зоне на ${victim.world?.name} ${victim.location.blockX}, ${victim.location.blockY}, ${victim.location.blockZ}"
            )

            notifyAdminsAboutAttackAttempt(damager, victim)
        }
    }

    private fun isInPvEZone(player: Player): Boolean {
        return plugin.eventManager.getAllActiveChests().any { (location, _) ->
            player.location.distance(location) <= 250
        }
    }

    private fun logKill(killer: Player, victim: Player) {
        val log = KillLog(
            killer.name,
            victim.name,
            victim.location.clone(),
            LocalDateTime.now()
        )
        killLogs.add(log)

        plugin.logger.warning(
            "PvE Kill: ${killer.name} убил ${victim.name} в PvE зоне на ${victim.world?.name} ${victim.location.blockX}, ${victim.location.blockY}, ${victim.location.blockZ}"
        )
    }

    private fun notifyAdmins(killer: Player, victim: Player) {
        val message = "§c[PvE Kill] §f${killer.name} §cубил §f${victim.name} §cв PvE зоне!"

        plugin.server.onlinePlayers
            .filter { it.hasPermission("dynamicevents.admin") }
            .forEach { it.sendMessage(message) }
    }

    private fun notifyAdminsAboutAttackAttempt(attacker: Player, victim: Player) {
        val message = "§e[PvE Attack] §f${attacker.name} §eпопытался атаковать §f${victim.name} §eв PvE зоне!"

        plugin.server.onlinePlayers
            .filter { it.hasPermission("dynamicevents.admin") }
            .forEach { it.sendMessage(message) }
    }

    data class KillLog(
        val killer: String,
        val victim: String,
        val location: Location,
        val timestamp: LocalDateTime
    )
}