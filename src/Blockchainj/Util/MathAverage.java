package Blockchainj.Util;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *  MathAverage
 *
 *  This class maintains an average, squared deviation, moving avergae and a moving squared
 *  deviation of data being pushed into it.
 *
 *  Average is the statistic mean, m: m = Sum(x_i, over i)/n, where i=0 to n-1
 *  Square deviation is the statistic variance, s: s = Sum(x_i^2, over i)/n - m^2
 *
 *  The class uses doube to store it's values but they can be casted to
 *  other types as long as there's no overflow.
 *
 *  For a moving average with a trail of length l the first k=1 to l-1 cycles
 *  of the moving average is calculated using k as the trail's length,
 *  so that numbers make sense. From cycle l and beyond the average is calculated normally.
 */

public class MathAverage {
    /* Sum and square sum of all values */
    private double sum;
    private double sumOfSquares;

    /* Sum and square sum of trail's values */
    private double movingSum;
    private double movingSumOfSquares;

    /* Trail of values this moving average holds */
    private final int trailLen;
    private final LinkedList<Double> trail;

    /* Counter of data values that have been pushed */
    private long pushCounter;


    /* Main constructor that include moving average */
    public MathAverage(int trailLength) {
        /* init parameters */
        trailLen = trailLength;

        /* init variables */
        sum = 0;
        sumOfSquares = 0;
        movingSum = 0;
        movingSumOfSquares = 0;
        pushCounter = 0;

        /* init trail with 0s */
        if(trailLen > 0) {
            trail = new LinkedList<>();
            for (int i = 0; i < trailLen; i++) {
                //noinspection UnnecessaryBoxing
                trail.add(new Double(0));
            }
        } else {
            trail = null;
        }
    }

    /* Main constructor that does not include moving average */
    public MathAverage() {
        this(0);
    }


    /* Private use only */
    private MathAverage(InputStream inputStream) throws IOException {
        sum = loadDOUBLE(inputStream);
        sumOfSquares = loadDOUBLE(inputStream);
        movingSum = loadDOUBLE(inputStream);
        movingSumOfSquares = loadDOUBLE(inputStream);
        pushCounter = loadINT64(inputStream);
        trailLen = loadINT32(inputStream);

        if(trailLen > 0) {
            trail = new LinkedList<>();

            for(int i=0; i<trailLen; i++) {
                //noinspection UnnecessaryBoxing
                trail.add(new Double(loadDOUBLE(inputStream)));
            }
        } else {
            trail = null;
        }
    }


    public int getTrailLen() { return trailLen; }


    /* Push a new value at the end of the moving average */
    public void push(double head) {
        /* Compute squared head */
        double headSqared = head * head;

        /* Update average */
        sum += head;
        sumOfSquares += headSqared;

        /* Update moving average */
        if(trail != null) {
            /* Pop tail */
            double tail = trail.remove();

            /* Push head */
            //noinspection UnnecessaryBoxing
            trail.add(new Double(head));

            /* Update variables */
            movingSum -= tail;
            movingSum += head;

            movingSumOfSquares -= (tail*tail);
            movingSumOfSquares += headSqared;
        }

        pushCounter++;
    }

    public void push(int head) { push((double)head); }

    public void push(float head) { push((double)head); }

    public void push(long head) { push((double)head); }


    /* Returns average */
    public double getAvg() {
        return computeAvg(sum, pushCounter);
    }


    /* Returns squared deviation */
    public double getSquaredDev() {
        return computeSquaredDev(sum, sumOfSquares, pushCounter);
    }


    /* Returns deviation */
    public double getDev() { return Math.sqrt(getSquaredDev()); }


    /* Returns moving average */
    public double getMovingAvg() {
        if(pushCounter < trailLen) {
            return computeAvg(movingSum, pushCounter);
        } else {
            return computeAvg(movingSum, trailLen);
        }
    }


    /* Returns moving square deviation */
    public double getMovingSquareDev() {
        if(pushCounter < trailLen) {
            return computeSquaredDev(movingSum, movingSumOfSquares, pushCounter);
        } else {
            return computeSquaredDev(movingSum, movingSumOfSquares, trailLen);
        }
    }


    /* Returns moving deviation */
    public double getMovingDev() { return Math.sqrt(getMovingSquareDev()); }


    /* Serialize */
    public void store(OutputStream outputStream) throws IOException {
        storeDOUBLE(sum, outputStream);
        storeDOUBLE(sumOfSquares, outputStream);
        storeDOUBLE(movingSum, outputStream);
        storeDOUBLE(movingSumOfSquares, outputStream);
        storeINT64(pushCounter, outputStream);
        storeINT32(trailLen, outputStream);

        if(trail == null) {
            if(trailLen>0) {
                throw new NullPointerException("Expected trail not null.");
            } else {
                //noinspection UnnecessaryReturnStatement
                return;
            }
        } else {
            Iterator<Double> iterator = trail.iterator();
            for(int i=0; i<trailLen; i++) {
                storeDOUBLE(iterator.next(), outputStream);
            }
        }
    }


    /* Deserialize */
    public static MathAverage load(InputStream inputStream) throws IOException {
        return new MathAverage(inputStream);
    }


    public void print(PrintStream printStream, boolean doTrail) {
        printStream.println("MathAverage: ");
        printStream.println("Sum: " + sum);
        printStream.println("Sum of squares: " + sumOfSquares);
        printStream.println("Moving sum: " + movingSum);
        printStream.println("Moving sum of squares: " + movingSumOfSquares);
        printStream.println("--Average: " + getAvg());
        printStream.println("--Squared deviation: " + getSquaredDev());
        printStream.println("--Moving average: " + getMovingAvg());
        printStream.println("--Moving squared deviation: " + getMovingSquareDev());
        printStream.println("Push counter: " + pushCounter);
        printStream.println("Trail length: " + trailLen);

        if(doTrail) {
            printStream.println("Trail: ");
            if(trail == null) {
                printStream.println("\t No trail.");
            } else {
                Iterator<Double> iterator = trail.iterator();
                for(int i=0; i<trailLen; i++) {
                    printStream.println(" " + i + "\t" + iterator.next());
                }
            }

        }
    }


    private static double computeAvg(double sum, long n) {
        return sum / (double)n;
    }

    private static double computeSquaredDev(double sum, double sumOfSquares, long n) {
        double avg = computeAvg(sum, n);
        return (sumOfSquares / (double)n) - (avg*avg);
    }


    public static float getAsFloat(double val) {
        return new Double(val).floatValue();
    }



    public static void storeINT32(int val, OutputStream outputStream)
            throws IOException {
        int INT32_SIZE = 4;
        byte[] out = new byte[INT32_SIZE];
        int offset = 0;

        /* Little Endian */
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));

        outputStream.write(out);
    }


    public static void storeINT64(long val, OutputStream outputStream)
            throws IOException {
        int INT64_SIZE = 8;
        byte[] out = new byte[INT64_SIZE];
        int offset = 0;

        /* Little Endian */
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));

        outputStream.write(out);
    }


    public static void storeDOUBLE(double value, OutputStream outputStream)
            throws IOException {
        storeINT64(Double.doubleToRawLongBits(value), outputStream);
    }


    public static int loadINT32(InputStream inputStream) throws IOException {
        int INT32_SIZE = 4;
        byte[] bytes = readBytesFromInputStream(inputStream, INT32_SIZE);
        int offset = 0;

        /* Little Endian */
        return (bytes[offset] & 0xFF) |
                ((bytes[offset + 1] & 0xFF) << 8) |
                ((bytes[offset + 2] & 0xFF) << 16) |
                ((bytes[offset + 3] & 0xFF) << 24);
    }


    public static long loadINT64(InputStream inputStream) throws IOException {
        int INT32_SIZE = 8;
        byte[] bytes = readBytesFromInputStream(inputStream, INT32_SIZE);
        int offset = 0;

        /* Little Endian */
        return (bytes[offset] & 0xFFL) |
                ((bytes[offset + 1] & 0xFFL) << 8) |
                ((bytes[offset + 2] & 0xFFL) << 16) |
                ((bytes[offset + 3] & 0xFFL) << 24) |
                ((bytes[offset + 4] & 0xFFL) << 32) |
                ((bytes[offset + 5] & 0xFFL) << 40) |
                ((bytes[offset + 6] & 0xFFL) << 48) |
                ((bytes[offset + 7] & 0xFFL) << 56);
    }


    public static double loadDOUBLE(InputStream inputStream) throws IOException {
        return Double.longBitsToDouble(loadINT64(inputStream));
    }


    public static byte[] readBytesFromInputStream(InputStream inputStream, int numBytes)
            throws IOException {
        byte[] output = new byte[numBytes];
        int bytesRead = inputStream.read(output, 0, numBytes);
        if( bytesRead != numBytes ) {
            throw new IOException("Expected " + numBytes + " but read " + bytesRead + ".");
        }
        return output;
    }



//    /* TEST/DEBUG */
//    public static void main(String[] args) throws IOException {
//        String path = "/tmp/testMathAverage.bin";
//
//        int trailLen = 4;
//        int extra = 1000;
//
//        MathAverage mathAverage = new MathAverage(trailLen);
//        for(int i=0; i<(trailLen+extra); i++) {
//            mathAverage.push( (int)(Utils.getRandomString(1).getBytes("US-ASCII")[0]) );
//        }
//
//        mathAverage.print(System.out, true);
//
//
//        FileOutputStream fileOutputStream = new FileOutputStream(path);
//        mathAverage.store(fileOutputStream);
//        fileOutputStream.close();
//
//
//        FileInputStream fileInputStream = new FileInputStream(path);
//        MathAverage mathAverage2 = MathAverage.load(fileInputStream);
//        fileInputStream.close();
//
//
//        System.out.println("\n\n");
//        mathAverage2.print(System.out, true);
//    }
}
