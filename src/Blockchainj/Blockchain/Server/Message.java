package Blockchainj.Blockchain.Server;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Message - Blockchainj.Bitcoin Protocol Message.
 *
 * Blockchainj.Bitcoin serialization.
 *
 */

public class Message {
    /* Magic bytes - uint32 */
    protected final byte[] magic;

    /* Command bytes - char[12] ASCII*/
    protected final byte[] command;

    /* Payload length - uint32 */
    protected final int payloadLength;
    protected final byte[] payloadLengthBytes;

    /* Payload checksum - uint32*/
    protected final byte[] checksum;

    /* Payload */
    protected final byte[] payload;

    /* Message header serialized size in bytes */
    public static int HEADER_SIZE = BitcoinParams.MAGIC_SIZE + BitcoinParams.COMMAND_SIZE +
            BitcoinParams.PAYLOAD_LENGTH_SIZE + BitcoinParams.PAYLOAD_CHECKSUM_SIZE;


    /* Constructor. Does not copy input. */
    public Message(byte[] magic, byte[] command, byte[] payload) throws IllegalArgumentException {
        /* check inputs */
        if(!BitcoinParams.isMagicValid(magic)) {
            throw new IllegalArgumentException("Magic not valid.");
        }

        if(command.length != BitcoinParams.COMMAND_SIZE) {
            throw new IllegalArgumentException("Command not valid.");
        }

        if(payload.length > BitcoinParams.MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("Payload length exceeded max length.");
        }

        this.magic = magic;
        this.command = command;
        this.payload = payload;
        this.payloadLength = payload.length;
        this.payloadLengthBytes = BitcoinParams.getUINT32(this.payloadLength);
        this.checksum = calcPayloadChecksum(this.payload);
    }


    /* Private constructor for internal use only. */
    private Message(byte[] magic, byte[] command, int payloadLen, byte[] checksum) {
        this.magic = magic;
        this.command = command;
        this.payloadLength = payloadLen;
        this.payloadLengthBytes = null;
        this.payload = null;
        this.checksum = checksum;
    }


    /* Calculate checksum and return it in internal byte order. */
    public static byte[] calcPayloadChecksum(byte[] payload) {
        return SHA256HASH.doDoubleSHA256(payload).getFirstNBytes(4);
    }


    /* Returns true if given checksum equals payload checksum. */
    public boolean equalsChecksum(byte[] checksum) {
        if(checksum.length != this.checksum.length) {
            return false;
        }

        return Arrays.equals(checksum, this.checksum);
    }


    /* Returns true if given command equels command */
    public boolean equalsCommand(byte[] command) {
        if(command.length != this.command.length) {
            return false;
        }

        return Arrays.equals(command, this.command);
    }


    /* Get methods */
    /* Returns the size in byte of the serialized message */
    public int getSerializedSize() {
        return magic.length + command.length + payloadLengthBytes.length + checksum.length +
                payload.length;
    }


    /* Returns the payload size in bytes */
    public int getPayloadLength() {
        return payloadLength;
    }


    /* Returns a copy of the command bytes */
    public byte[] getCommand() {
        return Arrays.copyOf(command, command.length);
    }


    /* Returns an inputstream for the payload */
    public InputStream getPayload() {
        return new ByteArrayInputStream(payload);
    }


    /* Returns magic as viewed by user */
    public String getMagicString() {
        return Hex.encodeHexString(Utils.reverseBytes(magic));
    }


    /* Returns command econded in ascii */
    public String getCommandString() {
        return Utils.getStringFromBytes(command, 0, command.length);
    }


    /* Returns checksum in internal byte order */
    public String getChecksumString() {
        return Hex.encodeHexString(checksum);
    }



    /* Deserialize message. */
    public static Message deserialize(InputStream inputStream) throws IOException {
        return deserialize(inputStream, false);
    }


    /* Deserialize message for socket. */
    public static Message deserializeFromSocket(InputStream inputStream) throws IOException {
        return deserialize(inputStream, true);
    }


    /* Deserialize with from socket option. */
    public static Message deserialize(InputStream inputStream, boolean fromSocket)
            throws IOException {
        Message headerMessage;
        byte[] payload;
        if(fromSocket) {
            /* Read header */
            headerMessage = deserializeHeader(
                    Utils.readBytesFromInputStreamSocket(inputStream, HEADER_SIZE));


            /* Read payload */
            payload = Utils.readBytesFromInputStreamSocket(
                    inputStream, headerMessage.payloadLength);
        }
        else {
            /* Read header */
            headerMessage =
                    deserializeHeader(Utils.readBytesFromInputStream(inputStream, HEADER_SIZE));

            /* Read payload */
            payload = Utils.readBytesFromInputStream(inputStream, headerMessage.payloadLength);
        }

        /* Make Message */
        Message message = new Message(headerMessage.magic, headerMessage.command, payload);

        /* Check checksum */
        if(!message.equalsChecksum(headerMessage.checksum)) {
            throw new IOException("Checksums don't match. Read: " +
                    Hex.encodeHexString(headerMessage.checksum) +
                    " Computed: " + message.getChecksumString());
        }

        /* Return Message */
        return message;
    }


    /* Deserialize Header */
    private static Message deserializeHeader(byte[] header) throws IOException {
        if(header.length != HEADER_SIZE) {
            throw new IOException("Message header must be " + HEADER_SIZE + " bytes.");
        }

        ByteBuffer bb = ByteBuffer.wrap(header);

        /* Read magic */
        byte[] magic = new byte[BitcoinParams.MAGIC_SIZE];
        bb.get(magic);

        /* Read command */
        byte[] command = new byte[BitcoinParams.COMMAND_SIZE];
        bb.get(command);

        /* Read payload length */
        byte[] payloadLenBytes = new byte[BitcoinParams.PAYLOAD_LENGTH_SIZE];
        bb.get(payloadLenBytes);
        int payloadLen = (int)BitcoinParams.readUINT32(payloadLenBytes, 0);

        /* Read payload checksum */
        byte[] checksum = new byte[BitcoinParams.PAYLOAD_CHECKSUM_SIZE];
        bb.get(checksum);

        /* Return special message object */
        return new Message(magic, command, payloadLen, checksum);
    }


    /* Serialize Message */
    public void serialize(OutputStream outputStream) throws IOException {
        serialize(outputStream, false);
    }


    /* Serialize Message to socket. */
    public void serializeToSocket(OutputStream outputStream) throws IOException {
        serialize(outputStream, true);
    }


    /* Socket To Serialize */
    public void serialize(OutputStream outputStream, boolean toSocket) throws IOException {
        //        ByteBuffer bb = ByteBuffer.allocate(getSerializedSize());
        //        bb.put(magic);
        //        bb.put(command);
        //        bb.put(payloadLengthBytes);
        //        bb.put(checksum);
        //        bb.put(payload);
        //        outputStream.write(bb.array());
        outputStream.write(magic);
        outputStream.write(command);
        outputStream.write(payloadLengthBytes);
        outputStream.write(checksum);
        outputStream.write(payload);

        if(toSocket) {
            outputStream.flush();
        }
    }


    /* DEBUG MOSTLY */
    public void print(PrintStream printStream, boolean doPayload) {
        System.out.println("Magic: " + getMagicString());
        System.out.println("Command: " + getCommandString());
        System.out.println("Payload len: " + getPayloadLength());
        System.out.println("Payload checksum: " + getChecksumString());
        if(doPayload) {
            System.out.println("Payload:");
            System.out.println(Hex.encodeHexString(payload));
        }
    }


    /* For logs mostly */
    @Override
    public String toString() {
        return "Magic:" + getMagicString() +
                " Command:" + getCommandString() +
                " Payload_length:" + getPayloadLength() +
                " Payload_checksum:" + getChecksumString();
    }
}
