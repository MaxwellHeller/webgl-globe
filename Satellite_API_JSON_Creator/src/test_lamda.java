import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import lambda_satellite_locator.SatElsetException;
import lambda_satellite_locator.Sgp4Data;
import lambda_satellite_locator.Sgp4Unit;

public class test_lamda {

		
		public static void main(String[] args) throws Exception {
		
		//Pulls a set of TLEs (Two Line Element) sets from Celestrack.com 
        // (NORAD maintained database) and creates a input stream to parse the info
        URL celestrack = null;
		try {
			celestrack = new URL("http://celestrak.com/NORAD/elements/stations.txt");
		} catch (MalformedURLException e6) {
			// TODO Auto-generated catch block
			e6.printStackTrace();
		}
        BufferedReader in = null;
        
        try {
			in = new BufferedReader( new InputStreamReader(celestrack.openStream()));
		} catch (IOException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
        
        //Initializes a Json Builder to format and assemble the json
        //coordinate file
        Map<String, Object> config = new HashMap<String, Object>();
        JsonBuilderFactory globe_json = Json.createBuilderFactory(config);
        
        JsonArrayBuilder globe = globe_json.createArrayBuilder();
        
       
        //Variables to store the calculated Latitude, Longitude, and Altitude 
        double lat = 0,lon = 0, alt = 0;
        
        //The delta time between calculated positions in minutes
    	double step = 0.1;
    	
    	Calendar calendar = Calendar.getInstance();
    	int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
    	int currentYear = calendar.get(Calendar.YEAR);
    	
    	int startYr = currentYear;
    	int stopYr = currentYear;
    	
    	double current_day_fraction = ((double)calendar.get(Calendar.HOUR_OF_DAY) + (((double)calendar.get(Calendar.MINUTE))/60)) / 24;               
    	double future_day_fraction = (((double)calendar.get(Calendar.HOUR_OF_DAY) + 0.5) + ((double)(calendar.get(Calendar.MINUTE))/60))/24;
    	
    	double current_time = dayOfYear + current_day_fraction;
    	double future_time = dayOfYear + future_day_fraction;
    	
    	double temp_time = current_time;

    	//Variables for calculating ECI position and then converting to Lat,Lon,Alt
    	double radiusearthkm = 6378.135;
    	double omega = 1.0 + 8640184.812866 / 3155760000.0;
    	double UT = 0;
    	double T = 0;
    	double gmst0 = 0;
    	double theta_GMST = 0;
    	double julian = 0;
    	double lat_temp;
    	double a = 6378.137;
    	double e = 0.081819190842622;
    	double c = 0;
    	double x = 0;
    	double y = 0;
    	double z = 0;
    	String inputLine = null;
    	
    	
        
        for(int i = 0; i < 3; i++){
        	
        	
        	inputLine = in.readLine();

        	int inputLength = 0;
        	while(inputLine.charAt(inputLength) != ' '){
        		inputLength++;
        	}
        	
        	inputLine = inputLine.substring(0, inputLength);
        	
        	System.out.println(inputLine);
        	
        	String card1= in.readLine();
        	String card2 = in.readLine();
        	
        	Sgp4Data data = null;
        	Sgp4Unit location = new Sgp4Unit();
        	
        	Vector results = location.runSgp4(card1, card2, startYr, current_time,
                    stopYr, future_time, step);
        	
        	JsonArrayBuilder coord = globe_json.createArrayBuilder();
        	
        	
        	for (int j = 0; j < results.size(); j++) {
                 data = (Sgp4Data) results.elementAt(j);
                 
                 temp_time = current_time + ((j*step)/60)/24;
                 
                 
                 julian = myJday(temp_time);
                 x = data.getX();
                 y = data.getY();
                 z = data.getZ();
                 
                 //Calculating the Julian Date to GMST_Theta
                 //From : "http://www.stltracker.com/resources/equations"
                 UT = (julian + 0.5)%1.0;
                 T = (julian - UT - 2451545.0) / 36525.0;
                 gmst0 = 24110.548412 + T * (8640184.812866 + T * (0.093104 - T * 6.2E-6));
                 theta_GMST = ((gmst0 + 86400.0 * omega * UT)% 86400.0) * ((2 * Math.PI) / 86400.0);
                 
                 //Calculating the longitude
                 lon = (Math.atan((y/x)) - theta_GMST) % (2*Math.PI);
                 
                 //Calculating the Latitude, and performing calculations to get a higher level of accuracy
                 lat = Math.atan( z / Math.sqrt((x* x) + (y * y)));
                 	//do {
                 	//		lat_temp = lat;
                	//	    c = a * e * e * Math.sin(lat_temp) / Math.sqrt( 1.0 - e * e * Math.sin(lat_temp) * Math.sin(lat_temp));
                	//	    lat = Math.atan(( z + c ) / Math.sqrt(x*x + y* y));
                	//	  } while (Math.abs(lat - lat_temp) < 1.0e-10 );
                 
                 if(Math.cos(lat) == 0){
                	 alt = (z / Math.sin(lat)) - (a*Math.sqrt(1-Math.pow(e, 2)));
                 }else{
                	 alt = (Math.sqrt((Math.pow(x,2) + Math.pow(y,2))) / Math.cos(lat)) - 
                			 (a / Math.sqrt(1- (Math.pow(e,2) * Math.pow(Math.sin(lat), 2))));
                 }
                 
                 
                 lat = (int)Math.toDegrees(lat);
                 lon = (int)Math.toDegrees(lon);
                 if(lon < -180){
                 		lon = lon+360;
                 	}
                 alt = (Math.abs(alt/80000)*((double)(5.0+j)/60));
                 alt = alt * 10000;
             	 alt = (int)alt;
             	 alt = alt / 10000.0;
                   System.out.print("Lat: " + lat);
                   System.out.print(" Lon: " + lon);
                   System.out.print(" Alt: " + alt + "\n");
                  
        		 coord.add(lon)
        			.add(lat)
        			.add(alt);
        		 

             }
        	
            	
        	//Adds the station name to an json array and appends the coordinate array
        	JsonArrayBuilder station = globe.add(globe_json.createArrayBuilder()
        			.add(inputLine).add(coord));
        	
        	
        }

        //Builds the Json Array
        JsonArray finished_json = globe.build();

        //Turns the Json array into a Inputstream/Buffered stream format
        String str = finished_json.toString();
        InputStream is = new ByteArrayInputStream(str.getBytes());
        BufferedInputStream json_out = new BufferedInputStream(is);
       
		
		String bucket = "webglobetest";
        
        try {
			in.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
 		//Creates Object metadata to tell S3 that our file is json formatted
        ObjectMetadata ob = new ObjectMetadata();
        ob.setContentDisposition("application/json");
        
        /*
        //Using the BufferedInputStream, uploads our Json file the
        //the root directory of the partner site hosted on Amazon S3
        final AmazonS3 s3 = new AmazonS3Client();
		try {
		    s3.putObject("webglobetest", "globe.json", json_out, ob);
		    
		} catch (AmazonServiceException e1) {
		    System.err.println(e1.getErrorMessage());
		    System.exit(1);
		}
		
		try {
			in.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/
    }
	
    //Calculates the Julian date given the current year and time instance
    //used for a single coordinate set
    static double myJday(double time){
		
		Calendar current_year = Calendar.getInstance();
		
		int yr = current_year.get(Calendar.YEAR);
		
		int year = 0;
		
		if (yr < 57)
		    year = yr + 2000;
		  else
		    year = yr + 1900;
		  double jd_year = 2415020.5 + (year-1900)*365 + Math.floor((year-1900-1)/4);
		  return jd_year + time - 1.0;
    }
	
	}
