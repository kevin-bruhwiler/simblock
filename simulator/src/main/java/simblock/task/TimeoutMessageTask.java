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

import simblock.block.Block;
import simblock.node.Node;

/**
 * The type Inv message task, allows a node to advertise its knowledge of a block.
 */
// Bitcoin protocol Wiki: https://en.bitcoin.it/wiki/Protocol_documentation#inv
public class TimeoutMessageTask extends AbstractMessageTask {
  public enum Type {
    Consensus,
    Hello
  }
  /**
   * Block to be advertised.
   */
  private final long interval;

  private final Type type;

  /**
   * Instantiates a new Inv message task.
   *
   * @param from  the sender
   * @param to    the receiver
   * @param block the block to be advertised
   */
  public TimeoutMessageTask(Node from, Node to, long timeout, Type, type) {
    super(from, to);
    this.interval = timeout;
    this.type = Type
  }

  @Override
  public long getInterval() {
    return this.interval;
  }

  public Type getType() {
    return this.type;
  }

}
