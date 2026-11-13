package com.waterhardness.note.data.local

import android.content.Context
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.usecase.HardnessCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CarePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("water_hardness_note_v1", Context.MODE_PRIVATE)
    fun <T> serialized(block: () -> T): T = mutationLock.withLock(block)
    fun read(): CareData {
        val raw = preferences.getString("snapshot", null) ?: return CareData()
        val root = JSONObject(raw)
        require(root.getInt("schemaVersion") == 1)
        val data = CareData(
            appliances = root.getJSONArray("appliances").objects().map { Appliance(it.getString("id"), it.getString("name"), ApplianceType.valueOf(it.getString("type")), Instant.parse(it.getString("createdAt")), it.getBoolean("archived")) },
            tests = root.getJSONArray("tests").objects().map { WaterTest(it.getString("id"), it.getString("draftId"), it.getString("applianceId"), it.getString("name"), LocalDate.parse(it.getString("date")), Instant.parse(it.getString("createdAt")), it.getInt("canonical"), it.getString("originalInput"), WaterUnit.valueOf(it.getString("unit")), Hardness.valueOf(it.getString("category")), EntrySource.valueOf(it.getString("source")), it.optJSONObject("chart")?.let(::readChart), it.getString("note")) },
            charts = root.getJSONArray("charts").objects().map(::readChart),
            plans = root.getJSONArray("plans").objects().map { DescalePlan(it.getString("applianceId"), AnchorType.valueOf(it.getString("anchorType")), LocalDate.parse(it.getString("startDate")), LocalDate.parse(it.getString("anchorDate")), it.getInt("interval"), it.stringOrNull("dueOverride")?.let(LocalDate::parse), it.getBoolean("reminderEnabled"), it.stringOrNull("snooze")?.let(LocalDate::parse), it.getString("cycle"), it.stringOrNull("lastDelivered")) },
            cleanings = root.getJSONArray("cleanings").objects().map { DescaleEvent(it.getString("id"), it.getString("applianceId"), it.getString("name"), LocalDate.parse(it.getString("date")), Instant.parse(it.getString("createdAt")), LocalDate.parse(it.getString("nextDue"))) },
            settings = root.getJSONObject("settings").let { CareSettings(WaterUnit.valueOf(it.getString("unit")), it.getBoolean("reminders"), it.getBoolean("haptics"), it.getBoolean("examples"), it.stringOrNull("chartId")) }
        )
        validate(data)
        return data
    }
    fun write(data: CareData) {
        validate(data)
        val root = JSONObject().put("schemaVersion", 1)
            .put("appliances", array(data.appliances) { JSONObject().put("id", it.id).put("name", it.name).put("type", it.type.name).put("createdAt", it.createdAt).put("archived", it.archived) })
            .put("tests", array(data.tests) { JSONObject().put("id", it.id).put("draftId", it.draftId).put("applianceId", it.applianceId).put("name", it.name).put("date", it.date).put("createdAt", it.createdAt).put("canonical", it.canonical).put("originalInput", it.originalInput).put("unit", it.unit.name).put("category", it.category.name).put("source", it.source.name).put("chart", it.chart?.let(::writeChart) ?: JSONObject.NULL).put("note", it.note) })
            .put("charts", array(data.charts, ::writeChart))
            .put("plans", array(data.plans) { JSONObject().put("applianceId", it.applianceId).put("anchorType", it.anchorType.name).put("startDate", it.startDate).put("anchorDate", it.anchorDate).put("interval", it.interval).put("dueOverride", it.dueOverride ?: JSONObject.NULL).put("reminderEnabled", it.reminderEnabled).put("snooze", it.snooze ?: JSONObject.NULL).put("cycle", it.cycle).put("lastDelivered", it.lastDelivered ?: JSONObject.NULL) })
            .put("cleanings", array(data.cleanings) { JSONObject().put("id", it.id).put("applianceId", it.applianceId).put("name", it.name).put("date", it.date).put("createdAt", it.createdAt).put("nextDue", it.nextDue) })
            .put("settings", JSONObject().put("unit", data.settings.unit.name).put("reminders", data.settings.reminders).put("haptics", data.settings.haptics).put("examples", data.settings.examples).put("chartId", data.settings.chartId ?: JSONObject.NULL))
        val previous = preferences.getString("snapshot", null)
        if (!preferences.edit().putString("snapshot", root.toString()).commit()) {
            preferences.edit().putString("snapshot", previous).commit()
            throw IllegalStateException("commit")
        }
    }
    private fun readChart(json: JSONObject) = ChartProfile(json.getString("id"), json.getString("label"), json.getJSONArray("values").let { a -> List(a.length()) { a.getInt(it) } }, json.getJSONArray("swatches").let { a -> List(a.length()) { a.getInt(it) } }, json.getBoolean("verified"), Instant.parse(json.getString("updatedAt")))
    private fun writeChart(chart: ChartProfile) = JSONObject().put("id", chart.id).put("label", chart.label).put("values", JSONArray(chart.values)).put("swatches", JSONArray(chart.swatches)).put("verified", chart.verified).put("updatedAt", chart.updatedAt)
    private fun validate(data: CareData) {
        require(data.appliances.map { it.id }.distinct().size == data.appliances.size)
        require(data.appliances.all { it.id.isNotBlank() && it.name.isNotBlank() && it.name.length <= 40 })
        require(data.tests.map { it.id }.distinct().size == data.tests.size && data.tests.map { it.draftId }.distinct().size == data.tests.size)
        require(data.tests.all { it.canonical in 0..10000 && it.category == HardnessCalculator.category(it.canonical) && it.note.length <= 160 && data.appliances.any { a -> a.id == it.applianceId } })
        require(data.tests.all { it.id.isNotBlank() && it.draftId.isNotBlank() && it.name.isNotBlank() && if (it.source == EntrySource.CHART) it.chart?.verified == true else it.chart == null })
        require((data.charts + data.tests.mapNotNull { it.chart }).all { HardnessCalculator.validChart(it.values) && it.swatches.size == 6 && it.swatches.all { s -> s in 0..5 } })
        require(data.charts.map { it.id }.distinct().size == data.charts.size)
        require(data.plans.map { it.applianceId }.distinct().size == data.plans.size)
        require(data.plans.all { it.interval in 1..365 && it.cycle.isNotBlank() && data.appliances.any { a -> a.id == it.applianceId } })
        require(data.cleanings.map { it.id }.distinct().size == data.cleanings.size)
        require(data.cleanings.all { data.appliances.any { a -> a.id == it.applianceId } })
        require(data.settings.chartId == null || data.charts.any { it.id == data.settings.chartId })
    }
    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
    private fun <T> array(items: List<T>, encode: (T) -> JSONObject) = JSONArray().apply { items.forEach { put(encode(it)) } }
    companion object {
        private val mutationLock = ReentrantLock()
    }
}
