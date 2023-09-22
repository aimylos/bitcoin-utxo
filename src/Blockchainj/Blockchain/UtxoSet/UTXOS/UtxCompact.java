package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.TXI;
import Blockchainj.Bitcoin.Transaction;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;


/**
 * UtxCompact - Implements UTX
 *
 * In expense of more computing time this implementation is more effiecient in memory.
 *
 */


public class UtxCompact implements UTX {
    public static final int UTX_TYPE = 2;

    /* The UTX data storage serialized. */
    private final byte[] utxStorageSerialized;

    /* Prototype Protocol serialized size. */
    private final int serializedSize;

    /* Actual utxo count. */
    private final int utxoCount;


    /* Main constructor */
    public UtxCompact(Transaction transaction)
            throws IllegalArgumentException {
        this(new UtxFast(transaction));
    }


    /* Private constructor */
    private UtxCompact(UtxFast utxFast) {
        /* Keep utx as byte array */
        try {
            ByteArrayOutputStream outputStream =
                    Utils.getNewOutputStream((int)utxFast.getStorageSerializedSize());

            utxFast.store(outputStream);

            utxStorageSerialized = outputStream.toByteArray();
            serializedSize = (int)utxFast.getSerializedSize();
            utxoCount = utxFast.getUtxosCount();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /* Loads UtxFast from byte array */
    private UtxFast loadUtx() {
        try {
            InputStream inputStream = new ByteArrayInputStream(utxStorageSerialized);

            return UtxFast.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public int getUtxosCount() { return utxoCount; }


    @Override
    public boolean hasUtxo(int outIndex) { return loadUtx().hasUtxo(outIndex); }


    @Override
    public UTXO getUtxo(int outIndex) { return loadUtx().getUtxo(outIndex); }


    @Override
    public UTX spentUTXO(TXI txi, int height)
            throws BitcoinUtxoSetException, IllegalArgumentException {
        UtxFast utxFast = loadUtx();

        UtxFast newUtx = (UtxFast)utxFast.spentUTXO(txi, height);

        if(newUtx != null) {
            return new UtxCompact(newUtx);
        } else {
            return null;
        }
    }


    /** Prototype Protocol serialization. */
    @Override
    public void serialize(OutputStream outputStream) throws IOException {
        UtxFast utxFast = loadUtx();

        utxFast.serialize(outputStream);
    }


    /** Prototype Protocol deserialization */
    public static UtxCompact deserialize(InputStream inputStream) throws IOException {
        UtxFast utxFast = UtxFast.deserialize(inputStream);

        return new UtxCompact(utxFast);
    }


    /** Storage serialization */
    @Override
    public void store(OutputStream outputStream) throws IOException {
        outputStream.write(utxStorageSerialized);
    }


    /** Storage deserialization */
    public static UtxCompact load(InputStream inputStream) throws IOException {
        UtxFast utxFast = UtxFast.load(inputStream);

        return new UtxCompact(utxFast);
    }


    /* Get methods */
    @Override
    public SHA256HASH getTxid() {
        return new SHA256HASH(Arrays.copyOf(utxStorageSerialized, 32));
    }

    @Override
    public long getVersion() { return loadUtx().getVersion(); }

    @Override
    public byte[] getVersionBytes() { return loadUtx().getVersionBytes(); }

    @Override
    public int getHeight() { return loadUtx().getHeight(); }

    @Override
    public boolean isCoinbase() { return loadUtx().isCoinbase(); }


    /* Serialized size for Prototype Protocol serialization. */
    @Override
    public long getSerializedSize() { return (long)serializedSize; }

    /* Serialized size for Storage serialization. */
    @Override
    public long getStorageSerializedSize() { return (long)utxStorageSerialized.length; }


    @Override
    public Iterator<UTXO> getUtxoIterator() { return loadUtx().getUtxoIterator(); }


    @Override
    public String toString() { return "UTX_Txid: " + getTxid().toString(); }


    /* compareTo method. Compare TXIDs. */
    @Override
    public int compareTo(UTX o) { return loadUtx().compareTo(o); }


    /* DEBUG ONLY */
    @Override
    public void print(PrintStream printStream, boolean doUtxos) {
        loadUtx().print(printStream, doUtxos);
    }


    @Override
    public void printParameters(PrintStream printStream) {
        printStream.println("UTX type: " + this.getClass().toString());
    }
}
