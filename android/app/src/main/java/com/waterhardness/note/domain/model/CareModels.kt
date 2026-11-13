package com.waterhardness.note.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class WaterUnit { MG_L, DH, MMOL_L }
enum class Hardness { SOFT, MODERATE, HARD, VERY_HARD }
enum class ApplianceType { KETTLE, COFFEE, DISPENSER, OTHER }
enum class EntrySource { CHART, PRINTED }
enum class AnchorType { PLAN_START, CLEANING }

data class Appliance(val id: String = UUID.randomUUID().toString(), val name: String, val type: ApplianceType, val createdAt: Instant = Instant.now(), val archived: Boolean = false)
data class ChartProfile(val id: String = UUID.randomUUID().toString(), val label: String, val values: List<Int> = listOf(0, 500, 1000, 1500, 2500, 4000), val swatches: List<Int> = (0..5).toList(), val verified: Boolean = false, val updatedAt: Instant = Instant.now())
data class WaterTest(val id: String = UUID.randomUUID().toString(), val draftId: String, val applianceId: String, val name: String, val date: LocalDate, val createdAt: Instant = Instant.now(), val canonical: Int, val originalInput: String, val unit: WaterUnit, val category: Hardness, val source: EntrySource, val chart: ChartProfile?, val note: String)
data class DescalePlan(val applianceId: String, val anchorType: AnchorType = AnchorType.PLAN_START, val startDate: LocalDate, val anchorDate: LocalDate, val interval: Int, val dueOverride: LocalDate? = null, val reminderEnabled: Boolean = false, val snooze: LocalDate? = null, val cycle: String = UUID.randomUUID().toString(), val lastDelivered: String? = null) {
    val dueDate: LocalDate get() = dueOverride ?: anchorDate.plusDays(interval.toLong())
}
data class DescaleEvent(val id: String = UUID.randomUUID().toString(), val applianceId: String, val name: String, val date: LocalDate, val createdAt: Instant = Instant.now(), val nextDue: LocalDate)
data class CareSettings(val unit: WaterUnit = WaterUnit.MG_L, val reminders: Boolean = false, val haptics: Boolean = true, val examples: Boolean = true, val chartId: String? = null)
data class CareData(val appliances: List<Appliance> = emptyList(), val tests: List<WaterTest> = emptyList(), val charts: List<ChartProfile> = emptyList(), val plans: List<DescalePlan> = emptyList(), val cleanings: List<DescaleEvent> = emptyList(), val settings: CareSettings = CareSettings()) {
    val activeAppliances: List<Appliance> get() = appliances.filterNot { it.archived }
    val activeChart: ChartProfile? get() = charts.find { it.id == settings.chartId }
}
data class StoreState(val data: CareData = CareData(), val loading: Boolean = true, val error: Boolean = false)
data class ExampleCare(val type: ApplianceType, val value: Int, val category: Hardness, val testDate: LocalDate, val cleaned: LocalDate, val interval: Int) {
    val dueDate: LocalDate get() = cleaned.plusDays(interval.toLong())
}
