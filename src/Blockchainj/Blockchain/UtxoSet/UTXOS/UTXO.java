package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Bitcoin.TXO;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.Utils;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;


/**
 * UTXO - Unspent Transaction Output
 *
 * Immutable class.
 *
 * An unspent transaction output. The minimal data needed to represent a transaction output
 * in the Prototype Protocol.
 * To be used  alongside a Transaction Object (Transaction, UTX).
 *
 * Prototype Protocol serialization.
 * <outIndex, 4bytes><value, 8bytes><scriptBytes, compactSizeUInt><script, bytes>
 *
 * Storage serialization:
 * <outIndex, compactSizeInt><value, 8bytes><scriptBytes, compactSizeUInt><script, bytes>
 *
 */


public class UTXO {
    /* Transaction output value and script bytes. */
    private final byte[] value;
    private final byte[] script;

    /* Transaction output out index. */
    private final int outIndex;


    /* Construct new UTXO from TXO */
    public UTXO(TXO txo, int outIndex) {
        this.outIndex = outIndex;
        value = txo.getValueBytes();
        script = txo.getScript();
    }


    /* Private constructor. Does not validate and copy input. */
    private UTXO (int outIndex, byte[] value, byte[] script) {
        this.outIndex = outIndex;
        this.value = value;
        this.script = script;
    }


    /* Get methods */
    public int getOutIndex() { return outIndex; }

    public long getValue() { return BitcoinParams.readINT64(value, 0); }

    public byte[] getValueBytes() { return Arrays.copyOf(value, value.length); }

    public int getScriptLen() { return script.length; }

    public byte[] getScriptLenBytes() { return CompactSizeUInt.getEncoded(script.length); }

    public String getScriptString() { return Hex.encodeHexString(script); }

    public byte[] getScript() { return Arrays.copyOf(script, script.length); }


    /* Prototype Protocol serialization */
    public int getSerializedSize() {
        return BitcoinParams.TRANSACTION_OUT_INDEX_SIZE
                + value.length
                + CompactSizeUInt.getSizeOf(script.length)
                + script.length;
    }


    /* Storage serialization */
    public int getStorageSerializedSize() {
        return CompactSizeUInt.getSizeOf(outIndex)
                + value.length
                + CompactSizeUInt.getSizeOf(script.length)
                + script.length;
    }


    /** Prototype Protocol serialization */
    public void serialize(OutputStream outputStream) throws IOException {
        BitcoinParams.UINT32ToOutputStream(outIndex, outputStream);
        outputStream.write(value);
        new CompactSizeUInt(script.length).serialize(outputStream);
        outputStream.write(script);
    }


    /** Prototype Protocol deserialization */
    public static UTXO deserialize(InputStream inputStream) throws IOException {
        /* read outIndex */
        int outIndex = (int)BitcoinParams.readUINT32(inputStream);

        /* read value */
        byte[] value = Utils.readBytesFromInputStream(
                inputStream, BitcoinParams.TRANSACTION_VALUE_SIZE);

        /* read script bytes */
        CompactSizeUInt scriptBytes = CompactSizeUInt.deserialize(inputStream);

        /* read script */
        byte[] script = Utils.readBytesFromInputStream(inputStream, (int)scriptBytes.getValue());

        return new UTXO(outIndex, value, script);
    }


    /** Storage serialization */
    public void store(OutputStream outputStream) throws IOException {
        (new CompactSizeUInt(outIndex)).serialize(outputStream);
        outputStream.write(value);
        (new CompactSizeUInt(script.length)).serialize(outputStream);
        outputStream.write(script);
    }


    /** Storage deserialization */
    public static UTXO load(InputStream inputStream) throws IOException {
        /* read outIndex */
        int outIndex = (int)CompactSizeUInt.deserialize(inputStream).getValue();

        /* read value */
        byte[] value = Utils.readBytesFromInputStream(
                inputStream, BitcoinParams.TRANSACTION_VALUE_SIZE);

        /* read script bytes */
        int scriptBytes = (int)CompactSizeUInt.deserialize(inputStream).getValue();

        /* read script */
        byte[] script = Utils.readBytesFromInputStream(inputStream, scriptBytes);

        return new UTXO(outIndex, value, script);
    }


    @Override
    public String toString() { return "TXO_OutIndex: " + outIndex; }


    public void print(PrintStream printStream) {
        printStream.println("OutIndex: " + getOutIndex());
        printStream.println("Value: " + getValue() + " -- LE: 0x" + Hex.encodeHexString(value));
        printStream.println("Script length: " + getScriptLen() + " -- 0x" +
                Hex.encodeHexString(getScriptLenBytes()));
        printStream.println("Script: " + getScriptString());
        printStream.println("Serialized size: " + getSerializedSize());
    }


//    /* Used by UTXCompact */
//    @Deprecated
//    public byte[] getCopyOfSerializedValueAndScript() {
//        byte[] out = new byte[value.length + script.length];
//
//        System.arraycopy(value, 0, out, 0, value.length);
//        int offset = value.length;
//
//        System.arraycopy(script, 0, out, offset, script.length);
//
//        return out;
//    }
//
//    /* Used by UTXCompact */
//    @Deprecated
//    public static TXO getTXOFromValueAndScript(int outIndex, byte[] data) {
//        byte[] value = Arrays.copyOf(data, BitcoinParams.TRANSACTION_VALUE_SIZE);
//        int offset = BitcoinParams.TRANSACTION_VALUE_SIZE;
//
//        int scriptLen = data.length - offset;
//        byte[] script = new byte[scriptLen];
//        System.arraycopy(data, offset, script, 0, script.length);
//
//        return new TXO(outIndex, value, script);
//    }
//
//    /* Used by UTXCompact */
//    @Deprecated
//    public static void storeFromValueAndScript(OutputStream outputStream, byte[] data)
//            throws IOException {
//        outputStream.write(data, 0, BitcoinParams.TRANSACTION_VALUE_SIZE);
//        int offset = BitcoinParams.TRANSACTION_VALUE_SIZE;
//
//        int scriptLen = data.length - offset;
//        new CompactSizeUInt(scriptLen).serialize(outputStream);
//
//        outputStream.write(data, offset, scriptLen);
//    }
//
//    /* Used by UTXCompact */
//    @Deprecated
//    public static int serializedSizeStoreFromValueAndScript(byte[] data) {
//        int txValueLen = BitcoinParams.TRANSACTION_VALUE_SIZE;
//        int scriptLen = data.length - txValueLen;
//        return txValueLen + CompactSizeUInt.getSizeOf(scriptLen) + scriptLen;
//    }
//
//    /* Used by UTXCompact */
//    @Deprecated
//    public static byte[] loadFromValueAndScript(InputStream inputStream) throws IOException {
//        /* Read value */
//        byte[] value = Utils.readBytesFromInputStream(
//                inputStream, BitcoinParams.TRANSACTION_VALUE_SIZE);
//
//        /* read script bytes */
//        CompactSizeUInt scriptBytes = CompactSizeUInt.deserialize(inputStream);
//
//        /* read script */
//        byte[] script = Utils.readBytesFromInputStream(inputStream, (int)scriptBytes.getValue());
//
//        /* Make returna array */
//        byte[] out = new byte[value.length + script.length];
//        System.arraycopy(value, 0, out, 0, value.length);
//        System.arraycopy(script, 0, out, value.length, script.length);
//
//        return out;
//    }
}
