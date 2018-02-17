import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class User 
{
	
	BlockingQueue<Message> toClockwiseNeighbor;
	BlockingQueue<Message> toCounterClockwiseNeighbor;
	
	Socket clockwiseNeighbor;
	Socket counterClockwiseNeighbor;
	
	ReentrantLock counterClockwiseLock;
	
	HashSet<Integer> messages;
	
	String myHost;
	int myPort;
	

	public static void main(String[] args, int argc) 
	{
		
		

	}
	
	/*
	 * 0: MY user host name
	 * 1: MY user port number
	 * 2: CONNECTING NODES host name
	 * 3: CONNECTING NODES port number
	 */
	public void run(String[] args, int argc) throws IOException
	{
		this.toClockwiseNeighbor = new ArrayBlockingQueue<Message>(1024);
		this.toCounterClockwiseNeighbor = new ArrayBlockingQueue<Message>(1024);
		
		this.clockwiseNeighbor = new Socket();
		this.counterClockwiseNeighbor = new Socket();
		
		//	This lock is used to make sure when a new node is added, the 
		this.counterClockwiseLock = new ReentrantLock();
		
		this.messages = new HashSet<Integer>();
		
		this.myHost = args[0];
		this.myPort= Integer.parseInt(args[1]);
		
		ServerSocket listener = new ServerSocket(this.myPort);
		
		
		
		// If more than 2 arguments is passed in, it means this 
		//	user want't to connect to an existing network.
		if(argc == 4)
		{
			if(!addMyselfToNetwork(args[2], Integer.parseInt(args[3]), listener))
			{
				System.err.println("Failed to add myself to the network");
				return;
			}
		}
		
		// Successfully added myself to the network
		Thread sender = new Thread(new Sender());
		sender.start();
		Thread receiver = new Thread(new Receiver());
		receiver.start();
		
		// At this time, the sender and receiver are doing their jobs
		// Now we have to listen for other users contacting myself to get added to the network.
		@SuppressWarnings("resource")
		Socket incomingUser = new Socket();
		while (true) {
            incomingUser = listener.accept();
            
            try {
				dealWithNewUser(incomingUser);
			} catch (InterruptedException e) {
				System.err.println("Could not add new node to the network");
			}
        }
		
		
	}
	
	private boolean dealWithNewUser(Socket incomingUser) throws InterruptedException
	{
		// Contact counter clockwise neighbor and have it make contact with incomingUSer
		this.toCounterClockwiseNeighbor.put(new Message('s', incomingUser.getPort(), 0));
		
		// Disconnect from my counter clockwise neighbor
		while(!this.toCounterClockwiseNeighbor.isEmpty());
		
		this.counterClockwiseLock.lock();
		
		this.counterClockwiseNeighbor.close();
		
		// Set the incomingUser to my new clockwise neighbor
		
		
		return false;
	}
	
	private boolean addMyselfToNetwork(String connectingHost, int connectingPort, ServerSocket listener)
	{
		try{
			// Establish a socket with the node we want to enter into the network 
			this.clockwiseNeighbor = new Socket(connectingHost, connectingPort);
			
			// Start listening for the connecting node to make the moves to connect
			this.counterClockwiseNeighbor = listener.accept();

			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	
	private class Sender implements Runnable
	{

		@Override
		public void run() 
		{
			
		}
		
	}
	
	private class Receiver implements Runnable
	{

		@Override
		public void run() 
		{
			
		}
		
	}

	private class Message
	{
		char typeFlag;
		String message;
		int id;
		
		public Message(char typeFlag, String message, int id)
		{
			this.typeFlag = typeFlag;
			this.message = message;
			this.id = id;
		}
		public Message(char typeFlag, int port, int id)
		{
			this.typeFlag = typeFlag;
			this.message = Integer.toString(port);
			this.id = id;
		}
	}
}
