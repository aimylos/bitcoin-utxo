package Blockchainj.Util;


import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Blockchain.ProtocolParams;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;


/**
 * Merkle Tree
 *
 * Full binary tree.
 * The number of leafs must be power of 2.
 * The hashing algorithm used is double SHA256.
 *
 * Use:
 * First update the leaves.
 * Then rehash tree.
 *
 * Serialization, only leaves:
 * <number of leaves, int32><hash_leaf_0, 32bytes>...<hash_leaf_n-1, 32bytes>
 *
 *
 * //<root, 32bytes><hash_i, 32bytes>...<hash_(2*(number of leaves)-1), 32bytes>
 * //root is 0, parent i, left child 2*i+1, right child 2*i+2
 *
 */

public class MerkleTree {
    /* number of leafs */
    private final int numLeaves;

    /* tree height */
    private final int height;

    /* number of nodes, including leaves */
    private final int numNodes;

    /* serialized size */
    private final long serializedSize;

    /* Tree  */
    private final SHA256HASH[] tree;

    /* Dirty table */
    private final boolean[] dirtyTable;

    /* is read only */
    private boolean isReadOnly = false;


    /* Constructor */
    public MerkleTree(int numLeaves) throws IllegalArgumentException {
        ProtocolParams.validateNumOfLeaves(numLeaves);
        this.numLeaves = numLeaves;

        /* tree height */
        height = ProtocolParams.ceilLog2(numLeaves);

        /* tree nodes including leaves */
        numNodes = 2*numLeaves - 1;

        /* serialized size */
        serializedSize = getSerializedSize(numLeaves);

        /* tree */
        tree = new SHA256HASH[numNodes];

        /* Dirty table */
        dirtyTable = new boolean[numNodes];

        /* build zero hash merkle tree */
        for(int i=0; i<numLeaves; i++) {
            updateLeafHash(i, SHA256HASH.ZERO_SHA256HASH);
        }
        rehashTree();
    }


    /* Shallow copy constructor */
    private MerkleTree(MerkleTree merkleTree) {
        numLeaves = merkleTree.numLeaves;
        height = merkleTree.height;
        numNodes = merkleTree.numNodes;
        serializedSize = merkleTree.serializedSize;
        tree = merkleTree.tree;
        dirtyTable = merkleTree.dirtyTable;
        isReadOnly = merkleTree.isReadOnly;
    }


    /* Update a leaf by index */
    public void updateLeafHash(int leafIndex, SHA256HASH hash)
            throws ArrayIndexOutOfBoundsException {
        if(isReadOnly) {
            throw new IllegalStateException("Read only access not allowed.");
        }

        if( (leafIndex<0) || (leafIndex>=numLeaves)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        int realIndex = (numNodes - numLeaves) + leafIndex;

        tree[realIndex] = hash;
        dirtyTable[realIndex] = true;
    }


    /* Rebuild the tree from the leaves to the root. */
    public void rehashTree() {
        if(isReadOnly) {
            throw new IllegalStateException("Read only access not allowed.");
        }

        /* Start from parents of leaves and work the way up. */
        for(int i=numNodes-numLeaves-1; i>=0; i--) {
            int left = 2*i+1;
            int right = 2*i+2;
            if(dirtyTable[left] || dirtyTable[right]) {
                tree[i] = SHA256HASH.concatAndDoubleSHA256(tree[left], tree[right]);
                //tree[i] = SHA256HASH.doDoubleSHA256(SHA256HASH.concat(tree[left], tree[right]));
                dirtyTable[i] = true;
                dirtyTable[left] = false;
                dirtyTable[right] = false;
            }
        }
        dirtyTable[0] = false; //root doesn't matter anyway
    }


    /* Returns merkle tree root. */
    public SHA256HASH getRoot() {
        return tree[0];
    }


    /* Returns leaf at index */
    public SHA256HASH getLeafHash(int leafIndex) throws ArrayIndexOutOfBoundsException {
        if((leafIndex < 0) || (leafIndex>=numLeaves)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        return tree[(numNodes-numLeaves) + leafIndex];
    }


    /* Returns shallow copy of this object as read only */
    public MerkleTree getReadOnly() {
        MerkleTree readonly = new MerkleTree(this);
        readonly.isReadOnly = true;
        return readonly;
    }


    /* Returns serialization bytes */
    public long getSerializedSize() { return serializedSize; }


    /* Returns serialized size for given number if leaves */
    public static long getSerializedSize(int numLeaves) throws IllegalArgumentException {
        ProtocolParams.validateNumOfLeaves(numLeaves);

        return ((long) BitcoinParams.INT32_SIZE)
                + ((long)numLeaves * (long)SHA256HASH.HASH_SIZE );
    }


    /* Custom serialization. */
    public void serialize(byte[] dest, int offset) {
        /* write number of leaves */
        BitcoinParams.INT32ToByteArray(numLeaves, dest, offset);
        offset += ProtocolParams.LEAVES_NUM_SIZE;

        /* write leaves */
        for(int i=0; i<numLeaves; i++) {
            getLeafHash(i).serialize(dest, offset);
            offset += SHA256HASH.SERIALIZED_SIZE;
        }
    }

    /* Custom serialization */
    public void serialize(OutputStream outputStream) throws IOException{
        /* write number of leaves */
        outputStream.write(BitcoinParams.getINT32(numLeaves));

        /* write leaves */
        for(int i=0; i<numLeaves; i++) {
            getLeafHash(i).serialize(outputStream);
        }
    }


    /* Custom deserialization */
    public static MerkleTree deserialize(byte[] src, int offset) throws IllegalArgumentException {
        /* read number of leaves */
        int numLeaves = BitcoinParams.readINT32(src, offset);
        offset += 4;

        /* make new merkle tree */
        MerkleTree merkleTree = new MerkleTree(numLeaves);

        /* update leaves */
        for(int i=0; i<numLeaves; i++) {
            merkleTree.updateLeafHash(i, SHA256HASH.deserialize(src, offset));
            offset += SHA256HASH.SERIALIZED_SIZE;
        }

        /* rehash tree */
        merkleTree.rehashTree();

        return merkleTree;
    }



    public SHA256HASH[] getMissingHashes(boolean[] availableHashesIndices) {
        //TODO
        return null;
    }



    public static int getCountForMissingHashes(int[] availableHashesIndices, int numLeaves)
            throws  IllegalArgumentException {
        /* Check inputs */
        ProtocolParams.validateNumOfLeaves(numLeaves);

        /* init tree height and nodes number */
        final int numNodes = 2*numLeaves - 1;
        final int treeHeight = ProtocolParams.ceilLog2(numLeaves);

        /* init leaves */
        LinkedHashMap<Integer, Integer> level = new LinkedHashMap<>();
        for(int i=0; i<availableHashesIndices.length; i++) {
            int index = (numNodes-numLeaves) + availableHashesIndices[i];
            int parentIndex = (index-1)/2;
            level.put(parentIndex, 1 + ( (level.get(parentIndex)==null)?0:1 ) );
        }

        /* init missing hashes count */
        int neededHashesCount = 0;

        /* For all levels */
        for(int i=0; i<treeHeight; i++) {
            LinkedHashMap<Integer, Integer> newLevel = new LinkedHashMap<>();
            Iterator<Integer> it = level.keySet().iterator();
            while(it.hasNext()) {
                int index = it.next();
                int val = level.get(index);

                if(val == 1) {
                    neededHashesCount++;
                }

                int parentIndex = (index-1)/2;
                newLevel.put(parentIndex, 1 + ( (newLevel.get(parentIndex)==null)?0:1 ) );
            }

            level = newLevel;
        }

        return neededHashesCount;
    }


    public static long getSerializedSizeForMissingHashes(int[] availableHashesIndices,
                                                         int numLeaves)
            throws  IllegalArgumentException {
        return (long)getCountForMissingHashes(availableHashesIndices, numLeaves)
                * (long)SHA256HASH.HASH_SIZE;
    }




    /* Print */
    public void print(PrintStream printStream, boolean headerOnly, boolean originalByteOrder) {
        printStream.println("Number of leaves: " + numLeaves);
        printStream.println("Number of nodes including leaves: " + numNodes);
        printStream.println("Tree height: " + height);
        printStream.println("Merkle tree root: " + getRoot().toString());

        if(!headerOnly) {
            int i = 0;
            int j = 0;
            while (i < numNodes) {
                printStream.println("Height: " + ProtocolParams.ceilLog2(j + 1));
                int c = 0;
                while (j <= 2 * i) {
                    printStream.format("%1$4s. ", c);
                    if (originalByteOrder) {
                        printStream.println(tree[j].getHashStringOriginal());
                    } else {
                        printStream.println(tree[j].getHashString());
                    }
                    j++;
                    c++;
                }
                printStream.println("\n");
                i = j;
            }
        }
    }


//    /* TEST/DEBUG */
//    public static void main(String args[]) {
//        int[] av = {0, 3, 4, 7};
//        System.out.println(MerkleTree.getCountForMissingHashes(av, 8));
//        //System.out.println(MerkleTree.getSerializedSizeForMissingHashes(av, 8));
//    }
}




//    public static int getCountForMissingHashes(int[] availableHashesIndices, int numLeaves)
//            throws  IllegalArgumentException {
//        /* Check inputs */
//        ProtocolParams.validateNumOfLeaves(numLeaves);
//
//        /* tree nodes including leaves */
//        final int numNodes = 2*numLeaves - 1;
//        boolean[] nodes = new boolean[numNodes];
//        Arrays.fill(nodes, false);
//
//        /* Init leaves */
//        for(int i=0; i<availableHashesIndices.length; i++) {
//            int index = availableHashesIndices[i];
//
//            if(index < 0 || index>= numLeaves) {
//                throw new IllegalArgumentException();
//            }
//
//            nodes[(numNodes-numLeaves) + index] = true;
//        }
//
//        /* init missing hashes count */
//        int neededHashesCount = 0;
//
//        /* Start from parents of leaves and work the way up */
//        for(int i=numNodes-numLeaves-1; i>=0; i--) {
//            int left = 2*i+1;
//            int right = 2*i+2;
//
//            /* if both children missing then let parrent missing */
//            if(!nodes[left] && !nodes[right]) {
//                continue;
//            }
//            /* if both children are available then mark parent as available */
//            if(nodes[left] && nodes[right]) {
//                nodes[i] = true;
//            }
//            /* if one child missing then mark parent available and increase counter by 1 */
//            else {
//                neededHashesCount++;
//                nodes[i] = true;
//            }
//        }
//
//        return neededHashesCount;
//    }



//    public static int getCountForMissingHashes(int[] availableHashesIndices, int numLeaves)
//            throws  IllegalArgumentException {
//        /* Check inputs */
//        ProtocolParams.validateNumOfLeaves(numLeaves);
//
//        /* tree nodes including leaves */
//        HashSet<Integer> availableNodes = new HashSet<>();
//        final int numNodes = 2*numLeaves - 1;
//
//        /* Init leaves */
//        for(int i=0; i<availableHashesIndices.length; i++) {
//            int index = availableHashesIndices[i];
//
//            if(index < 0 || index>= numLeaves) {
//                throw new IllegalArgumentException();
//            }
//
//            availableNodes.add((numNodes-numLeaves) + index);
//        }
//
//        /* init missing hashes count */
//        int neededHashesCount = 0;
//
//        /* Start from parents of leaves and work the way up */
//        for(int i=numNodes-numLeaves-1; i>=0; i--) {
//            int left = 2*i+1;
//            int right = 2*i+2;
//            boolean hasLeft = availableNodes.contains(left);
//            boolean hasRight = availableNodes.contains(right);
//
//            /* if both children missing then let parrent missing */
//            if(!hasLeft && !hasRight) {
//                continue;
//            }
//            /* if both children are available then mark parent as available */
//            if(hasLeft && hasRight) {
//                availableNodes.remove(left);
//                availableNodes.remove(right);
//                availableNodes.add(i);
//            }
//            /* if one child missing then mark parent available and increase counter by 1 */
//            else {
//                neededHashesCount++;
//                if(hasLeft) {
//                    availableNodes.remove(left);
//                } else {
//                    availableNodes.remove(right);
//                }
//                availableNodes.add(i);
//            }
//        }
//
//        return neededHashesCount;
//    }