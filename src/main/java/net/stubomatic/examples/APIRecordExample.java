package net.stubomatic.examples;

import java.util.HashMap;
import java.util.Map;

import net.stubomatic.StubData;
import net.stubomatic.Stubo;
import net.stubomatic.StuboException;
import net.stubomatic.StuboResponse;

import org.apache.log4j.BasicConfigurator;
import org.springframework.http.ResponseEntity;

/*
 * Record example using the Stubo API
 */
public class APIRecordExample 
{
	public static void main( String[] args )
	{
		BasicConfigurator.configure();
		try {
			Stubo stubo = new Stubo();
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("scenario", "jexample");
			vars.put("session", "jexample_1");
			vars.put("mode", "record");
			vars.put("force", "true");
			vars.put("tracking_level", "full");
			stubo.deleteStubs(vars);
			stubo.beginSession(vars);
			StubData stub = new StubData("hello", "goodbye");
			stubo.putStub(vars, stub.toJSONString());
			stubo.endSession(vars);	
			ResponseEntity<StuboResponse> responseEntity = stubo.getStatus(vars);
			StuboResponse response = responseEntity.getBody();
			assert(!response.isError());
	    	Map<String, ?> payload = response.getData();
	    	System.out.println(payload.toString());		
		} catch (StuboException ex) {
		    System.out.println(ex.getMessage());	
		} catch (Exception ex) {
			System.out.println(ex.getMessage());	
		}
	}
} 