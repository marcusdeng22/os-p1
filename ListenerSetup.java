/**
Marcus Deng
mwd160230
CS 6378.001

This class manages the acceptance of new client connections.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

class ListenerSetup extends Thread {
  private volatile boolean flag = true;

  private ServerSocket server;
  private int id;
  private volatile ConcurrentHashMap<String, Boolean> resourceLocks;
  private volatile ConcurrentHashMap<String, ConcurrentLinkedQueue<ObjectOutputStream>> deferred;
  private volatile AtomicLong localTime = new AtomicLong();
  private volatile ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt;
  private final int NUMCLIENTS;

  private ListenerHandler listenerList[];
  private boolean allConn = false;
  private Socket socket;

  public ListenerSetup(ServerSocket server, int id, ConcurrentHashMap<String, Boolean> resourceLocks, ConcurrentHashMap<String, ConcurrentLinkedQueue<ObjectOutputStream>> deferred, AtomicLong localTime, ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt, int numClients) {
    this.server = server;
    this.id = id;
    this.resourceLocks = resourceLocks;
    this.deferred = deferred;
    this.localTime = localTime;
    this.opt = opt;
    this.NUMCLIENTS = numClients;
    this.listenerList = new ListenerHandler[this.NUMCLIENTS];
  }

  @Override
  public void run() {
    try {
      //accept new client connections and keep track of them
      for (int x = 0; x < NUMCLIENTS; x ++) {
        try {
          socket = server.accept();
          System.out.println("New listener client connected: " + socket);

					ListenerHandler t = new ListenerHandler(id, socket, resourceLocks, deferred, localTime, opt);
          listenerList[x] = t;
          t.start();
        }
        catch (Exception e) {
          //unknown exception, so close the clients
          socket.close();
        }
      }
      //check the liveness of the connections
      allConn = true;
      System.out.println("max clients joined");
      while (flag) {
        for (int x = 0; x < NUMCLIENTS; x ++) {
          if (!listenerList[x].isAlive()) {
            System.out.println("a listener connection is dead! exiting...");
            allConn = false;
            close();
          }
        }
      }
    }
    catch (Exception e) {}
    finally {
      try {
        System.out.println("Client server is closed");
				close();
      }
      catch (Exception e) {
        System.out.println("No listener port to close");
      }
    }
  }

  //returns if all clients are connected
  public boolean allConnected() {
    return allConn;
  }

  //cleanup and close all connections
  public void close() throws IOException {
    flag = false;
		try {
      //wait for each client connection to close
      for (int x = 0; x < NUMCLIENTS; x ++) {
        listenerList[x].close();
        listenerList[x].join();
      }
		}
		catch (Exception e) {}
		try {
			server.close();
			socket.close();
		}
		catch (Exception e) {}
  }
}
