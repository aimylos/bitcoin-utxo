package Blockchainj.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * SHA256OutputStream
 *
 * This class can be used as an outputstream but all data written to it are hashed into a
 * SHA256 digest that can be retrieved at any time.
 *
 */

public class SHA256OutputStream extends OutputStream {
    /* MessageDigest */
    private final MessageDigest messageDigest;

    /* Last digest */
    private SHA256HASH lastDigest;
    private long lastBytesWritten;
    private static final byte[] LAST_DIGEST_EMPTY =  SHA256HASH.getDigest().digest();

    /* Keeps track of size of data written */
    private long bytesWritten;
    private long bytesWrittenFromBeginning;

    /* Closed state */
    boolean isClosed = false;


    /* Constructor */
    public SHA256OutputStream() {
        messageDigest = SHA256HASH.getDigest();
        lastDigest = new SHA256HASH(LAST_DIGEST_EMPTY);
        lastBytesWritten = 0;
        bytesWritten = 0;
        bytesWrittenFromBeginning = 0;
    }


    /* Get last digest */
    public SHA256HASH getLastDigest() {
        return lastDigest;
    }

    public long getLastBytesWritten() { return lastBytesWritten; }


    /* Get digest. After this messageDigest resets! */
    public SHA256HASH getDigest() throws IOException {
        if(isClosed) {
            throw new IOException("Stream is closed.");
        }
        lastDigest = new SHA256HASH( messageDigest.digest() );
        lastBytesWritten = bytesWritten;
        messageDigest.reset();
        bytesWritten = 0;
        return lastDigest;
    }


    /* Get Bytes written from last getDigest */
    public long getBytesWritten() { return bytesWritten; }

    /* Get Bytes written from beginning of time */
    public long getBytesWrittenFromBeginning() { return bytesWrittenFromBeginning; }

    /* Same as getBytesWrittenFromBeginning */
    public long size() { return getBytesWrittenFromBeginning(); }


    /* OutputStream methods */
    @Override
    public void close() throws IOException {
        if(isClosed) {
            throw new IOException("Stream is closed.");
        }
        isClosed = true;
        getDigest();
    }


    @Override
    public void flush() throws IOException {
        if(isClosed) {
            throw new IOException("Stream is closed.");
        }
        //Does nothing
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(isClosed) {
            throw new IOException("Stream is closed.");
        }
        messageDigest.update(b, off, len);
        bytesWritten += len;
        bytesWrittenFromBeginning += len;
    }


    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }


    @Override
    public void write(int b) throws IOException {
        if(isClosed) {
            throw new IOException("Stream is closed.");
        }
        messageDigest.update((byte)b);
        bytesWritten++;
        bytesWrittenFromBeginning++;
    }
}
