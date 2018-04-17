package appserver.satellite;

import appserver.job.Job;
import appserver.comm.ConnectivityInfo;
import appserver.job.UnknownToolException;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.job.Tool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.PropertyHandler;

/**
 * Class [Satellite] Instances of this class represent computing nodes that execute jobs by
 * calling the callback method of tool a implementation, loading the tool's code dynamically over a network
 * or locally from the cache, if a tool got executed before.
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Satellite extends Thread {

    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null;
    private Hashtable toolsCache = null;

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) throws FileNotFoundException {

        // read this satellite's properties and populate satelliteInfo object,
        // which later on will be sent to the server
        File file = new File(satellitePropertiesFile);
        Scanner inputFile = new Scanner(file);

        while (inputFile.hasNext())
        {
            String line = inputFile.nextLine();
            if(!line.startsWith("#") && line.length() > 0)
            {
                String[] parts  = line.split("\t");
                switch (parts[0].toLowerCase())
                {
                    case "host":
                        
                        satelliteInfo.setName(parts[1]);
                        break;
                    case "port":
                        satelliteInfo.setPort(Integer.parseInt(parts[1]));
                        break;
                }
            }   
        }
        inputFile.close();
        
        // read properties of the application server and populate serverInfo object
        // other than satellites, the as doesn't have a human-readable name, so leave it out
        file = new File(serverPropertiesFile);
        inputFile = new Scanner(file);

        while (inputFile.hasNext())
        {
            String line = inputFile.nextLine();
            if(!line.startsWith("#"))
            {
                line = line.replace(" ", "");
                String[] parts  = line.split("=");
                switch (parts[0].toLowerCase())
                {
                    case "host":
                        serverInfo.setName(parts[1]);
                        break;
                    case "port":
                        serverInfo.setPort(Integer.parseInt(parts[1]));
                        break;
                }
            }   
        }
        inputFile.close();
        
        // read properties of the code server and create class loader
        classLoader = new HTTPClassLoader();
        file = new File(classLoaderPropertiesFile);
        inputFile = new Scanner(file);

        while (inputFile.hasNext())
        {
            String line = inputFile.nextLine();
            if(!line.startsWith("#"))
            {
                line = line.replace(" ", "");
                String[] parts  = line.split("=");
                switch (parts[0].toLowerCase())
                {
                    case "host":
                        classLoader.host = parts[1];
                        break;
                    case "port":
                        classLoader.port = Integer.parseInt(parts[1]);
                        break;
                    case "doc_root":
                        classLoader.classRootDir = parts[1];
                        break;
                }
            }   
        }
        inputFile.close();

        // create tools cache
        toolsCache = new Hashtable();
    }

    @Override
    public void run() {

        // register this satellite with the SatelliteManager on the server
        // ---------------------------------------------------------------
        // TODO
        
        // create server socket
        // ---------------------------------------------------------------
        ServerSocket incoming = null;
        try{
             incoming = new ServerSocket(this.satelliteInfo.getPort());
        }catch(IOException e){
            System.err.println("Could not set up server socket");
            return;
        }
        
        // start taking job requests in a server loop
        // ---------------------------------------------------------------
        try{
            Socket socket;
           while(true)
            {
                socket = incoming.accept();
                new Thread(new SatelliteThread(socket, this)).start();
            } 
        }catch(IOException e){
            
        }        
    }

    // inner helper class that is instanciated in above server loop and processes single job requests
    private class SatelliteThread extends Thread {

        Satellite satellite = null;
        Socket jobRequest = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        SatelliteThread(Socket jobRequest, Satellite satellite) {
            this.jobRequest = jobRequest;
            this.satellite = satellite;
        }

        @Override
        public void run() 
        {
            // setting up object streams
            // ...
            try {
                this.writeToNet = new ObjectOutputStream(this.jobRequest.getOutputStream());
                this.readFromNet = new ObjectInputStream(this.jobRequest.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            // reading message
            // ...
            try {
                this.message = (Message)this.readFromNet.readObject();
                switch (message.getType()) 
                {
                    case JOB_REQUEST:
                        // processing job request
                        // TODO
                        
                        // This is the job that is being requested
                        Job job = (Job) message.getContent();
                        
                        // Now based on the job requirements, we load the Tool
                        Tool jobTool = getToolObject(job.getToolName());
                        
                        // Now we run the operation on the parameters
                        Integer newNumber = (Integer) jobTool.go(job.getParameters());
                        
                        // Send back the result
                        this.writeToNet.writeObject(newNumber);

                        break;

                    default:
                        System.err.println("[SatelliteThread.run] Warning: Message type not implemented");
                }
            } catch (IOException ex) {
                Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnknownToolException ex) {
                Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Aux method to get a tool object, given the fully qualified class string
     * If the tool has been used before, it is returned immediately out of the cache,
     * otherwise it is loaded dynamically
     */
    public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        Tool tool;
        if(this.toolsCache.containsKey(toolClassString))
        {
            tool = (Tool) this.toolsCache.get(toolClassString);
        }
        else
        {
            // TODO Make sure the path to the tool class string 
            String pathToolClass = "../" + toolClassString; 
            Class operationClass = classLoader.loadClass(pathToolClass);
            tool = (Tool) operationClass.newInstance();
            this.toolsCache.put(toolClassString, tool);
        }
        return tool;
    }

    public static void main(String[] args) throws FileNotFoundException {
        // start the satellite
        Satellite satellite;
        satellite = new Satellite(args[0], args[1], args[2]);
        satellite.run();
    }
}
