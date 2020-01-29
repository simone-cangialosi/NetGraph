/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package helpers

import javax.imageio.ImageIO
import java.awt.*
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Simple implementation of plot. Minimal features, no dependencies besides standard libraries.
 * Options are self-descriptive, see also samples.
 *
 * @author Yuriy Guskov
 */
@Suppress("UNUSED")
internal class Plot private constructor(opts: PlotOptions?) {

  private var opts = PlotOptions()

  private val boundRect: Rectangle
  private val plotArea: PlotArea
  private val xAxes = HashMap<String, Axis>(3)
  private val yAxes = HashMap<String, Axis>(3)
  private val dataSeriesMap = LinkedHashMap<String, DataSeries>(5)

  enum class Line {
    NONE, SOLID, DASHED
  }

  enum class Marker {
    NONE, CIRCLE, SQUARE, DIAMOND, COLUMN, BAR
  }

  enum class AxisFormat {
    NUMBER, NUMBER_KGM, NUMBER_INT, TIME_HM, TIME_HMS, DATE, DATETIME_HM, DATETIME_HMS
  }

  enum class LegendFormat {
    NONE, TOP, RIGHT, BOTTOM
  }

  internal enum class HorizAlign {
    LEFT, CENTER, RIGHT
  }

  internal enum class VertAlign {
    TOP, CENTER, BOTTOM
  }

  class PlotOptions {

    internal var title = ""
    internal var width = 800
    internal var height = 600
    internal var backgroundColor = Color.WHITE
    internal var foregroundColor = Color.BLACK
    internal var titleFont = Font("Arial", Font.BOLD, 16)
    internal var padding = 10 // padding for the entire image
    internal var plotPadding = 5 // padding for plot area (to have min and max values padded)
    internal var labelPadding = 10
    internal val defaultLegendSignSize = 10
    internal var legendSignSize = 10
    internal var grids = Point(10, 10) // grid lines by x and y
    internal var gridColor = Color.GRAY
    internal var gridStroke: Stroke = BasicStroke(1.0f, BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER, 10.0f, floatArrayOf(5.0f), 0.0f)
    internal var tickSize = 5
    internal var labelFont = Font("Arial", 0, 12)
    internal var legend = LegendFormat.NONE

    fun title(title: String): PlotOptions {
      this.title = title
      return this
    }

    fun width(width: Int): PlotOptions {
      this.width = width
      return this
    }

    fun height(height: Int): PlotOptions {
      this.height = height
      return this
    }

    fun bgColor(color: Color): PlotOptions {
      this.backgroundColor = color
      return this
    }

    fun fgColor(color: Color): PlotOptions {
      this.foregroundColor = color
      return this
    }

    fun titleFont(font: Font): PlotOptions {
      this.titleFont = font
      return this
    }

    fun padding(padding: Int): PlotOptions {
      this.padding = padding
      return this
    }

    fun plotPadding(padding: Int): PlotOptions {
      this.plotPadding = padding
      return this
    }

    fun labelPadding(padding: Int): PlotOptions {
      this.labelPadding = padding
      return this
    }

    fun labelFont(font: Font): PlotOptions {
      this.labelFont = font
      return this
    }

    fun grids(byX: Int, byY: Int): PlotOptions {
      this.grids = Point(byX, byY)
      return this
    }

    fun gridColor(color: Color): PlotOptions {
      this.gridColor = color
      return this
    }

    fun gridStroke(stroke: Stroke): PlotOptions {
      this.gridStroke = stroke
      return this
    }

    fun tickSize(value: Int): PlotOptions {
      this.tickSize = value
      return this
    }

    fun legend(legend: LegendFormat): PlotOptions {
      this.legend = legend
      return this
    }

  }

  init {
    if (opts != null)
      this.opts = opts
    boundRect = Rectangle(0, 0, this.opts.width, this.opts.height)
    plotArea = PlotArea()
  }

  fun opts(): PlotOptions {
    return opts
  }

  fun xAxis(name: String, opts: AxisOptions): Plot {
    xAxes[name] = Axis(name, opts)
    return this
  }

  fun yAxis(name: String, opts: AxisOptions): Plot {
    yAxes[name] = Axis(name, opts)
    return this
  }

  fun series(name: String, data: Data, opts: DataSeriesOptions?): Plot {
    var series: DataSeries? = dataSeriesMap[name]
    opts?.setPlot(this)
    if (series == null) {
      series = DataSeries(name, data, opts)
      dataSeriesMap[name] = series
    } else {
      series.data = data
      if (opts != null)
        series.opts = opts
    }
    return this
  }

  fun series(name: String, opts: DataSeriesOptions?): Plot {
    val series = dataSeriesMap[name]
    opts?.setPlot(this)
    if (series != null && opts != null)
      series.opts = opts
    return this
  }

  private fun calc(g: Graphics2D) {
    plotArea.calc(g)
  }

  private fun clear() {
    plotArea.clear()
    for (series in dataSeriesMap.values)
      series.clear()
  }

  private fun draw(): BufferedImage {
    val image = BufferedImage(opts.width, opts.height, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    try {
      calc(g)
      drawBackground(g)
      plotArea.draw(g)
      for (series in dataSeriesMap.values)
        series.draw(g)
      return image
    } finally {
      g.dispose()
    }
  }

  private fun drawBackground(g: Graphics2D) {
    g.color = opts.backgroundColor
    g.fillRect(0, 0, opts.width, opts.height)
  }

  @Throws(IOException::class)
  fun save(fileName: String, type: String) {
    clear()
    val bi = draw()
    val outputFile = File("$fileName.$type")
    ImageIO.write(bi, type, outputFile)
  }

  private inner class Legend {
    internal var rect: Rectangle? = null
    internal var labelRect: Rectangle2D? = null
    var entryWidth: Int = 0
    var entryWidthPadded: Int = 0
    var entryCount: Int = 0
    var xCount: Int = 0
    var yCount: Int = 0
  }

  private inner class PlotArea {

    internal val plotBorderRect = Rectangle() // boundRect | labels/legend | plotBorderRect | plotPadding | plotRect/clipRect
    internal val plotRect = Rectangle()
    internal val plotClipRect = Rectangle()
    internal val legend = Legend()

    internal val xPlotRange = Range(0.0, 0.0)
    internal val yPlotRange = Range(0.0, 0.0)

    init {
      clear()
    }

    internal fun clear() {
      plotBorderRect.bounds = boundRect
      plotRectChanged()
    }

    internal fun offset(dx: Int, dy: Int, dw: Int, dh: Int) {
      plotBorderRect.translate(dx, dy)
      plotBorderRect.setSize(plotBorderRect.width - dx - dw, plotBorderRect.height - dy - dh)
      plotRectChanged()
    }

    internal fun plotRectChanged() {
      plotRect.setBounds(plotBorderRect.x + opts.plotPadding, plotBorderRect.y + opts.plotPadding,
        plotBorderRect.width - opts.plotPadding * 2, plotBorderRect.height - opts.plotPadding * 2)
      xPlotRange.setMin(plotRect.getX())
      xPlotRange.setMax(plotRect.getX() + plotRect.getWidth())
      yPlotRange.setMin(plotRect.getY())
      yPlotRange.setMax(plotRect.getY() + plotRect.getHeight())

      plotClipRect.setBounds(plotBorderRect.x + 1, plotBorderRect.y + 1, plotBorderRect.width - 1, plotBorderRect.height - 1)
    }

    internal fun calc(g: Graphics2D) {
      calcAxes()
      calcRange(true)
      calcRange(false)
      calcAxisLabels(g, true)
      calcAxisLabels(g, false)
      g.font = opts.titleFont
      val titleRect = g.fontMetrics.getStringBounds(opts.title, g)
      g.font = opts.labelFont
      var xAxesHeight = 0
      var xAxesHalfWidth = 0
      for ((_, xAxis) in xAxes) {
        xAxesHeight += toInt(xAxis.labelRect!!.height) + opts.labelPadding * 2
        if (xAxis.labelRect!!.width > xAxesHalfWidth)
          xAxesHalfWidth = toInt(xAxis.labelRect!!.width)
      }
      var yAxesWidth = 0
      for ((_, value) in yAxes)
        yAxesWidth += toInt(value.labelRect!!.width) + opts.labelPadding * 2
      val dx = opts.padding + yAxesWidth
      var dy = opts.padding + toInt(titleRect.height + opts.labelPadding)
      var dw = opts.padding
      if (opts.legend != LegendFormat.RIGHT)
        dw += xAxesHalfWidth // half of label goes beyond a plot in right bottom corner
      var dh = opts.padding + xAxesHeight
      // offset for legend
      val temp = Rectangle(plotBorderRect) // save plotRect
      offset(dx, dy, dw, dh)
      calcLegend(g) // use plotRect
      plotBorderRect.bounds = temp // restore plotRect
      when (opts.legend) {
        LegendFormat.TOP -> dy += legend.rect!!.height + opts.labelPadding
        LegendFormat.RIGHT -> dw += legend.rect!!.width + opts.labelPadding
        LegendFormat.BOTTOM -> dh += legend.rect!!.height
        LegendFormat.NONE -> Unit
      }
      offset(dx, dy, dw, dh)
    }

    internal fun draw(g: Graphics2D) {
      drawPlotArea(g)
      drawGrid(g)
      drawAxes(g)
      drawLegend(g)
      // if check needed that content is inside padding
      //g.setColor(Color.GRAY);
      //g.drawRect(boundRect.x + opts.padding, boundRect.y + opts.padding, boundRect.width - opts.padding * 2, boundRect.height - opts.padding * 2);
    }

    internal fun drawPlotArea(g: Graphics2D) {
      g.color = opts.foregroundColor
      g.drawRect(plotBorderRect.x, plotBorderRect.y, plotBorderRect.width, plotBorderRect.height)
      g.font = opts.titleFont
      drawLabel(g, opts.title, plotBorderRect.x + toInt(plotBorderRect.getWidth() / 2), opts.padding, HorizAlign.CENTER, VertAlign.TOP)
    }

    internal fun drawGrid(g: Graphics2D) {
      val stroke = g.stroke
      g.stroke = opts.gridStroke
      g.color = opts.gridColor

      val leftX = plotBorderRect.x + 1
      val rightX = plotBorderRect.x + plotBorderRect.width - 1
      val topY = plotBorderRect.y + 1
      val bottomY = plotBorderRect.y + plotBorderRect.height - 1

      for (i in 0 until opts.grids.x + 1) {
        val x = toInt(plotRect.x + plotRect.getWidth() / opts.grids.x * i)
        g.drawLine(x, topY, x, bottomY)
      }

      for (i in 0 until opts.grids.y + 1) {
        val y = toInt(plotRect.y + plotRect.getHeight() / opts.grids.y * i)
        g.drawLine(leftX, y, rightX, y)
      }

      g.stroke = stroke
    }

    internal fun calcAxes() {
      val xAxis = if (xAxes.isEmpty()) Axis("", null) else xAxes.values.iterator().next()
      val yAxis = if (yAxes.isEmpty()) Axis("", null) else yAxes.values.iterator().next()
      var xCount = 0
      var yCount = 0
      for (series in dataSeriesMap.values) {
        if (series.opts.xAxis == null) {
          series.opts.xAxis = xAxis
          xCount++
        }
        if (series.opts.yAxis == null) {
          series.opts.yAxis = yAxis
          yCount++
        }
        series.addAxesToName()
      }
      if (xAxes.isEmpty() && xCount > 0)
        xAxes["x"] = xAxis
      if (yAxes.isEmpty() && yCount > 0)
        yAxes["y"] = yAxis
    }

    internal fun calcAxisLabels(g: Graphics2D, isX: Boolean) {
      val fm = g.fontMetrics
      var rect: Rectangle2D?
      var w = 0.0
      var h = 0.0
      val axes = if (isX) xAxes else yAxes
      val grids = if (isX) opts.grids.x else opts.grids.y
      for ((_, axis) in axes) {
        axis.labels = arrayOfNulls(grids + 1)
        axis.labelRect = fm.getStringBounds("", g)
        val xStep = axis.opts.range!!.diff / grids
        for (j in 0 until grids + 1) {
          axis.labels!![j] = formatDouble(axis.opts.range!!.min + xStep * j, axis.opts.format)
          rect = fm.getStringBounds(axis.labels!![j], g)
          if (rect!!.width > w)
            w = rect.width
          if (rect.height > h)
            h = rect.height
        }
        axis.labelRect!!.setRect(0.0, 0.0, w, h)
      }
    }

    internal fun calcRange(isX: Boolean) {
      for (series in dataSeriesMap.values) {
        val axis = if (isX) series.opts.xAxis else series.opts.yAxis
        if (axis!!.opts.dynamicRange) {
          val range = if (isX) series.xRange() else series.yRange()
          if (axis.opts.range == null)
            axis.opts.range = range
          else {
            if (range.max > axis.opts.range!!.max)
              axis.opts.range!!.setMax(range.max)
            if (range.min < axis.opts.range!!.min)
              axis.opts.range!!.setMin(range.min)
          }
        }
      }
      val axes = if (isX) xAxes else yAxes
      val it = axes.values.iterator()
      while (it.hasNext()) {
        val axis = it.next()
        if (axis.opts.range == null)
          it.remove()
      }
    }

    internal fun drawAxes(g: Graphics2D) {
      g.font = opts.labelFont
      g.color = opts.foregroundColor

      val leftXPadded = plotBorderRect.x - opts.labelPadding
      val rightX = plotBorderRect.x + plotBorderRect.width
      val bottomY = plotBorderRect.y + plotBorderRect.height
      val bottomYPadded = bottomY + opts.labelPadding

      var axisOffset = 0
      for ((_, axis) in xAxes) {
        val xStep = axis.opts.range!!.diff / opts.grids.x

        drawLabel(g, axis.name, rightX + opts.labelPadding, bottomY + axisOffset, HorizAlign.LEFT, VertAlign.CENTER)
        g.drawLine(plotRect.x, bottomY + axisOffset, plotRect.x + plotRect.width, bottomY + axisOffset)

        for (j in 0 until opts.grids.x + 1) {
          val x = toInt(plotRect.x + plotRect.getWidth() / opts.grids.x * j)
          drawLabel(g, formatDouble(axis.opts.range!!.min + xStep * j, axis.opts.format), x, bottomYPadded + axisOffset, HorizAlign.CENTER, VertAlign.TOP)
          g.drawLine(x, bottomY + axisOffset, x, bottomY + opts.tickSize + axisOffset)
        }
        axisOffset += toInt(axis.labelRect!!.height + opts.labelPadding * 2)
      }

      axisOffset = 0
      for ((_, axis) in yAxes) {
        val yStep = axis.opts.range!!.diff / opts.grids.y

        drawLabel(g, axis.name, leftXPadded - axisOffset, plotBorderRect.y - toInt(axis.labelRect!!.height + opts.labelPadding), HorizAlign.RIGHT, VertAlign.CENTER)
        g.drawLine(plotBorderRect.x - axisOffset, plotRect.y + plotRect.height, plotBorderRect.x - axisOffset, plotRect.y)

        for (j in 0 until opts.grids.y + 1) {
          val y = toInt(plotRect.y + plotRect.getHeight() / opts.grids.y * j)
          drawLabel(g, formatDouble(axis.opts.range!!.max - yStep * j, axis.opts.format), leftXPadded - axisOffset, y, HorizAlign.RIGHT, VertAlign.CENTER)
          g.drawLine(plotBorderRect.x - axisOffset, y, plotBorderRect.x - opts.tickSize - axisOffset, y)
        }
        axisOffset += toInt(axis.labelRect!!.width + opts.labelPadding * 2)
      }
    }

    internal fun calcLegend(g: Graphics2D) {
      legend.rect = Rectangle(0, 0)
      if (opts.legend == LegendFormat.NONE)
        return
      val size = dataSeriesMap.size
      if (size == 0)
        return

      val fm = g.fontMetrics
      val it = dataSeriesMap.values.iterator()
      legend.labelRect = fm.getStringBounds(it.next().nameWithAxes, g)
      var legendSignSize = opts.defaultLegendSignSize
      while (it.hasNext()) {
        val series = it.next()
        val rect = fm.getStringBounds(series.nameWithAxes, g)
        if (rect.width > legend.labelRect!!.width)
          legend.labelRect!!.setRect(0.0, 0.0, rect.width, legend.labelRect!!.height)
        if (rect.height > legend.labelRect!!.height)
          legend.labelRect!!.setRect(0.0, 0.0, legend.labelRect!!.width, rect.height)
        when (series.opts.marker) {
          Marker.CIRCLE, Marker.SQUARE -> if (series.opts.markerSize + opts.defaultLegendSignSize > legendSignSize)
            legendSignSize = series.opts.markerSize + opts.defaultLegendSignSize
          Marker.DIAMOND -> if (series.diagMarkerSize + opts.defaultLegendSignSize > legendSignSize)
            legendSignSize = series.diagMarkerSize + opts.defaultLegendSignSize
          else -> Unit
        }
      }
      opts.legendSignSize = legendSignSize

      legend.entryWidth = legendSignSize + opts.labelPadding + toInt(legend.labelRect!!.width)
      legend.entryWidthPadded = legend.entryWidth + opts.labelPadding

      when (opts.legend) {
        LegendFormat.TOP, LegendFormat.BOTTOM -> {
          legend.entryCount = Math.floor((plotBorderRect.width - opts.labelPadding).toDouble() / legend.entryWidthPadded).toInt()
          legend.xCount = if (size <= legend.entryCount) size else legend.entryCount
          legend.yCount = if (size <= legend.entryCount) 1 else Math.ceil(size.toDouble() / legend.entryCount).toInt()
          legend.rect!!.width = opts.labelPadding + legend.xCount * legend.entryWidthPadded
          legend.rect!!.height = opts.labelPadding + toInt(legend.yCount * (opts.labelPadding + legend.labelRect!!.height))
          legend.rect!!.x = plotBorderRect.x + (plotBorderRect.width - legend.rect!!.width) / 2
          if (opts.legend == LegendFormat.TOP)
            legend.rect!!.y = plotBorderRect.y
          else
            legend.rect!!.y = boundRect.height - legend.rect!!.height - opts.padding
        }
        LegendFormat.RIGHT -> {
          legend.rect!!.width = opts.labelPadding * 3 + legendSignSize + toInt(legend.labelRect!!.width)
          legend.rect!!.height = opts.labelPadding * (size + 1) + toInt(legend.labelRect!!.height * size)
          legend.rect!!.x = boundRect.width - legend.rect!!.width - opts.padding
          legend.rect!!.y = plotBorderRect.y + plotBorderRect.height / 2 - legend.rect!!.height / 2
        }
        LegendFormat.NONE -> Unit
      }
    }

    internal fun drawLegend(g: Graphics2D) {
      if (opts.legend == LegendFormat.NONE)
        return

      g.drawRect(legend.rect!!.x, legend.rect!!.y, legend.rect!!.width, legend.rect!!.height)
      val labelHeight = toInt(legend.labelRect!!.height)
      var x = legend.rect!!.x + opts.labelPadding
      var y = legend.rect!!.y + opts.labelPadding + labelHeight / 2

      when (opts.legend) {
        LegendFormat.TOP, LegendFormat.BOTTOM -> {
          dataSeriesMap.values.forEachIndexed { i, series ->
            drawLegendEntry(g, series, x, y)
            x += legend.entryWidthPadded
            if ((i + 1) % legend.xCount == 0) {
              x = legend.rect!!.x + opts.labelPadding
              y += opts.labelPadding + labelHeight
            }
          }
        }
        LegendFormat.RIGHT -> for (series in dataSeriesMap.values) {
          drawLegendEntry(g, series, x, y)
          y += opts.labelPadding + labelHeight
        }
        LegendFormat.NONE -> Unit
      }
    }

    internal fun drawLegendEntry(g: Graphics2D, series: DataSeries, x: Int, y: Int) {
      series.fillArea(g, x, y, x + opts.legendSignSize, y, y + opts.legendSignSize / 2)
      series.drawLine(g, x, y, x + opts.legendSignSize, y)
      series.drawMarker(g, x + opts.legendSignSize / 2, y, x, y + opts.legendSignSize / 2)
      g.color = opts.foregroundColor
      drawLabel(g, series.nameWithAxes, x + opts.legendSignSize + opts.labelPadding, y, HorizAlign.LEFT, VertAlign.CENTER)
    }

  }

  class Range {

    internal var min: Double = 0.toDouble()
    internal var max: Double = 0.toDouble()
    internal var diff: Double = 0.toDouble()

    constructor(min: Double, max: Double) {
      this.min = min
      this.max = max
      this.diff = max - min
    }

    constructor(range: Range) {
      this.min = range.min
      this.max = range.max
      this.diff = max - min
    }

    fun setMin(min: Double) {
      this.min = min
      this.diff = max - min
    }

    fun setMax(max: Double) {
      this.max = max
      this.diff = max - min
    }

    override fun toString(): String {
      return "Range [min=$min, max=$max]"
    }

  }

  class AxisOptions {

    internal var format = AxisFormat.NUMBER
    internal var dynamicRange = true
    internal var range: Range? = null

    fun format(format: AxisFormat): AxisOptions {
      this.format = format
      return this
    }

    fun range(min: Double, max: Double): AxisOptions {
      this.range = Range(min, max)
      this.dynamicRange = false
      return this
    }

  }

  internal inner class Axis(internal val name: String, opts: AxisOptions?) {
    internal var opts = AxisOptions()
    internal var labelRect: Rectangle2D? = null
    internal var labels: Array<String?>? = null

    init {
      if (opts != null)
        this.opts = opts
    }

    override fun toString(): String {
      return "Axis [name=$name, opts=$opts]"
    }

  }

  class DataSeriesOptions internal constructor() {

    internal var seriesColor = Color.BLUE
    internal var line = Line.SOLID
    internal var lineWidth = 2
    internal var lineDash = floatArrayOf(3.0f, 3.0f)
    internal var marker = Marker.NONE
    internal var markerSize = 10
    internal var markerColor = Color.WHITE
    internal var areaColor: Color? = null
    private var xAxisName: String? = null
    private var yAxisName: String? = null
    internal var xAxis: Axis? = null
    internal var yAxis: Axis? = null

    fun color(seriesColor: Color): DataSeriesOptions {
      this.seriesColor = seriesColor
      return this
    }

    fun line(line: Line): DataSeriesOptions {
      this.line = line
      return this
    }

    fun lineWidth(width: Int): DataSeriesOptions {
      this.lineWidth = width
      return this
    }

    fun lineDash(dash: FloatArray): DataSeriesOptions {
      this.lineDash = dash
      return this
    }

    fun marker(marker: Marker): DataSeriesOptions {
      this.marker = marker
      return this
    }

    fun markerSize(markerSize: Int): DataSeriesOptions {
      this.markerSize = markerSize
      return this
    }

    fun markerColor(color: Color): DataSeriesOptions {
      this.markerColor = color
      return this
    }

    fun areaColor(color: Color): DataSeriesOptions {
      this.areaColor = color
      return this
    }

    fun xAxis(name: String): DataSeriesOptions {
      this.xAxisName = name
      return this
    }

    fun yAxis(name: String): DataSeriesOptions {
      this.yAxisName = name
      return this
    }

    internal fun setPlot(plot: Plot?) {
      if (plot != null)
        this.xAxis = plot.xAxes[xAxisName]
      if (plot != null)
        this.yAxis = plot.yAxes[yAxisName]
    }

  }

  class Data internal constructor() {

    private var x1: DoubleArray? = null
    private var y1: DoubleArray? = null
    private var x2: MutableList<Double>? = null
    private var y2: MutableList<Double>? = null

    fun xy(x: DoubleArray, y: DoubleArray): Data {
      this.x1 = x
      this.y1 = y
      return this
    }

    fun xy(x: Double, y: Double): Data {
      if (this.x2 == null || this.y2 == null) {
        this.x2 = ArrayList(10)
        this.y2 = ArrayList(10)
      }
      x2!!.add(x)
      y2!!.add(y)
      return this
    }

    fun xy(x: MutableList<Double>, y: MutableList<Double>): Data {
      this.x2 = x
      this.y2 = y
      return this
    }

    fun size(): Int {
      if (x1 != null)
        return x1!!.size
      return if (x2 != null) x2!!.size else 0
    }

    fun x(i: Int): Double {
      if (x1 != null)
        return x1!![i]
      return if (x2 != null) x2!![i] else 0.0
    }

    fun y(i: Int): Double {
      if (y1 != null)
        return y1!![i]
      return if (y2 != null) y2!![i] else 0.0
    }

  }

  inner class DataSeries(private val name: String, data: Data, opts: DataSeriesOptions?) {
    internal var nameWithAxes: String? = null
    internal var opts = DataSeriesOptions()
    internal var data: Data? = null

    internal val diagMarkerSize: Int
      get() = Math.round(Math.sqrt((2 * opts.markerSize * opts.markerSize).toDouble())).toInt()

    init {
      if (opts != null)
        this.opts = opts
      this.data = data
      if (this.data == null)
        this.data = data()
    }

    fun clear() {}

    internal fun addAxesToName() {
      this.nameWithAxes = this.name + " (" + opts.yAxis!!.name + "/" + opts.xAxis!!.name + ")"
    }

    internal fun xRange(): Range {
      var range = Range(0.0, 0.0)
      if (data != null && data!!.size() > 0) {
        range = Range(data!!.x(0), data!!.x(0))
        for (i in 1 until data!!.size()) {
          if (data!!.x(i) > range.max)
            range.setMax(data!!.x(i))
          if (data!!.x(i) < range.min)
            range.setMin(data!!.x(i))
        }
      }
      return range
    }

    internal fun yRange(): Range {
      var range = Range(0.0, 0.0)
      if (data != null && data!!.size() > 0) {
        range = Range(data!!.y(0), data!!.y(0))
        for (i in 1 until data!!.size()) {
          if (data!!.y(i) > range.max)
            range.setMax(data!!.y(i))
          if (data!!.y(i) < range.min)
            range.setMin(data!!.y(i))
        }
      }
      return range
    }

    internal fun draw(g: Graphics2D) {
      g.clip = plotArea.plotClipRect
      if (data != null) {
        var x1 = 0.0
        var y1 = 0.0
        val size = data!!.size()
        if (opts.line != Line.NONE)
          for (j in 0 until size) {
            val x2 = x2x(data!!.x(j), opts.xAxis!!.opts.range!!, plotArea.xPlotRange)
            val y2 = y2y(data!!.y(j), opts.yAxis!!.opts.range!!, plotArea.yPlotRange)
            var ix1 = toInt(x1)
            var iy1 = toInt(y1)
            val ix2 = toInt(x2)
            val iy2 = toInt(y2)
            val iy3 = plotArea.plotRect.y + plotArea.plotRect.height
            // special case for the case when only the first point present
            if (size == 1) {
              ix1 = ix2
              iy1 = iy2
            }
            if (j != 0 || size == 1) {
              fillArea(g, ix1, iy1, ix2, iy2, iy3)
              drawLine(g, ix1, iy1, ix2, iy2)
            }
            x1 = x2
            y1 = y2
          }

        val halfMarkerSize = opts.markerSize / 2
        val halfDiagMarkerSize = diagMarkerSize / 2
        g.stroke = BasicStroke(2f)
        if (opts.marker != Marker.NONE)
          for (j in 0 until size) {
            val x2 = x2x(data!!.x(j), opts.xAxis!!.opts.range!!, plotArea.xPlotRange)
            val y2 = y2y(data!!.y(j), opts.yAxis!!.opts.range!!, plotArea.yPlotRange)
            drawMarker(g, halfMarkerSize, halfDiagMarkerSize, x2, y2,
              plotArea.plotRect.x.toDouble(), (plotArea.plotRect.y + plotArea.plotRect.height).toDouble())
          }
      }
    }

    internal fun fillArea(g: Graphics2D, ix1: Int, iy1: Int, ix2: Int, iy2: Int, iy3: Int) {
      if (opts.areaColor != null) {
        g.color = opts.areaColor
        g.fill(Polygon(
          intArrayOf(ix1, ix2, ix2, ix1),
          intArrayOf(iy1, iy2, iy3, iy3),
          4))
        g.color = opts.seriesColor
      }
    }

    internal fun drawLine(g: Graphics2D, ix1: Int, iy1: Int, ix2: Int, iy2: Int) {
      if (opts.line != Line.NONE) {
        g.color = opts.seriesColor
        setStroke(g)
        g.drawLine(ix1, iy1, ix2, iy2)
      }
    }

    private fun setStroke(g: Graphics2D) {
      when (opts.line) {
        Line.SOLID -> g.stroke = BasicStroke(opts.lineWidth.toFloat())
        Line.DASHED -> g.stroke = BasicStroke(opts.lineWidth.toFloat(), BasicStroke.CAP_ROUND,
          BasicStroke.JOIN_ROUND, 10.0f, opts.lineDash, 0.0f)
        Line.NONE -> Unit
      }
    }

    internal fun drawMarker(g: Graphics2D, x2: Int, y2: Int, x3: Int, y3: Int) {
      val halfMarkerSize = opts.markerSize / 2
      val halfDiagMarkerSize = diagMarkerSize / 2
      g.stroke = BasicStroke(2f)
      drawMarker(g, halfMarkerSize, halfDiagMarkerSize, x2.toDouble(), y2.toDouble(), x3.toDouble(), y3.toDouble())
    }

    private fun drawMarker(g: Graphics2D, halfMarkerSize: Int, halfDiagMarkerSize: Int, x2: Double, y2: Double, x3: Double, y3: Double) {
      when (opts.marker) {
        Marker.CIRCLE -> {
          g.color = opts.markerColor
          g.fillOval(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize)
          g.color = opts.seriesColor
          g.drawOval(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize)
        }
        Marker.SQUARE -> {
          g.color = opts.markerColor
          g.fillRect(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize)
          g.color = opts.seriesColor
          g.drawRect(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize)
        }
        Marker.DIAMOND -> {
          val xpts = intArrayOf(toInt(x2), toInt(x2 + halfDiagMarkerSize), toInt(x2), toInt(x2 - halfDiagMarkerSize))
          val ypts = intArrayOf(toInt(y2 - halfDiagMarkerSize), toInt(y2), toInt(y2 + halfDiagMarkerSize), toInt(y2))
          g.color = opts.markerColor
          g.fillPolygon(xpts, ypts, 4)
          g.color = opts.seriesColor
          g.drawPolygon(xpts, ypts, 4)
        }
        Marker.COLUMN -> {
          g.color = opts.markerColor
          g.fillRect(toInt(x2), toInt(y2), opts.markerSize, toInt(y3 - y2))
          g.color = opts.seriesColor
          g.drawRect(toInt(x2), toInt(y2), opts.markerSize, toInt(y3 - y2))
        }
        Marker.BAR -> {
          g.color = opts.markerColor
          g.fillRect(toInt(x3), toInt(y2), toInt(x2 - x3), opts.markerSize)
          g.color = opts.seriesColor
          g.drawRect(toInt(x3), toInt(y2), toInt(x2 - x3), opts.markerSize)
        }
        Marker.NONE -> Unit
      }
    }

  }

  companion object {

    fun plot(opts: PlotOptions?): Plot {
      return Plot(opts)
    }

    fun plotOpts(): PlotOptions {
      return PlotOptions()
    }

    fun axisOpts(): AxisOptions {
      return AxisOptions()
    }

    fun seriesOpts(): DataSeriesOptions {
      return DataSeriesOptions()
    }

    fun data(): Data {
      return Data()
    }

    internal fun drawLabel(g: Graphics2D, s: String?, x: Int, y: Int, hAlign: HorizAlign, vAlign: VertAlign) {
      var xx = x
      var yy = y
      val fm = g.fontMetrics
      val rect = fm.getStringBounds(s, g)

      // by default align by left
      if (hAlign == HorizAlign.RIGHT)
        xx -= rect.width.toInt()
      else if (hAlign == HorizAlign.CENTER)
        xx -= (rect.width / 2).toInt()

      // by default align by bottom
      if (vAlign == VertAlign.TOP)
        yy += rect.height.toInt()
      else if (vAlign == VertAlign.CENTER)
        yy += (rect.height / 2).toInt()

      g.drawString(s, xx, yy)
    }

    fun formatDouble(d: Double, format: AxisFormat): String {
      return when (format) {
        AxisFormat.TIME_HM -> String.format("%tR", Date(d.toLong()))
        AxisFormat.TIME_HMS -> String.format("%tT", Date(d.toLong()))
        AxisFormat.DATE -> String.format("%tF", Date(d.toLong()))
        AxisFormat.DATETIME_HM -> String.format("%tF %1\$tR", Date(d.toLong()))
        AxisFormat.DATETIME_HMS -> String.format("%tF %1\$tT", Date(d.toLong()))
        AxisFormat.NUMBER_KGM -> formatDoubleAsNumber(d, true)
        AxisFormat.NUMBER_INT -> Integer.toString(d.toInt())
        else -> formatDoubleAsNumber(d, false)
      }
    }

    private fun formatDoubleAsNumber(d: Double, useKGM: Boolean): String {
      if (useKGM && d > 1000 && d < 1000000000000L) {
        val numbers = longArrayOf(1000L, 1000000L, 1000000000L)
        val suffix = charArrayOf('K', 'M', 'G')

        var i = 0
        var r = 0.0
        for (number in numbers) {
          r = d / number
          if (r < 1000)
            break
          i++
        }
        if (i == suffix.size)
          i--
        return String.format("%1$,.2f%2\$c", r, suffix[i])
      } else
        return String.format("%1$.3G", d)
    }

    internal fun x2x(x: Double, xr1: Range, xr2: Range): Double {
      return if (xr1.diff == 0.0) xr2.min + xr2.diff / 2 else xr2.min + (x - xr1.min) / xr1.diff * xr2.diff
    }

    // y axis is reverse in Graphics
    internal fun y2y(x: Double, xr1: Range, xr2: Range): Double {
      return if (xr1.diff == 0.0) xr2.min + xr2.diff / 2 else xr2.max - (x - xr1.min) / xr1.diff * xr2.diff
    }

    internal fun toInt(d: Double): Int {
      return Math.round(d).toInt()
    }
  }
}
