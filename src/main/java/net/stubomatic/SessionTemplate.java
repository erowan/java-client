package net.stubomatic;

import org.springframework.web.client.RestTemplate;

public interface SessionTemplate {
	
	public void execute(RestTemplate restTemplate);

}
