package Blockchainj.Bitcoin;

import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * TransactionOutput - Transaction Output
 *
 * A transaction output for Bitcoin Protocol. This class keeps redundant data.
 *
 * Once TXID has been set, this class is immutable.
 *
 */


public class TransactionOutput extends TXO {
    /* Transaction Input Metadata */
    /* txid - 32 bytes SHA256 */
    private SHA256HASH txid;

    /* blockhash - 32 bytes SHA256 */
    private final SHA256HASH blockhash;

    /* block height - int32 */
    private final int height;

    /* True if transaction output is coinbase output */
    private final boolean isCoinbase;

    /* Out Index */
    private final int outIndex;


    /* Private constructor. Does not copy input. Does not validate input fully. */
    private TransactionOutput(SHA256HASH blockhash, int height, boolean isCoinbase,
                              int outIndex, byte[] value, byte[] script) {
        super(value, script);
        this.blockhash = blockhash;
        this.height = height;
        this.isCoinbase = isCoinbase;
        this.txid = null;
        this.outIndex = outIndex;
    }


    /* Set/Get methods */
    public SHA256HASH getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public SHA256HASH getTxid() { return txid; }

    public boolean isCoinbase() { return isCoinbase; }

    public int getOutIndex() { return outIndex; }


    /* Set txid. This is done after constructor has been called because all transactions need
       to be parsed before txid is calculated. */
    public void setTxid(SHA256HASH txid) {
        if(this.txid != null) {
            throw new IllegalStateException("TXID already set.");
        }

        this.txid = txid;
    }


    /* print transaction output, mostly for debugging */
    public void print(PrintStream printStream, boolean doMetadata) {
        if (doMetadata) {
            printStream.println(">>Blockhash: " + getBlockhash());
            printStream.println("Height: " + getHeight());
            printStream.println("Txid: " + getTxid());
        }
        System.out.println("isCoinbase: " + isCoinbase);
        super.print(printStream);
    }


    /* Bitcoin serialization */
    public void serialize(OutputStream outputStream) throws IOException {
        outputStream.write(value);
        new CompactSizeUInt(script.length).serialize(outputStream);
        outputStream.write(script);
    }


    public static TransactionOutput deserialize(SHA256HASH blockhash, int height, byte[] data,
                                         int offset, boolean isCoinbase, int outIndex)
            throws BitcoinBlockException {
        try {
            /* Deserialize transaction output */
            /* read value */
            byte[] value = Utils.readBytesFromByteArray(
                    data, offset, BitcoinParams.TRANSACTION_VALUE_SIZE);
            offset += BitcoinParams.TRANSACTION_VALUE_SIZE;

            /* read script bytes */
            CompactSizeUInt scriptBytes = CompactSizeUInt.deserialize(data, offset);
            offset += scriptBytes.getSerializedSize();

            /* read script */
            byte[] script = Utils.readBytesFromByteArray(data, offset, (int)scriptBytes.getValue());

            /* Make new transaction output */
            return new TransactionOutput(blockhash, height, isCoinbase, outIndex, value, script);
        } catch (ArrayIndexOutOfBoundsException e) {
            BitcoinBlockException be = new BitcoinBlockException(
                    "Parsing transaction output failed.", blockhash.toString(), height, e);
            be.setOffset(offset);
            throw be;
        }
    }
}