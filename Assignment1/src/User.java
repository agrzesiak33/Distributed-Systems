import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class User implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	LinkedBlockingQueue<Message> toClockwiseNeighbor;
	LinkedBlockingQueue<Message> toCounterClockwiseNeighbor;
	
	Socket clockwiseNeighbor;
	Socket counterClockwiseNeighbor;
	
	int clockWisePort;
	int counterClockWisePort;
	
	ObjectOutputStream clockwiseOutput;
	ObjectOutputStream counterClockwiseOutput;
	
	ObjectInputStream clockwiseInput;
	ObjectInputStream counterClockwiseInput;
	
	public boolean senderOK;
	boolean cReceiverOK;
	boolean ccReceiverOK;
	
	int numReceivers;
	
	HashSet<Integer> messages;
	
	String myHost;
	int myPort;
	
	int MY_PORT_NUMBER = 0;
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
		this.toClockwiseNeighbor = new LinkedBlockingQueue<Message>(40);
		this.toCounterClockwiseNeighbor = new LinkedBlockingQueue<Message>(40);
		
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
		System.out.println("I'M IN NETWORK");
		// Successfully added myself to the network
		Thread sender = new Thread(new Sender(this));
		this.senderOK = true;
		sender.start();
		
		Thread clockwiseReceiver = new Thread(new Receiver(true, this));
		this.cReceiverOK = true;
		clockwiseReceiver.start();
		this.numReceivers = 1;
		
		//If I am connecting to a network with more than 2 nodes.
		if(this.counterClockwiseNeighbor.isConnected())
		{
			Thread counterClockwiseReceiver = new Thread(new Receiver(false, this));
			this.ccReceiverOK = true;
			counterClockwiseReceiver.start();
			this.numReceivers = 2;
		}
		
		
		
		// At this time, the sender and receiver are doing their jobs
		// Now we have to listen for other users contacting myself to get added to the network.
		@SuppressWarnings("resource")
		Socket incomingUser = new Socket();
		while (true) {
			System.out.println("Listening for users");
            incomingUser = listener.accept();
            
            System.out.println("User found");
            
            try {
				boolean startThread = dealWithNewUser(incomingUser);
				// If I am already in a network with 2 nodes and someone connects making 3
				if(startThread)
				{
					Thread counterClockwiseReceiver = new Thread(new Receiver(true, this));
					this.ccReceiverOK = true;
					counterClockwiseReceiver.start();
					this.numReceivers = 2;
				}
			} catch (InterruptedException e) {
				System.err.println("Could not add new node to the network");
			}
        }
	}
	
	private boolean dealWithNewUser( Socket incomingUser) throws InterruptedException, IOException
	{
		ObjectOutputStream output = new ObjectOutputStream(incomingUser.getOutputStream());
		ObjectInputStream input = new ObjectInputStream(incomingUser.getInputStream());
		
		Message message = null;
		try {
			message = (Message) input.readObject();
		} catch (ClassNotFoundException e1) {}
		int incomingUserPortNumber = Integer.parseInt(message.message);
				
		// If I am the only node in the network
		if(!this.clockwiseNeighbor.isConnected())
		{
			System.out.println("ADD NEIGHBOR 1");
			// Tell new user that we are the only node in network.
			try {
				message = new Message(this.START_WITH_1_NODE_NETWORK, "", 0);
				output.writeObject(message);
				output.flush();
			} catch (Exception e) {}
			
			this.clockwiseNeighbor = incomingUser;
			this.clockwiseOutput = output;
			this.clockwiseInput =input;
			this.clockWisePort = incomingUserPortNumber;
			
		}
		else
		{
			System.out.println("Adding a node");
			// Send connecting user a message that it will expect a connection from neighbor
			output.writeObject(new Message(this.START_WITH_AVERAGE_NETWORK, "", 0));
			
			// In the case we don't have a counterclockwise neighbor, there are only 2 nodes in the network.
			if(!this.counterClockwiseNeighbor.isConnected())
			{
				System.out.println("Putting message in clockwise queue");
				Message temp = new Message(this.FORWARDING_PORT_FOR_CONTACT, incomingUserPortNumber, 0);
				this.toClockwiseNeighbor.put(temp);
			}
			// There are more than 2 nodes in the network.
			else
			{
				System.out.println("Putting message in counter clockwise queue");
				this.toCounterClockwiseNeighbor.put(
						new Message(this.FORWARDING_PORT_FOR_CONTACT, incomingUserPortNumber, 0));
			}			
			
			// Disconnect from my counter clockwise neighbor once all messages are sent
			while(!this.toCounterClockwiseNeighbor.isEmpty());		
			if(this.counterClockwiseNeighbor.isConnected())
			{
				try {this.counterClockwiseNeighbor.close();} catch (Exception e) {}

			}
			// Update counter clockwise neighbor to the new user
			this.counterClockwiseNeighbor = incomingUser;	
			this.counterClockwiseInput = input;
			this.counterClockwiseOutput = output;
			this.counterClockWisePort = incomingUserPortNumber;
		}
		return this.clockwiseNeighbor.isConnected() && !this.counterClockwiseNeighbor.isConnected();
	}
	
	private boolean addMyselfToNetwork(String connectingHost, int connectingPort, ServerSocket listener)
	{
		try{
			// Establish a socket with the node we want to enter into the network 
			this.clockwiseNeighbor = new Socket("localhost", connectingPort);
			this.clockwiseOutput = new ObjectOutputStream(this.clockwiseNeighbor.getOutputStream());
			this.clockwiseInput = new ObjectInputStream(this.clockwiseNeighbor.getInputStream());
			
			// Now we send the contacted user a message telling them the port number
			Message message = new Message(
					this.MY_PORT_NUMBER, Integer.toString(listener.getLocalPort()), 0);
			this.clockwiseOutput.writeObject(message);
			
			message = (Message) this.clockwiseInput.readObject();
			System.out.println("ADD_ME " +  message.toString());
			
			// If me and the contacting node are NOT the only ones in the network we listen for another node to make contact
			if(message.typeFlag != this.START_WITH_1_NODE_NETWORK)
			{
				System.out.println("UPDATING CC NEIGHBOR");
				this.counterClockwiseNeighbor = listener.accept();
				System.out.println("Got the socket");
				this.counterClockwiseOutput = new ObjectOutputStream(this.counterClockwiseNeighbor.getOutputStream());
				this.counterClockwiseOutput.flush();
				this.counterClockwiseInput = new ObjectInputStream(this.counterClockwiseNeighbor.getInputStream());
				
				
			}			
			return true;
		}catch(Exception e){
			System.out.println(e.toString());
			return false;
		}
	}
	
//	// TODO: Algorithm needs tuning.
//	private boolean removeMyselfFromNetwork() throws InterruptedException
//	{
//		// If there are only 2 nodes in the network.
//		if(this.counterClockwiseNeighbor.getLocalPort() == this.clockwiseNeighbor.getLocalPort())
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
//					new Message(this.QUIT_WITH_PORT, this.clockwiseNeighbor.getLocalPort(), 0));
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
			System.out.println("SENDER");
			while(!user.clockwiseNeighbor.isConnected()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
			System.out.println("SENDER_RUNNING");
			
			try {
				if(user.clockwiseOutput == null)
				{
					user.clockwiseOutput = new ObjectOutputStream(user.clockwiseNeighbor.getOutputStream());
				}
				if(user.counterClockwiseNeighbor.getLocalPort() != -1 && 
						user.counterClockwiseOutput == null)
				{
					user.counterClockwiseOutput = new ObjectOutputStream(user.counterClockwiseNeighbor.getOutputStream());
				}
				
			} catch (IOException e) {
				System.err.println("Unable to create output streams");
				System.out.println(e.getMessage());
			}
			
			while(user.senderOK)
			{		
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {}
				
				// If there is data to write, we send it.
				if(!user.toClockwiseNeighbor.isEmpty())
				{
					try {
						Message message = user.toClockwiseNeighbor.take();
						System.out.println("Sending message: " + message.toString());
						user.clockwiseOutput.writeObject(message);
						user.clockwiseOutput.flush();
					} catch (Exception e) {System.err.println("Error sending to clockwise");}
				}
				if(!user.toCounterClockwiseNeighbor.isEmpty() && user.toCounterClockwiseNeighbor != null)
				{
					try {
						Message message = user.toCounterClockwiseNeighbor.take();
						System.out.println("Sending message: " + message.toString());
						user.counterClockwiseOutput.writeObject(message);
						user.counterClockwiseOutput.flush();
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
			System.out.println("RECEIVER " + clockwise);
			boolean runningFlag = true;;
			
			// Make sure all streams are setup.
			try
			{
				if(clockwise)
				{
					if(user.clockwiseInput == null)
					{
						user.clockwiseInput = new ObjectInputStream(user.clockwiseNeighbor.getInputStream());
					}
				}
				else
				{
					if(user.counterClockwiseInput == null)
					{
						user.counterClockwiseInput = new ObjectInputStream(user.counterClockwiseNeighbor.getInputStream());
					}
				}
				
			}catch(Exception e){
				System.err.println("Couldn't setup input streams");
				return;
				}
			
			while(runningFlag)
			{
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
					System.out.println("Received message: " + message.toString());
					dealWithMessage(message);
				} catch (ClassNotFoundException | IOException e) 
				{
					System.err.println("Error reading from the socket");
				}
				
				runningFlag = (this.clockwise)? user.cReceiverOK : user.ccReceiverOK;
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
					
					//If there are only two nodes in the network, both have each other as the clockwise neighbor so 
					// pointers needs to be rearranged.
					if(!user.counterClockwiseNeighbor.isConnected())
					{
						user.counterClockwiseNeighbor = user.clockwiseNeighbor;
					}
					user.clockwiseNeighbor = newNeighbor;
					user.clockwiseOutput = new ObjectOutputStream(user.clockwiseNeighbor.getOutputStream());
					user.clockwiseInput = new ObjectInputStream(user.clockwiseNeighbor.getInputStream());
					
					if(user.numReceivers < 2)
					{
						Thread counterClockwiseReceiver = new Thread(new Receiver(false, user));
						this.ccReceiverOK = true;
						counterClockwiseReceiver.start();
						this.numReceivers = 2;
					}
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
