package lambda_satellite_locator;

import java.net.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.json.*;

import java.io.*;
import java.io.OutputStream;

public class URLReader {
    
	public static void main(String[] args) throws Exception {

        URL celestrak_station = new URL("http://celestrak.com/NORAD/elements/stations.txt");
        URL celestrak_iridium = new URL("http://celestrak.com/NORAD/elements/iridium.txt");
        URL celestrak_NOAA = new URL("http://celestrak.com/NORAD/elements/noaa.txt");
        URL celestrak_visual = new URL("http://celestrak.com/NORAD/elements/visual.txt");
        BufferedReader in = new BufferedReader( new InputStreamReader(celestrak_station.openStream()));
        
        Map<String, Object> config = new HashMap<String, Object>();
        JsonBuilderFactory globe_json = Json.createBuilderFactory(config);
        
        JsonArrayBuilder globe = globe_json.createArrayBuilder();
        
       
        
        double lat = 0,lon = 0, alt = 0;
        
        //String for the Station Name
        String sat_Name = null;
        
        //The delta time between calculated positions in minutes
    	double step = 0.1;
    	
    	Calendar calendar = Calendar.getInstance();
    	int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
    	int currentYear = calendar.get(Calendar.YEAR);
    	
    	int startYr = currentYear;
    	int stopYr = currentYear;
    	
    	//In hours
    	double period = 1.5;
    	
    	double current_day_fraction = ((double)calendar.get(Calendar.HOUR_OF_DAY) + (((double)calendar.get(Calendar.MINUTE))/60)) / 24;               
    	double future_day_fraction = (((double)calendar.get(Calendar.HOUR_OF_DAY) + period) + ((double)(calendar.get(Calendar.MINUTE))/60))/24;
    	
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
    	
    	
        
        for(int i = 0; i < 6; i++){
        	
        	//Decides which input stream to pull Sat TLE's from
        	if(i==3){
        		in = new BufferedReader( new InputStreamReader(celestrak_iridium.openStream()));
        		sat_Name = in.readLine();
        	}
        	else if(i==4){	
        		in = new BufferedReader( new InputStreamReader(celestrak_NOAA.openStream()));
        		sat_Name = in.readLine();
        		while(!sat_Name.equals("NOAA 19 [+]             ")){
        			sat_Name = in.readLine();
        		}
        	}else if(i == 5){
        		in = new BufferedReader( new InputStreamReader(celestrak_visual.openStream()));
        		sat_Name = in.readLine();
        		while(!sat_Name.equals("KORONAS-FOTON           ")){
        			sat_Name = in.readLine();
        		}
        	}else{
        		sat_Name = in.readLine();
        	}
        	
        	

        	int inputLength = 0;
        	while(!(sat_Name.charAt(inputLength) == ' ' && sat_Name.charAt(inputLength + 1) == ' ')){
        		inputLength++;
        	}
        	
        	sat_Name = sat_Name.substring(0, inputLength);
        	
        	System.out.println(sat_Name);
        	
        	String card1= in.readLine();
        	String card2 = in.readLine();
        	
        	Sgp4Data data = null;
        	Sgp4Unit location = new Sgp4Unit();
        	
        	Vector<Sgp4Data> results = location.runSgp4(card1, card2, startYr, current_time,
                    stopYr, future_time, step);
        	
        	JsonArrayBuilder coord = globe_json.createArrayBuilder();
        	
        	double[] pos = new double[3];
        	double julian_mod = 0;
        	
        	for (int j = 0; j < results.size(); j++) {
                 data = (Sgp4Data) results.elementAt(j);
                 
                 temp_time = current_time + ((j*step)/60)/24;
                 
                 
                 julian = myJday(temp_time);
                 
                 pos[0] = data.getX();
                 pos[1] = data.getY();
                 pos[2] = data.getZ();

                 julian_mod = julian - 2400000.5;
                 
                 pos = GeoFunctions.GeodeticLLA(pos, julian_mod);
                 
                 lat = (int)Math.toDegrees(pos[0]);
                 lon = (int)Math.toDegrees(pos[1]);
                 alt = pos[2];
                 alt = (Math.abs(alt/(100*alt)) *((double)(j)/(period*60)))*2;
                 alt = alt * 10000;
             	 alt = (int)alt;
             	 alt = alt / 10000.0;
                 
             	 //System.out.print("Y: " + pos[0]);
                 
                 
                 /*
                 //Calculating the Julian Date to GMST_Theta
                 //From : "http://www.stltracker.com/resources/equations"
                 UT = (julian + 0.5)%1.0;
                 T = (julian - UT - 2451545.0) / 36525.0;
                 gmst0 = 24110.548412 + T * (8640184.812866 + T * (0.093104 - T * 6.2E-6));
                 theta_GMST = ((gmst0 + 86400.0 * omega * UT)% 86400.0) * ((2 * Math.PI) / 86400.0);
                 
                 double d = (julian - 2400000.5)-51544.5;
                 
                 //Calculating the longitude
                 lon = (Math.atan2(y, x));
                 lon = lon -(280.4606 +360.9856473*d)*Math.PI/180.0;
                 */
                 
                 x = data.getX();
                 y = data.getY();
                 z = data.getZ();
                 
                 //Calculating the Latitude, and performing calculations to get a higher level of accuracy
                 lat = Math.atan( z / Math.sqrt((x* x) + (y * y)));
                 	//do {
                 	//		lat_temp = lat;
                	//	    c = a * e * e * Math.sin(lat_temp) / Math.sqrt( 1.0 - e * e * Math.sin(lat_temp) * Math.sin(lat_temp));
                	//	    lat = Math.atan(( z + c ) / Math.sqrt(x*x + y* y));
                
                 //	  } while (Math.abs(lat - lat_temp) < 1.0e-10 );
                 
                 /*
                 if(Math.cos(lat) == 0){
                	 alt = (z / Math.sin(lat)) - (a*Math.sqrt(1-Math.pow(e, 2)));
                 }else{
                	 alt = (Math.sqrt((Math.pow(x,2) + Math.pow(y,2))) / Math.cos(lat)) - 
                			 (a / Math.sqrt(1- (Math.pow(e,2) * Math.pow(Math.sin(lat), 2))));
                 }
                 
                 
                 lat = (int)Math.toDegrees(lat);
                 //lon = (int)Math.toDegrees(lon);
                // if(lon < -180){
                // 		lon = lon+360;
                /// 	}
                 alt = Math.abs(alt/(10*alt))*((double)(5.0+j)/60);
                 alt = alt * 10000;
             	 alt = (int)alt;
             	 alt = alt / 10000.0;
             	   System.out.print("X:  " + x);
                   System.out.print(" Y:   " + y);
                   System.out.print(" Z: " + z + "\n");
                   System.out.print("Lat: " + lat);
                   System.out.print(" Lon: " + lon);
                   System.out.print(" Alt: " + alt + "\n\n");
                   
                   */
                 lat = (int)Math.toDegrees(lat);
                 
                 System.out.print(" Lat: " + (int)lat);
                 System.out.print(" Lon: " + (int)lon);
                 System.out.print(" Alt: " + alt + "\n");
                  
        		 coord.add((int)lat)
        			.add((int)lon)
        			.add(alt);
        		 

             }
        	
        	
        	JsonArrayBuilder station = globe.add(globe_json.createArrayBuilder()
        			.add(sat_Name).add(coord));
        	
        	
        }

        
        JsonArray finished_json = globe.build();

        FileOutputStream json_out = new FileOutputStream("\\Users\\maxhh\\Desktop\\Capital One Summit\\globe.json");
        
        JsonWriter writer = Json.createWriter(json_out);
        		 writer.writeArray(finished_json);
        		 writer.close();
        		 json_out.close();
        
        in.close();
    }
	
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
