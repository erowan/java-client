package net.stubomatic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.AbstractClientHttpRequest;

/**
 * Reworked {@link org.springframework.http.client.ClientHttpRequest} 
 * implementation that uses Apache HttpComponents HttpClient to execute 
 * requests. Changed to call Stubo get/response URL when in 'playback' mode 
 * and 'records' the actual response of a real request when in 'record' mode.
 *
 * <p>Created via the {@link StuboRequestFactory}.
 *
 * @see StuboRequestFactory#createRequest(URI, HttpMethod)
 */
final class StuboHttpRequest extends AbstractClientHttpRequest {
	
	public static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

	private final CloseableHttpClient httpClient;

	private HttpUriRequest httpRequest;

	private final HttpContext httpContext;
	
	private final Session session;
	
	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream();
	
	protected static Logger logger = Logger.getLogger("net.stubo.StuboHttpRequest"); 

	public StuboHttpRequest(CloseableHttpClient httpClient, HttpUriRequest httpRequest, 
			                HttpContext httpContext, Session session) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
		this.httpContext = httpContext;
		this.session = session;
	}
	
	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.bufferedOutput;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	protected ClientHttpResponse executeInternal(HttpHeaders headers) 
	throws IOException {
		byte[] bytes = this.bufferedOutput.toByteArray();
		if (headers.getContentLength() == -1) {
			headers.setContentLength(bytes.length);
		}
		Map<String, String> stuboHeaders = this.getStuboHeaders(this.httpRequest, headers);
		if (this.session.getMode() == "record"){
			Charset charset = getContentTypeCharset(headers.getContentType());
			String payload = new String(bytes, charset.name());
		    this.session.recordRequest(this.httpRequest, payload, stuboHeaders);	        	
		} else {
			try {
				URI stuboURI = this.proxify(this.httpRequest.getURI());
				HttpPost httpPost = new HttpPost(stuboURI);
				for (Map.Entry<String, String> entry : stuboHeaders.entrySet()) {
		            headers.add(entry.getKey(), entry.getValue());
		        }
				if (this.session.getStubo().getAuthHeaders() != null) {
				    for (Entry<String, List<String>> entry : this.session.getStubo().getAuthHeaders().entrySet()) {
	                    headers.put(entry.getKey(), entry.getValue());
	                }
				}
				this.httpRequest = httpPost;
			} catch (URISyntaxException e) {
				throw new IOException("Unable to proxify user => stubo URI: " +
						e.getMessage());
			}
			
		}
		logger.debug("executeInternal => mode=" + this.session.getMode() +
				          ", hdrs: " + headers);	
		/*
		 Note: exceptions can be thrown here if the service is unavailable
		 i.e. Exception in thread "main" org.springframework.web.client.ResourceAccessException: I/O error on POST request for "http://httpbin.org/post":The target server failed to respond; nested exception is org.apache.http.NoHttpResponseException: The target server failed to respond
		 */
		ClientHttpResponse result = executeInternal(headers, bytes);
		logger.debug("executeInternal result: " + result.getStatusCode());
		if (this.session.getMode() == "record"){
			this.session.recordResponse(result);
		} else {
		    Stubo.ErrorHandler stuboErrorHandler = new Stubo.ErrorHandler();
		    if (stuboErrorHandler.hasError(result)){
		        stuboErrorHandler.handleError(result);        
		    }
		}
		this.bufferedOutput = null;
		return result;
	}
	

	protected ClientHttpResponse executeInternal(HttpHeaders headers, 
			byte[] bufferedOutput) throws IOException {
		addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest =
					(HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}
		CloseableHttpResponse httpResponse =
				this.httpClient.execute(this.httpRequest, this.httpContext);
		return new HttpComponentsClientHttpResponse(httpResponse);
	}

	/**
	 * Adds the given headers to the given HTTP request.
	 *
	 * @param httpRequest the request to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpUriRequest httpRequest, HttpHeaders headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN) &&
					!headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
				for (String headerValue : entry.getValue()) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}
		}
	}
	
	static void addStuboHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			httpRequest.addHeader(entry.getKey(), entry.getValue());
		}
	}
	
	private  Map<String, String> getStuboHeaders(HttpUriRequest request,
	                                             HttpHeaders headers){
        /* TODO:     
        if self.auth_token:
            info["Stubo-Auth"] = self.auth_token  
        */
		Map<String, String> info = new HashMap<String, String>();
		info.put("Stubo-Request-URI", request.getURI().toString());
		info.put("Stubo-Request-Host", request.getURI().getHost());
		info.put("Stubo-Request-Method", request.getMethod());
		if (request.getURI().getPath() != null) {
		    info.put("Stubo-Request-Path", request.getURI().getPath());
		}
		if (request.getURI().getQuery() != null) {
		    info.put("Stubo-Request-Query", request.getURI().getQuery());
		} 
		StringBuilder stuboHeaders = new StringBuilder("{");
		for (Map.Entry<String, String> entry : headers.toSingleValueMap().entrySet()) {
		    stuboHeaders.append("'" + entry.getKey() + "' : '" + entry.getValue() + "', ");
        }
		stuboHeaders.append("}");
		info.put("Stubo-Request-Headers", stuboHeaders.toString());
		return info;
	}  
	
	/*
	 * Take a raw url string and turn it into a valid Stubo get/response URL.
	        
	        Before:
	            http://foo.example.com/path
	        After:
	            http://<stubo_host>/stubo/api/get/response
	 */
	private URI proxify(URI sourceURI) throws URISyntaxException{
		String protocol = (this.session.isSSL()) ? "https" : "http";
		String uri = protocol + "://" + this.session.getDC() + 
		  "/stubo/api/get/response?session=" + this.session.getArg("session");
		if (sourceURI.getQuery() != null) {
		    uri += "&"+sourceURI.getQuery();
		}
		return new URI(uri);
	}  
	
	private Charset getContentTypeCharset(MediaType contentType) {
		if (contentType != null && contentType.getCharSet() != null) {
			return contentType.getCharSet();
		}
		else {
			return DEFAULT_CHARSET;
		}
	}

	
	final class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {

		private final CloseableHttpResponse httpResponse;

		private HttpHeaders headers;
		
		private HttpEntity entity = null;

		HttpComponentsClientHttpResponse(CloseableHttpResponse httpResponse) {
			this.httpResponse = httpResponse;
		}


		@Override
		public int getRawStatusCode() throws IOException {
			return this.httpResponse.getStatusLine().getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.httpResponse.getStatusLine().getReasonPhrase();
		}

		@Override
		public HttpHeaders getHeaders() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
				for (Header header : this.httpResponse.getAllHeaders()) {
					this.headers.add(header.getName(), header.getValue());
				}
			}
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
		    // Note: the response entity needs to be repeatedly read as it is 
		    // read first to capture the response in HTTPCall then by the 
		    // clients response extractor. BufferedHttpEntity makes the 
		    // entity 'repeatable'.
		    if (entity == null) {
		        if (this.httpResponse.getEntity() == null)
		            return null;
			    entity = new BufferedHttpEntity(this.httpResponse.getEntity());
		    }    
			return entity.getContent(); 
		}

		@Override
		public void close() {
	        // Release underlying connection back to the connection manager
	        try {
	            try {
	                // Attempt to keep connection alive by consuming its remaining content
	                EntityUtils.consume(this.httpResponse.getEntity());
	            } finally {
	                // Paranoia
	                this.httpResponse.close();
	            }
	        }
	        catch (IOException ignore) {
	        }
		}
	}
}

