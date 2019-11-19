/**
Marcus Deng
mwd160230
CS 6378.001

This class is for defining a message. It defines the message types, as well a
way to mark the timestamp of a message. Messages consist of the command, time,
body, and ID of the sender.
**/

import java.io.Serializable;
import java.lang.System;

public class Message implements Serializable {
  //command types
  public static final String TEST_MSG = "TEST";
  public static final String INFO_MSG = "INFO";
  public static final String WRIT_MSG = "WRIT";
  public static final String READ_MSG = "READ";
  public static final String EXIT_MSG = "EXIT";
  public static final String DONE_MSG = "DONE";
	public static final String ERR_MSG = "ERR";
  public static final String REQUEST_MSG = "RQST";
  public static final String REPLY_MSG = "RPLY";

  //fields of a message
  private final String cmd;
  private final long timestamp;
  private final String text;
  private final int id;

  public Message (String cmd, String text) {
    this.cmd = cmd;
    this.text = text;
    this.id = -1;
    this.timestamp = System.currentTimeMillis();
  }

  public Message (String cmd, String text, int id) {
    this.cmd = cmd;
    this.text = text;
    this.id = id;
    this.timestamp = System.currentTimeMillis();
  }

  public String getCmd() {
    return cmd;
  }

  public long getTime() {
    return timestamp;
  }

  public String getText() {
    return text;
  }

  public int getId() {
    return id;
  }

  public String toString() {
    return id + " " + cmd + " " + Long.toString(timestamp) + " " + text;
  }
}
