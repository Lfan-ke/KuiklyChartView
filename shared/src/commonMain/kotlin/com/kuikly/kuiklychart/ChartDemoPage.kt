package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chart.AreaChart
import com.tencent.kuiklybase.chart.BarChart
import com.tencent.kuiklybase.chart.ChartDataPoint
import com.tencent.kuiklybase.chart.ChartSeries
import com.tencent.kuiklybase.chart.GaugeChart
import com.tencent.kuiklybase.chart.LineChart
import com.tencent.kuiklybase.chart.PieChart
import com.tencent.kuiklybase.chart.PieSlice
import com.tencent.kuiklybase.chart.RadarAxis
import com.tencent.kuiklybase.chart.RadarChart
import com.tencent.kuiklybase.chart.RadarSeries
import com.tencent.kuiklybase.chart.FunnelChart
import com.tencent.kuiklybase.chart.FunnelSlice
import com.tencent.kuiklybase.chart.ScatterChart
import com.tencent.kuiklybase.chart.ScatterPoint
import com.tencent.kuiklybase.chart.ScatterSeries
import com.tencent.kuiklybase.chart.WaterfallChart
import com.tencent.kuiklybase.chart.WaterfallBar
import com.tencent.kuiklybase.chart.WaterfallBarType
import com.tencent.kuiklybase.chart.CandlestickChart
import com.tencent.kuiklybase.chart.CandleStick
import com.tencent.kuiklybase.chart.HeatmapChart
import com.tencent.kuiklybase.chart.HeatmapCell
import com.tencent.kuiklybase.chart.TreemapChart
import com.tencent.kuiklybase.chart.TreemapNode
import com.tencent.kuiklybase.chart.BoxplotChart
import com.tencent.kuiklybase.chart.BoxplotData
import com.tencent.kuiklybase.chart.BubbleChart
import com.tencent.kuiklybase.chart.BubbleData
import com.tencent.kuiklybase.chart.SankeyChart
import com.tencent.kuiklybase.chart.SankeyNode
import com.tencent.kuiklybase.chart.SankeyLink
import com.tencent.kuiklybase.chart.CalendarDay
import com.tencent.kuiklybase.chart.CalendarHeatmap
import com.tencent.kuiklybase.chart.CalendarColorScale
import com.tencent.kuiklybase.chart.MixedChart
import com.tencent.kuiklybase.chart.MixedSeries
import com.tencent.kuiklybase.chart.MixedSeriesType
import com.tencent.kuiklybase.chart.NightingaleRoseChart
import com.tencent.kuiklybase.chart.RoseSlice

@Page("ChartDemoPage")
internal class ChartDemoPage : com.tencent.kuikly.core.pager.Pager() {

    private var lastClick by observable("")
    private var lastPieClick by observable("")
    private var lastScatterClick by observable("")
    private var lastFunnelClick by observable("")
    private var lastCandleClick by observable("")
    private var lastHeatmapClick by observable("")
    private var lastTreemapClick by observable("")
    private var lastBoxplotClick by observable("")
    private var lastBubbleClick by observable("")
    private var lastSankeyClick by observable("")
    private var lastRoseClick by observable("")
    private var gaugeValue by observable(72f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xF5F5F5))
                }

                // Title
                Text {
                    attr {
                        text("KuiklyChartView Demo")
                        fontSize(20f)
                        fontWeightBold()
                        color(Color(0x333333))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }

                // Line Chart section
                Text {
                    attr {
                        text("折线图 - LineChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 16f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(220f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    LineChart {
                        attr {
                            size(Float.NaN, 220f)
                            data(
                                ChartSeries(
                                    name = "月销售额",
                                    points = listOf(
                                        ChartDataPoint("1月", 12.5f),
                                        ChartDataPoint("2月", 18.3f),
                                        ChartDataPoint("3月", 15.1f),
                                        ChartDataPoint("4月", 22.7f),
                                        ChartDataPoint("5月", 19.4f),
                                        ChartDataPoint("6月", 28.2f),
                                    ),
                                    color = Color(0x007AFF),
                                ),
                                ChartSeries(
                                    name = "月成本",
                                    points = listOf(
                                        ChartDataPoint("1月", 8.0f),
                                        ChartDataPoint("2月", 11.5f),
                                        ChartDataPoint("3月", 10.2f),
                                        ChartDataPoint("4月", 14.3f),
                                        ChartDataPoint("5月", 13.1f),
                                        ChartDataPoint("6月", 17.8f),
                                    ),
                                    color = Color(0xFF3B30),
                                ),
                            )
                            fillArea(true)
                            lineWidth(2.5f)
                        }
                        event {
                            onPointClick { sIdx, pIdx, value ->
                                ctx.lastClick = "折线图 系列$sIdx 点$pIdx 值=${value.fmt()}"
                            }
                        }
                    }
                    Text {
                        attr {
                            text(ctx.lastClick)
                            fontSize(12f)
                            color(Color(0x007AFF))
                            margin(left = 16f, top = 4f)
                        }
                    }
                }

                // Bar Chart section
                Text {
                    attr {
                        text("柱状图 - BarChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(220f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    BarChart {
                        attr {
                            size(Float.NaN, 220f)
                            data(
                                ChartSeries(
                                    name = "iOS",
                                    points = listOf(
                                        ChartDataPoint("Q1", 45f),
                                        ChartDataPoint("Q2", 62f),
                                        ChartDataPoint("Q3", 58f),
                                        ChartDataPoint("Q4", 74f),
                                    ),
                                    color = Color(0x007AFF),
                                ),
                                ChartSeries(
                                    name = "Android",
                                    points = listOf(
                                        ChartDataPoint("Q1", 38f),
                                        ChartDataPoint("Q2", 55f),
                                        ChartDataPoint("Q3", 63f),
                                        ChartDataPoint("Q4", 70f),
                                    ),
                                    color = Color(0x34C759),
                                ),
                                ChartSeries(
                                    name = "鸿蒙",
                                    points = listOf(
                                        ChartDataPoint("Q1", 12f),
                                        ChartDataPoint("Q2", 19f),
                                        ChartDataPoint("Q3", 28f),
                                        ChartDataPoint("Q4", 41f),
                                    ),
                                    color = Color(0xFF9500),
                                ),
                            )
                            cornerRadius(4f)
                            barSpacing(0.25f)
                        }
                        event {
                            onPointClick { sIdx, pIdx, value ->
                                ctx.lastClick = "柱状图 系列$sIdx 组$pIdx 值=${value.fmt()}"
                            }
                        }
                    }
                }

                // Area Chart section
                Text {
                    attr {
                        text("面积图 - AreaChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(200f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    AreaChart {
                        attr {
                            size(Float.NaN, 200f)
                            data(
                                ChartSeries(
                                    name = "月销售额",
                                    points = listOf(
                                        ChartDataPoint("1月", 12.5f),
                                        ChartDataPoint("2月", 18.3f),
                                        ChartDataPoint("3月", 15.1f),
                                        ChartDataPoint("4月", 22.7f),
                                        ChartDataPoint("5月", 19.4f),
                                        ChartDataPoint("6月", 28.2f),
                                    ),
                                    color = Color(0x007AFF),
                                ),
                                ChartSeries(
                                    name = "月成本",
                                    points = listOf(
                                        ChartDataPoint("1月", 8.0f),
                                        ChartDataPoint("2月", 11.5f),
                                        ChartDataPoint("3月", 10.2f),
                                        ChartDataPoint("4月", 14.3f),
                                        ChartDataPoint("5月", 13.1f),
                                        ChartDataPoint("6月", 17.8f),
                                    ),
                                    color = Color(0xFF3B30),
                                ),
                            )
                            lineWidth(2f)
                        }
                    }
                }

                // Pie Chart section
                Text {
                    attr {
                        text("饼图 - PieChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(260f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    PieChart {
                        attr {
                            size(Float.NaN, 260f)
                            data(
                                PieSlice("iOS", 45f, Color(0x007AFF)),
                                PieSlice("Android", 38f, Color(0x34C759)),
                                PieSlice("鸿蒙", 12f, Color(0xFF9500)),
                                PieSlice("Web", 3f, Color(0xFF3B30)),
                                PieSlice("其他", 2f, Color(0x8E8E93)),
                            )
                            holeRadius(0.35f)
                        }
                        event {
                            onSliceClick { idx, label, value ->
                                ctx.lastPieClick = "点击 $label: ${value.fmt()}"
                            }
                        }
                    }
                    Text {
                        attr {
                            text(ctx.lastPieClick)
                            fontSize(12f)
                            color(Color(0x007AFF))
                            margin(left = 16f, top = 4f)
                        }
                    }
                }

                // Donut Chart section
                Text {
                    attr {
                        text("环形图 - DonutChart（holeRadius=0.6）")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(240f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    PieChart {
                        attr {
                            size(Float.NaN, 240f)
                            data(
                                PieSlice("Q1", 23f, Color(0x5856D6)),
                                PieSlice("Q2", 31f, Color(0xFF2D55)),
                                PieSlice("Q3", 28f, Color(0xFF9500)),
                                PieSlice("Q4", 18f, Color(0x34C759)),
                            )
                            holeRadius(0.6f)
                            strokeColor(Color(0xF5F5F5))
                            strokeWidth(3f)
                        }
                    }
                }

                // Radar Chart section
                Text {
                    attr {
                        text("雷达图 - RadarChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(260f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    RadarChart {
                        attr {
                            size(Float.NaN, 260f)
                            axes(
                                RadarAxis("前端", 100f),
                                RadarAxis("后端", 100f),
                                RadarAxis("移动端", 100f),
                                RadarAxis("AI", 100f),
                                RadarAxis("运维", 100f),
                                RadarAxis("设计", 100f),
                            )
                            series(
                                RadarSeries(
                                    "候选人A",
                                    listOf(90f, 75f, 60f, 85f, 50f, 70f),
                                    Color(0xFF1677FFL),
                                    Color(0x331677FFL),
                                ),
                                RadarSeries(
                                    "候选人B",
                                    listOf(60f, 90f, 80f, 55f, 75f, 40f),
                                    Color(0xFF52C41AL),
                                    Color(0x3352C41AL),
                                ),
                            )
                            showLegend(true)
                        }
                    }
                }

                // Gauge Chart section
                Text {
                    attr {
                        text("仪表盘 - GaugeChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(220f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    GaugeChart {
                        attr {
                            size(Float.NaN, 220f)
                            value(ctx.gaugeValue)
                            range(0f, 100f)
                            arcColor(Color(0xFF1677FFL))
                            arcWidth(20f)
                            label("性能评分")
                            unit("%")
                            showNeedle(true)
                        }
                    }
                }
                View {
                    attr {
                        height(44f)
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentCenter()
                        margin(left = 16f, right = 16f, top = 8f)
                    }
                    Text {
                        attr {
                            text("调整数值: ")
                            fontSize(12f)
                            color(Color(0x888888))
                        }
                    }
                    for (v in listOf(25f, 50f, 72f, 90f)) {
                        View {
                            attr {
                                paddingLeft(14f)
                                paddingRight(14f)
                                paddingTop(6f)
                                paddingBottom(6f)
                                borderRadius(14f)
                                marginLeft(8f)
                                backgroundColor(
                                    if (ctx.gaugeValue == v) Color(0xFF1677FFL) else Color(0xFFE0E0E0L)
                                )
                            }
                            event { click { ctx.gaugeValue = v } }
                            Text {
                                attr {
                                    text("${v.toInt()}")
                                    fontSize(13f)
                                    color(
                                        if (ctx.gaugeValue == v) Color(0xFFFFFFFF) else Color(0xFF555555)
                                    )
                                }
                            }
                        }
                    }
                }

                // Scatter Chart section
                Text {
                    attr {
                        text("散点图 - ScatterChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(240f)
                        margin(left = 16f, right = 16f, bottom = 32f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    ScatterChart {
                        attr {
                            size(Float.NaN, 240f)
                            data(
                                ScatterSeries(
                                    "产品A",
                                    listOf(
                                        ScatterPoint(2f, 4f, 8f),
                                        ScatterPoint(3f, 8f, 10f),
                                        ScatterPoint(5f, 6f, 7f),
                                        ScatterPoint(7f, 12f, 12f),
                                        ScatterPoint(9f, 10f, 9f),
                                        ScatterPoint(11f, 15f, 11f),
                                    ),
                                    Color(0xFF1677FFL),
                                ),
                                ScatterSeries(
                                    "产品B",
                                    listOf(
                                        ScatterPoint(1f, 7f, 9f),
                                        ScatterPoint(4f, 3f, 7f),
                                        ScatterPoint(6f, 9f, 8f),
                                        ScatterPoint(8f, 5f, 10f),
                                        ScatterPoint(10f, 11f, 12f),
                                        ScatterPoint(12f, 8f, 6f),
                                    ),
                                    Color(0xFFFF7043L),
                                ),
                            )
                            showGrid(true)
                            showLegend(true)
                        }
                        event {
                            onPointClick { _, _, x, y ->
                                ctx.lastScatterClick = "(${x.toInt()}, ${y.toInt()})"
                            }
                        }
                    }
                    Text {
                        attr {
                            text(if (ctx.lastScatterClick.isEmpty()) "" else "点击了: ${ctx.lastScatterClick}")
                            fontSize(12f)
                            color(Color(0x007AFF))
                            margin(left = 16f, top = 4f)
                        }
                    }
                }

                // Funnel Chart section
                Text {
                    attr {
                        text("漏斗图 - FunnelChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(300f)
                        margin(left = 16f, right = 16f, bottom = 32f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    FunnelChart {
                        attr {
                            size(Float.NaN, 300f)
                            data(
                                FunnelSlice("访问", 500f, Color(0xFF1677FFL)),
                                FunnelSlice("点击", 350f, Color(0xFF52C41AL)),
                                FunnelSlice("加入购物车", 220f, Color(0xFFFF7A45L)),
                                FunnelSlice("支付", 120f, Color(0xFFFF4D4FL)),
                                FunnelSlice("复购", 60f, Color(0xFF722ED1L)),
                            )
                            showLabels(true)
                            showValues(true)
                            showLegend(true)
                            gap(6f)
                            strokeWidth(1.5f)
                        }
                        event {
                            onSliceClick { _, label, value ->
                                ctx.lastFunnelClick = "$label: ${value.toInt()}"
                            }
                        }
                    }
                    Text {
                        attr {
                            text(if (ctx.lastFunnelClick.isEmpty()) "" else "点击了: ${ctx.lastFunnelClick}")
                            fontSize(12f)
                            color(Color(0x007AFF))
                            margin(left = 16f, top = 4f)
                        }
                    }
                }

                // --- WaterfallChart ---
                View {
                    attr {
                        height(36f)
                        backgroundColor(Color(0xFFEEEEEEL))
                        justifyContentCenter()
                        paddingLeft(16f)
                        marginTop(12f)
                    }
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF555555L))
                            text("瀑布图 - 季度收益")
                        }
                    }
                }
                View {
                    attr {
                        height(200f)
                        marginLeft(16f)
                        marginRight(16f)
                        marginTop(8f)
                    }
                    WaterfallChart {
                        attr {
                            bars(
                                WaterfallBar("基础", 100f, WaterfallBarType.START),
                                WaterfallBar("Q1销售", 45f, WaterfallBarType.INCREASE),
                                WaterfallBar("Q2增长", 30f, WaterfallBarType.INCREASE),
                                WaterfallBar("Q3成本", 20f, WaterfallBarType.DECREASE),
                                WaterfallBar("Q4冲刺", 25f, WaterfallBarType.INCREASE),
                                WaterfallBar("年末", 0f, WaterfallBarType.TOTAL),
                            )
                        }
                    }
                }

                // --- CandlestickChart ---
                View {
                    attr {
                        height(36f)
                        backgroundColor(Color(0xFFEEEEEEL))
                        justifyContentCenter()
                        paddingLeft(16f)
                        marginTop(12f)
                    }
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF555555L))
                            text("K线图 - 点击蜡烛查看数据")
                        }
                    }
                }
                View {
                    attr {
                        height(220f)
                        marginLeft(16f)
                        marginRight(16f)
                        marginTop(8f)
                    }
                    CandlestickChart {
                        attr {
                            candles(
                                CandleStick("Mon", 100f, 108f, 112f, 97f),
                                CandleStick("Tue", 108f, 105f, 115f, 103f),
                                CandleStick("Wed", 105f, 118f, 121f, 104f),
                                CandleStick("Thu", 118f, 112f, 122f, 110f),
                                CandleStick("Fri", 112f, 125f, 128f, 111f),
                                CandleStick("Mon", 125f, 120f, 130f, 118f),
                            )
                        }
                        event {
                            onCandleClick { _, candle ->
                                ctx.lastCandleClick = "点击: ${candle.label} O=${candle.open.toInt()} C=${candle.close.toInt()}"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastCandleClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 16f)
                    }
                }

                // Heatmap Chart section
                Text {
                    attr {
                        text("热力图 - HeatmapChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(120f)
                        margin(left = 16f, right = 16f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    HeatmapChart {
                        attr {
                            size(Float.NaN, 120f)
                            cols(7)
                            rows(4)
                            cellPadding(4f)
                            cellRadius(3f)
                            cells(
                                HeatmapCell(0, 0, 2f), HeatmapCell(1, 0, 0f), HeatmapCell(2, 0, 5f),
                                HeatmapCell(3, 0, 8f), HeatmapCell(4, 0, 3f), HeatmapCell(5, 0, 6f),
                                HeatmapCell(6, 0, 0f),
                                HeatmapCell(0, 1, 4f), HeatmapCell(1, 1, 9f), HeatmapCell(2, 1, 7f),
                                HeatmapCell(3, 1, 2f), HeatmapCell(4, 1, 10f), HeatmapCell(5, 1, 5f),
                                HeatmapCell(6, 1, 1f),
                                HeatmapCell(0, 2, 0f), HeatmapCell(1, 2, 3f), HeatmapCell(2, 2, 8f),
                                HeatmapCell(3, 2, 6f), HeatmapCell(4, 2, 4f), HeatmapCell(5, 2, 9f),
                                HeatmapCell(6, 2, 7f),
                                HeatmapCell(0, 3, 1f), HeatmapCell(1, 3, 5f), HeatmapCell(2, 3, 0f),
                                HeatmapCell(3, 3, 3f), HeatmapCell(4, 3, 7f), HeatmapCell(5, 3, 2f),
                                HeatmapCell(6, 3, 4f),
                            )
                        }
                        event {
                            onCellClick { cell ->
                                ctx.lastHeatmapClick = "点击: (${cell.col}, ${cell.row}) 值=${cell.value.toInt()}"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastHeatmapClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 16f)
                    }
                }

                // Treemap Chart section
                Text {
                    attr {
                        text("矩形树图 - TreemapChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(200f)
                        margin(left = 16f, right = 16f, bottom = 32f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    TreemapChart {
                        attr {
                            size(Float.NaN, 200f)
                            padding(3f)
                            labelFontSize(12f)
                            showValues(true)
                            nodes(
                                TreemapNode("Apple", 3000f, Color(0xFF555555L)),
                                TreemapNode("MSFT", 2800f, Color(0xFF007AE0L)),
                                TreemapNode("Google", 2200f, Color(0xFF4285F4L)),
                                TreemapNode("Amazon", 1800f, Color(0xFFFF9900L)),
                                TreemapNode("Meta", 1200f, Color(0xFF1877F2L)),
                                TreemapNode("Netflix", 300f, Color(0xFFE50914L)),
                            )
                        }
                        event {
                            onNodeClick { node ->
                                ctx.lastTreemapClick = "点击: ${node.label} 市值=${node.value.toInt()}B"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastTreemapClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 16f)
                    }
                }

                // Boxplot Chart section
                Text {
                    attr {
                        text("箱线图 - BoxplotChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(220f)
                        margin(left = 16f, right = 16f, bottom = 32f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    BoxplotChart {
                        attr {
                            size(Float.NaN, 220f)
                            boxes(
                                BoxplotData("Q1", 10f, 20f, 35f, 55f, 70f, listOf(5f, 78f)),
                                BoxplotData("Q2", 15f, 28f, 42f, 60f, 80f),
                                BoxplotData("Q3", 8f, 18f, 30f, 48f, 65f, listOf(2f, 72f)),
                                BoxplotData("Q4", 20f, 35f, 50f, 68f, 85f),
                                BoxplotData("Q5", 12f, 22f, 38f, 52f, 75f, listOf(90f)),
                            )
                        }
                        event {
                            onBoxClick { box ->
                                ctx.lastBoxplotClick = "点击: ${box.label} 中位数=${box.median.toInt()}"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastBoxplotClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 16f)
                    }
                }

                // Bubble Chart section
                Text {
                    attr {
                        text("气泡图 - BubbleChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(220f)
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    BubbleChart {
                        attr {
                            size(Float.NaN, 220f)
                            showLabels(true)
                            points(
                                BubbleData(10f, 20f, 30f, "A", Color(0xFF1677FFL)),
                                BubbleData(25f, 55f, 60f, "B", Color(0xFF52C41AL)),
                                BubbleData(40f, 30f, 45f, "C", Color(0xFFFA8C16L)),
                                BubbleData(60f, 70f, 80f, "D", Color(0xFFFF4D4FL)),
                                BubbleData(75f, 40f, 35f, "E", Color(0xFF722ED1L)),
                                BubbleData(85f, 85f, 50f, "F", Color(0xFF13C2C2L)),
                            )
                        }
                        event {
                            onBubbleClick { b ->
                                ctx.lastBubbleClick = "点击: ${b.label} (${b.x.toInt()}, ${b.y.toInt()}) r=${b.r.toInt()}"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastBubbleClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 16f)
                    }
                }

                // Sankey Chart section
                Text {
                    attr {
                        text("桑基图 - SankeyChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(240f)
                        margin(left = 16f, right = 16f, bottom = 32f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    SankeyChart {
                        attr {
                            size(Float.NaN, 240f)
                            nodes(
                                SankeyNode("a", "访问"),
                                SankeyNode("b", "注册"),
                                SankeyNode("c", "购物车"),
                                SankeyNode("d", "结算"),
                                SankeyNode("e", "成交"),
                                SankeyNode("f", "流失"),
                            )
                            links(
                                SankeyLink("a", "b", 800f),
                                SankeyLink("a", "f", 200f),
                                SankeyLink("b", "c", 600f),
                                SankeyLink("b", "f", 200f),
                                SankeyLink("c", "d", 450f),
                                SankeyLink("c", "f", 150f),
                                SankeyLink("d", "e", 380f),
                                SankeyLink("d", "f", 70f),
                            )
                        }
                        event {
                            onNodeClick { n ->
                                ctx.lastSankeyClick = "点击节点: ${n.label}"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastSankeyClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 32f)
                    }
                }

                // ── NightingaleRoseChart ──────────────────────────────────
                Text {
                    attr {
                        text("玫瑰图 - NightingaleRoseChart")
                        fontSize(14f)
                        color(Color(0x666666))
                        margin(left = 16f, top = 24f, bottom = 8f)
                    }
                }
                View {
                    attr {
                        height(260f)
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFFFFF))
                        borderRadius(8f)
                    }
                    NightingaleRoseChart {
                        attr {
                            allFill()
                            slices(
                                RoseSlice("Q1", 320f, Color(0xFF1677FFL)),
                                RoseSlice("Q2", 480f, Color(0xFF36CFDBL)),
                                RoseSlice("Q3", 560f, Color(0xFF52C41AL)),
                                RoseSlice("Q4", 410f, Color(0xFFFA8C16L)),
                                RoseSlice("Q5", 290f, Color(0xFFFF4D4FL)),
                                RoseSlice("Q6", 640f, Color(0xFF722ED1L)),
                            )
                            showPercentage(true)
                            showCenter(true)
                            centerText("收入")
                            innerRadius(0.25f)
                            labelFontSize(10f)
                        }
                        event {
                            onSliceClick { slice, idx ->
                                ctx.lastRoseClick = "点击: ${slice.label} = ${slice.value}"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(ctx.lastRoseClick)
                        fontSize(12f)
                        color(Color(0xFF1677FFL))
                        margin(left = 16f, top = 4f, bottom = 32f)
                    }
                }

                // ── MixedChart ──────────────────────────────────────────────
                View {
                    attr { marginLeft(16f); marginTop(16f); marginBottom(4f) }
                    Text {
                        attr { text("混合图 - MixedChart (柱+线)"); fontSize(14f); color(Color(0xFF333333L)) }
                    }
                }
                View {
                    attr { height(220f); marginLeft(16f); marginRight(16f); marginBottom(16f) }
                    MixedChart {
                        attr {
                            mixedData(
                                MixedSeries(
                                    "月销量",
                                    listOf("1月","2月","3月","4月","5月","6月").mapIndexed { i, m ->
                                        ChartDataPoint(m, listOf(120f,200f,150f,80f,170f,110f)[i])
                                    },
                                    Color(0xFF1677FFL),
                                    MixedSeriesType.BAR,
                                ),
                                MixedSeries(
                                    "环比增长",
                                    listOf("1月","2月","3月","4月","5月","6月").mapIndexed { i, m ->
                                        ChartDataPoint(m, listOf(50f,180f,100f,60f,150f,90f)[i])
                                    },
                                    Color(0xFFFF4D4FL),
                                    MixedSeriesType.LINE,
                                    lineWidth = 2f,
                                    dotRadius = 4f,
                                ),
                            )
                            showGrid(true)
                        }
                    }
                }

                // ── CalendarHeatmap ─────────────────────────────────────────
                View {
                    attr { marginLeft(16f); marginTop(16f); marginBottom(4f) }
                    Text {
                        attr { text("日历热力图 - CalendarHeatmap"); fontSize(14f); color(Color(0xFF333333L)) }
                    }
                }
                View {
                    attr { marginLeft(16f); marginRight(16f); marginBottom(8f) }
                    CalendarHeatmap {
                        attr {
                            // Generate 6 months of synthetic activity data
                            days(buildList {
                                val baseYear = 2025
                                val months = listOf(
                                    Pair(7, 31), Pair(8, 31), Pair(9, 30),
                                    Pair(10, 31), Pair(11, 30), Pair(12, 31)
                                )
                                months.forEach { (m, days) ->
                                    for (d in 1..days) {
                                        val value = when {
                                            (d + m) % 7 == 0 -> 0
                                            (d + m) % 5 == 0 -> 15 + (d % 5) * 3
                                            (d + m) % 3 == 0 -> 8 + d % 4
                                            d % 2 == 0       -> 3 + m % 3
                                            else             -> 0
                                        }
                                        add(CalendarDay(baseYear, m, d, value))
                                    }
                                }
                            })
                            weeks(27)
                            colorScale(CalendarColorScale.GREEN)
                            cellSize(14f)
                            cellSpacing(2f)
                        }
                    }
                }
                View {
                    attr { marginLeft(16f); marginTop(8f); marginBottom(8f) }
                    Text {
                        attr { text("蓝色主题"); fontSize(12f); color(Color(0xFF999999L)) }
                    }
                }
                View {
                    attr { marginLeft(16f); marginRight(16f); marginBottom(32f) }
                    CalendarHeatmap {
                        attr {
                            days(buildList {
                                for (d in 1..90) {
                                    val m = when { d <= 31 -> 1; d <= 59 -> 2; else -> 3 }
                                    val day = when { d <= 31 -> d; d <= 59 -> d - 31; else -> d - 59 }
                                    val value = if (d % 3 == 0) (d % 20) + 1 else 0
                                    add(CalendarDay(2026, m, day, value))
                                }
                            })
                            weeks(14)
                            colorScale(CalendarColorScale.BLUE)
                            cellSize(14f)
                        }
                    }
                }
            }
        }
    }

    private fun Float.fmt(): String {
        val i = toInt()
        return if (this == i.toFloat()) "$i" else String.format("%.1f", this)
    }
}
