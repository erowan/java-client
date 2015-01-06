package net.stubomatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.stubomatic.Session;
import net.stubomatic.SessionTemplate;
import net.stubomatic.Stubo;
import net.stubomatic.StuboException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/*
 *  These integration tests require a stubo server.
 */
public class TestSession {
	
	String dc;
	String scenario = "testing";
	String sessionName = "testing_delme";
	Map<String, String> vars = new HashMap<String, String>();
	
	
	@Before
	public void setUp() {
		Testing testing = new Testing();
		String dc = testing.props.getProperty("stubo.url");
		this.dc = (dc != null) ? dc : new String("localhost:8001");
	}
	
    @After
    public void tearDown() {
    	Map<String, String> args = new HashMap<String, String>();
    	args.put("force", "true");
    	args.put("scenario", this.scenario);
    	args.put("session", this.sessionName);
    	Stubo stubo = new Stubo(this.dc);
        stubo.deleteStubs(args);
      
    }
    
    private Session getSession() {
    	return new Session(dc, scenario, sessionName);
    }
    
    private Session getSession(Map<String, String> args) {
    	return new Session(dc, scenario, sessionName, args);
    }
    
    @Test
    public void ctor() {
    	Session session = getSession();
    	assertNotNull(session);
    }
    
    @Test
    public void ctorWithArgs() {
    	Map<String, String> args = new HashMap<String, String>();
    	args.put("foo", "bar");
    	Session session = getSession(args);
    	assertEquals(session.getArg("foo"), "bar");
    }
    
    @Test
    public void record() {
    	final Session session = getSession();
    	session.record(new SessionTemplate() {
		    public void execute(RestTemplate rt) {
		    	Map<String, String> uriVariables = new HashMap<String, String>();
		    	HttpEntity<?> requestEntity = new HttpEntity("hello");
		    	ResponseEntity<String> responseEntity =  rt.exchange(
		    			"http://httpbin.org/post",
		    			HttpMethod.POST, requestEntity, String.class, 
		    			uriVariables);    
		    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
		    	assertEquals(session.getCalls().size(), 1);
		    }
		});
    	/* TODO: call stubo/api/stublist and check that */
    }
    
    @Test
    public void playAndScenarioNotFound() {
    	Session session = getSession();
    	try {
    		session.play(new SessionTemplate() {
    			public void execute(RestTemplate rt) {
    				Map<String, String> uriVariables = new HashMap<String, String>();
    				HttpEntity<?> requestEntity = new HttpEntity("hello");
    				ResponseEntity<String> responseEntity =  rt.exchange(
    						"http://httpbin.org/post",
    						HttpMethod.POST, requestEntity, String.class, 
    						uriVariables);    
    			}
    		});
    	} catch (StuboException ex) {
    		assertTrue(ex.getMessage().contains("Scenario not found"));
    	}
    }
    
    @Test
    public void recordAndPlay() {
    	Session session = getSession();
    	
    	session.record(new SessionTemplate() {
		    public void execute(RestTemplate rt) {
		    	Map<String, String> uriVariables = new HashMap<String, String>();
		    	HttpEntity<?> requestEntity = new HttpEntity("hello");
		    	ResponseEntity<String> responseEntity =  rt.exchange(
		    			"http://httpbin.org/post",
		    			HttpMethod.POST, requestEntity, String.class, 
		    			uriVariables);    
		    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
		    }
		});
    	
    	session.play(new SessionTemplate() {
		    public void execute(RestTemplate rt) {
		    	Map<String, String> uriVariables = new HashMap<String, String>();
		    	HttpEntity<?> requestEntity = new HttpEntity("hello");
		    	ResponseEntity<String> responseEntity =  rt.exchange(
		    			"http://httpbin.org/post",
		    			HttpMethod.POST, requestEntity, String.class, 
		    			uriVariables);    
		    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
		    }
		});  	
    }
    
    @Test
    public void recordOrPlay() {
    	final Session session = getSession();
    	
    	session.recordOrPlay(new SessionTemplate() {
		    public void execute(RestTemplate rt) {
		    	Map<String, String> uriVariables = new HashMap<String, String>();
		    	HttpEntity<?> requestEntity = new HttpEntity("hello");
		    	ResponseEntity<String> responseEntity =  rt.exchange(
		    			"http://httpbin.org/post",
		    			HttpMethod.POST, requestEntity, String.class, 
		    			uriVariables);    
		    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
		    	assertEquals(session.getCalls().size(), 1);
		    }
		});
    	/* TODO: call stubo/api/stublist to check that */
    } 
   
}