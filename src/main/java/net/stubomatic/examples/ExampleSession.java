package net.stubomatic.examples;

import net.stubomatic.Session;
import net.stubomatic.SessionTemplate;
import net.stubomatic.StuboException;

import org.apache.log4j.BasicConfigurator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/*
 *  Playback or record example using Stubo Session.
 *  On the first run it should 'record' to stubo.
 *  Each subsequent run should 'playback' from stubo.
 */
public class ExampleSession {

	public static void main(String[] args) {
		BasicConfigurator.configure();
		Session session = new Session("localhost:8001", "jexample_session", 
				                      "jexample_session_1");			                                            
		try {
			session.recordOrPlay(new SessionTemplate() {
			    public void execute(RestTemplate rt) {
			    	HttpEntity<?> requestEntity = new HttpEntity("hello");
			    	ResponseEntity<String> responseEntity =  rt.exchange(
			    			"http://httpbin.org/post",
			    			HttpMethod.POST, requestEntity, String.class);
			    	String response = responseEntity.getBody();
			    	System.out.println(response);
			    }
			});
		} catch (StuboException ex) {
		    System.out.println(ex.getMessage());	
		} catch (Exception ex) {
			System.out.println(ex.getMessage());	
		}
	}
}
