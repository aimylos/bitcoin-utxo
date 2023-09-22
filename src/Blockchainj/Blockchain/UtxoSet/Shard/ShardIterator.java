package Blockchainj.Blockchain.UtxoSet.Shard;

import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.UTXOS.UTX;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * ShardIterator
 *
 * Create shard iterator from utx iterator.
 */


public class ShardIterator implements Iterator<Shard> {
    private final Iterator<UTX> utxIterator;
    private final int shardNum;
    private int nextShardIndex = 0;
    private UTX nextUtx = null;

    public ShardIterator(Iterator<UTX> utxIterator, int shardNum) {
        ProtocolParams.validateShardNum(shardNum);
        this.utxIterator = utxIterator;
        this.shardNum = shardNum;
    }

    @Override
    public Shard next() throws NoSuchElementException {
        if(!hasNext()) {
            throw new NoSuchElementException();
        }

        /* Get return shard's index */
        int returnShardIndex = nextShardIndex;
        nextShardIndex++;

        /* Construct next shard */

        /* List to put UTXs */
        LinkedList<UTX> utxList = new LinkedList<>();

        /* Temporary empty shard */
        Shard tempShard = MainShardFactory.shardFactory.getNewShard(shardNum, returnShardIndex);

        while(true) {
            /* Get next UTX */
            if(nextUtx == null) {
                /* Check for next UTX */
                if(utxIterator.hasNext()) {
                    nextUtx = utxIterator.next();
                }
                /* If there are no more UTXs, empty shard still need to be returned! */
                else {
                    break;
                }
            }

            /* Check if UTX belongs to shard and add it */
            if(tempShard.inRange(nextUtx.getTxid())) {
                utxList.add(nextUtx);
                nextUtx = null;
            }
            /* Else leave the UTX for the next shard and break */
            else {
                break;
            }
        }

        /* Create new shard */
        if(utxList.size() > 0) {
            UTX[] utxs = utxList.toArray(new UTX[0]);
            return MainShardFactory.shardFactory.getNewShard(shardNum, returnShardIndex, utxs);
        } else {
            return MainShardFactory.shardFactory.getNewShard(shardNum, returnShardIndex);
        }
    }


    @Override
    public boolean hasNext() {
        return (nextShardIndex < shardNum);
    }
}
