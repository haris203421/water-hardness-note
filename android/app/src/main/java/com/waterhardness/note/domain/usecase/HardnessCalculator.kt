package com.waterhardness.note.domain.usecase

import com.waterhardness.note.domain.model.Hardness
import com.waterhardness.note.domain.model.WaterUnit
import java.math.BigDecimal
import java.math.RoundingMode

object HardnessCalculator {
    private fun factor(unit: WaterUnit) = when (unit) {
        WaterUnit.MG_L -> BigDecimal.ONE
        WaterUnit.DH -> BigDecimal("17.848")
        WaterUnit.MMOL_L -> BigDecimal("100.0869")
    }
    fun parse(input: String, unit: WaterUnit): Int? {
        val normalized = input.trim().replace(',', '.')
        if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(normalized)) return null
        val mg = normalized.toBigDecimalOrNull()?.multiply(factor(unit)) ?: return null
        if (mg < BigDecimal.ZERO || mg > BigDecimal("1000")) return null
        return mg.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP).toInt()
    }
    fun category(canonical: Int): Hardness = when (canonical) {
        in 0..600 -> Hardness.SOFT
        in 601..1200 -> Hardness.MODERATE
        in 1201..1800 -> Hardness.HARD
        else -> Hardness.VERY_HARD
    }
    fun format(canonical: Int, unit: WaterUnit): String {
        val places = when (unit) { WaterUnit.MG_L -> 1; WaterUnit.DH -> 1; WaterUnit.MMOL_L -> 2 }
        val value = BigDecimal(canonical).divide(BigDecimal.TEN).divide(factor(unit), places, RoundingMode.HALF_UP)
        return if (unit == WaterUnit.MG_L) value.stripTrailingZeros().toPlainString() else value.toPlainString()
    }
    fun step(input: String, unit: WaterUnit, direction: Int): String? {
        val value = input.replace(',', '.').toBigDecimalOrNull() ?: BigDecimal.ZERO
        val step = if (unit == WaterUnit.MG_L) BigDecimal.ONE else BigDecimal("0.1")
        val candidate = value.add(step.multiply(BigDecimal(direction))).stripTrailingZeros().toPlainString()
        return candidate.takeIf { parse(it, unit) != null }
    }
    fun validChart(values: List<Int>): Boolean = values.size == 6 && values.all { it in 0..10000 } && values.zipWithNext().all { (a, b) -> a < b }
}
