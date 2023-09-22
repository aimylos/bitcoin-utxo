package Blockchainj.Blockchain.Server;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Util.Utils;

import java.io.PrintStream;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 *  MessageGetCustom - Custom made get data message.
 *
 *  Command: "getcustom"
 *  Payload:
 *      Request type 12 bytes
 *      Element count uint32 4 bytes
 *      List of elements. Min:0, Max:10
 *
 *  Request types along with their List:
 *      <type, ascii, 12 bytes, right null padded>, <List, type>
 *
 *      "bestheight", no list
 *
 *      "bestblkhash", no list
 *      "blockhash", list of heights <int32, 4 bytes>
 *
 *      "bestmrklroot", no list
 *      "merkleroot", list of heights <int32, 4 bytes>
 *
 *      "bestmrkltree", no list
 *
 *      "bestshardnum", no list
 *      "shardnum", list of heights <int32 4 bytes>
 *
 *      "bestshard", list of indexes <int32 4 bytes>
 */

public class MessageGetCustom extends Message {
    /* GetCustom command */
    private static final byte[] CMD = ProtocolParams.MESSAGE_CMD_GETCUSTOM;

    /* Request type bytes - char[12] */
    private final byte[] requestType;

    /* Request index to request types */
    private final int requestTypeIndex;

    /* Request type case. */
    private final int requestTypeCase;

    /* Element list min/max count */
    private final int elementListMinCount;
    private final int elementListMaxCount;

    /* List element count - uint32 */
    private final int listElementCount;

    /* List of heights/indexes */
    private final int[] int32List;


    /* Constructor. Does not copy input all input. */
    public MessageGetCustom(byte[] magic, byte[] payload) throws IllegalArgumentException {
        super(magic, CMD, payload);

        try {
            int offset = 0;

            /* Read request type */
            requestType = Utils.readBytesFromByteArray(
                    payload, offset, ProtocolParams.REQUEST_TYPE_SIZE);
            offset += requestType.length;

            /* Parse requestType */
            requestTypeIndex = ProtocolParams.calcRequestTypeIndex(requestType);
            requestTypeCase = ProtocolParams.calcGetCustomRequestTypeCase(requestTypeIndex);
            elementListMinCount = ProtocolParams.calcGetCustomElementListMinCount(requestTypeIndex);
            elementListMaxCount = ProtocolParams.calcGetCustomElementListMaxCount(requestTypeIndex);

            /* Case 1 is no list */
            if(requestTypeCase == 1) {
                listElementCount = 0;
                int32List = null;
            } else if(requestTypeCase == 2 || requestTypeCase == 3) {
                listElementCount = (int) BitcoinParams.readUINT32(payload, offset);
                offset += ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE;

                if(listElementCount<elementListMinCount || listElementCount>elementListMaxCount) {
                    throw new IllegalArgumentException("List too long.");
                }

                int32List = new int[listElementCount];
                readInt32List(int32List, payload, offset);
            } else {
                throw new IllegalArgumentException("Invalid payload.");
            }
        } catch (BufferUnderflowException | IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Constructor */
    public MessageGetCustom(Message message) throws IllegalArgumentException {
        this(message.magic, message.payload);

        if(!isGetCustom(message)) {
            throw new IllegalArgumentException("Message is not MessageGetCustom");
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

    public int getListElementCount() {
        return listElementCount;
    }

    public int getInt32ListByIndex(int index) {
        return int32List[index];
    }

    public String getRequestTypeString() {
        /* Get ASCII */
        return Utils.getStringFromBytes(requestType, 0, requestType.length);
    }

    public boolean isRequestType(byte[] requestType) {
        return ProtocolParams.isRequestTypeEqual(this.requestType, requestType);
    }


    public static boolean isGetCustom(Message message) {
        return message.equalsCommand(CMD);
    }


    private static void readInt32List(int[] dest, byte[] src, int offset) {
        for(int i=0; i<dest.length; i++) {
            dest[i] = BitcoinParams.readINT32(src, offset);
            offset += 4;
        }
    }

    private static void writeInt32List(byte[] dest, int[] src, int offset) {
        for(int i=0; i<src.length; i++) {
            BitcoinParams.INT32ToByteArray(src[i], dest, offset);
            offset += 4;
        }
    }


    /* Creates a GetCustom message with given parameters */
    public static MessageGetCustom getMessageGetCustom(byte[] requestType, int[] int32List)
            throws IllegalArgumentException {
        /* Calc request case */
        int requestTypeIndex = ProtocolParams.calcRequestTypeIndex(requestType);
        int requestTypeCase = ProtocolParams.calcGetCustomRequestTypeCase(requestTypeIndex);
        byte[] payload;

        /* no list */
        if(requestTypeCase == 1) {
            payload  = Utils.readBytesFromByteArray(
                    requestType, 0, ProtocolParams.REQUEST_TYPE_SIZE);
        }
        /* height or index list */
        else if(requestTypeCase == 2 || requestTypeCase == 3) {
            int listElementCount = int32List.length;

            payload = new byte[ProtocolParams.REQUEST_TYPE_SIZE +
                    ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE +
                    (BitcoinParams.INT32_SIZE * listElementCount)];
            int offset = 0;

            System.arraycopy(requestType, 0, payload, offset, ProtocolParams.REQUEST_TYPE_SIZE);
            offset += ProtocolParams.REQUEST_TYPE_SIZE;

            BitcoinParams.UINT32ToByteArray(listElementCount, payload, offset);
            offset += ProtocolParams.REQUEST_TYPE_LIST_COUNT_SIZE;

            writeInt32List(payload, int32List, offset);
        }
        else {
            throw new IllegalArgumentException("Request type invalid.");
        }

        return new MessageGetCustom(BitcoinParams.MAGIC_MAIN, payload);
    }


    /* DEBUG MOSTLY */
    public void print(PrintStream printStream, boolean doMessageHeader, boolean doRawPayload) {
        if(doMessageHeader) {
            super.print(printStream, doRawPayload);
        }

        printStream.println("Request type: " + getRequestTypeString());
        printStream.println("Request type case: " + getRequestTypeCase());
        printStream.println("Element list count: " + getListElementCount());
        if(int32List != null) {
            printStream.println("Int32 list: ");
            for(int i=0; i<int32List.length; i++) {
                printStream.println("\t" + int32List[i]);
            }
        } else {
            printStream.println("Element list: " + null);
        }
    }


    @Override
    public String toString() {
        return super.toString() + " RequestType:" + getRequestTypeString() +
                " ListCount:" + getListElementCount();
    }


    /* Parse user input to MessageGetCustom */
    public static MessageGetCustom getMessageGetCustom(String[] args, int offset)
            throws IllegalArgumentException{
        try {
            /* First argument should be request type */
            byte[] requestType = new byte[ProtocolParams.REQUEST_TYPE_SIZE];
            byte[] args0 = args[offset].getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(args0, 0, requestType, 0, args0.length);
            for(int i=args0.length; i<ProtocolParams.REQUEST_TYPE_SIZE; i++) {
                requestType[i] = (byte)0x00;
            }
            offset++;

            /* Calc type case */
            int requestTypeIndex = ProtocolParams.calcRequestTypeIndex(requestType);
            int requestTypeCase = ProtocolParams.calcGetCustomRequestTypeCase(requestTypeIndex);

            /* no list */
            if (requestTypeCase == 1) {
                return getMessageGetCustom(requestType, null);
            }
            /* int32 list */
            else if(requestTypeCase == 2 || requestTypeCase == 3) {
                /* read all integers */
                ArrayList<Integer> list = new ArrayList<>();
                for(int i=offset; i<args.length; i++) {
                    try {
                        list.add(Integer.parseInt(args[i]));
                    } catch (NumberFormatException e) {
                        break;
                    }
                }

                if(list.size() == 0) {
                    return getMessageGetCustom(requestType, null);
                }
                else
                {
                    int[] int32List = new int[list.size()];
                    for(int i=0; i<list.size(); i++) {
                        int32List[i] = list.get(i);
                    }
                    return getMessageGetCustom(requestType, int32List);
                }
            }
            else {
                throw new IllegalArgumentException("Request type case not found.");
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /* Returns requests formats */
    public static String requestFormat() {
        return "bestheight \n" +
                "bestblkhash \n" +
                "blockhash <height list>  (max count: " +
                ProtocolParams.REQUEST_TYPE_LIST_MAX_COUNT + ")\n" +
                "bestmrklroot \n" +
                "merkleroot <height list>  (max count: " +
                ProtocolParams.REQUEST_TYPE_LIST_MAX_COUNT + ")\n" +
                "bestmrkltree \n" +
                "bestshardnum \n" +
                "shardnum <height list>  (max count: " +
                ProtocolParams.REQUEST_TYPE_LIST_MAX_COUNT + ")\n" +
                "bestshard <index list>  (max count: " +
                ProtocolParams.REQUEST_TYPE_LIST_MAX_COUNT + ")\n";
    }
}
