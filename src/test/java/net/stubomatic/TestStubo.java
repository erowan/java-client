package net.stubomatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.stubomatic.Stubo;
import net.stubomatic.StuboException;
import net.stubomatic.StuboResponse;

import org.junit.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/*
 *  These integration tests require a stubo server, a test stub is added 
 *  to help drive the tests.
 */
	
public class TestStubo {
	
	String dc;
	String scenario = "testing";
	String sessionName = "testing_delme";
	Map<String, String> vars = new HashMap<String, String>();
	
	@Before
    public void setUp() {
		Testing testing = new Testing();
		String dc = testing.props.getProperty("stubo.url");
		this.dc = (dc != null) ? dc : new String("localhost:8001");
        Stubo stubo = new Stubo(this.dc);
		vars.put("scenario", this.scenario);
		vars.put("session", this.sessionName);
		vars.put("mode", "record");
		vars.put("force", "true");
		stubo.deleteStubs(vars);
		stubo.beginSession(vars);
		StubData stub = new StubData("hello", "goodbye");
		ResponseEntity<StuboResponse> responseEntity;
        try {
            responseEntity = stubo.putStub(vars, stub.toJSONString());
            StuboResponse response = responseEntity.getBody();
            assert(!response.isError());
            responseEntity = stubo.endSession(vars);
            response = responseEntity.getBody();
            assert(!response.isError());
        } catch (JsonProcessingException e) {
            fail("Unable to convert StubData to JSON, error: " + e.getMessage());
        }
		
    }
 
    @After
    public void tearDown() {
        Stubo stubo = new Stubo(this.dc);
        Map<String, String> vars = new HashMap<String, String>();
		vars.put("scenario", this.scenario);
		vars.put("session", this.sessionName);
		vars.put("force", "true");
		stubo.endSession(vars);
		stubo.deleteStubs(vars); 
		vars.clear();
    }
    
    private Stubo getStubo() {
    	return new Stubo(this.dc);
    }
    
    @Test
    public void ctor() {
    	Stubo stubo = new Stubo();
    	assertNotNull(stubo);
    }
    
    @Test
    public void ctorWithDC() {
    	Stubo stubo = new Stubo("www.stubo.net");
    	assertEquals(stubo.getDC(), "www.stubo.net");
    }
        
    @Test
    public void getStatus() {
    	Stubo stubo = getStubo();
    	// \"data\": {\"cache_server\": {\"status\": \"ok\", \"local\": true}, \"info\": {\"cluster\": \"rowan\"}, \"database_server\": {\"status\": \"ok\"}, \"sessions\": []}
    	ResponseEntity<StuboResponse> responseEntity = stubo.getStatus();
    	StuboResponse response = responseEntity.getBody(); 
    	assertFalse(response.isError());
    	Map<String, ?> payload = response.getData();
        assertEquals(payload.get("database_server"), new HashMap<String, String>() {{
    		put("status", "ok");
    	}});
        assertFalse(payload.containsKey("sessions"));       		
    }
    
    
    @Test
    public void getStatusUnknownScenario() {
    	Stubo stubo = getStubo();
    	ResponseEntity<StuboResponse> responseEntity = stubo.getStatus(
    			Collections.singletonMap("scenario", "bogus")); 
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	Map<String, ?> payload = response.getData();
    	ArrayList<String> sessions = (ArrayList<String>) payload.get("sessions");
        assertEquals(sessions.size(), 0); 	
    }
    
    @Test
    public void getStatusknownScenario() {
    	Stubo stubo = getStubo();
    	ResponseEntity<StuboResponse> responseEntity = stubo.getStatus(
    			Collections.singletonMap("scenario", this.scenario)); 
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	Map<String, ?> payload = response.getData();
    	ArrayList<String> sessions = (ArrayList<String>) payload.get("sessions");
        assertEquals(sessions.size(), 1); 	
    }
    
    @Test
    public void beginSessionRecord() {
    	Stubo stubo = getStubo();
    	stubo.deleteStubs(vars);
    	this.vars.put("mode", "record");
    	ResponseEntity<StuboResponse> responseEntity = stubo.beginSession(this.vars);
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	assertEquals((String) response.getData().get("status"), "record");
    }
    
    @Test
    public void beginSessionPlayback() {
    	Stubo stubo = getStubo();
    	this.vars.put("mode", "playback");
    	ResponseEntity<StuboResponse> responseEntity = stubo.beginSession(this.vars);
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	assertEquals((String) response.getData().get("status"), "playback");
    }
    
    @Test
    public void beginSessionRecordDupError() {
    	Stubo stubo = getStubo();
    	this.vars.put("mode", "record");
    	try {
    		stubo.beginSession(this.vars);
    		fail("Expected exception to throw StuboException!");
        } catch (StuboException e) {
    		assertTrue(((String) e.getJsonError().get("message")).contains(
    				"Duplicate scenario found"));
    		assertEquals(e.getJsonError().get("code"), 400);
        }
    }
    
    @Test
    public void endSession() {
    	Stubo stubo = getStubo();
    	ResponseEntity<StuboResponse> responseEntity = stubo.endSession(this.vars);
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	assertEquals((String) response.getData().get("message"), "Session ended");
    }
    
    @Test
    public void getResponse() {
    	Stubo stubo = getStubo();
    	this.vars.put("mode", "playback");
    	stubo.beginSession(this.vars);
    	ResponseEntity<String> responseEntity = stubo.getResponse(
    			this.vars, "hello");
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	assertEquals(responseEntity.getBody(), "goodbye");
    	assertEquals(responseEntity.getHeaders().getContentType().toString(),
    			"text/html;charset=UTF-8"); 	
    }
    
    @Test
    public void getResponseNotPlaybackError() {
    	Stubo stubo = getStubo();
    	try {
    	    stubo.getResponse(vars, "hello"); 	   
    		fail("Expected exception to throw StuboException!");
        } catch (StuboException e) {
    		assertTrue(((String) e.getJsonError().get("message")).contains(
    				"cache status != playback"));
    		assertEquals(e.getJsonError().get("code"), 500);
        }
    }
    
    @Test
    public void getResponseNotFound() {
    	Stubo stubo = getStubo();
    	this.vars.put("mode", "playback");
    	stubo.beginSession(this.vars);
    	try {
    		stubo.getResponse(this.vars,  "xxx");	                                            
    		fail("Expected exception to throw StuboException!");  
    	} catch (StuboException e) {
    		assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
    		assertTrue(((String) e.getJsonError().get("message")).contains(
    		"No matching response found"));
    		assertEquals(e.getJsonError().get("code"), 400);
    	} 		                                 
    }
    
    @Test public void putStub(){
        Stubo stubo = getStubo();
        stubo.deleteStubs(this.vars);
        this.vars.put("mode", "record");
        stubo.beginSession(this.vars);      
        try {
            StubData stub = new StubData();
            stub.set("hello", "goodbye");
            ResponseEntity<StuboResponse> responseEntity = stubo.putStub(
                    this.vars, stub.toJSONString()); 
            assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
            StuboResponse response = responseEntity.getBody();
            assertFalse(response.isError()); 
        } catch (JsonProcessingException e) {
            fail("Unable to map StuboData to JSON, error: " + e.getMessage());
        }           
        
    }
   
    @Test public void getUknownDelayPolicy(){
    	Stubo stubo = getStubo();
    	this.vars.put("name", "foo");
    	ResponseEntity<StuboResponse> responseEntity = stubo.getDelayPolicy(
    			this.vars);
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	assertTrue(response.getData().isEmpty());
    }

    @Test public void putAndDeleteDelayPolicy(){
    	Stubo stubo = getStubo();
    	this.vars.put("name", "slow");
    	this.vars.put("delay_type", "fixed");
    	this.vars.put("milliseconds", "2000");
    	ResponseEntity<StuboResponse> responseEntity = stubo.putDelayPolicy(
    			this.vars);
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	StuboResponse response = responseEntity.getBody();
    	assertFalse(response.isError());
    	assertFalse(response.getData().isEmpty());

    	responseEntity = stubo.deleteDelayPolicy(this.vars);
    	assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    	response = responseEntity.getBody();
    	assertFalse(response.isError());
    	String message = (String) response.getData().get("message");
    	assertTrue(message.contains("Deleted")); 
    }
    
    
    /*
     * def test_user_args(self):
        stubo = self._get_stubo()  
        stubo.delete_stubs(scenario=self.scenario)
        session_name = self.session_name+"_template"
        response = stubo.begin_session(scenario=self.scenario,
                                       session=session_name, mode='record')
        matcher = """
        <rollme>                        
           <OriginDateTime>{{roll_date("2014-09-10", as_date(rec_date), as_date(play_date))}}T00:00:00Z</OriginDateTime>
        </rollme>
        """
        response = """
        <response>
        <putstub_arg>{% raw putstub_arg %}</putstub_arg>
        <getresponse_arg>{% raw getresponse_arg %}</getresponse_arg>
        </response>
        """
        payload = dict(request=dict(method="POST",
                                    bodyPatterns=[dict(contains=[matcher])]),
                                    response=dict(status=200,
                                                  body=response))
        
        response = stubo.put_stub(session=session_name, json=payload,
                                  rec_date='2014-09-10',
                                  putstub_arg='x')
        assert response.status_code == 200, 'unable to create test fixture in stubo'  
        response = stubo.end_session(scenario=self.scenario,
                                     session=session_name)  
        
        stubo.begin_session(scenario=self.scenario, session=session_name, 
                            mode='playback')
        request = """
        <rollme><OriginDateTime>2014-09-12T00:00:00Z</OriginDateTime></rollme>
        """
        response = stubo.get_response(session=session_name,
                                      data=request,
                                      play_date='2014-09-12',
                                      getresponse_arg='y',
                                      tracking_level='full') 
        self.assertEqual("""<response>
        <putstub_arg>x</putstub_arg>
        <getresponse_arg>y</getresponse_arg>
        </response>""".strip(), response.content.strip())     
     */
    
    @Test
    public void userUrlArgs() {
        Stubo stubo = getStubo();
        stubo.deleteStubs(this.vars); 
        this.vars.put("mode", "record");
        stubo.beginSession(this.vars);      
        try {
            StubData stub = new StubData();
            String matcher = new String("<rollme><OriginDateTime>{{roll_date('2014-09-10', as_date(rec_date), as_date(play_date))}}T00:00:00Z</OriginDateTime></rollme>");
            String responseBody = new String("<response><putstub_arg>{% raw putstub_arg %}</putstub_arg><getresponse_arg>{% raw getresponse_arg %}</getresponse_arg></response>");
            stub.set(matcher, responseBody);
            
            HttpHeaders headers = new HttpHeaders();  
            headers.setContentType( MediaType.APPLICATION_JSON );
            HttpEntity request = new HttpEntity(stub.toJSONString(), headers);
            this.vars.put("rec_date", "2014-09-10");
            this.vars.put("putstub_arg", "x");
            ResponseEntity<StuboResponse> responseEntity = stubo.putStub(this.vars, request); 
            assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
            StuboResponse response = responseEntity.getBody();
            assertFalse(response.isError()); 
        } catch (JsonProcessingException e) {
            fail("Unable to map StuboData to JSON, error: " + e.getMessage());
        }    
        
        stubo.endSession(this.vars);     
        
        this.vars.put("mode", "playback");
        this.vars.put("getresponse_arg", "y");
        this.vars.put("play_date", "2014-09-12");
        stubo.beginSession(this.vars);
        ResponseEntity<String> responseEntity = stubo.getResponse(
                this.vars, new String("<rollme><OriginDateTime>2014-09-12T00:00:00Z</OriginDateTime></rollme>"));
        
        assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        assertEquals(responseEntity.getBody(),
                "<response><putstub_arg>x</putstub_arg><getresponse_arg>y</getresponse_arg></response>");
        assertEquals(responseEntity.getHeaders().getContentType().toString(),
                "text/html;charset=UTF-8");     
    }
     
   
}
