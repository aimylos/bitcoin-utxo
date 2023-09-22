package Blockchainj.Blockchain.Main;

import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Bitcoin.RPC.RPCconnection;
import Blockchainj.Blockchain.Statistics.StatisticsBlocks;

import java.io.IOException;
import java.io.PrintStream;

/**
 *  MainStatisticsBlocks
 *
 *  This class computes statistics for blocks.
 *  Uses UserParams to configure parameters.
 */

public class MainStatisticsBlocks {

    public static void main(String[] args) throws IOException {
        /* Using UserParams for parameters */
        UserParams.loadOrWriteUserParams(args);

        /* Set Shard and UTX Types */
        UserParams.setShardAndUtxTypes();

        /* init print stream */
        PrintStream PRINT_STREAM = UserParams.PRINT_STREAM;

        /* Create new RPC connection object */
        RPCconnection rpcCon = UserParams.getNewRPCconnection();

        /* Create new BlockBuffer */
        BlockBuffer blockBuffer = UserParams.getNewBlockBuffer(rpcCon);

        /* Create new StatisticsBlocks */
        StatisticsBlocks statisticsBlocks =
                UserParams.STAT_STATIC_getNewStatisticsBlocks(blockBuffer);

        /* Hook SIGINT to close() */
        statisticsBlocks.hookSIGINTtoClose();

        /* Print statistics parameters */
        statisticsBlocks.printParameters(PRINT_STREAM);
        PRINT_STREAM.println("\n");

        /* Print statistics state */
        statisticsBlocks.print(PRINT_STREAM);
        PRINT_STREAM.println("\n");

        /* Write statistics information file */
        statisticsBlocks.writeStatInfo();

        /* Run stats */
        try {
            statisticsBlocks.runStats();
        } finally {
            statisticsBlocks.close();
        }
    }
}
