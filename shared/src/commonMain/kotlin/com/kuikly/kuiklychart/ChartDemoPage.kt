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
import com.tencent.kuiklybase.chart.LineChart
import com.tencent.kuiklybase.chart.PieChart
import com.tencent.kuiklybase.chart.PieSlice

@Page("ChartDemoPage")
internal class ChartDemoPage : com.tencent.kuikly.core.pager.Pager() {

    private var lastClick by observable("")
    private var lastPieClick by observable("")

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
                        margin(left = 16f, right = 16f, bottom = 24f)
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
            }
        }
    }

    private fun Float.fmt(): String {
        val i = toInt()
        return if (this == i.toFloat()) "$i" else String.format("%.1f", this)
    }
}
