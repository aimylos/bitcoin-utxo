package Blockchainj.Bitcoin;

import Blockchainj.Util.SHA256HASH;

/**
 * BitcoinParams - Bitcoin Protocol Utilities
 *
 */


public class BitcoinUtils {
    /** Bitcoin block height */
    /* Validate height */
    public static void validateHeight(int height) throws IllegalArgumentException {
        if(height < BitcoinParams.GENESIS_HEIGHT) {
            throw new IllegalArgumentException("Height must be >=" + BitcoinParams.GENESIS_HEIGHT);
        }
    }



    /** Bitcoin duplicate transactions exception */
    public static boolean isDuplicateTxid(Transaction t) {
        int height = t.getHeight();

        if (height <= BitcoinParams.NON_UNIQUE_TXIDS_LAST_HEIGHT &&
                height >= BitcoinParams.NON_UNIQUE_TXIDS_FIRST_HEIGHT) {
            SHA256HASH txid = t.getTxid();
            for (int i = 0; i < BitcoinParams.NON_UNIQUE_TXIDS.length; i++) {
                if (txid.equals(BitcoinParams.NON_UNIQUE_TXIDS[i])) {
                    /* If it's not first appearance skip transaction else process it. */
                    if (height != BitcoinParams.NON_UNIQUE_TXIDS_FIRST_SEEN[i]) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
