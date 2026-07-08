package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chart.BarChart
import com.tencent.kuiklybase.chart.ChartDataPoint
import com.tencent.kuiklybase.chart.ChartSeries
import com.tencent.kuiklybase.chart.LineChart

@Page("ChartDemoPage")
internal class ChartDemoPage : com.tencent.kuikly.core.pager.Pager() {

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
                    }
                }
            }
        }
    }
}
