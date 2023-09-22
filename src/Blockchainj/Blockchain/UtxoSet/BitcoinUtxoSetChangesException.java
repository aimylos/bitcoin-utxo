package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Util.SHA256HASH;

/**
 * Pretty much the same as BitcoinUtxoSetException but different name.
 */

public class BitcoinUtxoSetChangesException extends BitcoinUtxoSetException {
    public BitcoinUtxoSetChangesException(String message) {
        super(message, SHA256HASH.ZERO_SHA256HASH, -1);
    }

    public BitcoinUtxoSetChangesException(String message, String blockhash, int height,
                                          Exception e) {
        super(message, blockhash, height, e);
    }

    public BitcoinUtxoSetChangesException(String message, String blockhash, int height) {
        super(message, blockhash, height);
    }

    public BitcoinUtxoSetChangesException(String message, String blockhash, int height,
                                          Object obj) {
        super(message, blockhash, height, obj);
    }

    public BitcoinUtxoSetChangesException(String message, String blockhash, int height,
                                          Object obj, Object obj2) {
        super(message, blockhash, height, obj, obj2);
    }
}
