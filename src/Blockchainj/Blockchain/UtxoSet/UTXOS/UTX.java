package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.TXI;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * UTX - Unspent Transaction
 * A set of "unspent" transaction outputs that have in common the transaction that produced them.
 * These are not necessarily ALL the transaction outputs of that transaction.
 * All the transaction outputs came from the same transaction (common TXID).
 *
 * There MUST be at least one unspent transaction output in the UTX.
 *
 * Allong with the TXID and the UTXOs, the UTX also keeps the Transaction Version,
 * the Transaction's Block's height and where that transaction is the coinbase transaction
 * of the block.
 *
 * Keeps UTXOs sorted by outIndex.
 *
 * Immutable class.
 *
 * Prototype Protocol serialization (Has utxoCount after spent txo have been removed):
 *     <Txid, 32bytes><version, 4bytes><height, int32><isCoinbase, 1 byte>
 *         <utxoCount, compactSizeUint,cannot be 0x00><utxo[] serialized, look below>
 * utxo[] are the "unspent" UTXO's ordered by outIndex. There is at least ONE.
 * <utxo_first, UTXO serialization><utxo_second, UTXO serialization>...
 *      ...<utxo_last, UTXO serialization>
 *
 *
 * Storage serialization (Has complete/original utxoCount):
 *      <Txid, 32bytes><version, 4bytes><height, int32><isCoinbase, 1 byte>
 *          <utxoCount, compactSizeUint,cannot be 0x00><utxo[] stored, ?>
 */


public interface UTX extends Comparable<UTX>{
    /* True if UTXO is present in UTX. */
    boolean hasUtxo(int outIndex);

    /* Get UTXO - returns null if does not exist. */
    UTXO getUtxo(int outIndex);

    /* Spent UTXO with Transaction Input and current height.
     * If UTXO successfully spent then,
     * either returns new UTX if there more UTXOs or null if all UTXOs have been spent.
     * Throws BitcoinUtxoSetException if Transaction Input does not spent any UTXO.
     * Throws IllegalArgumentException if txids don't match. */
    UTX spentUTXO(TXI txi, int height) throws BitcoinUtxoSetException, IllegalArgumentException;


    /** Prototype Protocol protocol serialization. */
    void serialize(OutputStream outputStream) throws IOException;

    /** Storage serialization */
    void store(OutputStream outputStream) throws IOException;


    /* Returns the TXID of this UTX */
    SHA256HASH getTxid();

    /* Returns the version of this UTX's transaction */
    long getVersion();

    /* Returns a copy of the version's bytes */
    byte[] getVersionBytes();

    /* Returns the block's height that contains this transaction */
    int getHeight();

    /* Returns true if this transaction is coinbase */
    boolean isCoinbase();

    /* Returns the serialized size for Prototype Protocol serialization. */
    long getSerializedSize();

    /* Returns the serialized size for Storage serialization. */
    long getStorageSerializedSize();

    /* Returns the count of UTXOs this UTX has. This count does NOT include removed UTXOs. */
    int getUtxosCount();

    /* Returns a UTXO iterator of this UTX */
    Iterator<UTXO> getUtxoIterator();

    @Override
    String toString();

    /* compareTo() method. Compares the UTX's TXIDs. */
    @Override
    int compareTo(UTX o);

    /* Print UTX's data in readable format. */
    void print(PrintStream printStream, boolean doUtxos);

    /* Print UTX's parameters */
    void printParameters(PrintStream printStream);
}
