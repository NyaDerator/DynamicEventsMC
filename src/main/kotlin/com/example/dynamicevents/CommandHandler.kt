package com.example.dynamicevents

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Random

class CommandHandler(private val plugin: DynamicEventsPlugin) : CommandExecutor, TabCompleter {
    private val random = Random()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("dynamicevents.admin")) {
            sender.sendMessage("§cУ вас нет прав!")
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> {
                plugin.configManager.loadConfig()
                sender.sendMessage("§aКонфигурация перезагружена!")
            }
            "info" -> {
                val activeChests = plugin.eventManager.getAllActiveChests().size
                sender.sendMessage("§aАктивных сундуков: $activeChests")
            }
            "spawn" -> {
                plugin.eventManager.spawnRandomChest()
                sender.sendMessage("§aСундук заспавнен!")
            }
            "list" -> {
                val chests = plugin.eventManager.getAllActiveChests()
                if (chests.isEmpty()) {
                    sender.sendMessage("§eНет активных сундуков")
                    return true
                }

                sender.sendMessage("§6=== Активные сундуки ===")
                chests.entries.forEachIndexed { index, (location, event) ->
                    val world = location.world?.name ?: "unknown"
                    val coords = "${location.blockX}, ${location.blockY}, ${location.blockZ}"
                    val status = if (event.isOpen) "§aОткрыт" else "§cЗакрыт"
                    sender.sendMessage("§f$index. §${event.rarity.color}${event.rarity.displayName} §f($world: $coords) - $status")
                }
            }
            "remove" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cИспользование: /dynamicevents remove <номер>")
                    return true
                }

                val index = args[1].toIntOrNull()
                if (index == null) {
                    sender.sendMessage("§cНеверный номер!")
                    return true
                }

                val chests = plugin.eventManager.getAllActiveChests().entries.toList()
                if (index < 0 || index >= chests.size) {
                    sender.sendMessage("§cСундук с номером $index не найден!")
                    return true
                }

                val (location, event) = chests[index]
                plugin.eventManager.removeChestEvent(location)
                sender.sendMessage("§aСундук удален!")
            }
            "fill" -> {
                if (sender !is Player) {
                    sender.sendMessage("§cЭту команду можно использовать только в игре!")
                    return true
                }

                val player = sender as Player
                val targetBlock = player.getTargetBlockExact(5)

                if (targetBlock == null || targetBlock.type != Material.CHEST) {
                    sender.sendMessage("§cВы должны смотреть на сундук!")
                    return true
                }

                val chest = targetBlock.state as Chest
                val inventory = chest.blockInventory

                inventory.clear()

                for (i in 0 until inventory.size) {
                    if (random.nextBoolean()) {
                        val item = when (random.nextInt(10)) {
                            0 -> ItemStack(Material.DIAMOND, random.nextInt(3) + 1)
                            1 -> ItemStack(Material.GOLD_INGOT, random.nextInt(5) + 1)
                            2 -> ItemStack(Material.IRON_INGOT, random.nextInt(8) + 1)
                            3 -> ItemStack(Material.EMERALD, random.nextInt(4) + 1)
                            4 -> ItemStack(Material.REDSTONE, random.nextInt(16) + 1)
                            5 -> ItemStack(Material.LAPIS_LAZULI, random.nextInt(12) + 1)
                            6 -> ItemStack(Material.COAL, random.nextInt(16) + 1)
                            7 -> ItemStack(Material.BOOK, random.nextInt(5) + 1)
                            8 -> ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(3) + 1)
                            else -> ItemStack(Material.PAPER, random.nextInt(16) + 1)
                        }
                        inventory.setItem(i, item)
                    }
                }

                sender.sendMessage("§aСундук заполнен случайными предметами!")
            }
            else -> {
                sender.sendMessage("§6=== DynamicEvents Help ===")
                sender.sendMessage("§f/dynamicevents reload §7- Перезагрузить конфиг")
                sender.sendMessage("§f/dynamicevents info §7- Информация о сундуках")
                sender.sendMessage("§f/dynamicevents spawn §7- Создать случайный сундук")
                sender.sendMessage("§f/dynamicevents list §7- Список активных сундуков")
                sender.sendMessage("§f/dynamicevents remove <номер> §7- Удалить сундук")
                sender.sendMessage("§f/dynamicevents fill §7- Заполнить сундук (смотреть на сундук)")
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("dynamicevents.admin")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> listOf("reload", "info", "spawn", "list", "remove", "fill")
                .filter { it.startsWith(args[0], ignoreCase = true) }

            2 -> if (args[0].equals("remove", ignoreCase = true)) {
                plugin.eventManager.getAllActiveChests()
                    .keys
                    .indices
                    .map { it.toString() }
                    .filter { it.startsWith(args[1]) }
            } else emptyList()

            else -> emptyList()
        }
    }
}