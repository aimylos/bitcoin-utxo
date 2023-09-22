package Blockchainj.Blockchain.Main;

import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Bitcoin.RPC.RPCconnection;
import Blockchainj.Blockchain.Blockchain;
import Blockchainj.Blockchain.Server.BlockchainServer;
import Blockchainj.Blockchain.UtxoSet.UtxoSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/**
 *  Main - Main Prototype Protocol Utxo Set build and server.
 *
 *  This class contains the main method to build the utxo set and optionally run the server.
 *  Uses UserParams to configure parameters.
 */

public class Main {

    public static void main(String[] args) {
        /* Using UserParams for parameters */
        UserParams.loadOrWriteUserParams(args);

        /* Set Shard and UTX Types */
        UserParams.setShardAndUtxTypes();

        //noinspection UnusedAssignment
        PrintStream printStream = System.out;
        //noinspection UnusedAssignment
        BlockBuffer blockBuffer = null;
        //noinspection UnusedAssignment
        UtxoSet utxoSet = null;
        //noinspection UnusedAssignment
        Blockchain blockchain = null;
        //noinspection UnusedAssignment
        BlockchainServer blockchainServer = null;

        try {
            /* Set printstream */
            printStream = UserParams.PRINT_STREAM;

            /* Create utxo set. */
            try {
                utxoSet = UserParams.tryOpenUtxoSet(UserParams.getUtxoSetPath("BLOCKCHAIN"));
            } catch (FileNotFoundException e) {
                utxoSet = UserParams.createNewUtxoSet(UserParams.getUtxoSetPath("BLOCKCHAIN"));
            }

            /* Create new RPC connection object */
            RPCconnection rpcCon = UserParams.getNewRPCconnection();

            /* Create new BlockBuffer */
            blockBuffer = UserParams.getNewBlockBuffer(rpcCon);

            /* Create blockchain. */
            blockchain = UserParams.BLOCKCHAIN_getNewBlockchain(utxoSet, blockBuffer);

            /* Create a blockchain server. */
            blockchainServer = UserParams.BLOCKCHAIN_getNewBlockchainServer(utxoSet);

            /* Hook SIGINT to close safely */
            blockchain.hookSIGINTtoClose();
            if(blockchainServer != null) {
                blockchainServer.hookSIGINTtoClose();
            }

            /* Start server */
            if(blockchainServer != null) {
                blockchainServer.start();
            }


            /* Print Blockchain */
            blockchain.printParameters(printStream);
            printStream.println("\n");
            blockchain.print(printStream);
            printStream.println("\n\n");

            /* Build blockchain */
            blockchain.buildBlockchain();

            blockchain.print(printStream);
            printStream.println("\n\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            /* Blockchain and BlockchainServer should close uppon INTSIG from JVM automatically.
             * Never the less close check. */
            if(blockchain != null) {
                if(!blockchain.isClosed()) {
                    try {
                        blockchain.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(blockchainServer != null) {
                if(!blockchainServer.isClosed()) {
                    try {
                        blockchainServer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(blockBuffer != null) {
                if(!blockBuffer.isClosed()) {
                    blockBuffer.close();
                }
            }

        }
    }
}
