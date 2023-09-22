package Blockchainj.Blockchain;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Util.SHA256HASH;

import java.util.Arrays;

/**
 * ProtoclParams - Prototype Protocol Parameters
 *
 * Shards, messages, serialization
 *
 */

public class ProtocolParams {
    /* Height size - int32 */
    public static final int HEIGHT_SIZE = 4;

    /* Undefined height constant */
    public static final int UNDEFINED_HEIGHT = -1;

    /* A marker that can be put before a NON-ZERO compactSizeUInt. - 1 byte*/
    public static final byte MARKER = 0x00;

    /* Boolean value for coinbase - 1 byte */
    public static final byte ISCOINBASE_FALSE = 0x00;
    public static final byte ISCOINBASE_TRUE = 0x01;

    /* Size for boolean value */
    public static final int BOOLEAN_SIZE = 1;

    /* Byte boolean - 1byte */
    public static final byte BYTE_FALSE = 0x00;
    public static final byte BYTE_TRUE= 0x01;




    /** Message **/
    /* Message: Request type size - char[12] ASCII */
    public static final int REQUEST_TYPE_SIZE = 12;

    /* Message: Request type list of element count size - uint32 */
    public static final int REQUEST_TYPE_LIST_COUNT_SIZE = 4;

    /* Message: Request type list of element length min and max */
    public static final int REQUEST_TYPE_LIST_MIN_COUNT = 0;
    public static final int REQUEST_TYPE_LIST_MAX_COUNT = 10;

    /* Message: GetCustom message command bytes */
    public static final byte[] MESSAGE_CMD_GETCUSTOM = {
            (byte)0x67, (byte)0x65, (byte)0x74, (byte)0x63, (byte)0x75, (byte)0x73, (byte)0x74,
            (byte)0x6f, (byte)0x6d, (byte)0x00, (byte)0x00, (byte)0x00};

    /* Message: DataCustom message command bytes */
    public static final byte[] MESSAGE_CMD_DATACUSTOM = {
            (byte)0x64, (byte)0x61, (byte)0x74, (byte)0x61, (byte)0x63, (byte)0x75, (byte)0x73,
            (byte)0x74, (byte)0x6f, (byte)0x6d, (byte)0x00, (byte)0x00};


    /* Message: Request/Response type bytes, "bestheight" */
    public static final byte[] MESSAGE_TYPE_BESTHEIGHT = {
            (byte)0x62, (byte)0x65, (byte)0x73, (byte)0x74, (byte)0x68, (byte)0x65, (byte)0x69,
            (byte)0x67, (byte)0x68, (byte)0x74, (byte)0x00, (byte)0x00};

    /* Message: Request/Response type bytes, "bestblkhash" */
    public static final byte[] MESSAGE_TYPE_BESTBLKHASH = {
            (byte)0x62, (byte)0x65, (byte)0x73, (byte)0x74, (byte)0x62, (byte)0x6c, (byte)0x6b,
            (byte)0x68, (byte)0x61, (byte)0x73, (byte)0x68, (byte)0x00};

    /* Message: Request/Response type bytes, "blockhash" */
    public static final byte[] MESSAGE_TYPE_BLOCKHASH = {
            (byte)0x62, (byte)0x6c, (byte)0x6f, (byte)0x63, (byte)0x6b, (byte)0x68, (byte)0x61,
            (byte)0x73, (byte)0x68, (byte)0x00, (byte)0x00, (byte)0x00};

    /* Message: Request/Response type bytes, "bestmrklroot" */
    public static final byte[] MESSAGE_TYPE_BESTMRKLROOT = {
            (byte)0x62, (byte)0x65, (byte)0x73, (byte)0x74, (byte)0x6d, (byte)0x72, (byte)0x6b,
            (byte)0x6c, (byte)0x72, (byte)0x6f, (byte)0x6f, (byte)0x74};

    /* Message: Request/Response type bytes, "merkleroot" */
    public static final byte[] MESSAGE_TYPE_MERKLEROOT = {
            (byte)0x6d, (byte)0x65, (byte)0x72, (byte)0x6b, (byte)0x6c, (byte)0x65, (byte)0x72,
            (byte)0x6f, (byte)0x6f, (byte)0x74, (byte)0x00, (byte)0x00};

    /* Message: Request/Response type bytes, "bestmrkltree" */
    public static final byte[] MESSAGE_TYPE_BESTMRKLTREE = {
            (byte)0x62, (byte)0x65, (byte)0x73, (byte)0x74, (byte)0x6d, (byte)0x72, (byte)0x6b,
            (byte)0x6c, (byte)0x74, (byte)0x72, (byte)0x65, (byte)0x65};

    /* Message: Request/Response type bytes, "bestshardnum" */
    public static final byte[] MESSAGE_TYPE_BESTSHARDNUM = {
            (byte)0x62, (byte)0x65, (byte)0x73, (byte)0x74, (byte)0x73, (byte)0x68, (byte)0x61,
            (byte)0x72, (byte)0x64, (byte)0x6e, (byte)0x75, (byte)0x6d};

    /* Message: Request/Response type bytes, "shardnum" */
    public static final byte[] MESSAGE_TYPE_SHARDNUM = {
            (byte)0x73, (byte)0x68, (byte)0x61, (byte)0x72, (byte)0x64, (byte)0x6e, (byte)0x75,
            (byte)0x6d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

    /* Message: Request/Response type bytes, "bestshard" */
    public static final byte[] MESSAGE_TYPE_BESTSHARD = {
            (byte)0x62, (byte)0x65, (byte)0x73, (byte)0x74, (byte)0x73, (byte)0x68, (byte)0x61,
            (byte)0x72, (byte)0x64, (byte)0x00, (byte)0x00, (byte)0x00};

    /* Messsage: Request type array */
    public static final byte[][] MESSAGE_TYPES = {
            MESSAGE_TYPE_BESTHEIGHT,
            MESSAGE_TYPE_BESTBLKHASH,
            MESSAGE_TYPE_BLOCKHASH,
            MESSAGE_TYPE_BESTMRKLROOT,
            MESSAGE_TYPE_MERKLEROOT,
            MESSAGE_TYPE_BESTMRKLTREE,
            MESSAGE_TYPE_BESTSHARDNUM,
            MESSAGE_TYPE_SHARDNUM,
            MESSAGE_TYPE_BESTSHARD
    };

    /* Message: Request type cases for GetCustom */
    public static final int[] MESSAGE_TYPE_GET_CASE = {
            1, // MESSAGE_TYPE_BESTHEIGHT, case 1, no list
            1, // MESSAGE_TYPE_BESTBLKHASH, case 1, no list
            2, // MESSAGE_TYPE_BLOCKHASH, case 2, height list
            1, // MESSAGE_TYPE_BESTMRKLROOT, case 1, no list
            2, // MESSAGE_TYPE_MERKLEROOT, case 2, height list
            1, // MESSAGE_TYPE_BESTMRKLTREE, case 1, no list
            1, // MESSAGE_TYPE_BESTSHARDNUM, case 1, no list
            2, // MESSAGE_TYPE_SHARDNUM, case 2, height list
            3, // MESSAGE_TYPE_BESTSHARD, case 3, index list
    };

    /* Message: Request type cases for DataCustom */
    public static final int[] MESSAGE_TYPE_DATA_CASE = {
            1, // MESSAGE_TYPE_BESTHEIGHT, case 1, height list
            3, // MESSAGE_TYPE_BESTBLKHASH, case 3, SHA256HASH list
            3, // MESSAGE_TYPE_BLOCKHASH, case 3, SHA256HASH list
            3, // MESSAGE_TYPE_BESTMRKLROOT, case 3, SHA256HASH list
            3, // MESSAGE_TYPE_MERKLEROOT, case 3, SHA256HASH list
            4, // MESSAGE_TYPE_BESTMRKLTREE, case 4, MerkleTree list
            2, // MESSAGE_TYPE_BESTSHARDNUM, case 2, index list
            2, // MESSAGE_TYPE_SHARDNUM, case 2, index list
            5, // MESSAGE_TYPE_BESTSHARD, case 5, ShardList
    };

    /* Message: Request type min/max list count for DataCustom */
    public static final int[][] MESSAGE_TYPE_GET_MIN_MAX_LIST_COUNT = {
            {0,0}, // MESSAGE_TYPE_BESTHEIGHT
            {0,0}, // MESSAGE_TYPE_BESTBLKHASH
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_BLOCKHASH
            {0,0}, // MESSAGE_TYPE_BESTMRKLROOT
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_MERKLEROOT
            {0,0}, // MESSAGE_TYPE_BESTMRKLTREE
            {0,0}, // MESSAGE_TYPE_BESTSHARDNUM
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_SHARDNUM
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_BESTSHARD
    };

    /* Message: Request type min/max list count for DataCustom */
    public static final int[][] MESSAGE_TYPE_DATA_MIN_MAX_LIST_COUNT = {
            {1,1}, // MESSAGE_TYPE_BESTHEIGHT
            {1,1}, // MESSAGE_TYPE_BESTBLKHASH
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_BLOCKHASH
            {1,1}, // MESSAGE_TYPE_BESTMRKLROOT
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_MERKLEROOT
            {1,1}, // MESSAGE_TYPE_BESTMRKLTREE
            {1,1}, // MESSAGE_TYPE_BESTSHARDNUM
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_SHARDNUM
            {REQUEST_TYPE_LIST_MIN_COUNT, REQUEST_TYPE_LIST_MAX_COUNT}, // MESSAGE_TYPE_BESTSHARD
    };


    /** Shard and Merkle Tree **/
    /* shardNum (number of shards) and shardIndex size - int32
       >0 and power of 2 within MIN MAX RANGE */
    public static final int SHARD_NUM_SIZE = BitcoinParams.INT32_SIZE;

    /* Merkle tree numLeaves - int32
       >0 and power of 2 within MIN MAX RANGE */
    public static final int LEAVES_NUM_SIZE = BitcoinParams.INT32_SIZE;

    /* defaut number of shards */
    public static final int SHARD_NUM = 8192;

    /* Undefined shard index */
    public static final int UNDEFINED_SHARD_INDEX = -1;

    /* Undefined shard number */
    public static final int UNDEFINED_SHARD_NUM = -1;

    /* min and max number of shards */
    public static final int MIN_SHARD_NUM = 1;
    public static final int MAX_SHARD_NUM = 0x40000000; //2^30

    /* min and max number of bits for txid lookup */
    public static final int MIN_TXID_BITS = 0;
    public static final int MAX_TXID_BITS = 30;


    /** Shard and Merkle Tree Methods **/
    /* Check if number of shard is valid */
    public static boolean isValidShardNum(int shardNum) {
        try {
            validateShardNum(shardNum);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    /* Check if number of leaves is valid for merkle tree */
    public static void validateNumOfLeaves(int numLeaves) throws IllegalArgumentException {
        validateShardNum(numLeaves);
    }


    /* Check if number of shard is valid. Same as isValidShardNum */
    public static void validateShardNum(int shardNum) throws IllegalArgumentException {
        /* Check range */
        if ( (shardNum < MIN_SHARD_NUM) ||
                (shardNum > MAX_SHARD_NUM) ) {
            throw new IllegalArgumentException("Number of shards/leaves should be >=" +
                    MIN_SHARD_NUM + " end <= " + MAX_SHARD_NUM);
        }

        /* Check power of 2 */
        if ( !isPowerOf2(shardNum) ) {
            throw new IllegalArgumentException("Number of shards/leaves must be power of 2.");
        }
    }


    /* Returns true if int is >0 and power of 2. */
    public static boolean isPowerOf2(int input) {
        final int INT_BITS = 32;
        final int LOG2_BITS = INT_BITS - 1;

        if(input <= 0) {
            return false;
        } else {
            int bitsNum = LOG2_BITS - Integer.numberOfLeadingZeros(input);
            return ((1 << bitsNum) == input);
        }
    }


    /* Returns the number of bits needed to represent input as an unsigned integer
     * IF input is >0 else returns 0. */
    public static int getBitsNeeded(int input) throws IllegalArgumentException {
        final int INT_BITS = 32;
        final int LOG2_BITS = INT_BITS - 1;

        if(input <= 0) {
            throw new IllegalArgumentException("Input must be >0.");
        } else {
            return LOG2_BITS - Integer.numberOfLeadingZeros(input);
        }
    }


    /* Returns ceil(log2(input)), input must be >0 and integer */
    public static int ceilLog2(int input) throws IllegalArgumentException {
        final int INT_BITS = 32;
        final int LOG2_BITS = INT_BITS - 1;

        if(input <= 0) {
            throw new IllegalArgumentException("Input must be >0.");
        } else {
            return LOG2_BITS - Integer.numberOfLeadingZeros(input);
        }
    }


    /* Given a number of shards and a SHA256 hash, returns the shard index.
     * Look Shard for more info. */
    public static int calcShardIndex(int shardNum, SHA256HASH txid)
            throws IllegalArgumentException {
        final int INT_BITS = 32;
        final int LOG2_BITS = INT_BITS - 1;
        final int INT_BYTES = 4;

        /* Check shardNum */
        validateShardNum(shardNum);

        /* get bits needed for indexing shards from txid */
        int bitsNum = LOG2_BITS - Integer.numberOfLeadingZeros(shardNum);

        // this is not needed
        //if(bitsNum > MAX_TXID_BITS) {
        //    throw new IllegalArgumentException("shardNum must be <=" + MAX_SHARD_NUM);
        //}

        /* Make integer with the first 32 bits (4 bytes) of the Txid.
         * Txid is in Little Endian(LE), so reading as LE is the same as reversing bytes and
         * reading it as BE. */
        long shardIndex = txid.getLastUINT32LE();

        /* Trim extra bits from the end but unsigned right shift.
         * There are 2 extra bits from reading a int32, while max bits are 30.
         * There are another MAX_BITS-(number of bits calculated from log2). */
        //shardIndex = shardIndex >>> ( (MAX_BITS-bitsNum) + EXTRA_BITS );
        shardIndex = shardIndex >>> ( INT_BITS - bitsNum );

        return (int)shardIndex;
    }


    public static int[] getShardIndicesThatContainValidUtxs(
            int oldShardNum, int newShardNum, int newShardIndex)
            throws IllegalArgumentException {
        /* Check input */
        validateShardNum(oldShardNum);
        validateShardNum(newShardNum);
        if(newShardIndex < 0 || newShardIndex >= newShardNum) {
            throw new IllegalArgumentException(new IndexOutOfBoundsException());
        }

        if(oldShardNum == newShardNum) {
            /* Same shard numbers means same shard index */
            int[] res = new int[1];
            res[0] = newShardIndex;
            return res;
        }
        else if(oldShardNum < newShardNum) {
            /* smaller oldShardNum means all UTXs will be available in one oldShardNum shard */
            int modifier = newShardNum / oldShardNum;
            int oldShardIndex = newShardIndex/modifier;
            int[] res = new int[1];
            res[0] = oldShardIndex;
            return res;
        }
        else {
            /* greater oldShardNum means all UTXs will be in several oldShardNum shards */
            int modifier = oldShardNum / newShardNum;
            int firstOldShardIndex = newShardIndex * modifier;
            int res[] = new int[modifier];
            for(int i=0; i<modifier; i++) {
                res[i] = firstOldShardIndex + i;
            }
            return res;
        }
    }



    /** Message Methods */
    /* Compare two request types for equality */
    public static boolean isRequestTypeEqual(byte[] reqType1, byte[] reqType2) {
        if( (reqType1.length != REQUEST_TYPE_SIZE) || (reqType2.length != REQUEST_TYPE_SIZE) ) {
            return false;
        }
        return Arrays.equals(reqType1, reqType2);
    }


    /* Returns the index to the request types array for the given requestType. */
    public static int calcRequestTypeIndex(byte[] requestType) throws IllegalArgumentException {
        for(int i=0; i<MESSAGE_TYPES.length; i++) {
            if (isRequestTypeEqual(requestType, MESSAGE_TYPES[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Request type not found.");
    }


    /* GetCustom messages methods. */
    /* Returns the GetCustom case number for the request type given. */
    public static int calcGetCustomRequestTypeCase(int typeIndex)
            throws IllegalArgumentException {
        try {
            return MESSAGE_TYPE_GET_CASE[typeIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* Returns min element list count for the GetCustom message given the request type. */
    public static int calcGetCustomElementListMinCount(int typeIndex)
            throws IllegalArgumentException {
        try {
            return MESSAGE_TYPE_GET_MIN_MAX_LIST_COUNT[typeIndex][0];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* Returns max element list count for the GetCustom message given the request type. */
    public static int calcGetCustomElementListMaxCount(int typeIndex)
            throws IllegalArgumentException {
        try {
            return MESSAGE_TYPE_GET_MIN_MAX_LIST_COUNT[typeIndex][1];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* GetCustom messages methods. */
    /* Returns the GetCustom case number for the request type given. */
    public static int calcDataCustomRequestTypeCase(int typeIndex)
            throws IllegalArgumentException {
        try {
            return MESSAGE_TYPE_DATA_CASE[typeIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* Returns min element list count for the GetCustom message given the request type. */
    public static int calcDataCustomElementListMinCount(int typeIndex)
            throws IllegalArgumentException {
        try {
            return MESSAGE_TYPE_DATA_MIN_MAX_LIST_COUNT[typeIndex][0];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* Returns max element list count for the GetCustom message given the request type. */
    public static int calcDataCustomElementListMaxCount(int typeIndex)
            throws IllegalArgumentException {
        try {
            return MESSAGE_TYPE_DATA_MIN_MAX_LIST_COUNT[typeIndex][1];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }




    //    public static void main(String[] args){
//        int oldShardNum = 64;
//        int newShardNum = 16;
//        for(int index=0; index<newShardNum; index++){
//            int res[] = getShardIndicesThatContainValidUtxs(oldShardNum, newShardNum, index);
//
//            System.out.print("index: " + index + ":::");
//            for(int i =0; i<res.length; i++) {
//                System.out.print(res[i] + ", ");
//            }
//            System.out.println();
//        }
//    }
}
