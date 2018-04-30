package appserver.client;

import appserver.comm.Message;
import appserver.comm.MessageTypes;
import appserver.job.Job;
import utils.PropertyHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;

public class FibonacciClient implements MessageTypes {

   public String host;
   public int port;
   Properties properties;
   //constructor
   public FibonacciClient(String serverPropertiesFile) {
       try {
           properties = new PropertyHandler(serverPropertiesFile);
           host = properties.getProperty("HOST");
           System.out.println("[FibonacciClient.FibonacciClient] Host: " + host);
           port = Integer.parseInt(properties.getProperty("PORT"));
           System.out.println("[FibonacciClient.FibonacciClient] Port: " + port);
       } catch (Exception ex) {
           ex.printStackTrace();
       }
   }

   // run method
   public void run()
   {
       try {
           //conect to server
           Socket server = new Socket(host,port);
           
           // hard-coded string of class, aka tool name ... plus one argument
           String classString = "appserver.job.impl.Fibonacci";
           Random randomGenerator = new Random();
           Integer number = randomGenerator.nextInt(30); //random number from 0 to 29

           // create job and job request message
           Job job = new Job(classString, number);
           Message message = new Message(JOB_REQUEST, job);
           
           //sending job
           ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
           writeToNet.writeObject(message);
           
           //reading response from app server.
           ObjectInputStream readFromNet = new ObjectInputStream(server.getInputStream());
           Integer result = (Integer) readFromNet.readObject();
           System.out.println("RESULT: " + result);
           
       } catch (IOException e) {
           System.err.println("[Fibonacci.run] Error occurred. Invalid input / output");
           e.printStackTrace();
       } catch (ClassNotFoundException e) {
           System.err.println("[Fibonacci.run] Error occurred. Class not found");
           e.printStackTrace();
       }

   }

   public static void main(String [] args)
   {
       FibonacciClient newClient = null;
       if(args.length == 1) {
           newClient = new FibonacciClient(args[0]);
       } else {
           newClient = new FibonacciClient("../../config/Server.properties");
       }
       newClient.run();
   }
}
