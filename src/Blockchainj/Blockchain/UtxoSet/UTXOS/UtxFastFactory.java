package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class UtxFastFactory implements UtxFactory {
    @Override
    public UTX getNewUTX(Transaction transaction) {
        return new UtxFast(transaction);
    }

    @Override
    public UTX deserialize(InputStream inputStream) throws IOException {
        return UtxFast.deserialize(inputStream);
    }

    @Override
    public UTX load(InputStream inputStream) throws IOException {
        return UtxFast.load(inputStream);
    }

    @Override
    public void printUtxType(PrintStream printStream) {
        printStream.println("UTX type: " + UtxFast.class.toString());
    }
}
