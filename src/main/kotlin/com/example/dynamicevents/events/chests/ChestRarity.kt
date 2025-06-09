package com.example.dynamicevents.events.chests

import org.bukkit.boss.BarColor

enum class ChestRarity(
    val displayName: String,
    val color: Char,
    val barColor: BarColor
) {
    COMMON("Обычный", 'f', BarColor.WHITE),
    RARE("Редкий", 'b', BarColor.BLUE),
    EPIC("Эпический", '5', BarColor.PURPLE),
    LEGENDARY("Легендарный", '6', BarColor.YELLOW)
}