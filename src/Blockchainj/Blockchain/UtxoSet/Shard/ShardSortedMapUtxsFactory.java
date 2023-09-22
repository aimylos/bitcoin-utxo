package Blockchainj.Blockchain.UtxoSet.Shard;

import Blockchainj.Blockchain.UtxoSet.UTXOS.MainUtxFactory;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UtxFactory;
import Blockchainj.Util.SHA256HASH;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.SortedMap;

public class ShardSortedMapUtxsFactory implements ShardFactory {
    @Override
    public Shard getNewShard(int shardNum, int shardIndex) {
        return new ShardSortedMapUtxs(shardNum, shardIndex, null);
    }

    @Override
    public Shard getNewShard(int shardNum, int shardIndex, Object sortedUtxs) {
        //noinspection unchecked
        return getNewShard(shardNum, shardIndex, (SortedMap<SHA256HASH, UTX>)sortedUtxs);
    }

    public Shard getNewShard(int shardNum, int shardIndex, SortedMap<SHA256HASH, UTX> sortedUtxs) {
        return new ShardSortedMapUtxs(
                shardNum, shardIndex, sortedUtxs);
    }

    @Override
    public Shard deserialize(InputStream inputStream) throws IOException {
        return ShardSortedMapUtxs.deserialize(inputStream);
    }

    @Override
    public Shard load(InputStream inputStream) throws IOException {
        return ShardSortedMapUtxs.load(inputStream);
    }

    @Override
    public void printShardType(PrintStream printStream) {
        printStream.println("Shard type: " + ShardSortedMapUtxs.class.toString());
    }

    @Override
    public UtxFactory getUtxFactory() { return ShardSortedMapUtxs.getUtxFactoryStatic(); }
}
