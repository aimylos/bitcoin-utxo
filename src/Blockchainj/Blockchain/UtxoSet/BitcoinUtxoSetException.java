package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Util.SHA256HASH;

/**
 * Blockchainj.Bitcoin Utxo Set exception for miscellaneous utxo set errors.
 */

public class BitcoinUtxoSetException extends Exception {
    private String blockhash;
    private int height;
    /* excpeting UTX, STX, TXI or TXO */
    private Object obj;
    private Object obj2;



    public BitcoinUtxoSetException(String message, String blockhash, int height, Exception e) {
        super(message, e);
        this.blockhash = blockhash;
        this.height = height;
        this.obj = null;
        this.obj2 = null;
    }

    public BitcoinUtxoSetException(String message, String blockhash, int height) {
        super(message);
        this.blockhash = blockhash;
        this.height = height;
        this.obj = null;
        this.obj2 = null;
    }

    public BitcoinUtxoSetException(String message, String blockhash, int height, Object obj) {
        super(message);
        this.blockhash = blockhash;
        this.height = height;
        this.obj = obj;
        this.obj2 = null;
    }

    public BitcoinUtxoSetException(String message, String blockhash, int height, Object obj,
                                   Object obj2) {
        super(message);
        this.blockhash = blockhash;
        this.height = height;
        this.obj = obj;
        this.obj2 = obj2;
    }


    public BitcoinUtxoSetException(String message, Exception e) {
        super(message, e);
        this.blockhash = SHA256HASH.ZERO_SHA256HASH.toString();
        this.height = -1;
        this.obj = null;
        this.obj2 = null;
    }


    public BitcoinUtxoSetException(String message, Object obj) {
        super(message);
        this.blockhash = SHA256HASH.ZERO_SHA256HASH.toString();
        this.height = -1;
        this.obj = obj;
        this.obj2 = null;
    }


    public BitcoinUtxoSetException(String message, Object obj, Object obj2) {
        super(message);
        this.blockhash = SHA256HASH.ZERO_SHA256HASH.toString();
        this.height = -1;
        this.obj = obj;
        this.obj2 = obj2;
    }



    @Override
    public String getMessage() {
        String mainMsg = super.getMessage() + "\nBlockhash: " + blockhash + "\nHeight: " + height;

        if(obj != null && obj2 != null) {
            return  mainMsg + "\n" + obj.toString() + "\n" + obj2.toString();
        }
        else if(obj != null) {
            return  mainMsg + "\n" + obj.toString();
        }
        else {
            return mainMsg;

        }
    }

    public String getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public void setBlockhash(String blockhash) { this.blockhash = blockhash; }

    public void setHeight(int height) { this.height = height; }
}
