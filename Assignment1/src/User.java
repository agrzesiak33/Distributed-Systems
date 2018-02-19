import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


//TODO REPLACE ALL INSTANCES OF SUPER WITH AN OBJECT
public class User implements Serializable
{
	BlockingQueue<Message> toClockwiseNeighbor;
	BlockingQueue<Message> toCounterClockwiseNeighbor;
	
	Socket clockwiseNeighbor;
	Socket counterClockwiseNeighbor;
	
	public boolean senderOK;
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
	
	

	public static void main(String[] args) throws IOException 
	{
		System.out.println("Starting");
		User user = new User();
		user.run(args);
		

	}
	
	/*
	 * 0: MY user host name
	 * 1: MY user port number
	 * 2: CONNECTING NODES host name
	 * 3: CONNECTING NODES port number
	 */
	public void run(String[] args) throws IOException
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
		
		int argc = args.length;
		
		
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
		Thread sender = new Thread(new Sender(this));
		this.senderOK = true;
		sender.start();
		
		Thread clockwiseReceiver = new Thread(new Receiver(true));
		Thread counterClockwiseReceiver = new Thread(new Receiver(false));
		this.receiverOK = true;
		clockwiseReceiver.start();
		counterClockwiseReceiver.start();
		
		// At this time, the sender and receiver are doing their jobs
		// Now we have to listen for other users contacting myself to get added to the network.
		Socket incomingUser = new Socket();
		while (true) {
			System.out.println("Listening fro users");
            incomingUser = listener.accept();
            System.out.println("User found");
            
            try {
				dealWithNewUser(incomingUser);
			} catch (InterruptedException e) {
				System.err.println("Could not add new node to the network");
			}
        }
	}
	
	private boolean dealWithNewUser( Socket incomingUser) throws InterruptedException, IOException
	{
		ObjectOutputStream output = new ObjectOutputStream(incomingUser.getOutputStream());
		
		// If I am the only node in the network, both directions are set to the new node.
		if(!this.clockwiseNeighbor.isConnected())
		{
			System.out.println("Adding first neighbor");
			// Tell new user that we are the only node in network.
			try {
				Message temp = new Message(this.START_WITH_1_NODE_NETWORK, "", 0);
				System.out.println(temp.typeFlag);
				output.writeObject(temp);
				output.flush();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.clockwiseNeighbor = incomingUser;
			this.counterClockwiseNeighbor = incomingUser;
			
		}
		else
		{
			System.out.println("Adding a node");
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
		ObjectInputStream input;
		try{
			// Establish a socket with the node we want to enter into the network 
			this.clockwiseNeighbor = new Socket("localhost", connectingPort);
			
			// We now listen for a message telling us how to connect.
			input = new ObjectInputStream(this.clockwiseNeighbor.getInputStream());
			Object temp = input.readObject();
			Message message = (Message) temp;
			
			// If me and the contacting node are the only ones in the network.
			if(message.typeFlag == this.START_WITH_1_NODE_NETWORK)
			{
				this.counterClockwiseNeighbor = this.clockwiseNeighbor;
			}
			// If we need to listen for another node to make contact.
			else
			{
				this.counterClockwiseNeighbor = listener.accept();
			}
			
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
//		else if(!this.counterClockwiseNeighbor.isClosed())
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
	
	
	private class Sender extends User implements Runnable, Serializable
	{
		User user;
		public Sender(User user)
		{
			this.user = user;
		}
		@Override
		public void run() 
		{
			System.out.println("Sender created");
			ObjectOutputStream clockwise;
			System.out.println(user.myHost);
			while(super.clockwiseNeighbor == null);
			System.out.println("starting to send stuff");
			int clockwisePort = super.clockwiseNeighbor.getPort();
			ObjectOutputStream counterClockwise;
			int counterClockwisePort = super.counterClockwiseNeighbor.getPort();
			
			try {
				 clockwise = new ObjectOutputStream(
						super.clockwiseNeighbor.getOutputStream());
				 counterClockwise = new ObjectOutputStream(
						super.counterClockwiseNeighbor.getOutputStream());
			} catch (IOException e) {
				System.err.println("Unable to create output streams");
				return;
			}
			
			while(super.senderOK)
			{
				// If the sockets have changed, we need to get new output streams 
				try{
					if(clockwisePort != super.clockwiseNeighbor.getPort())
					{
						clockwise = new ObjectOutputStream(
								super.clockwiseNeighbor.getOutputStream()); 
						clockwisePort = super.clockwiseNeighbor.getPort();
					}
					if(counterClockwisePort != super.counterClockwiseNeighbor.getPort())
					{
						clockwise = new ObjectOutputStream(
								super.counterClockwiseNeighbor.getOutputStream()); 
						clockwisePort = super.clockwiseNeighbor.getPort();
					}
				}catch(Exception e){System.err.println("Error updating output streams");}
				
				if(!super.toClockwiseNeighbor.isEmpty())
				{
					try {
						clockwise.writeObject(super.toClockwiseNeighbor.take());
					} catch (Exception e) {System.err.println("Error sending to clockwise");}
				}
				if(!super.toCounterClockwiseNeighbor.isEmpty())
				{
					try {
						counterClockwise.writeObject(super.toCounterClockwiseNeighbor.take());
					} catch (Exception e) {System.err.println("Error sending to counter clockwise");}
				}
			}
		}
		
	}
	
	private class Receiver extends User implements Runnable, Serializable
	{
		boolean clockwise;
		public Receiver(boolean isClockwise)
		{
			this.clockwise = isClockwise;
		}
		
		@Override
		public void run() 
		{
			while(super.clockwiseNeighbor == null);
			System.out.println("Starting to listen for messages");
			ObjectInputStream input;
			try
			{
				if(clockwise)
				{
					input = new ObjectInputStream(super.clockwiseNeighbor.getInputStream());
				}
				else
				{
					input = new ObjectInputStream(super.counterClockwiseNeighbor.getInputStream());
				}
				
			}catch(Exception e){System.err.println("Couldn't setup input streams");return;}
			
			Message message = null;
			while(super.receiverOK)
			{
				try 
				{
					dealWithMessage((Message) input.readObject());
				} catch (ClassNotFoundException | IOException e) 
				{
					System.err.println("Error reading from the socket");
				}
				
			}
			
		}

		private void dealWithMessage(Message message) 
		{
			if(message.typeFlag == super.REGULAR_MESSAGE)
			{
				System.out.println(message.message);
			}
			// If it's the case where out neighbor is letting us know to make a connection with a node
			else if(message.typeFlag == super.FORWARDING_PORT_FOR_CONTACT)
			{
				int port = Integer.parseInt(message.message);
				try {
					Socket newNeighbor = new Socket("localhost", port);
					if(!super.clockwiseNeighbor.isClosed())
					{
						super.clockwiseNeighbor.close();
					}
					super.clockwiseNeighbor = newNeighbor;
				} catch (IOException e) {
					System.err.println("Couldn't establish a connection with the new node");
				}
			}
			else if(message.typeFlag == super.QUIT_WITH_2_NODE_NETWORK)
			{
				try {
					super.clockwiseNeighbor.close();
					super.counterClockwiseNeighbor.close();
				} catch (IOException e) {
					System.err.println("Couldn't close the connections to the neighbors");
				}
			}
			else if(message.typeFlag == super.QUIT_WITH_PORT)
			{
				int port = Integer.parseInt(message.message);
				try {
					Socket newNeighbor = new Socket("localhost", port);
					super.clockwiseNeighbor.close();
					super.clockwiseNeighbor = newNeighbor;
				} catch (IOException e) {
					System.err.println("Couldn't set a node as my new neighbor QWP");
				}
			}
			else if(message.typeFlag == super.QUIT_NOTIFICATION)
			{
				Socket newNeighbor;
				try {
					ServerSocket listener = new ServerSocket();
					newNeighbor = listener.accept();
					super.counterClockwiseNeighbor.close();
					super.counterClockwiseNeighbor = newNeighbor;
					listener.close();
				} catch (IOException e) {
					System.err.println("Error listening and adding new neighbor QN");
				}
			}
		}
	}
}
