package simblock.task;

import simblock.simulator.Network;

/**
 * The type Partition task.
 */
public class PartitionTask implements Task {
  private boolean partitioned = true;
  /**
   * Instantiates a new Partition task.
   *
   */
  public PartitionTask(boolean partitioned) {
    this.partitioned = partitioned;
  }

  public long getInterval() {
    // Only used as an absolute time task, this value does not matter
    return -1;
  }

  public void run() {
    // Instruct the Network class to start using new bandwidth matrices
     Network.partitioned = partitioned;
  }
}