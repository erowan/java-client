package net.stubomatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Map;

import net.stubomatic.StubData;
import net.stubomatic.StubData.UserExit;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestStubData {
    
    @Test
    public void testCtor() {
        StubData data = new StubData();
        assertNotNull(data);
    }
    
    @Test
    public void testMapper() {
        ObjectMapper mapper = new ObjectMapper();    
        String payload = "{\"request\":{\"method\":\"POST\",\"bodyPatterns\":[{\"contains\":[\"hello world\"]}]},\"response\":{\"status\":200,\"body\":\"goodbye\"},\"module\":{\"name\":\"foo\"}}";
        StubData stub;
        try {
            stub = mapper.readValue(payload, StubData.class);
            String result = mapper.writeValueAsString(stub);
            assertEquals(result, payload);        
        } catch (Exception e) {
            fail("Not expecting an exception, error is: " + e.getMessage());
        }           
    }
    
    @Test
    public void testCtorSet() {
        StubData stub = new StubData("hello", "goodbye");
        try {
            String payload = stub.toJSONString();
            assertEquals("{\"request\":{\"method\":\"POST\",\"bodyPatterns\":[{\"contains\":[\"hello\"]}]},\"response\":{\"status\":200,\"body\":\"goodbye\"},\"module\":null}", payload);        
        } catch (Exception e) {
            fail("Not expecting an exception, error is: " + e.getMessage());
        }           
    }
    
    @Test
    public void testSet() {
        StubData stub = new StubData();
        stub.set("hello", "goodbye");
        try {
            String payload = stub.toJSONString();
            assertEquals("{\"request\":{\"method\":\"POST\",\"bodyPatterns\":[{\"contains\":[\"hello\"]}]},\"response\":{\"status\":200,\"body\":\"goodbye\"},\"module\":null}", payload);        
        } catch (Exception e) {
            fail("Not expecting an exception, error is: " + e.getMessage());
        }           
    }
    
   
}
    