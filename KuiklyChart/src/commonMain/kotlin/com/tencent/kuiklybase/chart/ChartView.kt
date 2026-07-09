/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuiklybase.chart

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasLinearGradient
import com.tencent.kuikly.core.views.TextAlign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pre-built color palettes for chart series.
 * Inspired by ECharts, Ant Design Charts, and Material Design.
 */
object ChartTheme {
    /** Ant Design / KuiklyUI brand palette. */
    val Default: List<Color> = listOf(
        Color(0xFF1677FFL), Color(0xFF36CFDBL), Color(0xFF52C41AL),
        Color(0xFFFA8C16L), Color(0xFFFF4D4FL), Color(0xFF722ED1L),
        Color(0xFFEB2F96L), Color(0xFF13C2C2L),
    )
    /** ECharts default palette. */
    val ECharts: List<Color> = listOf(
        Color(0xFF5470C6L), Color(0xFF91CC75L), Color(0xFFFAC858L),
        Color(0xFFEE6666L), Color(0xFF73C0DEL), Color(0xFF3BA272L),
        Color(0xFFFC8452L), Color(0xFF9A60B4L), Color(0xFFEA7CCCL),
    )
    /** Material Design vibrant palette. */
    val Material: List<Color> = listOf(
        Color(0xFF2196F3L), Color(0xFF4CAF50L), Color(0xFFFFC107L),
        Color(0xFFF44336L), Color(0xFF9C27B0L), Color(0xFF00BCD4L),
        Color(0xFFFF5722L), Color(0xFF607D8BL),
    )
    /** Soft pastel palette for lighter UIs. */
    val Pastel: List<Color> = listOf(
        Color(0xFF74B9FFL), Color(0xFF55EFC4L), Color(0xFFFDCB6EL),
        Color(0xFFFF7675L), Color(0xFFA29BFEL), Color(0xFF00B894L),
        Color(0xFFE17055L), Color(0xFFDFE6E9L),
    )
}

fun ViewContainer<*, *>.LineChart(init: LineChartView.() -> Unit) {
    addChild(LineChartView(), init)
}

fun ViewContainer<*, *>.BarChart(init: BarChartView.() -> Unit) {
    addChild(BarChartView(), init)
}

data class ChartDataPoint(val label: String, val value: Float)

data class ChartSeries(
    val name: String,
    val points: List<ChartDataPoint>,
    val color: Color,
)

open class ChartAttr : ComposeAttr() {

    internal var seriesList by observable(emptyList<ChartSeries>())
    internal var showGrid by observable(true)
    internal var showAxisLabels by observable(true)
    internal var axisColor by observable(Color(0xFFB4B4B4L))
    internal var gridColor by observable(Color(0xFFDCDCDCL))
    internal var labelFontSize by observable(11f)
    internal var gridLineCount by observable(4)

    fun data(vararg series: ChartSeries) {
        seriesList = series.toList()
    }

    fun data(series: List<ChartSeries>) {
        seriesList = series
    }

    fun size(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }

    fun showGrid(show: Boolean) { showGrid = show }
    fun showAxisLabels(show: Boolean) { showAxisLabels = show }
    fun axisColor(color: Color) { axisColor = color }
    fun gridColor(color: Color) { gridColor = color }
    fun labelFontSize(size: Float) { labelFontSize = size }
    fun gridLineCount(count: Int) { gridLineCount = count.coerceIn(2, 10) }
}

open class ChartEvent : ComposeEvent() {

    internal var onPointClickHandler: ((seriesIndex: Int, pointIndex: Int, value: Float) -> Unit)? = null

    fun onPointClick(handler: (seriesIndex: Int, pointIndex: Int, value: Float) -> Unit) {
        onPointClickHandler = handler
    }
}

private const val PADDING_LEFT = 40f
private const val PADDING_RIGHT = 16f
private const val PADDING_TOP = 16f
private const val PADDING_BOTTOM = 32f

private fun allValues(seriesList: List<ChartSeries>): List<Float> =
    seriesList.flatMap { s -> s.points.map { it.value } }

private fun computeRange(values: List<Float>): Pair<Float, Float> {
    if (values.isEmpty()) return 0f to 1f
    val min = values.min()
    val max = values.max()
    return if (abs(max - min) < 1e-6f) (min - 1f) to (max + 1f) else min to max
}

private fun Float.fmt1(): String {
    val scaled = (this * 10).roundToInt()
    val intPart = scaled / 10
    val fracPart = abs(scaled % 10)
    return if (scaled < 0 && intPart == 0) "-0.$fracPart" else "$intPart.$fracPart"
}

private fun Float.fmt0(): String = roundToInt().toString()

class LineChartAttr : ChartAttr() {

    internal var showDots by observable(true)
    internal var dotRadius by observable(4f)
    internal var lineWidth by observable(2f)
    internal var fillArea by observable(false)
    internal var tapX by observable(-1f)
    internal var tapY by observable(-1f)
    internal var tapText by observable("")
    internal var showTooltip by observable(true)

    fun showDots(show: Boolean) { showDots = show }
    fun dotRadius(r: Float) { dotRadius = r.coerceAtLeast(1f) }
    fun lineWidth(w: Float) { lineWidth = w.coerceAtLeast(0.5f) }
    fun fillArea(fill: Boolean) { fillArea = fill }
    fun showTooltip(show: Boolean) { showTooltip = show }
}

class LineChartView : ComposeView<LineChartAttr, ChartEvent>() {

    private var lastW = 0f
    private var lastH = 0f

    override fun createAttr(): LineChartAttr = LineChartAttr()
    override fun createEvent(): ChartEvent = ChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        ctx.handleLineClick(params)
                    }
                }
            }) { context, w, h ->
                ctx.lastW = w
                ctx.lastH = h
                val series = ctx.attr.seriesList
                if (series.isEmpty()) return@Canvas

                val values = allValues(series)
                val (minVal, maxVal) = computeRange(values)
                val plotW = w - PADDING_LEFT - PADDING_RIGHT
                val plotH = h - PADDING_TOP - PADDING_BOTTOM
                val range = maxVal - minVal

                fun toX(idx: Int, total: Int): Float =
                    PADDING_LEFT + if (total > 1) idx * plotW / (total - 1) else plotW / 2f

                fun toY(value: Float): Float =
                    PADDING_TOP + plotH * (1f - (value - minVal) / range)

                if (ctx.attr.showGrid) {
                    val steps = ctx.attr.gridLineCount
                    context.beginPath()
                    context.strokeStyle(ctx.attr.gridColor)
                    context.lineWidth(0.5f)
                    for (i in 0..steps) {
                        val y = PADDING_TOP + plotH * i / steps
                        context.moveTo(PADDING_LEFT, y)
                        context.lineTo(w - PADDING_RIGHT, y)
                    }
                    context.stroke()
                }

                context.beginPath()
                context.strokeStyle(ctx.attr.axisColor)
                context.lineWidth(1f)
                context.moveTo(PADDING_LEFT, PADDING_TOP)
                context.lineTo(PADDING_LEFT, PADDING_TOP + plotH)
                context.lineTo(PADDING_LEFT + plotW, PADDING_TOP + plotH)
                context.stroke()

                if (ctx.attr.showAxisLabels) {
                    context.font(ctx.attr.labelFontSize)
                    context.fillStyle(ctx.attr.axisColor)
                    val steps = ctx.attr.gridLineCount
                    for (i in 0..steps) {
                        val v = minVal + range * (steps - i) / steps
                        val y = PADDING_TOP + plotH * i / steps
                        context.fillText(v.fmt1(), 2f, y + 4f)
                    }
                    val first = series.first()
                    val n = first.points.size
                    first.points.forEachIndexed { idx, pt ->
                        context.fillText(pt.label, toX(idx, n) - 8f, PADDING_TOP + plotH + 18f)
                    }
                }

                for (s in series) {
                    val n = s.points.size
                    if (n == 0) continue

                    if (ctx.attr.fillArea && n > 1) {
                        context.beginPath()
                        val grad = context.createLinearGradient(0f, PADDING_TOP, 0f, PADDING_TOP + plotH)
                        val rgb = s.color.hexColor
                        val r = ((rgb shr 16) and 0xFF).toInt()
                        val g = ((rgb shr 8) and 0xFF).toInt()
                        val b = (rgb and 0xFF).toInt()
                        grad.addColorStop(0f, Color(red255 = r, green255 = g, blue255 = b, alpha01 = 0.28f))
                        grad.addColorStop(1f, Color(red255 = r, green255 = g, blue255 = b, alpha01 = 0.02f))
                        context.fillStyle(grad)
                        context.moveTo(toX(0, n), PADDING_TOP + plotH)
                        s.points.forEachIndexed { idx, pt ->
                            context.lineTo(toX(idx, n), toY(pt.value))
                        }
                        context.lineTo(toX(n - 1, n), PADDING_TOP + plotH)
                        context.closePath()
                        context.fill()
                    }

                    context.beginPath()
                    context.strokeStyle(s.color)
                    context.lineWidth(ctx.attr.lineWidth)
                    context.lineCapRound()
                    s.points.forEachIndexed { idx, pt ->
                        val x = toX(idx, n)
                        val y = toY(pt.value)
                        if (idx == 0) context.moveTo(x, y) else context.lineTo(x, y)
                    }
                    context.stroke()

                    if (ctx.attr.showDots) {
                        context.fillStyle(s.color)
                        s.points.forEachIndexed { idx, pt ->
                            context.beginPath()
                            context.arc(toX(idx, n), toY(pt.value), ctx.attr.dotRadius, 0f, (2 * PI).toFloat(), false)
                            context.fill()
                        }
                    }
                }

                // Tooltip overlay for the tapped nearest point
                val tx = ctx.attr.tapX
                val ty = ctx.attr.tapY
                if (ctx.attr.showTooltip && tx >= 0f && ctx.attr.tapText.isNotEmpty()) {
                    val text = ctx.attr.tapText
                    val padding = 5f
                    val fSize = 11f
                    val boxW = text.length * fSize * 0.6f + padding * 2f
                    val boxH = fSize + padding * 2f
                    var bx = tx - boxW / 2f
                    val by = (ty - boxH - 10f).coerceAtLeast(2f)
                    bx = bx.coerceIn(2f, w - boxW - 2f)

                    context.save()
                    context.fillStyle(Color(0xDD222222L))
                    context.beginPath()
                    val br = 4f
                    context.moveTo(bx + br, by)
                    context.lineTo(bx + boxW - br, by)
                    context.arc(bx + boxW - br, by + br, br, (-PI / 2).toFloat(), 0f, false)
                    context.lineTo(bx + boxW, by + boxH - br)
                    context.arc(bx + boxW - br, by + boxH - br, br, 0f, (PI / 2).toFloat(), false)
                    context.lineTo(bx + br, by + boxH)
                    context.arc(bx + br, by + boxH - br, br, (PI / 2).toFloat(), PI.toFloat(), false)
                    context.lineTo(bx, by + br)
                    context.arc(bx + br, by + br, br, PI.toFloat(), (-PI / 2).toFloat(), false)
                    context.closePath()
                    context.fill()

                    context.fillStyle(Color.WHITE)
                    context.font(fSize)
                    context.fillText(text, bx + padding, by + padding + fSize * 0.85f)

                    // Highlight dot
                    context.beginPath()
                    context.arc(tx, ty, ctx.attr.dotRadius + 3f, 0f, (2 * PI).toFloat(), false)
                    context.strokeStyle(Color.WHITE)
                    context.lineWidth(2f)
                    context.stroke()
                    context.restore()
                }
            }
        }
    }

    private fun handleLineClick(params: ClickParams) {
        val w = lastW
        val h = lastH
        if (w == 0f || h == 0f) return
        val series = attr.seriesList
        if (series.isEmpty()) return
        val values = allValues(series)
        val (minVal, maxVal) = computeRange(values)
        val range = maxVal - minVal
        if (range < 1e-6f) return
        val plotW = w - PADDING_LEFT - PADDING_RIGHT
        val plotH = h - PADDING_TOP - PADDING_BOTTOM
        fun toX(idx: Int, total: Int): Float =
            PADDING_LEFT + if (total > 1) idx * plotW / (total - 1) else plotW / 2f
        fun toY(value: Float): Float =
            PADDING_TOP + plotH * (1f - (value - minVal) / range)

        // Find nearest point across all series
        var bestDist = Float.MAX_VALUE
        var bestSIdx = -1
        var bestPIdx = -1
        series.forEachIndexed { sIdx, s ->
            val n = s.points.size
            s.points.forEachIndexed { pIdx, pt ->
                val dx = params.x - toX(pIdx, n)
                val dy = params.y - toY(pt.value)
                val d = sqrt(dx * dx + dy * dy)
                if (d < bestDist) { bestDist = d; bestSIdx = sIdx; bestPIdx = pIdx }
            }
        }

        if (bestSIdx >= 0 && bestPIdx >= 0) {
            val s = series[bestSIdx]
            val pt = s.points[bestPIdx]
            val n = s.points.size
            attr.tapX = toX(bestPIdx, n)
            attr.tapY = toY(pt.value)
            attr.tapText = "${pt.label}: ${pt.value.fmt1()}"
            event.onPointClickHandler?.invoke(bestSIdx, bestPIdx, pt.value)
        }
    }
}

class BarChartAttr : ChartAttr() {

    internal var barSpacing by observable(0.2f)
    internal var cornerRadius by observable(2f)
    internal var showValueLabels by observable(true)
    internal var tapX by observable(-1f)
    internal var tapY by observable(-1f)
    internal var tapText by observable("")
    internal var showTooltip by observable(true)

    fun barSpacing(fraction: Float) { barSpacing = fraction.coerceIn(0f, 0.8f) }
    fun cornerRadius(r: Float) { cornerRadius = r.coerceAtLeast(0f) }
    fun showValueLabels(show: Boolean) { showValueLabels = show }
    fun showTooltip(show: Boolean) { showTooltip = show }
}

class BarChartView : ComposeView<BarChartAttr, ChartEvent>() {

    private var lastW = 0f
    private var lastH = 0f

    override fun createAttr(): BarChartAttr = BarChartAttr()
    override fun createEvent(): ChartEvent = ChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params ->
                        ctx.handleBarClick(params)
                    }
                }
            }) { context, w, h ->
                ctx.lastW = w
                ctx.lastH = h
                val series = ctx.attr.seriesList
                if (series.isEmpty()) return@Canvas

                val values = allValues(series)
                val (_, rawMax) = computeRange(values)
                val maxVal = if (rawMax == 0f) 1f else rawMax
                val plotW = w - PADDING_LEFT - PADDING_RIGHT
                val plotH = h - PADDING_TOP - PADDING_BOTTOM

                val nGroups = series.maxOfOrNull { it.points.size } ?: return@Canvas
                val nSeries = series.size
                val slotW = plotW / nGroups
                val spacing = slotW * ctx.attr.barSpacing
                val barW = (slotW - spacing) / nSeries

                fun toY(value: Float): Float =
                    PADDING_TOP + plotH * (1f - value / maxVal)

                if (ctx.attr.showGrid) {
                    val steps = ctx.attr.gridLineCount
                    context.beginPath()
                    context.strokeStyle(ctx.attr.gridColor)
                    context.lineWidth(0.5f)
                    for (i in 0..steps) {
                        val y = PADDING_TOP + plotH * i / steps
                        context.moveTo(PADDING_LEFT, y)
                        context.lineTo(w - PADDING_RIGHT, y)
                    }
                    context.stroke()
                }

                context.beginPath()
                context.strokeStyle(ctx.attr.axisColor)
                context.lineWidth(1f)
                context.moveTo(PADDING_LEFT, PADDING_TOP)
                context.lineTo(PADDING_LEFT, PADDING_TOP + plotH)
                context.lineTo(PADDING_LEFT + plotW, PADDING_TOP + plotH)
                context.stroke()

                if (ctx.attr.showAxisLabels) {
                    context.font(ctx.attr.labelFontSize)
                    context.fillStyle(ctx.attr.axisColor)
                    val steps = ctx.attr.gridLineCount
                    for (i in 0..steps) {
                        val v = maxVal * (steps - i) / steps
                        val y = PADDING_TOP + plotH * i / steps
                        context.fillText(v.fmt0(), 2f, y + 4f)
                    }
                }

                series.forEachIndexed { sIdx, s ->
                    context.fillStyle(s.color)
                    s.points.forEachIndexed { gIdx, pt ->
                        if (pt.value <= 0f) return@forEachIndexed
                        val x = PADDING_LEFT + spacing / 2f + gIdx * slotW + sIdx * barW
                        val barTop = toY(pt.value)
                        val barH = PADDING_TOP + plotH - barTop
                        val r = ctx.attr.cornerRadius.coerceAtMost(barW / 2f).coerceAtMost(barH / 2f)

                        context.beginPath()
                        context.moveTo(x + r, barTop)
                        context.lineTo(x + barW - r, barTop)
                        context.arc(x + barW - r, barTop + r, r, (-PI / 2).toFloat(), 0f, false)
                        context.lineTo(x + barW, barTop + barH)
                        context.lineTo(x, barTop + barH)
                        context.lineTo(x, barTop + r)
                        context.arc(x + r, barTop + r, r, PI.toFloat(), (-PI / 2).toFloat(), false)
                        context.closePath()
                        context.fill()

                        if (ctx.attr.showValueLabels) {
                            context.font(ctx.attr.labelFontSize)
                            context.fillStyle(s.color)
                            val label = if (pt.value == pt.value.roundToInt().toFloat()) {
                                pt.value.roundToInt().toString()
                            } else {
                                pt.value.fmt1()
                            }
                            context.fillText(label, x + barW / 2f - label.length * 3f, barTop - 4f)
                        }
                    }
                }

                if (ctx.attr.showAxisLabels) {
                    context.font(ctx.attr.labelFontSize)
                    context.fillStyle(ctx.attr.axisColor)
                    series.first().points.forEachIndexed { gIdx, pt ->
                        val x = PADDING_LEFT + spacing / 2f + gIdx * slotW + (nSeries - 1) * barW / 2f
                        context.fillText(pt.label, x - pt.label.length * 3f, PADDING_TOP + plotH + 18f)
                    }
                }

                // Tooltip overlay for the tapped bar
                val tx = ctx.attr.tapX
                val ty = ctx.attr.tapY
                if (ctx.attr.showTooltip && tx >= 0f && ctx.attr.tapText.isNotEmpty()) {
                    val text = ctx.attr.tapText
                    val padding = 5f
                    val fSize = 11f
                    val boxW = text.length * fSize * 0.6f + padding * 2f
                    val boxH = fSize + padding * 2f
                    var bx = tx - boxW / 2f
                    val by = (ty - boxH - 10f).coerceAtLeast(2f)
                    bx = bx.coerceIn(2f, w - boxW - 2f)

                    context.save()
                    context.fillStyle(Color(0xDD222222L))
                    context.beginPath()
                    val br = 4f
                    context.moveTo(bx + br, by)
                    context.lineTo(bx + boxW - br, by)
                    context.arc(bx + boxW - br, by + br, br, (-PI / 2).toFloat(), 0f, false)
                    context.lineTo(bx + boxW, by + boxH - br)
                    context.arc(bx + boxW - br, by + boxH - br, br, 0f, (PI / 2).toFloat(), false)
                    context.lineTo(bx + br, by + boxH)
                    context.arc(bx + br, by + boxH - br, br, (PI / 2).toFloat(), PI.toFloat(), false)
                    context.lineTo(bx, by + br)
                    context.arc(bx + br, by + br, br, PI.toFloat(), (-PI / 2).toFloat(), false)
                    context.closePath()
                    context.fill()

                    context.fillStyle(Color.WHITE)
                    context.font(fSize)
                    context.fillText(text, bx + padding, by + padding + fSize * 0.85f)
                    context.restore()
                }
            }
        }
    }

    private fun handleBarClick(params: ClickParams) {
        val w = lastW
        val h = lastH
        if (w == 0f || h == 0f) return
        val series = attr.seriesList
        if (series.isEmpty()) return
        val values = allValues(series)
        val (_, rawMax) = computeRange(values)
        val maxVal = if (rawMax == 0f) 1f else rawMax
        val plotW = w - PADDING_LEFT - PADDING_RIGHT
        val plotH = h - PADDING_TOP - PADDING_BOTTOM
        val nGroups = series.maxOfOrNull { it.points.size } ?: return
        val nSeries = series.size
        val slotW = plotW / nGroups
        val spacing = slotW * attr.barSpacing
        val barW = (slotW - spacing) / nSeries
        fun toY(value: Float): Float = PADDING_TOP + plotH * (1f - value / maxVal)
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { gIdx, pt ->
                if (pt.value <= 0f) return@forEachIndexed
                val x = PADDING_LEFT + spacing / 2f + gIdx * slotW + sIdx * barW
                val barTop = toY(pt.value)
                val barBottom = PADDING_TOP + plotH
                if (params.x >= x && params.x <= x + barW && params.y >= barTop && params.y <= barBottom) {
                    attr.tapX = x + barW / 2f
                    attr.tapY = barTop
                    attr.tapText = "${pt.label}: ${pt.value.fmt1()}"
                    event.onPointClickHandler?.invoke(sIdx, gIdx, pt.value)
                    return
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StackedBarChart
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.StackedBarChart(init: StackedBarChartView.() -> Unit) {
    addChild(StackedBarChartView(), init)
}

class StackedBarChartAttr : BarChartAttr() {
    internal var percentMode by observable(false)
    internal var showValues by observable(true)

    fun percentMode(enable: Boolean) { percentMode = enable }
    override fun showValueLabels(show: Boolean) { showValues = show }
}

class StackedBarChartView : ComposeView<StackedBarChartAttr, ChartEvent>() {

    override fun createAttr(): StackedBarChartAttr = StackedBarChartAttr()
    override fun createEvent(): ChartEvent = ChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { absolutePositionAllZero() } }) { context, w, h ->
                val a = ctx.attr
                val series = a.seriesList
                if (series.isEmpty()) return@Canvas

                val nGroups = series.maxOfOrNull { it.points.size } ?: return@Canvas
                val plotW = w - PADDING_LEFT - PADDING_RIGHT
                val plotH = h - PADDING_TOP - PADDING_BOTTOM
                val slotW = plotW / nGroups
                val barW = slotW * (1f - a.barSpacing.coerceIn(0f, 0.8f))
                val barOffset = (slotW - barW) / 2f

                // Compute column totals for scaling
                val colTotals = FloatArray(nGroups) { g ->
                    series.sumOf { s -> s.points.getOrNull(g)?.value?.toDouble() ?: 0.0 }.toFloat()
                }
                val globalMax = if (a.percentMode) 1f else (colTotals.maxOrNull()?.takeIf { it > 0f } ?: 1f)

                // Draw grid
                if (a.showGrid) {
                    val steps = a.gridLineCount.coerceIn(2, 10)
                    context.beginPath()
                    context.strokeStyle(a.gridColor)
                    context.lineWidth(0.5f)
                    for (i in 0..steps) {
                        val y = PADDING_TOP + plotH * i / steps
                        context.moveTo(PADDING_LEFT, y)
                        context.lineTo(w - PADDING_RIGHT, y)
                    }
                    context.stroke()
                }

                // Draw stacked bars
                for (g in 0 until nGroups) {
                    val colTotal = colTotals[g].takeIf { it > 0f } ?: continue
                    val x = PADDING_LEFT + barOffset + g * slotW
                    var baseY = PADDING_TOP + plotH

                    series.forEach { s ->
                        val raw = s.points.getOrNull(g)?.value ?: 0f
                        if (raw <= 0f) return@forEach
                        val fraction = if (a.percentMode) raw / colTotal else raw / globalMax
                        val segH = (plotH * fraction).coerceAtLeast(1f)
                        val segY = baseY - segH

                        context.beginPath()
                        if (a.cornerRadius > 0f && baseY == PADDING_TOP + plotH) {
                            val r = a.cornerRadius.coerceAtMost(segH / 2f)
                            context.moveTo(x + r, segY)
                            context.lineTo(x + barW - r, segY)
                            context.quadraticCurveTo(x + barW, segY, x + barW, segY + r)
                            context.lineTo(x + barW, segY + segH)
                            context.lineTo(x, segY + segH)
                            context.lineTo(x, segY + r)
                            context.quadraticCurveTo(x, segY, x + r, segY)
                        } else {
                            context.moveTo(x, segY)
                            context.lineTo(x + barW, segY)
                            context.lineTo(x + barW, baseY)
                            context.lineTo(x, baseY)
                        }
                        context.closePath()
                        context.fillStyle(s.color)
                        context.fill()

                        if (a.showValues && segH > 14f) {
                            val label = if (a.percentMode) "${(fraction * 100).toInt()}%" else raw.fmt1()
                            context.font(a.labelFontSize)
                            context.fillStyle(Color.WHITE)
                            context.textAlign(TextAlign.CENTER)
                            context.fillText(label, x + barW / 2f, segY + segH / 2f + a.labelFontSize * 0.35f)
                            context.textAlign(TextAlign.LEFT)
                        }

                        baseY = segY
                    }

                    // X axis labels
                    if (a.showAxisLabels) {
                        val lbl = series.firstOrNull()?.points?.getOrNull(g)?.label ?: continue
                        context.font(a.labelFontSize)
                        context.fillStyle(a.axisColor)
                        context.textAlign(TextAlign.CENTER)
                        context.fillText(lbl, x + barW / 2f, h - PADDING_BOTTOM + a.labelFontSize + 2f)
                        context.textAlign(TextAlign.LEFT)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AreaChart - LineChart with fill enabled by default
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.AreaChart(init: AreaChartView.() -> Unit) {
    addChild(AreaChartView(), init)
}

class AreaChartAttr : LineChartAttr() {
    init {
        fillArea = true
        showDots = false
    }
}

class AreaChartView : LineChartView() {
    override fun createAttr(): AreaChartAttr = AreaChartAttr()
}

// ---------------------------------------------------------------------------
// PieChart / DonutChart
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.PieChart(init: PieChartView.() -> Unit) {
    addChild(PieChartView(), init)
}

data class PieSlice(val label: String, val value: Float, val color: Color)

class PieChartAttr : ComposeAttr() {

    internal var slices by observable(emptyList<PieSlice>())
    internal var holeRadius by observable(0f)
    internal var strokeColor by observable(Color.WHITE)
    internal var strokeWidth by observable(2f)
    internal var showLabels by observable(true)
    internal var showLegend by observable(true)
    internal var labelFontSize by observable(11f)
    internal var startAngleDeg by observable(-90f)

    fun data(vararg slices: PieSlice) { this.slices = slices.toList() }
    fun data(slices: List<PieSlice>) { this.slices = slices }
    fun holeRadius(fraction: Float) { holeRadius = fraction.coerceIn(0f, 0.9f) }
    fun strokeColor(color: Color) { strokeColor = color }
    fun strokeWidth(width: Float) { strokeWidth = width.coerceAtLeast(0f) }
    fun showLabels(show: Boolean) { showLabels = show }
    fun showLegend(show: Boolean) { showLegend = show }
    fun labelFontSize(size: Float) { labelFontSize = size }
    fun startAngle(degrees: Float) { startAngleDeg = degrees }

    fun size(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }
}

class PieChartEvent : ComposeEvent() {

    internal var onSliceClickHandler: ((index: Int, label: String, value: Float) -> Unit)? = null

    fun onSliceClick(handler: (index: Int, label: String, value: Float) -> Unit) {
        onSliceClickHandler = handler
    }
}

class PieChartView : ComposeView<PieChartAttr, PieChartEvent>() {

    private var lastW = 0f
    private var lastH = 0f
    private var lastCx = 0f
    private var lastCy = 0f
    private var lastOuterR = 0f
    private var lastInnerR = 0f
    private var lastStartAngle = 0f
    private var lastSweepAngles = emptyList<Float>()

    override fun createAttr(): PieChartAttr = PieChartAttr()
    override fun createEvent(): PieChartEvent = PieChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event {
                    click { params -> ctx.handlePieClick(params) }
                }
            }) { context, w, h ->
                ctx.lastW = w
                ctx.lastH = h
                val slices = ctx.attr.slices
                if (slices.isEmpty()) return@Canvas
                val total = slices.sumOf { it.value.toDouble() }.toFloat()
                if (total <= 0f) return@Canvas

                val legendH = if (ctx.attr.showLegend) 28f else 0f
                val chartH = h - legendH
                val outerR = (minOf(w, chartH) / 2f - 8f).coerceAtLeast(10f)
                val innerR = outerR * ctx.attr.holeRadius
                val cx = w / 2f
                val cy = chartH / 2f

                ctx.lastCx = cx
                ctx.lastCy = cy
                ctx.lastOuterR = outerR
                ctx.lastInnerR = innerR

                val startAngleRad = ctx.attr.startAngleDeg * PI.toFloat() / 180f
                ctx.lastStartAngle = startAngleRad

                val sweepAngles = slices.map { (2f * PI.toFloat()) * it.value / total }
                ctx.lastSweepAngles = sweepAngles

                var angle = startAngleRad
                slices.forEachIndexed { idx, slice ->
                    val sweep = sweepAngles[idx]
                    val endAngle = angle + sweep
                    context.beginPath()
                    if (innerR > 0f) {
                        context.moveTo(cx + innerR * cos(angle), cy + innerR * sin(angle))
                        context.lineTo(cx + outerR * cos(angle), cy + outerR * sin(angle))
                        context.arc(cx, cy, outerR, angle, endAngle, false)
                        context.arc(cx, cy, innerR, endAngle, angle, true)
                    } else {
                        context.moveTo(cx, cy)
                        context.lineTo(cx + outerR * cos(angle), cy + outerR * sin(angle))
                        context.arc(cx, cy, outerR, angle, endAngle, false)
                    }
                    context.closePath()
                    context.fillStyle(slice.color)
                    context.fill()
                    if (ctx.attr.strokeWidth > 0f) {
                        context.strokeStyle(ctx.attr.strokeColor)
                        context.lineWidth(ctx.attr.strokeWidth)
                        context.stroke()
                    }
                    angle = endAngle
                }

                if (ctx.attr.showLabels) {
                    context.font(ctx.attr.labelFontSize)
                    context.textAlign(TextAlign.CENTER)
                    context.fillStyle(Color.WHITE)
                    angle = startAngleRad
                    slices.forEachIndexed { idx, slice ->
                        val sweep = sweepAngles[idx]
                        val midAngle = angle + sweep / 2f
                        val labelR = (innerR + outerR) / 2f
                        val lx = cx + labelR * cos(midAngle)
                        val ly = cy + labelR * sin(midAngle) + ctx.attr.labelFontSize * 0.35f
                        val pct = (slice.value / total * 100f).roundToInt()
                        if (sweep > 0.26f) context.fillText("$pct%", lx, ly)
                        angle += sweep
                    }
                    context.textAlign(TextAlign.LEFT)
                }

                if (ctx.attr.showLegend) {
                    val legendY = chartH + 6f
                    val slotW = w / slices.size.coerceAtLeast(1)
                    context.font(ctx.attr.labelFontSize)
                    slices.forEachIndexed { idx, slice ->
                        val lx = idx * slotW + 4f
                        context.fillStyle(slice.color)
                        context.fillRect(lx, legendY, 10f, 10f)
                        context.fillStyle(Color(0xFF505050L))
                        val label = if (slice.label.length > 6) slice.label.take(6) else slice.label
                        context.fillText(label, lx + 13f, legendY + 9f)
                    }
                }
            }
        }
    }

    private fun handlePieClick(params: ClickParams) {
        val handler = event.onSliceClickHandler ?: return
        if (lastW == 0f || lastH == 0f) return
        val slices = attr.slices
        if (slices.isEmpty()) return
        val dx = params.x - lastCx
        val dy = params.y - lastCy
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < lastInnerR || dist > lastOuterR) return
        var clickAngle = atan2(dy, dx)
        val startA = lastStartAngle
        val twoPi = 2f * PI.toFloat()
        while (clickAngle < startA) clickAngle += twoPi
        while (clickAngle >= startA + twoPi) clickAngle -= twoPi
        var angle = startA
        lastSweepAngles.forEachIndexed { idx, sweep ->
            val endAngle = angle + sweep
            if (clickAngle in angle..endAngle) {
                handler(idx, slices[idx].label, slices[idx].value)
                return
            }
            angle = endAngle
        }
    }
}

// ---------------------------------------------------------------------------
// RadarChart (spider / web chart)
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.RadarChart(init: RadarChartView.() -> Unit) {
    addChild(RadarChartView(), init)
}

data class RadarAxis(val label: String, val max: Float)

data class RadarSeries(
    val name: String,
    val values: List<Float>,
    val color: Color,
    val fillColor: Color = Color(red255 = 180, green255 = 180, blue255 = 180, alpha01 = 0.2f),
)

class RadarChartAttr : ComposeAttr() {
    internal var axes by observable(emptyList<RadarAxis>())
    internal var series by observable(emptyList<RadarSeries>())
    internal var webColor by observable(Color(0xFFDCDCDCL))
    internal var webLevels by observable(4)
    internal var showAxisLabels by observable(true)
    internal var labelFontSize by observable(11f)
    internal var showLegend by observable(true)

    fun axes(vararg axis: RadarAxis) { axes = axis.toList() }
    fun axes(list: List<RadarAxis>) { axes = list }
    fun series(vararg s: RadarSeries) { series = s.toList() }
    fun series(list: List<RadarSeries>) { series = list }
    fun webColor(color: Color) { webColor = color }
    fun webLevels(count: Int) { webLevels = count.coerceIn(2, 8) }
    fun showAxisLabels(show: Boolean) { showAxisLabels = show }
    fun labelFontSize(size: Float) { labelFontSize = size }
    fun showLegend(show: Boolean) { showLegend = show }
    fun size(w: Float, h: Float) { if (!w.isNaN()) width(w); if (!h.isNaN()) height(h) }
}

class RadarChartEvent : ComposeEvent() {
    internal var onSeriesClickHandler: ((seriesIndex: Int, name: String) -> Unit)? = null
    fun onSeriesClick(handler: (seriesIndex: Int, name: String) -> Unit) { onSeriesClickHandler = handler }
}

class RadarChartView : ComposeView<RadarChartAttr, RadarChartEvent>() {
    override fun createAttr(): RadarChartAttr = RadarChartAttr()
    override fun createEvent(): RadarChartEvent = RadarChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { absolutePositionAllZero() } }) { context, w, h ->
                val a = ctx.attr
                val n = a.axes.size
                if (n < 3) return@Canvas

                val legendH = if (a.showLegend) 24f else 0f
                val chartH = h - legendH
                val cx = w / 2f
                val cy = chartH / 2f
                val r = (minOf(w, chartH) / 2f - 36f).coerceAtLeast(10f)
                val levels = a.webLevels

                for (level in 1..levels) {
                    val frac = level.toFloat() / levels
                    context.beginPath()
                    for (i in 0 until n) {
                        val angle = 2f * PI.toFloat() * i / n - PI.toFloat() / 2f
                        val px = cx + r * frac * cos(angle)
                        val py = cy + r * frac * sin(angle)
                        if (i == 0) context.moveTo(px, py) else context.lineTo(px, py)
                    }
                    context.closePath()
                    context.strokeStyle(a.webColor)
                    context.lineWidth(1f)
                    context.stroke()
                }

                for (i in 0 until n) {
                    val angle = 2f * PI.toFloat() * i / n - PI.toFloat() / 2f
                    context.beginPath()
                    context.moveTo(cx, cy)
                    context.lineTo(cx + r * cos(angle), cy + r * sin(angle))
                    context.strokeStyle(a.webColor)
                    context.lineWidth(1f)
                    context.stroke()
                }

                if (a.showAxisLabels) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(0xFF505050L))
                    for (i in 0 until n) {
                        val angle = 2f * PI.toFloat() * i / n - PI.toFloat() / 2f
                        val labelR = r + 18f
                        val lx = cx + labelR * cos(angle)
                        val ly = cy + labelR * sin(angle)
                        val label = a.axes[i].label
                        val offsetX = when {
                            cos(angle) > 0.3f -> 0f
                            cos(angle) < -0.3f -> -(label.length * 6f)
                            else -> -(label.length * 3f)
                        }
                        context.fillText(label, lx + offsetX, ly + 4f)
                    }
                }

                a.series.forEach { s ->
                    if (s.values.size < n) return@forEach
                    context.beginPath()
                    for (i in 0 until n) {
                        val frac = (s.values[i] / a.axes[i].max).coerceIn(0f, 1f)
                        val angle = 2f * PI.toFloat() * i / n - PI.toFloat() / 2f
                        val px = cx + r * frac * cos(angle)
                        val py = cy + r * frac * sin(angle)
                        if (i == 0) context.moveTo(px, py) else context.lineTo(px, py)
                    }
                    context.closePath()
                    context.fillStyle(s.fillColor)
                    context.fill()
                    context.strokeStyle(s.color)
                    context.lineWidth(2f)
                    context.stroke()

                    for (i in 0 until n) {
                        val frac = (s.values[i] / a.axes[i].max).coerceIn(0f, 1f)
                        val angle = 2f * PI.toFloat() * i / n - PI.toFloat() / 2f
                        val px = cx + r * frac * cos(angle)
                        val py = cy + r * frac * sin(angle)
                        context.beginPath()
                        context.arc(px, py, 4f, 0f, 2f * PI.toFloat(), false)
                        context.fillStyle(s.color)
                        context.fill()
                    }
                }

                if (a.showLegend && a.series.isNotEmpty()) {
                    val legendY = chartH + 6f
                    val slotW = w / a.series.size.coerceAtLeast(1)
                    context.font(a.labelFontSize)
                    a.series.forEachIndexed { idx, s ->
                        val lx = idx * slotW + 4f
                        context.fillStyle(s.color)
                        context.fillRect(lx, legendY, 10f, 10f)
                        context.fillStyle(Color(0xFF505050L))
                        context.fillText(s.name.take(8), lx + 14f, legendY + 9f)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// GaugeChart (semi-circular gauge)
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.GaugeChart(init: GaugeChartView.() -> Unit) {
    addChild(GaugeChartView(), init)
}

class GaugeChartAttr : ComposeAttr() {
    internal var value by observable(0f)
    internal var minValue by observable(0f)
    internal var maxValue by observable(100f)
    internal var arcColor by observable(Color(0xFF1677FFL))
    internal var trackColor by observable(Color(0xFFDCDCDCL))
    internal var arcWidth by observable(16f)
    internal var showNeedle by observable(true)
    internal var needleColor by observable(Color(0xFF505050L))
    internal var label by observable("")
    internal var unit by observable("")
    internal var showMinMax by observable(true)
    internal var valueFontSize by observable(24f)
    internal var labelFontSize by observable(12f)

    fun value(v: Float) { value = v }
    fun range(min: Float, max: Float) { minValue = min; maxValue = max }
    fun arcColor(color: Color) { arcColor = color }
    fun trackColor(color: Color) { trackColor = color }
    fun arcWidth(w: Float) { arcWidth = w.coerceIn(4f, 48f) }
    fun showNeedle(show: Boolean) { showNeedle = show }
    fun needleColor(color: Color) { needleColor = color }
    fun label(text: String) { label = text }
    fun unit(text: String) { unit = text }
    fun showMinMax(show: Boolean) { showMinMax = show }
    fun valueFontSize(size: Float) { valueFontSize = size }
    fun labelFontSize(size: Float) { labelFontSize = size }
    fun size(w: Float, h: Float) { if (!w.isNaN()) width(w); if (!h.isNaN()) height(h) }
}

class GaugeChartView : ComposeView<GaugeChartAttr, ComposeEvent>() {
    override fun createAttr(): GaugeChartAttr = GaugeChartAttr()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { absolutePositionAllZero() } }) { context, w, h ->
                val a = ctx.attr
                val cx = w / 2f
                val cy = h * 0.62f
                val r = (minOf(w * 0.85f, h * 1.15f) / 2f).coerceAtLeast(20f)

                val startDeg = 135f
                val sweepDeg = 270f
                val startRad = startDeg * PI.toFloat() / 180f
                val endRad = (startDeg + sweepDeg) * PI.toFloat() / 180f

                val clampedValue = a.value.coerceIn(a.minValue, a.maxValue)
                val fraction = if (a.maxValue > a.minValue)
                    (clampedValue - a.minValue) / (a.maxValue - a.minValue) else 0f
                val valueRad = startRad + sweepDeg * PI.toFloat() / 180f * fraction
                val midR = r - a.arcWidth / 2f

                context.beginPath()
                context.arc(cx, cy, midR, startRad, endRad, false)
                context.strokeStyle(a.trackColor)
                context.lineWidth(a.arcWidth)
                context.stroke()

                if (fraction > 0f) {
                    context.beginPath()
                    context.arc(cx, cy, midR, startRad, valueRad, false)
                    context.strokeStyle(a.arcColor)
                    context.lineWidth(a.arcWidth)
                    context.stroke()
                }

                if (a.showNeedle) {
                    val needleLen = midR - a.arcWidth / 2f - 4f
                    val nx = cx + needleLen * cos(valueRad)
                    val ny = cy + needleLen * sin(valueRad)
                    context.beginPath()
                    context.moveTo(cx, cy)
                    context.lineTo(nx, ny)
                    context.strokeStyle(a.needleColor)
                    context.lineWidth(2.5f)
                    context.stroke()
                    context.beginPath()
                    context.arc(cx, cy, 5f, 0f, 2f * PI.toFloat(), false)
                    context.fillStyle(a.needleColor)
                    context.fill()
                }

                if (a.showMinMax) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(0xFF787878L))
                    val labelR = r + 6f
                    val minX = cx + labelR * cos(startRad)
                    val minY = cy + labelR * sin(startRad)
                    context.fillText(a.minValue.fmt0(), minX - 14f, minY + 4f)
                    val maxX = cx + labelR * cos(endRad)
                    val maxY = cy + labelR * sin(endRad)
                    context.fillText(a.maxValue.fmt0(), maxX - 6f, maxY + 4f)
                }

                context.font(a.valueFontSize)
                context.fillStyle(Color(0xFF282828L))
                val valText = if (a.unit.isEmpty()) clampedValue.fmt1() else "${clampedValue.fmt0()}${a.unit}"
                val valW = valText.length * a.valueFontSize * 0.55f
                context.fillText(valText, cx - valW / 2f, cy + r * 0.1f)

                if (a.label.isNotEmpty()) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(0xFF787878L))
                    val lblW = a.label.length * a.labelFontSize * 0.55f
                    context.fillText(a.label, cx - lblW / 2f, cy + r * 0.1f + a.valueFontSize + 4f)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ScatterChart (scatter / bubble chart)
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.ScatterChart(init: ScatterChartView.() -> Unit) {
    addChild(ScatterChartView(), init)
}

data class ScatterPoint(
    val x: Float,
    val y: Float,
    val size: Float = 6f,
    val label: String = "",
)

data class ScatterSeries(
    val name: String,
    val points: List<ScatterPoint>,
    val color: Color,
)

class ScatterChartAttr : ComposeAttr() {
    internal var seriesList by observable(emptyList<ScatterSeries>())
    internal var showGrid by observable(true)
    internal var showAxisLabels by observable(true)
    internal var axisColor by observable(Color(0xFFB4B4B4L))
    internal var gridColor by observable(Color(0xFFDCDCDCL))
    internal var labelFontSize by observable(11f)
    internal var showLegend by observable(true)
    internal var xAxisLabel by observable("")
    internal var yAxisLabel by observable("")

    fun data(vararg series: ScatterSeries) { seriesList = series.toList() }
    fun data(list: List<ScatterSeries>) { seriesList = list }
    fun showGrid(show: Boolean) { showGrid = show }
    fun showAxisLabels(show: Boolean) { showAxisLabels = show }
    fun axisColor(color: Color) { axisColor = color }
    fun gridColor(color: Color) { gridColor = color }
    fun labelFontSize(size: Float) { labelFontSize = size }
    fun showLegend(show: Boolean) { showLegend = show }
    fun xAxisLabel(text: String) { xAxisLabel = text }
    fun yAxisLabel(text: String) { yAxisLabel = text }
    fun size(w: Float, h: Float) { if (!w.isNaN()) width(w); if (!h.isNaN()) height(h) }
}

class ScatterChartEvent : ComposeEvent() {
    internal var onPointClickHandler: ((seriesIndex: Int, pointIndex: Int, x: Float, y: Float) -> Unit)? = null
    fun onPointClick(handler: (seriesIndex: Int, pointIndex: Int, x: Float, y: Float) -> Unit) {
        onPointClickHandler = handler
    }
}

class ScatterChartView : ComposeView<ScatterChartAttr, ScatterChartEvent>() {

    private var lastPL = PADDING_LEFT
    private var lastPT = PADDING_TOP
    private var lastCW = 0f
    private var lastCH = 0f
    private var lastXMin = 0f
    private var lastXRange = 1f
    private var lastYMin = 0f
    private var lastYRange = 1f

    override fun createAttr(): ScatterChartAttr = ScatterChartAttr()
    override fun createEvent(): ScatterChartEvent = ScatterChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event { click { params -> ctx.handleScatterClick(params) } }
            }) { context, w, h ->
                val a = ctx.attr
                val allPts = a.seriesList.flatMap { it.points }
                if (allPts.isEmpty()) return@Canvas

                val legendH = if (a.showLegend) 24f else 0f
                val pl = PADDING_LEFT
                val pt = PADDING_TOP
                val chartW = (w - pl - PADDING_RIGHT).coerceAtLeast(10f)
                val chartH = (h - pt - PADDING_BOTTOM - legendH).coerceAtLeast(10f)

                val xMin = allPts.minOf { it.x }
                val xMax = allPts.maxOf { it.x }
                val yMin = allPts.minOf { it.y }
                val yMax = allPts.maxOf { it.y }
                val xRange = if (abs(xMax - xMin) < 1e-6f) 1f else xMax - xMin
                val yRange = if (abs(yMax - yMin) < 1e-6f) 1f else yMax - yMin

                ctx.lastPL = pl; ctx.lastPT = pt
                ctx.lastCW = chartW; ctx.lastCH = chartH
                ctx.lastXMin = xMin; ctx.lastXRange = xRange
                ctx.lastYMin = yMin; ctx.lastYRange = yRange

                if (a.showGrid) {
                    val gridLines = 4
                    context.strokeStyle(a.gridColor)
                    context.lineWidth(0.5f)
                    for (i in 0..gridLines) {
                        val gy = pt + chartH * i / gridLines
                        context.beginPath()
                        context.moveTo(pl, gy); context.lineTo(pl + chartW, gy)
                        context.stroke()
                        val gx = pl + chartW * i / gridLines
                        context.beginPath()
                        context.moveTo(gx, pt); context.lineTo(gx, pt + chartH)
                        context.stroke()
                    }
                }

                context.strokeStyle(a.axisColor)
                context.lineWidth(1.5f)
                context.beginPath()
                context.moveTo(pl, pt)
                context.lineTo(pl, pt + chartH)
                context.lineTo(pl + chartW, pt + chartH)
                context.stroke()

                if (a.showAxisLabels) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(0xFF646464L))
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val xVal = xMin + xRange * i / gridLines
                        context.fillText(xVal.fmt1(), pl + chartW * i / gridLines - 8f, pt + chartH + 14f)
                        val yVal = yMax - yRange * i / gridLines
                        context.fillText(yVal.fmt1(), 2f, pt + chartH * i / gridLines + 4f)
                    }
                }

                a.seriesList.forEach { series ->
                    series.points.forEach { point ->
                        val px = pl + (point.x - xMin) / xRange * chartW
                        val py = pt + (1f - (point.y - yMin) / yRange) * chartH
                        val radius = point.size.coerceIn(3f, 30f)
                        context.beginPath()
                        context.arc(px, py, radius, 0f, 2f * PI.toFloat(), false)
                        context.fillStyle(series.color)
                        context.fill()
                    }
                }

                if (a.showLegend && a.seriesList.isNotEmpty()) {
                    val legendY = h - legendH + 6f
                    val slotW = w / a.seriesList.size.coerceAtLeast(1)
                    context.font(a.labelFontSize)
                    a.seriesList.forEachIndexed { idx, s ->
                        val lx = idx * slotW + 4f
                        context.beginPath()
                        context.arc(lx + 5f, legendY + 5f, 5f, 0f, 2f * PI.toFloat(), false)
                        context.fillStyle(s.color)
                        context.fill()
                        context.fillStyle(Color(0xFF505050L))
                        context.fillText(s.name.take(8), lx + 14f, legendY + 9f)
                    }
                }
            }
        }
    }

    private fun handleScatterClick(params: ClickParams) {
        val handler = event.onPointClickHandler ?: return
        if (lastCW == 0f || lastCH == 0f) return
        val a = attr
        val clickX = lastXMin + (params.x - lastPL) / lastCW * lastXRange
        val clickY = lastYMin + (1f - (params.y - lastPT) / lastCH) * lastYRange
        var bestDist = Float.MAX_VALUE
        var bestS = -1; var bestP = -1
        a.seriesList.forEachIndexed { sIdx, series ->
            series.points.forEachIndexed { pIdx, pt ->
                val d = sqrt((pt.x - clickX) * (pt.x - clickX) + (pt.y - clickY) * (pt.y - clickY))
                if (d < bestDist) { bestDist = d; bestS = sIdx; bestP = pIdx }
            }
        }
        if (bestS >= 0) {
            val p = a.seriesList[bestS].points[bestP]
            handler(bestS, bestP, p.x, p.y)
        }
    }
}

// ---------------------------------------------------------------------------
// FunnelChart (ECharts Funnel style)
// ---------------------------------------------------------------------------

fun ViewContainer<*, *>.FunnelChart(init: FunnelChartView.() -> Unit) {
    addChild(FunnelChartView(), init)
}

data class FunnelSlice(val label: String, val value: Float, val color: Color)

class FunnelChartAttr : ComposeAttr() {
    internal var slices by observable(emptyList<FunnelSlice>())
    internal var showLabels by observable(true)
    internal var showValues by observable(true)
    internal var showLegend by observable(true)
    internal var gap by observable(4f)
    internal var labelFontSize by observable(12f)
    internal var sort by observable(true)
    internal var strokeColor by observable(Color.WHITE)
    internal var strokeWidth by observable(1.5f)

    fun data(vararg slices: FunnelSlice) { this.slices = slices.toList() }
    fun data(list: List<FunnelSlice>) { slices = list }
    fun showLabels(show: Boolean) { showLabels = show }
    fun showValues(show: Boolean) { showValues = show }
    fun showLegend(show: Boolean) { showLegend = show }
    fun gap(g: Float) { gap = g.coerceAtLeast(0f) }
    fun labelFontSize(size: Float) { labelFontSize = size }
    fun sort(enabled: Boolean) { sort = enabled }
    fun strokeColor(color: Color) { strokeColor = color }
    fun strokeWidth(w: Float) { strokeWidth = w.coerceAtLeast(0f) }
    fun size(w: Float, h: Float) { if (!w.isNaN()) width(w); if (!h.isNaN()) height(h) }
}

class FunnelChartEvent : ComposeEvent() {
    internal var onSliceClickHandler: ((index: Int, label: String, value: Float) -> Unit)? = null
    fun onSliceClick(handler: (index: Int, label: String, value: Float) -> Unit) { onSliceClickHandler = handler }
}

class FunnelChartView : ComposeView<FunnelChartAttr, FunnelChartEvent>() {

    // [topLX, topRX, topY, botLX, botRX, botY] per slice
    private var lastSliceRects = emptyList<FloatArray>()
    private var lastSortedSlices = emptyList<FunnelSlice>()

    override fun createAttr(): FunnelChartAttr = FunnelChartAttr()
    override fun createEvent(): FunnelChartEvent = FunnelChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { absolutePositionAllZero() }
                event { click { params -> ctx.handleClick(params) } }
            }) { context, w, h ->
                val a = ctx.attr
                val raw = a.slices
                if (raw.isEmpty()) return@Canvas
                val slices = if (a.sort) raw.sortedByDescending { it.value } else raw
                ctx.lastSortedSlices = slices
                val n = slices.size
                val maxVal = slices.maxOf { it.value }.takeIf { it > 0f } ?: 1f

                val legendH = if (a.showLegend) 24f else 0f
                val chartW = (w - PADDING_LEFT - PADDING_RIGHT).coerceAtLeast(10f)
                val chartH = (h - PADDING_TOP - PADDING_BOTTOM - legendH).coerceAtLeast(10f)
                val sliceH = ((chartH - a.gap * (n - 1)) / n).coerceAtLeast(4f)
                val cx = PADDING_LEFT + chartW / 2f
                val rects = mutableListOf<FloatArray>()

                slices.forEachIndexed { idx, slice ->
                    val topW = chartW * slice.value / maxVal
                    val nextW = if (idx < n - 1) chartW * slices[idx + 1].value / maxVal else topW * 0.3f
                    val topY = PADDING_TOP + idx * (sliceH + a.gap)
                    val botY = topY + sliceH
                    val topLX = cx - topW / 2f
                    val topRX = cx + topW / 2f
                    val botLX = cx - nextW / 2f
                    val botRX = cx + nextW / 2f

                    rects.add(floatArrayOf(topLX, topRX, topY, botLX, botRX, botY))

                    context.beginPath()
                    context.moveTo(topLX, topY)
                    context.lineTo(topRX, topY)
                    context.lineTo(botRX, botY)
                    context.lineTo(botLX, botY)
                    context.closePath()
                    context.fillStyle(slice.color)
                    context.fill()
                    if (a.strokeWidth > 0f) {
                        context.strokeStyle(a.strokeColor)
                        context.lineWidth(a.strokeWidth)
                        context.stroke()
                    }

                    if (a.showLabels) {
                        context.font(a.labelFontSize)
                        context.fillStyle(Color.WHITE)
                        val labelText = if (a.showValues) "${slice.label}: ${slice.value.fmt0()}" else slice.label
                        val midY = (topY + botY) / 2f + a.labelFontSize * 0.35f
                        context.fillText(labelText, cx - labelText.length * a.labelFontSize * 0.3f, midY)
                    }
                }
                ctx.lastSliceRects = rects

                if (a.showLegend) {
                    val legendY = h - legendH + 6f
                    val slotW = w / slices.size.coerceAtLeast(1)
                    context.font(a.labelFontSize)
                    slices.forEachIndexed { idx, slice ->
                        val lx = idx * slotW + 4f
                        context.fillStyle(slice.color)
                        context.fillRect(lx, legendY, 10f, 10f)
                        context.fillStyle(Color(0xFF505050L))
                        context.fillText(slice.label.take(6), lx + 14f, legendY + 9f)
                    }
                }
            }
        }
    }

    private fun handleClick(params: ClickParams) {
        val handler = event.onSliceClickHandler ?: return
        val slices = lastSortedSlices
        lastSliceRects.forEachIndexed { idx, rect ->
            if (idx >= slices.size) return@forEachIndexed
            val topLX = rect[0]; val topRX = rect[1]; val topY = rect[2]
            val botLX = rect[3]; val botRX = rect[4]; val botY = rect[5]
            if (params.y < topY || params.y > botY) return@forEachIndexed
            val frac = if (botY > topY) (params.y - topY) / (botY - topY) else 0f
            val lx = topLX + (botLX - topLX) * frac
            val rx = topRX + (botRX - topRX) * frac
            if (params.x in lx..rx) {
                handler(idx, slices[idx].label, slices[idx].value)
                return
            }
        }
    }
}

fun ViewContainer<*, *>.WaterfallChart(init: WaterfallChartView.() -> Unit) {
    addChild(WaterfallChartView(), init)
}

enum class WaterfallBarType { START, INCREASE, DECREASE, TOTAL }

data class WaterfallBar(
    val label: String,
    val value: Float,
    val type: WaterfallBarType = WaterfallBarType.INCREASE,
)

class WaterfallChartAttr : ComposeAttr() {
    internal var bars by observable(emptyList<WaterfallBar>())
    internal var increaseColor by observable(Color(0xFF4CAF50L))
    internal var decreaseColor by observable(Color(0xFFF44336L))
    internal var totalColor by observable(Color(0xFF1677FFL))
    internal var startColor by observable(Color(0xFF9E9E9EL))
    internal var strokeColor by observable(Color(0x33000000L))
    internal var showValues by observable(true)
    internal var showLabels by observable(true)
    internal var barWidthFraction by observable(0.6f)
    internal var labelFontSize by observable(11f)
    internal var axisFontSize by observable(10f)
    internal var connectorColor by observable(Color(0xFFBBBBBBL))
    internal var showConnectors by observable(true)

    fun bars(list: List<WaterfallBar>) { bars = list }
    fun bars(vararg bar: WaterfallBar) { bars = bar.toList() }
    fun increaseColor(c: Color) { increaseColor = c }
    fun decreaseColor(c: Color) { decreaseColor = c }
    fun totalColor(c: Color) { totalColor = c }
    fun startColor(c: Color) { startColor = c }
    fun showValues(show: Boolean) { showValues = show }
    fun showLabels(show: Boolean) { showLabels = show }
    fun barWidthFraction(f: Float) { barWidthFraction = f.coerceIn(0.2f, 0.9f) }
    fun showConnectors(show: Boolean) { showConnectors = show }
}

class WaterfallChartView : ComposeView<WaterfallChartAttr, ComposeEvent>() {
    override fun createAttr(): WaterfallChartAttr = WaterfallChartAttr()
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { allFill() } }) { context, width, height ->
                val a = ctx.attr
                if (a.bars.isEmpty()) return@Canvas
                val bars = a.bars
                val n = bars.size
                val padL = 36f
                val padR = 8f
                val padT = 16f
                val padB = 28f
                val chartW = width - padL - padR
                val chartH = height - padT - padB
                val slotW = chartW / n

                val runningBase = FloatArray(n)
                val runningTop = FloatArray(n)
                var running = 0f
                bars.forEachIndexed { i, bar ->
                    when (bar.type) {
                        WaterfallBarType.START -> {
                            runningBase[i] = 0f
                            runningTop[i] = bar.value
                            running = bar.value
                        }
                        WaterfallBarType.INCREASE -> {
                            runningBase[i] = running
                            running += bar.value
                            runningTop[i] = running
                        }
                        WaterfallBarType.DECREASE -> {
                            runningTop[i] = running
                            running -= bar.value
                            runningBase[i] = running
                        }
                        WaterfallBarType.TOTAL -> {
                            runningBase[i] = 0f
                            runningTop[i] = running
                        }
                    }
                }
                val allVals = runningBase.toList() + runningTop.toList()
                val minVal = minOf(0f, allVals.min())
                val maxVal = maxOf(0f, allVals.max())
                val range = (maxVal - minVal).coerceAtLeast(1f)
                fun toY(v: Float) = padT + chartH * (1f - (v - minVal) / range)

                val zeroY = toY(0f)
                context.beginPath()
                context.moveTo(padL, zeroY)
                context.lineTo(padL + chartW, zeroY)
                context.strokeStyle(Color(0xFFCCCCCCL))
                context.lineWidth(0.5f)
                context.stroke()

                context.font(a.axisFontSize)
                context.fillStyle(Color(0xFF999999L))
                listOf(minVal, (minVal + maxVal) / 2, maxVal).forEach { v ->
                    val y = toY(v)
                    context.fillText(v.fmt0(), 0f, y + a.axisFontSize * 0.35f)
                }

                bars.forEachIndexed { i, bar ->
                    val x = padL + i * slotW
                    val bw = slotW * a.barWidthFraction
                    val bx = x + (slotW - bw) / 2f
                    val topY = toY(runningTop[i])
                    val botY = toY(runningBase[i])
                    val barH = abs(botY - topY).coerceAtLeast(1f)
                    val rectY = minOf(topY, botY)

                    val barColor = when (bar.type) {
                        WaterfallBarType.START -> a.startColor
                        WaterfallBarType.TOTAL -> a.totalColor
                        WaterfallBarType.INCREASE -> a.increaseColor
                        WaterfallBarType.DECREASE -> a.decreaseColor
                    }
                    context.fillStyle(barColor)
                    context.fillRect(bx, rectY, bw, barH)
                    context.strokeStyle(a.strokeColor)
                    context.lineWidth(0.5f)
                    context.beginPath()
                    context.moveTo(bx, rectY)
                    context.lineTo(bx + bw, rectY)
                    context.lineTo(bx + bw, rectY + barH)
                    context.lineTo(bx, rectY + barH)
                    context.closePath()
                    context.stroke()

                    if (a.showConnectors && i < n - 1) {
                        val nextBar = bars[i + 1]
                        val connY = when (nextBar.type) {
                            WaterfallBarType.TOTAL -> toY(0f)
                            else -> if (bar.type == WaterfallBarType.DECREASE) toY(runningBase[i]) else toY(runningTop[i])
                        }
                        val nextX = padL + (i + 1) * slotW + (slotW - slotW * a.barWidthFraction) / 2f
                        context.beginPath()
                        context.moveTo(bx + bw, connY)
                        context.lineTo(nextX, connY)
                        context.strokeStyle(a.connectorColor)
                        context.lineWidth(1f)
                        context.stroke()
                    }

                    if (a.showValues) {
                        val displayVal = bar.value.fmt1()
                        context.font(a.labelFontSize)
                        context.fillStyle(Color(0xFF444444L))
                        val labelX = bx + bw / 2f - displayVal.length * a.labelFontSize * 0.3f
                        val labelY = if (bar.type == WaterfallBarType.DECREASE) rectY + barH + a.labelFontSize + 1f else rectY - 3f
                        context.fillText(displayVal, labelX, labelY)
                    }

                    if (a.showLabels) {
                        context.font(a.axisFontSize)
                        context.fillStyle(Color(0xFF888888L))
                        val lbl = bar.label.take(4)
                        context.fillText(lbl, bx + bw / 2f - lbl.length * a.axisFontSize * 0.3f, padT + chartH + 18f)
                    }
                }
            }
        }
    }
}

fun ViewContainer<*, *>.CandlestickChart(init: CandlestickChartView.() -> Unit) {
    addChild(CandlestickChartView(), init)
}

// =============================================================================
// MixedChart - bar + line combo on shared canvas (ECharts mixed type)
// =============================================================================

enum class MixedSeriesType { BAR, LINE }

data class MixedSeries(
    val name: String,
    val points: List<ChartDataPoint>,
    val color: Color,
    val type: MixedSeriesType = MixedSeriesType.BAR,
    val lineWidth: Float = 2f,
    val dotRadius: Float = 4f,
    val fillArea: Boolean = false,
)

class MixedChartAttr : ChartAttr() {
    internal var mixedSeries by observable(emptyList<MixedSeries>())
    internal var barWidthFraction by observable(0.5f)
    internal var cornerRadius by observable(3f)

    fun mixedData(vararg s: MixedSeries) { mixedSeries = s.toList() }
    fun mixedData(list: List<MixedSeries>) { mixedSeries = list }
    fun barWidthFraction(f: Float) { barWidthFraction = f.coerceIn(0.1f, 0.9f) }
    fun cornerRadius(r: Float) { cornerRadius = r.coerceAtLeast(0f) }
}

class MixedChartView : ComposeView<MixedChartAttr, ChartEvent>() {

    override fun createAttr(): MixedChartAttr = MixedChartAttr()
    override fun createEvent(): ChartEvent = ChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { absolutePositionAllZero() } }) { context, width, height ->
                val a = ctx.attr
                if (a.mixedSeries.isEmpty()) return@Canvas

                val padL = PADDING_LEFT
                val padR = PADDING_RIGHT
                val padT = PADDING_TOP
                val padB = PADDING_BOTTOM
                val chartW = width - padL - padR
                val chartH = height - padT - padB

                val allVals = a.mixedSeries.flatMap { s -> s.points.map { it.value } }
                val (minVal, maxVal) = computeRange(allVals)
                val range = maxVal - minVal

                fun yPos(v: Float) = padT + chartH - (v - minVal) / range * chartH

                val labels = a.mixedSeries.firstOrNull()?.points?.map { it.label } ?: return@Canvas
                val n = labels.size
                val slotW = chartW / n

                // Grid and axis
                if (a.showGrid) {
                    context.lineWidth(0.5f)
                    context.strokeStyle(a.gridColor)
                    for (i in 0..a.gridLineCount) {
                        val y = padT + chartH - i.toFloat() / a.gridLineCount * chartH
                        context.beginPath(); context.moveTo(padL, y); context.lineTo(padL + chartW, y); context.stroke()
                    }
                }
                context.lineWidth(1f)
                context.strokeStyle(a.axisColor)
                context.beginPath(); context.moveTo(padL, padT); context.lineTo(padL, padT + chartH); context.lineTo(padL + chartW, padT + chartH); context.stroke()

                // Y labels
                if (a.showAxisLabels) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(0xFF888888L))
                    context.textAlign(TextAlign.CENTER)
                    for (i in 0..a.gridLineCount) {
                        val v = minVal + range * i / a.gridLineCount
                        val y = padT + chartH - i.toFloat() / a.gridLineCount * chartH
                        context.fillText(v.fmt0(), padL - 20f, y + 4f)
                    }
                }

                // Draw bar series first, then line series on top
                val barSeries = a.mixedSeries.filter { it.type == MixedSeriesType.BAR }
                val lineSeries = a.mixedSeries.filter { it.type == MixedSeriesType.LINE }
                val barCount = barSeries.size.coerceAtLeast(1)
                val barW = slotW * a.barWidthFraction / barCount

                barSeries.forEachIndexed { sIdx, series ->
                    series.points.forEachIndexed { pIdx, pt ->
                        val x = padL + pIdx * slotW + (sIdx * barW) + (slotW * (1f - a.barWidthFraction) / 2f)
                        val y = yPos(pt.value)
                        val bh = padT + chartH - y
                        context.fillStyle(series.color)
                        context.fillRoundRect(x, y, barW - 1f, bh, a.cornerRadius)
                    }
                }

                lineSeries.forEach { series ->
                    val pts = series.points
                    if (pts.isEmpty()) return@forEach
                    fun cx(i: Int) = padL + i * slotW + slotW / 2f
                    fun cy(i: Int) = yPos(pts[i].value)

                    if (series.fillArea) {
                        val grad = context.createLinearGradient(0f, padT, 0f, padT + chartH)
                        grad.addColorStop(0f, series.color)
                        grad.addColorStop(1f, Color(red255 = 255, green255 = 255, blue255 = 255, alpha01 = 0f))
                        context.fillStyle(grad)
                        context.beginPath()
                        context.moveTo(cx(0), padT + chartH)
                        context.lineTo(cx(0), cy(0))
                        for (i in 1 until pts.size) context.lineTo(cx(i), cy(i))
                        context.lineTo(cx(pts.lastIndex), padT + chartH)
                        context.closePath(); context.fill()
                    }

                    context.lineWidth(series.lineWidth)
                    context.strokeStyle(series.color)
                    context.lineCapRound()
                    context.beginPath()
                    context.moveTo(cx(0), cy(0))
                    for (i in 1 until pts.size) context.lineTo(cx(i), cy(i))
                    context.stroke()

                    pts.forEachIndexed { i, _ ->
                        context.fillStyle(series.color)
                        context.beginPath()
                        context.arc(cx(i), cy(i), series.dotRadius, 0f, (2 * PI).toFloat())
                        context.fill()
                    }
                }

                // X labels
                if (a.showAxisLabels) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(0xFF888888L))
                    context.textAlign(TextAlign.CENTER)
                    labels.forEachIndexed { i, lbl ->
                        context.fillText(lbl, padL + i * slotW + slotW / 2f, padT + chartH + 16f)
                    }
                }
            }
        }
    }
}

fun ViewContainer<*, *>.MixedChart(init: MixedChartView.() -> Unit) {
    addChild(MixedChartView(), init)
}

data class CandleStick(
    val label: String,
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float,
)

class CandlestickChartAttr : ComposeAttr() {
    internal var candles by observable(emptyList<CandleStick>())
    internal var bullColor by observable(Color(0xFF4CAF50L))
    internal var bearColor by observable(Color(0xFFF44336L))
    internal var wickColor by observable(Color(0xFF888888L))
    internal var axisFontSize by observable(10f)
    internal var candleWidthFraction by observable(0.6f)

    fun candles(list: List<CandleStick>) { candles = list }
    fun candles(vararg c: CandleStick) { candles = c.toList() }
    fun bullColor(c: Color) { bullColor = c }
    fun bearColor(c: Color) { bearColor = c }
    fun candleWidthFraction(f: Float) { candleWidthFraction = f.coerceIn(0.2f, 0.9f) }
}

class CandlestickChartEvent : ComposeEvent() {
    internal var onCandleClickHandler: ((Int, CandleStick) -> Unit)? = null
    fun onCandleClick(handler: (index: Int, candle: CandleStick) -> Unit) { onCandleClickHandler = handler }
}

class CandlestickChartView : ComposeView<CandlestickChartAttr, CandlestickChartEvent>() {
    private var lastCandles = emptyList<CandleStick>()
    private var lastSlotW = 0f
    private var lastPadL = 0f
    private var lastPadT = 0f
    private var lastChartH = 0f
    private var lastMinVal = 0f
    private var lastRange = 1f

    override fun createAttr(): CandlestickChartAttr = CandlestickChartAttr()
    override fun createEvent(): CandlestickChartEvent = CandlestickChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { allFill() }
                event {
                    click { params ->
                        val handler = ctx.event.onCandleClickHandler ?: return@click
                        val candles = ctx.lastCandles
                        if (candles.isEmpty()) return@click
                        val n = candles.size
                        val i = ((params.x - ctx.lastPadL) / ctx.lastSlotW).toInt().coerceIn(0, n - 1)
                        if (params.x >= ctx.lastPadL) handler(i, candles[i])
                    }
                }
            }) { context, width, height ->
                val a = ctx.attr
                if (a.candles.isEmpty()) return@Canvas
                val candles = a.candles
                val n = candles.size
                val padL = 40f
                val padR = 8f
                val padT = 16f
                val padB = 28f
                val chartW = width - padL - padR
                val chartH = height - padT - padB
                val slotW = chartW / n

                ctx.lastCandles = candles
                ctx.lastSlotW = slotW
                ctx.lastPadL = padL
                ctx.lastPadT = padT
                ctx.lastChartH = chartH

                val allVals = candles.flatMap { listOf(it.high, it.low) }
                val minVal = allVals.min()
                val maxVal = allVals.max()
                val range = (maxVal - minVal).coerceAtLeast(1f)
                ctx.lastMinVal = minVal
                ctx.lastRange = range

                fun toY(v: Float) = padT + chartH * (1f - (v - minVal) / range)

                context.font(a.axisFontSize)
                val steps = 4
                for (s in 0..steps) {
                    val v = minVal + range * s / steps
                    val y = toY(v)
                    context.fillStyle(Color(0xFF999999L))
                    context.fillText(v.fmt1(), 0f, y + a.axisFontSize * 0.35f)
                    context.beginPath()
                    context.moveTo(padL, y)
                    context.lineTo(padL + chartW, y)
                    context.strokeStyle(Color(0xFFEEEEEEL))
                    context.lineWidth(0.5f)
                    context.stroke()
                }

                candles.forEachIndexed { i, candle ->
                    val cx = padL + i * slotW + slotW / 2f
                    val bw = slotW * a.candleWidthFraction
                    val isBull = candle.close >= candle.open
                    val color = if (isBull) a.bullColor else a.bearColor

                    val bodyTop = toY(maxOf(candle.open, candle.close))
                    val bodyBot = toY(minOf(candle.open, candle.close))
                    val bodyH = (bodyBot - bodyTop).coerceAtLeast(1f)

                    context.beginPath()
                    context.moveTo(cx, toY(candle.high))
                    context.lineTo(cx, toY(candle.low))
                    context.strokeStyle(a.wickColor)
                    context.lineWidth(1f)
                    context.stroke()

                    context.fillStyle(color)
                    context.fillRect(cx - bw / 2f, bodyTop, bw, bodyH)

                    context.font(a.axisFontSize)
                    context.fillStyle(Color(0xFF888888L))
                    val lbl = candle.label.take(4)
                    context.fillText(lbl, cx - lbl.length * a.axisFontSize * 0.3f, padT + chartH + 18f)
                }
            }
        }
    }
}

fun ViewContainer<*, *>.HeatmapChart(init: HeatmapChartView.() -> Unit) {
    addChild(HeatmapChartView(), init)
}

data class HeatmapCell(
    val col: Int,
    val row: Int,
    val value: Float,
    val label: String = "",
)

class HeatmapChartAttr : ComposeAttr() {
    internal var cells by observable(emptyList<HeatmapCell>())
    internal var cols by observable(7)
    internal var rows by observable(4)
    internal var minColor by observable(Color(0xFFEBEDF0L))
    internal var maxColor by observable(Color(0xFF216E39L))
    internal var emptyColor by observable(Color(0xFFEBEDF0L))
    internal var cellPadding by observable(3f)
    internal var cellRadius by observable(2f)
    internal var showValues by observable(false)
    internal var axisFontSize by observable(10f)

    fun cells(vararg c: HeatmapCell) { cells = c.toList() }
    fun cells(list: List<HeatmapCell>) { cells = list }
    fun cols(n: Int) { cols = n.coerceAtLeast(1) }
    fun rows(n: Int) { rows = n.coerceAtLeast(1) }
    fun minColor(c: Color) { minColor = c }
    fun maxColor(c: Color) { maxColor = c }
    fun emptyColor(c: Color) { emptyColor = c }
    fun cellPadding(p: Float) { cellPadding = p.coerceAtLeast(0f) }
    fun cellRadius(r: Float) { cellRadius = r.coerceAtLeast(0f) }
    fun showValues(show: Boolean) { showValues = show }
    fun axisFontSize(sz: Float) { axisFontSize = sz.coerceAtLeast(6f) }
}

class HeatmapChartEvent : ComposeEvent() {
    internal var onCellClick: ((HeatmapCell) -> Unit)? = null
    fun onCellClick(action: (HeatmapCell) -> Unit) { onCellClick = action }
}

class HeatmapChartView : ComposeView<HeatmapChartAttr, HeatmapChartEvent>() {
    private var lastW = 0f
    private var lastH = 0f

    override fun createAttr(): HeatmapChartAttr = HeatmapChartAttr()
    override fun createEvent(): HeatmapChartEvent = HeatmapChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { allFill() }
                event { click { params -> ctx.handleCellClick(params) } }
            }) { context, w, h ->
                ctx.lastW = w
                ctx.lastH = h
                val a = ctx.attr
                if (a.cols <= 0 || a.rows <= 0) return@Canvas
                val cellW = (w - a.cellPadding * (a.cols + 1)) / a.cols
                val cellH = (h - a.cellPadding * (a.rows + 1)) / a.rows
                if (cellW <= 0f || cellH <= 0f) return@Canvas
                val maxVal = a.cells.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
                val cellMap = a.cells.associateBy { it.col to it.row }
                val r = a.cellRadius.coerceAtMost(minOf(cellW, cellH) / 2f)

                val drawCell = { x: Float, y: Float ->
                    if (r <= 0f) {
                        context.fillRect(x, y, cellW, cellH)
                    } else {
                        context.beginPath()
                        context.moveTo(x + r, y)
                        context.lineTo(x + cellW - r, y)
                        context.arc(x + cellW - r, y + r, r, -PI.toFloat() / 2f, 0f, false)
                        context.lineTo(x + cellW, y + cellH - r)
                        context.arc(x + cellW - r, y + cellH - r, r, 0f, PI.toFloat() / 2f, false)
                        context.lineTo(x + r, y + cellH)
                        context.arc(x + r, y + cellH - r, r, PI.toFloat() / 2f, PI.toFloat(), false)
                        context.lineTo(x, y + r)
                        context.arc(x + r, y + r, r, PI.toFloat(), PI.toFloat() * 3f / 2f, false)
                        context.closePath()
                        context.fill()
                    }
                }

                repeat(a.rows) { row ->
                    repeat(a.cols) { col ->
                        val cx = a.cellPadding + col * (cellW + a.cellPadding)
                        val cy = a.cellPadding + row * (cellH + a.cellPadding)
                        val cell = cellMap[col to row]
                        val color = if (cell == null || cell.value <= 0f) a.emptyColor
                                    else ctx.lerpColor(a.minColor, a.maxColor, cell.value / maxVal)
                        context.fillStyle(color)
                        drawCell(cx, cy)
                        if (a.showValues && cell != null && cell.value > 0f) {
                            context.fillStyle(Color.WHITE)
                            context.font(a.axisFontSize)
                            val txt = "${cell.value.toInt()}"
                            context.fillText(
                                txt,
                                cx + cellW / 2f - txt.length * a.axisFontSize * 0.3f,
                                cy + cellH / 2f + a.axisFontSize * 0.35f,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleCellClick(params: ClickParams) {
        val a = attr
        if (lastW <= 0f || lastH <= 0f) return
        val cellW = (lastW - a.cellPadding * (a.cols + 1)) / a.cols
        val cellH = (lastH - a.cellPadding * (a.rows + 1)) / a.rows
        if (cellW <= 0f || cellH <= 0f) return
        val col = ((params.x - a.cellPadding) / (cellW + a.cellPadding)).toInt().coerceIn(0, a.cols - 1)
        val row = ((params.y - a.cellPadding) / (cellH + a.cellPadding)).toInt().coerceIn(0, a.rows - 1)
        a.cells.firstOrNull { it.col == col && it.row == row }?.let { event.onCellClick?.invoke(it) }
    }

    private fun lerpColor(from: Color, to: Color, t: Float): Color {
        val tc = t.coerceIn(0f, 1f)
        val fh = from.hexColor
        val th = to.hexColor
        fun chan(shift: Int): Int {
            val fc = ((fh shr shift) and 0xFFL).toInt()
            val tc2 = ((th shr shift) and 0xFFL).toInt()
            return fc + ((tc2 - fc) * tc).toInt()
        }
        return Color(red255 = chan(16), green255 = chan(8), blue255 = chan(0), alpha01 = 1f)
    }
}

fun ViewContainer<*, *>.TreemapChart(init: TreemapChartView.() -> Unit) {
    addChild(TreemapChartView(), init)
}

data class TreemapNode(
    val label: String,
    val value: Float,
    val color: Color,
    val children: List<TreemapNode> = emptyList(),
)

class TreemapChartAttr : ComposeAttr() {
    internal var nodes by observable(emptyList<TreemapNode>())
    internal var padding by observable(2f)
    internal var labelFontSize by observable(12f)
    internal var showValues by observable(true)

    fun nodes(vararg n: TreemapNode) { nodes = n.toList() }
    fun nodes(list: List<TreemapNode>) { nodes = list }
    fun padding(p: Float) { padding = p.coerceAtLeast(0f) }
    fun labelFontSize(sz: Float) { labelFontSize = sz.coerceAtLeast(6f) }
    fun showValues(show: Boolean) { showValues = show }
}

class TreemapChartEvent : ComposeEvent() {
    internal var onNodeClick: ((TreemapNode) -> Unit)? = null
    fun onNodeClick(action: (TreemapNode) -> Unit) { onNodeClick = action }
}

private data class TreemapRect(
    val node: TreemapNode,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
)

class TreemapChartView : ComposeView<TreemapChartAttr, TreemapChartEvent>() {
    private var lastLayout = emptyList<TreemapRect>()

    override fun createAttr(): TreemapChartAttr = TreemapChartAttr()
    override fun createEvent(): TreemapChartEvent = TreemapChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { allFill() }
                event { click { params -> ctx.handleNodeClick(params) } }
            }) { context, w, h ->
                val a = ctx.attr
                if (a.nodes.isEmpty()) return@Canvas
                val sorted = a.nodes.sortedByDescending { it.value }
                val layout = ctx.squarify(sorted, 0f, 0f, w, h)
                ctx.lastLayout = layout
                layout.forEach { rect ->
                    val p = a.padding / 2f
                    val dx = rect.x + p
                    val dy = rect.y + p
                    val dw = (rect.w - a.padding).coerceAtLeast(0f)
                    val dh = (rect.h - a.padding).coerceAtLeast(0f)
                    if (dw <= 0f || dh <= 0f) return@forEach
                    context.fillStyle(rect.node.color)
                    context.fillRect(dx, dy, dw, dh)
                    if (dw >= 32f && dh >= 20f) {
                        val labelY = if (a.showValues && dh >= 36f) dy + dh / 2f - 4f
                                     else dy + dh / 2f + a.labelFontSize * 0.35f
                        context.fillStyle(Color.WHITE)
                        context.font(a.labelFontSize)
                        val lbl = rect.node.label
                        context.fillText(
                            lbl,
                            dx + dw / 2f - lbl.length * a.labelFontSize * 0.3f,
                            labelY,
                        )
                        if (a.showValues && dh >= 36f) {
                            val valStr = ctx.fmtValue(rect.node.value)
                            val valFontSize = a.labelFontSize * 0.85f
                            context.font(valFontSize)
                            context.fillStyle(Color(red255 = 255, green255 = 255, blue255 = 255, alpha01 = 0.75f))
                            context.fillText(
                                valStr,
                                dx + dw / 2f - valStr.length * valFontSize * 0.3f,
                                labelY + a.labelFontSize + 3f,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleNodeClick(params: ClickParams) {
        lastLayout.firstOrNull {
            params.x >= it.x && params.x <= it.x + it.w && params.y >= it.y && params.y <= it.y + it.h
        }?.let { event.onNodeClick?.invoke(it.node) }
    }

    private fun squarify(nodes: List<TreemapNode>, x: Float, y: Float, w: Float, h: Float): List<TreemapRect> {
        if (nodes.isEmpty() || w <= 0f || h <= 0f) return emptyList()
        if (nodes.size == 1) return listOf(TreemapRect(nodes[0], x, y, w, h))
        val total = nodes.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) return emptyList()
        val mid = nodes.size / 2
        val leftFrac = (nodes.take(mid).sumOf { it.value.toDouble() }.toFloat() / total).coerceIn(0.01f, 0.99f)
        return if (w >= h) {
            squarify(nodes.take(mid), x, y, w * leftFrac, h) +
            squarify(nodes.drop(mid), x + w * leftFrac, y, w * (1f - leftFrac), h)
        } else {
            squarify(nodes.take(mid), x, y, w, h * leftFrac) +
            squarify(nodes.drop(mid), x, y + h * leftFrac, w, h * (1f - leftFrac))
        }
    }

    private fun fmtValue(v: Float): String = when {
        v >= 1_000_000f -> "${(v / 1_000_000f).toInt()}M"
        v >= 1_000f -> "${(v / 1_000f).toInt()}K"
        else -> "${v.toInt()}"
    }
}

fun ViewContainer<*, *>.BoxplotChart(init: BoxplotChartView.() -> Unit) {
    addChild(BoxplotChartView(), init)
}

data class BoxplotData(
    val label: String,
    val min: Float,
    val q1: Float,
    val median: Float,
    val q3: Float,
    val max: Float,
    val outliers: List<Float> = emptyList(),
    val color: Color? = null,
)

class BoxplotChartAttr : ComposeAttr() {
    internal var boxes by observable(emptyList<BoxplotData>())
    internal var defaultColor by observable(Color(0xFF1677FFL))
    internal var medianColor by observable(Color(0xFFFF4D4FL))
    internal var boxWidthFraction by observable(0.5f)
    internal var showGrid by observable(true)
    internal var showOutliers by observable(true)
    internal var axisFontSize by observable(11f)
    internal var axisColor by observable(Color(0xFFB4B4B4L))
    internal var gridColor by observable(Color(0xFFDCDCDCL))

    fun boxes(vararg b: BoxplotData) { boxes = b.toList() }
    fun boxes(list: List<BoxplotData>) { boxes = list }
    fun defaultColor(c: Color) { defaultColor = c }
    fun medianColor(c: Color) { medianColor = c }
    fun boxWidthFraction(f: Float) { boxWidthFraction = f.coerceIn(0.2f, 0.9f) }
    fun showGrid(show: Boolean) { showGrid = show }
    fun showOutliers(show: Boolean) { showOutliers = show }
    fun axisFontSize(sz: Float) { axisFontSize = sz.coerceAtLeast(8f) }
    fun size(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }
}

class BoxplotChartEvent : ComposeEvent() {
    internal var onBoxClick: ((BoxplotData) -> Unit)? = null
    fun onBoxClick(action: (BoxplotData) -> Unit) { onBoxClick = action }
}

class BoxplotChartView : ComposeView<BoxplotChartAttr, BoxplotChartEvent>() {
    private var lastW = 0f
    private var lastH = 0f

    override fun createAttr(): BoxplotChartAttr = BoxplotChartAttr()
    override fun createEvent(): BoxplotChartEvent = BoxplotChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { allFill() }
                event { click { params -> ctx.handleClick(params) } }
            }) { context, w, h ->
                ctx.lastW = w
                ctx.lastH = h
                val a = ctx.attr
                val boxes = a.boxes
                if (boxes.isEmpty()) return@Canvas

                val padL = PADDING_LEFT
                val padR = PADDING_RIGHT
                val padT = PADDING_TOP
                val padB = PADDING_BOTTOM
                val chartW = w - padL - padR
                val chartH = h - padT - padB

                val allVals = boxes.flatMap { b ->
                    listOf(b.min, b.q1, b.median, b.q3, b.max) + b.outliers
                }
                val dataMin = allVals.minOrNull() ?: 0f
                val dataMax = allVals.maxOrNull() ?: 1f
                val range = (dataMax - dataMin).takeIf { it > 1e-6f } ?: 1f
                val margin = range * 0.1f
                val yMin = dataMin - margin
                val yMax = dataMax + margin
                val yRange = yMax - yMin

                fun toY(v: Float): Float = padT + chartH * (1f - (v - yMin) / yRange)

                if (a.showGrid) {
                    context.strokeStyle(a.gridColor)
                    context.lineWidth(0.5f)
                    val steps = 4
                    repeat(steps + 1) { i ->
                        val y = padT + chartH * i / steps
                        context.beginPath()
                        context.moveTo(padL, y)
                        context.lineTo(padL + chartW, y)
                        context.stroke()
                    }
                }

                context.strokeStyle(a.axisColor)
                context.lineWidth(1f)
                context.beginPath()
                context.moveTo(padL, padT)
                context.lineTo(padL, padT + chartH)
                context.lineTo(padL + chartW, padT + chartH)
                context.stroke()

                context.font(a.axisFontSize)
                context.fillStyle(Color(0xFF505050L))
                val steps = 4
                repeat(steps + 1) { i ->
                    val v = yMin + yRange * (steps - i) / steps
                    val y = padT + chartH * i / steps
                    val label = v.fmt1()
                    context.fillText(label, 2f, y + a.axisFontSize * 0.35f)
                }

                val slotW = chartW / boxes.size
                boxes.forEachIndexed { idx, box ->
                    val cx = padL + idx * slotW + slotW / 2f
                    val bw = slotW * a.boxWidthFraction
                    val color = box.color ?: a.defaultColor

                    val yQ1 = toY(box.q1)
                    val yQ3 = toY(box.q3)
                    val yMed = toY(box.median)
                    val yMinV = toY(box.min)
                    val yMaxV = toY(box.max)

                    context.strokeStyle(color)
                    context.lineWidth(1.5f)
                    context.beginPath()
                    context.moveTo(cx, yMinV)
                    context.lineTo(cx, yQ1)
                    context.stroke()
                    context.beginPath()
                    context.moveTo(cx, yQ3)
                    context.lineTo(cx, yMaxV)
                    context.stroke()

                    context.beginPath()
                    context.moveTo(cx - bw * 0.3f, yMinV)
                    context.lineTo(cx + bw * 0.3f, yMinV)
                    context.stroke()
                    context.beginPath()
                    context.moveTo(cx - bw * 0.3f, yMaxV)
                    context.lineTo(cx + bw * 0.3f, yMaxV)
                    context.stroke()

                    val grad = context.createLinearGradient(cx - bw / 2f, yQ3, cx + bw / 2f, yQ1)
                    grad.addColorStop(0f, Color(
                        red255 = ((color.hexColor shr 16) and 0xFFL).toInt(),
                        green255 = ((color.hexColor shr 8) and 0xFFL).toInt(),
                        blue255 = (color.hexColor and 0xFFL).toInt(),
                        alpha01 = 0.25f,
                    ))
                    grad.addColorStop(1f, Color(
                        red255 = ((color.hexColor shr 16) and 0xFFL).toInt(),
                        green255 = ((color.hexColor shr 8) and 0xFFL).toInt(),
                        blue255 = (color.hexColor and 0xFFL).toInt(),
                        alpha01 = 0.08f,
                    ))
                    context.fillStyle(grad)
                    context.fillRect(cx - bw / 2f, yQ3, bw, (yQ1 - yQ3).coerceAtLeast(1f))

                    context.strokeStyle(color)
                    context.lineWidth(1.5f)
                    context.beginPath()
                    context.moveTo(cx - bw / 2f, yQ3)
                    context.lineTo(cx + bw / 2f, yQ3)
                    context.lineTo(cx + bw / 2f, yQ1)
                    context.lineTo(cx - bw / 2f, yQ1)
                    context.closePath()
                    context.stroke()

                    context.strokeStyle(a.medianColor)
                    context.lineWidth(2f)
                    context.beginPath()
                    context.moveTo(cx - bw / 2f, yMed)
                    context.lineTo(cx + bw / 2f, yMed)
                    context.stroke()

                    if (a.showOutliers) {
                        context.fillStyle(color)
                        box.outliers.forEach { ov ->
                            val oy = toY(ov)
                            context.beginPath()
                            context.arc(cx, oy, 3f, 0f, 2f * PI.toFloat(), false)
                            context.fill()
                        }
                    }

                    context.font(a.axisFontSize)
                    context.fillStyle(Color(0xFF505050L))
                    val lbl = box.label.take(5)
                    context.fillText(lbl, cx - lbl.length * a.axisFontSize * 0.3f, padT + chartH + 18f)
                }
            }
        }
    }

    private fun handleClick(params: ClickParams) {
        val a = attr
        if (lastW <= 0f || lastH <= 0f || a.boxes.isEmpty()) return
        val chartW = lastW - PADDING_LEFT - PADDING_RIGHT
        val slotW = chartW / a.boxes.size
        val idx = ((params.x - PADDING_LEFT) / slotW).toInt().coerceIn(0, a.boxes.size - 1)
        event.onBoxClick?.invoke(a.boxes[idx])
    }
}

// ─── BubbleChart ─────────────────────────────────────────────────────────────

fun ViewContainer<*, *>.BubbleChart(init: BubbleChartView.() -> Unit) {
    addChild(BubbleChartView(), init)
}

data class BubbleData(
    val x: Float,
    val y: Float,
    val r: Float,
    val label: String = "",
    val color: Color? = null,
)

class BubbleChartAttr : ComposeAttr() {
    internal var series by observable(emptyList<BubbleData>())
    internal var xAxisLabel by observable("")
    internal var yAxisLabel by observable("")
    internal var showLabels by observable(false)
    internal var defaultColor by observable(Color(0xFF1677FFL))
    internal var gridColor by observable(Color(0xFFDCDCDCL))
    internal var axisColor by observable(Color(0xFFB4B4B4L))
    internal var axisFontSize by observable(11f)
    internal var maxRadius by observable(36f)
    internal var fillAlpha by observable(0.55f)

    fun points(list: List<BubbleData>) { series = list }
    fun points(vararg p: BubbleData) { series = p.toList() }
    fun xAxisLabel(s: String) { xAxisLabel = s }
    fun yAxisLabel(s: String) { yAxisLabel = s }
    fun showLabels(b: Boolean) { showLabels = b }
    fun defaultColor(c: Color) { defaultColor = c }
    fun maxRadius(r: Float) { maxRadius = r.coerceIn(8f, 80f) }
    fun fillAlpha(a: Float) { fillAlpha = a.coerceIn(0.1f, 1f) }
    fun size(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }
}

class BubbleChartEvent : ComposeEvent() {
    internal var onBubbleClick: ((BubbleData) -> Unit)? = null
    fun onBubbleClick(action: (BubbleData) -> Unit) { onBubbleClick = action }
}

class BubbleChartView : ComposeView<BubbleChartAttr, BubbleChartEvent>() {
    private var lastW = 0f
    private var lastH = 0f
    private var renderedPoints = emptyList<BubbleData>()
    private var renderedX = emptyList<Float>()
    private var renderedY = emptyList<Float>()
    private var renderedR = emptyList<Float>()

    override fun createAttr(): BubbleChartAttr = BubbleChartAttr()
    override fun createEvent(): BubbleChartEvent = BubbleChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { allFill() }
                event { click { params -> ctx.handleClick(params) } }
            }) { context, w, h ->
                ctx.lastW = w; ctx.lastH = h
                val a = ctx.attr
                val pts = a.series
                if (pts.isEmpty()) return@Canvas

                val padL = PADDING_LEFT + 4f
                val padR = PADDING_RIGHT
                val padT = PADDING_TOP
                val padB = PADDING_BOTTOM + 4f
                val chartW = w - padL - padR
                val chartH = h - padT - padB

                val xVals = pts.map { it.x }
                val yVals = pts.map { it.y }
                val rVals = pts.map { it.r }
                val xMin = xVals.minOrNull() ?: 0f
                val xMax = xVals.maxOrNull() ?: 1f
                val yMin = yVals.minOrNull() ?: 0f
                val yMax = yVals.maxOrNull() ?: 1f
                val rMax = rVals.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                val xRange = (xMax - xMin).takeIf { it > 1e-6f } ?: 1f
                val yRange = (yMax - yMin).takeIf { it > 1e-6f } ?: 1f
                val xPad = xRange * 0.12f; val yPad = yRange * 0.12f

                fun toSx(x: Float) = padL + (x - (xMin - xPad)) / (xRange + xPad * 2f) * chartW
                fun toSy(y: Float) = padT + chartH - (y - (yMin - yPad)) / (yRange + yPad * 2f) * chartH

                context.strokeStyle(a.gridColor); context.lineWidth(0.5f)
                repeat(5) { i ->
                    val gy = padT + chartH * i / 4f
                    val gx = padL + chartW * i / 4f
                    context.beginPath(); context.moveTo(padL, gy); context.lineTo(padL + chartW, gy); context.stroke()
                    context.beginPath(); context.moveTo(gx, padT); context.lineTo(gx, padT + chartH); context.stroke()
                }

                context.strokeStyle(a.axisColor); context.lineWidth(1f)
                context.beginPath()
                context.moveTo(padL, padT); context.lineTo(padL, padT + chartH); context.lineTo(padL + chartW, padT + chartH)
                context.stroke()

                val cxList = mutableListOf<Float>()
                val cyList = mutableListOf<Float>()
                val crList = mutableListOf<Float>()
                pts.forEach { p ->
                    val cx = toSx(p.x); val cy = toSy(p.y)
                    val scaledR = (p.r / rMax) * a.maxRadius
                    cxList += cx; cyList += cy; crList += scaledR

                    val baseColor = p.color ?: a.defaultColor
                    val rgb = baseColor.hexColor
                    val r = ((rgb shr 16) and 0xFF).toInt()
                    val g = ((rgb shr 8) and 0xFF).toInt()
                    val b = (rgb and 0xFF).toInt()
                    val grad = context.createLinearGradient(cx - scaledR, cy - scaledR, cx + scaledR, cy + scaledR)
                    grad.addColorStop(0f, Color(red255 = r, green255 = g, blue255 = b, alpha01 = a.fillAlpha))
                    grad.addColorStop(1f, Color(red255 = r, green255 = g, blue255 = b, alpha01 = a.fillAlpha * 0.4f))
                    context.fillStyle(grad)
                    context.beginPath()
                    context.arc(cx, cy, scaledR, 0f, 2f * PI.toFloat(), false)
                    context.fill()

                    context.strokeStyle(baseColor)
                    context.lineWidth(1.5f)
                    context.beginPath()
                    context.arc(cx, cy, scaledR, 0f, 2f * PI.toFloat(), false)
                    context.stroke()

                    if (a.showLabels && p.label.isNotEmpty()) {
                        context.font(a.axisFontSize)
                        context.fillStyle(Color(0xFF505050L))
                        context.textAlign(TextAlign.CENTER)
                        context.fillText(p.label, cx, cy + a.axisFontSize * 0.35f)
                        context.textAlign(TextAlign.LEFT)
                    }
                }
                ctx.renderedPoints = pts
                ctx.renderedX = cxList
                ctx.renderedY = cyList
                ctx.renderedR = crList

                context.font(a.axisFontSize); context.fillStyle(Color(0xFF505050L))
                repeat(5) { i ->
                    val v = (yMin - yPad) + (yRange + yPad * 2f) * (4 - i) / 4f
                    val gy = padT + chartH * i / 4f
                    context.fillText(v.fmt1(), 2f, gy + a.axisFontSize * 0.35f)
                }
                repeat(5) { i ->
                    val v = (xMin - xPad) + (xRange + xPad * 2f) * i / 4f
                    val gx = padL + chartW * i / 4f
                    context.fillText(v.fmt1(), gx - 8f, padT + chartH + 14f)
                }
            }
        }
    }

    private fun handleClick(params: ClickParams) {
        val pts = renderedPoints; if (pts.isEmpty()) return
        for (i in pts.indices) {
            val dx = params.x - renderedX[i]; val dy = params.y - renderedY[i]
            if (sqrt(dx * dx + dy * dy) <= renderedR[i] + 4f) {
                event.onBubbleClick?.invoke(pts[i]); return
            }
        }
    }
}

// ─── SankeyChart ─────────────────────────────────────────────────────────────

fun ViewContainer<*, *>.SankeyChart(init: SankeyChartView.() -> Unit) {
    addChild(SankeyChartView(), init)
}

data class SankeyNode(val id: String, val label: String = "", val color: Color? = null)
data class SankeyLink(val from: String, val to: String, val value: Float)

class SankeyChartAttr : ComposeAttr() {
    internal var nodes by observable(emptyList<SankeyNode>())
    internal var links by observable(emptyList<SankeyLink>())
    internal var nodeWidth by observable(20f)
    internal var nodePadding by observable(16f)
    internal var labelFontSize by observable(12f)
    internal var defaultColors by observable(ChartTheme.Default)
    internal var linkAlpha by observable(0.35f)
    internal var showLabels by observable(true)

    fun nodes(list: List<SankeyNode>) { nodes = list }
    fun nodes(vararg n: SankeyNode) { nodes = n.toList() }
    fun links(list: List<SankeyLink>) { links = list }
    fun links(vararg l: SankeyLink) { links = l.toList() }
    fun nodeWidth(w: Float) { nodeWidth = w.coerceIn(8f, 40f) }
    fun nodePadding(p: Float) { nodePadding = p.coerceAtLeast(4f) }
    fun labelFontSize(sz: Float) { labelFontSize = sz }
    fun palette(colors: List<Color>) { defaultColors = colors }
    fun linkAlpha(a: Float) { linkAlpha = a.coerceIn(0.1f, 0.9f) }
    fun showLabels(b: Boolean) { showLabels = b }
    fun size(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }
}

class SankeyChartEvent : ComposeEvent() {
    internal var onNodeClick: ((SankeyNode) -> Unit)? = null
    fun onNodeClick(action: (SankeyNode) -> Unit) { onNodeClick = action }
}

class SankeyChartView : ComposeView<SankeyChartAttr, SankeyChartEvent>() {

    override fun createAttr(): SankeyChartAttr = SankeyChartAttr()
    override fun createEvent(): SankeyChartEvent = SankeyChartEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { allFill() } }) { context, w, h ->
                val a = ctx.attr
                val nodes = a.nodes; val links = a.links
                if (nodes.isEmpty()) return@Canvas

                val padL = PADDING_LEFT; val padR = PADDING_RIGHT
                val padT = PADDING_TOP; val padB = PADDING_BOTTOM
                val chartW = w - padL - padR; val chartH = h - padT - padB

                // Assign each node to a column based on BFS-like depth
                val colMap = mutableMapOf<String, Int>()
                val queue = ArrayDeque<String>()
                val sources = nodes.map { it.id }.filter { id -> links.none { it.to == id } }
                sources.forEach { colMap[it] = 0; queue.add(it) }
                while (queue.isNotEmpty()) {
                    val cur = queue.removeFirst()
                    val col = colMap[cur] ?: 0
                    links.filter { it.from == cur }.forEach { lnk ->
                        if (!colMap.containsKey(lnk.to)) {
                            colMap[lnk.to] = col + 1
                            queue.add(lnk.to)
                        } else {
                            colMap[lnk.to] = maxOf(colMap[lnk.to]!!, col + 1)
                        }
                    }
                }
                nodes.forEach { n -> colMap.putIfAbsent(n.id, 0) }
                val maxCol = colMap.values.maxOrNull() ?: 0
                val colCount = maxCol + 1

                // Total value per node
                val totalIn = mutableMapOf<String, Float>()
                val totalOut = mutableMapOf<String, Float>()
                nodes.forEach { n -> totalIn[n.id] = 0f; totalOut[n.id] = 0f }
                links.forEach { l ->
                    totalIn[l.to] = (totalIn[l.to] ?: 0f) + l.value
                    totalOut[l.from] = (totalOut[l.from] ?: 0f) + l.value
                }
                val nodeValue = nodes.associate { n ->
                    n.id to maxOf(totalIn[n.id] ?: 0f, totalOut[n.id] ?: 0f).coerceAtLeast(1f)
                }

                // Compute column totals for scaling
                val colNodes = (0..maxCol).map { col -> nodes.filter { colMap[it.id] == col } }
                val colTotals = colNodes.map { colNs -> colNs.sumOf { (nodeValue[it.id] ?: 1f).toDouble() }.toFloat() }
                val maxColTotal = colTotals.maxOrNull()?.coerceAtLeast(1f) ?: 1f

                // Assign pixel heights and Y positions per column
                val nodeRect = mutableMapOf<String, FloatArray>() // [x, y, w, h]
                val nw = a.nodeWidth
                val colXStep = if (colCount <= 1) chartW else chartW / (colCount - 1).toFloat()

                colNodes.forEachIndexed { colIdx, colNs ->
                    val colTotal = colTotals[colIdx].coerceAtLeast(1f)
                    val totalPixels = chartH - a.nodePadding * (colNs.size - 1)
                    var curY = padT
                    colNs.forEach { n ->
                        val ph = ((nodeValue[n.id] ?: 1f) / colTotal * totalPixels).coerceAtLeast(4f)
                        val nx = padL + colIdx * colXStep - nw / 2f
                        nodeRect[n.id] = floatArrayOf(nx, curY, nw, ph)
                        curY += ph + a.nodePadding
                    }
                }

                // Draw links as bezier curves
                val linkOutY = mutableMapOf<String, Float>()
                val linkInY = mutableMapOf<String, Float>()
                nodeRect.forEach { (id, r) -> linkOutY[id] = r[1]; linkInY[id] = r[1] }

                links.forEach { lnk ->
                    val fromR = nodeRect[lnk.from] ?: return@forEach
                    val toR = nodeRect[lnk.to] ?: return@forEach
                    val fromNode = nodes.firstOrNull { it.id == lnk.from } ?: return@forEach
                    val colorIdx = nodes.indexOf(fromNode).coerceIn(0, a.defaultColors.size - 1)
                    val baseColor = fromNode.color ?: a.defaultColors[colorIdx % a.defaultColors.size]

                    val fromTotal = totalOut[lnk.from]?.coerceAtLeast(1f) ?: 1f
                    val pxH = (lnk.value / fromTotal * fromR[3]).coerceAtLeast(1f)
                    val x0 = fromR[0] + nw; val y0 = linkOutY[lnk.from] ?: fromR[1]
                    val x1 = toR[0]; val y1 = linkInY[lnk.to] ?: toR[1]
                    linkOutY[lnk.from] = y0 + pxH
                    linkInY[lnk.to] = y1 + pxH

                    val rgb = baseColor.hexColor
                    val r = ((rgb shr 16) and 0xFF).toInt()
                    val g = ((rgb shr 8) and 0xFF).toInt()
                    val b = (rgb and 0xFF).toInt()
                    val grad = context.createLinearGradient(x0, 0f, x1, 0f)
                    grad.addColorStop(0f, Color(red255 = r, green255 = g, blue255 = b, alpha01 = a.linkAlpha))
                    grad.addColorStop(1f, Color(red255 = r, green255 = g, blue255 = b, alpha01 = a.linkAlpha * 0.5f))
                    context.fillStyle(grad)
                    context.beginPath()
                    context.moveTo(x0, y0)
                    val cpX = (x0 + x1) / 2f
                    context.bezierCurveTo(cpX, y0, cpX, y1, x1, y1)
                    context.lineTo(x1, y1 + pxH)
                    context.bezierCurveTo(cpX, y1 + pxH, cpX, y0 + pxH, x0, y0 + pxH)
                    context.closePath()
                    context.fill()
                }

                // Draw nodes
                nodes.forEachIndexed { idx, n ->
                    val r = nodeRect[n.id] ?: return@forEachIndexed
                    val baseColor = n.color ?: a.defaultColors[idx % a.defaultColors.size]
                    val rgb = baseColor.hexColor
                    val ri = ((rgb shr 16) and 0xFF).toInt()
                    val gi = ((rgb shr 8) and 0xFF).toInt()
                    val bi = (rgb and 0xFF).toInt()
                    val grad = context.createLinearGradient(r[0], r[1], r[0], r[1] + r[3])
                    grad.addColorStop(0f, Color(red255 = ri, green255 = gi, blue255 = bi, alpha01 = 1f))
                    grad.addColorStop(1f, Color(red255 = ri, green255 = gi, blue255 = bi, alpha01 = 0.75f))
                    context.fillStyle(grad)
                    context.fillRect(r[0], r[1], r[2], r[3])

                    if (a.showLabels) {
                        val lbl = if (n.label.isNotEmpty()) n.label else n.id
                        val col = colMap[n.id] ?: 0
                        context.font(a.labelFontSize)
                        context.fillStyle(Color(0xFF282828L))
                        val lx = if (col == maxCol) r[0] - 4f - lbl.length * a.labelFontSize * 0.6f
                                  else r[0] + nw + 4f
                        context.fillText(lbl, lx, r[1] + r[3] / 2f + a.labelFontSize * 0.35f)
                    }
                }
            }
        }
    }
}

// ─── NightingaleRoseChart ─────────────────────────────────────────────────────

fun ViewContainer<*, *>.NightingaleRoseChart(init: NightingaleRoseView.() -> Unit) {
    addChild(NightingaleRoseView(), init)
}

data class RoseSlice(
    val label: String,
    val value: Float,
    val color: Color? = null,
)

class NightingaleRoseAttr : ComposeAttr() {
    internal var slices by observable(emptyList<RoseSlice>())
    internal var innerRadius by observable(0f)
    internal var outerRadius by observable(0.85f)
    internal var minRadius by observable(0.25f)
    internal var showLabels by observable(true)
    internal var labelFontSize by observable(11f)
    internal var showCenter by observable(false)
    internal var centerText by observable("")
    internal var defaultColors by observable(ChartTheme.Default)
    internal var showPercentage by observable(false)

    fun slices(vararg s: RoseSlice) { slices = s.toList() }
    fun slices(list: List<RoseSlice>) { slices = list }
    fun innerRadius(r: Float) { innerRadius = r.coerceIn(0f, 0.6f) }
    fun outerRadius(r: Float) { outerRadius = r.coerceIn(0.4f, 0.98f) }
    fun minRadius(r: Float) { minRadius = r.coerceIn(0.05f, 0.5f) }
    fun showLabels(show: Boolean) { showLabels = show }
    fun labelFontSize(s: Float) { labelFontSize = s }
    fun showCenter(show: Boolean) { showCenter = show }
    fun centerText(t: String) { centerText = t }
    fun theme(t: List<Color>) { defaultColors = t }
    fun showPercentage(show: Boolean) { showPercentage = show }
}

class NightingaleRoseEvent : ComposeEvent() {
    var onSliceClick: ((RoseSlice, Int) -> Unit)? = null
    fun onSliceClick(b: (RoseSlice, Int) -> Unit) { onSliceClick = b }
}

class NightingaleRoseView : ComposeView<NightingaleRoseAttr, NightingaleRoseEvent>() {
    override fun createAttr(): NightingaleRoseAttr = NightingaleRoseAttr()
    override fun createEvent(): NightingaleRoseEvent = NightingaleRoseEvent()

    // Cached geometry for click hit-testing
    private var lastCx = 0f
    private var lastCy = 0f
    private var lastInnerHole = 0f
    private var lastMinR = 0f
    private var lastMaxR = 0f
    private var lastMaxVal = 0.001f
    private var lastAngleStep = 0f

    private fun handleClick(params: ClickParams) {
        val handler = event.onSliceClick ?: return
        val slices = attr.slices
        if (slices.isEmpty()) return
        val tx = params.x - lastCx
        val ty = params.y - lastCy
        val dist = sqrt(tx * tx + ty * ty)
        if (dist < lastInnerHole || dist > lastMaxR) return
        var angle = atan2(ty, tx)
        val startBase = -PI.toFloat() / 2f
        while (angle < startBase) angle += 2f * PI.toFloat()
        slices.forEachIndexed { i, slice ->
            val startAngle = startBase + i * lastAngleStep
            val endAngle = startAngle + lastAngleStep * 0.9f
            val r = lastMinR + (lastMaxR - lastMinR) * (slice.value / lastMaxVal)
            val normalizedAngle = if (angle < startAngle) angle + 2f * PI.toFloat() else angle
            if (normalizedAngle in startAngle..endAngle && dist <= r) {
                handler(slice, i)
                return
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr { allFill() }
                event { click { params -> ctx.handleClick(params) } }
            }) { context, width, height ->
                val a = ctx.attr
                if (a.slices.isEmpty()) return@Canvas
                val cx = width / 2f
                val cy = height / 2f
                val maxR = minOf(cx, cy) * a.outerRadius
                val innerHole = a.innerRadius * maxR
                val minR = (maxR * a.minRadius).coerceAtLeast(innerHole + 2f)
                val maxVal = a.slices.maxOf { it.value }.coerceAtLeast(0.001f)
                val total = a.slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
                val n = a.slices.size
                val angleStep = (2f * PI / n).toFloat()

                ctx.lastCx = cx
                ctx.lastCy = cy
                ctx.lastInnerHole = innerHole
                ctx.lastMinR = minR
                ctx.lastMaxR = maxR
                ctx.lastMaxVal = maxVal
                ctx.lastAngleStep = angleStep

                a.slices.forEachIndexed { i, slice ->
                    val startAngle = -PI.toFloat() / 2f + i * angleStep
                    val endAngle = startAngle + angleStep * 0.9f
                    val r = minR + (maxR - minR) * (slice.value / maxVal)
                    val color = slice.color ?: a.defaultColors[i % a.defaultColors.size]

                    val midAngle = (startAngle + endAngle) / 2f
                    val gx0 = cx + minR * cos(midAngle)
                    val gy0 = cy + minR * sin(midAngle)
                    val gx1 = cx + r * cos(midAngle)
                    val gy1 = cy + r * sin(midAngle)
                    val rgb = color.hexColor
                    val ri = ((rgb shr 16) and 0xFF).toInt()
                    val gi = ((rgb shr 8) and 0xFF).toInt()
                    val bi = (rgb and 0xFF).toInt()
                    val grad = context.createLinearGradient(gx0, gy0, gx1, gy1)
                    grad.addColorStop(0f, Color(red255 = ri, green255 = gi, blue255 = bi, alpha01 = 0.7f))
                    grad.addColorStop(1f, Color(red255 = ri, green255 = gi, blue255 = bi, alpha01 = 1f))
                    context.fillStyle(grad)

                    context.beginPath()
                    if (innerHole > 0f) {
                        context.moveTo(cx + innerHole * cos(startAngle), cy + innerHole * sin(startAngle))
                        context.lineTo(cx + r * cos(startAngle), cy + r * sin(startAngle))
                        context.arc(cx, cy, r, startAngle, endAngle, false)
                        context.arc(cx, cy, innerHole, endAngle, startAngle, true)
                    } else {
                        context.moveTo(cx, cy)
                        context.arc(cx, cy, r, startAngle, endAngle, false)
                    }
                    context.closePath()
                    context.fill()

                    if (a.showLabels) {
                        val labelAngle = midAngle
                        val labelR = r + 14f
                        val lx = cx + labelR * cos(labelAngle)
                        val ly = cy + labelR * sin(labelAngle) + a.labelFontSize * 0.35f
                        context.font(a.labelFontSize)
                        context.fillStyle(Color(0xFF333333L))
                        context.textAlign(TextAlign.CENTER)
                        val pct = (slice.value / total * 100).toInt()
                        val lbl = if (a.showPercentage) "${slice.label} $pct%" else slice.label
                        context.fillText(lbl, lx, ly)
                    }
                }

                if (a.showCenter && a.centerText.isNotEmpty()) {
                    context.font(14f)
                    context.fillStyle(Color(0xFF333333L))
                    context.textAlign(TextAlign.CENTER)
                    context.fillText(a.centerText, cx, cy + 5f)
                }
            }
        }
    }
}

// =============================================================================
// CalendarHeatmap - GitHub-style contribution calendar
// =============================================================================

/**
 * A single day entry for [CalendarHeatmapView].
 *
 * @param year   4-digit year
 * @param month  1-based month (1–12)
 * @param day    1-based day of month
 * @param value  Activity count; 0 = no activity
 */
data class CalendarDay(val year: Int, val month: Int, val day: Int, val value: Int)

/**
 * Built-in color scales for the contribution heatmap.
 * Each list has 5 entries: [empty, level1, level2, level3, level4]
 */
enum class CalendarColorScale {
    /** GitHub classic green scale. */
    GREEN,
    /** Blue intensity scale (Ant Design Charts). */
    BLUE,
    /** Orange warmth scale. */
    ORANGE,
    /** Purple energy scale. */
    PURPLE,
}

class CalendarHeatmapAttr : ComposeAttr() {

    internal var days by observable(emptyList<CalendarDay>())
    internal var weeks by observable(53)
    internal var colorScale by observable(CalendarColorScale.GREEN)
    internal var customColors by observable(emptyList<Color>())
    internal var cellSize by observable(12f)
    internal var cellSpacing by observable(2f)
    internal var showMonthLabels by observable(true)
    internal var showDayLabels by observable(true)
    internal var labelColor by observable(Color(0xFF8C8C8CL))
    internal var labelFontSize by observable(10f)
    internal var emptyColor by observable(Color(0xFFEBEDF0L))
    internal var borderRadius by observable(2f)

    fun days(list: List<CalendarDay>) { days = list }
    fun days(vararg d: CalendarDay) { days = d.toList() }
    fun weeks(w: Int) { weeks = w.coerceIn(1, 53) }
    fun colorScale(s: CalendarColorScale) { colorScale = s }
    fun customColors(vararg c: Color) { customColors = c.toList() }
    fun cellSize(s: Float) { cellSize = s.coerceIn(6f, 32f) }
    fun cellSpacing(s: Float) { cellSpacing = s.coerceIn(0f, 8f) }
    fun showMonthLabels(show: Boolean) { showMonthLabels = show }
    fun showDayLabels(show: Boolean) { showDayLabels = show }
    fun labelColor(c: Color) { labelColor = c }
    fun labelFontSize(s: Float) { labelFontSize = s }
    fun emptyColor(c: Color) { emptyColor = c }
    fun borderRadius(r: Float) { borderRadius = r }

    internal fun resolveColors(): List<Color> {
        if (customColors.size >= 5) return customColors.take(5)
        return when (colorScale) {
            CalendarColorScale.GREEN  -> listOf(
                Color(0xFFEBEDF0L), Color(0xFF9BE9A8L), Color(0xFF40C463L),
                Color(0xFF30A14EL), Color(0xFF216E39L),
            )
            CalendarColorScale.BLUE   -> listOf(
                Color(0xFFEBEDF0L), Color(0xFFBAE7FFL), Color(0xFF69C0FFL),
                Color(0xFF1677FFL), Color(0xFF0050B3L),
            )
            CalendarColorScale.ORANGE -> listOf(
                Color(0xFFEBEDF0L), Color(0xFFFFD8B8L), Color(0xFFFFAD66L),
                Color(0xFFFA8C16L), Color(0xFFD46B08L),
            )
            CalendarColorScale.PURPLE -> listOf(
                Color(0xFFEBEDF0L), Color(0xFFD3ADF7L), Color(0xFF9254DEL),
                Color(0xFF722ED1L), Color(0xFF391085L),
            )
        }
    }
}

class CalendarHeatmapEvent : ComposeEvent() {
    var onDayClick: ((CalendarDay) -> Unit)? = null
}

class CalendarHeatmapView : ComposeView<CalendarHeatmapAttr, CalendarHeatmapEvent>() {

    override fun createAttr(): CalendarHeatmapAttr = CalendarHeatmapAttr()
    override fun createEvent(): CalendarHeatmapEvent = CalendarHeatmapEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val a = ctx.attr
            val cellStep = a.cellSize + a.cellSpacing
            val dayLabelW = if (a.showDayLabels) 24f else 0f
            val monthLabelH = if (a.showMonthLabels) 18f else 0f
            val canvasW = dayLabelW + a.weeks * cellStep
            val canvasH = monthLabelH + 7 * cellStep

            Canvas({ attr { width(canvasW); height(canvasH); overflow(false) } }) { context, width, height ->
                if (a.days.isEmpty()) return@Canvas

                val colors = a.resolveColors()
                val maxVal = a.days.maxOf { it.value }.coerceAtLeast(1)

                // Build lookup: "YYYY-MM-DD" -> CalendarDay
                val lookup = a.days.associateBy { "${it.year}-${it.month.toString().padStart(2,'0')}-${it.day.toString().padStart(2,'0')}" }

                // Find the start date (earliest day in data or first day of current view)
                val firstDay = a.days.minByOrNull { it.year * 10000 + it.month * 100 + it.day }
                    ?: return@Canvas

                // Day-of-week offset for first day (we approximate: day 0=Sun, 1=Mon...6=Sat)
                // Use Zeller's formula to get weekday for firstDay
                val fy = firstDay.year
                val fm = firstDay.month
                val fd = firstDay.day
                val startDow = weekday(fy, fm, fd)  // 0=Sun

                // Month labels
                if (a.showMonthLabels) {
                    context.font(a.labelFontSize)
                    context.fillStyle(a.labelColor)
                    context.textAlign(TextAlign.LEFT)
                    val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    var col = 0
                    var totalDays = startDow
                    var lastMonth = -1
                    while (col < a.weeks) {
                        val dayInWeek = 0  // check first day of each column
                        val daysFromStart = col * 7 - startDow
                        if (daysFromStart >= 0) {
                            // Approximate month for this column
                            val approxMonth = monthFromOffset(firstDay, daysFromStart)
                            if (approxMonth != lastMonth) {
                                lastMonth = approxMonth
                                val x = dayLabelW + col * cellStep
                                context.fillText(monthNames.getOrElse(approxMonth) { "" }, x, a.labelFontSize)
                            }
                        }
                        col++
                    }
                }

                // Day labels (Mon, Wed, Fri)
                if (a.showDayLabels) {
                    context.font(a.labelFontSize)
                    context.fillStyle(a.labelColor)
                    context.textAlign(TextAlign.RIGHT)
                    val dayLabels = listOf("", "Mon", "", "Wed", "", "Fri", "")
                    for (dow in 0..6) {
                        if (dayLabels[dow].isEmpty()) continue
                        val y = monthLabelH + dow * cellStep + a.cellSize * 0.75f
                        context.fillText(dayLabels[dow], dayLabelW - 2f, y)
                    }
                }

                // Draw cells
                for (col in 0 until a.weeks) {
                    for (row in 0..6) {
                        val totalOffset = col * 7 + row - startDow
                        if (totalOffset < 0) continue
                        val dayKey = dayKeyFromOffset(firstDay, totalOffset)
                        val dayData = lookup[dayKey]
                        val value = dayData?.value ?: 0
                        val level = when {
                            value <= 0            -> 0
                            value <= maxVal / 4   -> 1
                            value <= maxVal / 2   -> 2
                            value <= maxVal * 3/4 -> 3
                            else                  -> 4
                        }
                        val color = colors[level]
                        val x = dayLabelW + col * cellStep
                        val y = monthLabelH + row * cellStep
                        context.fillStyle(color)
                        context.fillRoundRect(x, y, a.cellSize, a.cellSize, a.borderRadius)
                    }
                }
            }
        }
    }

    // Zeller's congruence approximation for day of week (0=Sun, 1=Mon ... 6=Sat)
    private fun weekday(y: Int, m: Int, d: Int): Int {
        val ay = if (m < 3) y - 1 else y
        val am = if (m < 3) m + 12 else m
        val k = ay % 100
        val j = ay / 100
        val h = (d + (13 * (am + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7
        return ((h + 5) % 7 + 1) % 7  // 0=Sun
    }

    private fun monthFromOffset(base: CalendarDay, offset: Int): Int {
        // Simplified: assume 30-day months for label placement estimation
        val totalDay = base.month * 30 + base.day + offset
        return ((totalDay / 30) % 12).coerceIn(1, 12)
    }

    private fun dayKeyFromOffset(base: CalendarDay, offset: Int): String {
        // Simple implementation: convert to day-number, add offset, convert back
        var totalDays = daysFromEpoch(base.year, base.month, base.day) + offset
        val (y, m, d) = epochToDmy(totalDays)
        return "$y-${m.toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
    }

    private fun daysFromEpoch(y: Int, m: Int, d: Int): Int {
        val yy = if (m <= 2) y - 1 else y
        val mm = if (m <= 2) m + 12 else m
        return 365 * yy + yy / 4 - yy / 100 + yy / 400 + (153 * mm + 8) / 5 + d
    }

    private fun epochToDmy(n: Int): Triple<Int, Int, Int> {
        var z = n - 1
        val era = z / 146097
        z -= era * 146097
        val yoe = (z - z / 1460 + z / 36524 - z / 146096) / 365
        val y = yoe + era * 400
        val doy = z - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val yr = if (m <= 2) y + 1 else y
        return Triple(yr, m, d)
    }
}

fun ViewContainer<*, *>.CalendarHeatmap(init: CalendarHeatmapView.() -> Unit) {
    addChild(CalendarHeatmapView(), init)
}

// =============================================================================
// SparklineChart - minimal inline trend line, no axes, no labels
// =============================================================================

fun ViewContainer<*, *>.SparklineChart(init: SparklineChartView.() -> Unit) {
    addChild(SparklineChartView(), init)
}

class SparklineChartAttr : ComposeAttr() {
    internal var points by observable(emptyList<Float>())
    internal var lineColor by observable(Color(0xFF1677FFL))
    internal var fillColor by observable(Color(red255 = 22, green255 = 119, blue255 = 255, alpha01 = 0.15f))
    internal var sparkLineWidth by observable(1.5f)
    internal var filled by observable(true)

    fun data(values: List<Float>) { points = values }
    fun data(vararg values: Float) { points = values.toList() }
    fun lineColor(c: Color) { lineColor = c }
    fun fillColor(c: Color) { fillColor = c }
    fun lineWidth(w: Float) { sparkLineWidth = w.coerceAtLeast(0.5f) }
    fun filled(f: Boolean) { filled = f }
    fun chartSize(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }
}

class SparklineChartView : ComposeView<SparklineChartAttr, ComposeEvent>() {
    override fun createAttr(): SparklineChartAttr = SparklineChartAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { allFill() } }) { context, w, h ->
                val a = ctx.attr
                val pts = a.points
                val n = pts.size
                if (n < 2) return@Canvas

                val minV = pts.min()
                val maxV = pts.max()
                val range = if (abs(maxV - minV) < 1e-6f) 1f else maxV - minV

                fun toX(i: Int) = w * i / (n - 1)
                fun toY(v: Float) = h * (1f - (v - minV) / range)

                if (a.filled) {
                    context.beginPath()
                    context.moveTo(toX(0), h)
                    for (i in 0 until n) context.lineTo(toX(i), toY(pts[i]))
                    context.lineTo(toX(n - 1), h)
                    context.closePath()
                    context.fillStyle(a.fillColor)
                    context.fill()
                }

                context.beginPath()
                context.moveTo(toX(0), toY(pts[0]))
                for (i in 1 until n) context.lineTo(toX(i), toY(pts[i]))
                context.strokeStyle(a.lineColor)
                context.lineWidth(a.sparkLineWidth)
                context.lineCapRound()
                context.stroke()
            }
        }
    }
}

// =============================================================================
// ProgressRingChart - concentric activity rings (Apple Watch style)
// =============================================================================

fun ViewContainer<*, *>.ProgressRingChart(init: ProgressRingChartView.() -> Unit) {
    addChild(ProgressRingChartView(), init)
}

data class RingData(
    val label: String,
    val value: Float,
    val maxValue: Float,
    val color: Color,
    val trackColor: Color = Color(0x22000000L),
)

class ProgressRingChartAttr : ComposeAttr() {
    internal var rings by observable(emptyList<RingData>())
    internal var ringWidth by observable(14f)
    internal var ringGap by observable(6f)
    internal var centerText by observable("")
    internal var centerFontSize by observable(14f)
    internal var showLabels by observable(true)
    internal var labelFontSize by observable(11f)
    internal var startAngleDeg by observable(-90f)

    fun rings(list: List<RingData>) { rings = list }
    fun rings(vararg r: RingData) { rings = r.toList() }
    fun ringWidth(w: Float) { ringWidth = w.coerceIn(4f, 40f) }
    fun ringGap(g: Float) { ringGap = g.coerceAtLeast(2f) }
    fun centerText(t: String) { centerText = t }
    fun centerFontSize(sz: Float) { centerFontSize = sz }
    fun showLabels(show: Boolean) { showLabels = show }
    fun labelFontSize(sz: Float) { labelFontSize = sz }
    fun startAngle(degrees: Float) { startAngleDeg = degrees }
    fun chartSize(w: Float, h: Float) {
        if (!w.isNaN()) width(w)
        if (!h.isNaN()) height(h)
    }
}

class ProgressRingChartView : ComposeView<ProgressRingChartAttr, ComposeEvent>() {
    override fun createAttr(): ProgressRingChartAttr = ProgressRingChartAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({ attr { allFill() } }) { context, w, h ->
                val a = ctx.attr
                val rings = a.rings
                if (rings.isEmpty()) return@Canvas

                val n = rings.size
                val labelH = if (a.showLabels) 20f else 0f
                val chartH = h - labelH
                val cx = w / 2f
                val cy = chartH / 2f
                val totalRingSpace = n * a.ringWidth + (n - 1) * a.ringGap
                val outerR = minOf(cx, cy) - 4f
                val innerR = (outerR - totalRingSpace).coerceAtLeast(4f)

                val startRad = a.startAngleDeg * PI.toFloat() / 180f
                val twoPi = 2f * PI.toFloat()

                rings.forEachIndexed { i, ring ->
                    val midR = outerR - i * (a.ringWidth + a.ringGap) - a.ringWidth / 2f
                    if (midR <= 0f) return@forEachIndexed

                    // Track (background arc)
                    context.beginPath()
                    context.arc(cx, cy, midR, startRad, startRad + twoPi, false)
                    context.strokeStyle(ring.trackColor)
                    context.lineWidth(a.ringWidth)
                    context.stroke()

                    // Progress arc
                    val fraction = if (ring.maxValue > 0f) (ring.value / ring.maxValue).coerceIn(0f, 1f) else 0f
                    if (fraction > 0f) {
                        val endRad = startRad + twoPi * fraction
                        context.beginPath()
                        context.arc(cx, cy, midR, startRad, endRad, false)
                        context.strokeStyle(ring.color)
                        context.lineWidth(a.ringWidth)
                        context.lineCapRound()
                        context.stroke()
                    }
                }

                // Center text
                if (a.centerText.isNotEmpty()) {
                    context.font(a.centerFontSize)
                    context.fillStyle(Color(0xFF282828L))
                    context.textAlign(TextAlign.CENTER)
                    context.fillText(a.centerText, cx, cy + a.centerFontSize * 0.35f)
                    context.textAlign(TextAlign.LEFT)
                }

                // Legend row at bottom
                if (a.showLabels && rings.isNotEmpty()) {
                    val legendY = chartH + 4f + a.labelFontSize
                    val slotW = w / n.coerceAtLeast(1)
                    context.font(a.labelFontSize)
                    rings.forEachIndexed { i, ring ->
                        val lx = i * slotW + slotW / 2f
                        context.fillStyle(ring.color)
                        context.textAlign(TextAlign.CENTER)
                        val pct = if (ring.maxValue > 0f) ((ring.value / ring.maxValue) * 100f).roundToInt() else 0
                        context.fillText("${ring.label} $pct%", lx, legendY)
                    }
                    context.textAlign(TextAlign.LEFT)
                }
            }
        }
    }
}
