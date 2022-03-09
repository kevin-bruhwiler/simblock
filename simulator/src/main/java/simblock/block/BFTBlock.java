package simblock.block;

import simblock.node.Node;
import simblock.settings.SimulationConfiguration;

import java.math.BigInteger;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import static simblock.simulator.Simulator.getSimulatedNodes;
import static simblock.simulator.Simulator.getTargetInterval;


public class BFTBlock extends ProofOfWorkBlock {

    private final List<BFTBlock> parents;

    private List<Node> consensusGroup;
    private List<Node> signers;

    /**
     *
     * @param parents parents of the block
     * @param minter node that mined this block
     * @param time time
     * @param difficulty difficulty of the block
     */
    public BFTBlock(List<BFTBlock> parents, Node minter, long time, BigInteger difficulty) {
        super(parents.size() < 1 ? null : parents.get(0), minter, time, difficulty);
        this.parents = new ArrayList<>();
        this.parents.addAll(parents);
        this.signers = new ArrayList<>();
        this.consensusGroup = new ArrayList<>();
        this.height = parents.size() == 0 ? 0 : getMaxHeight(parents) + 1;

        if (parents.size() > 1) {
            // we get each parent's consensus group and select some of them based on the branch size (w * hash power)
            for (BFTBlock parent : parents) {
                List<Node> parentConsensusGroup = parent.getConsensusGroup();
                Integer branchSize = parent.getBranchSize();
                consensusGroup.addAll(parentConsensusGroup.subList(parentConsensusGroup.size() - branchSize, parentConsensusGroup.size()));
            }
        }
        else if (parents.size() == 1) {
            // we get parent's consensus group and remove the oldest. then add the parent as one of the participants
            consensusGroup = parents.get(0).getConsensusGroup();
            if (consensusGroup.size() >= SimulationConfiguration.WINDOW_SIZE)
                consensusGroup.remove(0);
            consensusGroup.add(parents.get(0).getMinter());
        }
        else if (parents.size() == 0) {
            // genesis blocks
        }
    }

    private int getMaxHeight(List<BFTBlock> parents) {
        int max = 0;
        for (BFTBlock block : parents) {
            if (block.getHeight() > max) {
                max = block.getHeight();
            }
        }
        return max;
    }

    /**
     *
     * @return consensus group which consist of maximum w nodes that mined w previous blocks. There can be duplicate nodes.
     */
    public List<Node> getConsensusGroup() {
        return this.consensusGroup;
    }

    /**
     *
     * @return size of the branch that block is in it. equals to the number of signers. based on window size and hash power.
     */
    private Integer getBranchSize() {
        return signers.size();
    }

    public List<BFTBlock> getParents() {
        return this.parents;
    }

    public boolean isOnSameChainAs(Block block) {
        if (block == null) {
            return false;
        } else if (block.equals(this)) {
            return true;
        } else if (this.height <= block.height) {
            if (((BFTBlock) block).getParents().size() == 0)
                return true;
            for (BFTBlock b : ((BFTBlock) block).getParents()) {
                if (this.isOnSameChainAs(b))
                    return true;
            }
            return false;
        } else {
            return block.isOnSameChainAs(this);
        }
    }


    public void setSigners(List<Node> responded) {
        this.signers = responded;
    }

    public static BFTBlock genesisBlock(Node minter) {
        long totalMiningPower = 0;
        for (Node node : getSimulatedNodes()) {
            totalMiningPower += node.getMiningPower();
        }
        genesisNextDifficulty = BigInteger.valueOf(totalMiningPower * getTargetInterval());
        List<BFTBlock> emptyParents = new ArrayList<>();
        return new BFTBlock(emptyParents,  minter, 0, BigInteger.ZERO);
    }
}
