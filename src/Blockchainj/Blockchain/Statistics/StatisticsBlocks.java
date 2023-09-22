package Blockchainj.Blockchain.Statistics;

import Blockchainj.Bitcoin.*;
import Blockchainj.Bitcoin.RPC.BitcoinRpcException;
import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.*;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxFastFactory;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxoSetChanges;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxoSetChangesCardinalities;
import Blockchainj.Util.MathAverage;

import java.io.*;
import java.util.*;

/**
 * StatisticsBlocks
 *
 * Static means that all statistics produced from this class are Utxo Set and independent.
 * These statistics are calculated from blocks and do not require a Utxo Set.
 *
 */


@SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions", "DanglingJavadoc"})
public class StatisticsBlocks extends Statistics {
    /* Stats file names */
    private static final String statInfoFilename = "stat_info.log";
    private static final String statLogFilename = "stat_log.log";

    /* Stat info */
    private static final String STAT_INFO_HEADER = "Blocks statistics. ";
    private static final LinkedHashMap<Integer, StatGroup> INIT_GROUPS= new LinkedHashMap<>();

    /* Best current log */
    private final LogStatic currentLog;

    /* BlockBuffer */
    private final BlockBuffer blockBuffer;
    private BlockBuffer.BlockIterator blockIterator = null;

    /* Current block being read. */
    private Block block = null;

    /* Misc parameters */
    public static final int UNDEFINED_HEIGHT = ProtocolParams.UNDEFINED_HEIGHT;

    /* Timer */
    private final StatisticsBlocksTimer timer;
    private static final boolean activeTimer = true;


    public StatisticsBlocks(String path,
                            boolean STATISTICS_DO_CONCURRENT,
                            int STATISTICS_THREAD_NUM,
                            BlockBuffer blockBuffer)
            throws IOException {
        super(path,
                statInfoFilename,
                statLogFilename,
                INIT_GROUPS,
                STATISTICS_DO_CONCURRENT,
                STATISTICS_THREAD_NUM);

        /* Read log */
        LogStatic tempLog;
        try {
            /* Create new empty log and try to sync it */
            tempLog = new LogStatic();
            readLog(tempLog);
        } catch (FileNotFoundException e) {
            /* Create and init log */
            tempLog = new LogStatic(UNDEFINED_HEIGHT);
        }
        currentLog = tempLog;

        /* Init blockbuffer */
        this.blockBuffer = blockBuffer;

        /* Init block iterator at bestheight+1 */
        if(getBestHeight() == UNDEFINED_HEIGHT) {
            this.blockIterator = this.blockBuffer.iterator(0);
        } else {
            this.blockIterator = this.blockBuffer.iterator(getBestHeight()+1);
        }

        /* Init timer */
        if(activeTimer) {
            this.timer = new StatisticsBlocksTimer();
        } else {
            this.timer = null;
        }
    }


    @Override
    protected boolean doCycle() throws IOException {
        /* TIMER */
        if(activeTimer) {
            //noinspection ConstantConditions
            timer.startRound();
            timer.startTimerForStage(StatisticsBlocksTimer.blockBuffer);
        }

        try {
            /* Get next block */
            try {
                block = blockIterator.next();
            } catch (NoSuchElementException e) {
                return false;
            } catch (BitcoinRpcException | BitcoinBlockException e) {
                throw new IOException(e);
            }

            /* TIMER */
            if (activeTimer) {
                timer.endTimerForStage1startTimerForStage2(
                        StatisticsBlocksTimer.blockBuffer, StatisticsBlocksTimer.stats);
            }

            /* Perform computation and storage from callables.
             * super.doCycle() always returns true or throws IOException. */
            super.doCycle();

            /* Update log. */
            currentLog.height = block.getHeight();

            /* TIMER */
            if (activeTimer) {
                timer.endTimerForStage(StatisticsBlocksTimer.stats);
            }

            /* Do not store log every cycle !*/
        } finally {
            /* Reset block to null */
            block = null;

            /* TIMER */
            if (activeTimer) {
                timer.endRound();
            }
        }

        /* Print progress */
        if (getBestHeight() % PRINT_PERIOD == 0) {
            if(PRINTSTREAM != null) {
                PRINTSTREAM.println("Current height: " + getBestHeight());

                /* Print times. */
                if (activeTimer) {
                    timer.print(PRINTSTREAM, true, true, true);
                    PRINTSTREAM.print("\n");
                }
            }
        }

        return true;
    }


    /* This is safe because super.close() is called first and after that no changes will
       take place to this instance. */
    @Override
    public void close() {
        if (isClosed()) {
            return;
        }

        /* Close instance so no more changes can occur */
        super.close();

        if(PRINTSTREAM != null) {
            PRINTSTREAM.println("Storing log to disk...");
        }

        try {
            storeLog(currentLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(PRINTSTREAM != null) {
            PRINTSTREAM.println("Log stored to disk.");
        }
    }


    @Override
    public void writeStatInfo() throws IOException {
        writeStatInfo(STAT_INFO_HEADER);
    }


    public int getBestHeight() { return currentLog.height; }


    /* Log */
    protected class LogStatic extends Log {
        int height;
        MathAverage txCount;
        MathAverage uniqueTxidCount;
        MathAverage[] shardsInvolved;

        LogStatic() {
            this.shardsInvolved = new MathAverage[SHARD_NUMS.length];
        }

        LogStatic(int height) {
            this.height = height;
            this.txCount = new MathAverage(TX_COUNT_MOV_AVG_LEN);
            this.uniqueTxidCount = new MathAverage(UNIQUE_TXID_COUNT_MOV_AVG_LEN);

            this.shardsInvolved = new MathAverage[SHARD_NUMS.length];
            for(int i=0; i<SHARD_NUMS.length; i++) {
                shardsInvolved[i] = new MathAverage(SHARDS_INVOLVED_COUNT_MOV_AVG_LEN);
            }
        }

        @Override
        void store(OutputStream outputStream) throws IOException {
            BitcoinParams.INT32ToOutputStream(height, outputStream);
            txCount.store(outputStream);
            uniqueTxidCount.store(outputStream);

            for(int i=0; i<SHARD_NUMS.length; i++) {
                shardsInvolved[i].store(outputStream);
            }
        }

        @Override
        void load(InputStream inputStream) throws IOException {
            height = BitcoinParams.readINT32(inputStream);
            txCount = MathAverage.load(inputStream);
            uniqueTxidCount = MathAverage.load(inputStream);

            for(int i=0; i<SHARD_NUMS.length; i++) {
                shardsInvolved[i] = MathAverage.load(inputStream);
            }
        }

        void print(PrintStream printStream) {
            printStream.println(">LogStatic: ");
            printStream.println("Height: " + height);
            printStream.println("\tTransaction Count: ");
            txCount.print(printStream, false);
            printStream.println("\tUnique TXID count: ");
            uniqueTxidCount.print(printStream, false);

            printStream.println("\tShards Invlolved count: ");
            for(int i=0; i<SHARD_NUMS.length; i++) {
                shardsInvolved[i].print(printStream, false);
            }
        }
    }


    /* Prints parameters */
    @Override
    public void printParameters(PrintStream printStream) {
        super.printParameters(printStream);
        printStream.println(">StatisticsBlocks parameters:");
        printStream.println("StatisticsBlocks active timer: " + activeTimer);
        blockBuffer.printParameters(printStream);
    }


    @Override
    public void print(PrintStream printStream) {
        super.print(printStream);
        printStream.println(">StatisticsBlocks: ");
        currentLog.print(printStream);
    }


    /**
     * ###################################################################
     * ###################### Make groups reachable ######################
     * ################################################################### */
    /* Returns the appopriate callable for that group */
    @Override
    protected GroupCallable<Void> getCallable(int groupId) {
        //No need to break. Unreachable statements.
        switch (groupId) {
            case 1: return new DoGroup_1<>();

            case 2: return new DoGroup_2<>();

            case 3: return new DoGroup_3<>();

            case 4: return new DoGroup_4<>();

            case 5: return new DoGroup_5<>();

            case 6: return new DoGroup_6<>();

            case 7: return new DoGroup_7<>();

            default: return null;
        }
    }

    /**
     * ###################################################################
     * ############################# Group 1 #############################
     * ################################################################### */
    private static final int TX_COUNT_MOV_AVG_LEN = 200;
    static {
        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 1;
        int statId = 1000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId + 1, groupId, activeGroup && true,
                "block_tx_count",
                "Block transactions count at this height.");
        group.addStat(stat);

        stat = new Stat(statId + 2, groupId, activeGroup && true,
                "block_avg_tx_count_since_genesis",
                "Block average of transactions per block since genesis block.");
        group.addStat(stat);

        stat = new Stat(statId + 3, groupId, activeGroup && true,
                "block_var_tx_count_since_genesis",
                "Block variance of transactions per block since genesis block.");
        group.addStat(stat);

        stat = new Stat(statId + 4, groupId, activeGroup && true,
                "block_mov_avg_tx_count_last_" + TX_COUNT_MOV_AVG_LEN,
                "Block moving average of transactions per block for last " +
                        TX_COUNT_MOV_AVG_LEN + " blocks.");
        group.addStat(stat);

        stat = new Stat(statId + 5, groupId, activeGroup && true,
                "block_mov_var_tx_count_last_" + TX_COUNT_MOV_AVG_LEN,
                "Block moving variance of transactions per block for last " +
                        TX_COUNT_MOV_AVG_LEN + " blocks.");
        group.addStat(stat);
    }

    private class DoGroup_1<V extends Void> extends GroupCallable<V> {

        DoGroup_1() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();
            int txCount = block.getTxnCount();

            /* Update log txCount. Nothing else for consistent concurrency. */
            MathAverage txCountAvg = currentLog.txCount;
            txCountAvg.push(txCount);

            /* Compute and update entries */
            Stat stat = getStat(1001);
            if(stat.isActive()) {
                EntryIntLong entryIntLong = new EntryIntLong(stat.getId(), height, txCount);
                appendEntry(entryIntLong);
            }

            stat = getStat(1002);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, txCountAvg.getAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1003);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, txCountAvg.getDev());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1004);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, txCountAvg.getMovingAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1005);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, txCountAvg.getMovingDev());
                appendEntry(entryIntDouble);
            }

            return null;
        }
    }


    /**
     * ###################################################################
     * ############################# Group 2 #############################
     * ################################################################### */
    static {
        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 2;
        int statId = 2000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "block_tx_inputs_count",
                "Block transactions' inputs count at this height");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "block_tx_avg_inputs_per_tx",
                "Block average of transaction inputs per transaction at this height");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "block_tx_var_inputs_per_tx",
                "Block variance of transaction inputs per transaction at this height");
        group.addStat(stat);

        //TODO avg input count for last 200
    }

    private class DoGroup_2<V extends Void> extends GroupCallable<V> {

        DoGroup_2() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();

            int inputsSum = 0;
            MathAverage inputsAvg = new MathAverage(); //No moving average needed.
            Transaction[] tx = block.getTx();
            for(int txIndex=0; txIndex<tx.length; txIndex++) {
                Transaction t = tx[txIndex];

                int txInCount = t.getTxInCount();
                inputsSum += txInCount;
                inputsAvg.push(txInCount);
            }

            /* Compute and update entries */
            Stat stat = getStat(2001);
            if(stat.isActive()) {
                EntryIntLong entryIntLong = new EntryIntLong(stat.getId(), height, inputsSum);
                appendEntry(entryIntLong);
            }

            stat = getStat(2002);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, inputsAvg.getAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(2003);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, inputsAvg.getDev());
                appendEntry(entryIntDouble);
            }

            return null;
        }
    }


    /**
     * ###################################################################
     * ############################# Group 3 #############################
     * ################################################################### */
    static {
        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 3;
        int statId = 3000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "block_tx_outputs_count",
                "Block transactions' outputs count at this height");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "block_tx_avg_outputs_per_tx",
                "Block average of transaction outputs per transaction at this height");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "block_tx_var_outputs_per_tx",
                "Block variance of transaction outputs per transaction at this height");
        group.addStat(stat);

        //TODO avg output count for last 200
    }

    private class DoGroup_3<V extends Void> extends GroupCallable<V> {

        DoGroup_3() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();

            int outputsSum = 0;
            MathAverage outputsAvg = new MathAverage(); //No moving average needed.
            Transaction[] tx = block.getTx();
            for(int txIndex=0; txIndex<tx.length; txIndex++) {
                Transaction t = tx[txIndex];

                int txOutCount = t.getTxOutCount();
                outputsSum += txOutCount;
                outputsAvg.push(txOutCount);
            }

            /* Compute and update entries */
            Stat stat = getStat(3001);
            if(stat.isActive()) {
                EntryIntLong entryIntLong = new EntryIntLong(stat.getId(), height, outputsSum);
                appendEntry(entryIntLong);
            }

            stat = getStat(3002);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, outputsAvg.getAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(3003);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, outputsAvg.getDev());
                appendEntry(entryIntDouble);
            }

            return null;
        }
    }


    /**
     * ###################################################################
     * ############################# Group 4 #############################
     * ################################################################### */
    private static final int UNIQUE_TXID_COUNT_MOV_AVG_LEN = 200;
    static {
        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 4;
        int statId = 4000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "block_unique_txid_count",
                "Block unique txid's count. " +
                        "Sum of all unique txid's inside transaction inputs (prevTxid)" +
                        " and outputs(transaction's txid)");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "block_avg_unique_txid_count_since_geneis",
                "Block average of unique txid's count since genesis. " +
                        "Sum of all unique txid's inside transaction inputs (prevTxid)" +
                        " and outputs(transaction's txid)");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "block_var_unique_txid_count_since_genesis",
                "Block variance of unique txid's count since genesis. " +
                        "Sum of all unique txid's inside transaction inputs (prevTxid)" +
                        " and outputs(transaction's txid)");
        group.addStat(stat);

        stat = new Stat(statId+4, groupId, activeGroup && true,
                "block_avg_unique_txid_count_last_" + UNIQUE_TXID_COUNT_MOV_AVG_LEN,
                "Block average of unique txid's count for last " +
                        UNIQUE_TXID_COUNT_MOV_AVG_LEN + " blocks. " +
                        "Sum of all unique txid's inside transaction inputs (prevTxid)" +
                        " and outputs(transaction's txid)");
        group.addStat(stat);

        stat = new Stat(statId+5, groupId, activeGroup && true,
                "block_var_unique_txid_count_last_" + UNIQUE_TXID_COUNT_MOV_AVG_LEN,
                "Block variance of unique txid's count for last " +
                        UNIQUE_TXID_COUNT_MOV_AVG_LEN + " blocks. " +
                        "Sum of all unique txid's inside transaction inputs (prevTxid)" +
                        " and outputs(transaction's txid)");
        group.addStat(stat);
    }

    private class DoGroup_4<V extends Void> extends GroupCallable<V> {

        DoGroup_4() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();

            UtxoSetChanges utxoSetChanges;
            try {
                utxoSetChanges = UtxoSetChanges.calcNewUtxoSetChanges(
                        block, 1, new UtxFastFactory());
            } catch (BitcoinUtxoSetChangesException e) {
                throw new IOException(e);
            }

            /* Calculate the utx and stx counts. Those hold the unique txids */
            int uniqueTxidsCount = utxoSetChanges.getTxidCount();

            /* Update log txCount. Nothing else for consistent concurrency. */
            MathAverage uniqueTxidsAvg = currentLog.uniqueTxidCount;
            uniqueTxidsAvg.push(uniqueTxidsCount);

            /* Compute and update entries */
            Stat stat = getStat(4001);
            if(stat.isActive()) {
                EntryIntLong entryIntLong = new EntryIntLong(
                        stat.getId(), height, uniqueTxidsCount);
                appendEntry(entryIntLong);
            }

            stat = getStat(4002);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, uniqueTxidsAvg.getAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(4003);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, uniqueTxidsAvg.getDev());
                appendEntry(entryIntDouble);
            }

            stat = getStat(4004);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, uniqueTxidsAvg.getMovingAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(4005);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, uniqueTxidsAvg.getMovingDev());
                appendEntry(entryIntDouble);
            }

            return null;
        }
    }



    /**
     * ###################################################################
     * ############################# Group 5 #############################
     * ################################################################### */
    private static final int SHARDS_INVOLVED_COUNT_MOV_AVG_LEN = 200;
    private static final int[] SHARD_NUMS = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
            131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432,
            67108864, 134217728, 268435456, 536870912};
    static {
        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 5;
        int statId = 5000;

        String desc_list = "";
        for(int i=0; i<SHARD_NUMS.length; i++) {
            //noinspection StringConcatenationInLoop
            desc_list = desc_list + SHARD_NUMS[i] + ", ";
        }

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "block_shard_involved_count",
                "Block, shards involved to fully verify this block. " +
                        "Tested for shard numbers: " + desc_list + ".");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "block_shard_involved_avg_since_genesis",
                "Block, average of shards involved to fully verify this block since genesis. " +
                        "Tested for shard numbers: " + desc_list + ".");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "block_shard_involved_var_since_genesis",
                "Block, variance of shards involved to fully verify this block since genesis. " +
                        "Tested for shard numbers: " + desc_list + ".");
        group.addStat(stat);

        stat = new Stat(statId+4, groupId, activeGroup && true,
                "block_shard_involved_avg_last_" + SHARDS_INVOLVED_COUNT_MOV_AVG_LEN,
                "Block, average of shards involved to fully verify this block for last " +
                        SHARDS_INVOLVED_COUNT_MOV_AVG_LEN + " blocks." +
                        "Tested for shard numbers: " + desc_list + ".");
        group.addStat(stat);

        stat = new Stat(statId+5, groupId, activeGroup && true,
                "block_shard_involved_var_last_" + SHARDS_INVOLVED_COUNT_MOV_AVG_LEN,
                "Block, variance of shards involved to fully verify this block for last " +
                        SHARDS_INVOLVED_COUNT_MOV_AVG_LEN + " blocks." +
                        "Tested for shard numbers: " + desc_list + ".");
        group.addStat(stat);
    }

    private class DoGroup_5<V extends Void> extends GroupCallable<V> {

        DoGroup_5() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();

            long[] shardsInvolvedCount = new long[SHARD_NUMS.length];
            MathAverage[] shardsInvolvedAvg = currentLog.shardsInvolved;

            /* Get utxo set changes for 1 shard */
            UtxoSetChanges utxoSetChanges;
            try {
                utxoSetChanges = UtxoSetChanges.calcNewUtxoSetChanges(
                        block, 1, new UtxFastFactory());
            } catch (BitcoinUtxoSetChangesException e) {
                throw new IOException(e);
            }


            /* For each shard number count modified shards */
            for(int i=0; i<SHARD_NUMS.length; i++) {
                try {
                    UtxoSetChangesCardinalities cardinalities =
                            UtxoSetChanges.calcUtxoSetChangesCardinalities(
                                    block, SHARD_NUMS[i], utxoSetChanges);

                    shardsInvolvedCount[i] = cardinalities.modifiedShardCount;

                } catch (BitcoinUtxoSetChangesException e) {
                    //Should not throw any since utxochanges is already calculated.
                }
            }

            /* Update averages and get values */
            double[] shardsInvolvedAvgs = new double[SHARD_NUMS.length];
            double[] shardsInvolvedDevs = new double[SHARD_NUMS.length];
            double[] shardsInvolvedMovAvgs = new double[SHARD_NUMS.length];
            double[] shardsInvolvedMovDevs = new double[SHARD_NUMS.length];
            for(int i=0; i<SHARD_NUMS.length; i++) {
                shardsInvolvedAvg[i].push(shardsInvolvedCount[i]);
                shardsInvolvedAvgs[i] = shardsInvolvedAvg[i].getAvg();
                shardsInvolvedDevs[i] = shardsInvolvedAvg[i].getDev();
                shardsInvolvedMovAvgs[i] = shardsInvolvedAvg[i].getMovingAvg();
                shardsInvolvedMovDevs[i] = shardsInvolvedAvg[i].getMovingDev();
            }


            /* Compute and update entries */
            Stat stat = getStat(5001);
            if(stat.isActive()) {
                EntryIntLongArray entry = new EntryIntLongArray(
                        stat.getId(), height, shardsInvolvedCount);
                appendEntry(entry);
            }

            stat = getStat(5002);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardsInvolvedAvgs);
                appendEntry(entry);
            }

            stat = getStat(5003);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardsInvolvedDevs);
                appendEntry(entry);
            }

            stat = getStat(5004);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardsInvolvedMovAvgs);
                appendEntry(entry);
            }

            stat = getStat(5005);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardsInvolvedMovDevs);
                appendEntry(entry);
            }

            return null;
        }
    }





    /**
     * ###################################################################
     * ############################# Group 6 #############################
     * ################################################################### */
    static {
        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 6;
        int statId = 6000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "block_height_timestamp",
                "Block heights and Unix timestamps.");
        group.addStat(stat);
    }

    private class DoGroup_6<V extends Void> extends GroupCallable<V> {

        DoGroup_6() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();
            long timestamp = block.getTime();


            /* Compute and update entries */
            Stat stat = getStat(6001);
            if(stat.isActive()) {
                EntryIntLong entry = new EntryIntLong(stat.getId(), height, timestamp);
                appendEntry(entry);
            }

            return null;
        }
    }


    /**
     * ###################################################################
     * ############################# Group 7 #############################
     * ################################################################### */
    static {
        /* Group parameters */
        boolean activeGroup = true;
        int groupId = 7;
        int statId = 7000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "block_size",
                "Blocks size.");
        group.addStat(stat);
    }

    private class DoGroup_7<V extends Void> extends GroupCallable<V> {

        DoGroup_7() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Compute statistics */
            int height = block.getHeight();
            long size = block.getSerializedSize();


            /* Compute and update entries */
            Stat stat = getStat(7001);
            if(stat.isActive()) {
                EntryIntLong entry = new EntryIntLong(stat.getId(), height, size);
                appendEntry(entry);
            }

            return null;
        }
    }
}
