package Blockchainj.Util;


import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * CompactSizeUInt - Compact Size Unsigned Integer
 *
 * Immutable and comparable. Thread safe.
 *
 * Blockchainj.Bitcoin's variable length integer.
 *
 */

public class CompactSizeUInt implements Comparable<CompactSizeUInt>{
    /* Value */
    //private long value;

    /* Value bytes */
    private byte[] valueBytes;


    /* Contructor from bytes. Copies input. */
    public CompactSizeUInt(byte[] data, int offset) {
        long value = getValue(data, offset);
        valueBytes = getEncoded(value);
    }


    /* Constructor from value. */
    public CompactSizeUInt(long value) {
        valueBytes = getEncoded(value);
    }



    /* Get methods */
    public long getValue() {
        return getValue(valueBytes, 0);
    }

    /* Returns copy of value bytes */
    public byte[] getValueBytes() {
        return Arrays.copyOf(valueBytes, valueBytes.length);
    }

    public int sizeOf() {
        return valueBytes.length;
    }

    public int getSerializedSize() {
        return valueBytes.length;
    }

    public boolean compareTo(byte[] valueBytes) {
        if(valueBytes.length != this.valueBytes.length) {
            return false;
        }
        return Arrays.equals(valueBytes, this.valueBytes);
    }

    public boolean compareFirstByte(byte b) {
        return b == valueBytes[0];
    }



    /* Serialize */
    public void serialize(byte[] dest, int offset) {
        System.arraycopy(valueBytes, 0, dest, offset, valueBytes.length);
    }

    public void serialize(OutputStream outputStream) throws IOException {
        outputStream.write(valueBytes);
    }


    /* Deserialize */
    public static CompactSizeUInt deserialize(byte[] src, int offset) {
        return new CompactSizeUInt(src, offset);
    }

    public static CompactSizeUInt deserialize(InputStream inputStream) throws IOException{
        /* read first byte */
        byte[] firstByte = new byte[1];
        if ( inputStream.read(firstByte) != 1 ) {
            throw new IOException("Expected compactSize uint.");
        }

        /* calculate number of bytes needed */
        int bytesCount = getSizeOf(firstByte, 0);

        if(bytesCount == 1) {
            return new CompactSizeUInt(firstByte, 0);
        }
        else {
            /* save first byte in new byte array */
            byte[] outBytes = new byte[bytesCount];
            outBytes[0] = firstByte[0];

            /* read the rest of the bytes */
            if (inputStream.read(outBytes, 1, bytesCount - 1) != (bytesCount - 1)) {
                throw new IOException("Expected compactSize uint.");
            }

            return new CompactSizeUInt(outBytes, 0);
        }
    }


    /* compareTo method. */
    @Override
    public int compareTo(CompactSizeUInt o) {
        long val1 = this.getValue();
        long val2 = o.getValue();

        //noinspection UseCompareMethod
        if(val1 < val2) {
            return -1;
        }
        else if(val1 > val2) {
            return 1;
        }
        else {
            return 0;
        }
    }


    /* equals method */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompactSizeUInt) {
            return ((CompactSizeUInt)obj).getValue() == this.getValue();
        } else {
            return false;
        }
    }


    /* Returns hex of valueBytes */
    @Override
    public String toString() {
        return Hex.encodeHexString(valueBytes);
    }



    /* Convert methods */

    /* convert CompactSizeUInt bytes to long */
    public static long getValue(byte[] data, int offset) {
        int firstByte = 0xFF & data[offset];
        if (firstByte < 253) {
            return firstByte;
        } else if (firstByte == 253) {
            return (0xFF & data[offset + 1]) | ((0xFF & data[offset + 2]) << 8);
        } else if (firstByte == 254) {
            return Utils.readUint32LE(data, offset + 1);
        } else {
            return Utils.readInt64LE(data, offset + 1);
        }
    }

    /* returns size of compactSizeUInt */
    public static int getSizeOf(byte[] data, int offset) {
        int firstByte = 0xFF & data[offset];
        if (firstByte < 253) {
            return 1;
        } else if (firstByte == 253) {
            return 3;
        } else if (firstByte == 254) {
            return 5;
        } else {
            return 9;
        }
    }

    /* return size of compactSizeUInt */
    public static int getSizeOf(long val) {
        // if negative, it's actually a very large unsigned long value
        if (val < 0) return 9; // 1 marker + 8 data bytes
        if (val < 253) return 1; // 1 data byte
        if (val <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (val <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }

    /* Convert value to compactSizeUInt */
    public static byte[] getEncoded(long val) {
        byte[] bytes;
        switch (getSizeOf(val)) {
            case 1:
                return new byte[]{(byte) val};
            case 3:
                return new byte[]{(byte) 253, (byte) (val), (byte) (val >> 8)};
            case 5:
                bytes = new byte[5];
                bytes[0] = (byte) 254;
                Utils.uint32ToByteArrayLE(val, bytes, 1);
                return bytes;
            default:
                bytes = new byte[9];
                bytes[0] = (byte) 255;
                Utils.uint64ToByteArrayLE(val, bytes, 1);
                return bytes;
        }
    }
}
