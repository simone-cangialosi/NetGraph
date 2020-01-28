/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.simonecangialosi.netgraph

import kotlin.math.sqrt

/**
 * A couple of coordinates that describe a point in the Cartesian Coordinate System.
 *
 * @property x the coordinate in the x axis
 * @property y the coordinate in the y axis
 */
data class Coords(val x: Double, val y: Double) {

  companion object {

    /**
     * The coefficient for the conversion from radians to degrees.
     */
    private const val RAD_TO_DEG: Double = 180.0 / Math.PI

    /**
     * Build a point by polar coordinates.
     *
     * @param angle the polar angle, in degrees
     * @param distance the distance from the axis origin
     */
    fun byPolar(angle: Double, distance: Double): Coords = Coords(
      x = distance * Math.cos(angle / RAD_TO_DEG),
      y = distance * Math.sin(angle / RAD_TO_DEG)
    )
  }

  /**
   * The angle of the line between this point and the axis origin, in degrees.
   */
  val angle: Double = RAD_TO_DEG * Math.atan2(this.y, this.x)

  /**
   * The distance between this point and the axis origin.
   */
  val length: Double = sqrt(this.x * this.x + this.y + this.y)

  /**
   * Add another point to this.
   *
   * @param other another point
   *
   * @return the result of the addition
   */
  operator fun plus(other: Coords): Coords = Coords(x = this.x + other.x, y = this.y + other.y)

  /**
   * Subtract another point from this.
   *
   * @param other another point
   *
   * @return the result of the subtraction
   */
  operator fun minus(other: Coords): Coords = Coords(x = this.x - other.x, y = this.y - other.y)

  /**
   * Move this point by a delta described by polar coordinates.
   *
   * @param angle the polar angle, in degrees
   * @param distance the distance of the movement
   *
   * @return a new point obtaining adding the given delta to this
   */
  fun move(angle: Double, distance: Double): Coords = this + byPolar(angle = angle, distance = distance)

  /**
   * @return the string representation of this point
   */
  override fun toString(): String = "($x, $y)"
}
