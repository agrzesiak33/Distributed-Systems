import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Scanner;
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
		
		this.numReceivers = 0;
		
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
		//System.out.println("I'M IN NETWORK");
		// Successfully added myself to the network
		Thread sender = new Thread(new Sender(this));
		this.senderOK = true;
		sender.start();
		
		//If I am connecting to a network with more than 2 nodes.
		if(this.counterClockwiseNeighbor.isConnected() && this.numReceivers < 2)
		{
			Thread counterClockwiseReceiver = new Thread(new Receiver(false, this));
			this.ccReceiverOK = true;
			counterClockwiseReceiver.start();
			this.numReceivers = 2;
		}
		
		Thread writerThread = new Thread(new Writer(this));
		writerThread.start();
		
		// At this time, the sender and receiver are doing their jobs
		// Now we have to listen for other users contacting myself to get added to the network.
		@SuppressWarnings("resource")
		Socket incomingUser = new Socket();
		while (true) {
			//System.out.println("Listening for users");
            incomingUser = listener.accept();
            
            //System.out.println("User found");
            
            try {
				boolean startThread = dealWithNewConnection(incomingUser);
				// If I am already in a network with 2 nodes and someone connects making 3
				//System.out.println(this.numReceivers);
				if(startThread && this.numReceivers < 2)
				{
					//System.out.println("Starting a new counter clockwise receiver");
					Thread counterClockwiseReceiver = new Thread(new Receiver(false, this));
					this.ccReceiverOK = true;
					counterClockwiseReceiver.start();
					this.numReceivers = 2;
				}
			} catch (InterruptedException e) {
				//System.err.println("Could not add new node to the network");
			}
        }
	}
	
	private boolean dealWithNewConnection( Socket incomingUser) throws InterruptedException, IOException
	{
		ObjectOutputStream output = new ObjectOutputStream(incomingUser.getOutputStream());
		ObjectInputStream input = new ObjectInputStream(incomingUser.getInputStream());
		
		boolean createAnotherReceiver = !this.counterClockwiseNeighbor.isConnected();
		Message message = null;
		int incomingUserPort = 0;
		try {
			message = (Message) input.readObject();
		} catch (ClassNotFoundException e1) {}
		try
		{
			incomingUserPort = Integer.parseInt(message.message);
		}
		catch(Exception e)
		{
			return false;
		}
		
		
		// If there is the quit flag set, it means a node is contacting us to fill a gap
		if(message.typeFlag == this.QUIT_WITH_PORT)
		{ 
			this.clockwiseNeighbor = incomingUser;
			this.clockwiseInput = input;
			this.clockwiseOutput = output;
			this.clockWisePort = incomingUserPort;
			
			if(this.clockWisePort == this.counterClockWisePort)
			{
				this.ccReceiverOK = false;
				this.counterClockwiseNeighbor.close();
			}
			
		}
		else
		{
			createAnotherReceiver = addNewNodeToNetwork(incomingUser, output, input, createAnotherReceiver,
					incomingUserPort);
		}
		return createAnotherReceiver;
	}

	private boolean addNewNodeToNetwork(Socket incomingUser, ObjectOutputStream output, ObjectInputStream input,
			boolean createAnotherReceiver, int incomingUserPortNumber) throws IOException, InterruptedException 
	{
		Message message;
		System.out.println("Adding a node at " + incomingUserPortNumber);
		// If I am the only node in the network
		if(!this.clockwiseNeighbor.isConnected())
		{
			//System.out.println("ADD NEIGHBOR 1");
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
			
			Thread clockwiseReceiver = new Thread(new Receiver(true, this));
			this.cReceiverOK = true;
			clockwiseReceiver.start();
			this.numReceivers++;
			
			createAnotherReceiver = false;
			
		}
		else
		{
			// Send connecting user a message that it will expect a connection from neighbor
			output.writeObject(new Message(this.START_WITH_AVERAGE_NETWORK, "", 0));
			
			// In the case we don't have a counterclockwise neighbor, there are only 2 nodes in the network.
			if(!this.counterClockwiseNeighbor.isConnected())
			{
				//System.out.println("Putting message in clockwise queue");
				Message temp = new Message(this.FORWARDING_PORT_FOR_CONTACT, incomingUserPortNumber, 0);
				this.toClockwiseNeighbor.put(temp);
			}
			// There are more than 2 nodes in the network.
			else
			{
				//System.out.println("Putting message in counter clockwise queue");
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
		return createAnotherReceiver;
	}
	
	private boolean addMyselfToNetwork(String connectingHost, int connectingPort, ServerSocket listener)
	{
		try{
			// Establish a socket with the node we want to enter into the network 
			this.clockwiseNeighbor = new Socket("localhost", connectingPort);
			this.clockwiseOutput = new ObjectOutputStream(this.clockwiseNeighbor.getOutputStream());
			this.clockwiseInput = new ObjectInputStream(this.clockwiseNeighbor.getInputStream());
			this.clockWisePort = connectingPort;
			
			// Now we send the contacted user a message telling them the port number
			Message message = new Message(
					this.MY_PORT_NUMBER, Integer.toString(listener.getLocalPort()), 0);
			this.clockwiseOutput.writeObject(message);
			
			message = (Message) this.clockwiseInput.readObject();
			//System.out.println("ADD_ME " +  message.toString());
			
			Thread clockwiseReceiver = new Thread(new Receiver(true, this));
			this.cReceiverOK = true;
			clockwiseReceiver.start();
			this.numReceivers++;
			
			// If me and the contacting node are NOT the only ones in the network we listen for another node to make contact
			if(message.typeFlag != this.START_WITH_1_NODE_NETWORK)
			{
				//System.out.println("UPDATING CC NEIGHBOR");
				this.counterClockwiseNeighbor = listener.accept();
				//System.out.println("Got the socket");
				this.counterClockwiseOutput = new ObjectOutputStream(this.counterClockwiseNeighbor.getOutputStream());
				this.counterClockwiseOutput.flush();
				this.counterClockwiseInput = new ObjectInputStream(this.counterClockwiseNeighbor.getInputStream());	
				
				Message portMessage = (Message)this.counterClockwiseInput.readObject();
				this.counterClockWisePort = Integer.parseInt(portMessage.message);
				
				Thread counterClockwiseReceiver = new Thread(new Receiver(false, this));
				this.ccReceiverOK = true;
				counterClockwiseReceiver.start();
				this.numReceivers++;
				//System.out.println(" Added myself: " + Integer.toString(this.numReceivers));
			}		
			System.out.println("You are now connected to the network");
			return true;
		}catch(Exception e){
			//System.out.println(e.toString());
			return false;
		}
	}
	
	private boolean removeMyselfFromNetwork() throws InterruptedException
	{
		this.ccReceiverOK = false;
		this.cReceiverOK = false;
		this.senderOK = false;
		
		// If I am the only node in the network
		if((!this.clockwiseNeighbor.isConnected() || this.clockwiseNeighbor.isClosed()) &&
				(!this.counterClockwiseNeighbor.isConnected() || this.counterClockwiseNeighbor.isClosed()))
		{
			System.exit(0);
		}
		
		// If we're here there is at least one other node. 
		// If there are only 2 nodes in the network
		if(!this.counterClockwiseNeighbor.isConnected() || this.counterClockwiseNeighbor.isClosed())
		{	
			this.toClockwiseNeighbor.put(new Message(this.QUIT_NOTIFICATION, "", 0));
		}
		else
		{
			this.toCounterClockwiseNeighbor.put(new Message(this.QUIT_NOTIFICATION, "", 0));
			
			//System.out.println("Port: " + this.clockWisePort);
			this.toClockwiseNeighbor.put(new Message(
					this.QUIT_WITH_PORT, Integer.toString(this.clockWisePort), 0));
			
			// Close connections to counter clockwise neighbor
			while(!this.toCounterClockwiseNeighbor.isEmpty());
			try {
				this.counterClockwiseInput.close();
				this.counterClockwiseOutput.close();
				this.counterClockwiseNeighbor.close();
			} catch (Exception e) {
				//System.err.println("Couldn't quit counter clockwise neighbor");
			}
		}
		// 	Close the connections to clockwise neighbor
		while(!this.toClockwiseNeighbor.isEmpty());
		try {
			this.clockwiseInput.close();
			this.clockwiseOutput.close();
			this.clockwiseNeighbor.close();
		} catch (IOException e) {
			//System.err.println("Couldn't quit clockwise neighbor");
		}
		System.exit(0);
		return true;
	}
	
	
    private class Writer implements Runnable {

        User currentUser;

        private boolean keepSending;
        public Writer(User u)
        {
            currentUser = u;
            keepSending =true;
        }

        @Override
        public void run()
        {
            Scanner myScanner = new Scanner(System.in);
            //System.out.println("Now user can write a message");
            while(keepSending)
            {
                String userMessage = myScanner.nextLine();
                //in case keepsending changed while waiting user input
                if (!keepSending)
                    break;

                //splits user input to check what command is used
                String[] command = userMessage.split(" ");

                if (command[0].equalsIgnoreCase("-help"))
                {
                    //System.out.println("For sending a message write -send and your message");
                    //System.out.println("For quiting write -quit");
                }
                else if (command[0].equalsIgnoreCase("-send"))
                {
                    //messages that are empty or made of only spaces are not accepted.
                    if (command.length <=1)
                    {
                        //System.out.println("Error: after -send command there must be a message written. Ex: -send Hello World.");
                    }
                    else {
                        userMessage = userMessage.substring(1+command[0].length());
                        SecureRandom SR = new SecureRandom();
                        int id = SR.nextInt(100000000); //id is random number between 1 and 100 million
                        Message regularMessage = new Message(currentUser.REGULAR_MESSAGE,userMessage,id);
                        
                        try {
                        	if(currentUser.clockwiseNeighbor.isConnected())
                        	{
                        		currentUser.toClockwiseNeighbor.put(regularMessage);
                        	}
                        	if(currentUser.counterClockwiseNeighbor.isConnected())
                        	{
                        		currentUser.toCounterClockwiseNeighbor.put(regularMessage);
                        	}
							
						} catch (InterruptedException e) {
							//System.err.println("Couldn't send the message");
						}
                    }

                }
                else if (command[0].equalsIgnoreCase("-quit"))
                {

                    try {
						currentUser.removeMyselfFromNetwork();
					} catch (InterruptedException e) {
						//System.err.println("Couldn't remove myself");
					}
                    keepSending =false;
                }
                else {
                    //System.out.println("Command Error. Type -help for help");
                }
            }
            myScanner.close();

        }
    }	
	
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
			//System.out.println("SENDER");
			while(!user.clockwiseNeighbor.isConnected()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
			//System.out.println("SENDER_RUNNING");
			
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
				//System.err.println("Unable to create output streams");
				//System.out.println(e.getMessage());
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
						//System.out.println("Sending message: " + message.toString());
						user.clockwiseOutput.writeObject(message);
						user.clockwiseOutput.flush();
					} catch (Exception e) {
						//System.err.println("Error sending to clockwise");
						}
				}
				if(!user.toCounterClockwiseNeighbor.isEmpty() && user.counterClockwiseNeighbor.isConnected())
				{
					try {
						Message message = user.toCounterClockwiseNeighbor.take();
						//System.out.println("Sending message: " + message.toString());
						user.counterClockwiseOutput.writeObject(message);
						user.counterClockwiseOutput.flush();
					} catch (Exception e) {//System.err.println("Error sending to counter clockwise");
					}
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
			//System.out.println("RECEIVER " + clockwise);
			boolean runningFlag = true;;
			
			// Make sure all streams are setup.
			try
			{
				if(clockwise)
				{
					if(user.clockwiseInput == null)
					{
						//System.out.println("Creating new clockwise input stream");
						user.clockwiseInput = new ObjectInputStream(user.clockwiseNeighbor.getInputStream());
					}
				}
				else
				{
					if(user.counterClockwiseInput == null)
					{
						//System.out.println("Creating new counter clockwise input stream");
						user.counterClockwiseInput = new ObjectInputStream(user.counterClockwiseNeighbor.getInputStream());
					}
				}
				
			}catch(Exception e){
				//System.err.println("Couldn't setup input streams");
				return;
				}
			
			while(runningFlag)
			{
				try 
				{
					// Read in a message
					Message message = null;
					if(this.clockwise && !user.clockwiseNeighbor.isClosed())
					{
						message = (Message) user.clockwiseInput.readObject();
					}
					else if(!this.clockwise && !user.counterClockwiseNeighbor.isClosed())
					{
						message = (Message) user.counterClockwiseInput.readObject();
					}
					
					if(message != null)
					{
						//System.out.println("Received message: " + message.toString());
						dealWithMessage(message);
					}
					
				} catch (ClassNotFoundException | IOException e) 
				{
					//System.err.println("Error reading from the socket");
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
				System.out.println("Adding node at " + port);
				try {
					Socket newNeighbor = new Socket("localhost", port);
					
					//If there are only two nodes in the network, both have each other as the clockwise neighbor so 
					// pointers needs to be rearranged.
					if(!user.counterClockwiseNeighbor.isConnected())
					{
						user.counterClockwiseNeighbor = user.clockwiseNeighbor;
						user.counterClockwiseInput = user.clockwiseInput;
						user.counterClockwiseOutput = user.counterClockwiseOutput;
					}
					user.clockwiseNeighbor = newNeighbor;
					user.clockwiseOutput = new ObjectOutputStream(user.clockwiseNeighbor.getOutputStream());
					user.clockwiseInput = new ObjectInputStream(user.clockwiseNeighbor.getInputStream());
					
					user.clockwiseOutput.writeObject(new Message(user.MY_PORT_NUMBER, Integer.toString(user.myPort), 0));
					
					//System.out.println("receivedMessage" + user.numReceivers);
					if(user.numReceivers < 2)
					{
						Thread counterClockwiseReceiver = new Thread(new Receiver(false, user));
						this.ccReceiverOK = true;
						counterClockwiseReceiver.start();
						this.numReceivers = 2;
					}
				} catch (IOException e) {
					//System.err.println("Couldn't establish a connection with the new node");
				}
			}
			else if(message.typeFlag == user.QUIT_WITH_2_NODE_NETWORK)
			{
				// If here, we got a message that we are now in a 2 node network
				user.ccReceiverOK = false;
				user.clockwiseNeighbor = user.counterClockwiseNeighbor;
				user.clockwiseOutput = user.counterClockwiseOutput;
				user.clockwiseInput = user.counterClockwiseInput;
				user.clockWisePort = user.counterClockWisePort;
				user.counterClockWisePort = -1;
			}
			else if(message.typeFlag == user.QUIT_WITH_PORT)
			{
				int port = Integer.parseInt(message.message);
				System.out.println(port + " just left");
				try {
					// As long as the neighbor we are wanting to contact is not already our neighbor...
					if(port != user.clockWisePort)
					{
						Socket newNeighbor = new Socket("localhost", port);
						user.ccReceiverOK = false;
						user.counterClockwiseNeighbor.close();
						user.counterClockwiseNeighbor = newNeighbor;
						user.counterClockwiseOutput = new ObjectOutputStream(user.counterClockwiseNeighbor.getOutputStream());
						user.counterClockwiseInput = new ObjectInputStream(user.counterClockwiseNeighbor.getInputStream());
						user.counterClockWisePort = port;
						user.ccReceiverOK = true;
					}
					// if it is, we send it a message to make us the clockwise neighbor
					else
					{
						user.toClockwiseNeighbor.put(new Message(user.QUIT_WITH_2_NODE_NETWORK, "",0));
					}
				} catch (IOException | InterruptedException e) {
					//System.err.println("Couldn't set a node as my new neighbor QWP");
					//System.out.println(e.toString());
				}
			}
			// If we are receiving this, we just have to remove our clockwise neighbor
			else if(message.typeFlag == user.QUIT_NOTIFICATION)
			{
				// We just have to get rid of the connection to our old clockwise neighbor
				try {
					System.out.println(user.clockWisePort + " just left");
					user.cReceiverOK = false;
					user.clockwiseNeighbor.close();
					user.clockwiseOutput.close();
					user.clockwiseInput.close();
				} catch (IOException e) {
					//System.err.println("clockwise connection alreayd closed");
				}
				
			}
		}
	}
}
