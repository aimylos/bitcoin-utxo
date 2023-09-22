package Blockchainj.Blockchain.Statistics;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class StatGroup {
    private final int groupId;
    private boolean active;
    private LinkedHashMap<Integer, Stat> stats;


    public StatGroup(int groupId, boolean active) {
        this.groupId = groupId;
        this.active = active;
        this.stats = new LinkedHashMap<>();
    }

    public int getGroupId() { return groupId; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public void addStat(Stat stat) { stats.put(stat.getId(), stat); }

    public Stat getStat(int statId) { return stats.get(statId); }

    public Iterator<Stat> getStatIterator() {
        return stats.values().iterator();
    }

    void store(PrintWriter printWriter) throws IOException {
        printWriter.println(">>Group Id: " + getGroupId());
        printWriter.println(" Active: " + isActive());

        Iterator<Stat> it = stats.values().iterator();
        while(it.hasNext()) {
            it.next().store(printWriter);
            printWriter.print("\n");
        }
    }


//    /* Groups */
//    private static final LinkedHashMap<Integer, StatGroup> INIT_GROUPS= new LinkedHashMap<>();
//
//    /* Get stat groups */
//    static LinkedHashMap<Integer, StatGroup> getStatGroups() {
//        LinkedHashMap<Integer, StatGroup> newStatGroup = new LinkedHashMap<>();
//        Iterator<StatGroup> iterator = INIT_GROUPS.values().iterator();
//        while(iterator.hasNext()) {
//            StatGroup statGroup = iterator.next();
//            newStatGroup.put(statGroup.getGroupId(), statGroup);
//        }
//
//        return newStatGroup;
//    }
//
//    protected static void putStatGroup(StatGroup statGroup) {
//        INIT_GROUPS.put(statGroup.getGroupId(), statGroup);
//    }
//
//    /* Callable interface for group callable */
//    interface GroupCallable<V extends Void> extends Callable<V> {
//        @Override
//        V call() throws IOException;
//    }
}
