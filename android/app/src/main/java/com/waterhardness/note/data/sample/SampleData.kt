package com.waterhardness.note.data.sample

import com.waterhardness.note.domain.model.*
import java.time.LocalDate

object SampleData {
    fun chart(label: String) = ChartProfile(id = "reference-chart", label = label)
    fun examples(today: LocalDate) = listOf(
        ExampleCare(ApplianceType.KETTLE, 1500, Hardness.HARD, today.minusDays(2), today.minusDays(18), 30),
        ExampleCare(ApplianceType.COFFEE, 1000, Hardness.MODERATE, today.minusDays(9), today.minusDays(61), 60),
        ExampleCare(ApplianceType.DISPENSER, 500, Hardness.SOFT, today.minusDays(4), today.minusDays(14), 90)
    )
}
