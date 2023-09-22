package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Util.Timer;

/** UtxoSetTimer
 *
 *  This class times the stages of utxo set.
 *
 *  When marked as active, the timer works normally.
 *  When marked as inactive, the timer's method when called do nothing.
 *
 *  A Round represents the complete cycle of a commitChanges() call.
 *
 */

public class UtxoSetTimer extends Timer {
    /* Times */
    public static final int calcUtxoSetChanges = 0;
    public static final int applyChanges = 1;
    public static final int rehashMerkleTree = 2;
    public static final int updateUtxoSetLog = 3;
    private static final int utxoSetTimerLabelCountNonConcurrent = 4;

    public static final int totalApplyShardChanges = 0;
    public static final int loadShard = 1;
    public static final int applyShardChanges = 2;
    public static final int calcShardHash = 3;
    public static final int storeShard = 4;
    public static final int shardTimerLabelCount = 5;

    private static final int[] sumLabelIndicesU = new int[shardTimerLabelCount];
    static {
        sumLabelIndicesU[totalApplyShardChanges] = utxoSetTimerLabelCountNonConcurrent;
        sumLabelIndicesU[loadShard] = utxoSetTimerLabelCountNonConcurrent + 1;
        sumLabelIndicesU[applyShardChanges] = utxoSetTimerLabelCountNonConcurrent + 2;
        sumLabelIndicesU[calcShardHash] = utxoSetTimerLabelCountNonConcurrent + 3;
        sumLabelIndicesU[storeShard] = utxoSetTimerLabelCountNonConcurrent + 4;
    }

    private static final int utxoSetTimerLabelCount = utxoSetTimerLabelCountNonConcurrent +
            shardTimerLabelCount;

    public static final String totalLabelU = "Total Utxo Set Time";
    public static final String[] timeLabelsU = new String[utxoSetTimerLabelCount ];
    static {
        timeLabelsU[calcUtxoSetChanges] = "Calculate Utxo Set Changes";
        timeLabelsU[applyChanges ] = "Apply Changes Real Time";
        timeLabelsU[rehashMerkleTree] = "Rehash Merkle Tree";
        timeLabelsU[updateUtxoSetLog] = "Update Utxo Set Log";
        timeLabelsU[sumLabelIndicesU[totalApplyShardChanges]] =
                "CumulativeThreadTime. Total Apply Shard Changes";
        timeLabelsU[sumLabelIndicesU[loadShard]] = "CumulativeThreadTime. Load Shard";
        timeLabelsU[sumLabelIndicesU[applyShardChanges]] =
                "CumulativeThreadTime. Apply Shard Changes";
        timeLabelsU[sumLabelIndicesU[calcShardHash]] = "CumulativeThreadTime. Calc Shard Hash";
        timeLabelsU[sumLabelIndicesU[storeShard]] = "CumulativeThreadTime. Store Shard";
    }

    public class ShardTimer extends Timer {
        public ShardTimer(boolean active) {
            super(new String[shardTimerLabelCount], active, -1);
        }
    }

    public UtxoSetTimer(boolean active) {
        super(timeLabelsU, totalLabelU, sumLabelIndicesU, active);

        PRINT_STRING_LENGTH = 64;
    }

    public UtxoSetTimer(boolean active, int movingSumLen) {
        super(timeLabelsU, totalLabelU, sumLabelIndicesU, active, movingSumLen);

        PRINT_STRING_LENGTH = 64;
    }

    public UtxoSetTimer() {
        this(true);
    }

    public ShardTimer getShardTimer() {
        return new ShardTimer(isActive());
    }
}