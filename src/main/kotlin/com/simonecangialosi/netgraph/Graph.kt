/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.simonecangialosi.netgraph

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

      return groups.values.map { Graph(it) }.also { position(it); collapse(it) }
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

      val sortedGraphs: List<Graph> = graphs.sortedByDescending { it.radius }
      var radius: Double = sortedGraphs.first().radius

      sortedGraphs.takeLast(sortedGraphs.size - 1).chunked(6).forEach { group ->

        var angle = 0.0

        radius += group.first().radius

        group.forEach { graph ->
          graph.moveTo(Coords.byPolar(angle = angle, distance = radius))
          angle += 60.0
        }

        radius += group.first().radius
      }
    }

    /**
     * Bring graphs next to the axis center, with no overlapping between them.
     * This operation is made by incremental steps, moving each graph by a predefined delta in direction of the axis
     * origin.
     *
     * @param graphs a list of graphs
     */
    private fun collapse(graphs: List<Graph>) {

      val deltaLength = 50.0
      val maxIterations = 200
      var i = 0

      val sortedGraphs: List<Graph> = graphs.sortedBy { it.center.length }
      val graphsPositioned: MutableList<Graph> = mutableListOf(sortedGraphs.first())

      sortedGraphs.takeLast(graphs.size - 1).forEach { graph ->

        val delta = Coords.byPolar(angle = graph.center.angle - 180, distance = deltaLength)

        while (i++ < maxIterations && graphsPositioned.none { graph.overlaps(it) }) {
          graph.moveBy(delta)
        }

        if (i < maxIterations)
          graph.moveBy(Coords(0.0, 0.0) - delta * 4.0) // bring back by 4 steps in case of overlapping
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
   * The edges of the graph as pairs of adjacent nodes.
   * Since the edges are bidirectional, each couple of nodes is present only once.
   */
  val edges: Set<Pair<Node, Node>> = this.nodes.flatMap { from ->
    from.children.map { to ->
      sequenceOf(from, to).minBy { it.id }!! to sequenceOf(from, to).maxBy { it.id }!!
    }
  }.toSet()

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
    this.moveBy(delta = coords - this.center)
  }

  /**
   * Translate the whole graph by a delta defined by polar coordinates.
   *
   * @param angle the polar angle, in degrees
   * @param distance the distance from the axis origin
   */
  fun moveBy(angle: Double, distance: Double) {
    this.moveBy(delta = Coords.byPolar(angle = angle, distance = distance))
  }

  /**
   * Translate the whole graph by a given delta.
   *
   * @param delta the delta of the movement
   */
  fun moveBy(delta: Coords) {

    this.nodes.forEach { it.coords += delta }

    this.center += delta
  }

  /**
   * @param other another graph
   *
   * @return true if there is an overlapping with an edge of the given graph, otherwise false
   */
  fun overlaps(other: Graph): Boolean =
    this.edges.any { edge1 ->
      other.edges.any { edge2 ->
        intersect(s1 = edge1.first.coords to edge1.second.coords, s2 = edge2.first.coords to edge2.second.coords)
      }
    }

  /**
   * @param s1 a segment, as a pair of its ends
   * @param s2 a segment, as a pair of its ends
   *
   * @return true if there is an intersection between the given segments, otherwise false
   */
  private fun intersect(s1: Pair<Coords, Coords>, s2: Pair<Coords, Coords>): Boolean =
    clockwiseRotation(s1.first, s1.second, s2.first) * clockwiseRotation(s1.first, s1.second, s2.second) <= 0 &&
      clockwiseRotation(s2.first, s2.second, s1.first) * clockwiseRotation(s2.first, s2.second, s1.second) <= 0

  /**
   * Analyze the rotation when passing from a first point to a second and then a third.
   *
   * @param p0 the first point
   * @param p1 the second point
   * @param p2 the third point
   *
   * @return 1 if the the rotation is clockwise, -1 if it is counterclockwise, 0 if there is no rotation
   */
  private fun clockwiseRotation(p0: Coords, p1: Coords, p2: Coords): Int {

    val dx1 = p1.x - p0.x
    val dy1 = p1.y - p0.y
    val dx2 = p2.x - p0.x
    val dy2 = p2.y - p0.y

    if (dx1 * dy2 > dy1 * dx2) return +1
    if (dx1 * dy2 < dy1 * dx2) return -1
    if ((dx1 * dx2 < 0) || (dy1 * dy2 < 0)) return -1
    if ((dx1 * dx1 + dy1 * dy1) < (dx2 * dx2 + dy2 * dy2)) return +1

    return 0
  }
}
