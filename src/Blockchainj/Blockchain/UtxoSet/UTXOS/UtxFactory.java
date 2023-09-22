package Blockchainj.Blockchain.UtxoSet.UTXOS;

import Blockchainj.Bitcoin.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * UtxFactory
 *
 * UTX instantiation methods.
 */

public interface UtxFactory {
    UTX getNewUTX(Transaction transaction);

    UTX deserialize(InputStream inputStream) throws IOException;

    UTX load(InputStream inputStream) throws IOException;

    void printUtxType(PrintStream printStream);
}


//    /**
//     * UTX instantiation methods.
//     * Use these to have a consistent type of UTX throughout the UtxoSet package.
//     */
//    static void SET_UTX_TYPE(int UTX_TYPE) { UtxType.SET_UTX_TYPE(UTX_TYPE); }
//
//    static int GET_UTX_TYPE() { return UtxType.GET_UTX_TYPE(); }
//
//
//    /* Get new UTX instance */
//    static UTX getNewUTX(Transaction transaction) {
//        UtxType.LOCK_UTX_TYPE();
//        return getNewUTX(transaction, GET_UTX_TYPE());
//    }
//
//    static UTX getNewUTX(Transaction transaction, int UTX_TYPE) {
//        switch (UTX_TYPE) {
//            case UtxFast.UTX_TYPE:
//                return new UtxFast(transaction);
//
//            case UtxCompact.UTX_TYPE:
//                return new UtxCompact(transaction);
//
//            default:
//                throw new IllegalArgumentException("Invalid utx type");
//        }
//    }
//
//
//    /* Deserialize UTX */
//    static UTX deserialize(InputStream inputStream) throws IOException {
//        UtxType.LOCK_UTX_TYPE();
//        return deserialize(inputStream, GET_UTX_TYPE());
//    }
//
//    static UTX deserialize(InputStream inputStream, int UTX_TYPE) throws IOException {
//        switch (UTX_TYPE) {
//            case UtxFast.UTX_TYPE:
//                return UtxFast.deserialize(inputStream);
//
//            case UtxCompact.UTX_TYPE:
//                return UtxCompact.deserialize(inputStream);
//
//            default:
//                throw new IllegalArgumentException("Invalid utx type");
//        }
//    }
//
//
//    /* Load UTX */
//    static UTX load(InputStream inputStream) throws IOException {
//        UtxType.LOCK_UTX_TYPE();
//        return load(inputStream, GET_UTX_TYPE());
//    }
//
//    static UTX load(InputStream inputStream, int UTX_TYPE) throws IOException {
//        switch (UTX_TYPE) {
//            case UtxFast.UTX_TYPE:
//                return UtxFast.load(inputStream);
//
//            case UtxCompact.UTX_TYPE:
//                return UtxCompact.load(inputStream);
//
//            default:
//                throw new IllegalArgumentException("Invalid utx type");
//        }
//    }
//
//
//    /* Print UTX_TYPE */
//    static void printUtxType(PrintStream printStream) {
//        UtxType.LOCK_UTX_TYPE();
//        printUtxType(printStream, GET_UTX_TYPE());
//    }
//
//    static void printUtxType(PrintStream printStream, int UTX_TYPE) {
//        final String s = "UTX_TYPE " + UTX_TYPE + ": ";
//        switch (UTX_TYPE) {
//            case UtxFast.UTX_TYPE:
//                printStream.println(s + UtxFast.class.toString());
//                break;
//            case UtxCompact.UTX_TYPE:
//                printStream.println(s + UtxCompact.class.toString());
//                break;
//            default:
//                printStream.println("Unknown UTX TYPE");
//        }
//    }