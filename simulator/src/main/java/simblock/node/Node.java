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

package simblock.node;

import static simblock.settings.SimulationConfiguration.BLOCK_SIZE;
import static simblock.settings.SimulationConfiguration.CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE;
import static simblock.settings.SimulationConfiguration.CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE;
import static simblock.settings.SimulationConfiguration.CBR_FAILURE_RATE_FOR_CHURN_NODE;
import static simblock.settings.SimulationConfiguration.CBR_FAILURE_RATE_FOR_CONTROL_NODE;
import static simblock.settings.SimulationConfiguration.COMPACT_BLOCK_SIZE;
import static simblock.simulator.Main.OUT_JSON_FILE;
import static simblock.simulator.Network.getBandwidth;
import static simblock.simulator.Simulator.arriveBlock;
import static simblock.simulator.Timer.getCurrentTime;
import static simblock.simulator.Timer.putTask;
import static simblock.simulator.Timer.removeTask;

import java.util.*;

import simblock.block.BFTBlock;
import simblock.block.Block;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.routing.AbstractRoutingTable;
import simblock.task.*;

/**
 * A class representing a node in the network.
 */
public class Node {
  /**
   * Unique node ID.
   */
  private final int nodeID;

  /**
   * Region assigned to the node.
   */
  private final int region;

  /**
   * Mining power assigned to the node.
   */
  private final long miningPower;

  /**
   * A nodes routing table.
   */
  private AbstractRoutingTable routingTable;

  /**
   * The consensus algorithm used by the node.
   */
  private AbstractConsensusAlgo consensusAlgo;

  /**
   * Whether the node uses compact block relay.
   */
  private boolean useCBR;

  /**
   * The node causes churn.
   */
  private boolean isChurnNode;

  /**
   * The current block.
   */
  private Block block;
  private Block pendingBlock;

  private Set<BFTBlock> acceptedBlocks = new HashSet<>();

  /*
  *  Blocks in different branches
  */
  private List<Block> blocks = new ArrayList<>();
  private List<Node> responded = new ArrayList<>();
  private TimeoutMessageTask timeoutTask = null;

  /**
   * Orphaned blocks known to node.
   */
  private final Set<Block> orphans = new HashSet<>();

  /**
   * The current minting task
   */
  private AbstractMintingTask mintingTask = null;

  /**
   * In the process of sending blocks.
   */
  // TODO verify
  private boolean sendingBlock = false;

  //TODO
  private final ArrayList<AbstractMessageTask> messageQue = new ArrayList<>();
  // TODO
  private final Set<Block> downloadingBlocks = new HashSet<>();

  /**
   * Processing time of tasks expressed in milliseconds.
   */
  private final long processingTime = 2;

  /**
   * Instantiates a new Node.
   *
   * @param nodeID            the node id
   * @param numConnection     the number of connections a node can have
   * @param region            the region
   * @param miningPower       the mining power
   * @param routingTableName  the routing table name
   * @param consensusAlgoName the consensus algorithm name
   * @param useCBR            whether the node uses compact block relay
   * @param isChurnNode       whether the node causes churn
   */
  public Node(
      int nodeID, int numConnection, int region, long miningPower, String routingTableName,
      String consensusAlgoName, boolean useCBR, boolean isChurnNode
  ) {
    this.nodeID = nodeID;
    this.region = region;
    this.miningPower = miningPower;
    this.useCBR = useCBR;
    this.isChurnNode = isChurnNode;

    try {
      this.routingTable = (AbstractRoutingTable) Class.forName(routingTableName).getConstructor(
          Node.class).newInstance(this);
      this.consensusAlgo = (AbstractConsensusAlgo) Class.forName(consensusAlgoName).getConstructor(
          Node.class).newInstance(this);
      this.setNumConnection(numConnection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the node id.
   *
   * @return the node id
   */
  public int getNodeID() {
    return this.nodeID;
  }

  /**
   * Gets the region ID assigned to a node.
   *
   * @return the region
   */
  public int getRegion() {
    return this.region;
  }

  /**
   * Gets mining power.
   *
   * @return the mining power
   */
  public long getMiningPower() {
    return this.miningPower;
  }

  /**
   * Gets the consensus algorithm.
   *
   * @return the consensus algorithm. See {@link AbstractConsensusAlgo}
   */
  @SuppressWarnings("unused")
  public AbstractConsensusAlgo getConsensusAlgo() {
    return this.consensusAlgo;
  }

  /**
   * Gets routing table.
   *
   * @return the routing table
   */
  public AbstractRoutingTable getRoutingTable() {
    return this.routingTable;
  }

  /**
   * Gets the current block.
   *
   * @return the block
   */
  public Block getBlock() {
    return this.block;
  }

  public List<Block> getBlocks() {
    return this.blocks;
  }

  /**
   * Gets all orphans known to node.
   *
   * @return the orphans
   */
  public Set<Block> getOrphans() {
    return this.orphans;
  }

  /**
   * Gets the number of connections a node can have.
   *
   * @return the number of connection
   */
  @SuppressWarnings("unused")
  public int getNumConnection() {
    return this.routingTable.getNumConnection();
  }

  /**
   * Sets the number of connections a node can have.
   *
   * @param numConnection the n connection
   */
  public void setNumConnection(int numConnection) {
    this.routingTable.setNumConnection(numConnection);
  }

  /**
   * Gets the nodes neighbors.
   *
   * @return the neighbors
   */
  public ArrayList<Node> getNeighbors() {
    return this.routingTable.getNeighbors();
  }

  /**
   * Adds the node as a neighbor.
   *
   * @param node the node to be added as a neighbor
   * @return the success state of the operation
   */
  @SuppressWarnings("UnusedReturnValue")
  public boolean addNeighbor(Node node) {
    return this.routingTable.addNeighbor(node);
  }

  /**
   * Removes the neighbor form the node.
   *
   * @param node the node to be removed as a neighbor
   * @return the success state of the operation
   */
  @SuppressWarnings("unused")
  public boolean removeNeighbor(Node node) {
    return this.routingTable.removeNeighbor(node);
  }

  /**
   * Initializes the routing table.
   */
  public void joinNetwork() {
    this.routingTable.initTable();
  }

  /**
   * Mint the genesis block.
   */
  public void genesisBlock() {
    Block genesis = this.consensusAlgo.genesisBlock();
    this.receiveBlock(genesis);
  }

  /**
   * Adds a new block to the to chain. If node was minting that task instance is abandoned, and
   * the new block arrival is handled.
   *
   * @param newBlock the new block
   */
  public void addToChain(Block newBlock) {
    // If the node has been minting
    if (this.mintingTask != null) {
      removeTask(this.mintingTask);
      this.mintingTask = null;
    }

    // If the node is still signing a block
    if (this.timeoutTask != null) {
      removeTask(this.timeoutTask);
      this.timeoutTask = null;
      this.orphans.add(this.pendingBlock);
    }

    // Update the current block
    this.block = newBlock;

    // Nick
    // handle adding blocks with branch
    if (newBlock instanceof BFTBlock) {
      this.acceptedBlocks.remove(newBlock);
      if (((BFTBlock)newBlock).getParents().size() > 1) {
        // A merging block
        List<Block> mergeBlocks = new ArrayList<>();
        for (Block b : this.blocks) {
          if (b.isOnSameChainAs(newBlock)) {
            mergeBlocks.add(b);
          }
        }

        this.blocks.removeAll(mergeBlocks);
        this.blocks.add(newBlock);
      } else {
        boolean isNewBranch = true;
        // Not merging block, find the corresponding branch
        for (int i = 0; i < this.blocks.size(); i ++) {
          if (this.blocks.get(i).isOnSameChainAs(newBlock)) {
            this.blocks.set(i, newBlock);
            isNewBranch = false;
            break;
          }
        }

        if (isNewBranch){
          this.blocks.add(newBlock);
        }
      }
    }

    printAddBlock(newBlock);
    // Observe and handle new block arrival
    arriveBlock(newBlock, this);
  }

  /**
   * Logs the provided block to the logfile.
   *
   * @param newBlock the block to be logged
   */
  private void printAddBlock(Block newBlock) {
    OUT_JSON_FILE.print("{");
    OUT_JSON_FILE.print("\"kind\":\"add-block\",");
    OUT_JSON_FILE.print("\"content\":{");
    OUT_JSON_FILE.print("\"timestamp\":" + getCurrentTime() + ",");
    OUT_JSON_FILE.print("\"node-id\":" + this.getNodeID() + ",");
    OUT_JSON_FILE.print("\"block-id\":" + newBlock.getId());
    OUT_JSON_FILE.print("}");
    OUT_JSON_FILE.print("},");
    OUT_JSON_FILE.flush();
  }

  /**
   * Add orphans.
   *
   * @param orphanBlock the orphan block
   * @param validBlock  the valid block
   */
  //TODO check this out later
  public void addOrphans(Block orphanBlock, Block validBlock) {
    if (orphanBlock != validBlock) {
      this.orphans.add(orphanBlock);
      this.orphans.remove(validBlock);
      if (validBlock == null || orphanBlock.getHeight() > validBlock.getHeight()) {
        this.addOrphans(orphanBlock.getParent(), validBlock);
      } else if (orphanBlock.getHeight() == validBlock.getHeight()) {
        this.addOrphans(orphanBlock.getParent(), validBlock.getParent());
      } else {
        this.addOrphans(orphanBlock, validBlock.getParent());
      }
    }
  }

  /**
   * Generates a new minting task and registers it
   */
  public void minting() {
    AbstractMintingTask task = this.consensusAlgo.minting();
    this.mintingTask = task;
    if (task != null) {
      putTask(task);
    }
  }

  /**
   * Send inv.
   *
   * @param block the block
   */
  public void sendInv(Block block) {
    for (Node to : this.routingTable.getNeighbors()) {
      AbstractMessageTask task = new InvMessageTask(this, to, block);
      putTask(task);
    }
  }

  /** Nick
   * Sign block. Called by BFTMinging
   *
   * @param block the block
   */
  public void signBlock(BFTBlock block) {
    this.pendingBlock = block;
    this.responded.clear();

    List<Node> group = block.getConsensusGroup();
    //System.out.println("Group size:" + group.size());
    for (Node to : group) {
      AbstractMessageTask task = new HelloMessageTask(this, to, block);
      putTask(task);
    }

    // TODO: ADD timeout to setting
    timeoutTask = new TimeoutMessageTask(this, this, 500, TimeoutMessageTask.Type.Hello);
    putTask(timeoutTask);
  }

  /**
   * Receive block.
   *
   * @param block the block
   */
  public void receiveBlock(Block block) {
    if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
      if (this.block != null && !this.block.isOnSameChainAs(block) && !(this.block instanceof BFTBlock)) {
        // If orphan mark orphan
        this.addOrphans(this.block, block);
      }
      // Else add to canonical chain
      this.addToChain(block);
      // Generates a new minting task
      this.minting();
      // Advertise received block
      this.sendInv(block);
    } else if (!this.orphans.contains(block) && !block.isOnSameChainAs(this.block) && !(this.block instanceof BFTBlock)) {
      // TODO better understand - what if orphan is not valid?
      // If the block was not valid but was an unknown orphan and is not on the same chain as the
      // current block
      this.addOrphans(block, this.block);
      arriveBlock(block, this);
    }
  }

  /**
   * Receive message.
   *
   * @param message the message
   */
  public void receiveMessage(AbstractMessageTask message) {
    Node from = message.getFrom();

    if (message instanceof InvMessageTask) {
      Block block = ((InvMessageTask) message).getBlock();
//      if (!block.isOnSameChainAs(this.getBlock()))
//        System.out.println("Recieved " + block);
      if (!this.orphans.contains(block) && !this.downloadingBlocks.contains(block)) {
        if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
          AbstractMessageTask task = new RecMessageTask(this, from, block);
          putTask(task);
          downloadingBlocks.add(block);
        } else if (!block.isOnSameChainAs(this.block)) {
          // get new orphan block
          AbstractMessageTask task = new RecMessageTask(this, from, block);
          putTask(task);
          downloadingBlocks.add(block);
        }
      }
    }

    if (message instanceof RecMessageTask) {
      this.messageQue.add((RecMessageTask) message);
      if (!sendingBlock) {
        this.sendNextBlockMessage();
      }
    }

    if(message instanceof GetBlockTxnMessageTask){
			this.messageQue.add((GetBlockTxnMessageTask) message);
			if(!sendingBlock){
				this.sendNextBlockMessage();
			}
		}

    if(message instanceof CmpctBlockMessageTask){
			Block block = ((CmpctBlockMessageTask) message).getBlock();
      Random random = new Random();
      float CBRfailureRate = this.isChurnNode ? CBR_FAILURE_RATE_FOR_CHURN_NODE : CBR_FAILURE_RATE_FOR_CONTROL_NODE;
			boolean success = random.nextDouble() > CBRfailureRate ? true : false;
			if(success){
				downloadingBlocks.remove(block);
				this.receiveBlock(block);
			}else{
				AbstractMessageTask task = new GetBlockTxnMessageTask(this, from, block);
				putTask(task);
			}
		}

    if (message instanceof BlockMessageTask) {
      Block block = ((BlockMessageTask) message).getBlock();
      downloadingBlocks.remove(block);
      this.receiveBlock(block);
    }
    

    // Nick
    // handle new message type
    if (message instanceof HelloMessageTask) {
//      if (this.timeoutTask == null) {
//        return;
//      }
      boolean accepted = true;
      Block newBlock = ((HelloMessageTask) message).getBlock();
      for (Block block : this.acceptedBlocks) {
        BFTBlock bftBlock = (BFTBlock) block;
        if (!bftBlock.equals(newBlock) && bftBlock.getHeight() >= newBlock.getHeight()) {
          accepted = false;
          break;
        }
      }

      for (Block block : this.blocks) {
        BFTBlock bftBlock = (BFTBlock) block;
        if (bftBlock.getHeight() >= newBlock.getHeight()) {
          accepted = false;
          break;
        }
      }

      if (accepted)
        acceptedBlocks.add((BFTBlock) newBlock);
      // If not seen any block at the slot, reply with yes
      AbstractMessageTask task = new ReplyMessageTask(this, from, accepted, newBlock);
      putTask(task);
    }

    if (message instanceof ReplyMessageTask) {
      if (this.timeoutTask == null || !((ReplyMessageTask)message).block.equals(this.pendingBlock)) {
        return;
      }

      if (((ReplyMessageTask)message).verified()) {
        responded.add(from);
      } else {
        // Should stop the task
        removeTask(this.timeoutTask);
        this.timeoutTask = null;

        // Current block is marked as wasted
        this.orphans.add(this.pendingBlock);
      }

      // If all responded, sign and sendout the new block
      if (responded.size() == ((BFTBlock)this.block).getConsensusGroup().size()) {
        // TODO: setup signee
        ((BFTBlock)this.pendingBlock).setSigners(responded);
        // Setup consensus delay
        removeTask(this.timeoutTask);
        // TODO: ADD consensus latency to setting
        this.timeoutTask = new TimeoutMessageTask(this, this, 15000, TimeoutMessageTask.Type.Consensus);
        putTask(this.timeoutTask);
      }
    }

    if (message instanceof TimeoutMessageTask) {
      TimeoutMessageTask.Type type = ((TimeoutMessageTask)message).getType();
      if (type == TimeoutMessageTask.Type.Consensus) {
        this.timeoutTask = null;
        receiveBlock(this.pendingBlock);
      } else if (type == TimeoutMessageTask.Type.Hello) {
        ((BFTBlock)this.pendingBlock).setSigners(responded);
        this.timeoutTask = new TimeoutMessageTask(this, this, 15000, TimeoutMessageTask.Type.Consensus);
        putTask(this.timeoutTask);
      }
    }
  }


  /**
   * Gets block size when the node fails compact block relay.
   */
  private long getFailedBlockSize(){
		Random random = new Random();
			if(this.isChurnNode){
				int index = random.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE.length);
				return (long)(BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE[index]);
			}else{
				int index = random.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE.length);
				return (long)(BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE[index]);
			}
	}

  /**
   * Send next block message.
   */
  // send a block to the sender of the next queued recMessage
  public void sendNextBlockMessage() {
    if (this.messageQue.size() > 0) {
      Node to = this.messageQue.get(0).getFrom();
      long bandwidth = getBandwidth(this.getRegion(), to.getRegion());

      // In the event of a partition, don't send the message
      if (bandwidth == 0){
         sendingBlock = false;
         this.messageQue.remove(0);
         sendNextBlockMessage();
         return;
	  }

      AbstractMessageTask messageTask;

      if(this.messageQue.get(0) instanceof RecMessageTask){
        Block block = ((RecMessageTask) this.messageQue.get(0)).getBlock();
        // If use compact block relay.
        if(this.messageQue.get(0).getFrom().useCBR && this.useCBR) {
          // Convert bytes to bits and divide by the bandwidth expressed as bit per millisecond, add
          // processing time.
          long delay = COMPACT_BLOCK_SIZE * 8 / (bandwidth / 1000) + processingTime;
          // Send compact block message.
          messageTask = new CmpctBlockMessageTask(this, to, block, delay);
        } else {
          // Else use lagacy protocol.
          long delay = BLOCK_SIZE * 8 / (bandwidth / 1000) + processingTime;
          messageTask = new BlockMessageTask(this, to, block, delay);
        }
      } else if(this.messageQue.get(0) instanceof GetBlockTxnMessageTask) {
        // Else from requests missing transactions.
        Block block = ((GetBlockTxnMessageTask) this.messageQue.get(0)).getBlock();
        long delay = getFailedBlockSize() * 8 / (bandwidth / 1000) + processingTime;
        messageTask = new BlockMessageTask(this, to, block, delay);
      } else {
        throw new UnsupportedOperationException();
      }
      
      sendingBlock = true;
      this.messageQue.remove(0);
      putTask(messageTask);
    } else {
      sendingBlock = false;
    }
  }
}
