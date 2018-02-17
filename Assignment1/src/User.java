import java.io.IOException;
import java.io.ObjectOutputStream;
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
	
	boolean senderOK;
	boolean receiverOK;
	
	ReentrantLock counterClockwiseLock;
	
	HashSet<Integer> messages;
	
	String myHost;
	int myPort;
	
	int START_WITH_1_NODE_NETWORK = 1;
	int START_WITH_AVERAGE_NETWORK = 2;
	int FORWARDING_PORT_FOR_CONTACT = 3;
	int REGULAR_MESSAGE = 4;
	int QUIT_WITH_2_NODE_NETWORK = 5;
	int QUIT_NOTIFICATION = 6;
	int QUIT_WITH_PORT = 7;
	
	

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
		this.senderOK = true;
		sender.start();
		
		Thread receiver = new Thread(new Receiver());
		this.receiverOK = true;
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
	
	private boolean dealWithNewUser(Socket incomingUser) throws InterruptedException, IOException
	{
		ObjectOutputStream output = new ObjectOutputStream(incomingUser.getOutputStream());
		
		// If I am the only node in the network, both directions are set to the new node.
		if(!this.clockwiseNeighbor.isConnected())
		{
			// Tell new user that we are the only node in network.
			output.writeObject(new Message(this.START_WITH_1_NODE_NETWORK, "", 0));
			
			this.clockwiseNeighbor = incomingUser;
			this.counterClockwiseNeighbor = incomingUser;
			
		}
		else
		{
			// Send connecting user a message that it will expect a connection from neighbor
			output.writeObject(new Message(this.START_WITH_AVERAGE_NETWORK, "", 0));
			
			// Contact counter clockwise neighbor and have it make contact with incomingUSer
			this.toCounterClockwiseNeighbor.put
				(new Message(this.FORWARDING_PORT_FOR_CONTACT, incomingUser.getPort(), 0));
			
			// Disconnect from my counter clockwise neighbor once all messages are sent
			// TODO
			while(!this.toCounterClockwiseNeighbor.isEmpty());		
			try {this.counterClockwiseNeighbor.close();} catch (Exception e) {}
			
			// Update counter clockwise neighbor to the new user
			this.counterClockwiseNeighbor = incomingUser;			
		}
		output.close();
		return true;
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
	
//	// TODO: Algorithm needs tuning.
//	private boolean removeMyselfFromNetwork() throws InterruptedException
//	{
//		// If there are only 2 nodes in the network.
//		if(this.counterClockwiseNeighbor.getPort() == this.clockwiseNeighbor.getPort())
//		{
//			this.toCounterClockwiseNeighbor.put(new Message(this.QUIT_WITH_2_NODE_NETWORK, "", 0));
//			while(!this.toCounterClockwiseNeighbor.isEmpty());		
//			try {this.counterClockwiseNeighbor.close(); this.clockwiseNeighbor.close();} catch (Exception e) {}
//		}
//		//	 If there are more than 2 nodes in the network.
//		else if(this.counterClockwiseNeighbor.isConnected())
//		{
//			// Tell counter clockwise neighbor to contact clockwise neighbor to make a connection
//			this.toCounterClockwiseNeighbor.put(
//					new Message(this.QUIT_WITH_PORT, this.clockwiseNeighbor.getPort(), 0));
//			this.toClockwiseNeighbor.put(new Message(this.QUIT_NOTIFICATION, "", 0));
//			
//			
//		}
//		this.senderOK = false;
//		this.receiverOK = false;
//		return true;
//	}
	
	
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
		int typeFlag;
		String message;
		int id;
		
		public Message(int typeFlag, String message, int id)
		{
			this.typeFlag = typeFlag;
			this.message = message;
			this.id = id;
		}
		public Message(int typeFlag, int port, int id)
		{
			this.typeFlag = typeFlag;
			this.message = Integer.toString(port);
			this.id = id;
		}
	}
}
