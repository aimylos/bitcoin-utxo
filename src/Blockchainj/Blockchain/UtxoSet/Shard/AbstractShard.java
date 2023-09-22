package Blockchainj.Blockchain.UtxoSet.Shard;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.UTXOS.MainUtxFactory;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxFactory;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxFastFactory;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.SHA256OutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;


/**
 * AbstractShard
 *
 * Implements Shard.
 */

public abstract class AbstractShard implements Shard {
    /* shard index and number of shards */
    protected final int shardIndex;
    protected final int shardNum;

    /* Prototype Protocol serialized size in bytes and storage seriliazed size.  */
    protected long serializedSize;
    protected long storageSerializedSize;

    /* UTXO count */
    protected int utxoCount;


    /* Creates an empty shard */
    public AbstractShard(int shardNum, int shardIndex) throws IllegalArgumentException {
        /* check number of shard */
        ProtocolParams.validateShardNum(shardNum);

        /* check shard index */
        if( (shardIndex < 0) || (shardIndex >= shardNum) ) {
            throw new IllegalArgumentException("Shard index must be >=0 and <" + shardNum);
        }

        /* Set shard num and shard index */
        this.shardNum = shardNum;
        this.shardIndex = shardIndex;

        /* init serialized size for empty shard. */
        serializedSize = Shard.getEmptyShardHeaderSerializedSize();
        storageSerializedSize = Shard.getEmptyShardHeaderSerializedSize();

        /* Init utxoCount to 0 */
        utxoCount = 0;
    }


    @Override
    public boolean inRange(SHA256HASH txid) {
        return (shardIndex == ProtocolParams.calcShardIndex(shardNum, txid));
    }


    @Override
    public boolean hasUTXO(SHA256HASH txid, int outIndex) {
        return getUTXO(txid, outIndex) != null;
    }


    /** Prototype Protocol serialization. */
    @Override
    public void serialize(OutputStream outputStream) throws IOException {
        /* write shard header */
        BitcoinParams.INT32ToOutputStream(shardNum, outputStream);
        BitcoinParams.INT32ToOutputStream(shardIndex, outputStream);

        /* write utxCount as CompactSizeUInt */
        new CompactSizeUInt(getUtxCount()).serialize(outputStream);

        /* Write UTXs using serialize method. */
        Iterator<UTX> utxIterator = getUtxIterator();
        while(utxIterator.hasNext()) {
            UTX utx = utxIterator.next();
            utx.serialize(outputStream);
        }
    }


    /** Storage serialization */
    @Override
    public void store(OutputStream outputStream) throws IOException {
        /* write shard header */
        BitcoinParams.INT32ToOutputStream(shardNum, outputStream);
        BitcoinParams.INT32ToOutputStream(shardIndex, outputStream);

        /* write utxCount as CompactSizeUInt */
        new CompactSizeUInt(getUtxCount()).serialize(outputStream);

        /* Write UTXs using store method. */
        Iterator<UTX> utxIterator = getUtxIterator();
        while(utxIterator.hasNext()) {
            UTX utx = utxIterator.next();
            utx.store(outputStream);
        }
    }


    @Override
    public SHA256HASH calcShardHash() throws IOException {
        /* get serialized size */
        long serializedSize = getSerializedSize();

//        /* Make outputstream retrievable into a bytearray. Use apache's. */
//        org.apache.commons.io.output.ByteArrayOutputStream outputStream =
//                new org.apache.commons.io.output.ByteArrayOutputStream(serializedSize);

        /* Make SHA256OutputStream */
        SHA256OutputStream outputStream = new SHA256OutputStream();

        /* Serialize shard. No shard metadata here. */
        serialize(outputStream);

        /* assert serialized size */
        if(serializedSize != outputStream.size()) {
            throw new RuntimeException("Serialized sized calculated (" + serializedSize +
                    ") does not match serialized sized generated (" + outputStream.size()
                    + ")." + " utxCount: " + getUtxCount());
        }

        /* return doubleSHA256 hash */
        return outputStream.getDigest().getHashOfHash();
    }


    @Override
    public int getShardNum() { return shardNum; }

    @Override
    public int getShardIndex() { return shardIndex; }

    @Override
    public long getSerializedSize() { return serializedSize; }

    @Override
    public long getStorageSerializedSize() { return storageSerializedSize; }

    @Override
    public long getHeaderSerializedSize() {
        long emptyHeaderSize = Shard.getEmptyShardHeaderSerializedSize();
        emptyHeaderSize -= CompactSizeUInt.getSizeOf(0);
        emptyHeaderSize += CompactSizeUInt.getSizeOf(getUtxCount());
        return emptyHeaderSize;
    }

    @Override
    public long getUtxSerializedSize() { return getSerializedSize() - getHeaderSerializedSize(); }

    @Override
    public int getUtxoCount() { return utxoCount; }

    @Override
    public boolean isEmpty() { return (getUtxCount() == 0); }


    @Override
    public UtxFactory getUtxFactory() { return getUtxFactoryStatic(); }

    public static UtxFactory getUtxFactoryStatic() { return MainUtxFactory.utxFactory; }


    @Override
    public void print(PrintStream printStream, boolean doUTXs, boolean doUTXOs) {
        printStream.println("shardNum: " + this.getShardNum());
        printStream.println("shardIndex: " + this.getShardIndex());
        try {
            printStream.println("Shard hash: " + this.calcShardHash().toString());
        } catch (IOException e) {
            printStream.println("Shard hash: " + e.toString());
        }
        printStream.println("UTX count: " + this.getUtxCount());
        printStream.println("TXO count: " + this.getUtxoCount());
        printStream.println("Serialized size: " + this.getSerializedSize());
        printStream.println("Storage serialized size: " + this.getStorageSerializedSize());

        if(doUTXs) {
            Iterator<UTX> utxIterator = getUtxIterator();
            while(utxIterator.hasNext()) {
                utxIterator.next().print(printStream, doUTXOs);
            }
        }
    }


    @Override
    public void printParameters(PrintStream printStream) {
        printStream.println(">" + this.getClass().toString());
        getUtxFactory().printUtxType(printStream);
    }


    /* DEBUG ONLY */
//    public static boolean equals(Shard shard1, Shard shard2) throws IOException {
//        if( (shard1.getShardNum() != shard2.getShardNum()) ||
//                (shard1.getShardIndex() != shard2.getShardIndex()) ||
//                (shard1.getUtxCount() != shard2.getUtxCount()) ||
//                (shard1.getUtxoCount() != shard2.getUtxoCount()) ||
//                (shard1.getSerializedSize() != shard2.getSerializedSize()) ) {
//            return false;
//        }
//
//        Iterator<UTX> it1 = shard1.getUtxIterator();
//        Iterator<UTX> it2 = shard2.getUtxIterator();
//        while (it1.hasNext() && it2.hasNext()) {
//            if ( !UTX.equals(it1.next(), it2.next()) ){
//                return false;
//            }
//        }
//        if(it1.hasNext() || it2.hasNext()) {
//            return false;
//        }
//
//        SHA256HASH hash1 = shard1.calcShardHash();
//        SHA256HASH hash2 = shard2.calcShardHash();
//
//        if( (!hash1.equals(hash2)) ){
//            return false;
//        }
//
//        /* double check cuz of changes */
//        //noinspection RedundantIfStatement
//        if( (hash1.compareTo(hash2) != 0) ) {
//            return false;
//        }
//
//        return true;
//    }


    /* TEST/DEBUG */
//    public static void main(String[] args) {
//        int startAt = 0; //Integer.parseInt(args[0]);
//        int endAt = 1000; //Integer.parseInt(args[1]);
//        String filenamepath = "/tmp/ShardSerialz.bin"; //args[2];
//        int shardNum = 16;
//        int shardIndex = 1;
//
//        test1(startAt, endAt, filenamepath, shardNum);
//        //test2(startAt, endAt, shardNum, shardIndex);
//    }


//    @SuppressWarnings("ConstantConditions")
//    public static void test1(int startAt, int endAt, String filenamepath, int shardNum) {
//        int printPeriod = 1000;
//        boolean printShards = false;
//        Shard[] shards;
//        if (shardNum >= 8) {
//            shards = new Shard[7];
//            shards[0] = new Shard(shardNum, 0);
//            shards[1] = new Shard(shardNum, shardNum/4-1);
//            shards[2] = new Shard(shardNum, shardNum/2-1);
//            shards[3] = new Shard(shardNum, shardNum/2);
//            shards[4] = new Shard(shardNum, shardNum/2+1);
//            shards[5] = new Shard(shardNum, 3*shardNum/4+1);
//            shards[6] = new Shard(shardNum, shardNum-1);
//        }
//        else if (shardNum == 4) {
//            shards = new Shard[4];
//            shards[0] = new Shard(shardNum, 0);
//            shards[1] = new Shard(shardNum, 1);
//            shards[2] = new Shard(shardNum, 2);
//            shards[3] = new Shard(shardNum, 3);
//        } else if (shardNum == 1) {
//            shards = new Shard[0];
//            shards[0] = new Shard(shardNum, 0);
//        } else if (shardNum == 2) {
//            shards = new Shard[2];
//            shards[0] = new Shard(shardNum, 0);
//            shards[1] = new Shard(shardNum, 1);
//        } else {
//            throw new RuntimeException();
//        }
//
//        /* load */
//        try{
//            SimpleBlockBuffer bb = new SimpleBlockBuffer(startAt, endAt, new RPCconnection());
//            for (; ; ) {
//                try {
//                    Block block = bb.getNextBlock();
//                    Iterator<Transaction> it = block.getTxIterator();
//                    while (it.hasNext()) {
//                        Transaction t = it.next();
//                        try {
//                            UTX utx = t.getUTX();
//
//                            for(int j=0; j<shards.length; j++) {
//                                if( shards[j].inRange(utx.getTxid()) ){
//                                    shards[j].addUTX(utx);
//                                    break;
//                                }
//                            }
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
//            /* serialize */
//            OutputStream outputStream = new FileOutputStream(filenamepath);
//            for(int j=0; j<shards.length; j++) {
//                shards[j].serialize(outputStream);
//            }
//            outputStream.close();
//
//            /* deserialize */
//            InputStream inputStream = new FileInputStream(filenamepath);
//            Shard[] shards2 = new Shard[shards.length];
//            for(int j=0; j<shards.length; j++) {
//                shards2[j] = Shard.deserialize(inputStream);
//            }
//            inputStream.close();
//
//            /* print shards */
//            if(printShards) {
//                for (int j = 0; j < shards.length; j++) {
//                    System.out.println("\n======================================================");
//                    shards[j].print(false, false);
//                    System.out.println("");
//                    shards2[j].print(false, false);
//                }
//            }
//
//            /* compare */
//            for(int j=0; j<shards.length; j++) {
//                if( !Shard.equals(shards[j], shards2[j]) ) {
//                    throw new RuntimeException("Shards not equal: " + shards[j].getShardIndex());
//                }
//            }
//
//        } catch (BitcoinRpcException | BitcoinBlockException | IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//
//    @SuppressWarnings("ConstantConditions")
//    public static void test2(int startAt, int endAt, int shardNum, int shardIndex) {
//        int printPeriod = 1000;
//        try {
//            /* load */
//            SimpleBlockBuffer bb = new SimpleBlockBuffer(startAt, endAt, new RPCconnection());
//            Shard shard = new Shard(shardNum, shardIndex);
//            for (; ; ) {
//                try {
//                    Block block = bb.getNextBlock();
//                    Iterator<Transaction> it = block.getTxIterator();
//                    while (it.hasNext()) {
//                        Transaction t = it.next();
//                        try {
//                            UTX utx = t.getUTX();
//                            if( shard.inRange(utx.getTxid()) ){
//                                shard.addUTX(utx);
//                            }
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
//            /* get serialized */
//            int serializedSize = shard.getSerializedSize();
//            org.apache.commons.io.output.ByteArrayOutputStream outputStream =
//                    new org.apache.commons.io.output.ByteArrayOutputStream(serializedSize);
//            shard.serializeData(outputStream);
//
//            /* print shard and serialized */
//            //shard.printShard(true, true);
//            System.out.println("\n");
//            System.out.println( Hex.encodeHexString(outputStream.toByteArray()) );
//
//            /* print hash */
//            System.out.println("\n");
//            System.out.println("Hash from serilized: " +
//                    SHA256HASH.doDoubleSHA256(outputStream.toByteArray()).toString());
//            System.out.println("Hash from shard: " +
//                    shard.calcShardHash().getHashString());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }
}
