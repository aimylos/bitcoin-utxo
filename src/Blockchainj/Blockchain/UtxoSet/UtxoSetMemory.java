package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Util.SHA256HASH;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Arrays;

public class UtxoSetMemory extends AbstractUtxoSet {
    /* Shards kept in memory */
    private Shard[] shards;


    /* Create new utxo set constructor. */
    public UtxoSetMemory(String utxoSetPath, int shardNum)
            throws IllegalArgumentException, IOException {
        super(utxoSetPath, shardNum);

        /* init shard array */
        shards = new Shard[getShardNum()];
        Arrays.fill(shards, null);

        /* Init utxo set */
        initNewUtxoSet();
    }


    /* Load existing utxo set */
    public UtxoSetMemory(String utxoSetPath, boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT)
            throws IOException {
        super(utxoSetPath);

        /* init shard array */
        shards = new Shard[getShardNum()];
        Arrays.fill(shards, null);

        /* init utxo set */
        loadAndInitUtxoSet(DO_MERKLE_TREE_CHECKSUM_ON_INIT);
    }


    @Override
    protected Shard createEmptyShard(int shardIndex, Path shardPathName) throws IOException {
        /* Get shard file */
        File shardFile = shardPathName.toFile();

        /* check if file already exists */
        if (shardFile.exists()) {
            throw new FileAlreadyExistsException(shardPathName.toString());
        }

        /* create new empty shard */
        Shard shard = getShardFactory().getNewShard(shardNum, shardIndex);

        /* Dont not store shard to DISK. Just hold it in memory. */
        putCachedShard(shard);

        /* return shard */
        return shard;
    }


    /* Get shard. Try memory else load from DISK. */
    @Override
    protected Shard getCachedShard(int shardIndex) throws IOException {
        if(shards[shardIndex] != null) {
            return shards[shardIndex];
        } else {
            return loadShard(shardIndex);
        }
    }


    /* Load shard from DISK and keep it in memory. */
    @Override
    protected Shard loadShard(int shardIndex, Path shardPathName) throws IOException {
        InputStream inputStream = null;
        try {
            /* get shard file */
            File shardFile = shardPathName.toFile();

            /* Read all file to byte array first. */
            byte[] fileBytes = org.apache.commons.io.FileUtils.readFileToByteArray(shardFile);

            /* Make inputstream from bytes array */
            inputStream = new ByteArrayInputStream(fileBytes);

            /* load shard */
            Shard shard = getShardFactory().load(inputStream);

            /* check shard */
            if(shard.getShardNum() != getShardNum()) {
                throw new IOException(new BitcoinUtxoSetException(
                        "Loaded shard's shardNum does not match current.",
                        getBestBlockhash().toString(), getBestHeight()) );
            }

            if(shard.getShardIndex() != shardIndex) {
                throw new IOException( new BitcoinUtxoSetException(
                        "Loaded shard's shardIndex does not match given.",
                        getBestBlockhash().toString(), getBestHeight()) );
            }

            /* Keep shard in memory. */
            shards[shardIndex] = shard;

            /* return shard */
            return shard;
        } finally {
            /* Close bytearray stream has not effect but close anyway */
            if(inputStream != null) {
                inputStream.close();
            }
        }
    }


    /* Put shard into memory. */
    @Override
    protected void putCachedShard(Shard shard) {
        int index = shard.getShardIndex();
        if(shards[index] != shard) {
            shards[index] = shard;
        }
    }


    /* Store shard to memory and DISK. */
    @Override
    protected void storeShard(Shard shard, Path shardPathName) throws IOException {
        /* Store shard to memory */
        shards[shard.getShardIndex()] = shard;

        /* Store shard to disk */
        org.apache.commons.io.output.ByteArrayOutputStream outputStream = null;
        try {
            /* get shard file */
            File shardFile = shardPathName.toFile();

            /* delete file if it exists */
            shardFile.delete();

            /* Make outputstream retrievable into a bytearray. Use apache's. */
            long serializedSize = shard.getStorageSerializedSize();
            if(serializedSize > Integer.MAX_VALUE) {
                throw new IOException("Shard storage serilialized size too big.");
            }
            outputStream = new org.apache.commons.io.output.ByteArrayOutputStream(
                    (int)serializedSize);

            /* store shard */
            shard.store(outputStream);

            /* Checks */
            if(shard.getStorageSerializedSize() != outputStream.size()) {
                throw new IOException("Calculated storage seriliazed size does not match " +
                        "actual store serialized size. Calculated:" +
                        shard.getStorageSerializedSize() +
                        " Actual:" + outputStream.size());
            }

            /* write byte array to file */
            org.apache.commons.io.FileUtils.writeByteArrayToFile(
                    shardFile, outputStream.toByteArray());
        } finally {
            /* Closing bytearray stream has no effect but close anyway. */
            if(outputStream != null) {
                /* close file output stream */
                outputStream.close();
            }
        }
    }


    @Override
    protected void commitPendingData() throws IOException {
         /* If HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT is true then there is no need
           to rebuild the merkle tree. Else rebuild merkle tree. */
        if(!HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT) {
            //TODO: concurrent hashing

            /* Hash shards and update merkle tree */
            for(int i=0; i<getShardNum(); i++) {
                /* Get shard */
                Shard shard = getCachedShard(i);

                /* Calculate hash. */
                SHA256HASH shardHash = shard.calcShardHash();

                /* Update merkle tree */
                merkleTree.updateLeafHash(i, shardHash);

                /* Store shard to DISK. */
                storeShard(shard);
            }

            /* Rehash merkle tree */
            merkleTree.rehashTree();

            /* Delete last entry from utxo set log */
            utxoSetLog.deleteLastEntry();

            /* Append new log entry */
            utxoSetLog.appendEntry(getBestBlockhash(), getShardNum(), merkleTree.getRoot(),
                    getBestHeight());
        }
        /* If rebuild of merkle tree wasn't needed, then just store shards to disk. */
        else {
            /* This class keeps pending data in memory. Write it to disk. */
            for(int i=0; i<getShardNum(); i++) {
                /* Get shard from memory. Takes no time. */
                Shard shard = getCachedShard(i);

                /* Store shard to DISK. */
                storeShard(shard);
            }
        }
    }


    @Override
    public void printParameters(PrintStream printStream) {
        super.printParameters(printStream);
        printStream.println("Utxo Set type: UtxoSetMemory");
    }
}