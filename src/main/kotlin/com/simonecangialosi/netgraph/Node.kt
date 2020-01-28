/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.simonecangialosi.netgraph

import java.lang.Math.max
import java.lang.Math.pow

/**
 * A node linked to others with no direction.
 *
 * @property id the node ID, unique within a graph
 */
class Node(val id: Int) {

  companion object {

    /**
     * The default distance between two direct nodes.
     */
    private const val DEFAULT_DISTANCE = 250.0
  }

  /**
   * The Cartesian coordinates of the node.
   */
  var coords: Coords = Coords(0.0, 0.0)
    internal set

  /**
   * The nodes of the graph that are directly connected to this node.
   */
  val children: MutableSet<Node> = mutableSetOf()

  /**
   * Whether the coordinates have been calculated within a graph.
   */
  private var isPut: Boolean = false

  /**
   * The children of this node that are in a circular path that brings to this node.
   */
  private val backPointers: MutableList<Node> = mutableListOf()

  /**
   * The children of this node that are not [backPointers].
   */
  private val nonBackPointers: MutableList<Node> = mutableListOf()

  /**
   * The progress step of the angle between this node and a child, during the positioning process.
   * It is calculated in order to put the children in the vertices of a polygon centered in this node.
   */
  private val angleStep: Double by lazy { 360.0 / this.children.size - (if (this.children.size == 2) 45.0 else 0.0) }

  /**
   * The angle of the next positioning child.
   * It is updated during the positioning process.
   */
  private var nextChildAngle: Double = 0.0

  /**
   * @param visited the nodes that have been already visited (empty when called from the first node)
   *
   * @return all the nodes connected in the graph, including this
   */
  internal fun withAllDescendants(visited: Set<Node> = setOf()): Set<Node> {

    val updatedVisited: MutableSet<Node> = visited.toMutableSet()

    val descendants: List<Node> = (this.children - visited).flatMap {
      it.withAllDescendants(updatedVisited + this).also { descendants -> updatedVisited.addAll(descendants) }
    }

    return descendants.toSet() + this
  }

  /**
   * Split the [children] in back-pointers and not.
   */
  internal fun setBackPointers() {

    this.backPointers.clear()
    this.nonBackPointers.clear()

    this.children.forEach {

      if (it.pointsTo(this))
        this.backPointers.add(it)
      else
        this.nonBackPointers.add(it)
    }
  }

  /**
   * @param node a node in the same graph of this
   * @param visited the nodes that have been already visited
   *
   * @return true if this node has a descendant that is part of a circular path that brings to it, otherwise false
   */
  private fun pointsTo(node: Node, visited: Set<Node> = setOf(this), depth: Int = 0): Boolean {

    val notVisited: Set<Node> = if (depth == 0)
      this.children - visited - node
    else
      this.children - visited

    if (notVisited.any { it == node }) return true

    return notVisited.any { it.pointsTo(node = node, visited = visited + notVisited, depth = depth + 1) }
  }

  /**
   * Calculate the coordinates of this node, taking a linked node as reference.
   * This method is called by a graph and tries to avoid overlapping between edges as much as possible, positioning the
   * nodes in the vertices of concentric polygons, starting from those with more edges and back-pointers.
   *
   * @param father the node already put, directly connected to this, that is the reference for the positioning (or null
   *               if this is the first node of the graph)
   */
  internal fun calculateCoords(father: Node? = null) {

    if (father != null) {

      this.nextChildAngle = father.nextChildAngle + 180.0 + this.angleStep

      if (this.backPointers.any { it != father && it.isPut && (it.coords.angle - father.coords.angle) < 5 })
        father.nextChildAngle += 45
    }

    this.coords = father?.calcNextChildPosition(this) ?: Coords(0.0, 0.0)
    this.isPut = true

    this.putBackPointers()
    this.putNonBackPointers()
  }

  /**
   * Set the coordinates of the back-pointer children.
   */
  private fun putBackPointers() {

    var k = 0

    this.backPointers.forEach { node ->

      if (!node.isPut) {

        node.calculateCoords(father = this)

        this.nextChildAngle += pow(-1.0, k++.toDouble()) * this.angleStep
      }
    }
  }

  /**
   * Set the coordinates of the children nodes that are not back-pointers.
   */
  private fun putNonBackPointers() {

    this.nonBackPointers.forEach { node ->

      if (!node.isPut) {

        node.calculateCoords(father = this)

        val sign: Int = if (this.nextChildAngle >= 0.0) 1 else -1
        this.nextChildAngle += sign * this.angleStep
      }
    }
  }

  /**
   * @param child a node linked directly to this
   *
   * @return the coordinates to assign to the given [child] node
   */
  private fun calcNextChildPosition(child: Node): Coords =
    this.coords.move(angle = this.nextChildAngle, distance = DEFAULT_DISTANCE * max(1, child.children.size - 2))

  /**
   * @return the string representation of this node
   */
  override fun toString(): String = "$id $coords"

  /**
   * @param other an object
   *
   * @return true if this node is equal to the given object, otherwise false
   */
  override fun equals(other: Any?): Boolean = other is Node && other.id == this.id

  /**
   * @return the hash code of this node, based on its ID
   */
  override fun hashCode(): Int = this.id.hashCode()
}
