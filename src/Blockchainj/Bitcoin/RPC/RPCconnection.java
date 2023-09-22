package Blockchainj.Bitcoin.RPC;


import Blockchainj.Bitcoin.BitcoinParams;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Perfoms RPC-JSON requests over a HTTP connection with the bitcoind server.
 *
 * Thread-safe.
 *
 */


public class RPCconnection {
    /* JSON-RPC variables */
    private final String rpcAuth;
    private final String rpcAddr;
    private final URL rpcURL;
    protected final long timeoutMillis = 10000; //10 seconds

    /* JSON parsing variables */
    private final static Pattern SHA256ResultPat =
            Pattern.compile("(\\\"result\\\"\\s*:\\s*\\\")([0-9a-fA-F]{64})");
    private final static Pattern rawBlockResultPat =
            Pattern.compile("(\\\"result\\\"\\s*:\\s*\\\")([0-9a-fA-F]+)(\\\")");
    private final static Pattern heightResultPat =
            Pattern.compile("(\\\"result\\\"\\s*:\\s*)([0-9]+)([^0-9]*,)");

    /* defualt JSON-RPC infos */
    final private static String defaultRPCip = "127.0.0.1";
    final private static String defaultRPCport = "8332";
    final private static String defaultRPCuser = "user";
    final private static String defaultRPCpassword = "user";

    /* Const variables */
    public static final String genesisHash = BitcoinParams.GENESIS_BLOCKHASH.toString();
    public static final String secondHash = BitcoinParams.SECOND_BLOCKHASH.toString();

    /* Semaphore for limiting concurrent requests */
    private final Semaphore semaphore;
    private final int maxConcurrentRequests;
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1;



    /* Main constructor. */
    public RPCconnection(String rpcip, String rpcport, String rpcuser, String rpcpassword) {
        /* Init rpc address -- http://127.0.0.1:8332 */
        rpcAddr = "http://" + rpcip + ":" + rpcport;

        /* create URL */
        try {
            rpcURL = new URL(rpcAddr);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        /* encode athendication to base 64 */
        String tempAuth = rpcuser + ":" + rpcpassword;
        rpcAuth = Base64.getEncoder().encodeToString(tempAuth.getBytes());

        /* set semaphore */
        if(DEFAULT_MAX_CONCURRENT_REQUESTS < 0) {
            semaphore = null;
            this.maxConcurrentRequests = -1;
        } else {
            /* fair (FIFO) semaphore */
            semaphore = new Semaphore(DEFAULT_MAX_CONCURRENT_REQUESTS, true);
            this.maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
        }
    }


    /* Defualt RPC info constructor. */
    public RPCconnection() {
        this(defaultRPCip, defaultRPCport, defaultRPCuser, defaultRPCpassword);
    }


    /* Performs a JSON-RPC request and returns respond if response code was 200. */
    protected BufferedReader doQuery(String params) throws BitcoinRpcException {
        HttpURLConnection rpcCon;

        try {
            /* Get permit from semaphore */
            if(semaphore != null) {
                semaphore.acquire();
            }

            /* Create connection. Supposed to be thread-safe. */
            rpcCon = (HttpURLConnection) rpcURL.openConnection();

            /* set header */
            rpcCon.setRequestMethod("POST");
            rpcCon.setRequestProperty("Content-Type", "application/json");
            rpcCon.setRequestProperty("Accept", "application/json");
            rpcCon.setRequestProperty("Authorization", "Basic " + rpcAuth);

            /* Set timeouts */
            rpcCon.setConnectTimeout((int)timeoutMillis);
            rpcCon.setReadTimeout((int)timeoutMillis);

            /* send post request */
            rpcCon.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(rpcCon.getOutputStream());
            wr.writeBytes(params);
            wr.flush();
            wr.close();

            /* get response code */
            int responseCode = rpcCon.getResponseCode();
            if (responseCode == 200) {
                return new BufferedReader(new InputStreamReader(rpcCon.getInputStream()));
                //return IOUtils.toByteArray(rpcCon.getInputStream());
            }
            else {
                throw new BitcoinRpcException("Response code not 200.", responseCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new BitcoinRpcException("HttpURLConnction exception.", e);
        } finally {
            /* Release permit */
            if(semaphore != null) {
                semaphore.release();
            }
        }
    }


    /* Query method */
    public String getBlockhashByHeightString(int height) throws BitcoinRpcException {
        return Hex.encodeHexString(getBlockhashByHeight(height));
    }

    public byte[] getBlockhashByHeight(int height) throws BitcoinRpcException {
        /* Prepare JSON request parameters. */
        String jsonParams =
                "{\"jsonrpc\": \"1.0\", \"id\":\"1\", \"method\":\"getblockhash\", \"params\":[" +
                        Integer.toString(height) + "]}";

        /* Do request and get response. Will throw BitcoinRpcException if it fails. */
        BufferedReader resp = doQuery(jsonParams);

        /* Read the one line JSON response. */
        String jsonResp;
        try {
            jsonResp = resp.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* Parse the JSON response and get the blockhash */
        String SHA256Hash;
        try {
            SHA256Hash = getSHA256FromJsonResponse(jsonResp);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        try {
            return Hex.decodeHex(SHA256Hash);
        } catch (DecoderException e) {
            throw new BitcoinRpcException("Hex decoder failed.", e);
        }
    }


    /* Query method */
    public String getRawBlockByBlockhashString(String blockhash) throws BitcoinRpcException {
        return Hex.encodeHexString(getRawBlockByBlockhash(blockhash));
    }

    public byte[] getRawBlockByBlockhash(String blockhash) throws BitcoinRpcException {
        /* Prepare JSON request parameters. */
        String jsonParams =
                "{\"jsonrpc\": \"1.0\", \"id\":\"1\", \"method\":\"getblock\", \"params\":[\"" +
                        blockhash + "\", 0]}";

        /* Do request and get response. Will throw BitcoinRpcException if it fails. */
        BufferedReader resp = doQuery(jsonParams);

        /* Read the one line JSON response. */
        String jsonResp;
        try {
            jsonResp = resp.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* Parse the JSON response and get the raw block */
        String rawblock;
        try {
            rawblock = getRawBlockFromJsonResponse(jsonResp);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        try {
            return Hex.decodeHex(rawblock);
        } catch (DecoderException e) {
            throw new BitcoinRpcException("Hex decoder failed.", e);
        }
    }


    /* Query method */
    public String getBestBlockhashString() throws BitcoinRpcException {
        return Hex.encodeHexString(getBestBlockhash());
    }

    public byte[] getBestBlockhash() throws BitcoinRpcException {
        /* Prepare JSON request parameters. */
        String jsonParams =
                "{\"jsonrpc\": \"1.0\", \"id\":\"1\", \"method\":\"getbestblockhash\", " +
                        "\"params\":[]}";

        /* Do request and get response. Will throw BitcoinRpcException if it fails. */
        BufferedReader resp = doQuery(jsonParams);

        /* Read the one line JSON response. */
        String jsonResp;
        try {
            jsonResp = resp.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* Parse the JSON response and get the blockhash */
        String SHA256Hash;
        try {
            SHA256Hash = getSHA256FromJsonResponse(jsonResp);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        try {
            return Hex.decodeHex(SHA256Hash);
        } catch (DecoderException e) {
            throw new BitcoinRpcException("Hex decoder failed.", e);
        }
    }


    /* Query method */
    public int getBlockCount() throws BitcoinRpcException {
        /* Prepare JSON request parameters. */
        String jsonParams =
                "{\"jsonrpc\": \"1.0\", \"id\":\"1\", \"method\":\"getblockcount\", " +
                        "\"params\":[]}";

        /* Do request and get response. Will throw BitcoinRpcException if it fails. */
        BufferedReader resp = doQuery(jsonParams);

        /* Read the one line JSON response. */
        String jsonResp;
        try {
            jsonResp = resp.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* Parse the JSON response and get the blockhash */
        int bestHeight;
        try {
            bestHeight = getHeightFromJsonResponse(jsonResp);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        return bestHeight;
    }



    /* Parsing method */
    private String getSHA256FromJsonResponse(String jsonResponse) throws IllegalArgumentException{
        /* make matcher for SHA256 hash */
        Matcher match = SHA256ResultPat.matcher(jsonResponse);
        if(match.find()) {
            return match.group(2).toLowerCase();
        }
        else {
            throw new IllegalArgumentException("SHA256 hash not found in Json response.");
        }
    }


    /* Parsing method */
    private String getRawBlockFromJsonResponse(String jsonResponse)
            throws IllegalArgumentException {
        /* make matcher for raw data */
        Matcher match = rawBlockResultPat.matcher(jsonResponse);
        if(match.find()) {
            return match.group(2).toLowerCase();
        }
        else {
            throw new IllegalArgumentException("Raw block data not found in Json response.");
        }
    }


    /* Parsing method */
    private int getHeightFromJsonResponse(String jsonResponse)
            throws IllegalArgumentException {
        /* make matcher for raw data */
        Matcher match = heightResultPat.matcher(jsonResponse);
        if(match.find()) {
            return  Integer.parseInt(match.group(2));
        }
        else {
            throw new IllegalArgumentException("Height not found in Json response.");
        }
    }


    public String getRpcAddr() {
        return rpcAddr;
    }

    public long getTimeoutMillis() { return timeoutMillis; }


    public void printParameters(PrintStream printStream) {
        printStream.println(">RPC Connection parameters:");
        printStream.println("RPC Connection rpcAuth: " + rpcAuth);
        printStream.println("RPC Connection rpcAddr: " + rpcAddr);
        printStream.println("RPC Connection rpcUrl: " + rpcURL.getHost());
        printStream.println("RPC Connection max concurrent requests: " + maxConcurrentRequests);
    }


//    public static void main(String[] args){
//        RPCconnection o = new RPCconnection();
//
//        try {
//            //System.out.println(o.getRpcAddr());
//            System.out.println(o.getBlockhashByHeightString(0));
//            System.out.println(o.getRawBlockByBlockhashString(RPCconnection.genesisHash));
//            //System.out.println(o.getRawBlockByBlockhash(
//            // "00000000000000000024fb37364cbf81fd49cc2d51c09c75c35433c3a1945d04"));
//            System.out.println(o.getBestBlockhashString());
//            System.out.println(o.getBlockCount());
//        } catch (BitcoinRpcException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
