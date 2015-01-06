package net.stubomatic.examples;

import java.util.HashMap;
import java.util.Map;

import net.stubomatic.Stubo;
import net.stubomatic.StuboException;

import org.apache.log4j.BasicConfigurator;
import org.springframework.http.ResponseEntity;

/*
 *  Playback example using the Stubo API
 */
public class APIPlaybackExample 
{
	public static void main( String[] args )
	{
		BasicConfigurator.configure();
		try {
			Stubo stubo = new Stubo();
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("scenario", "jexample");
			vars.put("session", "jexample_1");
			vars.put("mode", "playback");
			vars.put("force", "true");
			vars.put("tracking_level", "full");
			stubo.endSession(vars);
			stubo.beginSession(vars);	
			ResponseEntity<String> responseEntity = stubo.getResponse(vars, "hello");
			System.out.println(responseEntity.getStatusCode());
			String response = responseEntity.getBody();
	    	System.out.println(response);		
			stubo.endSession(vars);				
		}	
		catch (StuboException ex) {
			System.out.println(ex.getMessage());	
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());	
		}
	}
} 