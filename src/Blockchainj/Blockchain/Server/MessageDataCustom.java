package Blockchainj.Blockchain.Server;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Util.MerkleTree;
import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardSortedMapUtxs;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 *  MessageDataCustom - Custom made data message.
 *
 *  Uses the same request types as MessageGetCustom, but has the appropriate response list.
 *
 *  Command: "datacustom"
 *  Payload:
 *      Request type 12 bytes
 *      Element count uint32 4 bytes
 *      List of elements. Min:0, Max:10
 *
 *  Request types along with their List:
 *      <type, ascii, 12 bytes, right null padded>, <List, type>
 *
 *      "bestheight", 1 element, height <int32, 4 bytes>
 *
 *      "bestblkhash", 1 element, blockhash <char[32], 32 bytes>
 *      "blockhash", list of blockhashes, <char[32], 32 bytes>
 *
 *      "bestmrklroot", 1 element, hash, <char[32], 32 bytes>
 *      "merkleroot", list of hashes, <char[32], 32 bytes>
 *
 *      "bestmrkltree", 1 element, merkle tree
 *          <hash count, uint32, 4bytes><hash, char[32], 32bytes>...<hash, char[32], 32bytes>
 *
 *      "bestshardnum", 1 element, count <int32, 4bytes>
 *      "shardnum", list of counts, <int32, 4 bytes>
 *
 *      "bestshard", list of shards, <undefined>
 */

public class MessageDataCustom extends Message {
    /* DataCustom command */
    private static final byte[] CMD = ProtocolParams.MESSAGE_CMD_DATACUSTOM;

    /* Request type bytes - char[12]*/
    private final byte[] requestType;

    /* Request index to request types */
    private final int requestTypeIndex;

    /* Request type case.
     * 1: Height list - Integer
     * 2: Index list - Integer
     * 3: Hash list - SHA256HASH
     * 4: Merkle tree list - MerkleTree
     * 5: Shard list - Shard */
    private final int requestTypeCase;

    /* Element list min/max count */
    private final int elementListMinCount;
    private final int elementListMaxCount;

    /* List element count - uint32 */
    private final int elementListCount;

    /* List of elements.
     * Possible types: Integer, SHA256HASH, MerkleTree, Shard */
    private final ArrayList<?> elementList;


    /* Constructor. Parses payload. Does not copy input all input. */
    public MessageDataCustom(Message message) throws IllegalArgumentException {
        super(message.magic, CMD, message.payload);
        byte[] payload = message.payload;

        try {
            int offset = 0;

            /* Read request type */
            requestType = Utils.readBytesFromByteArray(
                    payload, offset, ProtocolParams.REQUEST_TYPE_SIZE);
            offset += ProtocolParams.REQUEST_TYPE_SIZE;

            /* Parse requestType */
            requestTypeIndex = ProtocolParams.calcRequestTypeIndex(requestType);
            requestTypeCase = ProtocolParams.calcDataCustomRequestTypeCase(requestTypeIndex);
            elementListMinCount =
                    ProtocolParams.calcDataCustomElementListMinCount(requestTypeIndex);
            elementListMaxCount =
                    ProtocolParams.calcDataCustomElementListMaxCount(requestTypeIndex);

            /* Parse element list */
            elementList = parseElementList(payload, offset, elementListMinCount,
                    elementListMaxCount, requestTypeCase);
            elementListCount = elementList.size();
        } catch (BufferUnderflowException | IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Private Constructor. Does not parse input. Does not copy input all input. */
    private <T> MessageDataCustom(byte[] magic, byte[] payload, T[] elementList)
            throws IllegalArgumentException {
        super(magic, CMD, payload);
        try {
            int offset = 0;

            /* Read request type */
            requestType = Utils.readBytesFromByteArray(
                    payload, offset, ProtocolParams.REQUEST_TYPE_SIZE);

            /* Parse requestType */
            requestTypeIndex = ProtocolParams.calcRequestTypeIndex(requestType);
            requestTypeCase = ProtocolParams.calcDataCustomRequestTypeCase(requestTypeIndex);
            elementListMinCount =
                    ProtocolParams.calcDataCustomElementListMinCount(requestTypeIndex);
            elementListMaxCount =
                    ProtocolParams.calcDataCustomElementListMaxCount(requestTypeIndex);

            /* Parse element list */
            this.elementList = new ArrayList<>(Arrays.asList(elementList));
            elementListCount = this.elementList.size();
        } catch (BufferUnderflowException | IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Get methods */
    public int getRequestTypeIndex() {
        return requestTypeIndex;
    }

    public int getRequestTypeCase() {
        return requestTypeCase;
    }

    public int getElementListMinCount() {
        return elementListMinCount;
    }

    public int getElementListMaxCount() {
        return elementListMaxCount;
    }

    public int getElementListCount() {
        return elementListCount;
    }

    public Object getElementByIndex(int index) {
        if(elementList == null) {
            throw new IndexOutOfBoundsException("Empty list.");
        } else {
            return elementList.get(index);
        }
    }

    public Iterator<?> getElementListIterator() {
        if(elementList == null) {
            return null;
        } else {
            return elementList.iterator();
        }
    }

    public String getRequestTypeString() {
        return Utils.getStringFromBytes(requestType, 0, requestType.length);
    }


    /* Creates a MessageDataCustom with given parameters */
    public static MessageDataCustom getMessageDataCustom(byte[] requestType, int[] int32List)
        throws IllegalArgumentException {
        try {
            /* init payload */
            int payloadSize = ProtocolParams.REQUEST_TYPE_SIZE +
                    ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE +
                    (BitcoinParams.INT32_SIZE * int32List.length);
            byte[] payload = new byte[payloadSize];
            int offset = 0;

            /* Write request type */
            System.arraycopy(requestType, 0, payload, offset, requestType.length);
            offset += ProtocolParams.REQUEST_TYPE_SIZE;

            /* Write element list count */
            BitcoinParams.UINT32ToByteArray(int32List.length, payload, offset);
            offset += ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE;

            /* Write element list */
            writeInt32List(payload, int32List, offset);

            Integer[] int32List2 = new Integer[int32List.length];
            for(int i=0; i<int32List.length; i++) {
                int32List2[i] = int32List[i];
            }

            return new MessageDataCustom(BitcoinParams.MAGIC_MAIN, payload, int32List2);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Creates a MessageDataCustom with given parameters */
    public static MessageDataCustom getMessageDataCustom(byte[] requestType, SHA256HASH[] hashList)
        throws IllegalArgumentException {
        try {
            /* init payload */
            int payloadSize = ProtocolParams.REQUEST_TYPE_SIZE +
                    ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE +
                    (SHA256HASH.HASH_SIZE * hashList.length);
            byte[] payload = new byte[payloadSize];
            int offset = 0;

            /* Write request type */
            System.arraycopy(requestType, 0, payload, offset, requestType.length);
            offset += requestType.length;

            /* Write element list count */
            BitcoinParams.UINT32ToByteArray(hashList.length, payload, offset);
            offset += ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE;

            /* Write element list */
            writeHashList(payload, hashList, offset);

            return new MessageDataCustom(BitcoinParams.MAGIC_MAIN, payload, hashList);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Creates a MessageDataCustom with given parameters */
    public static MessageDataCustom getMessageDataCustom(byte[] requestType,
                                                         MerkleTree[] merkleTreeList)
            throws IllegalArgumentException {
        try {
            /* init payload */
            int merkleTreesSerializedSize = 0;
            for(int i=0; i<merkleTreeList.length; i++) {
                merkleTreesSerializedSize += merkleTreeList[i].getSerializedSize();
            }
            int payloadSize = ProtocolParams.REQUEST_TYPE_SIZE +
                    ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE +
                    merkleTreesSerializedSize;
            byte[] payload = new byte[payloadSize];
            int offset = 0;

            /* Write request type */
            System.arraycopy(requestType, 0, payload, offset, requestType.length);
            offset += requestType.length;

            /* Write element list count */
            BitcoinParams.UINT32ToByteArray(merkleTreeList.length, payload, offset);
            offset += ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE;

            /* Write element list */
            writeMerkleTreeList(payload, merkleTreeList, offset);

            return new MessageDataCustom(BitcoinParams.MAGIC_MAIN, payload, merkleTreeList);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Creates a MessageDataCustom with given parameters */
    public static MessageDataCustom getMessageDataCustom(byte[] requestType,
                                                         Shard[] shardList)
            throws IllegalArgumentException, IOException {
        try {
            /* init payload */
            int shardListSerializedSize = 0;
            for(int i=0; i<shardList.length; i++) {
                shardListSerializedSize += shardList[i].getSerializedSize();
            }
            int payloadSize = ProtocolParams.REQUEST_TYPE_SIZE +
                    ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE +
                    shardListSerializedSize;
            org.apache.commons.io.output.ByteArrayOutputStream outputStream =
                    new org.apache.commons.io.output.ByteArrayOutputStream(shardListSerializedSize);

            /* Write request type */
            outputStream.write(requestType);

            /* Write element list count */
            BitcoinParams.UINT32ToOutputStream(shardList.length, outputStream);

            /* Write element list */
            writeShardList(outputStream, shardList);

            return new MessageDataCustom(
                    BitcoinParams.MAGIC_MAIN, outputStream.toByteArray(), shardList);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    private static void readInt32List(int[] dest, byte[] src, int offset) {
        for(int i=0; i<dest.length; i++) {
            dest[i] = BitcoinParams.readINT32(src, offset);
            offset += BitcoinParams.INT32_SIZE;
        }
    }

    private static void writeInt32List(byte[] dest, int[] src, int offset) {
        for(int i=0; i<src.length; i++) {
            BitcoinParams.INT32ToByteArray(src[i], dest, offset);
            offset += BitcoinParams.INT32_SIZE;
        }
    }


    private static void readHashList(SHA256HASH[] dest, byte[] src, int offset) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] = SHA256HASH.deserialize(src, offset);
            offset += SHA256HASH.SERIALIZED_SIZE;
        }
    }


    private static void writeHashList(byte[] dest, SHA256HASH[] src, int offset) {
        for (int i = 0; i < src.length; i++) {
            src[i].serialize(dest, offset);
            offset += SHA256HASH.SERIALIZED_SIZE;
        }
    }


    private static void readMerkleTreeList(MerkleTree[] dest, byte[] src, int offset) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] = MerkleTree.deserialize(src, offset);
            offset += dest[i].getSerializedSize();
        }
    }


    private static void writeMerkleTreeList(byte[] dest, MerkleTree[] src, int offset) {
        for(int i=0; i < src.length; i++) {
            src[i].serialize(dest, offset);
            offset += src[i].getSerializedSize();
        }
    }


    private static void readShardList(Shard[] dest, byte[] src, int offset) throws IOException {
        /* Make inputstream from byte array */
        ByteArrayInputStream inputStream = new ByteArrayInputStream(src, offset, src.length);

        for (int i = 0; i < dest.length; i++) {
            dest[i] = ShardSortedMapUtxs.deserialize(inputStream);
        }
    }


    private static void writeShardList(OutputStream dest, Shard[] src) throws IOException {
        for (int i=0; i<src.length; i++) {
            src[i].serialize(dest);
        }
    }


    public static boolean isDataCustom(Message message) {
        return message.equalsCommand(CMD);
    }


    @SuppressWarnings({"RedundantCast"})
    public void print(PrintStream printStream,
                      boolean doMessageHeader, boolean doRawPayload, boolean doList) {
        if(doMessageHeader) {
            super.print(printStream, doRawPayload);
        }

        printStream.println("Request type: " + getRequestTypeString());
        printStream.println("Request type case: " + getRequestTypeCase());
        printStream.println("Element list count: " + getElementListCount());
        if(elementList != null && doList) {
            printStream.println("Element list: ");
            Iterator<?> it = elementList.iterator();
            while (it.hasNext()) {
                if (requestTypeCase == 1) {
                    printStream.println((Integer) it.next());
                } else if (requestTypeCase == 2) {
                    printStream.println((Integer) it.next());
                } else if (requestTypeCase == 3) {
                    printStream.println(((SHA256HASH) it.next()).toString());
                } else if (requestTypeCase == 4) {
                    ((MerkleTree) it.next()).print(printStream, true, false);
                } else if (requestTypeCase == 5) {
                    printStream.println("");
                    ((Shard) it.next()).print(printStream, false, false);
                }
            }
        }
    }


    /* Prints raw response data */
    public void printRawData(OutputStream printStream) throws IOException {
        if(elementList != null) {
            Iterator<?> it = elementList.iterator();
            while(it.hasNext()) {
                if(requestTypeCase == 1) {
                    BitcoinParams.INT32ToOutputStream((Integer)it.next(), printStream);
                }
                else if(requestTypeCase == 2) {
                    BitcoinParams.INT32ToOutputStream((Integer)it.next(), printStream);
                }
                else if(requestTypeCase == 3) {
                    ((SHA256HASH)it.next()).serialize(printStream);
                }
                else if(requestTypeCase == 4) {
                    ((MerkleTree)it.next()).serialize(printStream);
                }
                else if(requestTypeCase == 5) {
                    ((Shard) it.next()).serialize(printStream);
                }
            }
            printStream.flush();
        }
    }


    /* Prints complete response data */
    @SuppressWarnings("RedundantCast")
    public void printCompleteReadableData(PrintStream printStream) throws IOException {
        if(elementList != null) {
            Iterator<?> it = elementList.iterator();
            while(it.hasNext()) {
                if(requestTypeCase == 1) {
                    printStream.println((Integer) it.next());
                }
                else if(requestTypeCase == 2) {
                    printStream.println((Integer) it.next());
                }
                else if(requestTypeCase == 3) {
                    printStream.println(((SHA256HASH)it.next()).getHashString());
                }
                else if(requestTypeCase == 4) {
                    ((MerkleTree)it.next()).print(printStream, false, false);
                    if(it.hasNext()) {
                        printStream.println("");
                    }
                }
                else if(requestTypeCase == 5) {
                    ((Shard) it.next()).print(printStream, true, true);

                    if(it.hasNext()) {
                        printStream.println("");
                    }
                }
            }
            printStream.flush();
        }
    }


    /* Prints preview response data */
    @SuppressWarnings("RedundantCast")
    public void printPreviewReadableData(PrintStream printStream) throws IOException {
        if(elementList != null) {
            Iterator<?> it = elementList.iterator();
            while(it.hasNext()) {
                if(requestTypeCase == 1) {
                    printStream.println((Integer) it.next());
                }
                else if(requestTypeCase == 2) {
                    printStream.println((Integer) it.next());
                }
                else if(requestTypeCase == 3) {
                    printStream.println(((SHA256HASH)it.next()).getHashString());
                }
                else if(requestTypeCase == 4) {
                    ((MerkleTree)it.next()).print(printStream, true, false);

                    if(it.hasNext()) {
                        printStream.println("");
                    }
                }
                else if(requestTypeCase == 5) {
                    ((Shard) it.next()).print(printStream, false, false);

                    if(it.hasNext()) {
                        printStream.println("");
                    }
                }
            }
            printStream.flush();
        }
    }


    /* Read element list within given range */
    public static ArrayList<Object> parseElementList(byte[] payload, int offset, int minCount,
                                                   int maxCount, int requestTypeCase)
            throws IllegalArgumentException {
        try {
            /* read element list count */
            int elementListCount = (int)BitcoinParams.readUINT32(payload, offset);
            offset += ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE;

            /* check element list count */
            if (elementListCount < minCount || elementListCount > maxCount) {
                throw new IllegalArgumentException("Element list count " + elementListCount +
                        " out of range (" + minCount + "," + maxCount + ")." );
            }

            /* read list */
            if (requestTypeCase == 1 || requestTypeCase == 2) {
                int[] heightList = new int[elementListCount];
                readInt32List(heightList, payload, offset);
                Integer[] heightList2 = new Integer[elementListCount];
                for(int i=0; i<elementListCount; i++) {
                    heightList2[i] = heightList[i];
                }
                return new ArrayList<>(Arrays.asList(heightList2));
            }
            else if (requestTypeCase == 3) {
                SHA256HASH[] hashList = new SHA256HASH[elementListCount];
                readHashList(hashList, payload, offset);
                return new ArrayList<>(Arrays.asList(hashList));
            }
            else if (requestTypeCase == 4) {
                MerkleTree[] merkleTreeList = new MerkleTree[elementListCount];
                readMerkleTreeList(merkleTreeList, payload, offset);
                return new ArrayList<>(Arrays.asList(merkleTreeList));
            }
            else if (requestTypeCase == 5) {
                Shard[] shardList = new Shard[elementListCount];
                readShardList(shardList, payload, offset);
                return new ArrayList<>(Arrays.asList(shardList));
            }
            else {
                throw new IllegalArgumentException("Request type not found.");
            }
        } catch (ArrayIndexOutOfBoundsException | NullPointerException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
