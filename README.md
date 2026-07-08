# KuiklyChartView

Kuikly 跨平台图表组件，支持折线图（LineChart）和柱状图（BarChart），适配 Android / iOS / 鸿蒙三端。

## 功能特性

- **LineChart（折线图）**：支持多系列数据、区域填充、数据点标注、自定义线宽和点半径
- **BarChart（柱状图）**：支持分组多系列、圆角矩形、数值标签
- **通用能力**：网格线、坐标轴标签、自定义颜色、自适应布局
- 纯 Kuikly DSL 实现，无原生平台依赖，开箱即用

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

## API 文档

### 数据模型

```kotlin
data class ChartDataPoint(val label: String, val value: Float)

data class ChartSeries(
    val name: String,
    val points: List<ChartDataPoint>,
    val color: Color,
)
```

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
        lineWidth(2.5f)
        showDots(true)
        dotRadius(4f)
        showGrid(true)
        gridLineCount(4)
    }
}
```

### BarChart（柱状图）

```kotlin
BarChart {
    attr {
        size(Float.NaN, 220f)
        data(
            ChartSeries(
                name = "iOS",
                points = listOf(
                    ChartDataPoint("Q1", 45f),
                    ChartDataPoint("Q2", 62f),
                ),
                color = Color(0x007AFF),
            ),
            ChartSeries(
                name = "Android",
                points = listOf(
                    ChartDataPoint("Q1", 38f),
                    ChartDataPoint("Q2", 55f),
                ),
                color = Color(0x34C759),
            )
        )
        cornerRadius(4f)
        barSpacing(0.25f)
        showValueLabels(true)
    }
}
```

### 通用属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `showGrid` | Boolean | true | 是否显示网格线 |
| `showAxisLabels` | Boolean | true | 是否显示坐标轴标签 |
| `axisColor` | Color | 灰色 | 坐标轴颜色 |
| `gridColor` | Color | 浅灰 | 网格线颜色 |
| `labelFontSize` | Float | 11f | 标签字号 |
| `gridLineCount` | Int | 4 | 网格线数量（2~10）|

## 示例

参见 `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/ChartDemoPage.kt`。

## 相关资源

- [Kuikly 官方文档](https://kuikly.tds.qq.com/)
- [KuiklyChatUI 参考实现](https://github.com/Kuikly-contrib/KuiklyChatUI)
