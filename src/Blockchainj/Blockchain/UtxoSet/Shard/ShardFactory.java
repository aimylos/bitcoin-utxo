package Blockchainj.Blockchain.UtxoSet.Shard;


import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * ShardFactory
 *
 * Shard instantiation methods.
 */

public interface ShardFactory {
    Shard getNewShard(int shardNum, int shardIndex);

    Shard getNewShard(int shardNum, int shardIndex, Object sortedUtxs);

    Shard deserialize(InputStream inputStream) throws IOException;

    Shard load(InputStream inputStream) throws IOException;

    UtxFactory getUtxFactory();

    void printShardType(PrintStream printStream);
}
