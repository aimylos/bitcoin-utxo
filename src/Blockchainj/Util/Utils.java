package Blockchainj.Util;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class Utils {
    public static void suggestGarbageCollectorRun() {
        System.gc();
        System.runFinalization();
    }


    public static void uint32ToByteArrayBE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & (val >> 24));
        out[offset + 1] = (byte) (0xFF & (val >> 16));
        out[offset + 2] = (byte) (0xFF & (val >> 8));
        out[offset + 3] = (byte) (0xFF & val);
    }

    public static void uint32ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    public static void int32ToByteArrayLE(int val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    public static void uint64ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));
    }


    /* Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit
       integer in little endian format. Since java doesn't have unsigned int, using long
       ensures unsigned 32-bit will be read correctly. */
    public static long readUint32LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFFL) |
                ((bytes[offset + 1] & 0xFFL) << 8) |
                ((bytes[offset + 2] & 0xFFL) << 16) |
                ((bytes[offset + 3] & 0xFFL) << 24);
    }

    /* Parse 4 bytes from byte array (starting at the offeset) as signed 32-bit integer in little
       endian format. */
    public static int readInt32LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) |
                ((bytes[offset + 1] & 0xFF) << 8) |
                ((bytes[offset + 2] & 0xFF) << 16) |
                ((bytes[offset + 3] & 0xFF) << 24);
    }

    /* Parse 8 bytes from the byte array (starting at the offset) as signed 64-bit integer
       in little endian format. */
    public static long readInt64LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFFL) |
                ((bytes[offset + 1] & 0xFFL) << 8) |
                ((bytes[offset + 2] & 0xFFL) << 16) |
                ((bytes[offset + 3] & 0xFFL) << 24) |
                ((bytes[offset + 4] & 0xFFL) << 32) |
                ((bytes[offset + 5] & 0xFFL) << 40) |
                ((bytes[offset + 6] & 0xFFL) << 48) |
                ((bytes[offset + 7] & 0xFFL) << 56);
    }

    /** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big endian format. */
    public static long readUint32BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFFL) << 24) |
                ((bytes[offset + 1] & 0xFFL) << 16) |
                ((bytes[offset + 2] & 0xFFL) << 8) |
                (bytes[offset + 3] & 0xFFL);
    }


    /* Returns a copy of the given byte array in reverse order. */
    public static byte[] reverseBytes(byte[] bytes) {
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }


    /* Read bytes from input stream. */
    public static byte[] readBytesFromInputStream(InputStream inputStream, int numBytes)
            throws IOException {
        if(numBytes == 0) {
            return new byte[0];
        }

        byte[] output = new byte[numBytes];
        int bytesRead = inputStream.read(output, 0, numBytes);
        if( bytesRead != numBytes ) {
            throw new IOException("Expected " + numBytes + " but read " + bytesRead + ".");
        }
        return output;
    }


    /* Read bytes from byte array */
    public static byte[] readBytesFromByteArray(byte[] data, int offset, int numBytes) {
        byte[] outBytes = new byte[numBytes];
        System.arraycopy(data, offset, outBytes, 0, numBytes);
        return outBytes;
    }


    /* Read bytes from input stream form socket */
    @Deprecated
    public static byte[] readBytesFromInputStreamSocket(InputStream inputStream, int numBytes)
            throws IOException {
        final int tries = 20;
        final int millis = 100;

        for(int i=0; i<tries; i++) {
            if( inputStream.available() >= numBytes ) {
                byte[] output = new byte[numBytes];
                int bytesRead = inputStream.read(output, 0, numBytes);
                if( bytesRead != numBytes ) {
                    throw new IOException("Expected " + numBytes + " but read " + bytesRead + ".");
                }
                return output;
            } else {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }

        throw new IOException("Failed to read " + numBytes + ".");
    }


    /* Try to return ASCII string from byte array else return hex string */
    public static String getStringFromBytes(byte[] src, int offset, int len) {
        try {
            return new String(src, offset, len, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Hex.encodeHexString( Arrays.copyOfRange(src, offset, offset+len));
        }
    }



    /* Source: https://stackoverflow.com/questions/41107/
            how-to-generate-a-random-alpha-numeric-string */
    public static String getRandomString(int length) {
        if (length < 1) throw new IllegalArgumentException();

        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = upper.toLowerCase(Locale.ROOT);
        String digits = "0123456789";
        String symbolsString = upper + lower + digits;
        char[] symbols = symbolsString.toCharArray();
        char[] buf = new char[length];
        Random random = new SecureRandom();

        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buf);
    }


    /* Returns a new apache byte array outputstream */
    public static org.apache.commons.io.output.ByteArrayOutputStream getNewOutputStream(int size) {
        return  new org.apache.commons.io.output.ByteArrayOutputStream(size);
    }
}
