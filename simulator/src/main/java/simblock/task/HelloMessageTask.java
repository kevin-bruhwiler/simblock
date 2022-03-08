/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.task;

import static simblock.simulator.Network.getBandwidth;
import static simblock.simulator.Network.getLatency;
import simblock.block.Block;
import simblock.node.Node;

/** Nick
 * The type Hello message task, check if reachbale to the destination
 */
// Bitcoin protocol Wiki: https://en.bitcoin.it/wiki/Protocol_documentation#inv
public class HelloMessageTask extends AbstractMessageTask {

  /**
   * Block to be advertised.
   */
  private final Block block;

  /**
   * Interval of sending message
   */
  private final long interval;

  /**
   * Instantiates a new Hello message task.
   *
   * @param from  the sender
   * @param to    the receiver
   * @param block the block to be advertised
   */
  public HelloMessageTask(Node from, Node to, Block block) {
    super(from, to);
    this.block = block;

    // Do not send the message if the bandwidth is 0
    if (getBandwidth(from.getRegion(), to.getRegion()) == 0) {
      this.interval = Long.MAX_VALUE;
    } else {
      this.interval = getLatency(from.getRegion(), to.getRegion());
    }
  }

  @Override
  public long getInterval() {
    return this.interval;
  }

  /**
   * Gets block.
   *
   * @return the block
   */
  public Block getBlock() {
    return this.block;
  }

}
