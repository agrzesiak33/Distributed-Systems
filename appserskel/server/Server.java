package appserver.server;

import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.comm.ConnectivityInfo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

import appserver.satellite.Satellite;
import utils.PropertyHandler;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Server {

    // Singleton objects - there is only one of them. For simplicity, this is not enforced though ...
    static SatelliteManager satelliteManager = null;
    static LoadManager loadManager = null;
    static ServerSocket serverSocket = null;

    public Server(String serverPropertiesFile) {

        // create satellite manager and load manager
        // ...
        satelliteManager = new SatelliteManager();
        loadManager = new LoadManager();
        
        // read server properties and create server socket
        // ...
        String directory = System.getProperty("user.dir");
        File propertiesFile = new File(directory + "\\config\\"+ serverPropertiesFile);
        String host ="";
        int port = 0;
        String doc_root = "";

        try {
            Scanner sc = new Scanner(propertiesFile);
            while (sc.hasNext())
            {
                String[] temp = sc.nextLine().split("\t");
                if (temp[0].equalsIgnoreCase("HOST"))
                {
                    host = temp[1];
                }
                else if (temp[0].equalsIgnoreCase("PORT"))
                {
                    port = Integer.parseInt(temp[1]);
                }
                else if(temp[0].equalsIgnoreCase("DOC_ROOT"))
                {
                    doc_root = temp[1];
                }
            }
            serverSocket = new ServerSocket(port); //server socket created.

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
    // serve clients in server loop ...
    // when a request comes in, a ServerThread object is spawned
    // ...
    }

    // objects of this helper class communicate with satellites or clients
    private class ServerThread extends Thread {

        Socket client = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        private ServerThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            // set up object streams and read message
            // ...
            try {
                writeToNet = new ObjectOutputStream(this.client.getOutputStream());
                readFromNet = new ObjectInputStream(this.client.getInputStream());
                //read message
                this.message = (Message) readFromNet.readObject();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


            // process message
            switch (message.getType()) {
                case REGISTER_SATELLITE:
                    // read satellite info
                    // ...
                    
                    //Satellite tempSatellite = (Satellite) message.getContent();
                    
                    // register satellite
                    
                    synchronized (Server.satelliteManager) {
                        // ...
                    }

                    // add satellite to loadManager
                    synchronized (Server.loadManager) {
                        // ...
                    }

                    break;

                case JOB_REQUEST:
                    System.err.println("\n[ServerThread.run] Received job request");

                    String satelliteName = null;
                    synchronized (Server.loadManager) {
                        // get next satellite from load manager
                        // ...
                        
                        // get connectivity info for next satellite from satellite manager
                        // ...
                    }

                    Socket satellite = null;
                    // connect to satellite
                    // ...

                    // open object streams,
                    // forward message (as is) to satellite,
                    // receive result from satellite and
                    // write result back to client
                    // ...

                    break;

                default:
                    System.err.println("[ServerThread.run] Warning: Message type not implemented");
            }
        }
    }

    // main()
    public static void main(String[] args) {
        // start the application server
        Server server = null;
        if(args.length == 1) {
            server = new Server(args[0]);
        } else {
            server = new Server("../../config/Server.properties");
        }
        server.run();
    }
}
