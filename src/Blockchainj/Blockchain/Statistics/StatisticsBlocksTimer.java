package Blockchainj.Blockchain.Statistics;

import Blockchainj.Util.Timer;

public class StatisticsBlocksTimer extends Timer {
    /* Times */
    public static final int blockBuffer = 0;
    public static final int stats = 1;
    private static final int timerLabelCount = 2;

    /* Times names */
    public static final String totalLabelB = "Total BlocksStat Time";
    public static final String[] timeLabelsB = new String[timerLabelCount];
    static {
        timeLabelsB[blockBuffer] = "BlockBuffer";
        timeLabelsB[stats] = "Stats";
    }

    public StatisticsBlocksTimer(boolean active) {
        super(timeLabelsB, totalLabelB, active);

        PRINT_STRING_LENGTH = 65;
    }

    public StatisticsBlocksTimer(boolean active, int movingSumLen) {
        super(timeLabelsB, totalLabelB, active, movingSumLen);

        PRINT_STRING_LENGTH = 65;
    }

    public StatisticsBlocksTimer() {
        this(true);
    }
}
