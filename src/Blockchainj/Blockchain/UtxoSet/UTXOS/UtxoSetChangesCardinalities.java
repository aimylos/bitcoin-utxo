package Blockchainj.Blockchain.UtxoSet.UTXOS;


import Blockchainj.Util.SHA256HASH;

/* Cardinalities class */
public class UtxoSetChangesCardinalities {
    public final SHA256HASH blockhash;
    public final int height;
    public final int shardNum;
    public final int modifiedShardCount;
    public final int utxCount;
    public final int utxoCount;
    public final int stxCount;
    public final int stxiCount;
    public final int txidCount;

    public UtxoSetChangesCardinalities(SHA256HASH blockhash, int height,
                                        int shardNum, int modifiedShardCount,
                                        int utxCount, int utxoCount,
                                        int stxCount, int stxiCount) {
        this.blockhash = blockhash;
        this.height = height;
        this.shardNum = shardNum;
        this.modifiedShardCount = modifiedShardCount;
        this.utxCount = utxCount;
        this.utxoCount = utxoCount;
        this.stxCount = stxCount;
        this.stxiCount = stxiCount;
        this.txidCount = utxCount + stxCount;
    }
}
