package com.example.dynamicevents.listeners

import com.example.dynamicevents.DynamicEventsPlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.block.Block
import org.bukkit.Material

class EventListener(private val plugin: DynamicEventsPlugin) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        plugin.pvpManager.handlePlayerDeath(event)
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager is Player && event.entity is Player) {
            plugin.pvpManager.handlePlayerDamage(event)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val chestEvent = plugin.eventManager.getChestEvent(location)

        if (chestEvent != null) {
            event.isCancelled = true
            event.player.sendMessage("§cВы не можете сломать этот сундук!")
        }
    }

    @EventHandler
    fun onChestInteract(event: PlayerInteractEvent) {
        val location = event.clickedBlock?.location ?: return
        val chestEvent = plugin.eventManager.getChestEvent(location)  ?: return

        if (event.action == Action.RIGHT_CLICK_BLOCK && !chestEvent.isOpen) {
            val block: Block? = event.clickedBlock
            if (block != null && (block.type == Material.CHEST || block.type == Material.TRAPPED_CHEST)) {
                val chestEvent = plugin.eventManager.getChestEvent(block.location)
                if (chestEvent != null) {
                    event.isCancelled = true
                    event.player.sendMessage("§cВы не можете открыть этот сундук!")
                }
            }
        }
    }
}