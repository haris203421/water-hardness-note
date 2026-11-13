package com.waterhardness.note.presentation.common

import com.waterhardness.note.domain.model.ChartProfile
import com.waterhardness.note.domain.model.WaterUnit
import com.waterhardness.note.domain.usecase.HardnessCalculator
import java.util.UUID

object ChartEditing {
    fun open(profile: ChartProfile?, label: String) = ChartEditor(label, (profile?.values ?: listOf(0, 500, 1000, 1500, 2500, 4000)).map { HardnessCalculator.format(it, WaterUnit.MG_L) }, profile?.swatches ?: (0..5).toList())
    fun conflict(editor: ChartEditor): Int? {
        val parsed = editor.values.map { HardnessCalculator.parse(it, WaterUnit.MG_L) }
        return parsed.indices.firstOrNull { parsed[it] == null || (it > 0 && (parsed[it] ?: -1) <= (parsed[it - 1] ?: -1)) }
    }
    fun profile(editor: ChartEditor) = ChartProfile(UUID.randomUUID().toString(), editor.label, editor.values.map { requireNotNull(HardnessCalculator.parse(it, WaterUnit.MG_L)) }, editor.swatches, true)
}
