package appserver.server;

import java.util.ArrayList;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class LoadManager {

    static ArrayList satellites = null;
    static int lastSatelliteIndex = -1;

    public LoadManager() {
        satellites = new ArrayList<String>();
    }

    public void satelliteAdded(String satelliteName) {
        // add satellite
    	this.satellites.add(satelliteName);
    }


    public String nextSatellite() throws Exception {
        
        //int numberSatellites;
        
        synchronized (satellites) {
            // implement policy that returns the satellite name according to a round robin methodology
        	if (this.satellites.size() <= 0) {
        		return "Error - No registered satellites";
        	}
        	String toReturn;
        	if (this.lastSatelliteIndex == -1) {
        		toReturn = this.satellites.get(0);
        	} else {
        		toReturn = this.satellites.get(this.lastSatelliteIndex);
        	}
        	this.lastSatelliteIndex = (this.lastSatelliteIndex + 1) % this.satellites.size();
        }

        return toReturn;// ... name of satellite who is supposed to take job
    }
}
