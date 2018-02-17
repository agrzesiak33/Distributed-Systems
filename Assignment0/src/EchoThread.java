import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class EchoThread implements Runnable
{
	protected Socket clientSocket;
	
	public EchoThread(Socket socket)
	{
		this.clientSocket = socket;
	}
	
	@Override
	public void run() {		
		DataOutputStream toClient = null;
		DataInputStream fromClient = null;
		
		try
		{
			// open data streams for input and output
			fromClient = new DataInputStream(this.clientSocket.getInputStream());
			toClient = new DataOutputStream(this.clientSocket.getOutputStream());
			
			//	Make sure only letters and spaces are send back.
			int quit = 0;
			byte inByte;
			char charFromClient;
			char lowerChar;
			//String temp = fromClient.readUTF();
			
			//	Once 4 is hit, all the letters of quit have been found in a row.
			while(quit < 4)
			{
				//	Listen to the client for letters.  Blocks until then.
				inByte = fromClient.readByte();
				charFromClient = (char)inByte;
				
				//	Only want to proceed with characters that are letters.
				if((charFromClient >= 'A' && charFromClient <= 'Z') || (charFromClient >= 'a' && charFromClient <= 'z'))
				{
					//	First we make sure the letter isn't in the sequence to shut down.
					lowerChar = Character.toLowerCase(charFromClient);
					switch (quit){
						case 0:
							quit = (lowerChar == 'q')? 1:0;
							break;
						case 1:
							quit = (lowerChar == 'u')? 2:0;
							break;
						case 2:
							quit = (lowerChar == 'i')? 3:0;
							break;
						case 3:
							quit = (lowerChar == 't')? 4:0;
							break;
					}
					
					toClient.writeByte(inByte);
					System.out.print((char)inByte);
				}
			}		
		}
		catch (Exception e) {
			
		}
		try {
			fromClient.close();
			toClient.close();
			///this.clientSocket.close();
		} catch (Exception e) {}
		
	}

}
