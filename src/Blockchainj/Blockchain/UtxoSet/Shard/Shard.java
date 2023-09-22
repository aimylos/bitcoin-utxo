package Blockchainj.Blockchain.UtxoSet.Shard;

import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Blockchain.UtxoSet.UTXOS.*;
import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * Shard
 * A slice of the UTXO set that contains UTXOs that have a TXID with k common first bits.
 *
 * The Shard has a sorted map of UTXs.
 *
 * Prototype Protocol serialization (UTXs must be in order):
 * <shardNum, int32><shardIndex, int32><utxCount, compactSizeUint,can be 0x00>
 *     <UTX_0><UTX_1>...<UTX_n>
 *
 * Storage serialization:
 * Same as Prototype Protocol serialization but different UTX serialization.
 *
 * Number of shards, TXID and shard indexing:
 * The maximum number of shards is 2^30=1073741824 and the minimum is 2^0=1.
 * The maximum number of bits used from the txid are 30 and the minimum is 0.
 * The Txid is reversed from Little Endian to Big Endian, just like the user would see it.
 * The first bits that form the shard index are read in Big Endian.
 *
 */

@SuppressWarnings("DanglingJavadoc")
public interface Shard {
    /* Returns true if Txid is within the range of legal Txids for this shard. */
    boolean inRange(SHA256HASH txid);

    /* Returns true if utxo exits in shard, else false. */
    boolean hasUTXO(SHA256HASH txid, int outIndex);

    /* Returns TXO if found, else null */
    UTXO getUTXO(SHA256HASH txid, int outIndex);


    /** Prototype Protocol protocol serialization. */
    void serialize(OutputStream outputStream) throws IOException;

    /** Prototype Protocol protocol serialization.
     *  Must have static deserialize(). */


    /** Storage serialization */
    void store(OutputStream outputStream) throws IOException;

    /** Storage deserialization
     *  Must have static load(). */


    /* Returns the double SHA256 hash of the serialized shard. */
    SHA256HASH calcShardHash() throws IOException;


    /* Returns the Shard's shard number. The number of shards the utxo set was splitted into. */
    int getShardNum();

    /* Returns the Shard's index. */
    int getShardIndex();

    /* Returns the Shard's serialized */
    long getSerializedSize();

    /* Returns the Shard's storage serialized size */
    long getStorageSerializedSize();

    /* Returns sum of all UTX's serialized sizes */
    long getUtxSerializedSize();

    /* Returns the Shard's header's serialized size */
    long getHeaderSerializedSize(); //should be serializedSize-UtxSeriliazedSize

    /* Returns serialized size for shard empty header.
     * If the shards has many utxs, there's going to be 2 or 5 extra bytes
     * but then again the big shards mean few shards, so this method may lead calculations
     * to be a few bytes off. */
    static long getEmptyShardHeaderSerializedSize() {
        return (long) (ProtocolParams.SHARD_NUM_SIZE +
                    ProtocolParams.SHARD_NUM_SIZE +
                CompactSizeUInt.getSizeOf(0) );
    }

    /* Returns the Shard's UTX count */
    int getUtxCount();

    /* Returns the Shard's UTXO count */
    int getUtxoCount();

    /* Return true if the Shard's UTXO count is 0 */
    boolean isEmpty();

    /* Returns a, sorted on UTXs' TXIDs, Iterator over the shard's UTXs */
    Iterator<UTX> getUtxIterator();


    /* Apply shard changes to shard.
     * Changes are permanent. If this operation fails, the shard's state is undefined. */
    void applyShardChanges(ShardChanges shardChanges) throws BitcoinUtxoSetException;


    /* Print shard's state */
    void print(PrintStream printStream, boolean doUTXs, boolean doUTXOs);

    /* Print shard's parameters */
    void printParameters(PrintStream printStream);


    /* Returns read only shallow copy of this shard */
    //TODO: Shard getReadOnly();

    /* Returns the Utx factory used by shard */
    UtxFactory getUtxFactory();
}
