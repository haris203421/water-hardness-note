package com.waterhardness.note.domain.usecase

import com.waterhardness.note.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

object PlanCalculator {
    fun remaining(plan: DescalePlan, today: LocalDate): Long = ChronoUnit.DAYS.between(today, plan.dueDate)
    fun suggestion(type: ApplianceType, hardness: Hardness?): Int? {
        if (type == ApplianceType.OTHER || hardness == null) return null
        return when (hardness) {
            Hardness.SOFT -> 90
            Hardness.MODERATE -> 60
            Hardness.HARD -> 30
            Hardness.VERY_HARD -> if (type == ApplianceType.COFFEE) 21 else 14
        }
    }
    fun recompute(data: CareData, applianceId: String): CareData {
        val latest = data.cleanings.filter { it.applianceId == applianceId }.maxOfOrNull { it.date }
        return data.copy(plans = data.plans.map { plan ->
            if (plan.applianceId != applianceId) plan else plan.copy(anchorType = if (latest == null) AnchorType.PLAN_START else AnchorType.CLEANING, anchorDate = latest ?: plan.startDate, dueOverride = null, snooze = null, cycle = UUID.randomUUID().toString(), lastDelivered = null)
        })
    }
}
