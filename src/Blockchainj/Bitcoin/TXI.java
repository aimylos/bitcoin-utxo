package Blockchainj.Bitcoin;

import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * TXI - Transaction Input
 *
 * Immutable class.
 *
 * A transaction input. The minimal data needed to represent a transaction input
 * in the Blockchainj.Bitcoin and(union) Prototype Protocol.
 * To be used  alongside a Transaction Object (Transaction, STX).
 *
 * Blockchainj.Bitcoin serialization.
 * Serialization format:
 * <prevTxid, 32bytes><prevOutIndex, 4bytes><scriptBytes, compactSizeUint><script, >0 bytes>
 *     <sequence, 4bytes>
 */

public class TXI {
    /* Blockchainj.Bitcoin Protocol. */
    /* Previous txid - 32bytes SHA256 */
    protected final SHA256HASH prevTxid;

    /* Previous outIndex - uin32 */
    protected final int prevOutIndex;

    /* Script - >1 bytes */
    protected final byte[] script;

    /* Sequence number - uint32 */
    protected final byte[] sequence;

    /* Serialized size bytes */
    protected final int serializedSize;


    /* Private constructor. Assumes valid inputs. */
    private TXI(SHA256HASH prevTxid, byte[] prevOutIndexBytes, byte[] script, byte[] sequence) {
        this.prevTxid = prevTxid;
        this.prevOutIndex = (int) BitcoinParams.readUINT32(prevOutIndexBytes, 0);
        this.script = script;
        this.sequence = sequence;
        this.serializedSize =
                prevTxid.getSerializedSize() +
                prevOutIndexBytes.length +
                CompactSizeUInt.getSizeOf(script.length) +
                script.length +
                sequence.length;
    }


    /* Shallow copy */
    public TXI(TXI txi) {
        prevTxid = txi.prevTxid;
        prevOutIndex = txi.prevOutIndex;
        script = txi.script;
        sequence = txi.sequence;
        serializedSize = txi.serializedSize;
    }


    /* Blockchainj.Bitcoin serialization. */
    public void serialize(OutputStream outputStream) throws IOException {
        prevTxid.serialize(outputStream);
        BitcoinParams.UINT32ToOutputStream(prevOutIndex, outputStream);
        (new CompactSizeUInt(script.length)).serialize(outputStream);
        outputStream.write(script);
        outputStream.write(sequence);
    }


    /* Blockchainj.Bitcoin deserialization. */
    @Deprecated
    public static TXI deserialize(InputStream inputStream) throws IOException {
        /* read prevTxid */
        SHA256HASH prevTxid = SHA256HASH.deserialize(inputStream);

        /* read prevOutIndex */
        byte[] prevOutIndexBytes = Utils.readBytesFromInputStream(
                inputStream, BitcoinParams.TRANSACTION_OUT_INDEX_SIZE);

        /* read scriptBytes */
        CompactSizeUInt scriptBytes = CompactSizeUInt.deserialize(inputStream);

        /* read script */
        byte[] script = Utils.readBytesFromInputStream(inputStream, (int)scriptBytes.getValue());

        /* read sequence */
        byte[] sequence = Utils.readBytesFromInputStream(
                inputStream, BitcoinParams.TRANSACTION_SEQUENCE_SIZE);

        return new TXI(prevTxid, prevOutIndexBytes, script, sequence);
    }


    /* Blockchainj.Bitcoin deserialization. */
    public static TXI deserialize(byte[] data, int offset) {
        /* read prevTxid */
        SHA256HASH prevTxid = SHA256HASH.deserialize(data, offset);
        offset += prevTxid.getSerializedSize();

        /* read prevOutIndex */
        byte[] prevOutIndexBytes = Utils.readBytesFromByteArray(
                data, offset, BitcoinParams.TRANSACTION_OUT_INDEX_SIZE);
        offset += BitcoinParams.TRANSACTION_OUT_INDEX_SIZE;

        /* read scriptBytes */
        CompactSizeUInt scriptBytes = CompactSizeUInt.deserialize(data, offset);
        offset += scriptBytes.getSerializedSize();

        /* read script */
        byte[] script = Utils.readBytesFromByteArray(data, offset, (int)scriptBytes.getValue());
        offset += (int)scriptBytes.getValue();

        /* read sequence */
        byte[] sequence = Utils.readBytesFromByteArray(
                data, offset, BitcoinParams.TRANSACTION_SEQUENCE_SIZE);

        return new TXI(prevTxid, prevOutIndexBytes, script, sequence);
    }


    @Override
    /* This is a specifically made equals for Set, that only check for prevOutIndex */
    public boolean equals(Object obj) {
        if(!(obj instanceof TXI)) {
            return false;
        }
        return ((TXI)obj).prevOutIndex == prevOutIndex;
    }


    /* This is a specifically made hasCode for Set, that only check for prevOutIndex */
    @Override
    public int hashCode() {
        return prevOutIndex;
    }


    @Override
    public String toString() {
        return "TXI_PrevTxid: " + prevTxid.toString() + ", TXI_PrevOutIndex: " + prevOutIndex;
    }


    public SHA256HASH getPrevTxid() { return prevTxid; }

    public int getPrevOutIndex() { return prevOutIndex; }

    public int getScriptLen() { return script.length; }

    public String getScriptString() { return Hex.encodeHexString(script); }

    public byte[] getScript() { return Arrays.copyOf(script, script.length); }

    public long getSequence() { return BitcoinParams.readUINT32(sequence, 0); }

    public int getSerializedSize() { return serializedSize; }


    /* DEBUG ONLY */
    public void print(PrintStream printStream) {
        printStream.println(">PrevTxid: " + getPrevTxid().toString());
        printStream.println("PrevOutIndex: " + getPrevOutIndex() + " -- LE: " +
                Hex.encodeHexString(BitcoinParams.getUINT32(getPrevOutIndex())));
        printStream.println("Script length: " + getScriptLen() + " -- " +
                (new CompactSizeUInt(getScriptLen()).getValueBytes()) );
        printStream.println("Script: " + getScriptString());
        printStream.println("Sequence: " + getSequence() + " -- LE: " +
                Hex.encodeHexString(sequence));
        printStream.println("Serialized size: " + getSerializedSize());
    }
}
