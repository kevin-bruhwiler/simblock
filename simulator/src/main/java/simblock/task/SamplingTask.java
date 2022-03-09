package simblock.task;

/**
 * The type Sampling task.
 */
public class SamplingTask implements Task {
  /**
   * Instantiates a new Sampling task.
   *
   */
  public SamplingTask() {}

  public long getInterval() {
    // Only used as an absolute time task, this value does not matter
    return -1;
  }

  public void run() {}
}