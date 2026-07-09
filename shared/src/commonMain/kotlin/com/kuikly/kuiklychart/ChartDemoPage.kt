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
import com.tencent.kuiklybase.chart.ScatterChart
import com.tencent.kuiklybase.chart.ScatterPoint
import com.tencent.kuiklybase.chart.ScatterSeries

@Page("ChartDemoPage")
internal class ChartDemoPage : com.tencent.kuikly.core.pager.Pager() {

    private var lastClick by observable("")
    private var lastPieClick by observable("")
    private var lastScatterClick by observable("")
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
            }
        }
    }

    private fun Float.fmt(): String {
        val i = toInt()
        return if (this == i.toFloat()) "$i" else String.format("%.1f", this)
    }
}
