## Starting from scratch
Create three folders in this directory, named 0, 1, and 2.
Each folder will be the directory of the server.

Run `python3 reset.py` to generate some files in these folders.

## Resetting
Run `python3 reset.py` to reset the files to their original state.
This script will also remove all .class files, if you want to recompile.

## Compilation
Run:
```
javac Client.java
javac Server.java
```
This will build all the required files.

## Running
First start all the servers, then the clients.
On each machine running a server, run `java Server <id; 0-2>`
where id is a number 0 to 2, inclusive.

For each machine running a client, run `java Client <id; 0-4> <list of IPs>`
where id is a number 0 to 4, inclusive, and the list of IPs is the IP address
of each server and client, excluding itself. The order of IP addresses is
important: server 0 to server 2, then client 0 to client 4. Do not include the
IP address of the current client.

## Debugging
There are constants defined in Server.java and Client.java that may help you
debug.

BASEPORT, SERVERPORT, and PORT define the starting port ranges for each
machine. Each server/client uses a different port.

Changing NUMCLIENTS will reduce the number of clients in the system.

Changing GENERATE to false will stop the auto-generation of commands.

Chaning DELAY will change the amount of time a command takes to execute,
in milliseconds.
