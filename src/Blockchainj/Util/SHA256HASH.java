package Blockchainj.Util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *  Comperable and Immutable SHA256HASH class.
 *  This class is thread safe.
 *
 *  SHA256 hashes are compared in the order they are viewed by the users.
 *  This means in reverse ordered from which they are serialized.
 *
 */

public class SHA256HASH implements Comparable<SHA256HASH> {
    /* hash size in bytes */
    public static final int HASH_SIZE = 32;
    public static final int SERIALIZED_SIZE = HASH_SIZE;

    private static final String ALGORITHM = "SHA-256";

    /* constants */
    private static final byte[] ZERO_HASH_BYTES = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final SHA256HASH ZERO_SHA256HASH = new SHA256HASH(ZERO_HASH_BYTES);

    /* hash data */
    private final byte[] hash;


    /* Constructor. Copies input. */
    public SHA256HASH(byte[] hash) throws IllegalArgumentException {
        if(hash.length != HASH_SIZE) {
            throw new IllegalArgumentException("SHA256 hash must be 32 bytes.");
        }
        this.hash = new byte[HASH_SIZE];
        System.arraycopy(hash, 0, this.hash, 0, HASH_SIZE);
    }


    /* Private constructor. Does not copy input. */
    //Bad practice. Could make object mutable.
//    private SHA256HASH(byte[] hash, byte randomByte) throws IllegalArgumentException {
//        if(hash.length != HASH_SIZE) {
//            throw new IllegalArgumentException("SHA256 hash must be 32 bytes.");
//        }
//        this.hash = hash;
//    }


    /* Constructor. From string as view by user. This means the bytes will be reverse to match
     * internal byte order. */
    public SHA256HASH(String hash) throws IllegalArgumentException {
        try {
            byte[] hashBytes = Utils.reverseBytes(Hex.decodeHex(hash));

            if(hashBytes.length != HASH_SIZE) {
                throw new IllegalArgumentException("SHA256 hash must be 32 bytes.");
            }
            this.hash = hashBytes;
        } catch (DecoderException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* Private constructor. Hashes data a specified number of times. */
    private SHA256HASH(byte[] data, int numberOfHashes) {
        //DigestUtils digestUtils = new DigestUtils(DigestUtils.getSha256Digest());
        MessageDigest messageDigest = getDigest();

        if(numberOfHashes <= 0) {
            throw new IllegalArgumentException("Number of hashes must be >0");
        }
        else if (numberOfHashes == 1) {
            this.hash = messageDigest.digest(data);
        }
        else if (numberOfHashes == 2) {
            byte[] hash1 = messageDigest.digest(data);
            messageDigest.reset();
            this.hash = messageDigest.digest(hash1);
        }
        else {
            byte[] hash = messageDigest.digest(data);
            for(int i=1; i<numberOfHashes; i++) {
                messageDigest.reset();
                hash = messageDigest.digest(hash);
            }
            this.hash = hash;
        }
    }


    /* Get methods */
    /* Returns a copy of the hash. */
    public byte[] getHash() { return Arrays.copyOf(hash, hash.length); }

    /* As viewed by Blockchainj.Bitcoin users. */
    public String getHashString() { return Hex.encodeHexString(Utils.reverseBytes(hash)); }

    /* Original byte order. Internal byte order. */
    public String getHashStringOriginal() { return Hex.encodeHexString(hash); }

    /* returns hash length in bytes */
    public int getSerializedSize() { return  SERIALIZED_SIZE; }


    /* Returns copy of first n bytes from internal ordered hash. */
    public byte[] getFirstNBytes(int n) {
        if(n <= 0 || n > HASH_SIZE) {
            throw new ArrayIndexOutOfBoundsException();
        }

        /* original, from, to(exclusive) */
        return Arrays.copyOfRange(hash, 0, n);
    }


    /* Parses last 32 bits of hash in LE into uint32 */
    public long getLastUINT32LE() {
        return Utils.readUint32LE(hash, HASH_SIZE - 4);
    }


    /* Computes this hash's SHA256 hash and returns it as new SHA256HASH */
    public SHA256HASH getHashOfHash() {
        return SHA256HASH.doSHA256(hash);
    }


    /* Serialize */
    public void serialize(OutputStream outputStream) throws IOException {
        outputStream.write(hash);
    }


    /* Serialize */
    public void serialize(byte[] dest, int offset) {
        System.arraycopy(hash, 0, dest, offset, HASH_SIZE);
    }

    /* Serialize */
    public void serialize(ByteBuffer byteBuffer) {
        byteBuffer.put(hash);
    }


    /* Deserialize */
    public static SHA256HASH deserialize(InputStream inputStream) throws IOException {
        byte[] hash = new byte[HASH_SIZE];
        if( inputStream.read(hash) != HASH_SIZE) {
            throw new IOException("Expected " + HASH_SIZE + " for SHA256 hash.");
        }
        return new SHA256HASH(hash);
    }


    /* Deserialize */
    public static SHA256HASH deserialize(byte[] src, int offset)
            throws NullPointerException, IndexOutOfBoundsException {
        byte[] hash = new byte[HASH_SIZE];
        System.arraycopy(src, offset, hash, 0, HASH_SIZE);
        return new SHA256HASH(hash);
    }


    /* Concatenates two hashes and returns new byte array of length 2*HASH_SIZE. */
    public static byte[] concat(SHA256HASH h1, SHA256HASH h2) {
        byte[] o = new byte[HASH_SIZE+HASH_SIZE];
        System.arraycopy(h1.hash, 0, o, 0, HASH_SIZE);
        System.arraycopy(h2.hash, 0, o, HASH_SIZE, HASH_SIZE);
        return o;
    }


    /* compareTo method. Compare in reverse internal byte order. */
    @Override
    public int compareTo(SHA256HASH o) {
        byte[] hash2 = o.hash;
        for(int i=HASH_SIZE-1; i>=0; i--) {
            if( (hash[i] & 0xFF) < (hash2[i] & 0xFF) ) {
                return -1;
            } else if( (hash[i] & 0xFF) > (hash2[i] & 0xFF) ) {
                return 1;
            }
        }
        return 0;
    }


    /* equals method */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SHA256HASH) {
            SHA256HASH hash = (SHA256HASH) obj;
            return Arrays.equals(this.hash, hash.hash);
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return getHashString();
    }


    @Override
    /* Due to grouping TXIDs into shards it's best if the hashCode is as more random for hashmaps
       to function more efficiently. */
    public int hashCode() {
        return Utils.readInt32LE(new byte[] {
                (byte)(hash[0] ^ hash[16]),
                (byte)(hash[5] ^ hash[21]),
                (byte)(hash[10] ^ hash[26]),
                (byte)(hash[15] ^ hash[31]),}, 0);
    }


    /* Returns new SHA256HASH from input bytes in reverse order */
    public static SHA256HASH getReverseHash(byte[] hash) {
        return new SHA256HASH(Utils.reverseBytes(hash));
    }


    /* Returns new zerohash */
    public static SHA256HASH getZeroHash() {
        return ZERO_SHA256HASH;
    }


    /* Returns a new SHA256HASH of the SHA256 hash of data */
    public static SHA256HASH doSHA256(byte[] data) {
        return new SHA256HASH(data, 1);
    }


    /* Returns a new SHA256HASH of the double SHA256 hash of data. */
    public static SHA256HASH doDoubleSHA256(byte[] data) {
        return new SHA256HASH(data, 2);
    }


    /* Returns a new SHA256 messageDigest */
    public static MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            return DigestUtils.getSha256Digest();
        }
    }


    /* Returns a new SHA256OutputStream */
    public static SHA256OutputStream getOutputStream() {
        return new SHA256OutputStream();
    }


    /* Concatanates the two hashes and returns the double SHA256 hash */
    public static SHA256HASH concatAndDoubleSHA256(SHA256HASH hash1, SHA256HASH hash2) {
        MessageDigest digest = getDigest();
        digest.update(hash1.hash);
        byte[] hashDigest1 = digest.digest(hash2.hash);
        byte[] hashDigest2 = digest.digest(hashDigest1);
        return new SHA256HASH(hashDigest2);
    }


    /* TEST/DEBUG */
//    public static void main(String[] args) {
//        try {
//            //test1();
//            test2();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    public static void test1() {
//        SimpleBlockBuffer bb = new SimpleBlockBuffer(0, 10000, new RPCconnection());
//        TreeMap<SHA256HASH, String> txidList = new TreeMap<>();
//
//        while(true){
//            try {
//                Block block = bb.getNextBlock();
//                Iterator<Transaction> it = block.getTxIterator();
//                while(it.hasNext()) {
//                    SHA256HASH txid = it.next().getTxid();
//                    txidList.put(txid, txid.toString());
//                }
//            } catch (BitcoinRpcException | BitcoinBlockException e) {
//                throw new RuntimeException(e);
//            } catch (EndOfRangeException e) {
//                break;
//            }
//        }
//
//        Iterator<String> it = txidList.values().iterator();
//        while(it.hasNext()) {
//            System.out.println(it.next());
//        }
//
//        System.out.println("Size: " + txidList.size());
//    }
//
//
//    public static void test2() {
//        String txidStr = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b";
//        //String txidStr = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7affffffff";
//        //String txidStr = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7a00000000";
//        SHA256HASH txid = new SHA256HASH(txidStr);
//
//        int shardNum = 1;
//        for(int i=0; i<31; i++) {
//            System.out.println(i + ".");
//            System.out.println("#shards: " + shardNum);
//            System.out.println(txid.toString().substring(0, 8));
//            System.out.println("index: " + ProtocolParams.calcShardIndex(shardNum, txid));
//            System.out.println("");
//            shardNum=shardNum<<1;
//        }
//    }
}
