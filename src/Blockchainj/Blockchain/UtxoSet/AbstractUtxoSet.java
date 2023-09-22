package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Bitcoin.Block;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.Shard.MainShardFactory;
import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardFactory;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardIterator;
import Blockchainj.Blockchain.UtxoSet.UTXOS.ShardChanges;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxoSetChanges;
import Blockchainj.Util.MerkleTree;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * AbstractUtxoSet
 *
 * UtxoSet implementation.
 *
 * Has optional concurrency for commitBlock() method.
 *
 * This implementation uses an internal number of shards to split the Utxo Set into.
 * The Shards are accessed via a put/get interface.
 * For disk load and storage the Shards are accessed via a store/load interface.
 *
 * The implementation of this Abstract class may or may not hold pending data in memory.
 * Nevertheless, the close() method must be called before terminating the program
 * to guarantee a successful reopening of the utxo set.
 *
 *
 */


public abstract class AbstractUtxoSet implements UtxoSet {
    @Override
    public ShardFactory getShardFactory() { return MainShardFactory.shardFactory; }

    /* Filenames */
    protected final UtxoSetFileNaming filenames;

    /* Utxo set log */
    protected final UtxoSetLog utxoSetLog;

    /* Utxo Set */
    /* Current blockhash and height */
    private SHA256HASH bestBlockhash;
    private int bestHeight;
    public static final SHA256HASH NULL_BLOCKHASH = SHA256HASH.ZERO_SHA256HASH;

    /* number of shards */
    protected final int shardNum;

    /* Utx and Utxo count */
    private AtomicInteger utxCount;
    private AtomicInteger utxoCount;

    /* Utxo set serialized size. Includes Shards.serializedSize. */
    private AtomicLong serializedSize;

    /* UTXs serialized size. Sum of all UTX's serialized size. */
    private AtomicLong serializedUtxSize;

    /* Utxo Merkle Tree */
    protected final MerkleTree merkleTree;

    /* Parameters for hashing shards and merkle tree updates */
    protected boolean HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT = false;
    public static final SHA256HASH NULL_MERKLE_TREE_ROOT = SHA256HASH.ZERO_SHA256HASH;

    /* Concurrency parameters */
    private boolean CONCURRENT_COMMIT = false;
    private int COMMIT_CORE_THREADS = Runtime.getRuntime().availableProcessors();
    public static final long COMMIT_THREAD_TIMEOUT = 600 * 1000; //10min
    public static final TimeUnit COMMIT_THREAD_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    /* Garbage collector call period */
    public static final int DEFAULT_GARBAGE_COLLECTOR_CALL_PERIOD = 100;
    private int GARBAGE_COLLECTOR_CALL_PERIOD = DEFAULT_GARBAGE_COLLECTOR_CALL_PERIOD;

    /* Mark utxo set as closed */
    private volatile boolean closed = false;

    /* UtxoSetTimer */
    private final UtxoSetTimer utxoSetTimer = new UtxoSetTimer();
    private boolean activeTimer = false;


    /* Constructor for new UtxoSet. Throws FileAlreadyExistsException if UtxoSet already exists
     * in the utxoSetPath provided dir.
     * The new UtxoSet will have an internal shard number of numShard. */
    public AbstractUtxoSet(String utxoSetPath, int shardNum)
            throws IllegalArgumentException, IOException {
        /* Init filenames with default naming. */
        this.filenames = new UtxoSetFileNaming(utxoSetPath);

        /* Init utxo set log */
        utxoSetLog = new UtxoSetLog(filenames.getUtxoSetLogFilenameAsPath());

        /* Get utxo set information */
        UtxoSetLog.UtxoSetLogEntry info = utxoSetLog.getLastEntry();

        /* If utxo set already exists and throw exception! */
        if(info != null) {
            throw new FileAlreadyExistsException("Utxo Set log already exists!");
        }

        /* create new utxo set */

        /* set height to undefined */
        bestHeight = UNDEFINED_HEIGHT;

        /* set zero hash blockhash */
        bestBlockhash = NULL_BLOCKHASH;

        /* set number of shards */
        ProtocolParams.validateShardNum(shardNum);
        this.shardNum = shardNum;

        /* init merkle tree */
        merkleTree = new MerkleTree(shardNum);
    }


    /* Constructor for existing Utxo Set. */
    public AbstractUtxoSet(String utxoSetPath) throws IOException {
        /* Init filenames with default naming. */
        this.filenames = new UtxoSetFileNaming(utxoSetPath);

        /* Init utxo set log */
        utxoSetLog = new UtxoSetLog(filenames.getUtxoSetLogFilenameAsPath());

        /* Get utxo set information */
        UtxoSetLog.UtxoSetLogEntry info = utxoSetLog.getLastEntry();

        /* If info is null then utxo set doesn't exist. Throw exception. */
        if(info == null) {
            throw new FileNotFoundException("Utxo set not found.");
        }

        /* Init utxo set. */

        /* set height */
        bestHeight = info.height;

        /* set blockhash */
        bestBlockhash = info.blockhash;

        /* set number of shards */
        shardNum = info.numShard;
        ProtocolParams.validateShardNum(shardNum);

        /* init merkle tree */
        merkleTree = new MerkleTree(shardNum);
    }


    /* Constructor for creating new UtxoSet from existing Utxo Set */
//    public AbstractUtxoSet(String oldUtxoSetPath, String newUtxoSetPath, int newShardNum)
//            throws IOException, IllegalArgumentException {
//        //TODO  look for code down
            //TODO implement only for UtxoSetIO
//    }




    /**
     * Parameters set methods. Best set after calling constructor.
     * Can still be called later though. */
    public synchronized void setCONCURRENT_COMMIT(boolean CONCURRENT_COMMIT) {
        this.CONCURRENT_COMMIT = CONCURRENT_COMMIT;
    }

    public synchronized void setCOMMIT_CORE_THREADS(int COMMIT_CORE_THREADS) {
        this.COMMIT_CORE_THREADS = COMMIT_CORE_THREADS;
    }

    public synchronized void setHASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT(boolean bool) {
        this.HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT = bool;
    }

    public synchronized void setGARBAGE_COLLECTOR_CALL_PERIOD(int GARBAGE_COLLECTOR_CALL_PERIOD) {
        this.GARBAGE_COLLECTOR_CALL_PERIOD = GARBAGE_COLLECTOR_CALL_PERIOD;
    }

    public synchronized void setActiveTimer(boolean activeTimer) {
        this.activeTimer = activeTimer;
    }


    /**
     *  Initialization methods must be called by AbstractUtxoSet implementaion constructors.
     *  In special cases these methods may not be called! */
    /* Init new utxo set. Calls createEmptyShard() implying that new shards must
     * be created and written on the disk. */
    protected void initNewUtxoSet() throws IOException {
        /* init counters and serialized sizes */
        utxCount = new AtomicInteger(0);
        utxoCount = new AtomicInteger(0);
        serializedSize = new AtomicLong(0);
        serializedUtxSize = new AtomicLong(0);

        /* create numShard new empty shards */
        for (int i = 0; i < getShardNum(); i++) {
            /* create new empty shard */
            Shard shard = createEmptyShard(i);

            /* Update serialize size */
            serializedSize.addAndGet(shard.getSerializedSize());
            serializedUtxSize.addAndGet(shard.getUtxSerializedSize());

            /* Calculate hash.*/
            SHA256HASH shardHash = shard.calcShardHash();

            /* update merkle tree hashes */
            merkleTree.updateLeafHash(i, shardHash);
        }

        /* rehash merkle tree */
        merkleTree.rehashTree();
    }


    /* Load and Init utxo set. Calls getShard() implying that since no other changes to the
     * shards have been performed, getShard() and loadShard() must return the same shard. */
    protected void loadAndInitUtxoSet(boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT) throws IOException {
        /* init counters and serialized sizes */
        utxCount = new AtomicInteger(0);
        utxoCount = new AtomicInteger(0);
        serializedSize = new AtomicLong(0);
        serializedUtxSize = new AtomicLong(0);

        /* Load shards and build merkle tree */
        for (int i = 0; i < getShardNum(); i++) {
            /* Load shard. */
            Shard shard = getCachedShard(i);

            /* Update serialize size */
            serializedSize.addAndGet(shard.getSerializedSize());
            serializedUtxSize.addAndGet(shard.getUtxSerializedSize());

            /* Update utx and utxo count */
            utxCount.addAndGet(shard.getUtxCount());
            utxoCount.addAndGet(shard.getUtxoCount());

            /* Calculate hash. */
            SHA256HASH shardHash = shard.calcShardHash();

            /* update merkle tree hash */
            merkleTree.updateLeafHash(i, shardHash);
        }

        /* rehash merkle tree */
        merkleTree.rehashTree();

        /* compare merkle tree root with log */
        if (DO_MERKLE_TREE_CHECKSUM_ON_INIT) {
            /* Get utxo set information */
            UtxoSetLog.UtxoSetLogEntry info = utxoSetLog.getLastEntry();

            SHA256HASH logMerkleRoot = info.merkleRoot;
            SHA256HASH computedMerkleRoot = merkleTree.getRoot();

            if (!logMerkleRoot.equals(computedMerkleRoot)) {
                throw new IOException("Log file's merkle root does not match " +
                        "loaded shards merkle tree root." +
                        " Blockhash: " + getBestBlockhash().toString() +
                        " Height: " + getBestHeight());
            }
        }
    }


    /**
     * Methods that handle shards. The abstract shard methods are left to be implemented.
     * The methods that have a Path parameter imply that there should be an interaction
     * with the disk, although that is not necessary. But note, that these methods are
     * called to imply that the data must be permanent in some way (store them on the disk). */
    /* Creates an empty shard with shardIndex and numShard.
       If shard already exists as a file, then IOException is thrown.
       This method may or may not store shard to disk. */
    abstract protected Shard createEmptyShard(int shardIndex, Path shardPathName)
            throws IOException;

    protected Shard createEmptyShard(int shardIndex) throws IOException {
        /* get shard path and name and file */
        Path shardPathName = filenames.getShardFilenameAsPath(shardIndex);
        return createEmptyShard(shardIndex, shardPathName);
    }


    /* Get most recent version of shard. Does not imply a DISK/CACHE commitment. */
    abstract protected Shard getCachedShard(int shardIndex) throws IOException;

    /* Put most recent version of shard. Does not imply a DISK/CACHE commitment. */
    abstract protected void putCachedShard(Shard shard) throws IOException;


    /* Load shard from implying DISK. */
    protected Shard loadShard(int shardIndex) throws IOException {
        return loadShard(shardIndex, filenames.getShardFilenameAsPath(shardIndex));
    }

    /* Load shard from implying DISK. */
    abstract protected Shard loadShard(int shardIndex, Path shardPathName) throws IOException;


    /* Store shard to implying DISK. */
    protected void storeShard(Shard shard) throws IOException {
        storeShard(shard, filenames.getShardFilenameAsPath(shard.getShardIndex()));
    }

    /* Stored shard to implying DISK. */
    abstract protected void storeShard(Shard shard, Path shardPathName) throws IOException;



    /**
     * Commit Block. Just like SQL transactions, this operation is atomic.
     * The changes should be all changes to the Utxo Set done by a single block.
     * After this operation, the Utxo Set height will be increased by one and the Utxo log
     * will be updated appropriately.
     * If a data existance (utxo not found or utxo already exist) error occurs, then
     * a BitcoinUtxoSetException is thrown and the utxo set is corrupted!
     *
     * NOT DONE...and all changes to the Utxo Set up to that point
     * are rolledback to the state before the method was called...NO ROLLBACK
     * To overcome ROLLBACK issues, archiving must be used periodically. */
    @Override
    public synchronized void commitBlock(Block block) throws BitcoinUtxoSetException, IOException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        /* Call garbage collector */
        if(block.getHeight()%GARBAGE_COLLECTOR_CALL_PERIOD == 0) {
            Utils.suggestGarbageCollectorRun();
        }

        /* TIMER */
        if(activeTimer) {
            utxoSetTimer.startRound();
            utxoSetTimer.startTimerForStage(UtxoSetTimer.calcUtxoSetChanges);
        }

        /* Do not check height consistency. It's not the UtxoSet's job. */

        /* Calculate UtxoSetChanges */
        UtxoSetChanges changes = UtxoSetChanges.calcNewUtxoSetChanges(
                block, shardNum, getShardFactory().getUtxFactory());

        /* TIMER */
        if(activeTimer) {
            utxoSetTimer.endTimerForStage1startTimerForStage2(
                    UtxoSetTimer.calcUtxoSetChanges, UtxoSetTimer.applyChanges);
        }

        /* If concurrent commit */
        if(CONCURRENT_COMMIT) {
            /* Unbounded blocking queue for thread pool excecutor. The producer (this thread),
             * will never submit more than modifiedShards threads. */
            LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();

            /* Init thread pool.
               As long as Unbounded queue is used, maxThreads is equeal to core threads. */
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    COMMIT_CORE_THREADS, COMMIT_CORE_THREADS,
                    COMMIT_THREAD_TIMEOUT, COMMIT_THREAD_TIMEOUT_UNIT, blockingQueue);

            /* List of futures. */
            LinkedList<Future<Void>> futures = new LinkedList<>();

            /* TIMER */
            LinkedList<UtxoSetTimer.ShardTimer> shardTimers = new LinkedList<>();

            /* For each shard change submit new task */
            Iterator<ShardChanges> it = changes.getShardChangesIterator();
            while(it.hasNext()) {
                ShardChanges shardChanges = it.next();

                /* TIMER Get timer for shard and store it. */
                //noinspection UnusedAssignment
                UtxoSetTimer.ShardTimer shardTimer = null;
                if(activeTimer) {
                    shardTimer = utxoSetTimer.getShardTimer();
                    shardTimers.add(shardTimer);
                }

                /* Make callable */
                //noinspection ConstantConditions
                ApplyShardChanges applyShardChanges =
                        new ApplyShardChanges(shardChanges, shardTimer);

                /* Submit task */
                futures.add(threadPoolExecutor.submit(applyShardChanges));
            }

            /* Shut down thread pool */
            threadPoolExecutor.shutdown();

            /* For each task look for errors */
            Iterator<Future<Void>> futureIt = futures.descendingIterator();
            while(futureIt.hasNext()) {
                try {
                    /* Wait for task to complete. */
                    futureIt.next().get();
                } catch (InterruptedException e) {
                    //Cannot roll back
                    throw new IOException(e);
                } catch (ExecutionException e) {
                    if(e.getCause() instanceof BitcoinUtxoSetException) {
                        /* NO ROLLBACK */
                        throw (BitcoinUtxoSetException) e.getCause();
                    } else if (e.getCause() instanceof IOException) {
                        throw (IOException) e.getCause();
                    } else {
                        throw new IOException(e.getCause());
                    }
                }
            }

            /* TIMER */
            if(activeTimer) {
                Iterator<UtxoSetTimer.ShardTimer> shardTimerIt = shardTimers.iterator();
                while(shardTimerIt.hasNext()) {
                    utxoSetTimer.addFromTimerCumulativeTimes(shardTimerIt.next());
                }
            }
        }
        /* Else non-concurrent commit */
        else {
            /* For each shard change apply changes */
            Iterator<ShardChanges> it = changes.getShardChangesIterator();
            while(it.hasNext()) {
                //noinspection CaughtExceptionImmediatelyRethrown
                try {
                    ShardChanges shardChanges = it.next();

                    /* TIMER */
                    //noinspection UnusedAssignment
                    UtxoSetTimer.ShardTimer shardTimer = null;
                    if(activeTimer)
                        shardTimer = utxoSetTimer.getShardTimer();

                    /* Apply changes */
                    //noinspection ConstantConditions
                    ApplyShardChanges applyShardChanges =
                            new ApplyShardChanges(shardChanges, shardTimer);
                    applyShardChanges.call();

                    /* TIMER */
                    if(activeTimer)
                        utxoSetTimer.addFromTimerCumulativeTimes(shardTimer);

                } catch (BitcoinUtxoSetException e) {
                    /* NO ROLLBACK */
                    throw e;
                }
                /* catch (IOException e) */
            }
        }


        /* TIMER */
        if(activeTimer) {
            utxoSetTimer.endTimerForStage1startTimerForStage2(
                    UtxoSetTimer.applyChanges, UtxoSetTimer.rehashMerkleTree);
        }

        if(HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT) {
            /* Rehash merkle tree */
            merkleTree.rehashTree();
        }

        /* TIMER */
        if(activeTimer) {
            utxoSetTimer.endTimerForStage1startTimerForStage2(
                    UtxoSetTimer.rehashMerkleTree, UtxoSetTimer.updateUtxoSetLog);
        }

        /* update state variables */
        bestBlockhash = changes.getBlockhash();
        bestHeight = changes.getHeight();

        /* update utxo set log */
        if(HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT) {
            utxoSetLog.appendEntry(bestBlockhash, shardNum, merkleTree.getRoot(), bestHeight);
        } else {
            utxoSetLog.appendEntry(bestBlockhash, shardNum, NULL_MERKLE_TREE_ROOT, bestHeight);
        }

        /* TIMER */
        if(activeTimer) {
            utxoSetTimer.endTimerForStage(UtxoSetTimer.updateUtxoSetLog);
            utxoSetTimer.endRound();
        }
    }


    /* This method must be called by a synchronized method.
       Rehash shards, update merkle tree, rehash merkle tree, delete last entry from utxo set log,
       append new entry to utxo set log and store any pending data to the disk. */
    abstract protected void commitPendingData() throws IOException;


    /**
     * Close methods. */
    /* Safely closes the utxo set.
       After this method has been called, access to the utxo write functions is not possible,
       access to shards is not possible, other non-blocked methods may have undefined behaviour. */
    @Override
    public synchronized void close() throws IOException {
        if(isClosed()) {
            return;
        }

        /* Commit any pending data to disk. */
        commitPendingData();

        /* Mark utxo set as closed */
        closed = true;
    }

    /* Call to this function is best made after aquiring at least read lock. */
    @Override
    public synchronized boolean isClosed() {
        return closed;
    }


    /**
     * Applies ShardChanges to Shard. */
    /* Apply shard changes to shard. Load shard, apply changes, hash shard, update merkle tree,
     * store shard.
     * Note that as long as the shard hasn't updated the merkle tree and hasn't been stored, any
     * changes done to it are not permanent. */
    private class ApplyShardChanges implements Callable<Void> {
        private final ShardChanges shardChanges;
        private final UtxoSetTimer.ShardTimer shardTimer;

        private ApplyShardChanges(ShardChanges shardChanges, UtxoSetTimer.ShardTimer shardTimer) {
            this.shardChanges = shardChanges;
            this.shardTimer = shardTimer;
        }

        @Override
        public Void call() throws BitcoinUtxoSetException, IOException {
            /* TIMER */
            if (activeTimer) {
                shardTimer.startRound();
                shardTimer.startTimerForStage(UtxoSetTimer.totalApplyShardChanges);
                shardTimer.startTimerForStage(UtxoSetTimer.loadShard);
            }

            /* Get shard. Do not imply DISK commitment. */
            Shard shard = getCachedShard(shardChanges.getShardIndex());

            /* previous serialized size */
            long prevShardSerializedSize = shard.getSerializedSize();
            long prevShardUtxSerializedSize = shard.getUtxSerializedSize();

            /* previous counters */
            int prevUtxCount = shard.getUtxCount();
            int prevUtxoCount = shard.getUtxoCount();

            /* TIMER */
            if (activeTimer) {
                shardTimer.endTimerForStage1startTimerForStage2(
                        UtxoSetTimer.loadShard, UtxoSetTimer.applyShardChanges);
            }

            /* Apply changes to shard. */
            try {
                shard.applyShardChanges(shardChanges);
            } catch (BitcoinUtxoSetException e) {
                /* NO ROLLBACK */
                e.setBlockhash(getBestBlockhash().toString());
                e.setHeight(getBestHeight());
                throw e;
            }

            /* Update serialized size */
            serializedSize.addAndGet(shard.getSerializedSize() - prevShardSerializedSize);
            serializedUtxSize.addAndGet(
                    shard.getUtxSerializedSize() - prevShardUtxSerializedSize);

            /* Update counters */
            utxCount.addAndGet(shard.getUtxCount() - prevUtxCount);
            utxoCount.addAndGet(shard.getUtxoCount() - prevUtxoCount);

            /* TIMER */
            if (activeTimer) {
                shardTimer.endTimerForStage1startTimerForStage2(
                        UtxoSetTimer.applyShardChanges, UtxoSetTimer.calcShardHash);
            }

            if(HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT) {
                /* Hash shard */
                SHA256HASH newHash = shard.calcShardHash();

                /* Update merkle tree. */
                merkleTree.updateLeafHash(shard.getShardIndex(), newHash);
            }

            /* TIMER */
            if (activeTimer) {
                shardTimer.endTimerForStage1startTimerForStage2(
                        UtxoSetTimer.calcShardHash, UtxoSetTimer.storeShard);
            }

            /* Put shard. Do not imply DISK commitment.  */
            putCachedShard(shard);

            /* TIMER */
            if (activeTimer) {
                shardTimer.endTimerForStage(UtxoSetTimer.storeShard);
                shardTimer.endTimerForStage(UtxoSetTimer.totalApplyShardChanges);
                shardTimer.endRound();
            }

            return null;
        }
    }


    /**
     * Get method for internal AbstractUtxoSet data. */
    protected int getShardNum() { return shardNum; }

    public int getInternalBestShardNum() { return getShardNum(); }

    public String getUtxoSetPath() { return filenames.getUtxoSetPath(); }

    /* Get internal shard number for given height. */
    public synchronized int getInternalShardNum(int height) throws IOException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        UtxoSetLog.UtxoSetLogEntry entry = utxoSetLog.getEntry(height);
        return entry.numShard;
    }

    public synchronized SHA256HASH getInternalBestMerkleRoot() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return merkleTree.getRoot();
    }

    public synchronized SHA256HASH getInteranlMerkleRoot(int height) throws IOException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        UtxoSetLog.UtxoSetLogEntry entry = utxoSetLog.getEntry(height);
        return entry.merkleRoot;
    }

    /* Does not return copy */
    public synchronized MerkleTree getInternalBestMerkleTree() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return merkleTree.getReadOnly();
    }

    /* Does not return copy */
    public synchronized Shard getInternalBestShard(int index) throws IOException{
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return getCachedShard(index);
    }



    /**
     * Interface get methods.
     */
    @Override
    public synchronized SHA256HASH getBestBlockhash() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return bestBlockhash;
    }


    @Override
    public synchronized int getBestHeight() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return bestHeight;
    }


    @Override
    public synchronized SHA256HASH getBlockhash(int height) throws NoSuchElementException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        try {
            UtxoSetLog.UtxoSetLogEntry entry = utxoSetLog.getEntry(height);
            return entry.blockhash;
        } catch (IOException e) {
            throw new NoSuchElementException(e.toString());
        }
    }


    @Override
    public synchronized int getUtxCount() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return utxCount.get();
    }


    @Override
    public synchronized int getUtxoCount() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return utxoCount.get();
    }


    @Override
    public synchronized long getUtxoSetSerializedSizeEstimate(int shardNum)
            throws IllegalArgumentException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        ProtocolParams.validateShardNum(shardNum);

        return getUtxSerializedSize() +
                ( (long)shardNum * Shard.getEmptyShardHeaderSerializedSize() );
    }


    @Override
    public synchronized long getUtxSerializedSize() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return serializedUtxSize.get();
    }


    @Override
    public synchronized Iterator<UTX> getUtxIterator() {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        return new UtxIterator();
    }


    @Override
    public synchronized Shard getShard(int shardNum, int shardIndex)
            throws IOException, IllegalArgumentException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        /* Check if internal Shard num matches shard num */
        if(shardNum == getInternalBestShardNum()) {
            return getInternalBestShard(shardIndex);
        }

        /* Get involved shard indecies */
        int[] oldShardIndices = ProtocolParams.getShardIndicesThatContainValidUtxs(
                getInternalBestShardNum(), shardNum, shardIndex);

        /* List to put UTXs */
        LinkedList<UTX> utxList = new LinkedList<>();

        /* Temporary empty shard */
        Shard tempShard = getShardFactory().getNewShard(shardNum, shardIndex);

        /* Iterate through shards to get UTXs. UTXs should be sorted. */
        for(int i=0; i<oldShardIndices.length; i++) {
            Shard shard = getCachedShard(oldShardIndices[i]);

            Iterator<UTX> utxIterator = shard.getUtxIterator();
            while(utxIterator.hasNext()) {
                UTX utx = utxIterator.next();

                if(tempShard.inRange(utx.getTxid())) {
                    utxList.add(utx);
                }
            }

            /* No need to call putCachedShard() since no modifications to shard */
        }

        /* Get UTXs into array */
        UTX[] utxs = utxList.toArray(new UTX[0]);

        /* Create and return new shard */
        return getShardFactory().getNewShard(shardNum, shardIndex, utxs);
    }


    @Override
    public synchronized Iterator<Shard> getShardIterator(int shardNum)
            throws IllegalArgumentException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        /* Check if internal Shard num matches shard num */
        if(shardNum == getInternalBestShardNum()) {
            return new InternalShardIterator();
        } else {
            return new ShardIterator(new UtxIterator(), shardNum);
        }
    }


    @Override
    public synchronized MerkleTree getMerkleTree(int shardNum)
            throws IOException, IllegalArgumentException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        ProtocolParams.validateShardNum(shardNum);

        /* Check if internal Shard num matches shard num */
        if(shardNum == getInternalBestShardNum()) {
            return getInternalBestMerkleTree();
        }

        /* Build new merkle tree */
        MerkleTree merkleTree = new MerkleTree(shardNum);

        /* Get shards and calculate hashses */
        Iterator<Shard> shardIterator = getShardIterator(shardNum);
        /* Iterator should return ALL shards */
        for(int i=0; i<shardNum; i++) {
            try {
                /* Get next shard */
                Shard shard = shardIterator.next();

                /* Hash shard */
                SHA256HASH shardHash = shard.calcShardHash();

                /* Put shard to tree */
                merkleTree.updateLeafHash(shard.getShardIndex(), shardHash);
            } catch (NoSuchElementException e) {
                throw new IOException(e);
            }
        }

        /* Build merkle tree */
        merkleTree.rehashTree();

        /* Return read only shallow copy of the new merkle tree */
        return merkleTree.getReadOnly();
    }



    /* print */
    /* Caller must get readLock, else call at own risk */
    @Override
    public synchronized void print(PrintStream printStream) {
        printStream.println("Utxo Set");
        printStream.println("Best height: " + getBestHeight());
        printStream.println("Best blockhash: " + getBestBlockhash());
        printStream.println("Utxo Set closed: " + closed);
        printStream.println("Internal number of shards: " + getInternalBestShardNum());
        printStream.println("Best Merkle tree root: " + getInternalBestMerkleRoot());
        printStream.println("Utx count: " + getUtxCount());
        printStream.println("Utxo count: " + getUtxoCount());

        if(activeTimer)
            utxoSetTimer.print(printStream, true, true, true);
    }


    /* Print parameters */
    @Override
    public synchronized void printParameters(PrintStream printStream) {
        printStream.println(">Utxo Set parameters:");
        printStream.println("Utxo Set hash shards and rebuild merkle tree on commit: " +
                HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT);
        printStream.println("Utxo Set internal shard num: " + shardNum);
        printStream.println("Utxo Set concurrent commit: " + CONCURRENT_COMMIT);
        printStream.println("Utxo Set concurrent commit threads: " + COMMIT_CORE_THREADS);
        printStream.println("Utxo Set active timer: " + activeTimer);
        getShardFactory().printShardType(printStream);
        getShardFactory().getUtxFactory().printUtxType(printStream);
    }



    /* Utx iterator */
    private class UtxIterator implements Iterator<UTX> {
        private Shard currentShard;
        private Iterator<UTX> currentIterator;
        private volatile boolean endReached;

        private UtxIterator() {
            try {
                /* get first shard and it's utx iterator */
                currentShard = getCachedShard(0);
                currentIterator = currentShard.getUtxIterator();
                endReached = false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        public UTX next() throws NoSuchElementException {
            if(hasNext()) {
                return currentIterator.next();
            } else {
                throw new NoSuchElementException();
            }
        }


        @Override
        public boolean hasNext() {
            if(endReached) {
                return false;
            }

            /* while there's no more utx in current iterator get next iterator */
            while(!currentIterator.hasNext()) {
                /* get next shard index */
                int nextShardIndex = currentShard.getShardIndex() + 1;

                /* Do not put shard back. UTXs should not be modified! */

                /* Get next shard if any else mark end reached. */
                if(nextShardIndex == shardNum) {
                    endReached = true;
                    return false;
                } else {
                    try {
                        currentShard = getCachedShard(nextShardIndex);
                        currentIterator = currentShard.getUtxIterator();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return true;
        }
    }


    private class InternalShardIterator implements Iterator<Shard> {
        private int nextShardIndex = 0;

        @Override
        public Shard next() throws NoSuchElementException {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            /* Get return shard's index */
            int returnShardIndex = nextShardIndex;
            nextShardIndex++;

            try {
                return getCachedShard(returnShardIndex);
            } catch (IOException e) {
                nextShardIndex = shardNum;
                throw new NoSuchElementException();
            }
        }


        @Override
        public boolean hasNext() {
            return (nextShardIndex < shardNum);
        }
    }
}








//    /* Lock methods
//     * To be used as:
//     * >Call the lock function.
//     * try {
//     *      ...do other locks or w/e.
//     * } finally {
//     * >Call appropriate unlock function. */
//    public void lockReadLock() throws IllegalMonitorStateException {
//        if(isClosed()) {
//            throw new IllegalMonitorStateException("Utxo Set closed.");
//        }
//        readWriteLock.readLock().lock();
//    }
//
//    public void tryLockReadLock() throws IllegalMonitorStateException {
//        if(isClosed()) {
//            throw new IllegalMonitorStateException("Utxo Set closed.");
//        }
//        readWriteLock.readLock().tryLock();
//    }
//
//    public void unlockReadLock() throws IllegalMonitorStateException {
//        readWriteLock.readLock().unlock();
//    }
//
//    public void lockWriteLock() throws UtxoSetClosedException {
//        if(isClosed()) {
//            throw new UtxoSetClosedException("Utxo Set closed.");
//        }
//        readWriteLock.writeLock().lock();
//    }
//
//    public void tryLockWriteLock() throws UtxoSetClosedException {
//        if(isClosed()) {
//            throw new UtxoSetClosedException("Utxo Set closed.");
//        }
//        readWriteLock.writeLock().tryLock();
//    }
//
//    public void unlockWriteLock() throws IllegalMonitorStateException {
//        readWriteLock.writeLock().unlock();
//    }
//
//    protected boolean isWriteLockedByCurrentThread() {
//        return readWriteLock.isWriteLockedByCurrentThread();
//    }


//        if(isClosed()) {
//            throw new UtxoSetClosedException("Utxo Set closed.");
//        }
//
//        /* Check if write lock is helf by this tread. */
//        if(!isWriteLockedByCurrentThread()) {
//            throw new SyncFailedException("Write lock must be acquired first.");
//        }
//
//        if(printStream != null) {
//            printStream.println("Starting converting utxo set from number of shards " +
//                    getShardNum() + " to " + newShardNum + "...");
//        }
//
//        /* Validate new shard num */
//        ProtocolParams.validateShardNum(newShardNum);
//
//        /* Create temporary shallow copy shard */
//        UtxoSetIO oldUtxoSet = new UtxoSetIO(this);
//
//        /* Convert shard fields */
//        shardNum = newShardNum;
//        merkleTree = new MerkleTree(newShardNum);
//
//        /* temp name */
//        String temp = "temp";
//
//        if(printStream != null) {
//            printStream.println("Input validation done. Creating new shard files...");
//        }
//
//        /* Create new empty shards */
//        for(int i=0; i<newShardNum; i++) {
//            createEmptyShard(i, filenames.getShardFilenameAsPath(i, temp));
//        }
//
//        if(printStream != null) {
//            printStream.println("New shard files creation done. Start converting...");
//        }
//
//        /* For all old shard populate new shards */
//        PendingShard currentNewShard =
//                new PendingShard(0, filenames.getShardFilenameAsPath(0, temp));
//        boolean firstCall = true;
//        long counter = 0;
//        int gcPeriod = 10000;
//        int printPeriod = 1000;
//        for(int oldIndex=0; oldIndex<oldUtxoSet.getShardNum(); oldIndex++) {
//            Shard oldShard = oldUtxoSet.loadShard(oldIndex);
//
//            /* Iterate all UTXs */
//            Iterator<UTX> it = oldShard.getUtxIterator();
//            while(it.hasNext()) {
//                UTX utx = it.next();
//                while(!currentNewShard.inRange(utx.getTxid())) {
//                    /* Store new shard */
//                    currentNewShard.commitAndStore(filenames.getShardFilenameAsPath(
//                            currentNewShard.getShardIndex(), temp));
//
//                    /* Load next new shard */
//                    int nextIndex = currentNewShard.getShardIndex()+1;
//                    currentNewShard = new PendingShard(nextIndex,
//                            filenames.getShardFilenameAsPath(nextIndex, temp));
//                }
//
//                currentNewShard.addUTX(utx);
//                counter++;
//
//                if((printStream != null) && (counter%printPeriod==0)) {
//                    printProgressBar(printStream,
//                            currentNewShard.getShardIndex(), oldShard.getShardIndex(), firstCall);
//                    firstCall = false;
//                }
//
//                /* Call gc */
//                if(counter%gcPeriod==0){
//                    callGC();
//                }
//            }
//        }
//        /* Store last new shard */
//        currentNewShard.commitAndStore(
//                filenames.getShardFilenameAsPath(currentNewShard.getShardIndex(), temp));
//
//        if(printStream != null) {
//            printStream.println("");
//            printStream.println("Converting done. Deleting old shard files...");
//        }
//
//        /* Delete old shards */
//        for(int i=0; i<oldUtxoSet.getShardNum(); i++) {
//            /* get shard path and name and file */
//            Path shardPathNameOld = oldUtxoSet.filenames.getShardFilenameAsPath(i);
//            File shardFileOld = shardPathNameOld.toFile();
//            shardFileOld.delete();
//        }
//
//        if(printStream != null) {
//            printStream.println("Deleting old shard files done. Renaming new shard files...");
//        }
//
//        /* Rename all shard files */
//        for(int i=0; i<newShardNum; i++) {
//            /* get shard path and name and file */
//            Path shardPathNameOld = filenames.getShardFilenameAsPath(i, temp);
//            Path shardPathNameNew = filenames.getShardFilenameAsPath(i);
//            File shardFileOld = shardPathNameOld.toFile();
//            File shardFileNew = shardPathNameNew.toFile();
//
//            shardFileOld.renameTo(shardFileNew);
//        }
//
//        if(printStream != null) {
//            printStream.println("Renaming new shard files done. " +
//                    "Rebuilding merkle tree and updating log...");
//        }
//
//        /* Rebuild merkle tree */
//        commitUtxoSetToDisk(true);
//
//        if(printStream != null) {
//            printStream.println("Rebuilding merkle tree and updating log done. Conversion done.");
//        }

//private class PendingShard {
//    Shard shard;
//    List<UTX> utxs;
//    boolean closed;
//
//    private PendingShard(int shardIndex, Path pathname) throws IOException {
//        closed = false;
//        shard = loadShard(shardIndex, pathname);
//        utxs = new LinkedList<>();
//
//        Iterator<UTX> it = shard.getUtxIterator();
//        while(it.hasNext()) {
//            utxs.add(it.next());
//        }
//    }
//
//    /* Expects utxs to be added in sorted order */
//    private void addUTX(UTX utx) throws IOException {
//        if(closed) {
//            throw new IOException("Accessing closed PendingShard");
//        }
//
//        utxs.add(utx);
//    }
//
//    private void commitAndStore(Path pathname) throws IOException {
//        if(closed) {
//            throw new IOException("Accessing closed PendingShard");
//        }
//
//        UTX[] utxsArray = utxs.toArray(new UTX[utxs.size()]);
//
//        shard = getNewShard(getShardNum(), getShardIndex(), utxsArray);
//
//        storeShard(shard, pathname);
//
//        closed = true;
//    }
//
//    private boolean inRange(SHA256HASH txid) {
//        return shard.inRange(txid);
//    }
//
//    private int getShardIndex() {
//        return shard.getShardIndex();
//    }
//
//    private int getShardNum() {
//        return shard.getShardNum();
//    }
//}
//
//
//    private void printProgressBar(PrintStream printStream, int newShardIndex, int oldShardIndex,
//                                  boolean firstCall) {
//        String s1 = String.format("New shard index: %1$-20s", Integer.toString(newShardIndex));
//        String s2 = String.format("Old shard index: %1$-20s", Integer.toString(oldShardIndex));
//        String s = s1 + s2;
//
//        if(!firstCall) {
//            for(int i=0; i<s.length(); i++)
//                printStream.print("\b");
//        }
//        printStream.print(s);
//    }