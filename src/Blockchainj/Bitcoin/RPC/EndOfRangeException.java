package Blockchainj.Bitcoin.RPC;

/**
 * End of height range.
 */

@Deprecated
public class EndOfRangeException extends Exception{
    private boolean normalEndOfRangeException;

    public EndOfRangeException(String message) {
        super(message);
        normalEndOfRangeException = true;
    }

    public EndOfRangeException(String message, boolean normal) {
        super(message);
        normalEndOfRangeException = normal;
    }

    public EndOfRangeException(String message, Exception e) {
        super(message, e);
        normalEndOfRangeException = false;
    }

    public EndOfRangeException(Exception e) {
        super(e);
        normalEndOfRangeException = false;
    }

    public boolean isNormalEndOfRangeException() { return normalEndOfRangeException; }
}
