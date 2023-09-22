package Blockchainj.Blockchain.Statistics;

import Blockchainj.Bitcoin.*;
import Blockchainj.Bitcoin.RPC.BitcoinRpcException;
import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Blockchain.Blockchain;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.*;
import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Blockchain.UtxoSet.UTXOS.ShardChanges;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxFastFactory;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxoSetChanges;
import Blockchainj.Util.MathAverage;
import Blockchainj.Util.MerkleTree;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * StatisticsUtxoSet
 *
 * Takes ready utxo set.
 * This threads writes on the utxo set. So it's best if no other thread writes on it.
 * If utxo set's height and statistics log's height don't match an exception is thrown.
 * If the utxo set's Shard and UTX types don't contain the necessary auxiliary data/metadata
 * the statistic groups that need it will either fail or ignore it.
 *
 * Creates appropriately parameterized Blockchainj.Blockchain.
 * For each Blockchainj.Blockchain.doCycles, this class computes the active statistics.
 *
 */

@SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
public class StatisticsUtxoSet extends Statistics {
    /* Stats file names */
    private static final String statInfoFilename = "stat_utxoset_info.log";
    private static final String statLogFilename = "stat_utxoset_log.log";

    /* Stat info */
    private String STAT_INFO_HEADER = "Utxo Set statistics.";
    private static final LinkedHashMap<Integer, StatGroup> INIT_GROUPS= new LinkedHashMap<>();

    /* Best current log */
    private final LogUtxoSet currentLog;
    public static final int UNDEFINED_HEIGHT = ProtocolParams.UNDEFINED_HEIGHT;


    /* Timer */
    private final StatisticsUtxoSetTimer timer;
    private static final boolean activeTimer = true;

    /* Main variables */
    private final Blockchain blockchain;
    private final UtxoSet utxoSet;


    /* Main constructor */
    public StatisticsUtxoSet(String statsPath,
                             UtxoSet utxoSet,
                             BlockBuffer blockBuffer,
                             boolean STATISTICS_DO_CONCURRENT,
                             int STATISTICS_THREAD_NUM)
            throws IOException {
        super(statsPath,
                statInfoFilename,
                statLogFilename,
                INIT_GROUPS,
                STATISTICS_DO_CONCURRENT,
                STATISTICS_THREAD_NUM);

        /* Set utxo set */
        this.utxoSet = utxoSet;

        /* Read/init log */
        LogUtxoSet tempLog;
        try {
            /* Create new empty log and try to sync it */
            tempLog = new LogUtxoSet();
            readLog(tempLog);
        } catch (FileNotFoundException e) {
            /* Create and init log */
            tempLog = new LogUtxoSet(UNDEFINED_HEIGHT);
        }
        currentLog = tempLog;


        /* Compare log height to utxo set height. They have to match */
        int utxoSetHeight = this.utxoSet.getBestHeight();
        int logHeight = currentLog.height;
        boolean bothUndefined =
                (utxoSetHeight == UtxoSet.UNDEFINED_HEIGHT) && (logHeight == UNDEFINED_HEIGHT);
        boolean bothEqual = (utxoSetHeight == logHeight);
        if( !bothUndefined && !bothEqual ) {
            throw new IllegalArgumentException(
                    "Utxo Set height does not match with Stat Log height");
        }

        /* Create blockchain */
        blockchain = new Blockchain(utxoSet, blockBuffer);
        setPRINT_STREAM(PRINTSTREAM);
        setPRINT_PERIOD(PRINT_PERIOD);

        /* Init timer */
        if(activeTimer) {
            this.timer = new StatisticsUtxoSetTimer();
        } else {
            this.timer = null;
        }
    }


    @Override
    public void setPRINT_STREAM(PrintStream printStream) {
        super.setPRINT_STREAM(printStream);
        blockchain.setPRINT_STREAM(printStream);
    }

    /* Set print period parameters */
    @Override
    public void setPRINT_PERIOD(int printPeriod) {
        super.setPRINT_PERIOD(printPeriod);
        blockchain.setPRINT_PERIOD(printPeriod);
    }


    @Override
    public boolean doCycle() throws IOException {
        /* TIMER */
        if(activeTimer) {
            //noinspection ConstantConditions
            timer.startRound();
            timer.startTimerForStage(StatisticsUtxoSetTimer.blockchain);
        }

        try {
            /* Perform a blockchain cycle */
            try {
                if (!blockchain.doCycle()) {
                    return false;
                }
            } catch (BitcoinUtxoSetException | BitcoinRpcException | BitcoinBlockException e) {
                throw new IOException(e);
            }

            /* TIMER */
            if (activeTimer) {
                timer.endTimerForStage1startTimerForStage2(
                        StatisticsUtxoSetTimer.blockchain, StatisticsUtxoSetTimer.stats);
            }

            /* Perform computation and storage from callables.
             * super.doCycle() always returns true or throws IOException. */
            super.doCycle();

            /* Update log. */
            currentLog.height = utxoSet.getBestHeight();

            /* TIMER */
            if (activeTimer) {
                timer.endTimerForStage(StatisticsUtxoSetTimer.stats);
            }
        } finally {
            /* TIMER */
            if(activeTimer) {
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

        try {
            if(PRINTSTREAM != null) {
                PRINTSTREAM.println("Storing log to disk...");
            }

            storeLog(currentLog);

            if(PRINTSTREAM != null) {
                PRINTSTREAM.println("Log stored to disk. Closing blockchain set...");
            }

            blockchain.close();

            if(PRINTSTREAM != null) {
                PRINTSTREAM.println("Blockchainj.Blockchain closed.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void writeStatInfo() throws IOException {
        writeStatInfo(STAT_INFO_HEADER);
    }


    public int getBestHeight() { return currentLog.height; }


    /* Log entry */
    protected class LogUtxoSet extends Log {
        int height;
        /* group 1*/
        MathAverage utxoCount;
        MathAverage utxCount;

        /* group 2*/
        MathAverage[] utxoSetSizes;
        MathAverage[] shardAvgSizes;
        double[] shardAvgSizesBytes; //Produced by group2
        ReentrantReadWriteLock group2Lock = new ReentrantReadWriteLock();

        LogUtxoSet() {
            utxoSetSizes = new MathAverage[GROUP2_SHARD_NUMS.length];
            shardAvgSizes = new MathAverage[GROUP2_SHARD_NUMS.length];
            shardAvgSizesBytes = new double[GROUP2_SHARD_NUMS.length];
        }

        LogUtxoSet(int height) {
            this.height = height;
            utxoCount = new MathAverage(UTXO_UTXOSET_COUNT_MOV_AVG_LEN);
            utxCount = new MathAverage(UTX_UTXOSET_COUNT_MOV_AVG_LEN);

            utxoSetSizes = new MathAverage[GROUP2_SHARD_NUMS.length];
            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                utxoSetSizes[i] = new MathAverage(GROUP2_UTXOSET_SIZE_MOV_AVG_LEN);
            }

            shardAvgSizes = new MathAverage[GROUP2_SHARD_NUMS.length];
            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                shardAvgSizes[i] = new MathAverage(GROUP2_UTXOSET_SIZE_MOV_AVG_LEN);
            }

            shardAvgSizesBytes = new double[GROUP2_SHARD_NUMS.length];
            Arrays.fill(shardAvgSizesBytes, 0);
        }

        @Override
        void store(OutputStream outputStream) throws IOException {
            BitcoinParams.INT32ToOutputStream(height, outputStream);
            utxoCount.store(outputStream);
            utxCount.store(outputStream);

            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                utxoSetSizes[i].store(outputStream);
            }

            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                shardAvgSizes[i].store(outputStream);
            }

            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                MathAverage.storeDOUBLE(shardAvgSizesBytes[i], outputStream);
            }
        }

        @Override
        void load(InputStream inputStream) throws IOException {
            height = BitcoinParams.readINT32(inputStream);
            utxoCount = MathAverage.load(inputStream);
            utxCount = MathAverage.load(inputStream);

            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                utxoSetSizes[i] = MathAverage.load(inputStream);
            }

            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                shardAvgSizes[i] = MathAverage.load(inputStream);
            }

            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                shardAvgSizesBytes[i] = MathAverage.loadDOUBLE(inputStream);
            }
        }

        void print(PrintStream printStream) {
            printStream.println(">LogStatic: ");
            printStream.println("Height: " + height);
        }
    }


    /* Prints parameters */
    @Override
    public void printParameters(PrintStream printStream) {
        super.printParameters(printStream);
        printStream.println(">StatisticsUtxoSet parameters:");
        printStream.println("StatisticsUtxoSet active timer: " + activeTimer);
        blockchain.printParameters(printStream);
    }


    @Override
    public void print(PrintStream printStream) {
        super.print(printStream);
        printStream.println(">StatisticsBlocks: ");
        currentLog.print(printStream);
        blockchain.print(printStream);
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

            default: return null;
        }
    }


    /**
     * ###################################################################
     * ############################# Group 1 #############################
     * ################################################################### */
    private static final int UTXO_UTXOSET_COUNT_MOV_AVG_LEN = 200;
    private static final int UTX_UTXOSET_COUNT_MOV_AVG_LEN = 200;
    static {
        /* Utxo and Utx count.
         * This group is shard num independent */

        /* Group parameters */
        boolean activeGroup = true;
        int groupId = 1;
        int statId = 1000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "utxoset_utxo_count",
                "Utxo set's utxo count.");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "utxoset_utxo_avg_since_genesis",
                "Utxo set's average of utxo count since genesis.");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "utxoset_utxo_var_since_genesis",
                "Utxo set's variance of utxo count since genesis.");
        group.addStat(stat);

        stat = new Stat(statId+4, groupId, activeGroup && true,
                "utxoset_utxo_avg_last_" + UTXO_UTXOSET_COUNT_MOV_AVG_LEN,
                "Utxo set's average of utxo count for last " + UTXO_UTXOSET_COUNT_MOV_AVG_LEN
                        + " blocks.");
        group.addStat(stat);

        stat = new Stat(statId+5, groupId, activeGroup && true,
                "utxoset_utxo_var_last_" + UTXO_UTXOSET_COUNT_MOV_AVG_LEN,
                "Utxo set's variance of utxo count for last " + UTXO_UTXOSET_COUNT_MOV_AVG_LEN
                        + " blocks.");
        group.addStat(stat);


        stat = new Stat(statId+6, groupId, activeGroup && true,
                "utxoset_utx_count",
                "Utxo set's utx count.");
        group.addStat(stat);

        stat = new Stat(statId+7, groupId, activeGroup && true,
                "utxoset_utx_avg_since_genesis",
                "Utxo set's average of utx count since genesis.");
        group.addStat(stat);

        stat = new Stat(statId+8, groupId, activeGroup && true,
                "utxoset_utx_var_since_genesis",
                "Utxo set's variance of utx count since genesis.");
        group.addStat(stat);

        stat = new Stat(statId+9, groupId, activeGroup && true,
                "utxoset_utx_avg_last_" + UTX_UTXOSET_COUNT_MOV_AVG_LEN,
                "Utxo set's average of utx count for last " + UTX_UTXOSET_COUNT_MOV_AVG_LEN
                        + " blocks.");
        group.addStat(stat);

        stat = new Stat(statId+10, groupId, activeGroup && true,
                "utxoset_utx_var_last_" + UTX_UTXOSET_COUNT_MOV_AVG_LEN,
                "Utxo set's variance of utx count for last " + UTX_UTXOSET_COUNT_MOV_AVG_LEN
                        + " blocks.");
        group.addStat(stat);
    }

    private class DoGroup_1<V extends Void> extends GroupCallable<V> {

        DoGroup_1() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Get utxo set height */
            int height = utxoSet.getBestHeight();
            int utxoCount = utxoSet.getUtxoCount();
            int utxCount = utxoSet.getUtxCount();

            /* Get avg counters */
            MathAverage utxoAvg = currentLog.utxoCount;
            MathAverage utxAvg = currentLog.utxCount;

            /* Update log avg counters. Nothing else for consistent concurrency. */
            utxoAvg.push(utxoCount);
            utxAvg.push(utxCount);

            /* Compute and update entries */
            Stat stat = getStat(1001);
            if(stat.isActive()) {
                EntryIntLong entryIntLong = new EntryIntLong(
                        stat.getId(), height, utxoCount);
                appendEntry(entryIntLong);
            }

            stat = getStat(1002);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxoAvg.getAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1003);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxoAvg.getDev());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1004);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxoAvg.getMovingAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1005);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxoAvg.getMovingDev());
                appendEntry(entryIntDouble);
            }



            stat = getStat(1006);
            if(stat.isActive()) {
                EntryIntLong entryIntLong = new EntryIntLong(
                        stat.getId(), height, utxCount);
                appendEntry(entryIntLong);
            }

            stat = getStat(1007);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxAvg.getAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1008);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxAvg.getDev());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1009);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxAvg.getMovingAvg());
                appendEntry(entryIntDouble);
            }

            stat = getStat(1010);
            if(stat.isActive()) {
                EntryIntDouble entryIntDouble = new EntryIntDouble(
                        stat.getId(), height, utxAvg.getMovingDev());
                appendEntry(entryIntDouble);
            }

            return null;
        }
    }



    /**
     * ###################################################################
     * ############################# Group 2 #############################
     * ################################################################### */
    private static final int GROUP2_UTXOSET_SIZE_MOV_AVG_LEN = 200;
    private static final int[] GROUP2_SHARD_NUMS = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
            131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432,
            67108864};
    static {
        /* Utxo Set serialized size for various shard numbers.
         * Also Avg Shard size. */

        /* Group parameters */
        boolean activeGroup = true;
        int groupId = 2;
        int statId = 2000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "utxoset_size",
                "Utxo set's size in bytes.");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "utxoset_size_avg_last_" + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN,
                "Utxo set's average of size for last " + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN +
                        " blocks.");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "utxoset_size_var_last_" + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN,
                "Utxo set's variance of size for last " + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN +
                        " blocks.");
        group.addStat(stat);

        /* Avg shard size */
        stat = new Stat(statId+4, groupId, activeGroup && true,
                "utxoset_shard_avg_size",
                "Utxo set's average shard size in bytes.");
        group.addStat(stat);

        stat = new Stat(statId+5, groupId, activeGroup && false,
                "utxoset_shard_avg_size_avg_last_" + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN,
                "Utxo set's average of shard size for last " + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN +
                        " blocks.");
        group.addStat(stat);

        stat = new Stat(statId+6, groupId, activeGroup && false,
                "utxoset_shard_avg_size_var_last_" + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN,
                "Utxo set's variance of shard size for last " + GROUP2_UTXOSET_SIZE_MOV_AVG_LEN +
                        " blocks.");
        group.addStat(stat);
    }

    private class DoGroup_2<V extends Void> extends GroupCallable<V> {

        DoGroup_2() {
            super();
        }

        @Override
        public V call() throws IOException {
            /* Get utxo set height */
            int height = utxoSet.getBestHeight();

            /* Get avgs */
            MathAverage[] utxoSetSizesAvg = currentLog.utxoSetSizes;

            /* Make new sizes array for various shard numbers */
            long[] utxoSetSizes = new long[GROUP2_SHARD_NUMS.length];
            double[] utxoSetSizesMovAvg = new double[GROUP2_SHARD_NUMS.length];
            double[] utxoSetSizesMovDev = new double[GROUP2_SHARD_NUMS.length];

            /* For each shard number calculate the serialized size */
            for(int i=0; i<GROUP2_SHARD_NUMS.length; i++) {
                int shardNum = GROUP2_SHARD_NUMS[i];

                /* Utxs size and shardheader estimated size */
                utxoSetSizes[i] = utxoSet.getUtxoSetSerializedSizeEstimate(shardNum);

                /* Update avgs */
                utxoSetSizesAvg[i].push(utxoSetSizes[i]);
                utxoSetSizesMovAvg[i] = utxoSetSizesAvg[i].getMovingAvg();
                utxoSetSizesMovDev[i] = utxoSetSizesAvg[i].getMovingDev();
            }


            /* Compute average shard sizes */
            /* init */
            double[] shardAvgSizes = currentLog.shardAvgSizesBytes;
            double[] shardAvgSizesAvgs = new double[GROUP2_SHARD_NUMS.length];
            double[] shardAvgSizesDevs = new double[GROUP2_SHARD_NUMS.length];
            MathAverage[] shardAvgSizesAvg = currentLog.shardAvgSizes;

            /* Get group2 lock. For shardAvgs. */
            currentLog.group2Lock.writeLock().lock();
            try {
                /* for each shard size */
                for (int i = 0; i < GROUP2_SHARD_NUMS.length; i++) {
                    int shardNum = GROUP2_SHARD_NUMS[i];

                    double avgShardSize = (double)utxoSetSizes[i] / (double)shardNum;

                    /* Update values */
                    shardAvgSizes[i] = avgShardSize;
                    shardAvgSizesAvg[i].push(avgShardSize);
                    shardAvgSizesAvgs[i] = shardAvgSizesAvg[i].getMovingAvg();
                    shardAvgSizesDevs[i] = shardAvgSizesAvg[i].getMovingDev();
                }
            } finally {
                currentLog.group2Lock.writeLock().unlock();
            }


            /* Compute and update entries
             * Utxo set sizes. */
            Stat stat = getStat(2001);
            if(stat.isActive()) {
                EntryIntLongArray entry = new EntryIntLongArray(
                        stat.getId(), height, utxoSetSizes);
                appendEntry(entry);
            }

            stat = getStat(2002);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, utxoSetSizesMovAvg);
                appendEntry(entry);
            }

            stat = getStat(2003);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, utxoSetSizesMovDev);
                appendEntry(entry);
            }

            /* shard average sizes */
            stat = getStat(2004);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardAvgSizes);
                appendEntry(entry);
            }

            stat = getStat(2005);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardAvgSizesAvgs);
                appendEntry(entry);
            }

            stat = getStat(2006);
            if(stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardAvgSizesDevs);
                appendEntry(entry);
            }


            return null;
        }
    }



    /**
     * ###################################################################
     * ############################# Group 3 #############################
     * ################################################################### */
    private static final int GROUP3_PERIOD = 50000; //Intensive stats
    private static final int[] GROUP3_SHARD_NUMS = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
            131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432,
            67108864 };
    static {
        /* Average shard serialized size per height for various shard numbers.
         * Calculated on actual UTXOs. */

        /* Group parameters */
        boolean activeGroup = true;
        int groupId = 3;
        int statId = 3000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "utxoset_shard_real_avg_size",
                "Utxo set's average shard size in bytes. Calculated on actual UTXOs.");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "utxoset_shard_real_var_size",
                "Utxo set's variance of shard size in bytes.  Calculated on actual UTXOs.");
        group.addStat(stat);
    }

    private class DoGroup_3<V extends Void> extends GroupCallable<V> {

        DoGroup_3() { super(); }

        @Override
        public V call() throws IOException {
            /* Get utxo set height */
            int height = utxoSet.getBestHeight();

            if(height%GROUP3_PERIOD != 0) {
                return null;
            }

            /* Get math average and init current avg */
            //MathAverage[] shardAvgSizesAvg = currentLog.shardAvgSizes;
            double[] shardAvgSizes = new double[GROUP3_SHARD_NUMS.length];
            double[] shardVarSizes = new double[GROUP3_SHARD_NUMS.length];

            /* for each shard size */
            for (int i = 0; i < GROUP3_SHARD_NUMS.length; i++) {
                int shardNum = GROUP3_SHARD_NUMS[i];

                /* create new temp shard avg */
                MathAverage tempShardsAvg = new MathAverage();

                /* Get shard iterator for this shardnum */
                Iterator<Shard> shardIterator = utxoSet.getShardIterator(shardNum);

                /* For each shard calculated serialized size */
                while(shardIterator.hasNext()) {
                    /* Get next shard */
                    Shard shard = shardIterator.next();

                    /* Add shard serialized size to accumulator */
                    tempShardsAvg.push(shard.getSerializedSize());
                }

                /* Update values */
                shardAvgSizes[i] = tempShardsAvg.getAvg();
                shardVarSizes[i] = tempShardsAvg.getDev();
            }

            /* Compute and update entries */
            Stat stat = getStat(3001);
            if (stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardAvgSizes);
                appendEntry(entry);
            }

            stat = getStat(3002);
            if (stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, shardVarSizes);
                appendEntry(entry);
            }

            return null;
        }
    }



    /**
     * ###################################################################
     * ############################# Group 4 #############################
     * ################################################################### */
    private static final int GROUP4_PERIOD = 500; //Intensive task.
    private static final int[] GROUP4_SHARD_NUMS = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
            131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432,
            67108864 };
    static {
        /* Bandwidth needed for full block verification.
         * This consists of sum of shard's sizes and merkle tree missing hashes.
         * Calculated on actual data.
         *
         * But the calculation depends to much on the block. Since this calculation is
         * intensive and a large period may be applied it makes no sense to do a sparse samlping.
         * There for this Group is mainly a control group. */

        /* Group parameters */
        boolean activeGroup = false;
        int groupId = 4;
        int statId = 4000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "utxoset_block_real_full_ver_full_size",
                "Size of all data (bytes) for full block verification.");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "utxoset_block_real_full_ver_shards_size",
                "Size of shard data (bytes) for full block verification.");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "utxoset_block_real_full_ver_hashes_size",
                "Size of missing merkle tree hashes data in (bytes) for full block verification.");
        group.addStat(stat);
    }

    private class DoGroup_4<V extends Void> extends GroupCallable<V> {

        DoGroup_4() { super(); }

        @Override
        public V call() throws IOException {
            /* Get utxo set height */
            int height = utxoSet.getBestHeight();

            if(height%GROUP4_PERIOD != 0) {
                return null;
            }

            /* Get latest block */
            Block block = blockchain.getLatestBlock();
            if(block == null) {
                throw new IOException(new NullPointerException());
            }

            /* init full block verification data sizes */
            long[] fullVerShardDataSizes = new long[GROUP4_SHARD_NUMS.length];
            long[] fullVerHashsesDataSizes = new long[GROUP4_SHARD_NUMS.length];
            long[] fullVerAllDataSizes = new long[GROUP4_SHARD_NUMS.length];

            /* for each shard size */
            for(int i=0; i<GROUP4_SHARD_NUMS.length; i++) {
                int shardNum = GROUP4_SHARD_NUMS[i];

                /* Get utxo set changes */
                UtxoSetChanges changes;
                try {
                    changes = UtxoSetChanges.calcNewUtxoSetChanges(
                            block, shardNum, new UtxFastFactory());
                } catch (BitcoinUtxoSetException e) {
                    throw new IOException(e);
                }

                /* init sizes */
                fullVerShardDataSizes[i] = 0;
                fullVerHashsesDataSizes[i] = 0;
                fullVerAllDataSizes[i] = 0;

                /* Init modified Shard indices */
                int[] modifiedShardIndices = new int[changes.getModifiedShardCount()];

                /* Get shard changes iterator */
                Iterator<ShardChanges> shardChangesIterator =
                        changes.getSortedShardChangesIterator();

                /* For each modified shard */
                for(int k=0; k<modifiedShardIndices.length; k++) {
                    /* Get index */
                    int shardIndex = shardChangesIterator.next().getShardIndex();

                    /* Set index to modified shard indices array */
                    modifiedShardIndices[k] = shardIndex;

                    /* Get shard */
                    Shard shard = utxoSet.getShard(shardNum, shardIndex);

                    /* Add shard serialized size to accumulator */
                    fullVerShardDataSizes[i] += shard.getSerializedSize();
                }

                /* Compute merkle tree missing hashes size */
                fullVerHashsesDataSizes[i] += MerkleTree.getSerializedSizeForMissingHashes(
                        modifiedShardIndices, shardNum);


                /* Compute all data size */
                fullVerAllDataSizes[i] += (fullVerShardDataSizes[i] + fullVerHashsesDataSizes[i]);
            }


            /* Compute and update entries */
            Stat stat = getStat(4001);
            if(stat.isActive()) {
                EntryIntLongArray entry = new EntryIntLongArray(
                        stat.getId(), height, fullVerAllDataSizes);
                appendEntry(entry);
            }

            stat = getStat(4002);
            if(stat.isActive()) {
                EntryIntLongArray entry = new EntryIntLongArray(
                        stat.getId(), height, fullVerShardDataSizes);
                appendEntry(entry);
            }

            stat = getStat(4003);
            if(stat.isActive()) {
                EntryIntLongArray entry = new EntryIntLongArray(
                        stat.getId(), height, fullVerHashsesDataSizes);
                appendEntry(entry);
            }

            return null;
        }
    }



    /**
     * ###################################################################
     * ############################# Group 5 #############################
     * ################################################################### */
    private static final int GROUP5_PERIOD = 1;
    private static final int[] GROUP5_SHARD_NUMS = GROUP2_SHARD_NUMS;
    static {
        /* Avergae bandwidth needed for full block verification.
         * Instead of calculation shard sizes, use group2's average shard size.
         * This consists of sum of shard's sizes and merkle tree missing hashes. */

        /* Group parameters */
        boolean activeGroup = true && INIT_GROUPS.get(2).isActive(); //group2 must be active
        int groupId = 5;
        int statId = 5000;

        /* Init group */
        StatGroup group = new StatGroup(groupId, activeGroup);
        INIT_GROUPS.put(group.getGroupId(), group);

        /* Init stats */
        Stat stat = new Stat(statId+1, groupId, activeGroup && true,
                "utxoset_block_full_ver_avg_full_size",
                "Size of all average data (bytes) for full block verification.");
        group.addStat(stat);

        stat = new Stat(statId+2, groupId, activeGroup && true,
                "utxoset_block_full_ver_avg_shards_size",
                "Size of average shard data (bytes) for full block verification.");
        group.addStat(stat);

        stat = new Stat(statId+3, groupId, activeGroup && true,
                "utxoset_block_full_ver_avg_hashes_size",
                "Size of missing merkle tree hashes data in (bytes) for full block verification.");
        group.addStat(stat);
    }

    private class DoGroup_5<V extends Void> extends GroupCallable<V> {

        DoGroup_5() { super(); }

        @Override
        public V call() throws IOException {
            /* Get utxo set height */
            int height = utxoSet.getBestHeight();

            if (height % GROUP5_PERIOD != 0) {
                return null;
            }

            /* Get latest block */
            Block block = blockchain.getLatestBlock();
            if (block == null) {
                throw new IOException(new NullPointerException());
            }

            /* init full block verification data sizes */
            double[] fullVerShardDataSizes = new double[GROUP5_SHARD_NUMS.length];
            long[] fullVerHashsesDataSizes = new long[GROUP5_SHARD_NUMS.length];
            double[] fullVerAllDataSizes = new double[GROUP5_SHARD_NUMS.length];

            /* get average shard sizes */
            double[] avgShardSizes = currentLog.shardAvgSizesBytes;

            /* for each shard size */
            for (int i = 0; i < GROUP5_SHARD_NUMS.length; i++) {
                int shardNum = GROUP5_SHARD_NUMS[i];

                /* Get utxo set changes */
                UtxoSetChanges changes;
                try {
                    changes = UtxoSetChanges.calcNewUtxoSetChanges(
                            block, shardNum, new UtxFastFactory());
                } catch (BitcoinUtxoSetException e) {
                    throw new IOException(e);
                }

                /* Get modified shard count */
                int modifiedShardCount = changes.getModifiedShardCount();

                /* Init modified shard indices */
                int[] modifiedShardIndices = new int[modifiedShardCount];

                /* For each shard change get shard index */
                Iterator<ShardChanges> shardChangesIterator =
                        changes.getSortedShardChangesIterator();
                for (int k = 0; k < modifiedShardIndices.length; k++) {
                    /* Get index */
                    modifiedShardIndices[k] = shardChangesIterator.next().getShardIndex();
                }

                /* Compute shards data size from average shard size */
                /* Get group3 lock */
                currentLog.group2Lock.readLock().lock();
                try {
                    fullVerShardDataSizes[i] = (double) modifiedShardCount * avgShardSizes[i];
                } finally {
                    currentLog.group2Lock.readLock().unlock();
                }

                /* Compute merkle tree missing hashes size */
                fullVerHashsesDataSizes[i] += MerkleTree.getSerializedSizeForMissingHashes(
                        modifiedShardIndices, shardNum);

                /* Compute all data */
                fullVerAllDataSizes[i] = fullVerShardDataSizes[i] +
                        (double) fullVerHashsesDataSizes[i];
            }


            /* Compute and update entries */
            Stat stat = getStat(5001);
            if (stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, fullVerAllDataSizes);
                appendEntry(entry);
            }

            stat = getStat(5002);
            if (stat.isActive()) {
                EntryIntDoubleArray entry = new EntryIntDoubleArray(
                        stat.getId(), height, fullVerShardDataSizes);
                appendEntry(entry);
            }

            stat = getStat(5003);
            if (stat.isActive()) {
                EntryIntLongArray entry = new EntryIntLongArray(
                        stat.getId(), height, fullVerHashsesDataSizes);
                appendEntry(entry);
            }

            return null;
        }
    }

}














//GROUP3
//private class ShardSerializedSize {
//    final int shardNum;
//    final int shardIndex;
//    long serializedSize;
//    int utxCount;
//
//    ShardSerializedSize(int shardNum, int shardIndex) {
//        this.shardNum = shardNum;
//        this.shardIndex = shardIndex;
//        utxCount = 0;
//        serializedSize = (new ShardDefault(shardNum, shardIndex, null)).getSerializedSize();
//    }
//
//    boolean inRange(UTX utx) {
//        return (shardIndex == ProtocolParams.calcShardIndex(shardNum, utx.getTxid()));
//    }
//
//    void addUtx(UTX utx) {
//        serializedSize += utx.getSerializedSize();
//        serializedSize -= (long)CompactSizeUInt.getSizeOf(utxCount);
//        utxCount++;
//        serializedSize += (long)CompactSizeUInt.getSizeOf(utxCount);
//    }
//
//    long getSerializedSize() { return serializedSize; }
//}
//
//                /* for each shard size */
//            for (int i = 0; i < GROUP3_SHARD_NUMS.length; i++) {
//        int shardNum = GROUP3_SHARD_NUMS[i];
//
//                /* create new temp shard avg */
//        MathAverage tempShardsAvg = new MathAverage();
//
//                /* For each shard calculated serialized size */
//        Iterator<UTX> utxIterator = utxoSet.getUtxIterator();
//        UTX utx = null;
//        for (int shardIndex = 0; shardIndex < shardNum; shardIndex++) {
//                /* init shard serialized size */
//        ShardSerializedSize shardSerializedSize =
//        new ShardSerializedSize(shardNum, shardIndex);
//
//                    /* get next utx and utxShardIndex */
//        int utxShardIndex = -1;
//        if (utx == null) {
//        if (utxIterator.hasNext()) {
//        utx = utxIterator.next();
//        }
//        }
//        if (utx != null) {
//        utxShardIndex = ProtocolParams.calcShardIndex(shardNum, utx.getTxid());
//        }
//
//                    /* while utx is within range */
//        while (utxShardIndex <= shardSerializedSize.shardIndex &&
//        (utxShardIndex != -1)) {
//                        /* Add utx to shard serialized size */
//        shardSerializedSize.addUtx(utx);
//
//                        /* get next utx */
//        if (utxIterator.hasNext()) {
//        utx = utxIterator.next();
//        utxShardIndex =
//        ProtocolParams.calcShardIndex(shardNum, utx.getTxid());
//        } else {
//        utx = null;
//        utxShardIndex = -1;
//        }
//        }
//
//                    /* Add shard serialized size to accumulator */
//        tempShardsAvg.push(shardSerializedSize.getSerializedSize());
//        }
//
//                /* Update values */
//        shardAvgSizes[i] = tempShardsAvg.getAvg();
//        shardVarSizes[i] = tempShardsAvg.getDev();
//        }










//GROUP4
//    /* for each shard size */
//            for(int i=0; i<GROUP4_SHARD_NUMS.length; i++) {
//        int shardNum = GROUP4_SHARD_NUMS[i];
//
//                /* Get utxo set changes */
//        UtxoSetChanges changes;
//        try {
//        changes = UtxoSetChanges.calcNewUtxoSetChanges(
//        block, shardNum, UTX.GET_UTX_TYPE());
//        } catch (BitcoinUtxoSetException e) {
//        throw new IOException(e);
//        }
//
//                /* Init modified shard indices */
//        int[] modifiedShardIndices = new int[changes.getModifiedShardCount()];
//
//                /* For each shard change get shard index */
//        Iterator<ShardChanges> shardChangesIterator =
//        changes.getSortedShardChangesIterator();
//        for(int k=0; k<modifiedShardIndices.length; k++) {
//                    /* Get index */
//        modifiedShardIndices[k] = shardChangesIterator.next().getShardIndex();
//        }
//
//                /* Calculate sizes */
//        calcSizesForShardsNumsForModifiedShards(
//        GROUP4_SHARD_NUMS,
//        fullVerShardDataSizes,
//        fullVerHashsesDataSizes,
//        fullVerAllDataSizes,
//        i,
//        modifiedShardIndices);
//        }
//
//
//private void calcSizesForShardsNumsForModifiedShards(
//        int[] SHARD_NUMS,
//        long[] fullVerShardDataSizes,
//        long[] fullVerHashsesDataSizes,
//        long[] fullVerAllDataSizes,
//        int shardNumIndex,
//        int[] modifiedShardIndices) {
//
//        int shardNum = SHARD_NUMS[shardNumIndex];
//
//        /* init sizes */
//        fullVerShardDataSizes[shardNumIndex] = 0;
//        fullVerHashsesDataSizes[shardNumIndex] = 0;
//        fullVerAllDataSizes[shardNumIndex] = 0;
//
//        /* For each modified shard calculated serialized size */
//        Iterator<UTX> utxIterator = utxoSet.getUtxIterator();
//        UTX utx = null;
//        for (int k = 0; k < modifiedShardIndices.length; k++) {
//            /* init shard serialized size */
//        ShardSerializedSize shardSerializedSize =
//        new ShardSerializedSize(shardNum, modifiedShardIndices[k]);
//
//            /* get next utx and utxShardIndex */
//        int utxShardIndex = -1;
//        if (utx == null) {
//        if (utxIterator.hasNext()) {
//        utx = utxIterator.next();
//        }
//        }
//        if (utx != null) {
//        utxShardIndex = ProtocolParams.calcShardIndex(shardNum, utx.getTxid());
//        }
//
//            /* while utx is less or equal range */
//        while (utxShardIndex <= shardSerializedSize.shardIndex && (utxShardIndex != -1)) {
//        if (utxShardIndex == shardSerializedSize.shardIndex) {
//                        /* Add utx to shard serialized size */
//        shardSerializedSize.addUtx(utx);
//        }
//
//                    /* get next utx */
//        if (utxIterator.hasNext()) {
//        utx = utxIterator.next();
//        utxShardIndex = ProtocolParams.calcShardIndex(shardNum, utx.getTxid());
//        } else {
//        utx = null;
//        utxShardIndex = -1;
//        }
//        }
//
//            /* Add shard serialized size to accumulator */
//        fullVerShardDataSizes[shardNumIndex] += shardSerializedSize.getSerializedSize();
//        }
//
//        /* Compute merkle tree missing hashes size */
//        fullVerHashsesDataSizes[shardNumIndex] += MerkleTree.getSerializedSizeForMissingHashes(
//        modifiedShardIndices, shardNum);
//
//            /* Compute all data size */
//        fullVerAllDataSizes[shardNumIndex] +=
//        (fullVerShardDataSizes[shardNumIndex] + fullVerHashsesDataSizes[shardNumIndex]);
//        }
