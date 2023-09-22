package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Util.SHA256HASH;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

public class UtxoSetIO extends AbstractUtxoSet {

    /* Create new utxo set constructor. */
    public UtxoSetIO(String utxoSetPath, int shardNum)
            throws IllegalArgumentException, IOException {
        super(utxoSetPath, shardNum);

        /* Init utxo set */
        initNewUtxoSet();
    }


    /* Load existing utxo set */
    public UtxoSetIO(String utxoSetPath, boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT)
            throws IOException {
        super(utxoSetPath);

        /* init utxo set */
        loadAndInitUtxoSet(DO_MERKLE_TREE_CHECKSUM_ON_INIT);
    }


    /* Special constructor that does not load utxo set upon construction.
       This constructor may lead to undefined behaviour. */
    public UtxoSetIO(String utxoSetPath, String sequence)
            throws IllegalArgumentException, IOException {
        super(utxoSetPath);

        if(!sequence.equals("19d6689c085ae165")) {
            throw new IllegalArgumentException("Incorrect sequence!");
        }
    }


    @Override
    protected Shard createEmptyShard(int shardIndex, Path shardPathName) throws IOException {
        /* get shard file */
        File shardFile = shardPathName.toFile();

        /* check if file already exists */
        if (shardFile.exists()) {
            throw new FileAlreadyExistsException(shardPathName.toString());
        }

        /* create new empty shard */
        Shard shard = getShardFactory().getNewShard(shardNum, shardIndex);

        /* Store shard to DISK. */
        storeShard(shard, shardPathName);

        /* return shard */
        return shard;
    }


    /* Get shard given a shard index. Since this is IO, getShard is equivalent to loadShard. */
    @Override
    protected Shard getCachedShard(int shardIndex) throws IOException {
        return loadShard(shardIndex);
    }


    /* Load shard from DISK. */
    @Override
    protected Shard loadShard(int shardIndex, Path shardPathName) throws IOException {
        InputStream inputStream = null;
        try {
            /* get shard file */
            File shardFile = shardPathName.toFile();

            /* create input stream, throws filenotfoundexception */
            //inputStream = new FileInputStream(shardFile);

            /* Read all file to byte array first. */
            byte[] fileBytes = org.apache.commons.io.FileUtils.readFileToByteArray(shardFile);
            //byte[] fileBytes = java.nio.file.Files.readAllBytes(shardPathName);

            /* Make inputstream from bytes array */
            inputStream = new ByteArrayInputStream(fileBytes);

            /* load shard from storage file */
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

            /* return shard */
            return shard;
        } finally {
            /* Close bytearray stream has not effect but close anyway */
            if(inputStream != null) {
                inputStream.close();
            }
        }
    }


    /* Put shard. Since this is IO, getShard is equivalent to storeShard. */
    @Override
    protected void putCachedShard(Shard shard) throws IOException {
        storeShard(shard);
    }


    /* Store shard to DISK. */
    @Override
    protected void storeShard(Shard shard, Path shardPathName) throws IOException {
        org.apache.commons.io.output.ByteArrayOutputStream outputStream = null;

        try {
            /* get shard file */
            File shardFile = shardPathName.toFile();

            /* delete file if it exists */
            shardFile.delete();

            /* create new empty file */
            //shardFile.createNewFile();

            /* create output stream */
            //outputStream = new FileOutputStream(shardFile);

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
    public void printParameters(PrintStream printStream) {
        super.printParameters(printStream);
        printStream.println("Utxo Set type: UtxoSetIO");
    }


    @Override
    protected void commitPendingData() throws IOException {
        /* If HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT is true then there is no need
           to rebuild the merkle tree.
           This class does not keep any pending data in memory. */
        if(HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT) {
            return;
        }

        /* Hash shards and update merkle tree */
        for(int i=0; i<getShardNum(); i++) {
            /* Get shard */
            Shard shard = getCachedShard(i);

            /* Calculate hash. */
            SHA256HASH shardHash = shard.calcShardHash();

            /* Update merkle tree */
            merkleTree.updateLeafHash(i, shardHash);
        }

        /* Rehash merkle tree */
        merkleTree.rehashTree();

        /* Delete last entry from utxo set log */
        utxoSetLog.deleteLastEntry();

        /* Append new log entry */
        utxoSetLog.appendEntry(getBestBlockhash(), getShardNum(), merkleTree.getRoot(),
                getBestHeight());
    }
}