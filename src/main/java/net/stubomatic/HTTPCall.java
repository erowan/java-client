package net.stubomatic;

import java.util.HashMap;
import java.util.Map;

/*
 *  Represents an HTTP request/response used for recording interactions 
 *  with an HTTP server       
 */
public class HTTPCall {
    
    final public String   host;   
    final public String   requestMethod;
    final public String   requestUrl;
	final public String   requestBody;
    final public Map<String, ?> requestHeaders;
    
    public int      responseStatus;
    public String   responseReason;
    public String   responseBody;
    public Map<String, ?> responseHeaders;
    
    public HTTPCall(String host, String requestMethod, String requestUrl,
			String requestBody, Map<String, ?> requestHeaders) {
		super();
		this.host = host;
		this.requestMethod = requestMethod;
		this.requestUrl = requestUrl;
		this.requestBody = requestBody;
		this.requestHeaders = requestHeaders;
		Map<String, String> responseHeaders = new HashMap<String, String>();
		this.setResponse(200, "", "", responseHeaders);
	}
    
    public void setResponse(int responseStatus, String responseReason, 
    		String responseBody, Map<String, ?> responseHeaders){
    	this.responseStatus = responseStatus;
		this.responseReason = responseReason;
		this.responseBody = responseBody;
		this.responseHeaders = responseHeaders;
    }	
    
			
    
    
}
