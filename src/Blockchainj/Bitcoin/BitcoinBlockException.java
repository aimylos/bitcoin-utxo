package Blockchainj.Bitcoin;

/**
 * Blockchainj.Bitcoin block exception for parsing and other block issues.
 */

public class BitcoinBlockException extends Exception {
    private final String blockhash;
    private final int height;
    private boolean outIndexSet = false;
    private int outIndex;
    private boolean offsetSet = false;
    private int offset;
    private String txidB4Error = null;
    private boolean offsetB4ErrorSet = false;
    private int offsetB4Error;


    public BitcoinBlockException(String message, String blockhash, int height, Exception e) {
        super(message, e);
        this.blockhash = blockhash;
        this.height = height;
    }

    public BitcoinBlockException(String message, String blockhash, int height) {
        super(message);
        this.blockhash = blockhash;
        this.height = height;
    }


    @Override
    public String getMessage() {
        String str = "\nBlockhash: " + blockhash + "\nHeight: " + height;
        if (outIndexSet) {
            str += "\nOutIndex: " + outIndex;
        }
        if (offsetSet) {
            str += "\nOffset: " + offset;
        }
        if (txidB4Error != null) {
            str += "\nTxid before error: " + txidB4Error;
        }
        if (offsetB4ErrorSet) {
            str += "\nOffset before error: " + offsetB4Error;
        }

        return super.getMessage() + str;
    }


    public String getBlockhash() { return blockhash; }

    public int getHeight() { return height; }

    public void setOutIndex(int outIndex) {
        this.outIndex = outIndex;
        outIndexSet = true;
    }

    public void setOffset(int offset) {
        this.offset = offset;
        offsetSet = true;
    }

    public void setTxidB4Error(String txidB4Error) {
        this.txidB4Error = txidB4Error;
    }

    public void setOffsetB4Error(int offsetB4Error) {
        this.offsetB4Error = offsetB4Error;
        offsetB4ErrorSet = true;
    }
}
