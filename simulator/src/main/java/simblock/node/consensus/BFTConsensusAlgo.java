package simblock.node.consensus;

import simblock.block.BFTBlock;
import simblock.block.Block;
import simblock.block.ProofOfWorkBlock;
import simblock.node.Node;
import simblock.task.AbstractMintingTask;
import simblock.task.BFTMiningTask;
import simblock.task.MiningTask;

import java.math.BigInteger;

import static simblock.simulator.Main.random;

public class BFTConsensusAlgo extends AbstractConsensusAlgo{


    /**
     * Instantiates a new Abstract consensus algo.
     *
     * @param selfNode the self node
     */
    public BFTConsensusAlgo(Node selfNode) {
        super(selfNode);
    }
    /**
     * Mints a new block by simulating Proof of Work.
     */
    @Override
    public BFTMiningTask minting() {
        Node selfNode = this.getSelfNode();
        ProofOfWorkBlock parent = (ProofOfWorkBlock) selfNode.getBlock();
        BigInteger difficulty = parent.getNextDifficulty();
        double p = 1.0 / difficulty.doubleValue();
        double u = random.nextDouble();
        return p <= Math.pow(2, -53) ? null : new BFTMiningTask(selfNode, (long) (Math.log(u) / Math.log(
                1.0 - p) / selfNode.getMiningPower()), difficulty);
    }

    /**
     * Tests if the receivedBlock is valid with regards to the current block. The receivedBlock
     * is valid if it is an instance of a Proof of Work block and the received block needs to have
     * a bigger difficulty than its parent next difficulty and a bigger total difficulty compared to
     * the current block.
     *
     * @param receivedBlock the received block
     * @param currentBlock  the current block
     * @return true if block is valid false otherwise
     */
    @Override
    public boolean isReceivedBlockValid(Block receivedBlock, Block currentBlock) {
        if (!(receivedBlock instanceof BFTBlock)) {
            return false;
        }
        if (currentBlock == null)
            return true;
        return receivedBlock.getHeight() > currentBlock.getHeight();
    }

    @Override
    public BFTBlock genesisBlock() {
        return BFTBlock.genesisBlock(this.getSelfNode());
    }
}
