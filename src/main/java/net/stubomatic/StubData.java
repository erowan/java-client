package net.stubomatic;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StubData {
    
    public static class Request {
        public String getMethod() {
            return method;
        }
        public void setMethod(String method) {
            this.method = method;
        }
        public List<Map> getBodyPatterns() {
            return bodyPatterns;
        }
        public void setBodyPatterns(List<Map> bodyPatterns) {
            this.bodyPatterns = bodyPatterns;
        }
             
        @Override
        public String toString() {
            return "Request [method=" + method + ", bodyPatterns=" + bodyPatterns + "]";
        }
        
        private String method;
        private List<Map> bodyPatterns;
       
    }
    
    public static class Response {
        public int getStatus() {
            return status;
        }
        public void setStatus(int status) {
            this.status = status;
        }
        public String getBody() {
            return body;
        }
        public void setBody(String body) {
            this.body = body;
        }
        int status;
        String body;
        
        @Override
        public String toString() {
            return "Response [status=" + status + ", body=" + body + "]";
        }
    }
    
    public static class UserExit {
        
        public UserExit() {
            super();
        }
        
        public UserExit(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return "UserExit [name=" + name + "]";
        }

        String name;
    }
    
    private Request request;
    private Response response;
    private UserExit module;
    
    public UserExit getModule() {
        return module;
    }
    public void setModule(UserExit module) {
        this.module = module;
    }
    public Request getRequest() {
        return request;
    }
    public void setRequest(Request request) {
        this.request = request;
    }
    public Response getResponse() {
        return response;
    }
    public void setResponse(Response response) {
        this.response = response;
    }
    
    public StubData() {
    }
    
    public StubData(String requestBody, String responseBody){
        this.set(requestBody, responseBody);
    }
    
    public void set(String requestBody, String responseBody){
        this.set(requestBody, responseBody, "POST", 200);
    }
    
    public void set(String requestBody, String responseBody, String method, 
                    int status){
        StubData.Request requestData = new StubData.Request();
        requestData.setMethod(method);
        List<?> bodyPatterns = Collections.singletonList(
                Collections.singletonMap("contains", Collections.singletonList(requestBody))
               );
        requestData.setBodyPatterns((List<Map>) bodyPatterns);
        this.setRequest(requestData);
        
        StubData.Response responseData = new StubData.Response();
        responseData.setStatus(status);
        responseData.setBody(responseBody);
        this.setResponse(responseData);
    }
    
    public String toJSONString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);     
    }
    
    @Override
    public String toString() {
        return "StubData [request=" + request + ", response=" + response
                + ", module=" + module + "]";
    }

}
