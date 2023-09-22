package Blockchainj.Bitcoin.RPC;


import Blockchainj.Bitcoin.BitcoinBlockException;
import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Bitcoin.Block;
import Blockchainj.Util.SHA256HASH;

import java.io.PrintStream;
import java.util.NoSuchElementException;

public class SimpleBlockBuffer extends AbstractBlockBuffer {
    /* blocks range in terms of height */
    private final int firstHeight;
    private final int lastHeight;

    /* RPC connection */
    private final RPCconnection rpcCon;

    /* main constructor */
    public SimpleBlockBuffer(int firstHeight, int lastHeight, RPCconnection rpcCon)
            throws IllegalArgumentException {
        super();

        BlockBuffer.validateHeights(firstHeight, lastHeight);

        /* set height range */
        this.firstHeight = firstHeight;
        this.lastHeight = lastHeight;

        /* set rpc connection */
        this.rpcCon = rpcCon;
    }


    /* Default constructor */
    public SimpleBlockBuffer(RPCconnection rpcCon) {
        this(MIN_HEIGHT, MAX_HEIGHT, rpcCon);
    }


    /* Block iterator */
    private class SimpleBlockBufferBlockIterator extends AbstractBlockIterator {
        private final int startHeight;
        private final int endHeight;
        private int nextHeight;

        private SimpleBlockBufferBlockIterator(int startHeight, int endHeight) {
            super();
            this.startHeight = startHeight;
            this.endHeight = endHeight;
            this.nextHeight = startHeight;
        }

        public Block next()
                throws NoSuchElementException, BitcoinRpcException, BitcoinBlockException {
            /* check end of range */
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            SHA256HASH blockhash = BitcoinParams.ZERO_BLOCKHASH;
            try {
                /* get blockhash for next height */
                blockhash = SHA256HASH.getReverseHash(rpcCon.getBlockhashByHeight(nextHeight));

                /* get raw block using blockhash */
                byte[] rawBlock = rpcCon.getRawBlockByBlockhash(blockhash.toString());

                /* parse raw block and get block */
                return Block.deserialize(blockhash, nextHeight, rawBlock, 0);
            } catch (BitcoinRpcException e) {
                e.setBlockhash(blockhash.toString());
                e.setHeight(nextHeight);
                close();
                throw e;
            } finally {
                /* increment height */
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

        return new SimpleBlockBufferBlockIterator(startHeight, lastHeight);
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

        return new SimpleBlockBufferBlockIterator(startHeight, endHeight);
    }


    @Override
    public void close() {
        super.close();
    }

    public int getFirstHeight() { return firstHeight; }

    public int getLastHeight() { return lastHeight; }


    public void printParameters(PrintStream printStream) {
        printStream.println(">SimpleBlockBuffer");
        printStream.println("SimpleBlockBuffer first height: " + getFirstHeight());
        printStream.println("SimpleBlockBuffer last height: " + getLastHeight());
        rpcCon.printParameters(printStream);
    }


//    public static void main(String[] args){
//        int startHeight = 0; //Integer.parseInt(args[0]);
//        int endHeight = 500000; //Integer.parseInt(args[1]);
//        int printPeriod = 100; //Integer.parseInt(args[2]);
//        SimpleBlockBuffer sbb = new SimpleBlockBuffer(new RPCconnection());
//
//        BlockIterator blockIterator = sbb.iterator(0, 1000);
//
//        while(blockIterator.hasNext()){
//            try {
//                Block block = blockIterator.next();
//
//                if(block.getHeight()%printPeriod == 0) {
//                    block.print(System.out, true, false, false);
//                    System.out.println("\n\n\n");
//                }
//            } catch (NoSuchElementException | BitcoinRpcException | BitcoinBlockException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
}
