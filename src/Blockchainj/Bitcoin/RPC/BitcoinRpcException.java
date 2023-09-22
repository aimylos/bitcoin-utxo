package Blockchainj.Bitcoin.RPC;


/**
 * Simple exception for Blockchainj.Bitcoin RPC-JSON HTTP status code errors or other RPC related issues.
 */


public class BitcoinRpcException extends Exception{
    private final int responseCode;
    private String blockhash;
    private int height;


    public BitcoinRpcException(String message) {
        super(message);
        this.responseCode = 0;
        this.blockhash = "";
        this.height = -1;
    }

    public BitcoinRpcException(String message, int responseCode, Exception e) {
        super(message, e);
        this.responseCode = responseCode;
        this.blockhash = "";
        this.height = -1;
    }

    public BitcoinRpcException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
        this.blockhash = "";
        this.height = -1;
    }


    public BitcoinRpcException(String message, Exception e) {
        super(message, e);
        this.responseCode = 0;
        this.blockhash = "";
        this.height = -1;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\nBlockhash: " + blockhash + "\nHeight: " + height +
                "\nResponse code: " + responseCode;
    }

    public int getResponseCode() { return responseCode; }

    public String getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public void setBlockhash(String blockhash) { this.blockhash = blockhash; }

    public void setHeight(int height) { this.height = height; }
}
