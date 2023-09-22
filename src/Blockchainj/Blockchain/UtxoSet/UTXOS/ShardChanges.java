package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.TXI;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Blockchain.UtxoSet.UTXOS.STX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Util.SHA256HASH;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * ShardChanges - Shard Changes
 *
 */


public class ShardChanges {
    /* Shard index. */
    private final int shardIndex;

    /* block height */
    private final int height;


    /* Newly spent transaction inputs.
     * All newly spent transaction inputs that have the prevTxid field
     * within range of this shard. */
    private final LinkedHashMap<SHA256HASH, STX> stxs;

    /* Newly unspent transaction outputs.
     * All newly unspent transaction outputs that have a Txid
     * within range of this shard. */
    private final LinkedHashMap<SHA256HASH, UTX> utxs;


    /* Spent transction input count and unspent transaction output count */
    private int stxiCount;
    private int utxoCount;

    /* Constructor */
    public ShardChanges(int shardIndex, int height) {
        this.height = height;
        this.shardIndex = shardIndex;
        this.stxs = new LinkedHashMap<>();
        this.utxs = new LinkedHashMap<>();
        this.stxiCount = 0;
        this.utxoCount = 0;
    }

    /* get counts */
    public int getStxsCount() { return stxs.size(); }

    public int getUtxsCount() { return utxs.size(); }

    public int getStxiCount() { return stxiCount; }

    public int getUtxoCount() { return utxoCount; }


    /* get iterators */
    public Iterator<STX> getStxIterator() {
        return stxs.values().iterator();
    }

    public Iterator<UTX> getUtxIterator() {
        return utxs.values().iterator();
    }

    public Iterator<STX> getSortedStxIterator() {
        ArrayList<STX> list = new ArrayList<>(stxs.values());
        list.sort(null);
        return list.iterator();
    }

    public Iterator<UTX> getSortedUtxIterator() {
        ArrayList<UTX> list = new ArrayList<>(utxs.values());
        list.sort(null);
        return list.iterator();
    }


    /* get shardIndex */
    public int getShardIndex() { return shardIndex; }


    public int getHeight() { return height; }


    /* Put TXI. Puts txi in corresponding STX if txi doesn't already exist in STX
     * and return true. Else if txi already exists in STX returns false without adding it. */
    public boolean putTXI(TXI txi) {
        /* get STX */
        STX stx = stxs.get(txi.getPrevTxid());

        /* create STX if does not exist */
        if(stx == null) {
            stx = new STX(txi.getPrevTxid());
            if(stxs.put(stx.getPrevTxid(), stx) != null) {
                throw new RuntimeException("Excected no STX for that prevTxid.");
            }
        }

        /* Add txi to stx if it doesn't already exist. Calls the not safe method but it's sure
         * for valid input. */
        if(stx.addStxiValidInput(txi)) {
            stxiCount++;
            return true;
        } else {
            return false;
        }
    }


    /* Remove TXI. Removes txi in corresponding STX and removes STX if empty. Returns true
     * if txi found and removed, else false. */
    @Deprecated
    private boolean removeTXI(SHA256HASH prevTxid, int prevOutIndex) {
        /* get STX */
        STX stx = stxs.get(prevTxid);

        /* if stx not found return null */
        if(stx == null) {
            return false;
        }

        /* remove txi */
        if(stx.removeStxi(prevOutIndex)) {
            /* if stx is empty remove it */
            if(stx.isEmpty()) {
                if(stxs.remove(prevTxid) == null) {
                    throw new RuntimeException("Expected STX but not found.");
                }
            }
            stxiCount--;
            return true;
        }
        else {
            return false;
        }
    }


    /* Put UTX. Puts utx in shard if utx doesn't already exists and returns true. */
    public boolean putUTX(UTX utx) {
        /* Check if utx already exists */
        if(utxs.containsKey(utx.getTxid())) {
            return false;
        }

        /* put utx */
        UTX prevUtx = utxs.put(utx.getTxid(), utx);

        if( prevUtx != null ) {
            throw new RuntimeException("Expected utx not found.");
        }
        else {
            utxoCount += utx.getUtxosCount();
            return true;
        }
    }


    /* Remove TXO. Spents txo in corresponding UTX and removes UTX if empty. Returns true
     * if txo found and removed, else false. */
    public boolean spentUTXO(TXI txi, int height) {
        /* get UTX */
        UTX utx = utxs.get(txi.getPrevTxid());

        /* if utx not found return false */
        if(utx == null) {
            return false;
        }

        try {
            /* Spent txi */
            UTX newUtx = utx.spentUTXO(txi, height);

            utxoCount--;

            /* if utx is empty remove it else replace it */
            if(newUtx == null) {
                utxs.remove(utx.getTxid());
            } else {
                utxs.put(newUtx.getTxid(), newUtx);
            }
        } catch (BitcoinUtxoSetException e) {
            return false;
        }

        return true;
    }



    /* DEBUG ONLY */
    public void print(PrintStream printStream, boolean doHeaderOnly, boolean doDetails) {
        printStream.println("ShardIndex: " + shardIndex);
        printStream.println("UTX count: " + getUtxsCount());
        printStream.println("STX count: " + getStxsCount());
        printStream.println("UTXO count: " + getUtxoCount());
        printStream.println("TXI count: " + getStxiCount());
        if(!doHeaderOnly) {
            Iterator<STX> it1 = getStxIterator();
            while(it1.hasNext()) {
                it1.next().print(printStream, doDetails);
            }

            Iterator<UTX> it2 = getUtxIterator();
            while(it2.hasNext()) {
                it2.next().print(printStream, doDetails);
            }
        }
    }
}
