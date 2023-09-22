package Blockchainj.Bitcoin.RPC;

//import Blockchainj.Bitcoin.RPC.BitcoinRpcException;
//import Blockchainj.Bitcoin.RPC.RPCconnection;
//
//import java.io.BufferedReader;
//import java.io.PrintStream;
//import java.net.SocketTimeoutException;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReentrantReadWriteLock;

//TODO: Test
//TODO: Put innactive in list and periodically test for connection.
//TODO: Handle different heights? Easy solution is to get minimum height?

//TODO: Blockbuffer time is around 2-3 hours for all blocks. This is not that important
@Deprecated
public class RPCconnectionMulti {//extends RPCconnection {
//    private final LinkedList<RPCconnection> rpCconnections = new LinkedList<>();
//    private final LinkedBlockingQueue<RPCconnection> rpcConQueue = new LinkedBlockingQueue<>();
//    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
//
//
//    public RPCconnectionMulti() {
//        super();
//    }
//
//
//    /* Add new rpcConnection */
//    public void addConnection(RPCconnection rpCconnection) {
//        lock.writeLock().lock();
//        try {
//            rpCconnections.add(rpCconnection);
//            putConnectionInBlockingqueue(rpCconnection);
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//
//    /* Returns active connections count */
//    public int getActiveConnections() {
//        lock.writeLock().lock();
//        try {
//            return rpCconnections.size();
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//
//    @Override
//    protected BufferedReader doQuery(String params) throws BitcoinRpcException {
//        /* Look for active connections */
//        if(getActiveConnections() <= 0) {
//            throw new BitcoinRpcException("No active connections.");
//        }
//
//        /* Get an active connection */
//        RPCconnection rpCconnection;
//        try {
//            rpCconnection = rpcConQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
//        } catch (InterruptedException e) {
//            throw new BitcoinRpcException("Waiting for active connection timeout");
//        }
//
//        /* Perform query */
//        BufferedReader output;
//        try {
//            output = rpCconnection.doQuery(params);
//        } catch (BitcoinRpcException e) {
//            if(e.getCause() != null) {
//                if(e.getCause() instanceof SocketTimeoutException) {
//                    /* Remove from active connections */
//                    removeConnection(rpCconnection);
//
//                    /* Try again */
//                    return doQuery(params);
//                }
//            }
//
//            throw e;
//        }
//
//        /* Put connection back in queue */
//        putConnectionInBlockingqueue(rpCconnection);
//
//        return output;
//    }
//
//
//    private void removeConnection(RPCconnection rpCconnection) {
//        /* Remove connection */
//        lock.writeLock().lock();
//        try {
//            rpCconnections.remove(rpCconnection);
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//
//    private void putConnectionInBlockingqueue(RPCconnection rpCconnection) {
//        //noinspection EmptyCatchBlock
//        try {
//            rpcConQueue.put(rpCconnection);
//        } catch (InterruptedException e) { }
//    }
//
//
//    @Override
//    public String getRpcAddr() {
//        lock.writeLock().lock();
//        try {
//            String out = null;
//
//            Iterator<RPCconnection> it = rpCconnections.iterator();
//            while(it.hasNext()) {
//                if(out == null) {
//                    out = it.next().getRpcAddr();
//                } else {
//                    //noinspection StringConcatenationInLoop
//                    out = out + ", " + it.next().getRpcAddr();
//                }
//            }
//
//            return out;
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//
//    @Override
//    public void printParameters(PrintStream printStream) {
//        printStream.println(">RPC Connection parameters:");
//        printStream.println("Active connections: " + getActiveConnections());
//
//        lock.writeLock().lock();
//        try {
//            Iterator<RPCconnection> it = rpCconnections.iterator();
//            while(it.hasNext()) {
//                it.next().printParameters(printStream);
//            }
//        } finally {
//            lock.writeLock().unlock();
//        }


//    private static RPCconnection[] testConnections(RPCconnection[] rpCconnections) {
//        final long timeoutMillis = 3000; //3 seconds
//
//        /* Test connections */
//        for(int i=0; i<rpCconnections.length; i++) {
//            RPCconnection rpcCon = rpCconnections[i];
//
//            Thread thread = (new Thread() {
//                @Override
//                public void run() {
//                    try {
//                        rpcCon.getBestBlockhash();
//                    } catch (BitcoinRpcException e) {
//                        this.interrupt();
//                    }
//                }
//            });
//
//            thread.start();
//
//            try {
//                thread.join(timeoutMillis);
//            } catch (InterruptedException e) {
//                rpCconnections[i] = null;
//            }
//        }
//
//        return rpCconnections;
//    }
}
