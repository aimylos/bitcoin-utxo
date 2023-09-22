package Blockchainj.Blockchain.Statistics;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Statistics
 *
 * Manage statistics.
 *
 * The class stores the statistics to the disk in the following format:
 * Filename: <the name for a specific statistic>
 * Format: <index_data><delimiter><value_data>
 *      The data can be an integer, a float, a metric unit OR a set of those.
 *
 *
 */

public abstract class Statistics {
    /* Filenames */
    private static final String STATS_FILE_EXTENSION = ".log";

    private final LinkedHashMap<Integer, StatGroup> groups;
    private final LinkedHashMap<Integer, Stat> stats;
    private final Path statsPath;

    private final Path statInfoPathname;
    private final Path statLogPathname;


    /* Entry formats */
    private static final Charset charset = Charset.forName("US-ASCII");
    private static final String DELIMETER = "\t";
    private static final byte[] DELIMETER_BYTES = new byte[] {(byte)0x09};
    private static final String END_TOKEN = "\n";
    private static final byte[] END_TOKEN_BYTES = new byte[] {(byte)0x0A};

    //TODO REMOVE LOCK AND USE SYNCRONIZED ONCE VERIFIED THAT SYNCHRONIZED WORKS
    /* Internal lock for synchro */
    private final ReentrantReadWriteLock readWriteLock;
    /* 1 permit semaphone. Can only perform compute/write actions if permit is acquired. */
    private final Semaphore actionPermit = new Semaphore(1);
    private boolean closed = false;

    /* Printstream for printing progress or other messages. */
    protected PrintStream PRINTSTREAM = null;
    protected int PRINT_PERIOD = 100;

    /* Concurrenct parameters for doCycle */
    public static final boolean DEFAULT_DO_CONCURRENT = true;
    public static final int DEFAULT_THREAD_NUM = Runtime.getRuntime().availableProcessors();
    private final boolean DO_CONCURRENT;
    private final int THREAD_NUM;


    /* stats is a hashmap of key:statname and value:statdescription */
    protected Statistics(String path, String statInfoFilename, String statLogFilename,
                         HashMap<Integer, StatGroup> groups,
                         boolean DO_CONCURRENT, int THREAD_NUM)
            throws IOException {
        /* Set concurrenct parameters */
        this.DO_CONCURRENT = DO_CONCURRENT;
        this.THREAD_NUM = THREAD_NUM;

        /* init lock */
        readWriteLock = new ReentrantReadWriteLock(true);

        /* init paths */
        this.statsPath = Paths.get(path);
        this.statInfoPathname = Paths.get(path, statInfoFilename);
        this.statLogPathname = Paths.get(path, statLogFilename);

        /* Make directory */
        File dir = statsPath.toFile();
        if ( !dir.exists() ) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        /* Init groups and stats */
        this.groups = new LinkedHashMap<>();
        this.stats = new LinkedHashMap<>();
        Iterator<StatGroup> itGroups = groups.values().iterator();
        while(itGroups.hasNext()) {
            StatGroup statGroup = itGroups.next();

            /* Add group */
            this.groups.put(statGroup.getGroupId(), statGroup);

            /* Add stats */
            Iterator<Stat> itStat = statGroup.getStatIterator();
            while(itStat.hasNext()) {
                Stat stat = itStat.next();

                stat.setPath(this.statsPath.toString());
                this.stats.put(stat.getId(), stat);
            }
        }
    }


    /* Set printStream. If null then no messages will be printed. */
    public void setPRINT_STREAM(PrintStream printStream) { this.PRINTSTREAM = printStream; }

    /* Set print period parameters */
    public void setPRINT_PERIOD(int printPeriod) { PRINT_PERIOD = printPeriod; }


    /* Appends entry to file  */
    protected void appendEntry(Entry entry) throws IOException {
        /* append entry to end of file */
        OutputStream outputStream = new FileOutputStream(
                stats.get(entry.getStatId()).getFilepathname().toFile(), true);
        entry.store(outputStream);
        outputStream.close();
    }


    /* Overwrites stat info to file */
    protected void writeStatInfo(String header) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(statInfoPathname.toFile());

        PrintWriter printWriter = new PrintWriter(fileOutputStream);
        printWriter.println(header);
        printWriter.print("\n");

        Iterator<StatGroup> itGroup = groups.values().iterator();
        while(itGroup.hasNext()) {
            StatGroup statGroup = itGroup.next();
            statGroup.store(printWriter);
            printWriter.print("\n");
        }

        printWriter.close();
        fileOutputStream.close();
    }

    public abstract void writeStatInfo() throws IOException;


    /* Overwrites log with Log */
    protected void storeLog(Log log) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(statLogPathname.toFile());
        log.store(fileOutputStream);
        fileOutputStream.close();
    }


    /* Load Log from file */
    protected void readLog(Log log) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(statLogPathname.toFile());
        log.load(fileInputStream);
        fileInputStream.close();
    }


    /* returns stats and groups */
    protected Stat getStat(int id) {
        return stats.get(id);
    }

    protected StatGroup getGroup(int groupId) { return groups.get(groupId); }

    protected Iterator<StatGroup> getGroupIterator() { return groups.values().iterator(); }


    /* Internal lock methods
     * To be used as:
     * >Call the lock function.
     * try {
     *      ...do other locks or w/e.
     * } finally {
     * >Call appropriate unlock function. } */
    protected void lock() {
        readWriteLock.writeLock().lock();
    }

    protected void unlock() {
        readWriteLock.writeLock().unlock();
    }


    /* Might block and wait for lock. */
    public boolean isClosed() {
        lock();

        try {
            return closed;
        } finally {
            unlock();
        }
    }

    /* Might block and wait for lock. */
    public void close() {
        /* Volatile check first */
        if(closed) {
            return;
        }

        if(PRINTSTREAM != null) {
            PRINTSTREAM.println("Getting lock for close from thread "
                    + Thread.currentThread().getId() + "...");
        }

        lock();

        if(PRINTSTREAM != null) {
            PRINTSTREAM.println("Lock for close acquired by thread "
                    + Thread.currentThread().getId() + "...");
        }

        try {
            /* If already closed nothing to do here */
            if(!closed) {
                /* Get the action permit so no more cycles can be pefrormed.
                 * The internal behaviour is set up in a way that if lock is acquired the permit
                 * can be acquired, which defeats the purpose of having a semaphore at the first
                 * place! But semaphores can be trusted more than volatiles. */
                int permit = actionPermit.drainPermits();
                if(permit <= 0) {
                    throw new RuntimeException("Expected permit not acquired.");
                }

                /* Set state closed. */
                closed = true;

                if(PRINTSTREAM != null) {
                    PRINTSTREAM.println("Closed. Releasing lock for close from thread "
                            + Thread.currentThread().getId() + "...");
                }
            }
        } finally {
            unlock();
        }
    }


    /* Hook SIGINT to close safely. */
    public void hookSIGINTtoClose() {
        //noinspection AnonymousHasLambdaAlternative
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                close();
            }
        });
    }


    /* Computes and stores stats. Can be overridden but it's behaviour must be kept the same. */
    public void runStats() throws IOException {
        boolean notDone = true;

        while(notDone) {
            /* Get lock. It's reentrant so locking it more that once it's ok. */
            lock();

            try {
                /* Get a permit to perform a cylce. */
                if(actionPermit.tryAcquire()) {
                    notDone = doCycle();
                }
                /* Else break from loop. */
                else {
                    notDone = false;
                }
            } finally {
                /* Release permit if held. */
                actionPermit.release();

                /* Unlock lock */
                unlock();
            }
        }
    }


    /* Does a computation and store cylce. Returns false if nothing else to process.
     * This method must not be interrupted.
     * Override this method to perform pre-group computations. */
    protected boolean doCycle() throws IOException {
        /* Do stats not concurrenctly */
        if(!DO_CONCURRENT) {
            Iterator<StatGroup> it = getGroupIterator();
            while (it.hasNext()) {
                StatGroup statGroup = it.next();

                if (statGroup.isActive()) {
                    GroupCallable<Void> callable = getCallable(statGroup.getGroupId());

                    callable.call();
                }
            }
        }
        /* Do stats concurrenctly */
        else {
            /* Thread timeout is set at 600 seconds. */
            long timeoutMillis = 600 * 1000;

            /* Unbounded blocking queue for thread pool excecutor. The producer (this thread),
             * will never submit more than group count threads. */
            LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();

            /* Init thread pool.
               As long as Unbounded queue is used, maxThreads is equeal to core threads. */
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    THREAD_NUM, THREAD_NUM,
                    timeoutMillis, TimeUnit.MILLISECONDS, blockingQueue);

            /* Array of futures */
            LinkedList<Future<Void>> futures = new LinkedList<>();

            /* For group submit new task */
            Iterator<StatGroup> it = getGroupIterator();
            while(it.hasNext()) {
                StatGroup statGroup = it.next();

                /* If group is active */
                if(statGroup.isActive()) {
                    /* Make callable */
                    GroupCallable<Void> callable = getCallable(statGroup.getGroupId());

                    /* Submit task */
                    futures.add(threadPoolExecutor.submit(callable));
                }
            }

            /* Shut down thread pool */
            threadPoolExecutor.shutdown();

            /* For each task look for errors */
            Iterator<Future<Void>> futureIt = futures.descendingIterator();
            while(futureIt.hasNext()) {
                try {
                    /* Wait for task to complete. */
                    futureIt.next().get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }

        return true;
    }


    /* Make groups reachable
     * Returns the appopriate callable for that group */
    protected abstract GroupCallable<Void> getCallable(int groupId);


    /* Abstract GroupCallable that performs a group statistic's computations and storage. */
    protected abstract class GroupCallable<V extends Void> implements Callable<V> {
        @Override
        public abstract V call() throws IOException;
    }


    /* Basic Entry class */
    protected abstract class Entry {
        /* Stat name */
        private final int statId;

        /* Tokens */
        protected final byte[] delim = DELIMETER_BYTES;
        protected final byte[] endToken = END_TOKEN_BYTES;

        /* Data */
        protected byte[] index_data;
        protected byte[] value_data;

        Entry(int statId) {
            this.statId = statId;
        }

        int getStatId() {
            return statId;
        }

        /* Write Entry to outputstream */
        void store(OutputStream outputStream) throws IOException {
            outputStream.write(index_data);
            outputStream.write(delim);
            outputStream.write(value_data);
            outputStream.write(endToken);
        }

        /* Descards index_data and value_data and loads them from inputstream */
        //abstract void load(InputStream inputStream) throws IOException;

        /* Size in bytes of Entry from store() method */
        int getSerializedSize() {
            return index_data.length + delim.length + value_data.length + endToken.length;
        }
    }


    /* Entry class for index_data:<int>, value_data:<long> */
    protected class EntryIntLong extends Entry {
        EntryIntLong(int statId, int index, long value) {
            super(statId);
            index_data = Integer.toString(index).getBytes(charset);
            value_data = Long.toString(value).getBytes(charset);
        }

        int getIndex() {
            return Integer.parseInt(new String(index_data, charset));
        }

        long getValue() {
            return Long.parseLong(new String(value_data, charset));
        }
    }


    /* Entry class for index_data:<int>, value_data:<double> */
    protected class EntryIntDouble extends Entry {
        EntryIntDouble(int statId, int index, Double value) {
            super(statId);
            index_data = Integer.toString(index).getBytes(charset);
            value_data = String.format("%.3f", value).getBytes(charset);
        }

        int getIndex() {
            return Integer.parseInt(new String(index_data, charset));
        }

        double getValue() {
            return Double.parseDouble(new String(value_data, charset));
        }
    }


    /* Entry class for index_data:<int>, value_data:<long[]> */
    protected class EntryIntLongArray extends Entry {
        protected final byte[] arrayValuesDelim = delim;

        EntryIntLongArray(int statId, int index, long value[]) {
            super(statId);
            index_data = Integer.toString(index).getBytes(charset);

            org.apache.commons.io.output.ByteArrayOutputStream outputStream =
                    new org.apache.commons.io.output.ByteArrayOutputStream();
            for(int i=0; i<value.length; i++) {
                //noinspection EmptyCatchBlock
                try {
                    outputStream.write(Long.toString(value[i]).getBytes(charset));

                    if(i != (value.length-1) ){
                        outputStream.write(arrayValuesDelim);
                    }
                } catch (IOException e) {}
            }

            value_data = outputStream.toByteArray();
        }

        int getIndex() {
            return Integer.parseInt(new String(index_data, charset));
        }
    }


    /* Entry class for index_data:<int>, value_data:<double[]> */
    protected class EntryIntDoubleArray extends Entry {
        protected final byte[] arrayValuesDelim = delim;

        EntryIntDoubleArray(int statId, int index, double value[]) {
            super(statId);
            index_data = Integer.toString(index).getBytes(charset);

            org.apache.commons.io.output.ByteArrayOutputStream outputStream =
                    new org.apache.commons.io.output.ByteArrayOutputStream();
            for(int i=0; i<value.length; i++) {
                //noinspection EmptyCatchBlock
                try {
                    outputStream.write(String.format("%.3f", value[i]).getBytes(charset));

                    if(i != (value.length-1) ){
                        outputStream.write(arrayValuesDelim);
                    }
                } catch (IOException e) {}
            }

            value_data = outputStream.toByteArray();
        }

        int getIndex() {
            return Integer.parseInt(new String(index_data, charset));
        }
    }


    /* Basic Log entry */
    protected abstract class Log {
        abstract void store(OutputStream outputStream) throws IOException;

        abstract void load(InputStream inputStream) throws IOException;
    }


    /* Prints parameters */
    public void printParameters(PrintStream printStream) {
        printStream.println(">Statistics parameters:");
        printStream.println("Statistics path: " + statsPath);
        printStream.println("Statistics info pathname" + statInfoPathname);
        printStream.println("Statistics log pathname" + statInfoPathname);
        printStream.println("Statistics concurrent: " + DO_CONCURRENT);
        printStream.println("Statistics threads: " + THREAD_NUM);
        printStream.println("Statistics print period: " + PRINT_PERIOD);
        printStream.println("Statistics closed: " + isClosed());
        Iterator<StatGroup> iterator = groups.values().iterator();
        while(iterator.hasNext()) {
            StatGroup statGroup = iterator.next();
            printStream.println("Statistics group " + statGroup.getGroupId() + " active: " +
                    statGroup.isActive());
        }
    }

    public void print(PrintStream printStream) {
        printStream.println(">Statistics: ");
        printStream.println("Statistics closed: " + isClosed());
    }
}
