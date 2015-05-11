package edu.upenn.cis.cis455.webserver;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.http.HttpServlet;
import org.apache.log4j.Logger;

public class HttpServer {

	static final Logger logger = Logger.getLogger(HttpServer.class);

	public static void main(String args[]) throws Exception
	{
		if (args.length < 2)
		{
			System.out.println("Please pass command line arguments\n");
			System.exit(0);
		}
		int port = Integer.parseInt(args[0]); //The port number
		if (port>65535) {
			System.out.println("Try again with a valid port number\n");
			System.exit(0);
		}
		String directory = args[1]; // The home directory
		if(directory.endsWith("/"))
			directory = directory.substring(0,directory.length()-1);
		String webdotxml = args[2];
		new Server(port, directory, webdotxml);
	}

}

class Server implements Runnable {

	static final Logger logger = Logger.getLogger(HttpServer.class);

	private int port;
	private Queue<Socket> Q = new LinkedList<Socket>();
	public static String directory;
	public static String webdotxml;
	public static boolean alive = true;
	public static ServerSocket serverSocket;
	public static ArrayList<Thread> threadpool = new ArrayList<Thread>();

	public Server(int port, String directory, String webdotxml) throws Exception
	{
		this.port = port;
		Server.webdotxml = webdotxml;
		Server.directory = directory;
		try {
			serverSocket = new ServerSocket(this.port); //Bind server socket to the port
			Thread t = new Thread(this);
			t.setPriority(10); //Set a higher priority for this thread as it is responsible for adding requests to the queue
			t.start();
		}
		catch(IOException e) {
			logger.error("Error starting the server\n");
			System.exit(0);
		}
		Handler handler = new Handler(Q);
		for(int i=0;i<20;i++) {
			Thread handlerThread = new Thread(handler);
			threadpool.add(handlerThread);
		}
		for (Thread t : threadpool) { //Start all the worker threads
			t.start();
		}
		Timer timer = new Timer();
		Thread daemon = new Thread(timer);
		daemon.setDaemon(true);
		daemon.start();
	}

	public void run() {
		while(alive) { //Loop until a command for shutdown is received
			Socket clientSocket;
			try {
				clientSocket = serverSocket.accept(); //Listen for requests
				synchronized(Q) {
					Q.add(clientSocket); //Add the client socket to the queue
					Q.notify(); //Wake up one of the waiting worker threads
				}
			}
			catch (SocketException e) {
				logger.error("Server stopped receiving requests");
			}
			catch (IOException e) {
				logger.error("Exception while trying to write to queue");
			}
		}
		for (Thread t : threadpool) {
			if (t.getState() == Thread.State.RUNNABLE)
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		for (HttpServlet servlet : Container.servlets.values()) {
			servlet.destroy();
		}
		//Inform all the worker threads to shutdown
		synchronized(Q) {
			Q.notifyAll(); //Wake up all the waiting threads so they can shutdown
		}
	}
	
}
