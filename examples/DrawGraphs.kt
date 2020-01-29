/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

import com.simonecangialosi.netgraph.Graph
import com.simonecangialosi.netgraph.Node
import helpers.Plot
import java.awt.Color
import kotlin.math.abs

/**
 * Create graphs from links between nodes, then print their coordinates and plot them into a PNG image.
 */
fun main() {

  val links = mapOf(
    1 to setOf(2, 3, 4, 5, 6, 7),
    2 to setOf(8, 9, 10),
    10 to setOf(7),
    11 to setOf(12, 13),
    13 to setOf(14),
    15 to setOf(16, 17, 18),
    17 to setOf(18),
    18 to setOf(19),
    19 to setOf(15)
  )

  val graphs: List<Graph> = Graph.byLinks(links)
  val nodes: Sequence<Node> = graphs.asSequence().flatMap { it.nodes.asSequence() }

  val plotSeries: List<Plot.Data> = getPlotLines(links = links, nodesMap = nodes.associateBy { it.id }) +
    nodes.map { Plot.data().xy(it.coords.x, it.coords.y) }

  drawGraphs(plotSeries)

  graphs.forEachIndexed { i, graph ->

    println("\nGraph #${i+1}")

    graph.nodes.sortedBy { it.id }.forEach {
      println("${it.id}\t%.1f\t%.1f".format(it.coords.x, it.coords.y))
    }
  }
}

/**
 * @param links the links between nodes
 * @param nodesMap the nodes associated by ID
 *
 * @return the list of plot series defining the lines that link the nodes
 */
private fun getPlotLines(links: Map<Int, Set<Int>>, nodesMap: Map<Int, Node>): List<Plot.Data> =

  links.flatMap { (from, toList) ->

    val fromNode = nodesMap.getValue(from)

    toList.map { to ->

      val toNode = nodesMap.getValue(to)

      Plot.data().xy(fromNode.coords.x, fromNode.coords.y).xy(toNode.coords.x, toNode.coords.y)
    }
  }

/**
 * Draw the graphs into a Cartesian Coordinates System and save it as a PNG image.
 *
 * @param plotSeries the plot series of points and lines
 */
private fun drawGraphs(plotSeries: List<Plot.Data>) {

  val maxRadius: Double =
    plotSeries.map { (0 until it.size()).map { i -> maxOf(abs(it.x(i)), abs(it.y(i))) }.max()!! }.max()!! + 50.0

  val plot = Plot.plot(null)
    .xAxis("x", Plot.axisOpts().range(-maxRadius, maxRadius))
    .yAxis("y", Plot.axisOpts().range(-maxRadius, maxRadius))

  val seriesOpt = Plot.seriesOpts()
    .marker(Plot.Marker.CIRCLE)
    .markerColor(Color.BLACK)
    .markerSize(10)
    .color(Color.BLACK)
    .lineWidth(1)

  plotSeries.forEachIndexed { i, dataSeries -> plot.series(i.toString(), dataSeries, seriesOpt) }

  plot.save("graphs", "png")
}
