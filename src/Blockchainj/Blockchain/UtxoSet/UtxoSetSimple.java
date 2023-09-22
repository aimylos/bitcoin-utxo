package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Bitcoin.*;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.ProtocolUtils;
import Blockchainj.Blockchain.UtxoSet.Shard.MainShardFactory;
import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardFactory;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardIterator;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTXO;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.MerkleTree;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * UtxoSetSimple
 *
 * Utxo Set that does not uses shards internally.
 *
 *
 * Serialization, sorted utxs:
 * <height, int32><blockhash, SHA256HASH><utxCount, compactSizeInt><utx[], utx serialization>
 *
 * Storage, sorted utxs:
 * <height, int32><blockhash, SHA256HASH><utxCount, compactSizeInt><utx[], utx storage>
 *
 */

public class UtxoSetSimple implements UtxoSet {
    @Override
    public ShardFactory getShardFactory() { return MainShardFactory.shardFactory; }

    /* Blockhash and height */
    private SHA256HASH bestBlockhash;
    private int bestHeight;


    /* UTXs */
    TreeMap<SHA256HASH, UTX> utxs;

    /* Utxo count */
    private int utxoCount;

    /* Close marker */
    private volatile boolean closed = false;

    /* Pathname to utxo set */
    private final Path filename;

    /* garbage collector call */
    private final int GARBAGE_COLLECTOR_CALL_PERIOD = 5000;


    /* Main constructor. Opens utxo set or creates new if filenotfound. */
    public UtxoSetSimple(String filename) throws IOException {
        /* Init filename */
        this.filename = Paths.get(filename);

        /* init fields */
        utxoCount = 0;
        utxs = new TreeMap<>();
        bestBlockhash = SHA256HASH.ZERO_SHA256HASH;
        bestHeight = UNDEFINED_HEIGHT;

        /* If file exists load it */
        if(this.filename.toFile().exists()) {
            /* Open file input stream */
            FileInputStream inputStream = new FileInputStream(this.filename.toFile());

            /* read height */
            bestHeight = ProtocolUtils.readHeight(inputStream);

            /* read blockhash */
            bestBlockhash = SHA256HASH.deserialize(inputStream);

            /* Read utx count */
            int utxCount = (int) CompactSizeUInt.deserialize(inputStream).getValue();

            /* Read utxs */
            for(int i=0; i<utxCount; i++) {
                /* load utx */
                UTX utx = getShardFactory().getUtxFactory().load(inputStream);

                /* add utx to map */
                utxs.put(utx.getTxid(), utx);

                /* update utxo count */
                utxoCount += utx.getUtxosCount();
            }
        }
    }


    @Override
    public synchronized void commitBlock(Block block) throws BitcoinUtxoSetException, IOException {
        if(isClosed()) {
            throw new IllegalStateException("Utxo Set closed.");
        }

        /* Call garbage collector */
        if(block.getHeight()%GARBAGE_COLLECTOR_CALL_PERIOD == 0) {
            Utils.suggestGarbageCollectorRun();
        }

        /* Process each transaction */
        Iterator<Transaction> txIterator = block.getTxIterator();
        while(txIterator.hasNext()) {
            Transaction t = txIterator.next();

            /* Check for non-unique txids */
            if(BitcoinUtils.isDuplicateTxid(t)) {
                continue;
            }

            //TODO REMOVE!
            if(t.isCoinbase() && t.getTxInCount() > 1) {
                t.print(System.out, true, true, true);
                throw new RuntimeException();
            }

            /* Process transaction inputs */
            Iterator<TransactionInput> txInIterator = t.getTxInIterator();
            while(txInIterator.hasNext()) {
                TransactionInput tIn = txInIterator.next();

                /* If transaction input is coinbase skip it */
                if(tIn.isCoinbase()) {
                    //noinspection UnnecessaryContinue
                    continue;
                }

//                Iterator<Transaction> t2 = block.getTxIterator();
//                while(t2.hasNext()) {
//                    Iterator<TransactionOutput> t3 = t2.next().getTxOutIterator();
//                    while(t3.hasNext()) {
//                        SHA256HASH txid = t3.next().getTxid();
//                        if(tIn.getPrevTxid().equals(txid)) {
//                            System.out.println(block.getHeight());
//                        }
//                    }
//                }


                /* Else check if transaction input can be spent. */
                /* Get utx */
                UTX utx = utxs.get(tIn.getPrevTxid());

                /* Check if utx found */
                if(utx == null) {
                    throw new BitcoinUtxoSetChangesException(
                            "UTX not found.", tIn.getBlockhash().toString(), tIn.getHeight(), tIn);
                }

                /* Try to spent tIn */
                UTX newUtx = utx.spentUTXO(tIn, block.getHeight());

                /* Decrease utxo count by 1 */
                utxoCount--;

                /* remove if utx is empty else replace with new */
                if(newUtx == null) {
                    utxs.remove(utx.getTxid());
                } else {
                    utxs.put(newUtx.getTxid(), newUtx);
                }
            }

            /* Process transaction outputs. Get new UTX instance */
            UTX utx = getShardFactory().getUtxFactory().getNewUTX(t);

            /*  UTX must not already exist in the changes. */
            if( utxs.put(utx.getTxid(), utx) != null ) {
                throw new BitcoinUtxoSetChangesException("Failed to put utx to chagnes.",
                        block.getBlockhash().toString(), block.getHeight(), utx);
            }


            Iterator<TransactionOutput> it = t.getTxOutIterator();
            while(it.hasNext()) {
                if(it.next().getScript()[2] == (byte)(0x6a & 0XFF)) {
                    System.out.println(block.getHeight());
                }
            }


            /* Add utxo count */
            utxoCount += utx.getUtxosCount();
        }

        /* update best blockhash and height */
        bestBlockhash = block.getBlockhash();
        bestHeight = block.getHeight();
    }



    @Override
    public synchronized void close() throws IOException {
        if(isClosed()) {
            return;
        }

        FileOutputStream outputStream = null;

        try {
            /* Create file */
            File file = this.filename.toFile();

            /* Delete previous utxo set */
            file.delete();

            /* Open file output stream */
            outputStream = new FileOutputStream(file);

            /* write height */
            ProtocolUtils.writeHeight(bestHeight, outputStream);

            /* write blockhash */
            bestBlockhash.serialize(outputStream);

            /* write utx count */
            (new CompactSizeUInt(utxs.size())).serialize(outputStream);

            /* write utxs */
            Iterator<UTX> utxIterator = utxs.values().iterator();
            while (utxIterator.hasNext()) {
                utxIterator.next().store(outputStream);
            }
        } finally {
            if(outputStream != null) {
                outputStream.close();
            }
            closed = true;
        }

    }


    @Override
    public synchronized boolean isClosed() {
        return closed;
    }



    @Override
    public synchronized int getBestHeight() { return bestHeight; }


    @Override
    public synchronized SHA256HASH getBestBlockhash() { return bestBlockhash; }


    @Override
    public synchronized SHA256HASH getBlockhash(int height) throws NoSuchElementException {
        if(height == bestHeight) {
            return bestBlockhash;
        }
        throw new NoSuchElementException();
    }


    @Override
    public synchronized int getUtxCount() { return utxs.size(); }


    @Override
    public synchronized int getUtxoCount() { return utxoCount; }


    @Override
    public synchronized long getUtxSerializedSize() {
        long serializedSize = 0;
        Iterator<UTX> utxIterator = utxs.values().iterator();
        while(utxIterator.hasNext()) {
            serializedSize += utxIterator.next().getSerializedSize();
        }

        return serializedSize;
    }


    @Override
    public synchronized Iterator<UTX> getUtxIterator() { return utxs.values().iterator(); }


    @Override
    public synchronized long getUtxoSetSerializedSizeEstimate(int shardNum)
            throws IllegalArgumentException {
        ProtocolParams.validateShardNum(shardNum);

        return getUtxSerializedSize() +
                ( (long)shardNum * Shard.getEmptyShardHeaderSerializedSize() );
    }


    @Override
    public synchronized Shard getShard(int shardNum, int shardIndex)
            throws IOException, IllegalArgumentException {
        return null;
    }


    @Override
    public synchronized Iterator<Shard> getShardIterator(int shardNum)
            throws IllegalArgumentException {
        return new ShardIterator(getUtxIterator(), shardNum);
    }


    @Override
    public synchronized MerkleTree getMerkleTree(int shardNum)
            throws IOException, IllegalArgumentException {
        ProtocolParams.validateShardNum(shardNum);

        /* Build new merkle tree */
        MerkleTree merkleTree = new MerkleTree(shardNum);

        /* Get shards and calculate hashses */
        Iterator<Shard> shardIterator = getShardIterator(shardNum);
        /* Iterator should return ALL shards */
        for(int i=0; i<shardNum; i++) {
            try {
                /* Get next shard */
                Shard shard = shardIterator.next();

                /* Hash shard */
                SHA256HASH shardHash = shard.calcShardHash();

                /* Put shard to tree */
                merkleTree.updateLeafHash(shard.getShardIndex(), shardHash);
            } catch (NoSuchElementException e) {
                throw new IOException(e);
            }
        }

        /* Build merkle tree */
        merkleTree.rehashTree();

        /* Return read only shallow copy of the new merkle tree */
        return merkleTree.getReadOnly();
    }


    @Override
    public synchronized void print(PrintStream printStream) {
        printStream.println("Utxo Set");
        printStream.println("Best height: " + getBestHeight());
        printStream.println("Best blockhash: " + getBestBlockhash());
        printStream.println("Utxo Set closed: " + closed);
        printStream.println("Utx count: " + getUtxCount());
        printStream.println("Utxo count: " + getUtxoCount());
    }


    @Override
    public synchronized void printParameters(PrintStream printStream) {
        printStream.println(">Utxo Set parameters:");
        printStream.println("UtxoSet type: " + this.getClass().toString());
        getShardFactory().printShardType(printStream);
        getShardFactory().getUtxFactory().printUtxType(printStream);
    }



}
