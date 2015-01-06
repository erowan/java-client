package net.stubomatic.examples;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import net.stubomatic.Session;
import net.stubomatic.SessionTemplate;
import net.stubomatic.StuboException;

/*
 *  Example that shows usage of a spring RestTemplate with Stubo to 
 *  record and playback 'recorded' stubs.
 */
public class Ex1 {
	
	protected static Logger logger = Logger.getLogger("net.stubo.examples.Ex1"); 
	private Session session;
	private final String URI = "http://httpbin.org/post";
	
	public Ex1(String dc) {
		super();
		this.session = new Session(dc, "jex1", "jex1_1");
	}
	
	public Ex1(String dc, Map<String, String> extras) {
		super();
		this.session = new Session(dc, "jex1", "jex1_1", extras);
	}
	
	public void run(RestTemplate restTemplate, Map<String, ?> urlVariables) {
		String query = this.session.getStubo().buildQuery(urlVariables);
		HttpEntity<?> requestEntity = new HttpEntity("hello world");
		ResponseEntity<String> responseEntity =  restTemplate.exchange(
		                                             URI+query,
		                                             HttpMethod.POST, 
		                                             requestEntity, 
		                                             String.class,
		                                             urlVariables); 
		String response = responseEntity.getBody();
		logger.info(response);	   	
	    
	}
	
	public void play(final Map<String, ?> urlVariables) {
		try {
			this.session.play(new SessionTemplate() {
			    public void execute(RestTemplate rt) {
			    	run(rt, urlVariables);
			    }
			});
		} catch (StuboException ex) {
			logger.error(ex.getMessage());	
		}	
	}
	
	public void record(final Map<String, ?> urlVariables) {
		try {
			this.session.record(new SessionTemplate() {
			    public void execute(RestTemplate rt) {
			    	run(rt, urlVariables);
			    }
			});
		} catch (StuboException ex) {
			logger.error(ex.getMessage());	
		}	
	}
	
	public void recordOrPlay(final Map<String, ?> urlVariables) {
		try {
			this.session.recordOrPlay(new SessionTemplate() {
			    public void execute(RestTemplate rt) {
			    	run(rt, urlVariables);
			    }
			});
		} catch (StuboException ex) {
			logger.error(ex.getMessage());	
		}	
	}

	public static void main( String[] args )
	{
		BasicConfigurator.configure();
		String dc = "localhost:8001";
		logger.info("Starting Ex1");
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("tracking_level", "full");
		Ex1 ex1 = new Ex1(dc, params);
		
		Map<String, String> urlVariables = new HashMap<String, String>();
		
		logger.info("straight http calls without stubo");
		ex1.run(new RestTemplate(), urlVariables);
		
		logger.info("stubo recording of http calls");
		urlVariables.put("recorded_at", "2014-11-10");
		ex1.record(urlVariables);
		
		logger.info("stubo playback of previously recorded http calls");
		urlVariables.put("played_at", "2014-11-11");
		ex1.play(urlVariables);
		
		logger.info("let stubo work out what you want to do");
		ex1.recordOrPlay(urlVariables);	
	}	
}
