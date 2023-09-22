package Blockchainj.Bitcoin.RPC;

import Blockchainj.Bitcoin.BitcoinBlockException;
import Blockchainj.Bitcoin.Block;
import Blockchainj.Util.SHA256HASH;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.concurrent.*;


/**
 * ConcurrentBlockBuffer
 *
 * Multithread block buffer. Uses one ThreadPoolExcecutor to manage the threads.
 *
 * Will create up to coreThreads+1. But the producer thread will mostly sleep.
 *
 * RPCconnection must be thread-safe and have a limit on concurrent HTTP connections
 * because this class does not have any way of adjusting that.
 *
 */

public class ConcurrentBlockBuffer extends AbstractBlockBuffer {
    /* Core and max thread count. */
    public static final int DEFAULT_CORE_THREADS = Runtime.getRuntime().availableProcessors();
    private final int CORE_THREADS;

    /* Buffer size. The max number of blocks queued in the buffer. */
    public static final int DEFAULT_BUFFER_SIZE = 16;
    private final int BUFFER_SIZE;

    /* Blocking queue timeout.
     * Since futures are immediately submitted there should be any major waiting timing
     * waiting for it. */
    private static final long BLOCKING_QUEUE_TIMEOUT = 20*1000; //20 seconds
    /* Thread timeout */
    private final long THREAD_TIMEOUT;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    /* blocks range in terms of height */
    private final int firstHeight;
    private final int lastHeight;

    /* RPC connection */
    private final RPCconnection RPC_CON;


    /* Main constructor. */
    public ConcurrentBlockBuffer(int firstHeight, int lastHeight, RPCconnection rpcCon,
                                 int bufferSize, int coreThreads)
            throws IllegalArgumentException {
        super();

        BlockBuffer.validateHeights(firstHeight, lastHeight);

        /* set height range */
        this.firstHeight = firstHeight;
        this.lastHeight = lastHeight;

        /* set buffer size */
        if(bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be >0.");
        } else {
            this.BUFFER_SIZE = bufferSize;
        }

        /* set core threads */
        if(coreThreads < 1) {
            throw new IllegalArgumentException("Core threads must be >=1.");
        } else {
            this.CORE_THREADS = coreThreads;
        }

        /* set rpc connection */
        this.RPC_CON = rpcCon;

        /* Set thread time out to rpcTimeout + 2 minutes */
        THREAD_TIMEOUT = rpcCon.getTimeoutMillis() + (120 * 1000);
    }


    /* Default constructor */
    public ConcurrentBlockBuffer(RPCconnection rpcCon)
            throws IllegalArgumentException {
        this(MIN_HEIGHT, MAX_HEIGHT, rpcCon, DEFAULT_BUFFER_SIZE, DEFAULT_CORE_THREADS);
    }


    /* Block iterator */
    private class ConcurrentBlockBufferBlockIterator extends AbstractBlockIterator {
        private final int startHeight;
        private final int endHeight;
        private int nextHeight;

        /* Actual block buffer */
        private final ArrayBlockingQueue<Future<Block>> futureBlockQueue =
                new ArrayBlockingQueue<>(BUFFER_SIZE);

        /* Producer thread */
        private final Producer producer = new Producer();


        private ConcurrentBlockBufferBlockIterator(int startHeight, int endHeight) {
            super();
            this.startHeight = startHeight;
            this.endHeight = endHeight;
            nextHeight = startHeight;
            producer.start();
        }


        /* Producer thread. */
        private class Producer extends Thread {
            private Exception e;
            @Override
            public void run() {
                ThreadPoolExecutor threadPoolExecutor = null;
                try {
                    /* Unbounded blocking queue for thread pool excecutor.
                     * The producer (this thread), will never submit more than
                     * bufferSize threads at a time. */
                    LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();

                    /* Init thread pool. As long as Unbounded queue is used,
                     * maxThreads is equeal to core threads. */
                    threadPoolExecutor = new ThreadPoolExecutor(CORE_THREADS, CORE_THREADS,
                            THREAD_TIMEOUT, TIMEOUT_UNIT, blockingQueue);

                    /* Get all blocks */
                    for(int h=startHeight; h<=endHeight; h++) {
                        if(isInterrupted()) {
                            throw new InterruptedException();
                        }

                        /* Prepare thread */
                        Worker worker = new Worker(h);

                        /* Submit new task */
                        Future<Block> future = threadPoolExecutor.submit(worker);

                        /* Put future in queue. This will block is queue is full. */
                        futureBlockQueue.put(future);
                    }
                } catch (Exception e) {
                    this.e = e;
                    if(threadPoolExecutor != null) {
                        threadPoolExecutor.shutdownNow();
                    }
                } finally {
                    if(threadPoolExecutor != null) {
                        threadPoolExecutor.shutdown();
                    }
                }
            }
        }


        /* Requests the blockhash for the given height. Requests the block for the given blockhash.
         * Creates a new Block from the request response. */
        private class Worker implements Callable<Block> {
            private final int height;

            public Worker(int height) { this.height = height; }

            @Override
            public Block call() throws BitcoinRpcException, BitcoinBlockException {
                SHA256HASH blockhash = null;
                try {
                    /* get blockhash for height */
                    blockhash = SHA256HASH.getReverseHash(RPC_CON.getBlockhashByHeight(height));

                    /* get raw block for blockhash */
                    byte[] rawBlock = RPC_CON.getRawBlockByBlockhash(blockhash.toString());

                    /* get block from raw block */
                    return Block.deserialize(blockhash, height, rawBlock, 0);

                } catch (BitcoinRpcException e) {
                    if (blockhash != null) {
                        e.setBlockhash(blockhash.toString());
                    }
                    e.setHeight(height);
                    throw e;
                }
            }
        }


        @Override
        protected void close() {
            super.close();
            producer.interrupt();
        }


        public Block next()
                throws NoSuchElementException, BitcoinRpcException, BitcoinBlockException {
            /* check end of range */
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                /* Poll without waiting if queue has something to offer */
                Future<Block> future = futureBlockQueue.poll();

                /* If queue was empty check if producer thread is still alive */
                if(future == null) {
                    /* If thread is alive poll with timeout */
                    if(producer.isAlive()) {
                        future = futureBlockQueue.poll(BLOCKING_QUEUE_TIMEOUT, TIMEOUT_UNIT);
                    }

                    /* If future is still null or producer thread is dead, end everything */
                    if(future == null) {
                        if(!producer.isAlive()) {
                            if(producer.e != null) {
                                throw  producer.e;
                            } else {
                                throw  new InterruptedException();
                            }
                        } else {
                            throw new TimeoutException();
                        }
                    }
                }

                /* Wait for future to join and get the return value. */
                Block block;
                try {
                    block = future.get();
                } catch (ExecutionException e) {
                    throw (Exception) e.getCause();
                }

                /* Verify height continuity */
                if(nextHeight != block.getHeight()) {
                    throw new RuntimeException("Expected height " + nextHeight +
                            " but found height " + block.getHeight());
                }

                return block;
            } catch (BitcoinRpcException | BitcoinBlockException e) {
                throw e;
            } catch (Exception e) {
                close();
                throw new NoSuchElementException(e.toString());
            } finally {
                /* Increament next height */
                nextHeight++;
            }
        }


        public boolean hasNext() {
            return (nextHeight <= endHeight) && !isClosed();
        }
    }


    @Override
    public BlockIterator iterator(int startHeight) {
        if( startHeight < firstHeight ) {
            throw new IllegalArgumentException("Start height out of range.");
        }

        return new ConcurrentBlockBufferBlockIterator(startHeight, lastHeight);
    }

    @Override
    public BlockIterator iterator(int startHeight, int endHeight) {
        if( startHeight < firstHeight ) {
            throw new IllegalArgumentException("Start height out of range.");
        }
        if( endHeight < startHeight ) {
            throw new IllegalArgumentException("End height must be >=startHeight.");
        }
        endHeight = (endHeight>lastHeight)?(lastHeight):endHeight;

        return new ConcurrentBlockBufferBlockIterator(startHeight, endHeight);
    }


    /* Closes buffer resources */
    @Override
    public void close() {
        super.close();
    }


    public int getFirstHeight() { return firstHeight; }

    public int getLastHeight() { return lastHeight; }


    @Override
    public void printParameters(PrintStream printStream) {
        printStream.println(">ConcurrentBlockBuffer");
        printStream.println("ConcurrentBlockBuffer first height: " + getFirstHeight());
        printStream.println("ConcurrentBlockBuffer last height: " + getLastHeight());
        printStream.println("ConcurrentBlockBuffer thread number: " + CORE_THREADS);
        printStream.println("ConcurrentBlockBuffer buffer size: " + BUFFER_SIZE);
        RPC_CON.printParameters(printStream);
    }


    public static void main(String[] args){
        int startHeight = 0; //Integer.parseInt(args[0]);
        int endHeight = 500000; //Integer.parseInt(args[1]);
        int printPeriod = 100; //Integer.parseInt(args[2]);
        ConcurrentBlockBuffer sbb = new ConcurrentBlockBuffer(new RPCconnection());
        sbb.printParameters(System.out);

        BlockIterator blockIterator = sbb.iterator(0, 1000);

        while(blockIterator.hasNext()){
            try {
                Block block = blockIterator.next();

                if(block.getHeight()%printPeriod == 0) {
                    block.print(System.out, true, false, false);
                    System.out.println("\n\n\n");
                }
            } catch (NoSuchElementException | BitcoinRpcException | BitcoinBlockException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
