/**
Marcus Deng
mwd160230
CS 6378.001

This class defines the Client. It will setup all the connections required to
execute the Ricart-Agrwala algorithm with Roucairol and Carvalho's optimization.
It will also generate random READ/WRIT messages.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client {
  private int id = 0;
  private Socket socket = null;
  private ObjectInputStream in = null;
  private ObjectOutputStream out = null;

  private boolean valid = false;  //test validity of connection

  //constants defined for the program
	private static final int NUMSERVERS = 3;
	private static final int SERVERPORT = 3000;  //base server port
	private static final int NUMCLIENTS = 5;
	private static final int PORT = 5000;        //base client port
	private static final boolean GENERATE = true;  //generate random messages
	private static final int DELAY = 1000;       //time for each command to take

  public Client(int id, String address, int port) throws IOException, ClassNotFoundException {
    this.id = id;
    // establish a connection to server
    try {
      socket = new Socket(address, port);

      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());
    }
    catch (Exception e) {
			System.out.println("failed to setup connection to server");
			return;
		}

    //get the test message from server
    String recv = ((Message) in.readObject()).getCmd();
    if (!recv.equals(Message.TEST_MSG)) {
      System.out.println("bad connection; quitting");
      return;
    }
    System.out.println("Connected to " + address + ":" + port);
    this.valid = true;
  }

  //cleanup connection with server
  public void close() throws IOException {
    System.out.println("client " + this.id + " closing");
    if (in != null) {
      in.close();
    }
    if (out != null) {
      out.close();
    }
    if (socket != null) {
      socket.close();
    }
    this.valid = false;
  }

  //returns if the connection is valid
  public boolean isValid() {
    return this.valid;
  }

  //send a request to the server
  public Message exec(Message data) throws IOException, ClassNotFoundException {
    if (!this.valid) {
      return null;
    }
    out.writeObject(data);
    return (Message) in.readObject();
  }

  //Ricart-Agrawala algorithm is executed here
	public static void ricartAgrawala(Listener listener, ListenerClient listenerClient[], ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt, String cmd, String resource, Message m, Client client[]) {
		try {
      //notify self for the request for the resource; this will perform the optimization checks
			ArrayList<Integer> clientPerm = listener.acquire(resource);
			//get locks from everyone else, and set the corresponding resources as acquired
			for (int x = 0; x < clientPerm.size(); x ++) {
				listenerClient[clientPerm.get(x).intValue()].acquire(resource);
				opt.get(resource).replace(clientPerm.get(x).intValue(), Boolean.FALSE);
			}
      //formally acquire resource for self
      listener.acquireSelf(resource);

			//now we have acquired locks, start execution
			try {
				Thread.sleep(DELAY);	//for testing purposes
			}
			catch (InterruptedException e) {
				System.out.println("break");
			}
			if (cmd.equals(Message.WRIT_MSG)) {
				System.out.println("sending message to all servers");
				for (int x = 0; x < NUMSERVERS; x ++) {
					Message resp = client[x].exec(m);
					System.out.println("Resp from svr " + x + ": " + resp);
					if (resp.getCmd().equals(Message.ERR_MSG)) {
						System.out.println("error from server " + x);
						System.out.println("message: " + resp.getText());
            System.out.println();
					}
				}
			}
			else {
				Random r = new Random();
				int serverID = r.nextInt(NUMSERVERS);
				System.out.println("picking server " + serverID + " for execution");
				Message resp = client[serverID].exec(m);
				System.out.println("Resp from svr " + serverID + ": " + resp);
				if (resp.getCmd().equals(Message.ERR_MSG)) {
					System.out.println("error from server " + serverID);
				}
				System.out.println("message: " + resp.getText());
        System.out.println();
			}
			//now we have all server responses, notify listener to resume accepting requests
			listener.unlock(resource);
      //send all the deferred requests
			listener.deferSend(resource);
		}
		catch (Exception e) {
			System.out.println("error");
		}
	}

  public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException {
		int argLen = NUMCLIENTS + NUMSERVERS;
		int argOffset = 0;
    if (args.length != argLen) {
      System.out.println("please provide client id, server addresses, and client addreses in that order and increasing order of server/client enumeration");
			return;
    }
    final int id;
    try {
      id = Integer.parseInt(args[0]);
      if (id < 0 || id >= NUMCLIENTS) {
        throw new IOException("invalid id");
      }
    }
    catch (Exception e) {
      System.out.println("please provide a valid client id 0-" + (NUMCLIENTS - 1));
      return;
    }
		argOffset += 1;

    //conect to servers, starting at 3000
    Client client[] = new Client[NUMSERVERS];
    for (int x = 0; x < NUMSERVERS; x ++) {
      client[x] = new Client(id, args[argOffset], SERVERPORT + x);
			argOffset += 1;
      if (!client[x].isValid()) {
        System.out.println("failed to setup connection to server " + x);
        return;
      }
    }
    //get the files available
    String fileList = client[0].exec(new Message(Message.INFO_MSG, "", id)).getText();
    String files[] = fileList.split("\n");  //metadata for files only contains file names for now
    System.out.println("Files available:");
    for (int x = 0; x < files.length; x ++) {
      System.out.println(files[x]);
    }
    System.out.println();

    //setup the optimization matrix
    //organized by resource, then (client, responded) tuples
    //if we need to ask a client for the resource, then the field is set to true
    //otherwise once we get the REPLY, we set the field to false
    ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt = new ConcurrentHashMap<>();
    for (int x = 0; x < files.length; x ++) {
      ConcurrentHashMap<Integer, Boolean> clientList = new ConcurrentHashMap<>();
      opt.put(files[x], clientList);
      for (int c = 0; c < NUMCLIENTS; c ++) {
        if (c != id) {
          clientList.put(c, Boolean.TRUE);
        }
      }
    }

    //setup my own mutex listener
    Listener listener = new Listener(id, PORT + id, files, opt, NUMCLIENTS);
		if (!listener.isValid()) {
			System.out.println("client listener failed; exiting");
			listener.stop();
			return;
		}
    listener.start();

    //setup connection to the other clients; starting at port 5000
    ListenerClient listenerClient[] = new ListenerClient[NUMCLIENTS];
    for (int x = 0; x < NUMCLIENTS; x ++) {
      if (x == id) {
        listenerClient[x] = null;
      }
      else {
        listenerClient[x] = new ListenerClient(args[argOffset], PORT + x, id);
				argOffset += 1;
				//test if connection is valid; fail and cleanup if not valid
				if (!listenerClient[x].isValid()) {
					System.out.println("failed to establish proper connection to client " + x + "; exiting");
					for (int y = 0; y < NUMCLIENTS; y ++) {
						if (listenerClient[y] != null) {
							listenerClient[y].close();
						}
					}
					listener.stop();
					return;
				}
      }
    }
    //delay to let messages propagate
		try {
			Thread.sleep(NUMCLIENTS * 500);
		}
		catch (Exception e) {}

    if (!listener.allConnected()) {
      System.out.println("failed to connect all clients");
      listener.stop();
      return;
    }
		System.out.println("established all connections; you can now start operations");
		System.out.println();

    //listen for input from console, and once it passes input validation, execute the algorithm
    BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
    //only these messages are allowed by the user, and they must specify a resource
    List<String> ALLOWED_MSG = new ArrayList<>();
    ALLOWED_MSG.add(Message.INFO_MSG);
    ALLOWED_MSG.add(Message.READ_MSG);
    ALLOWED_MSG.add(Message.WRIT_MSG);

    //thread for user input
		Thread cli = new Thread() {
			@Override
			public void run() {
				while (true) {
          System.out.print(">");
          String input = null;
          //poll for input
          ExecutorService ex = Executors.newSingleThreadExecutor();
          try {
            while (true) {
              Future<String> result = ex.submit(new CinRead());
              try {
                input = result.get(1000, TimeUnit.MILLISECONDS);
                break;
              }
              catch (TimeoutException te) {
                result.cancel(true);
              }
              catch (InterruptedException ie) {
                ex.shutdownNow();
                break;
              }
              catch (Exception e) {}
            }
          }
          finally {
            ex.shutdownNow();
          }
					System.out.println("got: " + input);
          if (input == null) {
						System.out.println("exiting...");
						//exitFlag = true;
						break;
					}
					if (input.equals(Message.EXIT_MSG)) {
						//exitFlag = true;
            System.out.println("exiting");
						break;
					}
					//check input here: need to provide a resource!
					if (input.length() < 5) {
						System.out.println("invalid command");
					}
					String cmd = input.substring(0, 4);
					if (!ALLOWED_MSG.contains(cmd)) {
						System.out.println("Invalid command; try again");
						continue;
					}
					String resource = input.substring(4).trim();

					if (!Arrays.asList(files).contains(resource)) {
						System.out.println("Resource not found; try again");
						continue;
					}
					Message m = new Message(cmd, resource, id);

          //start a thread to execute the algorithm so we can grab more input
					Thread cliExec = new Thread() {
						@Override
						public void run() {
							Client.ricartAgrawala(listener, listenerClient, opt, cmd, resource, m, client);
						}
					};
					try {
						cliExec.start();
					}
					catch (Exception e) {
						System.out.println("cli exec error");
					}
				}
			}
		};
		try {
			cli.start();
		}
		catch (Exception e) {
			System.out.println("cli error");
		}

		//generate random read/write requests until exit
		Random randomFile = new Random();
		Random randomCmd = new Random();
		while (GENERATE && cli.isAlive() && listener.isAlive()) {
			int rFile = randomFile.nextInt(files.length);
			int rCmd = randomCmd.nextInt(2);
			if (rCmd == 0) {
				System.out.println("picked read of file " + files[rFile]);
				Client.ricartAgrawala(listener, listenerClient, opt, Message.READ_MSG, files[rFile], new Message(Message.READ_MSG, files[rFile], id), client);
				try {
					Thread.sleep(randomFile.nextInt(NUMCLIENTS * 1000) + (NUMCLIENTS * 1000));
				}
				catch (Exception e) {}
			}
			else {
				System.out.println("picked write of file " + files[rFile]);
				Client.ricartAgrawala(listener, listenerClient, opt, Message.WRIT_MSG, files[rFile], new Message(Message.WRIT_MSG, files[rFile], id), client);
				try {
					Thread.sleep(randomFile.nextInt(NUMCLIENTS * 1000) + (NUMCLIENTS * 1000));
				}
				catch (Exception e) {}
			}
		}

    //otherwise just wait for the user to exit
		if (!GENERATE) {
      if (!listener.isAlive()) {
        System.out.println("A client connection failed; check your addresses");
      }
      while (listener.isAlive() && cli.isAlive()) {
  			try {
          Thread.sleep(1000);
  			}
  			catch (InterruptedException e) {}
  		}
    }
    //stop the cli
    cli.interrupt();
    cin.close();

    //handle close since we're exiting
    System.out.println("closing connection to servers");
    for (int x = 0; x < NUMSERVERS; x ++) {
      client[x].close();
    }
    System.out.println("closing connection to clients");
    for (int x = 0; x < NUMCLIENTS; x ++) {
      if (x != id) {
        listenerClient[x].close();
      }
    }
    System.out.println("closing client server");
    listener.stop();
    System.out.println("done");
    return;
  }
}

//class to manage input from standard in to allow for interrupts
class CinRead implements Callable<String> {
  public String call() throws IOException {
    BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
    String input = null;
    do {
      try {
        while (!cin.ready()) {
          Thread.sleep(200);
        }
        input = cin.readLine();
      }
      catch (Exception e) {
        return null;
      }
    } while ("".equals(input));
    return input;
  }
}
