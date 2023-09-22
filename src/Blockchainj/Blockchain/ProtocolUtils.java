package Blockchainj.Blockchain;

import Blockchainj.Bitcoin.BitcoinParams;
import Blockchainj.Bitcoin.BitcoinUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** ProtocolUtils - Prototype Protocol Utilities */

public class ProtocolUtils {
    /** Block height */
    public static void writeHeight(int height, OutputStream outputStream) throws IOException {
        BitcoinParams.INT32ToOutputStream(height, outputStream);
    }

    public static int readHeight(InputStream inputStream) throws IOException {
        try {
            int height = BitcoinParams.readINT32(inputStream);
            BitcoinUtils.validateHeight(height);
            return height;
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }


    /** Booelean */
    public static void writeBoolean(boolean bool, OutputStream outputStream) throws IOException {
        outputStream.write((bool)?(ProtocolParams.BYTE_TRUE):(ProtocolParams.BYTE_FALSE));
    }

    public static boolean readBoolean(InputStream inputStream) throws IOException {
        byte bool = (byte) (inputStream.read() & 0xFF);
        if(bool == ProtocolParams.BYTE_TRUE) {
            return true;
        } else if(bool == ProtocolParams.BYTE_FALSE) {
            return false;
        } else {
            throw new IOException("Failed to read boolean.");
        }
    }
}
