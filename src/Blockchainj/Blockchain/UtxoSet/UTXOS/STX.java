package Blockchainj.Blockchain.UtxoSet.UTXOS;


import Blockchainj.Bitcoin.TXI;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Util.SHA256HASH;

import java.io.PrintStream;
import java.util.*;

/**
 * STX - Spent Transaction
 * A set of "spent" transaction inputs that have in common the previous TXID field.
 *
 * No serialization.
 * Keeps TXIs in insertion order.
 */

public class STX implements Comparable<STX> {
    /* Previous TXID */
    private final SHA256HASH prevTxid;

    /* Metadata - Might not have if TXIs come from multiple blocks. */
    private final boolean hasMetadata;
    private final int height;
    private final SHA256HASH blockhash;

    /* Set of spent TXIs. Use Map instread of Set, because Map has newer versions in java and
     * Integer keys are efficient. Still, no duplicates allowed.
     * Key is prevOutIndex<Integer>, values is stxi<TXI>.*/
    private final LinkedHashMap<Integer, TXI> stxis;


    /* Private constructor. Assumes valid input. */
    private STX(SHA256HASH prevTxid, TXI[] stxis, SHA256HASH blockhash, int height,
                boolean hasMetadata) {
        if(!hasMetadata) {
            this.hasMetadata = false;
            this.blockhash = SHA256HASH.ZERO_SHA256HASH;
            this.height = ProtocolParams.UNDEFINED_HEIGHT;
        }
        else {
            this.hasMetadata = true;
            this.blockhash = blockhash;
            this.height = height;
        }

        this.prevTxid = prevTxid;

        /* Init map. Initial capacity is 16 and loadFactor is 0.75%.
         * Assumes all stxis are valid and no duplicate exists. */
        this.stxis = new LinkedHashMap<>();
        if(stxis!=null) {
            for(int i=0; i<stxis.length; i++) {
                this.stxis.put(stxis[i].getPrevOutIndex(), stxis[i]);
            }
        }
    }


    /* Constructor for TXIs that come from mulitple blocks (no metadata).
     * Assumes valid stxis but can be null. */
    public STX(SHA256HASH prevTxid, TXI[] stxis) {
        this(prevTxid, stxis, null, 0, false);
    }

    /* Constructor for TXIs that come from mulitple blocks (no metadata). */
    public STX(SHA256HASH prevTxid) {
        this(prevTxid, null, null, 0, false);
    }


    /* Constructor for TXIs that come from the same block (with metadata).
     * Assumes valid stxis but can be null. */
    public STX(SHA256HASH prevTxid, TXI[] stxis, SHA256HASH blockhash, int height) {
        this(prevTxid, stxis, blockhash, height, true);
    }

    /* Constructor for TXIs that come from the same block (with metadata). */
    public STX(SHA256HASH prevTxid, SHA256HASH blockhash, int height) {
        this(prevTxid, null, blockhash, height, true);
    }


    /* Get methods */
    public SHA256HASH getPrevTxid() { return prevTxid; }

    public boolean hasMetadata() { return hasMetadata; }

    public SHA256HASH getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    /* Get stxis iterator. Follows insertion-order. */
    public Iterator<TXI> getTxiIterator() { return stxis.values().iterator(); }

    /* Get stxis count */
    public int getStxisCount() { return stxis.size(); }

    /* True if empty */
    public boolean isEmpty() { return stxis.isEmpty(); }


    /* Add stxi. Returns false if already exists, else adds to set.
     * Not safe add method. Does not check if input belongs to this STX. Faster add. */
    public boolean addStxiValidInput(TXI stxi) throws IllegalArgumentException {
        /* Cannot put new stxi without first look if it already exist. If old stxi is removed
           and added again, the order insertion order will be lost which is not good. */
        if(stxis.containsKey(stxi.getPrevOutIndex())) {
            return false;
        }

        TXI oldTxi = stxis.put(stxi.getPrevOutIndex(), stxi);
        if(oldTxi != null) {
            throw new RuntimeException("Expected null but got " + oldTxi.toString());
        }
        else {
            return true;
        }
    }


    /* Add stxi. Returns false if already exists, else adds to set. */
    @Deprecated //not really but remember not to use
    public boolean addStxi(TXI stxi) throws IllegalArgumentException {
        if(!stxi.getPrevTxid().equals(prevTxid)) {
            throw new IllegalArgumentException("PrevTxid's must be match");
        }
        return addStxiValidInput(stxi);
    }


    /* Remove stxi. Returns true if found and removed */
    public boolean removeStxi(int prevOutIndex) {
        TXI oldTxi = stxis.remove(prevOutIndex);
        return oldTxi == null;
    }


    /* Contains stxi. Return true if found. */
    public boolean contains(int prevOutIndex) {
        return stxis.containsKey(prevOutIndex);
    }


//    /* Used to find TXIs in Set */
//    private class TXISHADOW {
//        int prevOutIndex;
//        private TXISHADOW(int prevOutIndex) {
//            this.prevOutIndex = prevOutIndex;
//        }
//        @Override
//        public boolean equals(Object obj) {
//            return obj.hashCode() == hashCode();
//        }
//        @Override
//        public int hashCode() {
//            return prevOutIndex;
//        }
//    }


    @Override
    public String toString() {
        return "STX_PrevTxid: " + prevTxid.toString() + ", STX_Blockhash: " + blockhash +
                ", STX_Height: " + height;
    }


    /* compareTo method. Compare TXIDs. */
    @Override
    public int compareTo(STX o) {
        return prevTxid.compareTo(o.prevTxid);
    }


    /* DEBUG ONLY */
    public void print(PrintStream printStream, boolean doStxis) {
        printStream.println("PrevTxid: " + prevTxid.toString());
        if(doStxis) {
            Iterator<TXI> it = getTxiIterator();
            while(it.hasNext()) {
                it.next().print(printStream);
            }
        }
    }


//    /* DEBIG/TEST */
//    public static void main(String[] args) {
//        int startAt = 400000; //Integer.parseInt(args[0]);
//        int endAt = 400000; //Integer.parseInt(args[1]);
//        SHA256HASH mainPrevTxid = new SHA256HASH(
//                "1f2c97378a491e469fccd9c8764edb5f07cfe7a03c7ace337d7d047520ebd657");
//        STX stx = new STX(mainPrevTxid);
//
//        try {
//            SimpleBlockBuffer bb = new SimpleBlockBuffer(startAt, endAt, new RPCconnection());
//            for(;;) {
//                Block block;
//                try {
//                    block = bb.getNextBlock();
//                } catch (EndOfRangeException e) {
//                    break;
//                }
//                Iterator<Transaction> it = block.getTxIterator();
//                while(it.hasNext()) {
//                    Transaction t = it.next();
//                    TXI[] txis = t.getTXIs();
//                    for(int i=0; i<txis.length; i++) {
//                        if(txis[i].prevTxid.equals(mainPrevTxid)) {
//                            //txis[i].print();
//                            System.out.println(stx.addStxiValidInput(txis[i]));
//                            stx.print(true);
//                            System.out.println("\n");
//                        }
//                    }
//                }
//            }
//
//            List<Integer> prevOutIndexList = new ArrayList<>();
//            Iterator<TXI> it = stx.getTxiIterator();
//            while(it.hasNext()) {
//                prevOutIndexList.add(it.next().getPrevOutIndex());
//            }
//
//            Iterator<Integer> it2 = prevOutIndexList.iterator();
//            while(it2.hasNext()) {
//                int prevOutIndexInt = it2.next();
//                System.out.println(stx.contains(prevOutIndexInt));
//                System.out.println(stx.removeStxi(prevOutIndexInt));
//                stx.print(true);
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }
}
