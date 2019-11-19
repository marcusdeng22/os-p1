/**
Marcus Deng
mwd160230
CS 6378.001

This class manages the communication with a client. It will handle the request
messages and reply. Clients send their requests to here, and we respond if the
algorithm determines so. Otherwise, we defer.
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

//this will handle the REPLY message
class ListenerHandler extends Thread {
  private int id;
  private Socket s;
  private ObjectInputStream in;
  private ObjectOutputStream out;

  private volatile ConcurrentHashMap<String, Boolean> resourceLocks;
  private volatile ConcurrentHashMap<String, ConcurrentLinkedQueue<ObjectOutputStream>> deferred;
  private volatile AtomicLong localTime;
  private volatile ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt;

  private volatile boolean flag = true;

  public ListenerHandler(int id, Socket s, ConcurrentHashMap<String, Boolean> resourceLocks, ConcurrentHashMap<String, ConcurrentLinkedQueue<ObjectOutputStream>> deferred, AtomicLong localTime, ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> opt) throws IOException {
    this.id = id;
    this.s = s;
    this.out = new ObjectOutputStream(s.getOutputStream());
    this.in = new ObjectInputStream(s.getInputStream());
    this.resourceLocks = resourceLocks;
    this.deferred = deferred;
    this.localTime = localTime;
    this.opt = opt;
  }

  //handles REQU and REPLY, as well as TEST messages for establishing connection validity
  @Override
  public void run() {
    try {
      while (flag) {
        //execute the algorithm response here
        //if message = REQUEST for resource, check if resource is used
        //if resource is used; then put in queue
        //else resource is not used, so REPLY
        //if message != REQUEST, then error
        Message rcv = (Message) in.readObject();
        String msg = rcv.getCmd();
        String resource = rcv.getText();
        int msgId = rcv.getId();
        System.out.println("got a message: " + rcv);
        if (msg.equals(Message.REQUEST_MSG)) {
          //optimization: since this process has requested this resource, we will need to ask for permission next time
          ConcurrentHashMap<Integer, Boolean> temp = opt.get(resource);
          temp.replace(msgId, Boolean.TRUE);
          //check if not in critical section or if request's timestamp is less than mine
          if (resourceLocks.get(resource).equals(Boolean.FALSE)) {
            System.out.println("not in critical section");
          }
          else {
            System.out.println("in critical section");
          }
          if (rcv.getTime() < localTime.get()) {
            System.out.println("request timestamp < me");
          }
          else {
            System.out.println("request timestamp > me");
          }
          if (resourceLocks.get(resource).equals(Boolean.FALSE) || rcv.getTime() < localTime.get() || (rcv.getTime() == localTime.getTime() && rcv.getId() < localTime.getId()) {
						Message resp = new Message(Message.REPLY_MSG, resource, id);
						System.out.println("sending REPLY: " + resp);
						out.writeObject(resp);
          }
          else {
            //enqueue the request
            System.out.println("deferring request: BLOCKED");
            ConcurrentLinkedQueue<ObjectOutputStream> deferredSocketQ;
            if (deferred.containsValue(resource)) {
              deferredSocketQ = deferred.get(resource);
            }
            else {
              deferredSocketQ = new ConcurrentLinkedQueue<>();
              deferred.put(resource, deferredSocketQ);
            }
            deferredSocketQ.offer(out);
          }
        }
				else if (msg.equals(Message.TEST_MSG)) {
					out.writeObject(new Message(Message.TEST_MSG, "", id));
				}
        else {
          System.out.println("Invalid request received");
        }
      }
    }
    catch (EOFException eof) {
      System.out.println("eof");
      //trigger exit
      try {
        close();
      }
      catch (Exception e) {
        System.out.println("a client closed unexpectedly, exiting");
      }
    }
    catch (Exception e) {
      System.out.println("socket closed");
    }
    finally {
      try {
				close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    System.out.println("You should exit now...");
  }

  //cleanup and close connection
  public void close() throws IOException {
    flag = false;
    in.close();
		out.close();
		s.close();
  }
}
