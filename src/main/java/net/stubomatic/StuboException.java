package net.stubomatic;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;


public class StuboException extends RestClientException {

	private final HttpStatus statusCode;

	private final String statusText;

	private final StuboResponse jsonError;

	private final HttpHeaders responseHeaders;



	/**
	 * Construct a new instance of {@code StuboException} based on an
	 * {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers, may be {@code null}
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 */
	protected StuboException(HttpStatus statusCode, String statusText,
			HttpHeaders responseHeaders, StuboResponse responseBody) {
		super(statusCode.value() + " " + statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.jsonError = responseBody; 
	}
	
	protected StuboException(HttpStatus statusCode, String statusText) {
		super(statusCode.value() + " " + statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
        this.jsonError = new StuboResponse();
        this.responseHeaders = new HttpHeaders();
	}

	public String getMessage() {
		if (this.jsonError.isError()) {
			return this.jsonError.getError().toString();
		} else {
			return super.getMessage();
		}
	}

	/**
	 * Return the HTTP status code.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Return the HTTP status text.
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * Return the HTTP response headers.
	 * @since 3.1.2
	 */
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}
	
	public Map<String, ?> getJsonError() {
		return this.jsonError.getError();
	}

}
