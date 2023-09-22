package Blockchainj.Blockchain.UtxoSet.Shard;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Bitcoin.TXI;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Blockchain.UtxoSet.UTXOS.STX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.ShardChanges;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTXO;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * ShardSortedMapUtxs
 *
 * AbstractShard implementation that holds UTXs in an SortedMap.
 * This implementation is less efficient memory-wise.
 * This implementation is efficient computing-wise for medium to large Shards.
 *
 */

public class ShardSortedMapUtxs extends AbstractShard {
    public static final int SHARD_TYPE = 2;

    /* Array for UTX set. UTXs must be sorted. */
    private final TreeMap<SHA256HASH, UTX> utxs;


    /* Main constructor.
     * If UTX[] is null, empty shard will be created. */
    public ShardSortedMapUtxs(int shardNum, int shardIndex, SortedMap<SHA256HASH, UTX> utxs)
            throws IllegalArgumentException {
        super(shardNum, shardIndex);

        /* If utxs is null create an empty map and finish creating an empty shard */
        if(utxs == null) {
            this.utxs = new TreeMap<>();
        }
        /* If utxs is not null, add them to map */
        else {
            /* Create new TreeMap from given sortedMap */
            this.utxs = new TreeMap<>(utxs);

            /* Remove utxCount serialized size. */
            long prevUtxCountSerializedSize = (long) CompactSizeUInt.getSizeOf(0);
            serializedSize -= prevUtxCountSerializedSize;
            storageSerializedSize -= prevUtxCountSerializedSize;

            /* Get utx iterator */
            Iterator<UTX> utxIterator = this.utxs.values().iterator();

            /* Compute serialized sizes and utxo count */
            while(utxIterator.hasNext()) {
                /* Get utx. Must not be null. */
                UTX utx = utxIterator.next();

                /* Update utxoCount and serialized sizes */
                utxoCount +=  utx.getUtxosCount();
                serializedSize += utx.getSerializedSize();
                storageSerializedSize += utx.getStorageSerializedSize();
            }

            /* Add utx count serialized size. */
            long newUtxCountSerializedSize = (long)CompactSizeUInt.getSizeOf(this.utxs.size());
            serializedSize += newUtxCountSerializedSize;
            storageSerializedSize += newUtxCountSerializedSize;
        }
    }


    /* Adds the UTX to the shard. Returns null if UTX doesn't already exists, else it replaces the
       old one with the new one. */
    protected UTX addUTX(UTX utx) throws IllegalArgumentException {
        /* previous utxCount size */
        long prevUtxCountSize = (long)CompactSizeUInt.getSizeOf(getUtxCount());

        /* Put new utx into map */
        UTX oldUTX = utxs.put(utx.getTxid(), utx);

        /* check if utx already existed */
        if (oldUTX != null) {
            /* update utxoCount */
            utxoCount -= oldUTX.getUtxosCount();

            /* update serialized size */
            serializedSize -= oldUTX.getSerializedSize();
            storageSerializedSize -= oldUTX.getStorageSerializedSize();
        }

        /* update utxoCount */
        utxoCount += utx.getUtxosCount();

        /* update serialized size from utx */
        serializedSize += utx.getSerializedSize();
        storageSerializedSize += utx.getStorageSerializedSize();

        /* update serialized size from utxCount */
        long newUtxCountSize = (long)CompactSizeUInt.getSizeOf(getUtxCount());
        serializedSize -= prevUtxCountSize;
        serializedSize += newUtxCountSize;
        storageSerializedSize -= prevUtxCountSize;
        storageSerializedSize += newUtxCountSize;

        return oldUTX;
    }


    /* Removes and returns UTXO, returns null if it doesn't exists */
    protected void spentUTXO(TXI txi, int height) throws BitcoinUtxoSetException {
        /* get utx */
        UTX utx = utxs.get(txi.getPrevTxid());

        /* return null if UTX not found */
        if(utx == null) {
            throw new BitcoinUtxoSetException("UTX not found", txi);
        }

        /* keep previous serialized size */
        long utxPrevSerializedSize = utx.getSerializedSize();
        long utxPrevStorageSerializedSize = utx.getStorageSerializedSize();

        /* try to spent TXO from UTX */
        UTX newUtx = utx.spentUTXO(txi, height);

        /* decrease UTXO count */
        utxoCount--;

        /* if utx is not empty replace new with old */
        if(newUtx != null) {
            utxs.put(newUtx.getTxid(), newUtx);

            /* update serialized size */
            serializedSize -= utxPrevSerializedSize;
            serializedSize += newUtx.getSerializedSize();
            storageSerializedSize -= utxPrevStorageSerializedSize;
            storageSerializedSize += newUtx.getStorageSerializedSize();

        }
        /* if UTX is empty */
        else {
            /* previous utxCount size */
            long prevUtxCountSize = (long)CompactSizeUInt.getSizeOf(getUtxCount());

            /* remove UTX */
            if( utxs.remove(utx.getTxid()) == null) {
                throw new RuntimeException("UTX expected but not found.");
            }

            /* update serialized size */
            serializedSize -= utxPrevSerializedSize;
            storageSerializedSize -= utxPrevStorageSerializedSize;

            /* update serialized size from utxCount */
            long newUtxCountSize = (long)CompactSizeUInt.getSizeOf(getUtxCount());
            serializedSize -= prevUtxCountSize;
            serializedSize += newUtxCountSize;
            storageSerializedSize -= prevUtxCountSize;
            storageSerializedSize += newUtxCountSize;
        }
    }


    @Override
    public UTXO getUTXO(SHA256HASH txid, int outIndex) {
        /* get utx */
        UTX utx = utxs.get(txid);

        if(utx == null) {
            /* return null if UTX not found */
            return null;
        } else {
            /* get utxo and return */
            return utx.getUtxo(outIndex);
        }
    }


    /** Prototype Protocol serialization. */
    public static ShardSortedMapUtxs deserialize(InputStream inputStream) throws IOException {
        try {
            /* read shard metadata */
            int shardNum = BitcoinParams.readINT32(inputStream);
            int shardIndex = BitcoinParams.readINT32(inputStream);

            /* read utxCount */
            CompactSizeUInt utxCount = CompactSizeUInt.deserialize(inputStream);
            int utxCountInt = (int)utxCount.getValue();

            /* Create UTX map */
            TreeMap<SHA256HASH, UTX> utxs = new TreeMap<>();

            /* read UTXs. */
            if (utxCountInt == 0) {
                return new ShardSortedMapUtxs(shardNum, shardIndex, null);
            } else {
                for (int i = 0; i < utxCountInt; i++) {
                    UTX utx = getUtxFactoryStatic().deserialize(inputStream);
                    utxs.put(utx.getTxid(), utx);
                }

                return new ShardSortedMapUtxs(shardNum, shardIndex, utxs);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }


    /** Storage deserialization */
    public static ShardSortedMapUtxs load(InputStream inputStream) throws IOException {
        try {
            /* read shard metadata */
            int shardNum = BitcoinParams.readINT32(inputStream);
            int shardIndex = BitcoinParams.readINT32(inputStream);

            /* read utxCount */
            CompactSizeUInt utxCount = CompactSizeUInt.deserialize(inputStream);
            int utxCountInt = (int)utxCount.getValue();

            /* Create UTX map */
            TreeMap<SHA256HASH, UTX> utxs = new TreeMap<>();

            /* read UTXs. */
            if (utxCountInt == 0) {
                return new ShardSortedMapUtxs(shardNum, shardIndex, null);
            } else {
                for (int i = 0; i < utxCountInt; i++) {
                    UTX utx = getUtxFactoryStatic().load(inputStream);
                    utxs.put(utx.getTxid(), utx);
                }

                return new ShardSortedMapUtxs(shardNum, shardIndex, utxs);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }


    @Override
    public int getUtxCount() { return utxs.size(); }


    @Override
    public Iterator<UTX> getUtxIterator() { return utxs.values().iterator(); }


    /* Apply shard changes to shard.
     * Changes are permanent. If this operation fails, the shard's state is undefined. */
    @Override
    public void applyShardChanges(ShardChanges shardChanges) throws BitcoinUtxoSetException {
        /* For each newly spent transaction inputs, remove their unspent transaction outputs.
         * Get sorted iterator. */
        Iterator<STX> stxIt = shardChanges.getStxIterator();
        while(stxIt.hasNext()) {
            /* Get next stx */
            STX stx = stxIt.next();

            /* For each TXI in stx remove the approprite UTXO */
            Iterator<TXI> txiIt = stx.getTxiIterator();
            while(txiIt.hasNext()) {
                /* Get next txi */
                TXI txi = txiIt.next();

                /* Remove utxo */
                spentUTXO(txi, shardChanges.getHeight());
            }
        }

        /* For each newly unspent transaction outputs, add their UTX to the shard */
        Iterator<UTX> utxIt = shardChanges.getUtxIterator();
        while(utxIt.hasNext()) {
            /* Get next UTX */
            UTX utx = utxIt.next();

            /* Check if utx has correct txid. */
            if(ProtocolParams.calcShardIndex(shardNum, utx.getTxid()) != getShardIndex()) {
                throw new BitcoinUtxoSetException("UTX txid out of shard range.", utx);
            }

            /* Add new utx. */
            UTX oldUtx = addUTX(utx);

            /* Utx must not already exist */
            if(oldUtx != null) {
                throw new BitcoinUtxoSetException("UTX already existed.", oldUtx, utx);
            }
        }
    }


    @Override
    public void printParameters(PrintStream printStream) {
        super.printParameters(printStream);
        printStream.println(">" + this.getClass().toString());
    }
}
