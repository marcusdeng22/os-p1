/**
Marcus Deng
mwd160230
CS 6378.001

This class manages the creation and events of a server instance on the client.
This will accept connections from other clients, and defer responses according
to the algorithm.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

//handles the connection setup from other clients
class Listener {
	private static boolean valid = true;

	private final int id;
  private ListenerSetup listenerThread = null;
  private volatile boolean flag = true;
  private volatile ConcurrentHashMap<String, Boolean> resourceLocks;
  private volatile ConcurrentHashMap<String, ConcurrentLinkedQueue<ObjectOutputStream>> deferred = new ConcurrentHashMap<>();
  private volatile AtomicLong localTime = new AtomicLong();
  private volatile ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt;
  private ServerSocket server;

  public Listener(int id, int port, String files[], ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt, int numClients) throws IOException {
    this.id = id;
		try {
			this.server = new ServerSocket(port);
		}
		catch (BindException e) {
			System.out.println("socket in use!");
			flag = false;
			valid = false;
			return;
		}
    System.out.println("Client server started on port " + port);
		//all resources are not claimed yet
    resourceLocks = new ConcurrentHashMap<>();
    for (int x = 0; x < files.length; x ++) {
      resourceLocks.put(files[x], Boolean.FALSE);
    }
    System.out.println("created resource locks");

    this.opt = opt;
		//setup the thread that will accept new connections from other clients
    this.listenerThread = new ListenerSetup(server, id, resourceLocks, deferred, localTime, opt, numClients - 1);
  }

	//returns if this instance is valid
	public boolean isValid() {
		return valid;
	}

	//returns if all clients are still connected
	public boolean allConnected() {
		return listenerThread.allConnected();
	}

	//returns if the thread that accepts connections is still alive; if it is dead then there was an error on a socket
	public boolean isAlive() {
		return listenerThread.isAlive();
	}

	//start accepting connections
  public void start() {
    listenerThread.start();
  }

	//close the connections and cleanup
  public void stop() throws IOException, InterruptedException {
    server.close();
    listenerThread.close();
		listenerThread.join();
  }

	//returns a list of the processes that I need to ask for permission (optimization)
  public ArrayList<Integer> acquire(String resource) {
    //optimization here: if all other processes have not sent me a REQUEST for resource, then I don't need to REQUEST to them
    ArrayList<Integer> clientPerm = new ArrayList<>();
    ConcurrentHashMap<Integer, Boolean> optMap = opt.get(resource);
    Iterator it = optMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry tuple = (Map.Entry) it.next();
      if (tuple.getValue().equals(Boolean.TRUE)) {
        clientPerm.add((Integer) tuple.getKey());
      }
    }
    return clientPerm;
  }

	//acquire a resource locally, and stop replying to requests for that resource
	public void acquireSelf(String resource) {
		System.out.println("locking self for acquisition of " + resource);
    try {
      synchronized(this) {
        while (resourceLocks.get(resource).equals(Boolean.TRUE)) {
          wait();
        }
        resourceLocks.replace(resource, Boolean.TRUE);
				//save the time of the request
        localTime.set(System.currentTimeMillis());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
	}

	//unlock the resource
  synchronized public void unlock(String resource) {
		System.out.println("unlocked self for resource " + resource);
    resourceLocks.replace(resource, Boolean.FALSE);
    notify();
  }

	//send all the deferred resquests
  public void deferSend(String resource)  throws IOException {
    //for each resource in the deferred queue, send a REPLY
    if (!deferred.containsKey(resource) || deferred.get(resource).isEmpty()) {
      System.out.println("no deferred requests");
      return;
    }
    ConcurrentLinkedQueue<ObjectOutputStream> socketList = deferred.get(resource);
    while (!socketList.isEmpty()) {
      ObjectOutputStream o = socketList.poll();
      o.writeObject(new Message(Message.REPLY_MSG, resource, id));
    }
    System.out.println("sent all deferred requests");
  }
}
