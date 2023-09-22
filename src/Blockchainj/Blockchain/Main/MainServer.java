package Blockchainj.Blockchain.Main;


import Blockchainj.Blockchain.Server.BlockchainServer;
import Blockchainj.Blockchain.UtxoSet.UtxoSetIO;

import java.io.IOException;
import java.io.PrintStream;

/**
 *  Main Server - Main Prototype Protocol server.
 *
 *  This class contains the main method to run the server.
 *  Uses UserParams to configure parameters.
 */

public class MainServer {

    public static void main(String[] args) {
        /* Using UserParams for parameters */
        UserParams.loadOrWriteUserParams(args);

        /* Set Shard and UTX Types */
        UserParams.setShardAndUtxTypes();

        //noinspection UnusedAssignment
        PrintStream printStream = null;
        //noinspection UnusedAssignment
        UtxoSetIO utxoSetIO = null;
        //noinspection UnusedAssignment
        BlockchainServer blockchainServer = null;

        try {
            /* Set print stream */
            printStream = UserParams.PRINT_STREAM;

            /* Create utxo set. User IO Utxo Set*/
            utxoSetIO = UserParams.BLOCKCHAIN_SERVER_getNewUtxoSetIO();

            /* Print message. No need to get lock! */
            utxoSetIO.print(printStream);

            /* Create a blockchain server. */
            blockchainServer = UserParams.BLOCKCHAIN_SERVER_getNewBlockchainServer(utxoSetIO);

            /* Hook SIGINT to close safely */
            blockchainServer.hookSIGINTtoClose();

            /* Print message */
            printStream.println("Starting server...");

            /* Start server */
            blockchainServer.start();

            /* Print message */
            printStream.println("Server started. Waiting for requests...");

            //noinspection InfiniteLoopStatement
            for(;;) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    /* Print message */
                    printStream.println("Server interrupted by " + e.getMessage());
                }
            }

            /* Close - will close uppon INTSIG from JVM automatically. */
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            /* Close server */
            try {
                closeServer(blockchainServer, printStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void closeServer(BlockchainServer blockchainServer, PrintStream printStream)
            throws IOException {
        if(blockchainServer == null) {
            /* Print message */
            printStream.println("No server to close.");
            return;
        }

        /* Print message */
        printStream.println("Closing server...");

        /* Close server safely */
        blockchainServer.close();

        /* Print message */
        printStream.println("Server closed. Exiting...");
    }
}
