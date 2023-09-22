package Blockchainj.Blockchain;

import Blockchainj.Bitcoin.*;
import Blockchainj.Bitcoin.RPC.BitcoinRpcException;
import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Blockchain.UtxoSet.AbstractUtxoSet;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Blockchain.UtxoSet.UtxoSet;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;


/**
 * Blockchainj.Blockchain
 *
 * Gets blocks in order and builds UtxoSet.
 *
 * This class holds exclusive write access to the AbstractUtxoSet.
 *
 */

public class Blockchain {
    /* Utxo set */
    private final UtxoSet utxoSet;

    /* BlockBuffer producer. */
    private final BlockBuffer blockBuffer;
    private BlockBuffer.BlockIterator blockIterator;

    /* Last processed block and utxo set chagnes */
    private Block latestBlock = null;

    /* Closed marker */
    private volatile boolean closed = false;

    /* Blockchainj.Blockchain Test/Debug/Log parameters */
    /* BlockchainTimer. Timer boolean must be static final! */
    private final BlockchainTimer blockchainTimer = new BlockchainTimer();
    private boolean activeTimer = false;

    /* Printing */
    private PrintStream PRINT_STREAM = System.out;
    private int PRINT_PERIOD = 100;

    /* Shutdown hook thread */
    private Thread shutdownHookThread = null;


    /* New blockchain constructor. If blockchainTimer is null, then timing is inactive. */
    public Blockchain(UtxoSet utxoSet, BlockBuffer blockBuffer) {
        this.utxoSet = utxoSet;
        this.blockBuffer = blockBuffer;

        /* Get unbounded blockbuffer iterator */
        int bestHeight = utxoSet.getBestHeight();
        if(bestHeight == AbstractUtxoSet.UNDEFINED_HEIGHT) {
            blockIterator = blockBuffer.iterator(0);
        } else {
            blockIterator = blockBuffer.iterator(bestHeight+1);
        }
    }

    /* Set methods */
    public synchronized void setPRINT_STREAM(PrintStream PRINT_STREAM) {
        this.PRINT_STREAM = PRINT_STREAM;
    }

    public synchronized void setPRINT_PERIOD(int PRINT_PERIOD) { this.PRINT_PERIOD = PRINT_PERIOD; }

    public synchronized void setActiveTimer(boolean activeTimer) {
        this.activeTimer = activeTimer;
    }


    /* Build blockchain until blockbuffer is out of range.
     * This method is not syncronized, but doCycle() is, so close is accessable. */
    public void buildBlockchain() throws IOException, BitcoinUtxoSetException,
            BitcoinRpcException, BitcoinBlockException {
        if(isClosed()) {
            throw new IllegalStateException("Blockchain is closed.");
        }

        //noinspection StatementWithEmptyBody
        while(doCycle()) {
            /* Blocking here helps for a smooth call to other synchronized methods. */
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                //DO nothing.
            }
        }
    }


    /* Returns true if a block has been processed or false if there's no more blocks to process.
     * Synchronized because close() cannot be called while doCycle() is executing,
     * also because two concurrent calls to doCycle() are not possible either. */
    public synchronized boolean doCycle() throws IOException, BitcoinUtxoSetException,
            BitcoinRpcException, BitcoinBlockException {
        if(isClosed()) {
            return false;
        }

        try {
            /* TIMER */
            if(activeTimer) {
                //noinspection ConstantConditions
                blockchainTimer.startRound();
                blockchainTimer.startTimerForStage(BlockchainTimer.blockBuffer);
            }

            /* get next block */
            latestBlock = blockIterator.next();

            /* check if it follows the blockchain */
            if( !checkNextBlock(latestBlock) ) {
                throw new BitcoinBlockException("Block does not follow blockchain.",
                        latestBlock.getBlockhash().toString(), latestBlock.getHeight());
            }

            /* TIMER */
            if(activeTimer) {
                blockchainTimer.endTimerForStage1startTimerForStage2(
                        BlockchainTimer.blockBuffer, BlockchainTimer.commitBlock);
            }

            /* Commit block to utxoset. Will throw error if utxo set closed. */
//            utxoSet.lockWriteLock();
//            try {
            utxoSet.commitBlock(latestBlock);
//            } finally {
//                utxoSet.unlockWriteLock();
//            }

            /* TIMER */
            if(activeTimer) {
                blockchainTimer.endTimerForStage(BlockchainTimer.commitBlock);
                blockchainTimer.endRound();
            }

            /* print progress */
            if( (PRINT_STREAM != null) && (latestBlock.getHeight()%PRINT_PERIOD ==0 ) ) {
                print(PRINT_STREAM);

                /* print two lines vertical space */
                PRINT_STREAM.print("\n\n");
            }

            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }



    /* Check if this block follows the previous one. */
    private boolean checkNextBlock(Block block) {
        /* Check for genesis block */
        SHA256HASH blockhash = block.getBlockhash();
        if( blockhash.equals(BitcoinParams.GENESIS_BLOCKHASH) ) {
            return true;
        }

        /* Check blockhashes */
        SHA256HASH prevBlockhash = block.getPrevBlockhash();
        SHA256HASH utxoBlockhash = utxoSet.getBestBlockhash();
        if( !utxoBlockhash.equals(prevBlockhash) ) {
            return false;
        }

        /* Check heights */
        int prevHeight = block.getHeight() - 1;
        int utxoHeight = utxoSet.getBestHeight();
        //noinspection RedundantIfStatement
        if( prevHeight != utxoHeight ) {
            return false;
        }

        return true;
    }


    /** Close blockchain.
     *  This method safely closes the blockchain by completing any commit or other write
     *  operations to the utxo set.
     *  Synchronized because doCycle() and close() should not be executed together.
     */
    public synchronized void close() throws IOException {
        if(isClosed()) {
            return;
        }

        /* Print closing... message */
        if(PRINT_STREAM != null)
            PRINT_STREAM.println("Close blockchain called from thread: "
                    + Thread.currentThread().getId() + "\n" +
                    "Closing Blockchain...\nClosing UtxoSet...");

        /* Close utxo set. */
        if(!utxoSet.isClosed()) {
            utxoSet.close();
        }

        /* Print closed mesasge */
        if(PRINT_STREAM != null)
            PRINT_STREAM.println("Utxo closed.\nBlockchain closed.");

        /* Remove shutdown hook */
        if(shutdownHookThread != null) {
            if(Thread.currentThread().getId() != shutdownHookThread.getId()) {
                Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
            }
        }

        /* Mark blockchain as closed */
        closed = true;
    }


    public synchronized boolean isClosed() { return closed; }


    /* Hook SIGINT to close safely. */
    public void hookSIGINTtoClose() {
        if(shutdownHookThread != null) {
            return;
        }

        //noinspection AnonymousHasLambdaAlternative
        shutdownHookThread =  new Thread() {
            @Override
            public void run() {
                try {
                    close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }


    /* Get latest block. Block is immutable. */
    public synchronized Block getLatestBlock() { return latestBlock; }


    /** Prints blockchain state, along with it's utxo set state info **/
    public synchronized void print(PrintStream printStream) {
        printStream.println("Blockchain:");
        printStream.println("Print called from thread: " + Thread.currentThread().getId());
        if(latestBlock != null) {
            printStream.println("Latest block height: " + latestBlock.getHeight());
            printStream.println("Latest block blockhash: " + latestBlock.getBlockhash());
        }
        if(activeTimer) {
            blockchainTimer.print(printStream, true, true, true);
        }

        utxoSet.print(printStream);



//        /* get read lock */
//        utxoSet.lockReadLock();
//        try {
//        } finally {
//            utxoSet.unlockReadLock();
//        }
    }


    /* Print blockchain parameters */
    public void printParameters(PrintStream printStream) {
        printStream.println(">Blockchainj.Blockchain parameters:");
        printStream.println("Blockchainj.Blockchain active timer: " + activeTimer);
        printStream.println("Blockchainj.Blockchain do print: " + (PRINT_STREAM!=null));
        printStream.println("Blockchainj.Blockchain print period: " + PRINT_PERIOD);
        utxoSet.printParameters(printStream);
        blockBuffer.printParameters(printStream);
    }


//    /* Keeps track of misc stats */
//    private class Stats {
//        /* Modified shards moving average */
//        MathAverage modifiedShards = new MathAverage(200);
//        int modifiedShardsCurrentHeight;
//
//        /* Txids in block moving average */
//        MathAverage txidInBlock = new MathAverage(200);
//        int txidInBlockCurrentHeight;
//
//        private Stats() {
//
//        }
//
//        private void updateModifiedShards(UtxoSetChanges changes) {
//            modifiedShards.push(changes.getModifiedShardCount());
//            modifiedShardsCurrentHeight = changes.getHeight();
//        }
//
//        private void updateTxidInBlock(UtxoSetChanges changes) {
//            txidInBlock.push(changes.calcTxidCount());
//            txidInBlockCurrentHeight = changes.getHeight();
//        }
//
//        private void printModifiedShards(PrintStream printStream) {
//            /* Print average modified shards */
//            printStream.println("Average modified shards for height "
//                    + modifiedShardsCurrentHeight + " is "
//                    + modifiedShards.getMovingAvg()
//                    + " for the past " + modifiedShards.getTrailLen() + " blocks.");
//        }
//
//        private void printTxidInBlock(PrintStream printStream) {
//            printStream.println("Average TXIDs in blocks for height "
//                    + txidInBlockCurrentHeight + " is "
//                    + txidInBlock.getMovingAvg()
//                    + " for the past " + txidInBlock.getTrailLen() + " blocks.");
//        }
//
//        private void updateAll(UtxoSetChanges changes) {
//            updateModifiedShards(changes);
//            updateTxidInBlock(changes);
//        }
//
//        private void printAll(PrintStream printStream) {
//            printModifiedShards(printStream);
//            printTxidInBlock(printStream);
//        }
//    }
}