package net.stubomatic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;


public class Testing {
	
	public Properties props = new Properties();

	public Testing() {
		super();
		ClassPathResource resource = new ClassPathResource("testing.properties");
		InputStream is = null;
		try {
		    is = resource.getInputStream();
            props.load(is);
		} catch (IOException ex) {
        	System.out.println("Unable to load testing.properties, error: " +
        			ex.getMessage());
        }
        finally {
            if (is != null) {
                try { is.close(); } catch (IOException ex) {} ;
        	}
        }   
	}
	
	
}