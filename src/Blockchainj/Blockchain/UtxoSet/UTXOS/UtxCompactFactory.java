package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class UtxCompactFactory implements UtxFactory {
    @Override
    public UTX getNewUTX(Transaction transaction) {
        return new UtxCompact(transaction);
    }

    @Override
    public UTX deserialize(InputStream inputStream) throws IOException {
        return UtxCompact.deserialize(inputStream);
    }

    @Override
    public UTX load(InputStream inputStream) throws IOException {
        return UtxCompact.load(inputStream);
    }

    @Override
    public void printUtxType(PrintStream printStream) {
        printStream.println("UTX type: " + UtxCompact.class.toString());
    }
}
