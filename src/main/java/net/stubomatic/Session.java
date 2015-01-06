package net.stubomatic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 *  The Session class executes a user provided {@link net.stubo.SessionTemplate}
 *  to play or record HTTP interactions. Session management is taken care of  
 *  by wrapping the SessionTemplate#execute within a start/stop session block.     
 */
public class Session {

	protected static Logger logger = Logger.getLogger("net.stubomatic.Session"); 
	private Stubo stubo;
	private final Map<String, String> args = new HashMap<String, String>();
	private boolean deleteStubs = true;
	private boolean startedOk = false;
	private RestTemplate stuboAdapter;
	private List<HTTPCall> calls = new  ArrayList<HTTPCall>();
	private HTTPCall currentCall = null;
	private String userExit = null;

	public Session(String scenario, String sessionName){
	    this(Protocol.HTTP, "localhost:8001", scenario, sessionName);               
	}
	
	public Session(String dc, String scenario, String sessionName){
        this(Protocol.HTTP, dc, scenario, sessionName, 
             new HashMap<String, String>());
    }
	 
    public Session(Protocol protocol, String dc, String scenario, 
                   String sessionName){
       this(protocol, dc, scenario, sessionName, new HashMap<String, String>());
	}
    
    public Session(String dc, String scenario, String sessionName, 
                   Map<String, String> args){
        this(Protocol.HTTP, dc, scenario, sessionName, args);        
    }
	
	public Session(Protocol protocol, String dc, String scenario, 
	               String sessionName, 
			       Map<String, String> args){
	    this.stubo = new Stubo(protocol, dc);
	    this.setArgs(scenario, sessionName, args);
	}
	
	public Session(Protocol protocol, String dc,
	               UsernamePasswordCredentials credentials, 
	               String scenario, 
                   String sessionName, 
                   Map<String, String> args){
	    this.stubo = new Stubo(protocol, dc, credentials);
        this.setArgs(scenario, sessionName, args);
    }	
	
	private void setArgs(String scenario, String sessionName,
	                     Map<String, String> args){
        this.args.put("scenario", scenario);
        this.args.put("session", sessionName);
        this.args.put("force", "false");
        this.args.put("mode", null);
        if (args.containsKey("deleteStubs")) {
            this.deleteStubs = Boolean.parseBoolean(args.remove("deleteStubs")); 
        }
        // overrides & any extra stubo api args e.g. tracking_level=full
        this.args.putAll(args);
        this.stuboAdapter = new RestTemplate(new StuboRequestFactory(this));
	}
	
	public String getArg(String key) {
        return this.args.get(key);
	}
	
	public void setArg(String key, String value) {
        this.args.put(key, value);
	}
	
	public String getDC() {
        return this.stubo.getDC();
	}
	
	public Stubo getStubo() {
	    return this.stubo;
	}
	
	public String getMode(){
        return this.getArg("mode");
	}
	
	public void setMode(String mode){
        this.setArg("mode", mode);
	}
	
	public boolean isSSL() {
        return this.stubo.getProtocol().isSSL();
	}
	
	public String getSessionName() {
        return this.getArg("session_name");
	}
	
	public String getScenario() {
        return this.getArg("scenario");
	}	
	
	public ResponseEntity<StuboResponse> start(){
        assert(this.getMode() != null);   
        if (this.getMode().equals("record") && this.deleteStubs) {
             this.stubo.deleteStubs(this.args);
        }     
        ResponseEntity<StuboResponse> response = this.stubo.beginSession(
        		                                     this.args);
        this.startedOk = true; 
        return response;
	}  
	
	@SuppressWarnings("rawtypes")
    public ResponseEntity<StuboResponse> stop()  {
        Assert.notNull(this.getMode(), "'mode' must not be null");
        ResponseEntity<StuboResponse> response = null;
        if (this.startedOk) {
        	if (this.getMode().equals("record")) {
                for (HTTPCall httpCall : this.calls) {
                    StubData stub = new StubData();
                    stub.set(httpCall.requestBody, httpCall.responseBody,
                             httpCall.requestMethod, httpCall.responseStatus);
                    String userExitName = this.getArg("user_exit");
                    if (userExitName != null) {
                        stub.setModule(new  StubData.UserExit(userExitName));
                    }
                    
                    HttpHeaders headers = new HttpHeaders();  
                    headers.setContentType( MediaType.APPLICATION_JSON );
                    // pass request headers through
                    for (Map.Entry<String, ?> entry : httpCall.requestHeaders.entrySet()) {
                        headers.add(entry.getKey(), (String) entry.getValue());
                    }
                    
                    ObjectMapper mapper = new ObjectMapper(); 
                    HttpEntity request;
                    String query = (String) httpCall.requestHeaders.get("Stubo-Request-Query");
                    Map<String, String> queryArgs = new HashMap<String, String>();
                    if (query != null) {
                        List<NameValuePair> params = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
                        for (NameValuePair param : params) {
                            queryArgs.put(param.getName(), param.getValue());
                        }
                    }    
                    queryArgs.putAll(this.args);
                    try {
                        request = new HttpEntity(mapper.writeValueAsString(stub), headers);
                        this.stubo.putStub(queryArgs, request); 
                    } catch (JsonProcessingException e) {
                        logger.error("Unable to map StuboData to JSON, error: " + e.getMessage());
                        return response;
                    }         	
                } 
        	}
        	response = this.stubo.endSession(this.args);
        }    
        return response;
	}   	
	 
	public ResponseEntity<StuboResponse> record(SessionTemplate sessionTemplate)
	{
        this.setMode("record");	
        return this.run(sessionTemplate);
	}
	
	public ResponseEntity<StuboResponse> play(SessionTemplate sessionTemplate) {
        this.setMode("playback");	
        return this.run(sessionTemplate);
	}
	
	public ResponseEntity<StuboResponse> recordOrPlay(
	       SessionTemplate sessionTemplate){
        return this.run(sessionTemplate);
	}
	
	public ResponseEntity<StuboResponse> recordOrPlay(
			SessionTemplate sessionTemplate, String mode)  {
        this.setMode(mode);
        return this.run(sessionTemplate);
	}
	
	private ResponseEntity<StuboResponse> run(SessionTemplate sessionTemplate) {
        ResponseEntity<StuboResponse> response = null;
        try {
            if (this.getMode() == null){
                this.setMode(this.discoverMode());  
            }   
            this.start();
            sessionTemplate.execute(this.getRestTemplate());
        }    
        finally {
            response = this.stop();
        } 
        return response;
    } 
	
	private String getSessionMode() {
        String status = "notfound";
        ResponseEntity<StuboResponse> responseEntity = this.stubo.getStatus(this.args);
        Map<String, ?> payload = responseEntity.getBody().getData();
        HashMap session = (HashMap) payload.get("session");
        if (session.containsKey("status")) {
            status = (String) session.get("status");
        }
        return status;
    }
	
	private String discoverMode(){
        String sessionMode = this.getSessionMode();
        if (sessionMode.equals("notfound")) {
            return "record";
        } else if (sessionMode.equals("dormant")) {
            return "playback";
        } else {
            throw new StuboException(HttpStatus.BAD_REQUEST, "session " + 
                this.getSessionName() + " in " + sessionMode + 
                " should be 'dormant'.");
        }
    }
	
	public RestTemplate getRestTemplate() {
        return this.stuboAdapter;
    }

	public void recordRequest(HttpUriRequest httpRequest, String payload,
	        Map<String, ?> headers){
        String host = (String) headers.get("Stubo-Request-Host");
        HTTPCall newCall = new HTTPCall(host, httpRequest.getMethod(),
                httpRequest.getURI().toString(), payload, headers);		
        this.currentCall = newCall;
    }        
	
	private String readBody(InputStream bytes, Charset charset) throws IOException {
	    return StreamUtils.copyToString(bytes, charset);
    }
	
	public void recordResponse(ClientHttpResponse response){
        HTTPCall newCall = this.currentCall;
        if (newCall == null) {
            throw new StuboException(HttpStatus.EXPECTATION_FAILED,
                "Called record response when no request was made.");
        }
        HttpHeaders headers = response.getHeaders();
        Charset charset = Stubo.getCharset(response);
        if (charset == null)
            charset = StuboHttpRequest.DEFAULT_CHARSET ;
        String payload = "";
        int statusCode = 200;
        String statusText = "";
      
        try {
            payload = this.readBody(response.getBody(), charset);
            statusCode = response.getStatusCode().value();
            statusText = response.getStatusText();
        } catch (IOException e) {
            logger.error("Unable to read response with charset:  " + 
                charset.toString() + ". Error: " + e.getMessage());
        }  
        HashMap<String, String> strHeaders = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                strHeaders.put(headerName, headerValue);
            }
        }
        newCall.setResponse(statusCode, statusText,  payload, strHeaders);
        this.calls.add(newCall);
        this.currentCall = null;				
    }
	
	public List<HTTPCall> getCalls() {
        return this.calls;
    }
}




