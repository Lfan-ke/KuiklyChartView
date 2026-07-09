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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
    internal var axisColor by observable(Color(180, 180, 180, 1f))
    internal var gridColor by observable(Color(220, 220, 220, 1f))
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

    fun showDots(show: Boolean) { showDots = show }
    fun dotRadius(r: Float) { dotRadius = r.coerceAtLeast(1f) }
    fun lineWidth(w: Float) { lineWidth = w.coerceAtLeast(0.5f) }
    fun fillArea(fill: Boolean) { fillArea = fill }
}

class LineChartView : ComposeView<LineChartAttr, ChartEvent>() {

    private var lastW = 0f
    private var lastH = 0f

    override fun createAttr() = LineChartAttr()
    override fun createEvent() = ChartEvent()

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
                        context.fillStyle(Color(s.color.hexColor and 0x00FFFFFFL, 0.15f))
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
            }
        }
    }

    private fun handleLineClick(params: ClickParams) {
        val handler = event.onPointClickHandler ?: return
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
        val hitRadius = attr.dotRadius + 8f
        var found = false
        series.forEachIndexed { sIdx, s ->
            if (found) return@forEachIndexed
            val n = s.points.size
            s.points.forEachIndexed { pIdx, pt ->
                if (found) return@forEachIndexed
                val dx = params.x - toX(pIdx, n)
                val dy = params.y - toY(pt.value)
                if (sqrt(dx * dx + dy * dy) <= hitRadius) {
                    handler(sIdx, pIdx, pt.value)
                    found = true
                }
            }
        }
    }
}

class BarChartAttr : ChartAttr() {

    internal var barSpacing by observable(0.2f)
    internal var cornerRadius by observable(2f)
    internal var showValueLabels by observable(true)

    fun barSpacing(fraction: Float) { barSpacing = fraction.coerceIn(0f, 0.8f) }
    fun cornerRadius(r: Float) { cornerRadius = r.coerceAtLeast(0f) }
    fun showValueLabels(show: Boolean) { showValueLabels = show }
}

class BarChartView : ComposeView<BarChartAttr, ChartEvent>() {

    private var lastW = 0f
    private var lastH = 0f

    override fun createAttr() = BarChartAttr()
    override fun createEvent() = ChartEvent()

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
            }
        }
    }

    private fun handleBarClick(params: ClickParams) {
        val handler = event.onPointClickHandler ?: return
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
        var found = false
        series.forEachIndexed { sIdx, s ->
            if (found) return@forEachIndexed
            s.points.forEachIndexed { gIdx, pt ->
                if (found) return@forEachIndexed
                if (pt.value <= 0f) return@forEachIndexed
                val x = PADDING_LEFT + spacing / 2f + gIdx * slotW + sIdx * barW
                val barTop = toY(pt.value)
                val barBottom = PADDING_TOP + plotH
                if (params.x >= x && params.x <= x + barW && params.y >= barTop && params.y <= barBottom) {
                    handler(sIdx, gIdx, pt.value)
                    found = true
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
    override fun createAttr() = AreaChartAttr()
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

    override fun createAttr() = PieChartAttr()
    override fun createEvent() = PieChartEvent()

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
                    context.fillStyle(Color.WHITE)
                    angle = startAngleRad
                    slices.forEachIndexed { idx, slice ->
                        val sweep = sweepAngles[idx]
                        val midAngle = angle + sweep / 2f
                        val labelR = (innerR + outerR) / 2f
                        val lx = cx + labelR * cos(midAngle)
                        val ly = cy + labelR * sin(midAngle)
                        val pct = (slice.value / total * 100f).roundToInt()
                        if (sweep > 0.26f) {
                            val text = "$pct%"
                            context.fillText(text, lx - text.length * 3f, ly + 4f)
                        }
                        angle += sweep
                    }
                }

                if (ctx.attr.showLegend) {
                    val legendY = chartH + 6f
                    val slotW = w / slices.size.coerceAtLeast(1)
                    context.font(ctx.attr.labelFontSize)
                    slices.forEachIndexed { idx, slice ->
                        val lx = idx * slotW + 4f
                        context.fillStyle(slice.color)
                        context.fillRect(lx, legendY, 10f, 10f)
                        context.fillStyle(Color(80, 80, 80, 1f))
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
    val fillColor: Color = Color(180, 180, 180, 0.2f),
)

class RadarChartAttr : ComposeAttr() {
    internal var axes by observable(emptyList<RadarAxis>())
    internal var series by observable(emptyList<RadarSeries>())
    internal var webColor by observable(Color(220, 220, 220, 1f))
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
    override fun createAttr() = RadarChartAttr()
    override fun createEvent() = RadarChartEvent()

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
                    context.fillStyle(Color(80, 80, 80, 1f))
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
                        context.fillStyle(Color(80, 80, 80, 1f))
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
    internal var trackColor by observable(Color(220, 220, 220, 1f))
    internal var arcWidth by observable(16f)
    internal var showNeedle by observable(true)
    internal var needleColor by observable(Color(80, 80, 80, 1f))
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
    override fun createAttr() = GaugeChartAttr()

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
                    context.fillStyle(Color(120, 120, 120, 1f))
                    val labelR = r + 6f
                    val minX = cx + labelR * cos(startRad)
                    val minY = cy + labelR * sin(startRad)
                    context.fillText(a.minValue.fmt0(), minX - 14f, minY + 4f)
                    val maxX = cx + labelR * cos(endRad)
                    val maxY = cy + labelR * sin(endRad)
                    context.fillText(a.maxValue.fmt0(), maxX - 6f, maxY + 4f)
                }

                context.font(a.valueFontSize)
                context.fillStyle(Color(40, 40, 40, 1f))
                val valText = if (a.unit.isEmpty()) clampedValue.fmt1() else "${clampedValue.fmt0()}${a.unit}"
                val valW = valText.length * a.valueFontSize * 0.55f
                context.fillText(valText, cx - valW / 2f, cy + r * 0.1f)

                if (a.label.isNotEmpty()) {
                    context.font(a.labelFontSize)
                    context.fillStyle(Color(120, 120, 120, 1f))
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
    internal var axisColor by observable(Color(180, 180, 180, 1f))
    internal var gridColor by observable(Color(220, 220, 220, 1f))
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

    override fun createAttr() = ScatterChartAttr()
    override fun createEvent() = ScatterChartEvent()

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
                    context.fillStyle(Color(100, 100, 100, 1f))
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
                        context.fillStyle(Color(80, 80, 80, 1f))
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

    override fun createAttr() = FunnelChartAttr()
    override fun createEvent() = FunnelChartEvent()

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
                        context.fillStyle(Color(80, 80, 80, 1f))
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
