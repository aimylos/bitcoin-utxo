package Blockchainj.Bitcoin;

import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.Utils;

import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.util.Arrays;

/**
 * TXO - Transaction Output
 *
 * Immutable class.
 *
 * A Bitcoin transaction output. The minimal data needed to represent a transaction output
 * in the Bitcoin Protocol.
 *
 * Serialization:
 * <value, 8bytes><scriptBytes, compactSizeUInt><script, bytes>
 *
 */

public class TXO {
    /* Blockchainj.Bitcoin and Prototype Protocol. */
    protected final byte[] value;
    protected final byte[] script;


    /* Private constructor. Assumes valid inputs. */
    protected TXO(byte[] value, byte[] script) {
        this.value = value;
        this.script = script;
    }

//    /* Shallow copy constructor. */
//    public TXO(TXO txo) {
//        this.value = txo.value;
//        this.script = txo.script;
//    }


    public long getValue() { return BitcoinParams.readINT64(value, 0); }

    public byte[] getValueBytes() { return Arrays.copyOf(value, value.length); }

    public int getScriptLen() { return script.length; }

    public byte[] getScriptLenBytes() { return CompactSizeUInt.getEncoded(script.length); }

    public String getScriptString() { return Hex.encodeHexString(script); }

    public byte[] getScript() { return Arrays.copyOf(script, script.length); }

    /* Bitcoin protocol */
    public int getSerializedSize() {
        return value.length
                + CompactSizeUInt.getSizeOf(script.length)
                + script.length;
    }


    /* DEBUG */
    public void print(PrintStream printStream) {
        printStream.println("Value: " + getValue() + " -- LE: 0x" + Hex.encodeHexString(value));
        printStream.println("Script length: " + getScriptLen() + " -- 0x" +
                Hex.encodeHexString(getScriptLenBytes()));
        printStream.println("Script: " + getScriptString());
        printStream.println("Serialized size: " + getSerializedSize());

    }


    /* DEBUG ONLY */
//    public byte[] getSerializedData() throws IOException {
//        org.apache.commons.io.output.ByteArrayOutputStream outputStream =
//                new org.apache.commons.io.output.ByteArrayOutputStream(getSerializedSize());
//        serialize(outputStream);
//        byte[] output = outputStream.toByteArray();
//        outputStream.close();
//        return output;
//    }


    /* DEBUG/TEST */
//    public static void main(String[] args)
//            throws IOException, BitcoinBlockException, BitcoinRpcException {
//        int startAt = 450000; //Integer.parseInt(args[0]);
//        int endAt = 450002; //Integer.parseInt(args[1]);
//        String filenamepath = "/tmp/TXOSerialz.bin"; //args[2];
//        List<TXO> txoList = new LinkedList<>();
//        SimpleBlockBuffer bb = new SimpleBlockBuffer(startAt, endAt, new RPCconnection());
//
//        for (; ; ) {
//            try {
//                Block block = bb.getNextBlock();
//                Iterator<Transaction> it = block.getTxIterator();
//                while (it.hasNext()) {
//                    Transaction t = it.next();
//                    Iterator<TransactionOutput> it2 = t.getTxOutIterator();
//                    while(it2.hasNext()) {
//                        txoList.add(it2.next());
//                    }
//                }
//
//                if(block.getHeight()%1000 == 0) {
//                    System.out.println("Reading from blocks at height: " + block.getHeight());
//                }
//            } catch (EndOfRangeException e) {
//                break;
//            }
//        }
//
//        /* serialize */
//        OutputStream outputStream = new FileOutputStream(filenamepath);
//        Iterator<TXO> it = txoList.iterator();
//        while(it.hasNext()) {
//            it.next().serialize(outputStream);
//        }
//        outputStream.close();
//
//        /* deserialize */
//        InputStream inputStream = new FileInputStream(filenamepath);
//        it = txoList.iterator();
//        int count =0;
//        while(it.hasNext()) {
//            TXO dTXO = TXO.deserialize(inputStream);
//            TXO oTXO = it.next();
//            SHA256OutputStream o1 = new SHA256OutputStream();
//            SHA256OutputStream o2 = new SHA256OutputStream();
//            oTXO.serialize(o1);
//            dTXO.serialize(o2);
//            if(!o1.getDigest().equals(o2.getDigest())) {
//                oTXO.print();
//                System.out.println("\n");
//                dTXO.print();
//                System.out.println("\nCount: " + count);
//                System.exit(1);
//            }
//            count++;
//        }
//        inputStream.close();
//    }
}