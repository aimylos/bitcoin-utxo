package Blockchainj.Blockchain;


import Blockchainj.Util.Timer;

/** BlockchainTimer
 *
 *  This class times the stages of building the blockchain.
 *
 *  When marked as active, the timer works normally.
 *  When marked as inactive, the timer's method when called do nothing.
 *
 *  A Round represents the complete cycle of a block's processing.
 *
 */

public class BlockchainTimer extends Timer {
    /* Times */
    public static final int blockBuffer = 0;
    public static final int commitBlock = 1;
    private static final int blockchainTimerLabelCount = 2;

    /* Times names */
    public static final String totalLabelB = "Total Blockchain Time";
    public static final String[] timeLabelsB = new String[blockchainTimerLabelCount];
    static {
        timeLabelsB[blockBuffer] = "BlockBuffer";
        timeLabelsB[commitBlock] = "CommitChanges";
    }

    public BlockchainTimer(boolean active) {
        super(timeLabelsB, totalLabelB, active);

        PRINT_STRING_LENGTH = 65;
    }

    public BlockchainTimer(boolean active, int movingSumLen) {
        super(timeLabelsB, totalLabelB, active, movingSumLen);

        PRINT_STRING_LENGTH = 65;
    }

    public BlockchainTimer() {
        this(true);
    }
}
