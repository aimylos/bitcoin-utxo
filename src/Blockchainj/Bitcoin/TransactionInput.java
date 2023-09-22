package Blockchainj.Bitcoin;

import Blockchainj.Util.SHA256HASH;

import java.io.PrintStream;

/**
 * TransactionInput - Blockchainj.Bitcoin Transaction Input
 *
 * A transaction input with metadata.
 *
 * Once TXID has been set, this class is immutable.
 *
 */

public class TransactionInput extends TXI {
    /* Transaction Input Metadata */
    /* txid - 32 bytes SHA256 */
    private SHA256HASH txid;

    /* blockhash - 32 bytes SHA256 */
    private final SHA256HASH blockhash;

    /* block height - int32 */
    private final int height;

    /* Coinbase indicator */
    private final boolean isCoinbase;


    /* Private constructor. Does not copy input. Does not validate input fully. */
    private TransactionInput(SHA256HASH blockhash, int height, TXI txi, boolean isCoinbase) {
        super(txi);
        this.blockhash = blockhash;
        this.height = height;
        this.txid = null; //TXID not set
        this.isCoinbase = isCoinbase;
    }



    /* Set/Get methods */
    public SHA256HASH getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public SHA256HASH getTxid() { return txid; }

    /* Set txid. This is done after constructor has been called because all transactions need
       to be parsed before txid is calculated. Can only be set once. */
    public void setTxid(SHA256HASH txid) {
        if(this.txid != null) {
            throw new IllegalStateException("TXID already set.");
        }

        this.txid = txid;
    }

    public boolean isCoinbase() { return isCoinbase; }


    /* print transaction input, mostly for debugging */
    public void print(PrintStream printStream, boolean doMetadata) {
        if (doMetadata) {
            printStream.println(">>Blockhash: " + getBlockhash());
            printStream.println("Height: " + getHeight());
            printStream.println("Txid: " + getTxid());
            printStream.println("isCoinbase: " + isCoinbase());
        }
        super.print(printStream);
    }


    /* Blockchainj.Bitcoin deserialization. */
    public static TransactionInput deserialize(SHA256HASH blockhash, int height, byte[] data,
                                               int offset, boolean isCoinbase)
            throws BitcoinBlockException {
        try {
            /* Deserialize transaction input */
            TXI txi = TXI.deserialize(data, offset);

            /* Make new transaction input */
            return new TransactionInput(blockhash, height, txi, isCoinbase);
        } catch (ArrayIndexOutOfBoundsException e) {
            BitcoinBlockException be = new BitcoinBlockException(
                    "Parsing transaction input failed.", blockhash.toString(), height, e);
            be.setOffset(offset);
            throw be;
        }
    }
}
