package Blockchainj.Bitcoin;

import Blockchainj.Util.CompactSizeUInt;
import Blockchainj.Util.SHA256HASH;
import Blockchainj.Util.Utils;
import org.apache.commons.codec.binary.Hex;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Block - Bitcoin block
 *
 * Immutable class.
 *
 * The raw data are parsed into the appropriate fields.
 * The fields are then hashed according to protocol to calculate the TXIDs, used to calculate the
 * TX Merkle Root, used to calculate the blockhash, used to compare with the provided blockhash
 * with which the block was retrieved.
 *
 * Blockchainj.Bitcoin protocol serialization.
 *
 * Internal byte order is Blockchainj.Bitcoin serialization order.
 *
 */

public class Block {
    /* Block Metadata */
    /* blockhash - SHA256 */
    private final SHA256HASH blockhash;
    /* block height - int32 */
    private final int height;
    /* block serialized size in bytes */
    private final int serializedSize;


    /* Header Format */
    private final byte[] version;
    /* previous blockhash - 32 bytes SHA256 */
    private final SHA256HASH prevBlockhash;
    /* TX Merkle root hash - 32 bytes SHA256 */
    private final SHA256HASH txMerkleRoot;
    /* time - uint32 */
    private final byte[] time;
    /* nBits/difficutly - uint32 */
    private final byte[] nBits;
    /* nonce - uint32 */
    private final byte[] nonce;

    /* Transactions */
    /* txnCount - compactSize uint 1-9 bytes */
    private final CompactSizeUInt txnCount;
    /* transactions */
    private final Transaction[] tx;


    /* Private constructor. Does not copy input. Does not validate input fully. */
    private Block(int height, byte[] version, SHA256HASH prevBlockhash, SHA256HASH txMerkleRoot,
                  byte[] time, byte[] nBits, byte[] nonce, Transaction[] tx)
            throws BitcoinBlockException {
        this.height = height;
        this.version = version;
        this.prevBlockhash = prevBlockhash;
        this.txMerkleRoot = txMerkleRoot;
        this.time = time;
        this.nBits = nBits;
        this.nonce = nonce;
        this.txnCount = new CompactSizeUInt(tx.length);
        this.tx = tx;

        /* Calculate blockhash */
        blockhash = calcBlockhash();

        /* Calculate tx merkle root and compare it to header merkle root to make sure the block has
           been read correctly and maintain data integrity. */
        SHA256HASH calculatedTxMerkleRoot = calcTxMerkleRoot();
        if ( !txMerkleRoot.equals(calculatedTxMerkleRoot) )  {
            throw new BitcoinBlockException(
                    "Calculated merkle root does not match block header's merkle root",
                    blockhash.toString(), height);
        }

        /* Serialized Size */
        int tempSerializedSize = BitcoinParams.BLOCK_HEADER_SIZE;
        tempSerializedSize += txnCount.getSerializedSize();
        for (int i=0; i<tx.length; i++) {
            tempSerializedSize += tx[i].getSerializedSize();
        }
        serializedSize = tempSerializedSize;
    }


    /* Hash header to get blockhash */
    private SHA256HASH calcBlockhash() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BitcoinParams.BLOCK_HEADER_SIZE);
        byteBuffer.put(version);
        prevBlockhash.serialize(byteBuffer);
        txMerkleRoot.serialize(byteBuffer);
        byteBuffer.put(time);
        byteBuffer.put(nBits);
        byteBuffer.put(nonce);
        return SHA256HASH.doDoubleSHA256(byteBuffer.array());
    }


    /* Hash tx to get tx merkle tree root */
    private SHA256HASH calcTxMerkleRoot() {
        /* if there's only one transaction in the tx then it's used as the merkle root */
        if(tx.length == 1) {
            return tx[0].getTxid();
        }

        /* build merkle tree using a FIFO queue */
        LinkedList<SHA256HASH> queue = new LinkedList<>();

        /* first push all txid's into the queue. */
        for(int i=0; i<tx.length; i++) {
            queue.addLast(tx[i].getTxid());
        }

        /* push a null indicator at the end of the queue */
        queue.addLast(null);

        /* Main loop.
           - For each pair (hash1, hash2) preceding null indicator, concatenate, hash and push at
             the end.
           - If null indicator is reached, push it back at the end.
           - If the pair is (hash, null) and queue is empty then return hash as merkle root.
           - If the pair is (hash, null) and queue is non-empty, concatenate the hash with self,
             hash and push at the end. Then push null at the end.
           - Repeat.
        */
        for(;;) {
            if (queue.peekFirst() == null) {
                queue.removeFirst();
                queue.addLast(null);
            }

            SHA256HASH hash1 = queue.removeFirst();
            SHA256HASH hash2 = queue.removeFirst();

            if(queue.isEmpty()) {
                return hash1;
            }
            else if (hash2 == null) {
                queue.addLast( SHA256HASH.doDoubleSHA256(SHA256HASH.concat(hash1, hash1)) );
                queue.addLast(null);
            }
            else {
                queue.addLast( SHA256HASH.doDoubleSHA256(SHA256HASH.concat(hash1, hash2)) );
            }
        }
    }


    /* Get methods */
    public SHA256HASH getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public int getVersion() { return BitcoinParams.readINT32(version, 0); }

    public SHA256HASH getPrevBlockhash() { return prevBlockhash; }

    public SHA256HASH getTxMerkleRoot() { return txMerkleRoot; }

    public long getTime() { return BitcoinParams.readUINT32(time, 0); }

    public long getNBits() { return BitcoinParams.readUINT32(nBits, 0); }

    public long getNonce() { return  BitcoinParams.readUINT32(nonce, 0); }

    public int getTxnCount() { return (int)txnCount.getValue(); }

    public int getSerializedSize() { return serializedSize; }

    public Transaction getTxByIndex(int i) { return tx[i]; }

    public Iterator<Transaction> getTxIterator() { return Arrays.asList(tx).iterator(); }

    public Transaction[] getTx() { return Arrays.copyOf(tx, tx.length); }


    /* DEBUG */
    public void print(PrintStream printStream,
                      boolean doBlockHeader, boolean doTx, boolean doTxHeaderOnly) {
        if(doBlockHeader) {
            printStream.println("Blockhash: " + this.getBlockhash());
            printStream.println("Height: " + this.getHeight());
            printStream.println("Version: " + this.getVersion() + " -- LE: " +
                    Hex.encodeHexString(version));
            printStream.println("Previous blockhash: " + this.getPrevBlockhash());
            printStream.println("TX Merkle root: " + this.getTxMerkleRoot());
            printStream.println("Time: " + this.getTime() + " -- LE: " +
                    Hex.encodeHexString(time));
            printStream.println("Nbits: " + this.getNBits() + " -- LE: " +
                    Hex.encodeHexString(nBits));
            printStream.println("Nonce: " + this.getNonce() + " -- LE: " +
                    Hex.encodeHexString(nonce));
            printStream.println("Tx Count: " + this.getTxnCount() + " -- " + txnCount);
            printStream.println("Block size (bytes): " + this.getSerializedSize());
        }

        if(doTx) {
            for(int i=0; i<tx.length; i++) {
                printStream.println("\n\n");
                tx[i].print(printStream, false, true, !doTxHeaderOnly);
            }
        }
    }


    /* Deserialize */
    public static Block deserialize(SHA256HASH blockhash, int height, byte[] data, int offset)
            throws BitcoinBlockException {
        int originalOffset = offset;
        boolean headerParsed = false;
        try {
            /* Deserialize header */
            byte[] version =
                    Utils.readBytesFromByteArray(data, offset, BitcoinParams.BLOCK_VERSION_SIZE);
            offset += BitcoinParams.BLOCK_VERSION_SIZE;

            SHA256HASH prevBlockhash = SHA256HASH.deserialize(data, offset);
            offset += prevBlockhash.getSerializedSize();

            SHA256HASH txMerkleRoot = SHA256HASH.deserialize(data, offset);
            offset += txMerkleRoot.getSerializedSize();

            byte[] time =
                    Utils.readBytesFromByteArray(data, offset, BitcoinParams.BLOCK_TIME_SIZE);
            offset += BitcoinParams.BLOCK_TIME_SIZE;

            byte[] nBits =
                    Utils.readBytesFromByteArray(data, offset, BitcoinParams.BLOCK_NBITS_SIZE);
            offset += BitcoinParams.BLOCK_NBITS_SIZE;

            byte[] nonce =
                    Utils.readBytesFromByteArray(data, offset, BitcoinParams.BLOCK_NONCE_SIZE);
            offset += BitcoinParams.BLOCK_NONCE_SIZE;

            headerParsed = true;


            /* Deserialize transactions */
            CompactSizeUInt txCount = CompactSizeUInt.deserialize(data, offset);
            offset += txCount.getSerializedSize();

            Transaction[] tx = new Transaction[(int)txCount.getValue()];
            for (int i = 0; i < tx.length; i++) {
                try {
                    /* coinbase */
                    if (i == 0) {
                        tx[i] = Transaction.deserialize(blockhash, height, data, offset, true);
                    }
                    /* not coinbase */
                    else {
                        tx[i] = Transaction.deserialize(blockhash, height, data, offset, false);
                    }
                    offset += tx[i].getSerializedSize();
                } catch (BitcoinBlockException e) {
                    if (i > 0) {
                        e.setTxidB4Error(tx[i-1].getTxid().toString());
                        e.setOffsetB4Error(offset);
                    }
                    else {
                        e.setTxidB4Error("FIRST TRANSACTION");
                        e.setOffsetB4Error(offset);
                    }
                    throw  e;
                }
            }


            /* Make new block */
            Block block = new Block(
                    height, version, prevBlockhash, txMerkleRoot, time, nBits, nonce, tx);

            /* Match with original blockhash */
            if( !block.getBlockhash().equals(blockhash) ) {
                throw new BitcoinBlockException(
                        "Given blockhash doesn't match calculated blockhash.",
                        blockhash.toString(), height);
            }

            /* Match offset with serialized size */
            if( (offset-originalOffset) != block.getSerializedSize() ) {
                throw new BitcoinBlockException(
                        "Block serialized size doesn't match read bytes.",
                        blockhash.toString(), height);
            }

            return block;
        } catch (IndexOutOfBoundsException e) {
            if(!headerParsed) {
                throw new BitcoinBlockException(
                        "Parsing block header failed.", blockhash.toString(), height, e);
            }
            else {
                throw new BitcoinBlockException(
                        "Parsing block body failed.", blockhash.toString(), height, e);
            }
        }
    }

//    public static void main(String[] args) {
////        RPCconnection rpcCon = new RPCconnection();
////        int height = 401824;//481824; //125552;
////        try {
////            SHA256HASH blockhash = new SHA256HASH(
////                    Utils.reverseBytes(rpcCon.getBlockhashByHeight(height)));
////            byte[] rawBlock = rpcCon.getRawBlockByBlockhash(blockhash.toString());
////            Block block = Block.deserialize(blockhash, height, rawBlock, 0);
////            block.print(true, true, true);
////        } catch (BitcoinRpcException | BitcoinBlockException e) {
////            e.printStackTrace();
////            throw new RuntimeException();
////        }
//
//        PrintStream printStream = System.out;
//        RPCconnection rpcCon = new RPCconnection();
//        ConcurrentBlockBuffer cbb = new ConcurrentBlockBuffer(490000, 490000, rpcCon, 64, 4);
//        cbb.start();
//        //noinspection UnnecessaryLocalVariable
//        BlockBuffer bb = cbb;
//        //BlockBuffer bb = new SimpleBlockBuffer(400000, 401000, rpcCon);
//        while (true) {
//            try {
//                Block block = bb.getNextBlock();
//                //block.print(true, true, true);
//                UtxoSetChanges utxoSetChanges = block.calcUtxoSetChanges(256);
//                ShardChanges shardChanges = utxoSetChanges.getShardChangesIterator().next();
//                Iterator<STX> sortedStxIterator = shardChanges.getSortedStxIterator();
//                while(sortedStxIterator.hasNext()) {
//                    printStream.println(sortedStxIterator.next());
//                }
//
//                printStream.println("\n");
//
//                Iterator<UTX> sortedUtxIterator = shardChanges.getSortedUtxIterator();
//                while(sortedUtxIterator.hasNext()) {
//                    printStream.println(sortedUtxIterator.next());
//                }
//
//            } catch(BitcoinRpcException | BitcoinBlockException e){
//                throw new RuntimeException(e);
//            } catch (EndOfRangeException e) {
//                System.exit(0);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
}
