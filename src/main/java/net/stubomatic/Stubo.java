package net.stubomatic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.Scanner;

import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 *  Stubo API
 */
public class Stubo 
{
	protected static Logger logger = Logger.getLogger("net.stubomatic.Stubo"); 
	// use apache http client
	private final RestTemplate restTemplate;
	private final String dc;
	private final Protocol protocol;

    private final String api = "stubo/api";
	private HttpHeaders authHeaders = null;
    
    public Stubo() {
	    this(Protocol.HTTP, "localhost:8001");
	}
    
    public Stubo(String dc) {
        this(Protocol.HTTP, dc);
    }

	public Stubo(Protocol protocol, String dc) {
	    this(protocol, dc, configureRestTemplate());
	}

    public Stubo(Protocol protocol, String dc, RestTemplate restTemplate) {
	    this.protocol = protocol;
	    this.dc = dc;
	    this.restTemplate = restTemplate;
	}

	public Stubo(Protocol protocol, String dc,  UsernamePasswordCredentials credentials) {
	    this(protocol, dc, configureRestTemplate());
	    this.authHeaders = createAuthHeaders(credentials);      
	}

	private static RestTemplate configureRestTemplate() {
	    RestTemplate template = new RestTemplate(
	             new HttpComponentsClientHttpRequestFactory());
	    template.setErrorHandler(new Stubo.ErrorHandler());
	    return template;
	}
    
    private static HttpHeaders createAuthHeaders(final UsernamePasswordCredentials credentials){
        HttpHeaders hdrs = new HttpHeaders();
        String auth = credentials.getUserName() + ":" + credentials.getPassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        hdrs.add("Authorization", authHeader);
        return hdrs;
    }
    
    public HttpHeaders getAuthHeaders(){
        return authHeaders;
    }
    
    private HttpEntity<?> setAuth(HttpEntity<?> requestEntity) {
        if (this.authHeaders != null) {
            if (requestEntity != null) {
                HttpHeaders allHdrs = new HttpHeaders();
                for (Entry<String, List<String>> entry : requestEntity.getHeaders().entrySet()) {
                    allHdrs.put(entry.getKey(), entry.getValue());
                }
                for (Entry<String, List<String>> entry : this.authHeaders.entrySet()) {
                    allHdrs.put(entry.getKey(), entry.getValue());
                }
                requestEntity = new HttpEntity(requestEntity.getBody(), allHdrs);
            } else {
                requestEntity = new HttpEntity(this.authHeaders);
            }
        }  
        return requestEntity;
    }
    
    public Protocol getProtocol() {
        return protocol;
    }
    
    public String getDC(){
        return this.dc;
    }
    
    public RestTemplate getRestTemplate() {
        return this.restTemplate;
    }
    
    public ResponseEntity<StuboResponse> getStatus() {
        Map<String, String> empty = new HashMap<String, String>();
        return this.getStatus(empty);
    }
    
    public ResponseEntity<StuboResponse> getStatus(Map<String, ?> uriVariables){
        return this.execute("get/status", HttpMethod.GET, null, uriVariables);
    }
    
    public ResponseEntity<StuboResponse> deleteStubs(Map<String, ?> uriVariables) {
        return this.execute("delete/stubs", HttpMethod.POST, null, uriVariables); 
    }
      
    public ResponseEntity<StuboResponse> beginSession(Map<String, ?> uriVariables) {
        return this.execute("begin/session", HttpMethod.POST, null, uriVariables);      		            
    }
    
    public ResponseEntity<StuboResponse> endSession(Map<String, ?> uriVariables) {
        return this.execute("end/session", HttpMethod.POST, null, uriVariables);      		            
    }
        
    public ResponseEntity<StuboResponse> putStub(Map<String, ?> uriVariables, 
                                                 String payload) {
        HttpHeaders headers = new HttpHeaders();  
        headers.setContentType( MediaType.APPLICATION_JSON );
        HttpEntity<?> requestEntity = new HttpEntity(payload, headers);
        return this.putStub(uriVariables, requestEntity);
    }  
    
    public ResponseEntity<StuboResponse> putStub(Map<String, ?> uriVariables, 
                                                 HttpEntity<?> requestEntity) {
        return this.execute("put/stub", HttpMethod.POST, requestEntity, 
                            uriVariables);
    }
    
    public ResponseEntity<StuboResponse> getDelayPolicy(
            Map<String, ?> uriVariables) {
        return this.execute("get/delay_policy", HttpMethod.GET, null, 
                            uriVariables); 
    }
    
    public ResponseEntity<StuboResponse> putDelayPolicy(
            Map<String, ?> uriVariables) {
        return this.execute("put/delay_policy", HttpMethod.POST, null, 
                            uriVariables); 
    }
    
    public ResponseEntity<StuboResponse> deleteDelayPolicy(
            Map<String, ?> uriVariables) {
        return this.execute("delete/delay_policy", HttpMethod.POST, null, 
                            uriVariables); 
    }
    
    public ResponseEntity<String> getResponse(
            Map<String, ?> uriVariables,  String payload) {	                                        
        HttpEntity<?> requestEntity = new HttpEntity(payload);
        return this.getResponse(uriVariables, requestEntity);
    }  
    
    public ResponseEntity<String> getResponse(
    		Map<String, ?> uriVariables, HttpEntity<?> requestEntity) {
        return this.execute("get/response", HttpMethod.POST, 
        		requestEntity, uriVariables, String.class);    		          
    }
    
    <T> ResponseEntity<T> getResponse(Map<String, ?> uriVariables, 
            String payload, Class<T> responseType) {                                       
        HttpEntity<?> requestEntity = new HttpEntity(payload);
        return this.getResponse(uriVariables, requestEntity, responseType);		          
    }
    
    <T> ResponseEntity<T> getResponse(Map<String, ?> uriVariables, 
    		HttpEntity<?> requestEntity, Class<T> responseType) {	
        return this.execute("get/response", HttpMethod.POST, 
            requestEntity, uriVariables, responseType);    		          
    }
    
    public ResponseEntity<StuboResponse> executeMethod(String method, 
            Map<String, ?> uriVariables){
    	return this.executeMethod(method, uriVariables, null);	
    }
    
    public ResponseEntity<StuboResponse> executeMethod(String method, 
        Map<String, ?> uriVariables, String payload){
        HttpEntity<?> requestEntity = new HttpEntity(payload);
        return this.execute(method, HttpMethod.POST, requestEntity, 
                            uriVariables);    	
    }
    		                                          
    private ResponseEntity<StuboResponse> execute(String method, 
            HttpMethod httpMethod, HttpEntity<?> requestEntity, 
            Map<String, ?> uriVariables) {
        String query = buildQuery(uriVariables);
        ResponseEntity<StuboResponse> result =  this.restTemplate.exchange(
            this.getUrl(method) + query, 
            httpMethod, 
            setAuth(requestEntity), 
            StuboResponse.class, 
            uriVariables);       
        logger.debug(result);
        return result;
    }    
    
    private <T> ResponseEntity<T> execute(String method, 
            HttpMethod httpMethod, HttpEntity<?> requestEntity, 
            Map<String, ?> uriVariables, Class<T> responseType) {
        String query = buildQuery(uriVariables);
        ResponseEntity<T> result =  this.restTemplate.exchange(
            this.getUrl(method) + query,
            httpMethod, 
            setAuth(requestEntity), 
            responseType, 
            uriVariables);       
        logger.debug(result);
        return result;
    }  
    
    public String buildQuery(Map<String, ?> uriVariables){
        String query = new String();
        for (String key : uriVariables.keySet()) {
            if (query.length() == 0){
                query += "?" + key + "={" + key + "}";
            } else {
                query += "&" + key + "={" + key + "}";
            }    
        }
        return query;
    }
    
    private String getUrl(String method){
        return this.protocol + "://" + this.dc + "/" + this.api + "/" + method;
    }
    
    static public Charset getCharset(ClientHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        MediaType contentType = headers.getContentType();
        return contentType != null ? contentType.getCharSet() : null;
    }
    
    static class ErrorHandler extends DefaultResponseErrorHandler {
     
    	public void handleError(ClientHttpResponse response) 
    	  throws StuboException,  IOException, RestClientException {
            HttpStatus statusCode = getHttpStatusCode(response);
            HttpHeaders headers = response.getHeaders();
            MediaType mediaType = headers.getContentType();
            if (headers.getFirst("X-Stubo-Version") == null){
                switch (statusCode.series()) {
                    case CLIENT_ERROR:
                        throw new HttpClientErrorException(statusCode, 
	    				        response.getStatusText(), headers,
	    				getResponseBody(response), getCharset(response));	
                    case SERVER_ERROR:
                        throw new HttpServerErrorException(statusCode,
                                response.getStatusText(), headers, 
                        getResponseBody(response), getCharset(response));
                    default:
                        throw new RestClientException(
                            "Unknown status code [" + statusCode + "]");
                }	
            }
            // should be one of ours - a stubo json error
            String payload = new Scanner(response.getBody(),
                "UTF-8").useDelimiter("\\A").next();
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            StuboResponse jsonPayload = objectMapper.readValue(payload, 
                StuboResponse.class);
            Map<String, ?> error = jsonPayload.getError();
            String errorMessage = (String) error.get("message");
            logger.error(errorMessage);
            throw new StuboException(statusCode, response.getStatusText(), 
                headers, jsonPayload);	
        }
    	
    	private HttpStatus getHttpStatusCode(ClientHttpResponse response) 
    	throws IOException {
    		HttpStatus statusCode;
    		try {
    			statusCode = response.getStatusCode();
    		}
    		catch (IllegalArgumentException ex) {
    			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(),
    					response.getStatusText(), response.getHeaders(), 
    					getResponseBody(response), getCharset(response));
    		}
    		return statusCode;
    	}
    	
    	private byte[] getResponseBody(ClientHttpResponse response) {
    		try {
    			InputStream responseBody = response.getBody();
    			if (responseBody != null) {
    				return FileCopyUtils.copyToByteArray(responseBody);
    			}
    		}
    		catch (IOException ex) {
    			// ignore
    		}
    		return new byte[0];
    	}

    	
    }
    
}
