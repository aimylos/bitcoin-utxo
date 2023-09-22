package Blockchainj.Blockchain.UtxoSet;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * UtxoSetFileNaming
 *
 * Handles the naming of the files used by the utxo set.
 * Initialized with a path to the utxo set directory.
 *
 */

public class UtxoSetFileNaming {
    /* Utxo set log file name */
    private static final String LOGFILE_NAME = "utxo_log.bin";
    private final String logFileFullname;

    /* Shard name prefix and suffix. Name:  utxo_shard_<index>.bin */
    private static final String SHARD_NAME_PREFIX = "utxo_shard";
    private static final String SHARD_NAME_SUFFIX = ".bin";
    private final String shardNamePrefixFullname;
    private final String shardNameSuffix;

    /* Utxo set path to directory */
    private final String utxoSetPath;


    /* Main constructor. */
    public UtxoSetFileNaming(String utxoSetPath, String logFileName, String shardNamePrefix)
            throws IllegalArgumentException {
        /* create path */
        Path path = Paths.get(utxoSetPath);

        /* Make path and check existance */
        this.utxoSetPath = path.toString();

        /* create path if not found */
        File dir = path.toFile();
        if ( !dir.exists() ) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        this.logFileFullname = Paths.get(this.utxoSetPath, logFileName).toString();
        this.shardNamePrefixFullname = Paths.get(this.utxoSetPath, shardNamePrefix).toString();
        this.shardNameSuffix = SHARD_NAME_SUFFIX;
    }

    public UtxoSetFileNaming(String utxoSetPath) throws IllegalArgumentException {
        this(utxoSetPath, LOGFILE_NAME, SHARD_NAME_PREFIX);
    }


    /* Get utxo set path */
    public Path getUtxoSetPathAsPath() { return Paths.get(utxoSetPath); }

    public String getUtxoSetPath() { return utxoSetPath; }


    /* Get utxo set log filename */
    public Path getUtxoSetLogFilenameAsPath() { return Paths.get(logFileFullname); }

    public String getUtxoSetLogFilename() { return logFileFullname; }


    /* Get shard filename by index */
    public Path getShardFilenameAsPath(int shardIndex) {
        return Paths.get(getShardFilename(shardIndex));
    }

    public String getShardFilename(int shardIndex) {
        return shardNamePrefixFullname + "_" + Integer.toString(shardIndex) + shardNameSuffix;
    }


    /* Get shard filename by index and append extra string */
    public Path getShardFilenameAsPath(int shardIndex, String extra) {
        return Paths.get(getShardFilename(shardIndex, extra));
    }

    public String getShardFilename(int shardIndex, String extra) {
        return shardNamePrefixFullname + "_" + Integer.toString(shardIndex) +
                "_" + extra + shardNameSuffix;
    }


    /* DEBUG/TEST */
    public static void main(String[] agrs) {
        String path = "/tmp/";

        try {
            UtxoSetFileNaming utxoSetFileNaming = new UtxoSetFileNaming(path);

            System.out.println(utxoSetFileNaming.getUtxoSetPath());
            System.out.println(utxoSetFileNaming.getUtxoSetLogFilename());
            System.out.println(utxoSetFileNaming.getShardFilename(0));
            System.out.println(utxoSetFileNaming.getShardFilename(165));
            System.out.println(utxoSetFileNaming.getShardFilename(19247892));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
