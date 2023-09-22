package Blockchainj.Bitcoin.RPC;

import java.util.Iterator;
import java.util.LinkedHashMap;

//TODO: stay behind option

public abstract class AbstractBlockBuffer implements BlockBuffer {
    /* Resources handed out */
    private volatile long nextIteratorId = 0;
    private final LinkedHashMap<Long, AbstractBlockIterator> activeIterators =
            new LinkedHashMap<>();

    /* Closed marker */
    private volatile boolean closed = false;


    protected abstract class AbstractBlockIterator implements BlockIterator {
        private final long id;
        private volatile boolean closed = false;

        protected AbstractBlockIterator() {
            id = getNextIteratorId();

            if(AbstractBlockBuffer.this.isClosed()) {
                close();
            } else {
                addIterator(id, this);
            }
        }

        protected synchronized void close() {
            closed = true;
            removeIterator(id);
        }

        protected synchronized boolean isClosed() {
            return closed;
        }
    }


    private synchronized long getNextIteratorId() { return nextIteratorId++; }

    private synchronized void addIterator(long id, AbstractBlockIterator it) {
        if(isClosed()) {
            return;
        }
        activeIterators.put(id, it);
    }

    private synchronized void removeIterator(long id) {
        if(isClosed()) {
            return;
        }
        activeIterators.remove(id);
    }


    @Override
    public synchronized void close() {
        closed = true;

        /* Close active iterators */
        Iterator<AbstractBlockIterator> it = activeIterators.values().iterator();
        while(it.hasNext()) {
            it.next().close();
            it.remove();
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }
}
