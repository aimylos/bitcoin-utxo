package Blockchainj.Blockchain.UtxoSet;


import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Deprecated
public class UtxoSetManager {
//    private boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT;
//
//    /* Utxo set */
//    private UtxoSetIO utxoSet;
//
//    /* Utxo set path */
//    private final String utxoSetPath;
//
//    /* Utxo set filenames */
//    private final UtxoSetFileNaming utxoSetFileNaming;
//
//    /* Utxo set log */
//    private final UtxoSetLog utxoSetLog;
//
//
//    /* Main constructor */
//    public UtxoSetManager(String utxoSetPath, boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT)
//            throws IOException {
//        this.DO_MERKLE_TREE_CHECKSUM_ON_INIT = DO_MERKLE_TREE_CHECKSUM_ON_INIT;
//        this.utxoSetPath = utxoSetPath;
//
//        try {
//            /* init */
//            utxoSetFileNaming = new UtxoSetFileNaming(utxoSetPath);
//            utxoSetLog = new UtxoSetLog(utxoSetFileNaming.getUtxoSetLogFilenameAsPath());
//
//            /* Check if utxo set exists */
//            UtxoSetLog.UtxoSetLogEntry info = utxoSetLog.getLastEntry();
//            if( (info == null) || (info.height < 0) ){
//                throw new IOException("No Utxo Set found or Utxo Set log not valid.");
//            }
//
//            /* Load utxo set.
//               Use UtxoSetIO so that load and init is only needed if
//               DO_MERKLE_TREE_CHECKSUM_ON_INIT is true. */
//            if(DO_MERKLE_TREE_CHECKSUM_ON_INIT) {
//                //noinspection ConstantConditions
//                utxoSet = new UtxoSetIO(utxoSetPath, -1, DO_MERKLE_TREE_CHECKSUM_ON_INIT);
//            } else {
//                utxoSet = new UtxoSetIO(utxoSetPath, "19d6689c085ae165");
//            }
//        } catch (IllegalArgumentException | BitcoinUtxoSetException e) {
//            throw new IOException(e);
//        }
//    }
//
//
//    /* Creates a utxo set manager */
//    public static UtxoSetManager getUtxoSetManager(String path,
//                                                   boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT,
//                                                   PrintStream printStream)
//            throws IOException {
//        if(printStream != null)
//            printStream.println("Loading utxo set...");
//
//        UtxoSetManager utxoSetManager = new UtxoSetManager(path, DO_MERKLE_TREE_CHECKSUM_ON_INIT);
//
//        if(printStream != null)
//            printStream.println("Utxo set loaded.");
//
//        return utxoSetManager;
//    }
//
//
//    private void getLock(PrintStream printStream) throws UtxoSetClosedException {
//        if(printStream != null)
//            printStream.println("Acquiring utxo set lock...");
//
//        utxoSet.lockWriteLock();
//
//        if(printStream != null)
//            printStream.println("Utxo set lock acquired.");
//    }
//
//    private void releaseLock(PrintStream printStream) {
//        if(printStream != null)
//            printStream.println("Releasing utxo set lock...");
//
//        utxoSet.unlockWriteLock();
//
//        if(printStream != null)
//            printStream.println("Lock released.");
//    }
//
//
//
//    /* Get best log entry */
//    public UtxoSetLog.UtxoSetLogEntry getBestUtxoSetLogEntry() throws IOException {
//        getLock(null);
//
//        try {
//            return utxoSetLog.getLastEntry();
//        } finally {
//            releaseLock(null);
//        }
//    }
//
//
//    /* Get entry at height */
//    public UtxoSetLog.UtxoSetLogEntry getUtxoSetLogEntry(int height) throws IOException {
//        getLock(null);
//
//        try {
//            return utxoSetLog.getEntry(height);
//        } finally {
//            releaseLock(null);
//        }
//    }
//
//
//    /* Rehash all shards, rebuild merkle tree and update utxo set log.
//       !May corrupt utxo set log.! */
//    public void reBuildMerkleTreeAndCommitToUtxoSetLog(PrintStream printStream) throws IOException {
//        getLock(printStream);
//
//        try {
//            if(printStream != null)
//                printStream.println("Rebuilding utxo set merkle tree and updating log...");
//
//            utxoSet.commitUtxoSetToDisk(true);
//
//            if(printStream != null)
//                printStream.println("Rebuilding utxo set merkle tree and updating log done.");
//        } finally {
//            if(printStream != null)
//                releaseLock(printStream);
//        }
//    }
//
//
//    public void convertUtxoSet2NewShardNum(int newShardNum, PrintStream printStream,
//                                           boolean printProgress)
//            throws IOException {
//        getLock(printStream);
//
//        try {
//            if(printStream != null)
//                printStream.println("Converting utxo set shardNum from " + utxoSet.getShardNum() +
//                        " to " + newShardNum + "...");
//
//            if(printProgress)
//                utxoSet.convertUtxoSet2NewShardNum(newShardNum, printStream);
//            else
//                utxoSet.convertUtxoSet2NewShardNum(newShardNum);
//
//            if(printStream != null)
//                printStream.println("Converting utxo set shardNum from " + utxoSet.getShardNum() +
//                        " to " + newShardNum + " done.");
//        } finally {
//            releaseLock(printStream);
//        }
//    }
//
//
//    /* Compares two utxo set merkle tree roots at given height */
//    public boolean equalsMerkleTreeRoot(UtxoSetManager utxoSetManager2,
//                                               int height,
//                                               PrintStream printStream) throws IOException {
//        getLock(printStream);
//
//        try {
//            UtxoSetLog.UtxoSetLogEntry entry1 = getUtxoSetLogEntry(height);
//            UtxoSetLog.UtxoSetLogEntry entry2 = utxoSetManager2.getUtxoSetLogEntry(height);
//            boolean match = entry1.merkleRoot.equals(entry2.merkleRoot);
//
//            if (printStream != null) {
//                if (match) {
//                    printStream.println("Merkle tree roots match at height " + height + "." +
//                            " Root:" + entry1.merkleRoot);
//                } else {
//                    printStream.println("Merkle tree roots do not match at height " + height + "." +
//                            " Root1:" + entry1.merkleRoot + "  Root2:" + entry2.merkleRoot);
//                }
//            }
//
//            return match;
//        } finally {
//            releaseLock(printStream);
//        }
//    }
//
//
//    public void printUtxoSet(PrintStream printStream) {
//        utxoSet.print(printStream);
//    }
//
//
//
//
//    public static void main(String[] args) throws IOException {
//        /* Using UserParams for parameters */
//        try {
//            CommandLine cmd = getCMD(args, false);
//            Iterator<Option> it = commands.iterator();
//
//            if(cmd.hasOption(rebuildMerkleTreeAndUpdateLogCmd)) {
//                String path = cmd.getOptionValue(rebuildMerkleTreeAndUpdateLogCmd);
//                rebuildMerkleTreeAndUpdateLog(path, System.out, true);
//            }
//
//            else if(cmd.hasOption(convertUtxoSetShardNumCmd)) {
//                String[] userArgs = cmd.getOptionValues(convertUtxoSetShardNumCmd);
//                System.out.println(userArgs[0]);
//                if(userArgs.length != 2) {
//                    getCMD(null, true);
//                    throw new IllegalArgumentException("Path or new shard number missing.");
//                }
//                convertUtxoSetShardNum(userArgs[0], Integer.parseInt(userArgs[1]),
//                        System.out, true, true);
//            }
//
//            else if(cmd.hasOption(compareBestMerkleTreeRootCmd)) {
//                String[] paths = cmd.getOptionValues(compareBestMerkleTreeRootCmd);
//                if(paths.length != 2) {
//                    getCMD(null, true);
//                    throw new IllegalArgumentException("Paths missing.");
//                }
//                compareBestMerkleTreeRoot(paths[0], paths[1], System.out, true, true);
//            }
//
//            else if(cmd.hasOption(getLogEntryCmd)) {
//                String[] userArgs = cmd.getOptionValues(getLogEntryCmd);
//                if(userArgs == null ) {
//                    getCMD(null, true);
//                    throw new IllegalArgumentException("Path missing.");
//                }
//                else if(userArgs.length == 1) {
//                    getLogEntry(userArgs[0], -1, true, System.out, true);
//                } else if(userArgs.length == 2) {
//                    getLogEntry(
//                            userArgs[0], Integer.parseInt(userArgs[1]), false, System.out, true);
//                } else {
//                    getCMD(null, true);
//                    throw new IllegalArgumentException("Path missing.");
//                }
//            }
//
//            else {
//                getCMD(null, true);
//            }
//        } catch (IllegalArgumentException e) {
//            if(e.getMessage() != null)
//                System.err.println(e.getMessage());
//            System.exit(1);
//        }
//    }
//
//
//    /* Gets command line arguments */
//    public static List<Option> commands = new LinkedList<>();
//    public static CommandLine getCMD(String[] args, boolean printHelp)
//            throws IllegalArgumentException {
//        /* User options */
//        Options options = new Options();
//        Iterator<Option> it = commands.iterator();
//        while(it.hasNext()) {
//            options.addOption(it.next());
//        }
//
//        CommandLineParser parser = new DefaultParser();
//        HelpFormatter formatter = new HelpFormatter();
//        CommandLine cmd = null;
//
//        //TODO Set shard and utx type
//
//        if(args != null) {
//            try {
//                cmd = parser.parse(options, args);
//            } catch (ParseException e) {
//                System.out.println(e.getMessage());
//                formatter.printHelp("Utxo set manager", options);
//                throw new IllegalArgumentException();
//            }
//        }
//
//        if(printHelp) {
//            formatter.printHelp("Utxo set manager", options);
//        }
//
//        return cmd;
//    }
//
//
//    /* Load utxo set and rebuild merkle tree */
//    public static final String rebuildMerkleTreeAndUpdateLogCmd = "rebld";
//    static {
//        String cmd = rebuildMerkleTreeAndUpdateLogCmd;
//        Option option = new Option(cmd, true,
//                "Use: " + cmd + " <utxo set path> \n" +
//                        "Loads utxo set without checksum, rebuilds the merkle tree " +
//                        "and updates the utxo set log. May corrupt the utxo set log.");
//        option.setArgs(1);
//        commands.add(option);
//    }
//    public static void rebuildMerkleTreeAndUpdateLog(String path,
//                                                     PrintStream printStream,
//                                                     boolean printLoadedUtxoSet)
//            throws IOException {
//        UtxoSetManager utxoSetManager = getUtxoSetManager(path, false, printStream);
//
//        if(printLoadedUtxoSet) {
//            utxoSetManager.printUtxoSet(printStream);
//        }
//
//        utxoSetManager.reBuildMerkleTreeAndCommitToUtxoSetLog(printStream);
//    }
//
//
//    /* Convert utxo set shard num */
//    public static final String convertUtxoSetShardNumCmd = "conv";
//    static {
//        String cmd = convertUtxoSetShardNumCmd;
//        Option option = new Option(cmd, true,
//                "Use: " + cmd + " <utxo set path> <new shard number> \n" +
//                        "Convert utxo set shard number to new shard number.");
//        option.setArgs(2);
//        commands.add(option);
//    }
//    public static void convertUtxoSetShardNum(String path, int newShardNum,
//                                              PrintStream printStream,
//                                              boolean printLoadedUtxoSet,
//                                              boolean printProgress)
//            throws IOException {
//        UtxoSetManager utxoSetManager = getUtxoSetManager(path, false, printStream);
//
//        if(printLoadedUtxoSet) {
//            utxoSetManager.printUtxoSet(printStream);
//        }
//
//        utxoSetManager.convertUtxoSet2NewShardNum(newShardNum, printStream, printProgress);
//    }
//
//
//    /* Compare best heights */
//    public static final String compareBestMerkleTreeRootCmd = "cmproot";
//    static {
//        String cmd = compareBestMerkleTreeRootCmd;
//        Option option = new Option(cmd, true,
//                "Use: " + cmd + " <utxo set path1> <utxo set path2> \n" +
//                        "Compares best merkle tree roots of both utxo set.");
//        option.setArgs(2);
//        commands.add(option);
//    }
//    public static void compareBestMerkleTreeRoot(String path1, String path2,
//                                                 PrintStream printStream,
//                                                 boolean printLoadedUtxoSet,
//                                                 boolean printEntries)
//            throws IOException {
//        UtxoSetManager utxoSetManager1 = getUtxoSetManager(path1, false, printStream);
//        UtxoSetManager utxoSetManager2 = getUtxoSetManager(path2, false, printStream);
//
//        if(printLoadedUtxoSet) {
//            utxoSetManager1.printUtxoSet(printStream);
//            printStream.println("");
//            utxoSetManager2.printUtxoSet(printStream);
//            printStream.println("\n");
//        }
//
//        UtxoSetLog.UtxoSetLogEntry entry1 = utxoSetManager1.getBestUtxoSetLogEntry();
//        UtxoSetLog.UtxoSetLogEntry entry2 = utxoSetManager2.getBestUtxoSetLogEntry();
//
//        int height1 = entry1.height;
//        int height2 = entry2.height;
//
//        if(printEntries) {
//            entry1.print();
//            printStream.println("");
//            entry2.print();
//            printStream.println("\n");
//        }
//
//        if(height1 < height2) {
//            boolean matched =
//                    utxoSetManager1.equalsMerkleTreeRoot(utxoSetManager2, height1, printStream);
//        } else {
//            boolean matched =
//                    utxoSetManager1.equalsMerkleTreeRoot(utxoSetManager2, height2, printStream);
//        }
//    }
//
//
//    /* Compare best heights */
//    public static final String getLogEntryCmd = "log";
//    static {
//        String cmd = getLogEntryCmd;
//        Option option = new Option(cmd, true,
//                "Use: " + cmd + " <utxo set path> [height] \n" +
//                        "Prints utxo set log entry. If height is omitted then best " +
//                        "height is retrived.");
//        option.setArgs(2);
//        option.setOptionalArg(true);
//        commands.add(option);
//    }
//    public static void getLogEntry(String path,
//                                   int height,
//                                   boolean bestHeight,
//                                   PrintStream printStream,
//                                   boolean printLoadedUtxoSet)
//            throws IOException {
//        UtxoSetManager utxoSetManager = getUtxoSetManager(path, false, printStream);
//
//        if(printLoadedUtxoSet) {
//            utxoSetManager.printUtxoSet(printStream);
//            printStream.println("\n");
//        }
//
//        UtxoSetLog.UtxoSetLogEntry entry;
//        if(bestHeight) {
//            entry = utxoSetManager.getBestUtxoSetLogEntry();
//        } else {
//            entry = utxoSetManager.getUtxoSetLogEntry(height);
//        }
//
//        printStream.println("Entry:");
//        entry.print();
//    }
}
