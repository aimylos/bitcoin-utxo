package Blockchainj.Blockchain.UtxoSet;

import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Utxo Set Log File
 *
 * This file keeps a log (a history) of the blockhash, number of shards and utxo merkle tree root
 * for every height.
 * Starting always at height 0.
 *
 * Serialization format:
 * Position  0: <blockhash, 32bytes><number of shards, 4bytes><utxo merkle tree root, 32bytes>
 * Position 68: <blockhash, 32bytes><number of shards, 4bytes><utxo merkle tree root, 32bytes>
 *     ...
 *
 * Height 0 entry starts at position 0.
 * Height 1 entry starts at position 68.
 * ...
 *
 * Use:
 * - Get entry with best height.
 * - Get entry for any available height.
 * - Append entry, increasing best height by 1.
 * - Delete last entry, decreasing best height by 1.
 */

public class UtxoSetLog {
    /* Utxo set log file */
    private final File utxoSetLogFile;


    /* Utxo set log entry. */
    public class UtxoSetLogEntry {
        final static int NUMSHARD_SIZE = 4;
        final static int LOGENTRY_SIZE =
                SHA256HASH.HASH_SIZE + NUMSHARD_SIZE + SHA256HASH.HASH_SIZE;
        final SHA256HASH blockhash;
        final int numShard;
        final byte[] numShardBytes;
        final SHA256HASH merkleRoot;
        final int height;

        /* constructor */
        UtxoSetLogEntry(SHA256HASH blockhash, int numShard, SHA256HASH merkleRoot, int height) {
            this.blockhash = blockhash;
            this.numShard = numShard;
            this.numShardBytes = new byte[NUMSHARD_SIZE];
            Utils.int32ToByteArrayLE(numShard, numShardBytes, 0);
            this.merkleRoot = merkleRoot;
            this.height = height;
        }

        /* Deserialize constructor. Inner class cannot have static methods. */
        UtxoSetLogEntry(InputStream inputStream, int height) throws IOException {
            /* read blockhash */
            blockhash = SHA256HASH.deserialize(inputStream);

            /* read numShard */
            numShardBytes = new byte[NUMSHARD_SIZE];
            if( inputStream.read(numShardBytes) != NUMSHARD_SIZE) {
                throw new IOException("Expected " + NUMSHARD_SIZE + " for numShard.");
            }
            numShard = Utils.readInt32LE(numShardBytes, 0);

            /* read merkle tree root */
            merkleRoot = SHA256HASH.deserialize(inputStream);

            this.height = height;
        }

        void serialize(OutputStream outputStream) throws IOException {
            blockhash.serialize(outputStream);
            outputStream.write(numShardBytes);
            merkleRoot.serialize(outputStream);
        }

        void print() {
            System.out.println("Blockhash: " + blockhash.getHashString());
            System.out.println("Height: " + height);
            System.out.println("numShard: " + numShard);
            System.out.println("Merkle root: " + merkleRoot.getHashString());
        }
    }


    /* Constructor */
    public UtxoSetLog(Path utxoSetLogFilePathname) throws IOException {
        utxoSetLogFile = utxoSetLogFilePathname.toFile();

        /* if file does not exist, init new log file */
        if (!utxoSetLogFile.exists()) {
            utxoSetLogFile.getParentFile().mkdirs();
            utxoSetLogFile.createNewFile();
        }

        /* Verify log file. TODO: add checksum later?? */
        long fileSize = utxoSetLogFile.length();
        if (!isCorrectFileSize(fileSize)) {
            throw new IOException(utxoSetLogFilePathname.toString() + " is badly formatted.");
        }
    }


    /* check correct file size */
    private boolean isCorrectFileSize(long fileSize) {
        return (fileSize % UtxoSetLogEntry.LOGENTRY_SIZE) == 0;
    }


    /* returns log file size */
    public long getFileSize() { return utxoSetLogFile.length(); }


    /* Append entry. Entry height must match log file entry height */
    public void appendEntry(UtxoSetLogEntry entry) throws IOException, IllegalArgumentException{
        /* Get and check file size. */
        long fileSize = getFileSize();
        if(!isCorrectFileSize(fileSize)) {
            throw new IOException(utxoSetLogFile.toString() + "is badly formatted.");
        }

        /* Calc entry height */
        int newEntryHeight  = (int) (fileSize / UtxoSetLogEntry.LOGENTRY_SIZE);

        /* Heights must match */
        if(newEntryHeight != entry.height) {
            throw new IllegalArgumentException("Entry height must match current log file " +
                    "entry height.");
        }

        /* append new entry to end of log file */
        OutputStream outputStream = new FileOutputStream(utxoSetLogFile, true);
        entry.serialize(outputStream);
        outputStream.close();
    }


    /* same as appendEntry */
    public void appendEntry(SHA256HASH blockhash, int numShard, SHA256HASH merkleRoot, int height)
        throws IOException, IllegalArgumentException {
        appendEntry(new UtxoSetLogEntry(blockhash, numShard, merkleRoot, height));
    }


    /* Read last entry. If no entries return null. */
    public UtxoSetLogEntry getLastEntry() throws IOException {
        /* Get filesize, calc last height and get it. */
        long fileSize = getFileSize();
        int lastHeight = (int) (fileSize / UtxoSetLogEntry.LOGENTRY_SIZE) - 1;

        if(fileSize == 0) {
            return null;
        } else {
            return getEntry(lastHeight);
        }
    }


    /* Read entry at height */
    public UtxoSetLogEntry getEntry(int height) throws IOException {
        /* Get and check filesize. */
        long fileSize = getFileSize();
        if (!isCorrectFileSize(fileSize)) {
            throw new IOException(utxoSetLogFile.toString() + "is badly formatted.");
        }

        /* Calc last entry height. Can be -1 which is <0 which is no entries, */
        int bestHeight = (int) (fileSize / UtxoSetLogEntry.LOGENTRY_SIZE) - 1;

        /* check range */
        if( (height < 0) || (height > bestHeight) ) {
            throw new IOException("Height not found");
        }

        /* calc skip bytes */
        long skipBytes = (long)height * (long)UtxoSetLogEntry.LOGENTRY_SIZE;

        /* skip bytes */
        InputStream inputStream = new FileInputStream(utxoSetLogFile);
        if ( inputStream.skip(skipBytes) != skipBytes ) {
            throw new IOException("Unable to skip bytes: " + skipBytes);
        }

        /* read last entry */
        return new UtxoSetLogEntry(inputStream, height);
    }


    /* Delete last entry */
    public void deleteLastEntry() throws IOException {
        /* Get and check filesize. */
        long fileSize = getFileSize();
        if (!isCorrectFileSize(fileSize)) {
            throw new IOException(utxoSetLogFile.toString() + "is badly formatted.");
        }
        else if (fileSize == 0) {
            throw new IOException(utxoSetLogFile.toString() + "is empty.");
        }

        /* open appended file and truncate it */
        FileOutputStream outputStream = new FileOutputStream(utxoSetLogFile, true);
        FileChannel fileChannel = outputStream.getChannel();
        fileChannel.truncate(fileSize - UtxoSetLogEntry.LOGENTRY_SIZE);
        fileChannel.close();
        outputStream.close();
    }


    /* returns a utxo set log entry */
//    public UtxoSetLogEntry createEntry(SHA256HASH blockhash, int numShard,
//                                    SHA256HASH merkleRoot, int height) {
//        return new UtxoSetLogEntry(blockhash, numShard, merkleRoot, height);
//    }




//    public static void main(String args[]) {
//        String pathname = "/tmp/testutxologfile";
//
//        try {
//            Path path = Paths.get(pathname);
//            UtxoSetLog utxoSetLog = new UtxoSetLog(path);
//
////            System.out.println(utxoSetLog.getFileSize());
////            utxoSetLog.appendEntry(utxoSetLog.getEntry(
////                    SHA256HASH.ZERO_HASH, 1024, SHA256HASH.ZERO_HASH, 4));
////            System.out.println(utxoSetLog.getFileSize());
//            utxoSetLog.deleteLastEntry();
//
//            utxoSetLog.getLastEntry().print();
//            utxoSetLog.getEntry(0).print();
//            utxoSetLog.getEntry(1).print();
//            utxoSetLog.getEntry(2).print();
//            utxoSetLog.getEntry(3).print();
//            utxoSetLog.getEntry(4).print();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}

