package Blockchainj.Blockchain.Statistics;

import Blockchainj.Util.Timer;

public class StatisticsUtxoSetTimer extends Timer {
    /* Times */
    public static final int blockchain = 0;
    public static final int stats = 1;
    private static final int timerLabelCount = 2;

    /* Times names */
    public static final String totalLabelB = "Total UtxoSetStat Time";
    public static final String[] timeLabelsB = new String[timerLabelCount];
    static {
        timeLabelsB[blockchain] = "Blockchainj/Blockchain";
        timeLabelsB[stats] = "Stats";
    }

    public StatisticsUtxoSetTimer(boolean active) {
        super(timeLabelsB, totalLabelB, active);

        PRINT_STRING_LENGTH = 65;
    }

    public StatisticsUtxoSetTimer(boolean active, int movingSumLen) {
        super(timeLabelsB, totalLabelB, active, movingSumLen);

        PRINT_STRING_LENGTH = 65;
    }

    public StatisticsUtxoSetTimer() { this(true); }
}
