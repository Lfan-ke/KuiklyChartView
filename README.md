# KuiklyChartView

基于 [KuiklyUI](https://github.com/Tencent-TDS/KuiklyUI) 跨端框架构建的图表组件库，支持 22 种图表类型，适配 Android、iOS、鸿蒙三端。

## 功能特性

| 类别 | 图表类型 |
|------|----------|
| 基础图表 | LineChart、BarChart、AreaChart、PieChart |
| 统计图表 | RadarChart、ScatterChart、BubbleChart、BoxplotChart |
| 金融图表 | CandlestickChart、WaterfallChart |
| 高级图表 | GaugeChart、HeatmapChart、TreemapChart、FunnelChart |
| 流向图表 | SankeyChart、MixedChart |
| 周期图表 | CalendarHeatmap、NightingaleRoseChart |
| 轻量图表 | SparklineChart、ProgressRingChart |

全部采用纯 Kuikly DSL + Canvas 实现，无原生平台依赖，开箱即用。支持：
- 4 种内置主题（Default / Ocean / Sunset / Forest）
- 点击数据点展示 Tooltip
- 动画与数据更新
- 自定义颜色、网格、坐标轴

## 快速接入

### 1. 添加 Maven 仓库

在项目 `settings.gradle.kts` 中添加：

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent") }
    }
}
```

### 2. 添加依赖

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuiklybase:KuiklyChart:1.0.0-2.0.21")
            }
        }
    }
}
```

## 基础用法

### LineChart（折线图）

```kotlin
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
                ),
                color = Color(0x007AFF),
            )
        )
        fillArea(true)
        showDots(true)
        showGrid(true)
    }
}
```

### BarChart（柱状图）

```kotlin
BarChart {
    attr {
        size(Float.NaN, 220f)
        data(
            ChartSeries("iOS", listOf(ChartDataPoint("Q1", 45f), ChartDataPoint("Q2", 62f)), Color(0x007AFF)),
            ChartSeries("Android", listOf(ChartDataPoint("Q1", 38f), ChartDataPoint("Q2", 55f)), Color(0x34C759))
        )
        cornerRadius(4f)
        showValueLabels(true)
    }
}
```

### PieChart（饼图）

```kotlin
PieChart {
    attr {
        size(Float.NaN, 260f)
        slices(
            PieSlice("产品A", 35f, Color(0x5C7CFA)),
            PieSlice("产品B", 25f, Color(0x2DC7A0)),
            PieSlice("产品C", 20f, Color(0xF59E0B)),
            PieSlice("产品D", 20f, Color(0xEF4444))
        )
        donut(true)
        showLabels(true)
    }
}
```

## 通用属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `showGrid` | Boolean | true | 是否显示网格线 |
| `showAxisLabels` | Boolean | true | 是否显示坐标轴标签 |
| `axisColor` | Color | 灰色 | 坐标轴颜色 |
| `gridColor` | Color | 浅灰 | 网格线颜色 |
| `labelFontSize` | Float | 11f | 标签字号 |
| `gridLineCount` | Int | 4 | 网格线数量（2~10）|
| `theme(ChartTheme)` | ChartTheme | DEFAULT | 主题（DEFAULT/OCEAN/SUNSET/FOREST）|

## 示例

完整示例见 `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/ChartDemoPage.kt`，
在线效果演示见 [GitHub Pages](https://lfan-ke.github.io/KuiklyChartView/)。

## 相关资源

- [Kuikly 官方文档](https://kuikly.tds.qq.com/)
- [KuiklyUI 仓库](https://github.com/Tencent-TDS/KuiklyUI)

## License

[KuiklyUI License](https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE)
