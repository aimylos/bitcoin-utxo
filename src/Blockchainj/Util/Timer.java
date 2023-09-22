package Blockchainj.Util;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;

public class Timer {
    /* Active marker */
    private boolean active;

    /* Time labels */
    protected static String DEFAULT_TOTAL_LABEL = "Total";
    protected String totalLabel;
    protected String[] labels;
    protected int labelCount;

    /* Special fields for concurrent timers. */
    protected int[] sumLabelIndices;

    /* Current round times */
    protected long currentRoundStart;
    protected long currentRoundEnd;
    protected long[] currentRoundTimes;

    /* Last round times */
    protected long lastRoundStart;
    protected long lastRoundTotal;
    protected long[] lastRoundTimes;

    /* Moving sum times */
    public static final int DEFAULT_MOVING_AVG_LEN = 100;
    protected final boolean doMovingSum;
    protected final int movingAvgLen;
    protected LinkedList<Long> movingAvgRoundStarts;
    protected LinkedList<Long> movingAvgRoundTotals;
    protected LinkedList<Long[]> movingAvgRoundTimes;
    protected long movingAvgRoundTotalsSum;
    protected long[] movingAvgRoundTimesSums;

    /* Cumulative times */
    protected long cumulativeStart = 0;
    protected long cumulativeTotal = 0;
    protected long[] cumulativeTimes;

    /* Round counter */
    protected long roundCounter = 0;

    /* Print */
    protected double MILLISPERSECOND = 1000.0;
    protected NumberFormat DECIMAL_FORMATTER = new DecimalFormat("#0.0000");
    protected int PRINT_STRING_LENGTH = 40;


    /* C1 */
    public Timer(String[] labels, String totalLabel, int[] sumLabelIndices, boolean active,
                 int movingAvgLen) {
        this.active = active;
        this.labels = labels;
        this.totalLabel = totalLabel;
        this.sumLabelIndices = sumLabelIndices;
        this.movingAvgLen = movingAvgLen;
        this.doMovingSum = (movingAvgLen>0);
        labelCount = labels.length;
        currentRoundTimes = new long[labelCount];
        lastRoundTimes = new long[labelCount];
        cumulativeTimes = new long[labelCount];
        Arrays.fill(cumulativeTimes, 0);


        if(doMovingSum) {
            movingAvgRoundStarts = new LinkedList<>();
            movingAvgRoundTotals = new LinkedList<>();
            movingAvgRoundTimes = new LinkedList<>();
            movingAvgRoundTotalsSum = 0;
            movingAvgRoundTimesSums = new long[labelCount];
            Arrays.fill(movingAvgRoundTimesSums, 0);
            for (int i = 0; i < movingAvgLen; i++) {
                movingAvgRoundStarts.add((long) 0);
                movingAvgRoundTotals.add((long) 0);
                Long[] ar = new Long[labelCount];
                Arrays.fill(ar, (long) 0);
                movingAvgRoundTimes.add(ar);
            }
        } else {
            movingAvgRoundStarts = null;
            movingAvgRoundTotals = null;
            movingAvgRoundTimes = null;
            movingAvgRoundTotalsSum = 0;
            movingAvgRoundTimesSums = null;
        }
    }

    public Timer(String[] labels, String totalLabel, int[] sumLabelIndices, boolean active) {
        this(labels, totalLabel, sumLabelIndices, active, DEFAULT_MOVING_AVG_LEN);
    }

//    public Timer(String[] labels){
//        this(labels, DEFAULT_TOTAL_LABEL, new int[0], true, DEFAULT_MOVING_AVG_LEN);
//    }
//
//    public Timer(String[] labels, int[] sumLabelIndices){
//        this(labels, DEFAULT_TOTAL_LABEL, sumLabelIndices, true, DEFAULT_MOVING_AVG_LEN);
//    }
//
//    public Timer(String[] labels, String totalLabel){
//        this(labels, totalLabel, new int[0], true, DEFAULT_MOVING_AVG_LEN);
//    }
//
//    public Timer(String[] labels, String totalLabel, int[] sumLabelIndices){
//        this(labels, totalLabel, sumLabelIndices, true, DEFAULT_MOVING_AVG_LEN);
//    }


    /* C2 */
    public Timer(String[] labels, String totalLabel, boolean active, int movingSumLen){
        this(labels, totalLabel, new int[0], active, movingSumLen);
    }

    public Timer(String[] labels, String totalLabel, boolean active){
        this(labels, totalLabel, new int[0], active, DEFAULT_MOVING_AVG_LEN);
    }


    /* C3 */
    public Timer(String[] labels, boolean active, int movingSumLen){
        this(labels, DEFAULT_TOTAL_LABEL, new int[0], active, movingSumLen);
    }

    public Timer(String[] labels, boolean active){
        this(labels, DEFAULT_TOTAL_LABEL, new int[0], active, DEFAULT_MOVING_AVG_LEN);
    }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public long getCumulativeTimeForStage(int stage) { return cumulativeTimes[stage]; }

    public long getTotalCumulativeTime() { return cumulativeTotal; }


    public void startRound() {
        if(!active)
            return;
        currentRoundEnd = 0;
        Arrays.fill(currentRoundTimes, 0);
        currentRoundStart = time();
    }

    public void endRound() {
        if(!active)
            return;

        currentRoundEnd = time();

        /* calculate last times */
        lastRoundStart = currentRoundStart;
        lastRoundTotal = currentRoundEnd - currentRoundStart;
        System.arraycopy(currentRoundTimes, 0, lastRoundTimes, 0, labelCount);

        /* update cumulative times */
        if(roundCounter == 0)
            cumulativeStart = currentRoundStart;
        cumulativeTotal += lastRoundTotal;
        for(int i=0; i<labelCount; i++) {
            cumulativeTimes[i] += lastRoundTimes[i];
        }

        /* update moving sum */
        if(doMovingSum) {
            movingAvgRoundStarts.remove();
            movingAvgRoundStarts.add(lastRoundStart);
            movingAvgRoundTotalsSum -= movingAvgRoundTotals.remove();
            movingAvgRoundTotalsSum += lastRoundTotal;
            movingAvgRoundTotals.add(lastRoundTotal);
            Long[] popedTimes = movingAvgRoundTimes.remove();
            Long[] pushTimes = new Long[labelCount];
            for (int i = 0; i < labelCount; i++) {
                pushTimes[i] = lastRoundTimes[i];
                movingAvgRoundTimesSums[i] -= popedTimes[i];
                movingAvgRoundTimesSums[i] += pushTimes[i];
            }
            movingAvgRoundTimes.add(pushTimes);
        }

        /* increment rounds */
        roundCounter++;
    }

    public void startTimerForStage(int stage) {
        if(!active)
            return;
        currentRoundTimes[stage] = time();
    }

    public void endTimerForStage(int stage) {
        if(!active)
            return;
        currentRoundTimes[stage] = time() - currentRoundTimes[stage];
    }

    public void endTimerForStage1startTimerForStage2(int stage1, int stage2) {
        long t = time();
        currentRoundTimes[stage1] = t - currentRoundTimes[stage1];
        currentRoundTimes[stage2] = t;
    }

    public void addFromTimerCumulativeTimes(Timer timer) {
        for(int i=0; i<sumLabelIndices.length; i++) {
            currentRoundTimes[sumLabelIndices[i]] += timer.getCumulativeTimeForStage(i);
        }
    }


    public void print(PrintStream printStream, boolean doLastTimes, boolean doMovingSum,
                      boolean doCumulativeTimes) {
        if(!active)
            return;
        if(doLastTimes || doMovingSum || doCumulativeTimes) {
            if(doLastTimes)
                printTime(printStream, "Last Round " + roundCounter + " " + totalLabel,
                        lastRoundTotal);
            if(doMovingSum && this.doMovingSum)
                printTime(printStream, "Moving sum of " + movingAvgLen + " at round " +
                        roundCounter + " " + totalLabel, movingAvgRoundTotalsSum);
            if(doCumulativeTimes)
                printTime(printStream, "Cumulative " + roundCounter + " " + totalLabel,
                        cumulativeTotal);
            printStream.print("\n");

            for(int i=0; i<labelCount; i++) {
                if(doLastTimes)
                    printTime(printStream, "Last " + labels[i], lastRoundTimes[i]);
                if(doMovingSum && this.doMovingSum)
                    printTime(printStream, "MvgSum " + labels[i], movingAvgRoundTimesSums[i]);
                if(doCumulativeTimes)
                    printTime(printStream, "Total " + labels[i], cumulativeTimes[i]);
                printStream.print("\n");
            }
        }
    }

    private void printTime(PrintStream printStream, String label, long timeInMillis) {
        String seconds = DECIMAL_FORMATTER.format( timeInMillis / MILLISPERSECOND );
        String out = String.format("%s: %s", label, seconds);
        printStream.print(String.format("%1$-" + PRINT_STRING_LENGTH + "s", out));
    }

    public static long time() {
        return System.currentTimeMillis();
    }
}
