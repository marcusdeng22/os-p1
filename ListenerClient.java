/**
Marcus Deng
mwd160230
CS 6378.001

This class will create and send the REQU (request) messages for a resource. It
will block until it receives the response. Requests resources from another
client.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

//this will handle sending the REQUEST messages; establishes connection to other clients
class ListenerClient {
	private boolean valid = false;
	private String addr;
	private int port;
  private Socket clientSocket = null;
  private ObjectInputStream in = null;
  private ObjectOutputStream out = null;
  private final int clientId;

  public ListenerClient(String addr, int port, int clientId) throws IOException, ClassNotFoundException {
    boolean wait = true;
		this.addr = addr;
		this.port = port;
    this.clientId = clientId;
    while (wait) {
      try {
        clientSocket = new Socket(addr, port);
        wait = false;
      }
			//client not yet running, so wait and try again later
      catch (ConnectException e) {
        try {
          Thread.sleep(500);
        }
        catch (InterruptedException ie) {
          System.out.println("failed to connect");
					return;
        }
      }
			catch (Exception e) {
				System.out.println("connection failed");
				return;
			}
    }
    out = new ObjectOutputStream(clientSocket.getOutputStream());
    in = new ObjectInputStream(clientSocket.getInputStream());
		//send a test message for connection validity
		out.writeObject(new Message(Message.TEST_MSG, "", clientId));
		if (!((Message) in.readObject()).getCmd().equals(Message.TEST_MSG)) {
			System.out.println("error connecting to client " + addr);
			return;
		}
    System.out.println("Established connection to client: " + addr + " " + port);
		this.valid = true;
  }

	//return if connection is valid
	public boolean isValid() {
		return valid;
	}

  //send the request here and wait for reply
  public boolean acquire(String resource) throws IOException, ClassNotFoundException {
    System.out.println("sending request for resource: " + resource);
    out.writeObject(new Message(Message.REQUEST_MSG, resource, clientId));
    Message resp = (Message) in.readObject();
    System.out.println("received response: " + resp);
    return true;
  }

	//cleanup and close connection to client
  public void close() {
    System.out.println("closing client connection: " + addr + " " + port);
		try {
			in.close();
			out.close();
			clientSocket.close();
		}
		catch (Exception e) {
			System.out.println("socket already closed");
		}
  }
}
