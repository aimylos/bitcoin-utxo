package Blockchainj.Bitcoin;

import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.SHA256OutputStream;
import Blockchainj.Util.Utils;

import org.apache.commons.codec.binary.Hex;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Transaction - Bitcoin transaction
 *
 * Immutable class.
 *
 * All bitcoin values are kept as byte arrays with protocol endianness.
 *
 * SegWit. Witnesses are kept as raw data but not used.
 *
 * Blockchainj.Bitcoin protocol serialization.
 *
 * Internal byte order is Blockchainj.Bitcoin serialization order.
 *
 */

public class Transaction {
    /* Transaction Metadata */
    /* txid - SHA256 */
    private final SHA256HASH txid;
    /* blockhash - 32 bytes SHA256 */
    private final SHA256HASH blockhash;
    /* block height - int32 */
    private final int height;
    /* transaction size in bytes */
    private final int serializedSize;
    private final int serializedSizeNoWitnesses;
    /* coinbase indicator */
    private final boolean isCoinbase;

    /* Transaction data */
    /* Transaction version - uint32 */
    private final byte[] version;
    /* Witness marker and flag */
    private final byte[] witMarkerFlag;
    private final boolean hasWitnesses;
    /* tx_in count - compactSize uint 1-9 bytes */
    private final CompactSizeUInt txInCount;
    /* tx_in */
    private final TransactionInput[] txIn;
    /* tx_out count - compactSize uint 1-9 bytes */
    private final CompactSizeUInt txOutCount;
    /* tx_out */
    private final TransactionOutput[] txOut;
    /* witnesses raw data - >=0 bytes */
    private final byte[] witnesses;
    /* lock_time - uint32 */
    private final byte[] lockTime;


    /* Private constructor. Does not copy input. Does not validate input fully. */
    private Transaction(SHA256HASH blockhash, int height, boolean isCoinbase, byte[] version,
                        byte[] witMarkerFlag, TransactionInput[] txIn, TransactionOutput[] txOut,
                        byte[] witnesses, byte[] lockTime) throws BitcoinBlockException {
        this.blockhash = blockhash;
        this.height = height;
        this.isCoinbase = isCoinbase;
        this.version = version;
        if(witMarkerFlag != null) {
            this.hasWitnesses = true;
            this.witMarkerFlag = witMarkerFlag;
            this.witnesses = witnesses;
        }
        else {
            this.hasWitnesses = false;
            this.witMarkerFlag = null;
            this.witnesses = null;
        }
        this.lockTime = lockTime;
        this.txInCount = new CompactSizeUInt(txIn.length);
        this.txIn = txIn;
        this.txOutCount = new CompactSizeUInt(txOut.length);
        this.txOut = txOut;

        /* Calculate txid */
        try {
            this.txid = calcTxid();
        } catch (IOException e) {
            throw new BitcoinBlockException("Failed to calculate txid.", blockhash.toString(),
                    height, e);
        }

        /* Set txid to all transaction inputs and outputs */
        for(int i=0; i<txIn.length; i++) {
            txIn[i].setTxid(txid);
        }
        for(int i=0; i<txOut.length; i++) {
            txOut[i].setTxid(txid);
        }

        /* Serialized size */
        int tempSize = version.length + txInCount.getSerializedSize() +
                txOutCount.getSerializedSize() + lockTime.length;
        for(int i=0; i<txIn.length; i++) {
            tempSize += txIn[i].getSerializedSize();
        }
        for(int i=0; i<txOut.length; i++) {
            tempSize += txOut[i].getSerializedSize();
        }
        serializedSizeNoWitnesses = tempSize;
        if(this.hasWitnesses) {
            serializedSize = serializedSizeNoWitnesses + witMarkerFlag.length + witnesses.length;
        }
        else {
            serializedSize = serializedSizeNoWitnesses;
        }
    }


    /* Hashes transaction to get txid */
    public SHA256HASH calcTxid() throws IOException {
        /* Txid is calculated without witnesses */
        SHA256OutputStream sha256OutputStream = SHA256HASH.getOutputStream();
        sha256OutputStream.write(version);
        txInCount.serialize(sha256OutputStream);
        for(int i=0; i<txIn.length; i++) {
            txIn[i].serialize(sha256OutputStream);
        }
        txOutCount.serialize(sha256OutputStream);
        for(int i=0; i<txOut.length; i++) {
            txOut[i].serialize(sha256OutputStream);
        }
        sha256OutputStream.write(lockTime);

        /* Return double SHA256 */
        return sha256OutputStream.getDigest().getHashOfHash();
//        /* Txid is calculated without witnesses */
//        ByteBuffer txData = ByteBuffer.allocate(txSizeNoWitnesses);
//        txData.put(version);
//        txData.put(txInCount);
//        for(int i=0; i<txIn.length; i++) {
//            txIn[i].getRawTxInData(txData);
//        }
//        txData.put(txOutCount);
//        for(int i=0; i<txOut.length; i++) {
//            txOut[i].getRawTxOutData(txData);
//        }
//        txData.put(lockTime);
//        return Utils.SHA256.digest(Utils.SHA256.digest(txData.array()));
    }


    /* returns TransactionInput[] array from this transaction's inputs */
    public TransactionInput[] getTransactionInputs() { return Arrays.copyOf(txIn, txIn.length); }

    /* returns TransactionOutput[] array from this transaction's inputs */
    public TransactionOutput[] getTransactionOutputs() { return Arrays.copyOf(txOut, txOut.length); }


    /* returns iterator for this transaction's inputs and outpus */
    public Iterator<TransactionInput> getTxInIterator() { return Arrays.asList(txIn).iterator(); }

    public Iterator<TransactionOutput> getTxOutIterator() {
        return Arrays.asList(txOut).iterator();
    }


    /* Get methods */
    public SHA256HASH getBlockhash() {
        return blockhash;
    }

    public int getHeight() {
        return height;
    }

    public SHA256HASH getTxid() { return txid; }

    public long getVersion() {
        return BitcoinParams.readUINT32(version, 0);
    }

    public byte[] getVersionBytes() { return Arrays.copyOf(version, version.length); }

    public boolean hasWitnesses() { return hasWitnesses; }

    public byte[] getWitMarkerFlag() { return Arrays.copyOf(witMarkerFlag, witMarkerFlag.length); }

    private byte[] getWitnesses() { return Arrays.copyOf(witnesses, witnesses.length); }

    public int getTxInCount() {
        return (int)txInCount.getValue();
    }

    public int getTxOutCount() {
        return (int)txOutCount.getValue();
    }

    public long getLockTime() {
        return BitcoinParams.readUINT32(lockTime, 0);
    }

    public boolean isCoinbase() { return isCoinbase; }

    public int getSerializedSize() { return serializedSize; }

    public int getSerializedSizeNoWitnesses() { return serializedSizeNoWitnesses; }


    /* DEBUG */
    public void print(PrintStream printStream,
                      boolean doMetadata, boolean doHeader, boolean doTxData ) {
        if(doMetadata) {
            printStream.println("Blockhash:" + this.getBlockhash());
            printStream.println("Height:" + this.getHeight());
        }
        if(doHeader) {
            printStream.println("Txid:" + this.getTxid());
            printStream.println("isCoinbase: " + this.isCoinbase());
            printStream.println("Version: " + this.getVersion() + " -- LE: " +
                    Hex.encodeHexString(version));
            printStream.println("Has witnesses: " + this.hasWitnesses());
            printStream.println("TxInCount:" + this.getTxInCount() + " -- " + txInCount.toString());
            printStream.println("TxOutCount:" + this.getTxOutCount() + " -- " + txOutCount);
            printStream.println("Locktime: " + this.getLockTime() + " -- LE: " +
                    Hex.encodeHexString(lockTime));
            printStream.println("Tx size (with witnesses): " + this.getSerializedSize());
            printStream.println("Tx size (no witnesses): " + this.getSerializedSizeNoWitnesses());
        }
        if(doTxData) {
            printStream.println("-=Transaction Inputs=-");
            for(int i=0; i<txIn.length; i++) {
                txIn[i].print(printStream, false);
            }

            printStream.println("-=Transaction Outputs=-");
            for(int i=0; i<txOut.length; i++) {
                txOut[i].print(printStream, false);
            }
        }
    }


    /* Deserialize */
    public static Transaction deserialize(SHA256HASH blockhash, int height, byte[] data,
                                          int offset, boolean isCoinbase)
        throws BitcoinBlockException {
        int originalOffset = offset;
        boolean headerParsed = false;
        boolean txInParsed = false;
        boolean txOutParsed = false;
        boolean witnessesParsed = false;
        try {
            /* Deserialize header */
            byte[] version = Utils.readBytesFromByteArray(
                    data, offset, BitcoinParams.TRANSACTION_VERSION_SIZE);
            offset += BitcoinParams.TRANSACTION_VERSION_SIZE;

            /* look for witness marker and witness flag */
            byte[] witMarkerFlag = Utils.readBytesFromByteArray(
                    data, offset, BitcoinParams.TRANSACTION_WIT_MARKER_FLAG_SIZE);
            boolean hasWitness;
            if (Arrays.equals(witMarkerFlag, BitcoinParams.TRANSACTION_WIT_MARKER_FLAG)) {
                hasWitness = true;
                offset += BitcoinParams.TRANSACTION_WIT_MARKER_FLAG_SIZE;
            } else {
                hasWitness = false;
                witMarkerFlag = null;
            }
            headerParsed = true;

            /* Deserialize txIn */
            CompactSizeUInt txInCount = CompactSizeUInt.deserialize(data, offset);
            offset += txInCount.getSerializedSize();

            TransactionInput[] txIn = new TransactionInput[(int) txInCount.getValue()];
            for (int i = 0; i < txIn.length; i++) {
                txIn[i] = TransactionInput.deserialize(blockhash, height, data, offset, isCoinbase);
                offset += txIn[i].getSerializedSize();
            }
            txInParsed = true;

            /* Deserialize txOut */
            CompactSizeUInt txOutCount = CompactSizeUInt.deserialize(data, offset);
            offset += txOutCount.getSerializedSize();

            TransactionOutput[] txOut = new TransactionOutput[(int) txOutCount.getValue()];
            for (int i = 0; i < txOut.length; i++) {
                txOut[i] = TransactionOutput.deserialize(
                        blockhash, height, data, offset, isCoinbase, i);
                offset += txOut[i].getSerializedSize();
            }
            txOutParsed = true;

            /* Deserialize witnesses if any */
            byte[] witnesses = null;
            if (hasWitness) {
                /* count witnesses bytes */
                int offset2 = offset;

                /* there is one witness field per transaction input */
                for (int i = 0; i < txInCount.getValue(); i++) {
                    /* read number of stack items */
                    CompactSizeUInt stackItemsCount = CompactSizeUInt.deserialize(data, offset2);
                    offset2 += stackItemsCount.getSerializedSize();

                    /* read stack items */
                    for (int j = 0; j < stackItemsCount.getValue(); j++) {
                        /* read stack item byte length and skip it */
                        CompactSizeUInt stackItemBytes = CompactSizeUInt.deserialize(data, offset2);
                        offset2 += stackItemBytes.getSerializedSize();
                        offset2 += (int) stackItemBytes.getValue();
                    }
                }

                /* witnesses bytes */
                int witnessesBytes = offset2 - offset;

                /* create witnesses byte array and copy data */
                witnesses = Utils.readBytesFromByteArray(data, offset, witnessesBytes);
                offset += witnessesBytes;
            }
            witnessesParsed = true;

            /* Deserialize locktime */
            byte[] locktime = Utils.readBytesFromByteArray(
                    data, offset, BitcoinParams.TRANSACTION_LOCKTIME_SIZE);
            offset += BitcoinParams.TRANSACTION_LOCKTIME_SIZE;

            /* Make new transaction */
            Transaction transaction = new Transaction(blockhash, height, isCoinbase, version,
                    witMarkerFlag, txIn, txOut, witnesses, locktime);

            /* Match offset with serialized size */
            if( (offset-originalOffset) != transaction.getSerializedSize() ) {
                throw new BitcoinBlockException(
                        "Transaction serialized size doesn't match read bytes.",
                        blockhash.toString(), height);
            }

            return transaction;
        } catch (IndexOutOfBoundsException e) {
            if (!headerParsed) {
                throw new BitcoinBlockException(
                        "Parsing transaction header failed.", blockhash.toString(), height, e);
            } else if (!txInParsed) {
                throw new BitcoinBlockException(
                        "Parsing transaction txIn failed.", blockhash.toString(), height, e);
            } else if (!txOutParsed) {
                throw new BitcoinBlockException(
                        "Parsing transaction txOut failed.", blockhash.toString(), height, e);
            } else if (!witnessesParsed) {
                throw new BitcoinBlockException(
                        "Parsing transaction witnesses failed.", blockhash.toString(), height, e);
            } else {
                throw new BitcoinBlockException(
                        "Parsing transaction locktime failed.", blockhash.toString(), height, e);
            }
        }
    }
}
