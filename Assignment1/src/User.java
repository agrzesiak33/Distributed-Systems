import java.io.IOException;
import java.io.InputStream;
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


public class User implements Serializable
{
	BlockingQueue<Message> toClockwiseNeighbor;
	BlockingQueue<Message> toCounterClockwiseNeighbor;
	
	Socket clockwiseNeighbor;
	Socket counterClockwiseNeighbor;
	
	ObjectOutputStream clockwiseOutput;
	ObjectOutputStream counterClockwiseOutput;
	
	ObjectInputStream clockwiseInput;
	ObjectInputStream counterClockwiseInput;
	
	public boolean senderOK;
	boolean cReceiverOK;
	boolean ccReceiverOK;
	
	
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
		
		this.clockwiseInput = null;
		this.clockwiseOutput = null;
		this.counterClockwiseInput = null;
		this.counterClockwiseOutput = null;
				
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
		
		Thread clockwiseReceiver = new Thread(new Receiver(true, this));
		this.cReceiverOK = true;
		clockwiseReceiver.start();
		
		if(this.counterClockwiseNeighbor.isConnected())
		{
			Thread counterClockwiseReceiver = new Thread(new Receiver(false, this));
			counterClockwiseReceiver.start();
			this.ccReceiverOK = true;
		}
		
		
		
		// At this time, the sender and receiver are doing their jobs
		// Now we have to listen for other users contacting myself to get added to the network.
		@SuppressWarnings("resource")
		Socket incomingUser = new Socket();
		while (true) {
			System.out.println("Listening fro users");
            incomingUser = listener.accept();
            System.out.println("User found");
            
            try {
				boolean startThread = dealWithNewUser(incomingUser);
				if(startThread)
				{
					Thread counterClockwiseReceiver = new Thread(new Receiver(false, this));
					counterClockwiseReceiver.start();
					this.ccReceiverOK = true;
				}
			} catch (InterruptedException e) {
				System.err.println("Could not add new node to the network");
			}
        }
	}
	
	private boolean dealWithNewUser( Socket incomingUser) throws InterruptedException, IOException
	{
		ObjectOutputStream output = new ObjectOutputStream(incomingUser.getOutputStream());
		boolean addingTo2NodeNetwork = this.counterClockwiseNeighbor.isClosed();
		
		// If I am the only node in the network, both directions are set to the new node.
		if(!this.clockwiseNeighbor.isConnected())
		{
			System.out.println("Adding first neighbor");
			// Tell new user that we are the only node in network.
			try {
				Message temp = new Message(this.START_WITH_1_NODE_NETWORK, "", 0);
				output.writeObject(temp);
				output.flush();
			} catch (Exception e) {}
			
			this.clockwiseNeighbor = incomingUser;
			this.clockwiseOutput = output;
			
		}
		else
		{
			System.out.println("Adding a node");
			// Send connecting user a message that it will expect a connection from neighbor
			output.writeObject(new Message(this.START_WITH_AVERAGE_NETWORK, "", 0));
			
			// In the case we don't have a counterclockwise neighbor, there are only 2 nodes in the network.
			if(this.counterClockwiseNeighbor.isClosed())
			{
				this.toClockwiseNeighbor.put(
						new Message(this.FORWARDING_PORT_FOR_CONTACT, incomingUser.getPort(), 0));
			}
			else
			{
				this.toCounterClockwiseNeighbor.put(
						new Message(this.FORWARDING_PORT_FOR_CONTACT, incomingUser.getPort(), 0));
			}			
			
			// Disconnect from my counter clockwise neighbor once all messages are sent
			while(!this.toCounterClockwiseNeighbor.isEmpty());		
			if(this.counterClockwiseNeighbor.isConnected())
			{
				try {this.counterClockwiseNeighbor.close();} catch (Exception e) {}

			}
			// Update counter clockwise neighbor to the new user
			this.counterClockwiseNeighbor = incomingUser;			
		}
		return addingTo2NodeNetwork;
	}
	
	private boolean addMyselfToNetwork(String connectingHost, int connectingPort, ServerSocket listener)
	{
		try{
			// Establish a socket with the node we want to enter into the network 
			this.clockwiseNeighbor = new Socket("localhost", connectingPort);
			
			// We now listen for a message telling us how to connect.
			this.clockwiseInput = new ObjectInputStream(this.clockwiseNeighbor.getInputStream());
			Message message = (Message) this.clockwiseInput.readObject();
			
			// If me and the contacting node are NOT the only ones in the network we listen for another node to make contact
			if(message.typeFlag != this.START_WITH_1_NODE_NETWORK)
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
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		User user;
		public Sender(User user)
		{
			this.user = user;
		}
		@Override
		public void run() 
		{
			System.out.println("Sender created");
			System.out.println(user.myHost);
			while(!user.clockwiseNeighbor.isConnected());
			System.out.println("starting to send stuff");
			int clockwisePort = user.clockwiseNeighbor.getPort();
			int counterClockwisePort = user.counterClockwiseNeighbor.getPort();
			
			try {
				if(user.clockwiseOutput == null)
				{
					user.clockwiseOutput = new ObjectOutputStream(user.clockwiseNeighbor.getOutputStream());
				}
				if(counterClockwisePort != 0 && user.counterClockwiseOutput == null)
				{
					user.counterClockwiseOutput = new ObjectOutputStream(user.counterClockwiseNeighbor.getOutputStream());
				}
				
			} catch (IOException e) {
				System.err.println("Unable to create output streams");
				System.out.println(e.getMessage());
				return;
			}
			
			while(user.senderOK)
			{
				// If the sockets have changed, we need to get new output streams 
				try{
					if(clockwisePort != user.clockwiseNeighbor.getPort())
					{
						user.clockwiseOutput = new ObjectOutputStream(
								user.clockwiseNeighbor.getOutputStream()); 
						
						clockwisePort = user.clockwiseNeighbor.getPort();
					}
					if(counterClockwisePort != user.counterClockwiseNeighbor.getPort())
					{
						user.counterClockwiseOutput = new ObjectOutputStream(
								user.counterClockwiseNeighbor.getOutputStream()); 
						
						counterClockwisePort = user.counterClockwiseNeighbor.getPort();
					}
				}catch(Exception e){System.err.println("Error updating output streams");}
				
				// If there is data to write, we send it.
				if(!user.toClockwiseNeighbor.isEmpty())
				{
					try {
						user.clockwiseOutput.writeObject(user.toClockwiseNeighbor.take());
					} catch (Exception e) {System.err.println("Error sending to clockwise");}
				}
				if(!user.toCounterClockwiseNeighbor.isEmpty())
				{
					try {
						user.counterClockwiseOutput.writeObject(user.toCounterClockwiseNeighbor.take());
					} catch (Exception e) {System.err.println("Error sending to counter clockwise");}
				}
			}
		}	
	}
	
	private class Receiver extends User implements Runnable, Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		boolean clockwise;
		User user;
		public Receiver(boolean isClockwise, User user)
		{
			this.user = user;
			this.clockwise = isClockwise;
		}
		
		@Override
		public void run() 
		{
			while(!user.clockwiseNeighbor.isConnected());
			System.out.println("Starting to listen for messages");
			int listeningPort;
			boolean runningFlag;
			try
			{
				if(clockwise)
				{
					if(user.clockwiseInput == null)
					{
						user.clockwiseInput = new ObjectInputStream(user.clockwiseNeighbor.getInputStream());
					}
					
					listeningPort = user.clockwiseNeighbor.getPort();
					runningFlag = user.cReceiverOK;
				}
				else
				{
					if(user.counterClockwiseInput == null)
					{
						user.counterClockwiseInput = new ObjectInputStream(user.counterClockwiseNeighbor.getInputStream());
					}
					
					listeningPort = user.counterClockwiseNeighbor.getPort();
					runningFlag = user.ccReceiverOK;
				}
				
			}catch(Exception e){
				System.err.println("Couldn't setup input streams");
				return;
				}
			
			while(runningFlag)
			{
				// If the sockets have changes, we need new input streams
				try{
					if(this.clockwise && user.clockwiseNeighbor.getPort() != listeningPort)
					{
						user.clockwiseInput = new ObjectInputStream(user.clockwiseNeighbor.getInputStream());
						listeningPort = user.clockwiseNeighbor.getPort();
					}
					if(!this.clockwise && user.counterClockwiseNeighbor.getPort() != listeningPort)
					{
						user.counterClockwiseInput = new ObjectInputStream(user.counterClockwiseNeighbor.getInputStream());
						listeningPort = user.clockwiseNeighbor.getPort();
					}
				}catch(Exception e){System.err.println("Error updating output streams");}
				
				try 
				{
					// Read in a message
					Message message;
					if(this.clockwise)
					{
						message = (Message) user.clockwiseInput.readObject();
					}
					else
					{
						message = (Message) user.counterClockwiseInput.readObject();
					}
					dealWithMessage(message);
				} catch (ClassNotFoundException | IOException e) 
				{
					System.err.println("Error reading from the socket");
				}
				
			}
			
		}

		private void dealWithMessage(Message message) 
		{
			if(message.typeFlag == user.REGULAR_MESSAGE)
			{
				System.out.println(message.message);
			}
			// If it's the case where out neighbor is letting us know to make a connection with a node
			else if(message.typeFlag == user.FORWARDING_PORT_FOR_CONTACT)
			{
				int port = Integer.parseInt(message.message);
				try {
					Socket newNeighbor = new Socket("localhost", port);
					if(!user.clockwiseNeighbor.isClosed())
					{
						user.clockwiseNeighbor.close();
					}
					
					//If there are only two nodes in the network, both have each other as the clockwise neighbor so 
					// pointers needs to be rearranged.
					if(user.counterClockwiseNeighbor.isClosed())
					{
						user.counterClockwiseNeighbor = user.clockwiseNeighbor;
					}
					user.clockwiseNeighbor = newNeighbor;
				} catch (IOException e) {
					System.err.println("Couldn't establish a connection with the new node");
				}
			}
			else if(message.typeFlag == user.QUIT_WITH_2_NODE_NETWORK)
			{
				try {
					user.clockwiseNeighbor.close();
					user.counterClockwiseNeighbor.close();
				} catch (IOException e) {
					System.err.println("Couldn't close the connections to the neighbors");
				}
			}
			else if(message.typeFlag == user.QUIT_WITH_PORT)
			{
				int port = Integer.parseInt(message.message);
				try {
					Socket newNeighbor = new Socket("localhost", port);
					user.clockwiseNeighbor.close();
					user.clockwiseNeighbor = newNeighbor;
				} catch (IOException e) {
					System.err.println("Couldn't set a node as my new neighbor QWP");
				}
			}
			else if(message.typeFlag == user.QUIT_NOTIFICATION)
			{
				Socket newNeighbor;
				try {
					ServerSocket listener = new ServerSocket();
					newNeighbor = listener.accept();
					user.counterClockwiseNeighbor.close();
					user.counterClockwiseNeighbor = newNeighbor;
					listener.close();
				} catch (IOException e) {
					System.err.println("Error listening and adding new neighbor QN");
				}
			}
		}
	}
}
