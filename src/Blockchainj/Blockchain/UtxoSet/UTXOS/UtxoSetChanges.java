package Blockchainj.Blockchain.UtxoSet.UTXOS;


import Blockchainj.Bitcoin.*;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetChangesException;
import Blockchainj.Util.SHA256HASH;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * UtxoSetChanges
 *
 * The changes that need to be applied to the Utxo Set after having processed a block.
 *
 * Use:
 * - Add a STXI. Adds a Spent Transaction Input that doesn't already exist.
 * - Remove a TXI. Removes a Spent Transaction Input that exists.
 * - Add a UTX. Adds an Unspent Transaction's outputs that doesn't already exist.
 * - Remove a UTXO. Removes an Unspent Transaction output that exists.
 * - Calculate stats.
 *
 */

public class UtxoSetChanges {
    /* Block metadata */
    private final SHA256HASH blockhash;
    private final int height;

    /* Number of shards */
    private final int shardNum;

    /* counters */
    private int utxCount = 0;
    private int utxoCount = 0;
    private int stxCount = 0;
    private int stxiCount = 0;

    /* Shard changes. Keep changes ordered */
    private final TreeMap<Integer, ShardChanges> shardChanges;


    /* Constructor */
    public UtxoSetChanges(int shardNum, SHA256HASH blockhash, int height) {
        this.shardNum = shardNum;
        this.blockhash = blockhash;
        this.height = height;
        this.shardChanges = new TreeMap<>();
    }


    /* Get methods */
    public SHA256HASH getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public int getShardNum() { return shardNum; }

    public int getModifiedShardCount() { return shardChanges.size(); }

    public int getUtxCount() {
//        int counter = 0;
//        Iterator<ShardChanges> it = shardChanges.values().iterator();
//        while(it.hasNext()) {
//            counter += it.next().getUtxsCount();
//        }
//
//        if(counter != utxCount) {
//            throw new RuntimeException("utx count dont' match");
//        }

        return utxCount;
    }

    public int getUtxoCount() {
//        int counter = 0;
//        Iterator<ShardChanges> it = shardChanges.values().iterator();
//        while(it.hasNext()) {
//            counter += it.next().getUtxoCount();
//        }
//
//        if(counter != utxoCount) {
//            throw new RuntimeException("utxo count dont' match");
//        }

        return utxoCount;
    }

    public int getStxCount() {
//        int counter = 0;
//        Iterator<ShardChanges> it = shardChanges.values().iterator();
//        while(it.hasNext()) {
//            counter += it.next().getStxsCount();
//        }
//
//        if(counter != stxCount) {
//            throw new RuntimeException("stx count dont' match");
//        }

        return stxCount;
    }

    public int getStxiCount() {
//        int counter = 0;
//        Iterator<ShardChanges> it = shardChanges.values().iterator();
//        while(it.hasNext()) {
//            counter += it.next().getStxiCount();
//        }
//
//        if(counter != stxiCount) {
//            throw new RuntimeException("stxi count dont' match");
//        }

        return stxiCount;
    }

    public int getTxidCount() { return getUtxCount() + getStxCount(); }


    public Iterator<ShardChanges> getShardChangesIterator() {
        return shardChanges.values().iterator();
    }

    public Iterator<ShardChanges> getSortedShardChangesIterator() {
        return shardChanges.values().iterator();
    }


    /* Get shardChanges and create if does not exist. */
    private ShardChanges getShardChanges(int shardIndex) {
        /* get shardChanges */
        ShardChanges shardC = shardChanges.get(shardIndex);

        /* if shard changes don't exist create it */
        if(shardC == null) {
            shardC = new ShardChanges(shardIndex, height);
            if( shardChanges.put(shardIndex, shardC) != null) {
                throw new RuntimeException("Excpected no ShardChanges for that shardIndex");
            }
        }

        return shardC;
    }


    /* Put a TXI. Puts txi in corresponding shardChanges. */
    public boolean putTXI(TXI txi) {
        /* get shard index for txi */
        int shardIndex = ProtocolParams.calcShardIndex(shardNum, txi.getPrevTxid());

        /* get shard changes */
        ShardChanges shardC = getShardChanges(shardIndex);

        /* prev counters */
        int prevStxCount = shardC.getStxsCount();
        int prevStxiCount = shardC.getStxiCount();

        /* add txi to shardChg */
        boolean res = shardC.putTXI(txi);

        if(res) {
            stxCount -= prevStxCount;
            stxCount += shardC.getStxsCount();

            stxiCount -= prevStxiCount;
            stxiCount += shardC.getStxiCount();

            return true;
        } else {
            return false;
        }
    }


//    /* Remove a TXI. Removes txi in corresponding shardChanges. */
//    @Deprecated
//    private boolean removeTXI(SHA256HASH prevTxid, int prevOutIndex) {
//        /* get shard index for txi */
//        int shardIndex = ProtocolParams.calcShardIndex(shardNum, prevTxid);
//
//        /* get shard changes */
//        ShardChanges shardC = getShardChanges(shardIndex);
//
//        /* prev counters */
//        int prevStxCount = shardC.getStxsCount();
//        int prevStxiCount = shardC.getStxiCount();
//
//        /* remove txi from shardC */
//        boolean res = shardC.removeTXI(prevTxid, prevOutIndex);
//
//        if(res) {
//            stxCount -= prevStxCount;
//            stxCount += shardC.getStxsCount();
//
//            stxiCount -= prevStxiCount;
//            stxiCount += shardC.getStxiCount();
//
//            return true;
//        } else {
//            return false;
//        }
//    }


    /* Put a UTX. Puts utx in corresponding shardChanges. */
    public boolean putUTX(UTX utx) {
        /* get shard index for txi */
        int shardIndex = ProtocolParams.calcShardIndex(shardNum, utx.getTxid());

        /* get shard changes */
        ShardChanges shardC = getShardChanges(shardIndex);

        /* Previous counters */
        int prevUtxCount = shardC.getUtxsCount();
        int prevUtxoCount = shardC.getUtxoCount();

        /* add utx to shardChg */
        boolean res = shardC.putUTX(utx);

        if(res) {
            utxCount -= prevUtxCount;
            utxCount += shardC.getUtxsCount();

            utxoCount -= prevUtxoCount;
            utxoCount += shardC.getUtxoCount();

            return true;
        } else {
            return false;
        }
    }


    /* Remove a TXO. Spents txo from corresponding shardChanges */
    public boolean spentUTXO(TXI txi, int height) {
        /* get shard index for txi */
        int shardIndex = ProtocolParams.calcShardIndex(shardNum, txi.getPrevTxid());

        /* get shard changes */
        ShardChanges shardC = getShardChanges(shardIndex);

        /* Previous counters */
        int prevUtxCount = shardC.getUtxsCount();
        int prevUtxoCount = shardC.getUtxoCount();

        /* remove utxo from shardC */
        boolean res = shardC.spentUTXO(txi, height);

        if(res) {
            utxCount -= prevUtxCount;
            utxCount += shardC.getUtxsCount();

            utxoCount -= prevUtxoCount;
            utxoCount += shardC.getUtxoCount();

            return true;
        } else {
            return false;
        }
    }


    /* DEBUG ONLY */
    public void print(PrintStream printStream, boolean doShardsHeadersOnly, boolean doDetails) {
        printStream.println("Blockhash: " + blockhash.toString());
        printStream.println("Height: " + height);
        printStream.println("ShardNum: " + shardNum);
        printStream.println("UTX count: " + getUtxCount());
        printStream.println("STX count: " + getStxCount());
        printStream.println("TXO count: " + getUtxoCount());
        printStream.println("TXI count: " + getStxiCount());

        Iterator<ShardChanges> it = shardChanges.values().iterator();
        while(it.hasNext()) {
            printStream.println("");
            it.next().print(printStream, doShardsHeadersOnly, doDetails);
        }
    }


    /** Process block into utxo set changes.
     *  To gain time, this method creates a UtxoSetChanges object, without accessing the utxo set.
     *  Transaction inputs that spent a transaction output created in the same block will be
     *  removed (both the spending transaction input and the spent transaction output.
     *  For this block to be valid, and therefore the utxoSetChanges to be valid, every
     *  transaction input in the utxoSetChanges must exist as an unspent transaction output
     *  in the utxo set and every transaction output in the utxoSetChanges must not exist in the
     *  utxo set.
     *  This method does not know if this block is valid. But the utxoSetChanges created by this
     *  method, once and if applied to the utxo set successfully, validate the block. **/
    /* This method will create UTX instances.
       The data used to create the instances will be first acquired as deep copies and
       will be completely detached from the block.

       The UTX_TYPE is used in accordance to UtxType. */
    public static UtxoSetChanges calcNewUtxoSetChanges(Block block, int shardNum,
                                                       UtxFactory utxFactory)
            throws BitcoinUtxoSetChangesException {
        /* Validate shard number */
        ProtocolParams.validateShardNum(shardNum);

        /* init changes */
        UtxoSetChanges changes = new UtxoSetChanges(
                shardNum, block.getBlockhash(), block.getHeight());

        /* Process each transaction */
        Iterator<Transaction> txIterator = block.getTxIterator();
        mainTxLoop: while(txIterator.hasNext()) {
            Transaction t = txIterator.next();

            /* Check for non-unique txids */
            int height = block.getHeight();
            if(height <= BitcoinParams.NON_UNIQUE_TXIDS_LAST_HEIGHT &&
                    height >= BitcoinParams.NON_UNIQUE_TXIDS_FIRST_HEIGHT) {
                SHA256HASH txid = t.getTxid();
                for(int i=0; i<BitcoinParams.NON_UNIQUE_TXIDS.length; i++) {
                    if(txid.equals(BitcoinParams.NON_UNIQUE_TXIDS[i])) {
                        /* If it's not first appearance skip transaction else process it. */
                        if(height != BitcoinParams.NON_UNIQUE_TXIDS_FIRST_SEEN[i]) {
                            continue mainTxLoop;
                        }
                    }
                }
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
                /* Else check if transaction input can be spent from a transaction output in
                changes. If not add it to changes, else remove according TXO from changes. */
                else if( !changes.spentUTXO(tIn, block.getHeight()) ) {
                    if( !changes.putTXI(tIn) ) {
                        throw new BitcoinUtxoSetChangesException(
                                "Failed to put txi to changes.", tIn.getBlockhash().toString(),
                                tIn.getHeight(), tIn);
                    }
                }
            }

            /* Process transaction outputs. Get new UTX instance */
            UTX utx = utxFactory.getNewUTX(t);

            /*  UTX must not already exist in the changes. */
            if( !changes.putUTX(utx) ) {
                throw new BitcoinUtxoSetChangesException("Failed to put utx to chagnes.",
                        block.getBlockhash().toString(), height, utx);
            }
        }

        return changes;
    }


    /* Returns utx, utxo, stx, stxi counts without recalculating changes
     * for given shardNum.
     * For more efficient calculations provide a utxoSetChanges instrance of shardNumber 1.  */
    public static UtxoSetChangesCardinalities calcUtxoSetChangesCardinalities(
            Block block, int shardNum, UtxoSetChanges utxoSetChanges)
            throws BitcoinUtxoSetChangesException {
        /* Check utxoSetChanges provided */
        if(utxoSetChanges != null) {
            /* Reading mutliple shards is still more efficient that rebuilding utxoSetChanges
               but not worth it. */
            if(     (utxoSetChanges.getShardNum() != 1) ||
                    (!block.getBlockhash().equals(utxoSetChanges.getBlockhash())) ||
                    (block.getHeight() != utxoSetChanges.getHeight()) ) {
                utxoSetChanges = null;
            }
        }

        /* Cardinalities to calculate */
        int modifiedShardCount;
        int utxCount;
        int utxoCount;
        int stxCount;
        int stxiCount;

        /* If corrent utxoSetChanges provided calculate from it */
        if(utxoSetChanges != null) {
            /* Get the only shard change. Two irregular blocks won't have any. */
            Iterator<ShardChanges> itShardChange = utxoSetChanges.getShardChangesIterator();
            ShardChanges shardChanges;
            if(itShardChange.hasNext()) {
                shardChanges = itShardChange.next();
            } else {
                /* else create empty one */
                shardChanges = new ShardChanges(0, block.getHeight());
            }

            /* Create hashset for modified shards */
            HashSet<Integer> shardsInvolved = new HashSet<>(utxoSetChanges.getTxidCount()*2);

            Iterator<STX> itSTX = shardChanges.getStxIterator();
            while(itSTX.hasNext()) {
                STX stx = itSTX.next();

                /* Get stx index and add into set */
                int shardIndex = ProtocolParams.calcShardIndex(shardNum, stx.getPrevTxid());
                shardsInvolved.add(shardIndex);
            }

            Iterator<UTX> itUTX = shardChanges.getUtxIterator();
            while(itUTX.hasNext()) {
                UTX utx = itUTX.next();

                /* Get utx index and add into set */
                int shardIndex = ProtocolParams.calcShardIndex(shardNum, utx.getTxid());
                shardsInvolved.add(shardIndex);
            }

            modifiedShardCount = shardsInvolved.size();
        }
        /* If utxoSetChanges are not available, create them */
        else {
            utxoSetChanges = UtxoSetChanges.calcNewUtxoSetChanges(
                    block, shardNum, new UtxFastFactory());
            modifiedShardCount = utxoSetChanges.getModifiedShardCount();
        }

        utxCount = utxoSetChanges.getUtxCount();
        utxoCount = utxoSetChanges.getUtxoCount();
        stxCount = utxoSetChanges.getStxCount();
        stxiCount = utxoSetChanges.getStxiCount();

        return new UtxoSetChangesCardinalities(
                block.getBlockhash(), block.getHeight(),
                shardNum, modifiedShardCount,
                utxCount, utxoCount,
                stxCount, stxiCount);
    }


//    /* DEBUG/TEST */
//    public static void main(String[] args) {
//
//        int height = 500000;
//        int shardNum = 1024;
//
//        test1(height, shardNum);
//    }
//
//    /* TEST */
//    private static void test1(int height, int shardNum) {
//        int printPeriod = 1000;
//        try{
//            SimpleBlockBuffer bb = new SimpleBlockBuffer(height, height, new RPCconnection());
//            Block block = bb.getNextBlock();
//            UtxoSetChanges changes = new UtxoSetChanges(
//                    shardNum, block.getBlockhash(), block.getHeight());
//
//            for(int i=0; i<block.getTxnCount(); i++) {
//                Transaction t = block.getTxByIndex(i);
//
//                /* add txi */
//                TXI[] txis = t.getTXIs();
//                for(int j=0; j<txis.length; j++) {
//                    if(t.isCoinbase()) {
//                        break;
//                    }
//                    if( !changes.putTXI(txis[j]) )
//                    {
//                        //changes.print(false, false);
//                        throw new RuntimeException("txi");
//                    }
//                }
//
//                /* add utx */
//                if( !changes.putUTX(t.getUTX()) ) {
//                    //changes.print(false, false);
//                    throw new RuntimeException("utx");
//                }
//            }
//
//            changes.print(true, false);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}
