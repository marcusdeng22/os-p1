/**
Marcus Deng
mwd160230
CS 6378.001

This class defines the Server process. It will list all of the files in its
associated directory, read the last line of a file, and append to to a file. It
waits for a request, which is only granted by clients via distributed mutual
exclusion using Ricart-Agrwala with Roucairol and Carvalho's optimization.

Usage: java Server <id; 0-2>
**/

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public class Server {
	private static final int BASEPORT = 3000;

	//main server class, starts a thread per connection
	public Server(int id, int port) throws IOException {
		ServerSocket server = new ServerSocket(port);
		System.out.println("Server started on port " + port);
		// started server and waits for a connection
		try {
			while (true) {
				Socket socket = null;
				try {
					socket = server.accept();
					System.out.println("New client connected: " + socket);
					//start a new thread for a new client
					Thread t = new ClientHandler(id, socket);
					t.start();
				}
				catch (Exception e) {
					//unknown exception, so close the clients
					socket.close();
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			server.close();
		}
	}

	public static void main(String args[]) throws IOException {
		if (args.length != 1) {
			System.out.println("please provide server id");
			return;
		}
		int id = 0;
		int port = BASEPORT;
		try {
			id = Integer.parseInt(args[0]);
			port += id;
			if (id < 0 || id > 2) {
				throw new IOException("invalid id");
			}
		}
		catch (Exception e) {
			System.out.println("please provide a valid server id 0-2");
			return;
		}
		Server server = new Server(id, port);
	}
}

//thread that handles communication with a client
class ClientHandler extends Thread {
	private int id;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Socket s;

	//define the communication streams
	public ClientHandler(int id, Socket s) throws IOException {
		this.id = id;
		this.s = s;
		this.out = new ObjectOutputStream(s.getOutputStream());
		this.in = new ObjectInputStream(s.getInputStream());
	}

	//gets the file path of a requested file, depending on the server's ID
	private File getFile(String data) {
		String folder = String.valueOf(this.id);
		String path = folder + File.separatorChar + data;
		File f = new File(path);
		if (f.exists() && !f.isDirectory()) {
			return f;
		}
		return null;
	}

	//main communication method
	@Override
	public void run() {
		Message recv;
		try {
			//test connection
			System.out.println("server hello to client test");
			out.writeObject(new Message(Message.TEST_MSG, "", id));
			//wait for request
			while (true) {
				try {
					recv = (Message) in.readObject();
					System.out.println("recv: " + recv);
					String flag = recv.getCmd();
					String data = recv.getText();
					String ret = data;
					boolean err = false;
					switch (flag) {
						// case Message.EXIT_MSG:
						// 	System.out.println("received exit message, closing self");
						// 	break;
						//INFO returns the names of all files in the directory, or the name of the file
						case Message.INFO_MSG:
							System.out.println("received info msg, info for: " + data);
							if (data.length() == 0) {
								//get all the files on server
								System.out.println("looking for all files");
								File folderPath = new File(String.valueOf(this.id));
								File fileList[] = folderPath.listFiles();
								ret = "";
								for (int x = 0; x < fileList.length; x ++) {
									if (fileList[x].isFile()) {
										ret += fileList[x].getName() + "\n";
									}
								}
								System.out.println("returning: " + ret);
							}
							else {
								//get the specific file on server
								File f = getFile(data);
								if (f != null) {
									System.out.println("found file!");
									ret = data;
								}
								else {
									err = true;
								}
							}
							break;
						//WRIT appends the client ID and timestamp to the file
						case Message.WRIT_MSG:
							System.out.println("received write msg");
							File f1 = getFile(data);
							if (f1 == null) {
									ret = "invalid file";
									err = true;
									break;
							}
							//append to file
							try {
								PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f1, true)));
								out.println(recv.getId() + " " + recv.getTime());
								out.close();
							} catch (IOException e) {
								ret = "invalid file";
								err = true;
								break;
							}
							break;
						//READ returns the last line of the file
						case Message.READ_MSG:
							System.out.println("received read msg");
							File f2 = getFile(data);
							if (f2 == null) {
								ret = "invalid file";
								err = true;
								break;
							}
							//read last line
							BufferedReader input = new BufferedReader(new FileReader(f2));
							String last = "";
							String line = "";
							while ((line = input.readLine()) != null) {
								last = line;
							}
							input.close();
							ret = last;
							break;
					}
					if (err == true) {
						//return ERR message
						out.writeObject(new Message(Message.ERR_MSG, ret, id));
					}
					else {
						//return DONE message
						out.writeObject(new Message(Message.DONE_MSG, ret, id));
					}
				}
				catch (SocketException e) {
					System.out.println("remote unreachable, closing self");
					break;
				}
				catch (EOFException e) {
					System.out.println("remote closed, closing self");
					break;
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			//cleanup
			try {
				System.out.println("closing");
				in.close();
				out.close();
				s.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
