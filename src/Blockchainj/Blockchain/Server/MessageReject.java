package Blockchainj.Blockchain.Server;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.Utils;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.PrintStream;


/**
 * MessageReject - Blockchainj.Bitcoin Reject Message.
 */


public class MessageReject extends Message {
    /* Reject command */
    private static final byte[] CMD = BitcoinParams.MESSAGE_CMD_REJECT;

    /* Index of fields on payload. */
    private final CompactSizeUInt messageTypeLen;
    private final int messageTypeLenIndex;
    private final int messageTypeIndex;
    private final int ccodeIndex;
    private final CompactSizeUInt reasonLen;
    private final int reasonLenIndex;
    private final int reasonIndex;
    private final int extraDataIndex;


    /* Constructor. Does not copy data. */
    public MessageReject(byte[] magic, byte[] payload) throws IllegalArgumentException {
        super(magic, CMD, payload);

        try {
            int offset = 0;


            messageTypeLen = CompactSizeUInt.deserialize(payload, offset);
            messageTypeLenIndex = offset;
            offset += messageTypeLen.getSerializedSize();

            if (messageTypeLen.getValue() > 0) {
                messageTypeIndex = offset;
                offset += messageTypeLen.getValue();
            } else {
                messageTypeIndex = -1;
            }

            ccodeIndex = offset;
            offset += 1;

            reasonLen = CompactSizeUInt.deserialize(payload, offset);
            reasonLenIndex = offset;
            offset += reasonLen.getSerializedSize();

            if (reasonLen.getValue() > 0) {
                reasonIndex = offset;
                offset += reasonLen.getValue();
            } else {
                reasonIndex = -1;
            }

            if (offset < payload.length) {
                extraDataIndex = offset;
            } else if (offset == payload.length) {
                extraDataIndex = -1;
            } else {
                throw new IllegalArgumentException("Payload not valid.");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Payload not valid.");
        }
    }


    /* Constructor. Does not copy data. */
    public MessageReject(Message messasge) throws IllegalArgumentException {
        this(messasge.magic, messasge.payload);

        if(!isReject(messasge)) {
            throw new IllegalArgumentException("Message is not MessageReject.");
        }
    }


    /* Get methods */
    public String getMessageTypeString() {
        if(messageTypeIndex < 0) {
            return "EMPTY_FIELD";
        } else {
            return Utils.getStringFromBytes(
                    payload, messageTypeIndex, (int)messageTypeLen.getValue());
        }
    }

    public int getMessageTypeLen() {
        return (int)messageTypeLen.getValue();
    }

    public String getCCodeString() {
        return Hex.encodeHexString(Utils.readBytesFromByteArray(payload, ccodeIndex, 1));
    }

    public String getReasonString() {
        if(reasonIndex < 0) {
            return "EMPTY_FIELD";
        } else {
            return Utils.getStringFromBytes(payload, reasonIndex, (int)reasonLen.getValue());
        }
    }

    public int getReasonLen() {
        return (int)reasonLen.getValue();
    }

    public String getExtraDataString() {
        if(extraDataIndex < 0) {
            return "EMPTY_FIELD";
        } else {
            return Utils.getStringFromBytes(payload, extraDataIndex, payload.length);
        }
    }

    public int getExtraDataLen() {
        return payload.length - extraDataIndex;
    }


    /* Creates a reject message with given parameters */
    public static MessageReject getRejectMessage(byte[] messageType, byte ccode, byte[] reason,
                                        byte[] extraData) throws IOException{
        /* Extra variables */
        int payloadLen = 0;
        CompactSizeUInt messageTypeLen;
        CompactSizeUInt reasonLen;

        /* Message type */
        if(messageType == null) {
            messageTypeLen = new CompactSizeUInt(0);
            payloadLen += messageTypeLen.getSerializedSize();
        } else {
            messageTypeLen = new CompactSizeUInt(messageType.length);
            payloadLen += messageTypeLen.getSerializedSize();
            payloadLen += messageTypeLen.getValue();
        }

        /* ccode */
        payloadLen++;

        /* Reason */
        if(reason == null) {
            reasonLen = new CompactSizeUInt(0);
            payloadLen += reasonLen.getSerializedSize();
        } else {
            reasonLen = new CompactSizeUInt(reason.length);
            payloadLen += reasonLen.getSerializedSize();
            payloadLen += reasonLen.getValue();
        }

        /* extra data */
        if(extraData != null) {
            payloadLen += extraData.length;
        }

        /* Make outputstream retrievable into a bytearray. Use apache's. */
        org.apache.commons.io.output.ByteArrayOutputStream outputStream =
                new org.apache.commons.io.output.ByteArrayOutputStream(payloadLen);

        messageTypeLen.serialize(outputStream);
        if(messageType != null) {
            outputStream.write(messageType);
        }

        outputStream.write(ccode);

        reasonLen.serialize(outputStream);
        if(reason != null) {
            outputStream.write(reason);
        }

        if(extraData != null) {
            outputStream.write(extraData);
        }

        return new MessageReject(BitcoinParams.MAGIC_MAIN, outputStream.toByteArray());
    }


    public static boolean isReject(Message message) {
        return message.equalsCommand(CMD);
    }


    /* DEBUG MOSTLY */
    public void print(PrintStream printStream, boolean doMessageHeader, boolean doRawPayload) {
        if(doMessageHeader) {
            super.print(printStream, doRawPayload);
        }

        System.out.println("Message type: " + getMessageTypeString());
        System.out.println("Message type length: " + getMessageTypeLen());
        System.out.println("Ccode: " + getCCodeString());
        System.out.println("Reason: " + getReasonString());
        System.out.println("Reason length: " + getReasonLen());
        System.out.println("Extra data: " + getExtraDataString());
        System.out.println("Extra data lengthL " + getExtraDataLen());
    }

    @Override
    public String toString() {
        return super.toString() + " CCode:" + getCCodeString();
    }

    public String toStringReject() {
        return "Message type: " + getMessageTypeString() +
                "Ccode: " + getCCodeString() +
                "Reason: " + getReasonString() +
                "Extra data: " + getExtraDataString();
    }
}
