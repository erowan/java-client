package net.stubomatic;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;

public class StuboRequestFactory extends HttpComponentsClientHttpRequestFactory {
    
	private final Session session;
	protected static Logger logger = Logger.getLogger("net.stubo.StuboRequestFactory"); 
	private int connectTimeout;
    private int socketTimeout;
	
	public StuboRequestFactory(Session session) {
		super();
		this.session = session;
		if (this.session.getArg("connect_timeout") != null){
		    this.connectTimeout = Integer.parseInt(this.session.getArg("connect_timeout"));
		}
		if (this.session.getArg("socket_timeout") != null){
            this.socketTimeout = Integer.parseInt(this.session.getArg("socket_timeout"));
        }
	}
	
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		logger.debug("createRequest uri: " + uri);
		CloseableHttpClient client = (CloseableHttpClient) getHttpClient();
		Assert.state(client != null, "Synchronous execution requires an HttpClient to be set");
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
        HttpContext context = createHttpContext(httpMethod, uri);
        if (context == null) {
            context = HttpClientContext.create();
        }
        // Request configuration not set in the context
        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
            // Use request configuration given by the user, when available
            RequestConfig config = null;
            if (httpRequest instanceof Configurable) {
                config = ((Configurable) httpRequest).getConfig();
            }
            if (config == null) {
                if (this.socketTimeout > 0 || this.connectTimeout > 0) {
                    config = RequestConfig.custom()
                            .setConnectTimeout(this.connectTimeout)
                            .setSocketTimeout(this.socketTimeout)
                            .build();
                }
				else {
                    config = RequestConfig.DEFAULT;
                }
            }
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
        }
		/*if (this.bufferRequestBody) {
			return new HttpComponentsClientHttpRequest(client, httpRequest, context);
		}
		else {
			return new HttpComponentsStreamingClientHttpRequest(client, httpRequest, context);
		}*/
        return new StuboHttpRequest(client, httpRequest, context, session);
	}

}
