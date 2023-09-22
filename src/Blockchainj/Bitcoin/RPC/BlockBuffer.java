package Blockchainj.Bitcoin.RPC;

import Blockchainj.Bitcoin.BitcoinBlockException;
import Blockchainj.Bitcoin.Block;

import java.io.PrintStream;
import java.util.NoSuchElementException;

public interface BlockBuffer {
    int MIN_HEIGHT = 0;
    int MAX_HEIGHT = Integer.MAX_VALUE-10;

    /* Returns iterator starting at given height */
    BlockIterator iterator(int startHeight);

    /* Returns iterator starting start height until end height */
    BlockIterator iterator(int startHeight, int endHeight);

    /* Prints blockbuffer parameters */
    void printParameters(PrintStream printStream);

    /* Closes buffer resources */
    void close();

    boolean isClosed();


    interface BlockIterator {
        Block next() throws NoSuchElementException, BitcoinRpcException, BitcoinBlockException;

        boolean hasNext();
    }


    static void validateHeights(int firstHeight, int lastHeight) throws IllegalArgumentException {
        /* first height must be at least MIN_HEIGHT */
        if (firstHeight < MIN_HEIGHT) {
            throw new IllegalArgumentException("startHeight must be >=" + MIN_HEIGHT + ".");
        }

        /* last height must not exceed MAX_HEIGHT */
        if ( lastHeight > MAX_HEIGHT ) {
            throw new IllegalArgumentException("lastHeight must <=" + MAX_HEIGHT + ".");
        }

        /* last height must be >= first height */
        if ( lastHeight < firstHeight ) {
            throw new IllegalArgumentException("lastHeight must be >=startHeight.");
        }
    }
}
