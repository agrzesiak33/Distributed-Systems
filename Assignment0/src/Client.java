import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	public static void main(String args[])
	{
		Socket socket;
		DataOutputStream out = null;
		DataInputStream in = null;
		try {
			socket = new Socket("localhost", 5589);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
			BufferedReader buff = new BufferedReader( new InputStreamReader(in));
			
			Scanner scanner = new Scanner(System.in);
			String userInput;
			int inChar;
			do
			{
				userInput = scanner.nextLine();
				out.writeBytes(userInput);
				
				userInput = "";
				while((inChar = buff.read()) != -1) 
				{
					userInput = userInput + Character.toString((char)inChar);
				}
				System.out.println(userInput);
			}
			while(userInput != "quit");
			scanner.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
