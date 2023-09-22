package Blockchainj.Blockchain.Main;

import Blockchainj.Bitcoin.RPC.BlockBuffer;
import Blockchainj.Bitcoin.RPC.ConcurrentBlockBuffer;
import Blockchainj.Bitcoin.RPC.RPCconnection;
import Blockchainj.Bitcoin.RPC.SimpleBlockBuffer;
import Blockchainj.Blockchain.Blockchain;
import Blockchainj.Blockchain.Server.BlockchainServer;
import Blockchainj.Blockchain.Statistics.StatisticsBlocks;
import Blockchainj.Blockchain.Statistics.StatisticsUtxoSet;
import Blockchainj.Blockchain.UtxoSet.*;
import Blockchainj.Blockchain.UtxoSet.Shard.ShardSortedMapUtxs;
import Blockchainj.Blockchain.UtxoSet.UTXOS.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("DanglingJavadoc")
public class UserParams {
    /* Print stream */
    public static final PrintStream PRINT_STREAM = System.out;

    private static final LinkedHashMap<String, String> DEFAULT_PARAMETERS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    static {
        /* Set parameters */
        /** Shard and UTX type parametes */
        DEFAULT_PARAMETERS.put("CAREFUL_SHARD_TYPE", getStr(ShardSortedMapUtxs.SHARD_TYPE));
        DEFAULT_PARAMETERS.put("CAREFUL_UTX_TYPE", getStr(UtxFast.UTX_TYPE));


        /** RPC connection information. */
        DEFAULT_PARAMETERS.put("RPC_IP", getStr("127.0.0.1"));
        DEFAULT_PARAMETERS.put("RPC_PORT", getStr("8332"));
        DEFAULT_PARAMETERS.put("RPC_USER", getStr("user"));
        DEFAULT_PARAMETERS.put("RPC_PASSWORD", getStr("user"));

//        TODO comma seperated multiple rpcs
//        DEFAULT_PARAMETERS.put("RPC_IP_MULTI", getStr("127.0.0.1"));
//        DEFAULT_PARAMETERS.put("RPC_PORT_MULTI", getStr("8332"));
//        DEFAULT_PARAMETERS.put("RPC_USER_MULTI", getStr("user"));
//        DEFAULT_PARAMETERS.put("RPC_PASSWORD_MULTI", getStr("user"));



        /** BlockBuffer parameters */
        DEFAULT_PARAMETERS.put("BLOCKBUFFER_CONCURRENT", getStr(true));
        DEFAULT_PARAMETERS.put("BLOCKBUFFER_THREADS", getStr(4));
        DEFAULT_PARAMETERS.put("BLOCKBUFFER_SIZE", getStr(64));
        DEFAULT_PARAMETERS.put("BLOCKBUFFER_END_HEIGHT", getStr(527742));
        //TODO DEFAULT_PARAMETERS.put("BLOCKBUFFER_STAY_BEHIND_BLOCKS", getStr(12));



        /** UtxoSet parameters */
        /* Number of shard for new utxo set */
        DEFAULT_PARAMETERS.put("UTXO_SET_INTERNAL_SHARD_NUM", getStr(8192));

        /* Performance parameters. */
        DEFAULT_PARAMETERS.put("UTXO_SET_CONCURRENT_COMMIT", getStr(true));
        DEFAULT_PARAMETERS.put("UTXO_SET_COMMIT_THREADS", getStr(4));

        /* Match utxo set log data to utxo set data. This is the core data checksum. */
        DEFAULT_PARAMETERS.put("UTXO_SET_DO_MERKLE_TREE_CHECKSUM_ON_INIT", getStr(true));

        /* Compute the utxo set merkle tree for every block. */
        /* Build once on specific SHARDNUM for data reference. */
        DEFAULT_PARAMETERS.put("UTXO_SET_HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT",
                getStr(true));

        /* Choose UTXO SET TYPE. UtxoSetIO, UtxoSetMemory, UtxoSetMemory2 */
        DEFAULT_PARAMETERS.put("UTXO_SET_TYPE_IO", getStr(false));
        DEFAULT_PARAMETERS.put("UTXO_SET_TYPE_MEMORY", getStr(false));
        DEFAULT_PARAMETERS.put("UTXO_SET_TYPE_MEMORY2", getStr(false));
        DEFAULT_PARAMETERS.put("UTXO_SET_TYPE_SIMPLE", getStr(true));


        /* Period to do a suggestive call to the garbage collector. */
        DEFAULT_PARAMETERS.put("UTXO_SET_GARBAGE_COLLECTOR_PERIOD", getStr(10000));

        /* Activate utxo set timer */
        DEFAULT_PARAMETERS.put("UTXO_SET_ACTIVE_TIMER", getStr(true));



        /** Blockchain Build parameters */
        DEFAULT_PARAMETERS.put("BLOCKCHAIN_PATH_UTXO_SET",
                getStr("/home/asdf/blockchainj_files/utxo_set"));
        DEFAULT_PARAMETERS.put("BLOCKCHAIN_DO_PRINT", getStr(true));
        DEFAULT_PARAMETERS.put("BLOCKCHAIN_PRINT_PERIOD", getStr(1000));

        /* Has an online server while building the blockchain. */
        DEFAULT_PARAMETERS.put("BLOCKCHAIN_DO_SERVER", getStr(false));

        /* Activate blockchain timer */
        DEFAULT_PARAMETERS.put("BLOCKCHAIN_ACTIVE_TIMER", getStr(true));



        /** BlockchainServer parameters */
        DEFAULT_PARAMETERS.put("SERVER_PATH_UTXO_SET",
                getStr("/home/asdf/blockchainj_files/utxo_set"));
        DEFAULT_PARAMETERS.put("SERVER_UTXO_SET_DO_MERKLE_TREE_CHECKSUM_ON_INIT", getStr(true));
        DEFAULT_PARAMETERS.put("SERVER_IP", getStr("127.0.0.1"));
        DEFAULT_PARAMETERS.put("SERVER_PORT", getStr(8334));
        DEFAULT_PARAMETERS.put("SERVER_LOG_PATH",
                getStr("/home/asdf/blockchainj_files/server_logs"));


        /** StatisticsBlocks parameters */
        DEFAULT_PARAMETERS.put("STAT_BLOCKS_PATH",
                getStr("/home/user/bitcoin_my_statistics"));
        DEFAULT_PARAMETERS.put("STAT_BLOCKS_PRINT_PERIOD", getStr(1000));
        DEFAULT_PARAMETERS.put("STAT_BLOCKS_DO_CONCURRENT", getStr(true));
        DEFAULT_PARAMETERS.put("STAT_BLOCKS_THREAD_NUM", getStr(4));


        /** StatisticsUtxoSet parameters */
        DEFAULT_PARAMETERS.put("STAT_UTXOSET_PATH",
                getStr("/home/asdf/blockchainj_files/stat_utxoset"));
        DEFAULT_PARAMETERS.put("STAT_UTXOSET_PATH_UTXO_SET",
                getStr("/home/asdf/blockchainj_files/stat_utxoset/utxo_set"));
        DEFAULT_PARAMETERS.put("STAT_UTXOSET_PRINT_PERIOD", getStr(100));
        DEFAULT_PARAMETERS.put("STAT_UTXOSET_DO_CONCURRENT", getStr(true));
        DEFAULT_PARAMETERS.put("STAT_UTXOSET_THREAD_NUM", getStr(4));


        Iterator<String> it = DEFAULT_PARAMETERS.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            parameters.put(key, DEFAULT_PARAMETERS.get(key));
        }
    }


    public static String getString(String parameterName) {
        String param = parameters.get(parameterName);
        if(param == null) {
            throw new IllegalArgumentException();
        } else {
            return param;
        }
    }

    public static int getInt(String parameterName) {
        return Integer.parseInt(getString(parameterName));
    }

    public static boolean getBool(String parameterName) {
        return Boolean.parseBoolean(getString(parameterName));
    }

    public static String getDefaultString(String parameterName) {
        return DEFAULT_PARAMETERS.get(parameterName);
    }

    public static int getDefaultInt(String parameterName) {
        return Integer.parseInt(getDefaultString(parameterName));
    }

    public static boolean getDefaultBool(String parameterName) {
        return Boolean.parseBoolean(getDefaultString(parameterName));
    }

    private static String getStr(int val) { return Integer.toString(val); }
    private static String getStr(boolean val) { return Boolean.toString(val); }
    private static String getStr(String str) { return str; }


    public static void readConfigFile(String path) throws IOException {
        Properties prop = new MyProperties();
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            prop.load(input);

            parameters.clear();

            Iterator<String> it = DEFAULT_PARAMETERS.keySet().iterator();
            while(it.hasNext()) {
                String key = it.next();
                parameters.put( key, prop.getProperty(key, DEFAULT_PARAMETERS.get(key)) );
            }
        } finally {
            if(input != null)
                input.close();
        }
    }

    public static void writeConfigFile(String path) throws IOException {
        Properties prop = new MyProperties();
        OutputStream output = null;
        try {
            output = new FileOutputStream(path);

            Iterator<String> it = parameters.keySet().iterator();
            while(it.hasNext()) {
                String key = it.next();
                prop.setProperty(key, parameters.get(key));
            }

            prop.store(output, "Blockchainj.Blockchain Parameters");
        } finally {
            if(output != null)
                output.close();
        }
    }


    /* Reads command line arguments and loads or writes to file user parameters.
     * Will terminate the program if
     *      'newconf' option is demanded
      *      Option value is wrong
      *      IOException is caught */
    public static void loadOrWriteUserParams(String[] args) {
        /* User options */
        Options options = new Options();
        options.addOption("conf", "configfile", true,
                "Use: -conf <pathanme> \n Configuration file to load parameters from.");
        options.addOption("newconf", "newconfigfile", true,
                "Use: -newconf <pathname> Write new configuration file with given name.");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            /* Parse options */
            CommandLine cmd = parser.parse(options, args);

            /* Check for parameter file to load */
            if (cmd.hasOption("conf")) {
                UserParams.readConfigFile(cmd.getOptionValue("conf"));
            }

            /* Check if parameter file must be stored */
            if (cmd.hasOption("newconf")) {
                UserParams.writeConfigFile(cmd.getOptionValue("newconf"));
                System.exit(0);
            }

        } catch (ParseException | IllegalArgumentException  e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Blockchainj/Blockchain", options);
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /** Shard and UTX type parametes */
    public static void setShardAndUtxTypes() {
        //Only one shard factory

        switch (UserParams.getInt("CAREFUL_UTX_TYPE")) {
            case UtxFast.UTX_TYPE:
                MainUtxFactory.utxFactory = new UtxFastFactory();
                break;
            case UtxCompact.UTX_TYPE:
                MainUtxFactory.utxFactory = new UtxCompactFactory();
                break;
        }
    }


    /** RPCconnection */
    public static RPCconnection getNewRPCconnection() {
        /* Create new RPC connection */
        return new RPCconnection(
                UserParams.getString("RPC_IP"),
                UserParams.getString("RPC_PORT"),
                UserParams.getString("RPC_USER"),
                UserParams.getString("RPC_PASSWORD"));
    }


    /** BlockBuffer */
    public static BlockBuffer getNewBlockBuffer(RPCconnection rpcCon) {
        //TODO UserParams.getInt("BLOCKBUFFER_STAY_BEHIND_BLOCKS")
        if(UserParams.getBool("BLOCKBUFFER_CONCURRENT")) {
            return new ConcurrentBlockBuffer(
                    0,
                    UserParams.getInt("BLOCKBUFFER_END_HEIGHT"),
                    rpcCon,
                    UserParams.getInt("BLOCKBUFFER_SIZE"),
                    UserParams.getInt("BLOCKBUFFER_THREADS"));
        } else {
            return new SimpleBlockBuffer(
                    0,
                    UserParams.getInt("BLOCKBUFFER_END_HEIGHT"),
                    rpcCon);
        }
    }


    /** UtxoSet */
    public static UtxoSet tryOpenUtxoSet(String path)
            throws IOException, BitcoinUtxoSetException {
        UtxoSet utxoSet;
        boolean DO_MERKLE_TREE_CHECKSUM_ON_INIT =
                UserParams.getBool("UTXO_SET_DO_MERKLE_TREE_CHECKSUM_ON_INIT");

        /* Try to open existing utxo set. */
        if(UserParams.getBool("UTXO_SET_TYPE_IO")) {
            utxoSet = new UtxoSetIO(
                    path,
                    DO_MERKLE_TREE_CHECKSUM_ON_INIT);

        } else if(UserParams.getBool("UTXO_SET_TYPE_MEMORY")) {
            utxoSet = new UtxoSetMemory(
                    path,
                    DO_MERKLE_TREE_CHECKSUM_ON_INIT);

        } else if(UserParams.getBool("UTXO_SET_TYPE_MEMORY2")) {
            utxoSet = new UtxoSetMemory2(
                    path,
                    DO_MERKLE_TREE_CHECKSUM_ON_INIT);

        } else if(UserParams.getBool("UTXO_SET_TYPE_SIMPLE")) {
            utxoSet = new UtxoSetSimple(Paths.get(path, "utxo_set.bin").toString());

        } else {
            throw new RuntimeException(new IllegalArgumentException("UTXO_SET_TYPE not set"));
        }

        setUtxoSetParameters(utxoSet);

        return utxoSet;
    }


    public static UtxoSet createNewUtxoSet(String path)
            throws IOException, BitcoinUtxoSetException {
        UtxoSet utxoSet;
        int internalShardNum = UserParams.getInt("UTXO_SET_INTERNAL_SHARD_NUM");

        /* Create utxo set. */
        if(UserParams.getBool("UTXO_SET_TYPE_IO")) {
            utxoSet = new UtxoSetIO(
                    path,
                    internalShardNum);

        } else if(UserParams.getBool("UTXO_SET_TYPE_MEMORY")) {
            utxoSet = new UtxoSetMemory(
                    path,
                    internalShardNum);

        } else if(UserParams.getBool("UTXO_SET_TYPE_MEMORY2")) {
            utxoSet = new UtxoSetMemory2(
                    path,
                    internalShardNum);
        } else if(UserParams.getBool("UTXO_SET_TYPE_SIMPLE")) {
            utxoSet = new UtxoSetSimple(Paths.get(path, "utxo_set.bin").toString());

        } else {
            throw new RuntimeException(new IllegalArgumentException("UTXO_SET_TYPE not set"));
        }

        setUtxoSetParameters(utxoSet);

        return utxoSet;
    }

    private static void setUtxoSetParameters(UtxoSet utxoSet) {
        if(utxoSet instanceof AbstractUtxoSet) {
            AbstractUtxoSet abstractUtxoSet = (AbstractUtxoSet)utxoSet;

            if (UserParams.getBool("UTXO_SET_CONCURRENT_COMMIT")) {
                abstractUtxoSet.setCONCURRENT_COMMIT(true);
                abstractUtxoSet.setCOMMIT_CORE_THREADS(
                        UserParams.getInt("UTXO_SET_COMMIT_THREADS"));
            } else {
                abstractUtxoSet.setCONCURRENT_COMMIT(false);
            }

            abstractUtxoSet.setHASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT(
                    UserParams.getBool("UTXO_SET_HASH_SHARDS_AND_REBUILD_MERKLE_TREE_ON_COMMIT"));

            abstractUtxoSet.setGARBAGE_COLLECTOR_CALL_PERIOD(
                    UserParams.getInt("UTXO_SET_GARBAGE_COLLECTOR_PERIOD"));

            abstractUtxoSet.setActiveTimer(UserParams.getBool("UTXO_SET_ACTIVE_TIMER"));
        }
    }

    public static String getUtxoSetPath(String forWhat) {
        switch (forWhat) {
            case "BLOCKCHAIN":
                return UserParams.getString("BLOCKCHAIN_PATH_UTXO_SET");

            case "SERVER":
                UserParams.getStr("SERVER_PATH_UTXO_SET");

            case "STAT_UTXOSET":
                return UserParams.getString("STAT_UTXOSET_PATH_UTXO_SET");

            default:
                throw new RuntimeException(
                        new IllegalArgumentException("Must choose UtxoSet what for?"));
        }
    }



    /** Blockchain */
    public static Blockchain BLOCKCHAIN_getNewBlockchain(UtxoSet utxoSet, BlockBuffer blockBuffer) {
        Blockchain blockchain;
        blockchain = new Blockchain(utxoSet, blockBuffer);
        blockchain.setPRINT_STREAM((UserParams.getBool("BLOCKCHAIN_DO_PRINT"))?
                UserParams.PRINT_STREAM:null);
        blockchain.setPRINT_PERIOD(UserParams.getInt("BLOCKCHAIN_PRINT_PERIOD"));
        blockchain.setActiveTimer(UserParams.getBool("BLOCKCHAIN_ACTIVE_TIMER"));

        return blockchain;
    }

    public static BlockchainServer BLOCKCHAIN_getNewBlockchainServer(UtxoSet utxoSet)
            throws IOException {
        if(UserParams.getBool("BLOCKCHAIN_DO_SERVER")) {
            return BLOCKCHAIN_SERVER_getNewBlockchainServer(utxoSet);
        } else {
            return null;
        }
    }



    /** BlockchainServer */
    public static UtxoSetIO BLOCKCHAIN_SERVER_getNewUtxoSetIO()
            throws BitcoinUtxoSetException, IOException{
        return new UtxoSetIO(
                UserParams.getString("SERVER_PATH_UTXO_SET"),
                UserParams.getBool("UTXO_SET_DO_MERKLE_TREE_CHECKSUM_ON_INIT"));
    }

    public static BlockchainServer BLOCKCHAIN_SERVER_getNewBlockchainServer(UtxoSet utxoSet)
            throws IOException {
        BlockchainServer blockchainServer;

        blockchainServer = new BlockchainServer(
                UserParams.getString("SERVER_IP"),
                UserParams.getInt("SERVER_PORT"),
                utxoSet,
                UserParams.getString("SERVER_LOG_PATH"));

        return blockchainServer;
    }



    /** StatisticsBlocks  */
    public static StatisticsBlocks STAT_STATIC_getNewStatisticsBlocks(BlockBuffer blockBuffer)
            throws IOException {
        /* Create new StatisticsBlocks */
        StatisticsBlocks statisticsBlocks =
                new StatisticsBlocks(
                        UserParams.getString("STAT_BLOCKS_PATH"),
                        UserParams.getBool("STAT_BLOCKS_DO_CONCURRENT"),
                        UserParams.getInt("STAT_BLOCKS_THREAD_NUM"),
                        blockBuffer);

        /* Set print stream */
        statisticsBlocks.setPRINT_STREAM(UserParams.PRINT_STREAM);

        /* Set print period */
        statisticsBlocks.setPRINT_PERIOD(UserParams.getInt("STAT_BLOCKS_PRINT_PERIOD"));

        return statisticsBlocks;
    }




    /** Statistics Utxo Set only */
    public static StatisticsUtxoSet STAT_UTXOSET_getNewStatisticsUtxoSet(BlockBuffer blockBuffer,
                                                                         UtxoSet utxoSet)
            throws IOException {
        /* Create new StatisticsBlocks */
        StatisticsUtxoSet statisticsStatic =
                new StatisticsUtxoSet(
                        UserParams.getString("STAT_UTXOSET_PATH"),
                        utxoSet,
                        blockBuffer,
                        UserParams.getBool("STAT_UTXOSET_DO_CONCURRENT"),
                        UserParams.getInt("STAT_UTXOSET_THREAD_NUM"));

        /* Set print stream */
        statisticsStatic.setPRINT_STREAM(UserParams.PRINT_STREAM);

        /* Set print period */
        statisticsStatic.setPRINT_PERIOD(UserParams.getInt("STAT_UTXOSET_PRINT_PERIOD"));

        return statisticsStatic;
    }









//    /* Writes new default parameter file */
//    public static void main(String[] args) {
//        String path = args[0];
//
//        try {
//            writeConfigFile(path);
//            //readConfigFile(path);
//            //System.out.println(getString("BLOCKCHAIN_SERVER_IP"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }





    public static class MyProperties extends Properties {
        private static final long serialVersionUID = 1L;

        @Override
        public Enumeration<Object> keys() {
            Enumeration<Object> keysEnum = super.keys();
            Vector<Object> keyList = new Vector<>();

            while (keysEnum.hasMoreElements()) {
                keyList.add(keysEnum.nextElement());
            }

            //noinspection Convert2Lambda,Java8ListSort
            Collections.sort(keyList, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });

            return keyList.elements();
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            Set<Map.Entry<Object, Object>> set = super.entrySet();

            //noinspection Convert2Lambda,Java8ListSort
            SortedSet<Map.Entry<Object, Object>> newSet = new TreeSet<>(new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });

            newSet.addAll(set);

            return newSet;
        }
    }
}