/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

import com.simonecangialosi.netgraph.Graph

/**
 * Create graphs from links between nodes, then print their coordinates.
 */
fun main() {

  val links = mapOf(
    1 to setOf(2, 3, 4, 5, 6, 7),
    2 to setOf(8, 9, 10),
    10 to setOf(7)
  )

  Graph.byLinks(links).forEachIndexed { i, it ->

    println("\nGraph #${i+1}")

    it.nodes.forEach {
      println("${it.id}\t${it.coords.x}\t${it.coords.y}")
    }
  }
}
