package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Bitcoin.TXI;
import Blockchainj.Bitcoin.Transaction;
import Blockchainj.Bitcoin.TransactionOutput;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.ProtocolUtils;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * UtxFast - Implements UTX
 *
 * In expense of more memory this implementation is more effiecient in computing time.
 *
 */

public class UtxFast implements UTX {
    public static final int UTX_TYPE = 1;

    /* Transaction TXID. */
    private final SHA256HASH txid;

    /* Transaction version. Kept internally in Bitcoin compact version. */
    private final byte[] version;

    /* Height. */
    private final int height;

    /* isCoinbase */
    private final boolean isCoinbase;

    /* Prototype Protocol serialized size. */
    private final int serializedSize;

    /* Storage serialized size. */
    private final int storageSerializedSize;

    /* Set of unspent UTXOs. */
    private final UTXO[] utxos;


    /* Constructor. Constructs new UTX from Transaction. */
    public UtxFast(Transaction transaction) {
        this(transaction.getTxid(),
                transaction.getVersionBytes(),
                transaction.getHeight(),
                transaction.isCoinbase(),
                transaction.getTxOutIterator(),
                null,
                transaction.getTxOutCount());
    }


    private UtxFast(SHA256HASH txid,
                   byte[] version,
                   int height,
                   boolean isCoinbase,
                   Iterator<TransactionOutput> utxoIterator,
                   UTXO[] utxoArray,
                   int utxoCount) {
        /* Get immutable txid */
        this.txid = txid;

        /* Get version and store it in compact way */
        this.version = BitcoinParams.getTransactionVersionIndexOrBytes(version);

        /* Set height */
        this.height = height;

        /* Set isCoinbase */
        this.isCoinbase = isCoinbase;

        /* Init utxos and utxo count */
        utxos = new UTXO[utxoCount];

        /* init serialized sizes */
        int tempSerializedSize = SHA256HASH.SERIALIZED_SIZE +
                BitcoinParams.TRANSACTION_VERSION_SIZE +
                ProtocolParams.HEIGHT_SIZE +
                ProtocolParams.BOOLEAN_SIZE +
                CompactSizeUInt.getSizeOf(utxos.length);
        int tempStorageSerializedSize = tempSerializedSize;

        /* Read transaction outputs */
        for(int i=0; i<utxos.length; i++) {
            UTXO utxo;
            if(utxoIterator != null) {
                /* Get next transaction output */
                TransactionOutput txOut = utxoIterator.next();

                /* Create new UTXO */
                utxo = new UTXO(txOut, txOut.getOutIndex());
            } else {
                utxo = utxoArray[i];
            }

            /* Compute serialized sizes */
            tempSerializedSize += utxo.getSerializedSize();
            tempStorageSerializedSize += utxo.getStorageSerializedSize();

            /* Add utxo to utxos */
            utxos[i] = utxo;
        }

        serializedSize = tempSerializedSize;
        storageSerializedSize = tempStorageSerializedSize;
    }



    /* Prototype Protocol and storage header serialization. */
    private void serializeOrStore(OutputStream outputStream, boolean serializeVsStore)
            throws IOException {
        /* Write txid */
        txid.serialize(outputStream);

        /* Write transaction version */
        outputStream.write(BitcoinParams.getTransactionVersion(version));

        /* Write transaction height */
        ProtocolUtils.writeHeight(height, outputStream);

        /* Write isCoinbase */
        ProtocolUtils.writeBoolean(isCoinbase, outputStream);

        /* Write utxo count */
        new CompactSizeUInt(getUtxosCount()).serialize(outputStream);

        /* write utxos */
        if(serializeVsStore) {
            for(int i=0; i<utxos.length; i++) {
                utxos[i].serialize(outputStream);
            }
        } else {
            for(int i=0; i<utxos.length; i++) {
                utxos[i].store(outputStream);
            }
        }
    }


    /* Prototype Protocol and storage deserialization */
    private static UtxFast deserializeOrLoad(InputStream inputStream, boolean deserializeVsLoad)
            throws IOException {
        /* read txid */
        SHA256HASH txid = SHA256HASH.deserialize(inputStream);

        /* read version */
        byte[] version = Utils.readBytesFromInputStream(
                inputStream, BitcoinParams.TRANSACTION_VERSION_SIZE);

        /* read height */
        int height = ProtocolUtils.readHeight(inputStream);

        /* read isCoinbase */
        boolean isCoinbase = ProtocolUtils.readBoolean(inputStream);

        /* read utxosCount */
        int utxosCount = (int)(CompactSizeUInt.deserialize(inputStream)).getValue();

        /* Check utxo count */
        if(utxosCount <= 0) {
            throw new IOException("Attempt to deserialize empty UTX");
        }

        /* Read utxos. */
        UTXO[] utxos = new UTXO[utxosCount];
        int prevOutIndex = -1;
        for (int i = 0; i < utxos.length; i++) {
            if(deserializeVsLoad) {
                utxos[i] = UTXO.deserialize(inputStream);
            } else {
                utxos[i] = UTXO.load(inputStream);
            }

            if(utxos[i].getOutIndex() <= prevOutIndex) {
                throw new IOException("Attempt to deserialize non sorted UTX");
            }
            prevOutIndex = utxos[i].getOutIndex();
        }

        return new UtxFast(txid, version, height, isCoinbase, null, utxos, utxos.length);
    }


    /** Prototype Protocol serialization. */
    @Override
    public void serialize(OutputStream outputStream) throws IOException {
        serializeOrStore(outputStream, true);
    }


    /** Prototype Protocol deserialization */
    public static UtxFast deserialize(InputStream inputStream) throws IOException {
        return deserializeOrLoad(inputStream, true);
    }


    /** Storage serialization */
    @Override
    public void store(OutputStream outputStream) throws IOException {
        serializeOrStore(outputStream, false);
    }


    /** Storage deserialization */
    public static UtxFast load(InputStream inputStream) throws IOException {
        return deserializeOrLoad(inputStream, false);
    }


    /* Get methods */
    @Override
    public int getUtxosCount() {
        return utxos.length;
    }


    @Override
    public boolean hasUtxo(int outIndex) {
        return (getUtxo(outIndex) != null);
    }


    @Override
    public UTXO getUtxo(int outIndex) {
        //TODO binary search
        for(int i=0; i<utxos.length; i++) {
            if(utxos[i].getOutIndex() == outIndex) {
                return utxos[i];
            }
        }
        return null;
    }


    @Override
    public SHA256HASH getTxid() { return txid; }

    @Override
    public long getVersion() {
        return BitcoinParams.readUINT32(BitcoinParams.getTransactionVersion(version), 0);
    }

    @Override
    public byte[] getVersionBytes() {
        byte[] ver = BitcoinParams.getTransactionVersion(version);
        return Arrays.copyOf(ver, ver.length);
    }

    @Override
    public int getHeight() { return height; }

    @Override
    public boolean isCoinbase() { return isCoinbase; }


    /* Serialized size for Prototype Protocol serialization. */
    @Override
    public long getSerializedSize() { return (long)serializedSize; }

    /* Serialized size for Storage serialization. */
    @Override
    public long getStorageSerializedSize() { return (long)storageSerializedSize; }


    @Override
    public Iterator<UTXO> getUtxoIterator() { return Arrays.asList(utxos).iterator(); }


    @Override
    public String toString() { return "UTX_Txid: " + getTxid().toString(); }


    /* compareTo method. Compare TXIDs. */
    @Override
    public int compareTo(UTX o) { return getTxid().compareTo(o.getTxid()); }


    /* DEBUG ONLY */
    @Override
    public void print(PrintStream printStream, boolean doUtxos) {
        printStream.println("Txid:" + this.getTxid());
        printStream.println("Version: " + this.getVersion() + " -- LE: " +
                Hex.encodeHexString(version));
        printStream.println("UtxosCount:" + this.getUtxosCount());
        printStream.println("Serialized size: " + this.getSerializedSize());

        if(doUtxos) {
            for(int i=0; i<utxos.length; i++) {
                if(utxos[i] != null) {
                    utxos[i].print(printStream);
                }
            }
        }
    }


    @Override
    public void printParameters(PrintStream printStream) {
        printStream.println("UTX type: " + this.getClass().toString());
    }


    @Override
    public UTX spentUTXO(TXI txi, int height)
            throws BitcoinUtxoSetException, IllegalArgumentException{
        /* Check if txids match */
        if(!txi.getPrevTxid().equals(txid)) {
            throw new IllegalArgumentException("TXIDs must match");
        }

        /* Get utxo */
        UTXO utxo = getUtxo(txi.getPrevOutIndex());

        /* If utxo does not exist throw exception */
        if(utxo == null) {
            throw new BitcoinUtxoSetException("UTXO not found.", txi);
        }

        /* If utxoCount was 1 return null cause of empty UTX */
        if(getUtxosCount() == 1) {
            return null;
        }

        /* create new utxo array without previous utxo */
        UTXO[] newUtxos = new UTXO[getUtxosCount() - 1];

        /* populate utxos */
        int newIndex = 0;
        for(int i=0; i<utxos.length; i++) {
            if(utxos[i].getOutIndex() != utxo.getOutIndex()) {
                newUtxos[newIndex] = utxos[i];
                newIndex++;
            }
        }

        return new UtxFast(
                txid, getVersionBytes(), height, isCoinbase, null, newUtxos, newUtxos.length);
    }

//        /** ONLY USE FOR UPDATING UTXO SET FORWARD **/
//    /* utxo remove - returns null if does not exist */
//        @Override
//        public UTXO removeUtxo(int outIndex) {
//        /* utxoCount size before remove */
//            int prevUtxoCountSize = CompactSizeUInt.getSizeOf(getUtxosCount());
//
//        /* get utxo */
//            UTXO utxo = getUtxo(outIndex);
//
//        /* If not found return null */
//            if(utxo == null) {
//                return null;
//            }
//        /* If found remove it */
//            else {
//                utxos[outIndex] = null;
//            }
//
//        /* Decrease non removed utxo count */
//            nonRemovedUtxosCount--;
//
//        /* update serialized size with new utxoCount */
//            serializedSize -= prevUtxoCountSize;
//            serializedSize += CompactSizeUInt.getSizeOf(getUtxosCount());
//
//        /* update serialized size without removed utxo */
//            serializedSize -= utxo.getSerializedSize();
//            storageSerializedSize -= utxo.getStorageSerializedSize();
//
//            return utxo;
//        }
//    }


    /* DEBUG ONLY */
//    public static boolean equals(UTX utx1, UTX utx2) throws IOException {
//        ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
//        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
//        utx1.serialize(byteArrayOutputStream1);
//        utx2.serialize(byteArrayOutputStream2);
//        byte[] utx1Serialized = byteArrayOutputStream1.toByteArray();
//        byte[] utx2Serialized = byteArrayOutputStream2.toByteArray();
//        return Arrays.equals(utx1Serialized, utx2Serialized);
//    }


    /* DEBUG/TEST */
//    public static void main(String[] args) {
//        int startAt = 0; //Integer.parseInt(args[0]);
//        int endAt = 10000; //Integer.parseInt(args[1]);
//        String filenamepath = "/tmp/UTXserialz.bin"; //args[2]; //
//
//        //test1();
//        //test2(filenamepath);
//        test3(startAt, endAt, filenamepath);
//    }
//
//    private static void test1() {
//        RPCconnection rpcCon = new RPCconnection();
//        int height = 485954;
//        try {
//            SimpleBlockBuffer blockBuffer = new SimpleBlockBuffer(height, height, rpcCon);
//            Block block = blockBuffer.getNextBlock();
//
//            block.getTxByIndex(54).getUTX().print(true);
//
//        } catch (BitcoinRpcException | BitcoinBlockException | EndOfRangeException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private static void test2(String filenamepath) {
//        RPCconnection rpcCon = new RPCconnection();
//        int height = 0;
//        try {
//            SimpleBlockBuffer blockBuffer = new SimpleBlockBuffer(height, height, rpcCon);
//            Block block = blockBuffer.getNextBlock();
//
//            OutputStream outputStream = new FileOutputStream(filenamepath);
//            UTX utxOut = block.getTxByIndex(0).getUTX();
//            utxOut.serialize(outputStream);
//            outputStream.close();
//
//            InputStream inputStream = new FileInputStream(filenamepath);
//            UTX utxIn = UTX.deserialize(inputStream);
//            inputStream.close();
//
//            System.out.println(UTX.equals(utxIn, utxOut));
////            utxOut.printUTX(true);
////            System.out.println("\n=======================================================\n");
////            utxIn.printUTX(true);
//        }
//        catch (BitcoinRpcException | BitcoinBlockException | EndOfRangeException | IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    private static void test3(int startAt, int endAt, String filenamepath) {
//        int printPeriod = 1000;
//        try{
//            SimpleBlockBuffer bb = new SimpleBlockBuffer(startAt, endAt, new RPCconnection());
//            OutputStream outputStream = new FileOutputStream(filenamepath);
//            List<UTX> utxList = new ArrayList<>();
//
//            for (; ; ) {
//                try {
//                    Block block = bb.getNextBlock();
//                    Iterator<Transaction> it = block.getTxIterator();
//                    while (it.hasNext()) {
//                        Transaction t = it.next();
//                        try {
//                            UTX utx = t.getUTX();
//                            utxList.add(utx);
//                            utx.serialize(outputStream);
//                        } catch (Exception e) {
//                            t.print(true, true, false);
//                            throw new RuntimeException(e);
//                        }
//                    }
//
//                    if(block.getHeight()%printPeriod == 0) {
//                        System.out.println("Reading from blocks at height: " + block.getHeight());
//                    }
//                } catch (EndOfRangeException e) {
//                    break;
//                }
//            }
//
//            outputStream.close();
//            InputStream inputStream = new FileInputStream(filenamepath);
//            Iterator<UTX> it = utxList.iterator();
//
//            int counter = 0;
//            printPeriod = printPeriod*10;
//            while(it.hasNext()) {
//                UTX utx1 = it.next();
//                UTX utx2 = UTX.deserialize(inputStream);
//                if(!UTX.equals(utx1, utx2)) {
//                    utx1.print(false);
//                    utx2.print(false);
//                    throw new RuntimeException();
//                }
//
//
//                if(counter%printPeriod == 0) {
//                    System.out.println("Reading from file");
//                }
//                counter++;
//            }
//
//            inputStream.close();
//
//        } catch (BitcoinRpcException | BitcoinBlockException | IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
