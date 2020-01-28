/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.simonecangialosi.netgraph

import kotlin.math.min

/**
 * A connected graph that places its nodes in the Cartesian Coordinate System avoiding overlapping as much as possible.
 *
 * @property nodes the nodes that compose this graph
 */
class Graph(val nodes: Set<Node>) {

  companion object {

    /**
     * Build graphs from a map of links.
     * Links are intended to be bi-directional, so it is not necessary to put all the nodes as keys.
     *
     * @param links map each node ID to the set of linked nodes IDs
     *
     * @return a list of disconnected graphs, whose nodes are linked as described in the given [links] map
     */
    fun byLinks(links: Map<Int, Set<Int>>): List<Graph> {

      val groups: MutableMap<Node, Set<Node>> = mutableMapOf()
      val nodes: Set<Node> = buildNodes(links)

      nodes.forEach { node ->

        val group: Set<Node> = node.withAllDescendants()

        groups.getOrPut(group.minBy { it.id }!!) { group }
      }

      return groups.values.map { Graph(it) }.also { position(it) }
    }

    /**
     * Build nodes from a map of links.
     * Links are intended to be bi-directional, so it is not necessary to put all the nodes as keys.
     *
     * @param links map each node ID to the set of linked nodes IDs
     *
     * @return a set of nodes linked as described in the given [links] map
     */
    private fun buildNodes(links: Map<Int, Set<Int>>): Set<Node> {

      val nodes: MutableMap<Int, Node> = mutableMapOf()

      links.forEach { (nodeId, directIds) ->

        nodes.getOrPut(nodeId) { Node(nodeId) }

        directIds.forEach { nodes.getOrPut(it) { Node(it) } }
      }

      links.forEach { (nodeId, directIds) ->

        val node: Node = nodes.getValue(nodeId)

        directIds.forEach {

          val directNode: Node = nodes.getValue(it)

          node.children.add(directNode)
          directNode.children.add(node)
        }
      }

      return nodes.values.toSet()
    }

    /**
     * Set the position of the given list of graphs so that there is no overlapping.
     * Starting from the graph with more nodes, in the center of axis, all the graphs are placed in the vertices of
     * concentric hexagons.
     *
     * @param graphs a list of graphs
     */
    private fun position(graphs: List<Graph>) {

      val groupSize = 6
      val sortedGraphs: List<Graph> = graphs.sortedByDescending { it.radius }
      val numOfGroups: Int = Math.ceil((sortedGraphs.size - 1).toDouble() / groupSize).toInt() // except the first graph
      var radius: Double = sortedGraphs.first().radius

      (0 until numOfGroups).forEach { k ->

        val start: Int = 1 + k * groupSize
        val end: Int = min(sortedGraphs.size, start + groupSize)
        val group: List<Graph> = sortedGraphs.subList(start, end)

        var angle = 0.0
        radius += group.first().radius

        group.forEach { graph ->
          graph.moveTo(Coords.byPolar(angle = angle, distance = radius))
          angle += 60.0
        }

        radius += group.first().radius
      }
    }
  }

  /**
   * The max distance between the [center] and the coordinates of a node of the graph.
   */
  val radius: Double

  /**
   * The center of the graph.
   * It corresponds with the coordinates of the node with max edges.
   */
  var center: Coords = Coords(0.0, 0.0)
    private set

  /**
   * Set the nodes coordinates.
   */
  init {

    require(this.nodes.isNotEmpty())

    val sortedNodes: Sequence<Node> = this.nodes.asSequence().sortedByDescending { it.children.size }

    sortedNodes.forEach { it.setBackPointers() }

    sortedNodes.first().calculateCoords() // recursively

    this.radius = sortedNodes.map { it.coords.length }.max()!!
  }

  /**
   * Translate the whole graph so that its center corresponds with given coordinates.
   *
   * @param coords the destination of the graph
   */
  fun moveTo(coords: Coords) {

    val delta: Coords = coords - this.center

    this.nodes.forEach { it.coords += delta }

    this.center = coords
  }
}
