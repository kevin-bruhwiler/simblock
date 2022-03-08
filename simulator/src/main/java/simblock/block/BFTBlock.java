package simblock.block;

import simblock.node.Node;
import simblock.settings.SimulationConfiguration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


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
        this.parents = parents;
        this.signers = new ArrayList<>();
        this.consensusGroup = new ArrayList<>();

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
}
