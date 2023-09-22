//package Blockchainj.Bitcoin.RPC;
//
//import Blockchainj.Bitcoin.BitcoinBlockException;
//import Blockchainj.Bitcoin.Block;
//
//import java.io.PrintStream;
//import java.util.concurrent.TimeUnit;
//
///**
// * BlockBufferAdvance
// *
// * Parameterized on SimpleBlockBuffer and ConcurrentBlockBuffer.
// * Has stayBehind option.
// * Updates on best available height.
// */
//
@Deprecated
public class BlockBufferAdvance {//implements BlockBuffer {
//    /* Default parameters */
//    public static final int DEFAULT_STAY_BEHIND = 12;
//    public static final boolean DEFAULT_BLOCKBUFFER_CONCURRENT = false;
//    public static final int DEFAULT_BLOCKBUFFER_THREADS = 1;
//    public static final int DEFAULT_BLOCKBUFFER_SIZE = 4;
//
//    /* How many blocks to stay behind the best height RPC server has to offer */
//    private final int stayBehind;
//
//    /* RPC connection */
//    private final RPCconnection rpcCon;
//
//    /* Blockbuffer parameters */
//    private final boolean BLOCKBUFFER_CONCURRENT;
//    private final int BLOCKBUFFER_THREADS;
//    private final int BLOCKBUFFER_SIZE;
//
//    /* BlockBuffer */
//    private BlockBuffer blockBuffer = null;
//    private int nextHeight = 0;
//
//
//    /* Main constructor */
//    public BlockBufferAdvance(RPCconnection rpcCon,
//                              boolean CONCURRENT, int MAX_THREADS, int BUFFER_SIZE,
//                              int stayBehind) {
//        this.rpcCon = rpcCon;
//        this.BLOCKBUFFER_CONCURRENT = CONCURRENT;
//        this.BLOCKBUFFER_THREADS = MAX_THREADS;
//        this.BLOCKBUFFER_SIZE = BUFFER_SIZE;
//        this.stayBehind = stayBehind;
//    }
//
//
//    /* Default constructor */
//    public BlockBufferAdvance(RPCconnection rpcCon) {
//        this(rpcCon,
//                DEFAULT_BLOCKBUFFER_CONCURRENT,
//                DEFAULT_BLOCKBUFFER_THREADS, DEFAULT_BLOCKBUFFER_SIZE,
//                DEFAULT_STAY_BEHIND);
//    }
//
//
//    /* Creates new blockbuffer */
//    private BlockBuffer getBlockBuffer(int startHeight, int endHeight) {
//        if(BLOCKBUFFER_CONCURRENT) {
//            /* Concurrent blockbuffer */
//            ConcurrentBlockBuffer concurrentBlockBuffer = new ConcurrentBlockBuffer(
//                    startHeight, endHeight, rpcCon,
//                    BLOCKBUFFER_SIZE, BLOCKBUFFER_THREADS);
//
//            concurrentBlockBuffer.start();
//
//            return concurrentBlockBuffer;
//        } else {
//            /* Simple blockbuffer */
//            //noinspection UnnecessaryLocalVariable
//            SimpleBlockBuffer simpleBlockBuffer = new SimpleBlockBuffer(
//                    startHeight, endHeight, rpcCon);
//
//            return simpleBlockBuffer;
//        }
//    }
//
//
//    /* Creates new blockbuffer and stays behind */
//    private BlockBuffer getBlockBufferStayBehind(int startHeight, int stayBehind)
//            throws BitcoinRpcException {
//        final int triesToGetBestHeight = 2;
//        final int secondsToWaitBetweenTries = 3;
//
//        if(stayBehind < 0) {
//            throw new IllegalArgumentException("stayBehind must be >=0");
//        }
//
//        /* Get best height */
//        int bestHeight;
//        for (int i=1;;i++) {
//            try {
//                bestHeight = rpcCon.getBlockCount();
//                break;
//            } catch (BitcoinRpcException e) {
//                if(i == triesToGetBestHeight) {
//                    throw e;
//                } else {
//                    try {
//                        TimeUnit.SECONDS.sleep(secondsToWaitBetweenTries);
//                    } catch (InterruptedException e2) {
//                        throw e;
//                    }
//                }
//            }
//        }
//
//        /* check heights */
//        int currentHeight = startHeight-1;
//        int endHeight = bestHeight - stayBehind;
//
//        if( endHeight <= currentHeight) {
//            /* Return null */
//            return null;
//        }
//        else {
//            /* Return new blockbuffer. */
//            return getBlockBuffer(startHeight, endHeight);
//        }
//    }
//
//
//    @Override
//    public void setNextHeight(int nextHeight) throws IndexOutOfBoundsException {
//        try {
//            if(blockBuffer != null) {
//                blockBuffer.close();
//            }
//
//            blockBuffer = getBlockBufferStayBehind(nextHeight, stayBehind);
//
//            /* It's ok if it's null. It's gonna be picked up as an end of range. */
////            if(blockBuffer == null) {
////                throw new IndexOutOfBoundsException();
////            }
//
//            this.nextHeight = nextHeight;
//        } catch (BitcoinRpcException | IllegalArgumentException e) {
//            throw  new IndexOutOfBoundsException(e.toString());
//        }
//    }
//
//
//    /* Returns next block by height. */
//    private Block getNextBlock(int depth)
//            throws EndOfRangeException, BitcoinRpcException, BitcoinBlockException {
//        if(blockBuffer == null) {
//            blockBuffer = getBlockBufferStayBehind(nextHeight, stayBehind);
//        }
//
//        if(blockBuffer == null) {
//            throw new EndOfRangeException("No more blocks available.");
//        }
//
//        try {
//            Block block = blockBuffer.getNextBlock();
//            if(nextHeight != block.getHeight()) {
//                throw new EndOfRangeException("Unexpected height. " +
//                        "Expected " + nextHeight + " but got " + block.getHeight());
//            }
//            nextHeight++;
//
//            return block;
//        } catch (EndOfRangeException e) {
//            /* Try once more with new blockbuffer */
//            blockBuffer.close();
//            blockBuffer = null;
//
//            if(depth >= 2) {
//                throw new EndOfRangeException("No more blocks available");
//            } else {
//                return getNextBlock(depth+1);
//            }
//        }
//    }
//
//
//    @Override
//    public Block getNextBlock()
//            throws EndOfRangeException, BitcoinRpcException, BitcoinBlockException {
//        return getNextBlock(1);
//    }
//
//
//    /* Returns the height of the block that getNextBlock() will return */
//    @Override
//    public int getNextHeight() throws EndOfRangeException {
//        return nextHeight;
//    }
//
//
//    @Override
//    public void close() {
//        if(blockBuffer != null) {
//            blockBuffer.close();
//        }
//        blockBuffer = null;
//    }
//
//
//    @Override
//    public void printParameters(PrintStream printStream) {
//        printStream.println(">BlockBufferAdvance parameters:");
//        if(BLOCKBUFFER_CONCURRENT) {
//            printStream.println("BlockBufferAdvance concurrent: " + BLOCKBUFFER_CONCURRENT);
//            printStream.println("BlockBufferAdvance concurrent threads: " + BLOCKBUFFER_THREADS);
//            printStream.println("BlockBufferAdvance buffer size: " + BLOCKBUFFER_SIZE);
//        } else {
//            printStream.println("BlockBufferAdvance concurrent: " + BLOCKBUFFER_CONCURRENT);
//        }
//        printStream.println("BlockBufferAdvance stay behind: " + stayBehind);
//        rpcCon.printParameters(printStream);
//    }
//
}
