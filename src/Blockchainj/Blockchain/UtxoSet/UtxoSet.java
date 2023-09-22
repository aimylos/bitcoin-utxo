package Blockchainj.Blockchain.UtxoSet;


import Blockchainj.Bitcoin.Block;
import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardFactory;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Util.MerkleTree;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * UtxoSet
 *
 * The Utxo Set manages the Unspent Transaction Outputs.
 *
 *
 * The Utxo Set has a state.
 *
 * Starting from the undefined height, when the Utxo Set is empty,
 * then height 0 to the best height, the Utxo Set will change it's state.
 * One state for each height and every consecutive state has consecutive height.
 *
 * The Utxo Set state consists of all the Unspent Transaction Outputs at the state's height, H,
 * once the block's transactions (block at height H) have been processed.
 *
 * To move the Utxo Set state forward means commiting the block's transactions to the Utxo Set
 * when both the block and the current Utxo Set state are at heights H+1 and H respectively.
 * Once the commitment is successful, the Utxo Set state will change and it's height will
 * be H+1.
 * !This interface does not promise (yet) that the Utxo Set state will be correct if the
 * commitment fails. An implementation of it might do though.
 * !This interface does not require (yet) that the Utxo Set state be able to move backwards,
 * from a height H to a height H-1, also referred as the "rollback".
 *
 *
 *
 * Utxo Set and Prototype Protocol.
 *
 * The Utxo Set provides a sorted iterator through all the Unspent Transaction Outputs.
 * The sorting is done on the Transaction's Output TXID primarily
 * and on the Transaction Output's index secondarily.
 *
 * The Utxo Set provides Prototype Protocol Shards.
 * When requiring the Shards the following parameter must be provided:
 *      The number of shards to split the Utxo Set into, as described by the Prototype Protocol protocol.
 *
 *
 *
 * Serialization.
 *
 * The Prototype Protocol serialization of the Utxo Set consists of all the serialized shards
 * for a given number of shards to split the Utxo Set into.
 * The shards are serialized in order, sorted on their shard index.
 *
 * The serialized size of the Utxo Set may differ for various shard numbers that split the Set.
 *
 * Nevertheless, the serialized size of the Sorted Unspent Transaction Outputs
 * remains constant for any shard number split of the set.
 * It does change from state to state, obviously.
 *
 *
 *
 * Close Utxo Set.
 *
 * The Utxo Set close() method stops the Utxo Set from making any more changes to itself.
 * The state of the Utxo Set will not be able to change (no more commitments will be possible).
 * The Utxo Set will remain readable, but all pending data in the memory will be stored on
 * the disk.
 * After the Utxo Set has been closed succesfully it can then be reloaded from the disk
 * in the future, and it's state will be the same as the state of the Utxo Set
 * when the close() method terminated.
 * The format of the Utxo Set on the disk may vary depending on it's implementation, but
 * it's Prototype Protocol serialization remains the same.
 *
 *
 *
 * Synchronization and access.
 *
 * The basic synchronization scheme that must be followed is:
 *      Read methods must acquire some kind of read lock.
 *      Write methods (block commitment, close(), ect) must acquire some kind of write lock.
 * Of cource, synchronized methods will do too, but concurrent reading will not be possible.
 *
 * The Utxo Set internal structure has private access and cannot be corrupted.
 * Nevertheless, the access to the Utxo Set data (Unspent Transaction Outputs: UTX and TXO)
 * is public. Since UTXs are mutable, if their data is altered in any way outside the Utxo Set,
 * the Utxo Set may become corrupted.
 *
 */

public interface UtxoSet {
    /** Undefined height */
    int UNDEFINED_HEIGHT = ProtocolParams.UNDEFINED_HEIGHT;

    //TODO Add lock for optional long data reading


    /**
     * Commit Block. Just like SQL transactions, this operation is atomic.
     * The changes should be all changes to the Utxo Set done by a single block.
     * After this operation, the Utxo Set height will be increased by one and the Utxo log
     * will be updated appropriately.
     * If a data existance (utxo not found or utxo already exist) error occurs, then
     * a BitcoinUtxoSetException is thrown and the utxo set is corrupted!
     *
     * NOT DONE...and all changes to the Utxo Set up to that point
     * are rolledback to the state before the method was called...NO ROLLBACK
     * To overcome ROLLBACK issues, archiving must be used periodically...NOT DONE
     *
     * This is a write operation. */
    void commitBlock(Block block) throws BitcoinUtxoSetException, IOException;


    /**
     * Safely closes the utxo set.
     * After this method has been called, access to the utxo write methods is not possible.
     * Access to read methods may have undefined behaviour.
     *
     * This is a write operation. */
    void close() throws IOException;

    /**
     * This is a read operation. */
    boolean isClosed();


    /**
     * Returns the height of the current Utxo Set state.
     *
     * This is a read operation.
     */
    int getBestHeight();


    /**
     * Returns the blockhash of the current Utxo Set state.
     * This is the blockhash of the block that cause the Utxo Set to reach the current state.
     *
     * This is a read operation. */
    SHA256HASH getBestBlockhash();


    /**
     * Returns the blockhash of the block that cause the Utxo Set to reach the state it had
     * at the given height.
     *
     * This is a read operation. */
    SHA256HASH getBlockhash(int height) throws NoSuchElementException;


    /**
     * Returns the UTX (Transactions with Unspent Outputs) count of the current Utxo Set state.
     *
     * This is a read operation.
     */
    int getUtxCount();

    /**
     * Returns the UTXO (Unspent Transaction Outputs) count of the current Utxo Set state.
     *
     * This is a read operation.
     */
    int getUtxoCount();


    /**
     * Returns the serialized size of the UTXs of the current Utxo Set state.
     *
     * This is a read operation. */
    long getUtxSerializedSize();


    /**
     * Returns an Iterator over the UTXs of the current Utxo Set state.
     * The iterator is sorted on ascending UTX's TXID.
     *
     * Although this is a read operation, if any write operations are performed during
     * the iteration of this iterator, the behaviour of the iterator will be undefined! */
    Iterator<UTX> getUtxIterator();


    /**
     * Returns a good estimate of the serialized size of the Utxo Set
     * for a given shard number to split the Utxo Set into.
     *
     * This is a read operation. */
    long getUtxoSetSerializedSizeEstimate(int shardNum) throws IllegalArgumentException;


    /**
     * Returns a Shard for the given shard number to split the Utxo Set into and given
     * a shard index. The Shard is created from current Utxo Set state.
     *
     * Although this is a read operation, if any write operations are performed
     * on the returned Shard, the behaviour of the UtxoSet will be undifiend!
     * //TODO: consider read only shallow copy
     *
     * This is a read operation. */
    Shard getShard(int shardNum, int shardIndex) throws IOException, IllegalArgumentException;


    /**
     *  Returns an Iterator over the Shards of the current Utxo Set state,
     *  given shard number to split the Utxo Set into and given a shard index.
     *  Will also return Shards that happen to be empty.
     *  The iterator is sorted on ascending shard index.
     *
     *  Although this is a read operation, if any write operations are performed during
     *  the iteration of this iterator, the behaviour of the iterator will be undefined!
     *  //TODO: consider read only shallow copy
     *
     * This is a read operation. */
    Iterator<Shard> getShardIterator(int shardNum) throws IllegalArgumentException;


    /**
     * Returns the Merkle Tree for a give shard number to split the Utxo Set into.
     *
     * This is a read operation. */
    MerkleTree getMerkleTree(int shardNum) throws IOException, IllegalArgumentException;


    /**
     * Print miscellaneous information about the Utxo Set and it's current state.
     *
     * This is a read operation. */
    void print(PrintStream printStream);


    /**
     * Print miscellaneous information about the Utxo Set parameters.
     *
     * This is a read operation. */
    void printParameters(PrintStream printStream);


    /**
     * Returns shard factory used by Utxo Set.
     */
    ShardFactory getShardFactory();
}
