package Blockchainj.Blockchain.Statistics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stat
 *
 * All information and parameters for a statistic.
 */

public class Stat {
    public static final String STATS_FILE_EXTENSION = ".log";

    private final int id;
    private final int groupId;
    private final boolean active;

    private final String name;
    private final String filename;
    private Path filepathname = null;

    private final String description;


    Stat(int id, int groupId, boolean active,
         String name, String description) {
        this.id = id;
        this.name = name;
        this.filename = name + STATS_FILE_EXTENSION;
        this.filepathname = null;
        this.description = description;
        this.active = active;
        this.groupId = groupId;
    }

    void setPath(String path) {
        this.filepathname = Paths.get(path, this.filename);
    }

    public int getId() { return id; }

    public int getGroupId() { return groupId; }

    public Path getFilepathname() { return Paths.get(filepathname.toString()); }

    public boolean isActive() { return active; }

    public void store(PrintWriter printWriter) throws IOException {
        printWriter.println("Name: "+ name);
        printWriter.println(" Filename: " + filename);
        printWriter.println(" Description: " + description);
        printWriter.println(" Stat id: " + id);
        printWriter.println(" Group id: " + groupId);
        printWriter.println(" Active: " + active);
    }
}
