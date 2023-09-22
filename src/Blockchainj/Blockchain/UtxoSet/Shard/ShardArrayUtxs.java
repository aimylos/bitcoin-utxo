package Blockchainj.Blockchain.UtxoSet.Shard;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Bitcoin.TXI;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;


/**
 * ShardArrayUtxs
 *
 * AbstractShard implementation that holds UTXs in an array.
 * This implementation is efficient memory-wise.
 * This implementation is efficient computing-wise for relatively small Shards,
 * while it's highly inefficient for large Shards.
 *
 */


@Deprecated
public class ShardArrayUtxs {
//public class ShardArrayUtxs extends AbstractShard {
//    public static final int SHARD_TYPE = 1;
//
//    /* Array for UTX set. UTXs must be sorted. */
//    private UTX[] utxs;
//
//
//    /* Main constructor.
//     * UTX[] must be sorted or null.
//     * If UTX[] is null, empty shard will be created. */
//    public ShardArrayUtxs(int shardNum, int shardIndex, UTX[] sortedUtxs)
//            throws IllegalArgumentException {
//        super(shardNum, shardIndex);
//
//        /* If UTX[] is null, create empty shard. */
//        if(sortedUtxs == null) {
//            /* Init empty sorted map */
//            utxs = new UTX[0];
//        }
//        /* Else create shard from sorted UTX[] */
//        else {
//            /* Remove utxCount serialized size. */
//            long prevUtxCountSerializedSize = (long) CompactSizeUInt.getSizeOf(0);
//            serializedSize -= prevUtxCountSerializedSize;
//            storageSerializedSize -= prevUtxCountSerializedSize;
//
//            /* Set utxs array */
//            utxs = sortedUtxs;
//            int utxCount = utxs.length;
//
//            /* Compute serialized sizes and utxo count
//             * Also make sure input is sorted. */
//            UTX prevUTX = null;
//            for(int i=0; i<utxCount; i++) {
//                /* Get utx. Must not be null. */
//                UTX utx = utxs[i];
//
//                /* Make sure it's greater than previous UTX */
//                if(prevUTX != null) {
//                    if(prevUTX.compareTo(utx) >= 0) {
//                        throw new IllegalArgumentException("UTX[] is not sorted.");
//                    }
//                }
//                prevUTX = utx;
//
//                /* Update utxoCount and serialized sizes */
//                utxoCount +=  utx.getUtxosCount();
//                serializedSize += utx.getSerializedSize();
//                storageSerializedSize += utx.getStorageSerializedSize();
//            }
//
//            /* Add utx count serialized size. */
//            long newUtxCountSerializedSize = (long)CompactSizeUInt.getSizeOf(utxCount);
//            serializedSize += newUtxCountSerializedSize;
//            storageSerializedSize += newUtxCountSerializedSize;
//        }
//    }
//
//
//    @Override
//    public UTXO getUTXO(SHA256HASH txid, int outIndex) {
//        /* TODO: Binary search */
//        for(int i=0; i<getUtxCount(); i++) {
//            if(utxs[i].getTxid().equals(txid)) {
//                return utxs[i].getUtxo(outIndex);
//            }
//        }
//
//        return null;
//    }
//
//
//    /** Prototype Protocol serialization. */
//    public static ShardArrayUtxs deserialize(InputStream inputStream) throws IOException {
//        try {
//            /* read shard metadata */
//            int shardNum = BitcoinParams.readINT32(inputStream);
//            int shardIndex = BitcoinParams.readINT32(inputStream);
//
//            /* read utxCount */
//            CompactSizeUInt utxCount = CompactSizeUInt.deserialize(inputStream);
//
//            /* Create UTX array */
//            UTX[] sortedUtxs = new UTX[(int)utxCount.getValue()];
//
//            /* read UTXs. Expect UTXs to be sorted. Will be caught by constructor. */
//            if (sortedUtxs.length == 0) {
//                return new ShardArrayUtxs(shardNum, shardIndex, null);
//            } else {
//                for (int i = 0; i < sortedUtxs.length; i++) {
//                    sortedUtxs[i] = UTX.deserialize(inputStream);
//                }
//
//                return new ShardArrayUtxs(shardNum, shardIndex, sortedUtxs);
//            }
//        } catch (IllegalArgumentException e) {
//            throw new IOException(e);
//        }
//    }
//
//
//    /** Storage deserialization */
//    public static ShardArrayUtxs load(InputStream inputStream) throws IOException {
//        try {
//            /* read shard metadata */
//            int shardNum = BitcoinParams.readINT32(inputStream);
//            int shardIndex = BitcoinParams.readINT32(inputStream);
//
//            /* read utxCount */
//            CompactSizeUInt utxCount = CompactSizeUInt.deserialize(inputStream);
//
//            /* Create UTX array */
//            UTX[] sortedUtxs = new UTX[(int)utxCount.getValue()];
//
//            /* read UTXs. Expect UTXs to be sorted. Will be caught by constructor. */
//            if (sortedUtxs.length == 0) {
//                return new ShardArrayUtxs(shardNum, shardIndex, null);
//            } else {
//                for (int i = 0; i < sortedUtxs.length; i++) {
//                    sortedUtxs[i] = UTX.load(inputStream);
//                }
//
//                return new ShardArrayUtxs(shardNum, shardIndex, sortedUtxs);
//            }
//        } catch (IllegalArgumentException e) {
//            throw new IOException(e);
//        }
//    }
//
//
//    @Override
//    public int getUtxCount() { return utxs.length; }
//
//    @Override
//    public Iterator<UTX> getUtxIterator() { return Arrays.asList(utxs).iterator(); }
//
//
//
//    /* Apply shard changes to shard.
//     * Changes are permanent. If this operation fails, the shard's state is undefined. */
//    @Override
//    //TODO Redo this method. May be missing or adding utxos that it shouldn't!
//    public void applyShardChanges(ShardChanges shardChanges) throws BitcoinUtxoSetException {
//        /* Keep previous utxCount serialized size */
//        long prevUtxCountSeriliazedSize = (long)CompactSizeUInt.getSizeOf(getUtxCount());
//
//        /* For each newly spent transaction inputs, remove their unspent transaction outputs.
//         * Get sorted iterator. */
//        Iterator<STX> stxIt = shardChanges.getSortedStxIterator();
//        int utxIndex = 0;
//        int utxCount = getUtxCount();
//        int utxRemovedCount = 0;
//        while(stxIt.hasNext()) {
//            /* Get next spent stx */
//            STX spentStx = stxIt.next();
//
//            /* Get utx in shard. */
//            UTX currentUtx = null;
//            int currentUtxIndex = -1;
//            for(; utxIndex<utxCount; utxIndex++) {
//                if( utxs[utxIndex].getTxid().equals(spentStx.getPrevTxid()) ) {
//                    currentUtxIndex = utxIndex;
//                    currentUtx = utxs[utxIndex];
//                    utxIndex++;
//                    break;
//                }
//            }
//
//            /* Check if utx found. */
//            if(currentUtx == null) {
//                throw new BitcoinUtxoSetException("UTX not found.", spentStx);
//            }
//
//            /* Keep previous utx seriliazed size. */
//            long prevUtxSerializedSize = currentUtx.getSerializedSize();
//            long prevUtxStorageSerializedSize = currentUtx.getStorageSerializedSize();
//
//            /* For each TXI remove the TXO */
//            Iterator<TXI> txiIt = spentStx.getTxiIterator();
//            while(txiIt.hasNext()) {
//                /* Get spent txi */
//                TXI txi = txiIt.next();
//
//                /* Remove spent utxo */
//                UTXO utxo = currentUtx.removeUtxo(txi.getPrevOutIndex());
//
//                /* if UTXO not found throw exception */
//                if (utxo == null) {
//                    throw new BitcoinUtxoSetException("UTXO not found.", txi);
//                }
//
//                /* Update utxo count */
//                utxoCount--;
//            }
//
//            /* If utx is empty remove it. Update serilaized sizes. */
//            if(currentUtx.isEmpty()) {
//                utxs[currentUtxIndex] = null;
//                utxRemovedCount++;
//
//                serializedSize -= prevUtxSerializedSize;
//                storageSerializedSize -= prevUtxStorageSerializedSize;
//            } else {
//                serializedSize -= prevUtxSerializedSize;
//                serializedSize += currentUtx.getSerializedSize();
//                storageSerializedSize -= prevUtxStorageSerializedSize;
//                storageSerializedSize += currentUtx.getStorageSerializedSize();
//            }
//        }
//
//        /* New utx count */
//        int newUtxCount = (utxCount-utxRemovedCount) + shardChanges.getUtxsCount();
//
//        /* New utxs array */
//        UTX[] newUtxs = new UTX[newUtxCount];
//
//        /* For each newly unspent transaction outputs, add their UTX to the shard */
//        /* Merge old utxs with new utxs */
//        int oldUtxIndex = 0;
//        int oldUtxCount = utxs.length;
//        UTX oldUtx = null;
//        Iterator<UTX> newUtxIt = shardChanges.getSortedUtxIterator();
//        UTX newUtx = null;
//        for(int i=0; i<newUtxs.length; i++) {
//            /* Get old utx */
//            if(oldUtx == null && oldUtxIndex<oldUtxCount) {
//                for(; oldUtxIndex<oldUtxCount; oldUtxIndex++) {
//                    if(utxs[oldUtxIndex] != null) {
//                        oldUtx = utxs[oldUtxIndex];
//                        utxs[oldUtxIndex] = null; //Help garbage collector
//                        oldUtxIndex++;
//                        break;
//                    }
//                }
//            }
//
//            /* Get new utx */
//            if(newUtx == null && newUtxIt.hasNext()) {
//                newUtx = newUtxIt.next();
//
//                /* Check if utx has correct txid. */
//                //noinspection ConstantConditions
//                if(ProtocolParams.calcShardIndex(shardNum, newUtx.getTxid()) != getShardIndex()) {
//                    throw new BitcoinUtxoSetException("UTX txid out of shard range.", newUtx);
//                }
//
//                /* Check if utx is empty. */
//                if(newUtx.isEmpty()) {
//                    throw new BitcoinUtxoSetException("UTX is empty.", newUtx);
//                }
//            }
//
//            /* compare utxs */
//            int oldVsNew;
//            if(oldUtx == null) {
//                oldVsNew = 1;
//            } else if(newUtx == null) {
//                oldVsNew = -1;
//            } else {
//                oldVsNew = oldUtx.compareTo(newUtx);
//            }
//
//            /* Check if old and new utx match. Since this a UTX created in the latest block,
//               no UTX with the same Txid must exist. */
//            if(oldVsNew == 0) {
//                throw new BitcoinUtxoSetException("UTX already existed.", oldUtx, newUtx);
//            }
//            else if(oldVsNew < 0) {
//                /* Add old utx to new utx array */
//                newUtxs[i] = oldUtx;
//                oldUtx = null;
//            }
//            else {
//                /* Add new utx to new utx array */
//                newUtxs[i] = newUtx;
//
//                /* Update utxoCount */
//                //noinspection ConstantConditions
//                utxoCount += newUtx.getUtxosCount();
//
//                /* Update serialized size. */
//                serializedSize += newUtx.getSerializedSize();
//                storageSerializedSize += newUtx.getStorageSerializedSize();
//
//                newUtx = null;
//            }
//        }
//
//        /* Replace utxs array */
//        utxs = newUtxs;
//
//        /* Update seriliazed sizes for new utxCount */
//        long newUtxCountSerializedSize = (long)CompactSizeUInt.getSizeOf(utxs.length);
//        serializedSize -= prevUtxCountSeriliazedSize;
//        serializedSize += newUtxCountSerializedSize;
//        storageSerializedSize -= prevUtxCountSeriliazedSize;
//        storageSerializedSize += newUtxCountSerializedSize;
//    }
//
//
//
//    @Override
//    public void printParameters(PrintStream printStream) {
//        super.printParameters(printStream);
//        printStream.println(">" + this.getClass().toString());
//    }
}
