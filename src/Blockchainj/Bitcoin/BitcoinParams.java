package Blockchainj.Bitcoin;

import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * BitcoinParams - Bitcoin Protocol Parameters
 *
 */

public class BitcoinParams {
    /** Common byte sizes **/
    public static final int UINT32_SIZE = 4;
    public static final int INT32_SIZE = 4;
    public static final int INT64_SIZE = 8;


    /** Block **/
    /* SHA256 Hashing Algorithm. Using SHA256HASH. */

    /* Block: header size in bytes */
    public static final int BLOCK_HEADER_SIZE = 80;

    /* Block: version size - int32 */
    public static final int BLOCK_VERSION_SIZE = INT32_SIZE;

    /* Block: time size - uint32 */
    public static final int BLOCK_TIME_SIZE = UINT32_SIZE;

    /* Block: nbits size - uint32 */
    public static final int BLOCK_NBITS_SIZE = UINT32_SIZE;

    /* Block: nonce size - uint32 */
    public static final int BLOCK_NONCE_SIZE = UINT32_SIZE;


    /* Transaction: version size - uint32 */
    public static final int TRANSACTION_VERSION_SIZE = UINT32_SIZE;

    /* Transaction: Out index size - uint32 */
    public static final int TRANSACTION_OUT_INDEX_SIZE = UINT32_SIZE;

    /* Transaction: Sequence number size - uint32 */
    public static final int TRANSACTION_SEQUENCE_SIZE = UINT32_SIZE;

    /* Transaction: Locktime size - uint32 */
    public static final int TRANSACTION_LOCKTIME_SIZE = UINT32_SIZE;

    /* Transaction: Witness marker and flag */
    public static final int TRANSACTION_WIT_MARKER_FLAG_SIZE = 2;
    public static final byte[] TRANSACTION_WIT_MARKER_FLAG = {0x00, 0x01};

    /* Transaction: Value size - int64 */
    public static final int TRANSACTION_VALUE_SIZE = INT64_SIZE;


    /* Constants */
    /* Block: Blockhashes */
    public static final SHA256HASH GENESIS_BLOCKHASH = new SHA256HASH(
            "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f");
    public static final SHA256HASH SECOND_BLOCKHASH = new SHA256HASH(
            "00000000839a8e6886ab5951d76f411475428afc90947ee320161bbf18eb6048");
    public static final SHA256HASH ZERO_BLOCKHASH = SHA256HASH.ZERO_SHA256HASH;

    /* first block height */
    public static final int GENESIS_HEIGHT = 0;


    /** Message **/
    /* Message: Magic size - uint32 */
    public static final int MAGIC_SIZE = UINT32_SIZE;

    /* Message: Command size - char[12] ASCII */
    public static final int COMMAND_SIZE = 12;

    /* Message: Payload length size - uint32 */
    public static final int PAYLOAD_LENGTH_SIZE = UINT32_SIZE;

    /* Message: Payload checksum size - uint32 */
    public static final int PAYLOAD_CHECKSUM_SIZE = UINT32_SIZE;


    /* Constants */
    /* Message: magic bytes - internal byte order */
    public static final byte[] MAGIC_MAIN = {(byte)0xF9, (byte)0xBE, (byte)0xB4, (byte)0xD9};

    /* Message: Max messasge payload length in bytes */
    public static final int MAX_PAYLOAD_LENGTH = 0x02000000;

    /* Message: Reject message command bytes */
    public static final byte[] MESSAGE_CMD_REJECT = {
            (byte)0x72, (byte)0x65, (byte)0x6A, (byte)0x65, (byte)0x63, (byte)0x74, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};


    /** Blockchainj.Bitcoin Protocol Exceptions */
    /* Non unique txids. There are 2 non unique txids in the blockchain. Under those circumstances,
       this protocol exception will not repeat. It can only repeat due to a random coincidence.
       These 2 non unique txids can theoretically be spent but the network won't allow it.
       Since the data of those transaction was the same, the TXO data is the same. Therefore
       the shard hash will not change whether the first or second non unique txid appearance is
       included, unless metadata is included in the hash.
       At this point it's easier to include the first appearance of the non unique txids in the
       utxo set and drop the second one. Any spending of that transaction output will be done by
       the fullnode and not by this program anyway. Only Utxo Set consistency is important. */
    public static final SHA256HASH[] NON_UNIQUE_TXIDS = {
            new SHA256HASH("e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468"),
            new SHA256HASH("d5d27987d2a3dfc724e359870c6644b40e497bdc0589a033220fe15429d88599")};
    public static final int[] NON_UNIQUE_TXIDS_FIRST_SEEN = {91722, 91812};
    public static final int[] NON_UNIQUE_TXIDS_LAST_SEEN = {91880, 91842};
    public static final int NON_UNIQUE_TXIDS_LAST_HEIGHT;
    public static final int NON_UNIQUE_TXIDS_FIRST_HEIGHT;
    static {
        int[] temp = Arrays.copyOf(NON_UNIQUE_TXIDS_LAST_SEEN, NON_UNIQUE_TXIDS_LAST_SEEN.length);
        Arrays.sort(temp); //sort ascending
        NON_UNIQUE_TXIDS_LAST_HEIGHT = temp[temp.length - 1]; //get max

        int[] temp2 =Arrays.copyOf(NON_UNIQUE_TXIDS_FIRST_SEEN, NON_UNIQUE_TXIDS_FIRST_SEEN.length);
        Arrays.sort(temp2); //sort ascending
        NON_UNIQUE_TXIDS_FIRST_HEIGHT = temp2[0]; //get min
    }


    /** Blockchainj.Bitcoin Protocol - Known Transaction Versons */
    public static final byte[][] KNOWN_TRANSACTION_VERSIONS = new byte[][]{
            {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20},
            {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x30},
            {(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00},
            {(byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00},
            {(byte)0x02, (byte)0x00, (byte)0x00, (byte)0x20},
            {(byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00} };
    public static final byte UNKNOWN_TRANSACTION_VERSION_INDEX = (byte)(0xFF);



    /** Common methods for Blockchainj.Bitcoin Protocol (Endianness) **/
    /* Read uint32. Reads 4 bytes of byte array and returns uint32. */
    public static long readUINT32(byte[] data, int offset) {
        return Utils.readUint32LE(data, offset);
    }

    /* Read int32. Reads 4 bytes of byte array and returns int32. */
    public static int readINT32(byte[] data, int offset) {
        return (int)Utils.readUint32LE(data, offset);
    }

    /* Read int64. Reads 8 bytes of byte array and returns int64. */
    public static long readINT64(byte[] data, int offset) {
        return Utils.readInt64LE(data, offset);
    }


    /* Get uint32 byte array from uint32 value. */
    public static byte[] getUINT32(long value) {
        byte[] b = new byte[UINT32_SIZE];
        Utils.uint32ToByteArrayLE(value, b, 0);
        return b;
    }

    /* Get int32 byte array from int32 value. */
    public static byte[] getINT32(int value) {
        byte[] b = new byte[INT32_SIZE];
        Utils.int32ToByteArrayLE(value, b, 0);
        return b;
    }

    /* Get int64 byte array from int64 value. */
    public static byte[] getINT64(long value) {
        byte[] b = new byte[INT64_SIZE];
        Utils.uint64ToByteArrayLE(value, b, 0);
        return b;
    }


    /* Write uint32 to byte array. */
    public static void UINT32ToByteArray(long value, byte[] dest, int offset) {
        Utils.uint32ToByteArrayLE(value, dest, offset);
    }

    /* Write int32 to byte array. */
    public static void INT32ToByteArray(int value, byte[] dest, int offset) {
        Utils.int32ToByteArrayLE(value, dest, offset);
    }

    /* Write int64 to byte array. */
    public static void INT64ToByteArray(long value, byte[] dest, int offset) {
        Utils.uint64ToByteArrayLE(value, dest, offset);
    }


    /* Write uint32 to outputstream */
    public static void UINT32ToOutputStream(long value, OutputStream outputStream)
            throws IOException {
        outputStream.write(getUINT32(value));
    }

    /* Write int32 to outputstream */
    public static void INT32ToOutputStream(int value, OutputStream outputStream)
            throws IOException {
        outputStream.write(getINT32(value));
    }

    /* Write int64 to outputstream */
    public static void INT64ToOutputStream(long value, OutputStream outputStream)
            throws IOException {
        outputStream.write(getINT64(value));
    }


    /* Read uint32 from input stream */
    public static long readUINT32(InputStream inputStream)
            throws IOException {
        return readUINT32(Utils.readBytesFromInputStream(inputStream, UINT32_SIZE), 0);
    }

    /* Read int32 from input stream */
    public static int readINT32(InputStream inputStream)
            throws IOException {
        return readINT32(Utils.readBytesFromInputStream(inputStream, INT32_SIZE), 0);
    }

    /* Read int64 from input stream */
    public static long readINT64(InputStream inputStream)
            throws IOException {
        return readINT64(Utils.readBytesFromInputStream(inputStream, INT64_SIZE), 0);
    }



    /** Message methods. **/
    /* Check message magic bytes - internal byte order */
    public static boolean isMagicValid(byte[] magic) {
        if(magic.length != MAGIC_SIZE) {
            return false;
        }

        return Arrays.equals(magic, MAGIC_MAIN);
    }



    /** Blockchainj.Bitcoin Protocol - Known Transaction Versons */
    /* Get transaction version index. If 0xFF then transaction version is not known. */
    public static byte getTransactionVersionIndex(byte[] version) {
        for(int i=0; i<KNOWN_TRANSACTION_VERSIONS.length; i++) {
            if(version[0] == KNOWN_TRANSACTION_VERSIONS[i][0] &&
                    version[1] == KNOWN_TRANSACTION_VERSIONS[i][1] &&
                    version[2] == KNOWN_TRANSACTION_VERSIONS[i][2] &&
                    version[3] == KNOWN_TRANSACTION_VERSIONS[i][3]) {
                return (byte)(0xFF & i);
            }
        }
        return (byte)(0xFF);
    }

    /* Get new version from index */
    public static byte[] getTransactionVersion(byte index) {
        return Arrays.copyOf(KNOWN_TRANSACTION_VERSIONS[(int)index], TRANSACTION_VERSION_SIZE);
    }

    /* Does not copy input */
    public static byte[] getTransactionVersionIndexOrBytes(byte[] version) {
        byte temp = getTransactionVersionIndex(version);
        if(temp == UNKNOWN_TRANSACTION_VERSION_INDEX) {
            return version;
        } else {
            byte[] out = new byte[1];
            out[0] = temp;
            return out;
        }
    }

    /* Does not copy input */
    public static byte[] getTransactionVersion(byte[] indexOrBytes) {
        if(indexOrBytes.length == TRANSACTION_VERSION_SIZE) {
            return indexOrBytes;
        } else {
            return getTransactionVersion(indexOrBytes[0]);
        }
    }
}
