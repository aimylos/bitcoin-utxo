package Blockchainj.Blockchain.Server;

import Blockchainj.Blockchain.ProtocolParams;
import Blockchainj.Blockchain.UtxoSet.*;
import Blockchainj.Blockchain.UtxoSet.Shard.Shard;
import Blockchainj.Util.MerkleTree;
import Blockchainj.Util.SHA256HASH;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;

/**
 * BlockchainServer
 *
 * Listens for Prototype Protocol requests from the network.
 *
 * This class requires read access to a AbstractUtxoSet.
 *
 * Requests:
 * - Get Utxo Set Merkle tree root for any height.
 * - Get Utxo Set Merkle tree root for best height along with height.
 *
 * - Get Utxo Set Merkle tree for best height along with height.
 *
 * - Get Utxo Set best height.
 * - Get Utxo Set blockhash for best height along with height.
 * - Get Utxo Set blockhash for any height.
 *
 * - Get Utxo Set number of shards for any height.
 * - Get Utxo Set number of shard for best height along with height.
 *
 * - Get Utxo Set Shard for some index(es), for best height along with height.
 */

public class BlockchainServer extends Thread {
    /* Listeing port */
    public static final int DEFAULT_PORT = 8334;

    /* Max clients that can connect */
    public static final int DEFAULT_BACKLOG = 16;

    /* IP to bind */
    public static final String DEFAULT_BINDADDR = "127.0.0.1";

    /* AbstractUtxoSet. Read only access */
    private final UtxoSet utxoSet;

    /* Server Socket */
    private ServerSocket serverSocket;

    /* Closed marker */
    private volatile boolean closed = false;

    /* Server log filename and file */
    public static final String LOG_FILENAME = "blockchainServer.log";
    private final File logFile;


    /* Constructor */
    public BlockchainServer(String bindAddr, int port, int backlog, UtxoSet utxoSet, String logPath)
            throws IOException {
        if(utxoSet == null) {
            throw new NullPointerException("AbstractUtxoSet cannot be null.");
        }

        /* Set utxo set */
        this.utxoSet = utxoSet;

        /* Log file */
        logFile = Paths.get(logPath, LOG_FILENAME).toFile();
        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
            logFile.createNewFile();
        }

        /* Make new server socket */
        try {
            serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(bindAddr));
        } catch (IOException e) {
            writeToLog(e.getMessage());
            throw e;
        }
    }


    /* Default constructor */
    public BlockchainServer(UtxoSet utxoSet, String logPath) throws IOException {
        this(DEFAULT_BINDADDR, DEFAULT_PORT, DEFAULT_BACKLOG, utxoSet, logPath);
    }


    /* Default constructor */
    public BlockchainServer(String bindAddr, int port, UtxoSet utxoSet, String logPath)
            throws IOException {
        this(bindAddr, port, DEFAULT_BACKLOG, utxoSet, logPath);
    }


    /* Close server */
    public synchronized void close() throws IOException {
        serverSocket.close();
        closed = true;
    }


    public synchronized boolean isClosed() {
        return closed;
    }


    /* Hook SIGINT to close safely. */
    public synchronized void hookSIGINTtoClose() {
        //noinspection AnonymousHasLambdaAlternative
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    /* Accept one client at a time.
     *  - Read up to 1 Blockchainj.Bitcoin Message request.
     *  - Respond to the request.
     *  - End connection. */
    @Override
    public void run() {
        /* Terminal error handler. */
        try {
            /* Main loop. */
            //noinspection InfiniteLoopStatement
            while (!isClosed()) {
                /* Accept new client */
                Socket socket = serverSocket.accept();
                writeToLog("New Client: \n" + socket.toString());

                /* Handle client */
                try {
                    Message message;
                    Message response;
                    boolean readMessage = false;

                    try {
                        /* Read Message */
                        writeToLog("Reading message...");
                        message = Message.deserializeFromSocket(socket.getInputStream());
                        writeToLog("Read message:: " + message.toString());
                        readMessage = true;

                        /* Make response */
                        writeToLog("Computing response...");
                        response = doResponse(message);
                        writeToLog("Response Computed.");
                    } catch (IOException | IllegalArgumentException e) {
                        /* Make error response */
                        if(!readMessage) {
                            writeToLog("Failed to read message. " + e.toString());
                        } else {
                            writeToLog("Failed to compute response based on message."
                                    + e.toString());
                        }
                        writeToLog("Computing error response...");
                        response = doResponse(e);
                        writeToLog("Error response computed.");
                    }

                    /* Send response message */
                    writeToLog("Sending response message::" + response.toString());
                    response.serializeToSocket(socket.getOutputStream());
                    writeToLog("Response message sent.");
                } catch (IOException e) {
                    /* Log error */
                    writeToLog("Failed to handle client. " + e.toString());
                } finally {
                    /* Close client */
                    writeToLog("Closing connection...");
                    socket.close();
                    writeToLog("Closed Client:" + socket.toString() + "\n\n");
                }
            }
        } catch (Exception e) {
            /* Handle server errors */
            try {
                writeToLog(e.getMessage());
            } catch (IOException e2) {
                throw new RuntimeException(e);
            }
        } finally {
            /* Close server */
            try {
                serverSocket.close();
                writeToLog("Closing server.\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /* Print string to log file */
    private void writeToLog(String msg) throws IOException {
        PrintWriter pr = new PrintWriter(new FileOutputStream(logFile, true), true);
        pr.println(msg);
        pr.close();
    }


    /* Read request message and make response message */
    private Message doResponse(Message message) throws IOException {
        /* Expecting GetCustom message. */
        if(!MessageGetCustom.isGetCustom(message)) {
            writeToLog("Read message is not 'getcustom' message.");
            return MessageReject.getRejectMessage(message.getCommand(), (byte)0x01,
                    "Expected getcustom message".getBytes(), null);
        }

        MessageGetCustom messageGetCustom;
        MessageDataCustom response;
        boolean messageParsed = false;
        try {
            /* Parse message */
            messageGetCustom = new MessageGetCustom(message);
            messageParsed = true;
            writeToLog("Read message parsed successfully to 'getcustom' message: "
                    + messageGetCustom.toString());

            /* Compute response */
            if(messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BESTHEIGHT)) {
                response = doBestheight();
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BESTBLKHASH)) {
                response = doBestblkhash();
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BLOCKHASH)) {
                response = doBlockhash(messageGetCustom);
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BESTMRKLROOT)) {
                response = doBestmrklroot();
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_MERKLEROOT)) {
                response = doMerkleroot(messageGetCustom);
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BESTMRKLTREE)) {
                response = doBestmrkltree();
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BESTSHARDNUM)) {
                response = doBestshardnum();
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_SHARDNUM)) {
                response = doShardnum(messageGetCustom);
            }
            else if (messageGetCustom.isRequestType(ProtocolParams.MESSAGE_TYPE_BESTSHARD)) {
                response = doBestshard(messageGetCustom);
            }
            else {
                throw new IllegalArgumentException("Request type '" +
                        messageGetCustom.getRequestTypeString() + "' not found.");
            }

            writeToLog("Respone computed succesfully to 'datacustom' message: " +
                    response.toString());

            return response;
        } catch (IllegalArgumentException e) {
            if (!messageParsed) {
                writeToLog("Read message did not parsed successfully to 'getcustom' message: "
                        + e.toString());
                return doResponse(e);
            } else {
                writeToLog("Failed to compute response." + e.toString());
                return doResponse(e);
            }
        }
    }


    /* Make an error response message */
    private Message doResponse(Exception e) throws IOException {
        return MessageReject.getRejectMessage(null, (byte)0x01, e.getMessage().getBytes(), null);
    }


    /* Computes 'bestheight' response */
    private MessageDataCustom doBestheight() {
        int[] height = new int[1];

        /* Read height */
        height[0] = utxoSet.getBestHeight();

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_BESTHEIGHT, height);
    }


    /* Computes 'bestblkhash' response */
    private MessageDataCustom doBestblkhash() {
        SHA256HASH[] besthash = new SHA256HASH[1];

        /* Read blockhash */
        besthash[0] = utxoSet.getBestBlockhash();

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_BESTBLKHASH, besthash);
    }


    /* Computes 'blockhash' response */
    private MessageDataCustom doBlockhash(MessageGetCustom request) {
        SHA256HASH[] hashList = new SHA256HASH[request.getListElementCount()];

        /* Read blockhash list */
        for(int i=0; i<hashList.length; i++) {
            try {
                hashList[i] = utxoSet.getBlockhash(request.getInt32ListByIndex(i));
            } catch (IllegalArgumentException e) {
                hashList[i] = SHA256HASH.getZeroHash();
            }
        }

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_BLOCKHASH, hashList);
    }


    /* Computes 'bestmrklroot' response */
    private MessageDataCustom doBestmrklroot() {
        SHA256HASH[] besthash = new SHA256HASH[1];

        //TODO Returns internal shardNum's. This request must accept ShardNum.
        /* Read Merkle tree root */
        try {
            besthash[0] = utxoSet.getMerkleTree(
                    ((AbstractUtxoSet) utxoSet).getInternalBestShardNum()).getRoot();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_BESTMRKLROOT, besthash);
    }


    /* Computes 'merkleroot' response */
    private MessageDataCustom doMerkleroot(MessageGetCustom request) {
        SHA256HASH[] hashList = new SHA256HASH[request.getListElementCount()];


        /* Read merkle root list */
        //TODO THIS REQUEST IS INVALID
        for(int i=0; i<hashList.length; i++) {
            try {
                hashList[i] = SHA256HASH.getZeroHash();
                //utxoSet.getMerkleRoot(request.getInt32ListByIndex(i));
            } catch (IllegalArgumentException e) {
                hashList[i] = SHA256HASH.getZeroHash();
            }
        }

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_MERKLEROOT, hashList);
    }


    /* Computes 'bestmrkltree' response */
    private MessageDataCustom doBestmrkltree() {
        MerkleTree[] merkleTreeList = new MerkleTree[1];
        MessageDataCustom response;


        //TODO Returns internal shardNum's. This request must accept ShardNum.
        /* Read merkle tree */
        try {
            merkleTreeList[0] = utxoSet.getMerkleTree(
                    ((AbstractUtxoSet) utxoSet).getInternalBestShardNum());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        /* Must create message in here, else merkle tree could change state. */
        response = MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_BESTMRKLTREE, merkleTreeList);

        /* Return message */
        return response;
    }


    /* Computes 'bestshardnum' response */
    private MessageDataCustom doBestshardnum() {
        int[] index = new int[1];

        //TODO THIS REQUEST IS ONLY VALID IF SHARD NUMBER IS A FIXED NUMBER
        /* Read height */
        index[0] = ProtocolParams.UNDEFINED_SHARD_INDEX;

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_BESTSHARDNUM, index);
    }


    /* Computes 'shardnum' response */
    private MessageDataCustom doShardnum(MessageGetCustom request) {
        int[] indexList = new int[request.getListElementCount()];

        //TODO THIS REQUEST IS ONLY VALID IF SHARD NUMBER IS A FIXED NUMBER
        /* Read blockhash list */
        for(int i=0; i<indexList.length; i++) {
            try {
                indexList[i] = ProtocolParams.UNDEFINED_SHARD_INDEX;
            } catch (IllegalArgumentException e) {
                indexList[i] = ProtocolParams.UNDEFINED_SHARD_INDEX;
            }
        }

        /* Return message */
        return MessageDataCustom.getMessageDataCustom(
                ProtocolParams.MESSAGE_TYPE_SHARDNUM, indexList);
    }


    /* Computes 'bestshard' response */
    private MessageDataCustom doBestshard(MessageGetCustom request) {
        Shard[] shardList = new Shard[request.getListElementCount()];
        MessageDataCustom response;

        //TODO Returns internal shardNum's. This request must accept ShardNum.
        /* Read blockhash list */
        for(int i=0; i<shardList.length; i++) {
            try {
                shardList[i] = utxoSet.getShard(
                        ((AbstractUtxoSet) utxoSet).getInternalBestShardNum(),
                        request.getInt32ListByIndex(i));
                //shardList[i] = utxoSet.getShard(16384, request.getInt32ListByIndex(i));
            //} catch (IOException | IllegalArgumentException e) {
            } catch (IOException e) {
                /* Invalidate the whole message */
                throw new IllegalArgumentException(e);
            }
        }

        /* This is why shards and UTXs should be readonly, else shards could change state. */
        try {
            response = MessageDataCustom.getMessageDataCustom(
                    ProtocolParams.MESSAGE_TYPE_BESTSHARD, shardList);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        /* Return message */
        return response;
    }

}