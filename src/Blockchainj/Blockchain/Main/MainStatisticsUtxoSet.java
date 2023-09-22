package Blockchainj.Blockchain.Main;

import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Bitcoin.RPC.RPCconnection;
import Blockchainj.Blockchain.Statistics.StatisticsUtxoSet;
import Blockchainj.Blockchain.UtxoSet.BitcoinUtxoSetException;
import Blockchainj.Blockchain.UtxoSet.UtxoSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/**
 *  MainStatisticsUtxoSet
 *
 *  This class computes statistics for utxo set.
 *  Uses UserParams to configure parameters.
 */

public class MainStatisticsUtxoSet {

    public static void main(String[] args) throws IOException, BitcoinUtxoSetException {
        /* Using UserParams for parameters */
        UserParams.loadOrWriteUserParams(args);

        /* Set Shard and UTX Types */
        UserParams.setShardAndUtxTypes();

        /* init print stream */
        PrintStream PRINT_STREAM = UserParams.PRINT_STREAM;

        /* Create utxo set. */
        UtxoSet utxoSet;
        /* Create utxo set. */
        try {
            utxoSet = UserParams.tryOpenUtxoSet(UserParams.getUtxoSetPath("STAT_UTXOSET"));
        } catch (FileNotFoundException e) {
            utxoSet = UserParams.createNewUtxoSet(UserParams.getUtxoSetPath("STAT_UTXOSET"));
        }

        /* Create new RPC connection */
        RPCconnection rpcCon = UserParams.getNewRPCconnection();

        /* Create new BlockBuffer */
        BlockBuffer blockBuffer = UserParams.getNewBlockBuffer(rpcCon);

        /* Create new StatisticsBlocks */
        StatisticsUtxoSet statisticsStatic = UserParams.STAT_UTXOSET_getNewStatisticsUtxoSet(
                blockBuffer, utxoSet);

        /* Hook SIGINT to close() */
        statisticsStatic.hookSIGINTtoClose();

        /* Print statistics parameters */
        statisticsStatic.printParameters(PRINT_STREAM);
        PRINT_STREAM.println("\n");

        /* Print statistics state */
        statisticsStatic.print(PRINT_STREAM);
        PRINT_STREAM.println("\n");

        /* Write statistics information file */
        statisticsStatic.writeStatInfo();

        /* Run stats */
        try {
            statisticsStatic.runStats();
        } finally {
            statisticsStatic.close();
        }
    }
}
