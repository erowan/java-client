package net.stubomatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Map;

import net.stubomatic.StuboResponse;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestStuboResponse {
	
	@Test
	public void testCtor() {
		StuboResponse response = new StuboResponse();
		assertNotNull(response);
	}
	
	@Test
	public void testToString() {
		StuboResponse response = new StuboResponse();
		String str = response.toString();
		assertEquals(str, "StuboResponse [version=null, data=null, error=null]");
	}
	
	@Test
	public void testIsError() {
		StuboResponse response = new StuboResponse();
	    assertFalse(response.isError());
	}
	
	@Test
	public void testGetData() {
		ObjectMapper objectMapper = new ObjectMapper();    
		String payload = "{\"version\": \"5.6.5\", \"data\": {\"cache_server\": {\"status\": \"ok\", \"local\": true}, \"info\": {\"cluster\": \"rowan\"}, \"database_server\": {\"status\": \"ok\"}}}";
		StuboResponse response;
		try {
			response = objectMapper.readValue(payload, StuboResponse.class);
			assertFalse(response.isError());
			assertEquals(response.getVersion(), "5.6.5");
			Map<String, ?> data = response.getData();
			assertTrue(data.size() != 0);
			response.toString();
		} catch (Exception e) {
			fail("Not expecting an exception, error is: " + e.getMessage());
		} 			
	}
	
	@Test
	public void testGetError() {
		ObjectMapper objectMapper = new ObjectMapper();    
		String payload = "{\"version\": \"5.6.5\", \"error\": {\"message\": \"help!\", \"code\": \"400\"}}";
		StuboResponse response;
		try {
			response = objectMapper.readValue(payload, StuboResponse.class);
			assertTrue(response.isError());
			assertEquals(response.getVersion(), "5.6.5");
			Map<String, ?> error = response.getError();
			String errorMessage = (String) error.get("message");
			assertEquals(errorMessage, "help!");
			String errorCode = (String) error.get("code");
			assertEquals(errorCode, "400");
			response.toString();
		} catch (Exception e) {
			fail("Not expecting an exception, error is: " + e.getMessage());
		} 			
	}
	
	@Test
    public void testNestedArrayValue() {
        ObjectMapper objectMapper = new ObjectMapper(); 
        String payload = "{\"version\": \"5.7.4\", \"data\": {\"links\": [[\"a\", \"b\"], [\"apple\", \"tart\"]]}}";
        try {
            StuboResponse response = objectMapper.readValue(payload, StuboResponse.class);
            Map<String, ?> data = response.getData();
            ArrayList<Object> links = ((ArrayList<Object>) data.get("links"));
            ArrayList<String> archive = (ArrayList<String>) links.get(1);
            assertEquals(archive.get(0), "apple");
        } catch (Exception e) {
            fail("Not expecting an exception, error is: " + e.getMessage());
        }     
    }
}
	