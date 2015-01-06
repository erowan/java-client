package net.stubomatic;

import java.util.Map;

import org.springframework.util.Assert;

public class StuboResponse {

	private String version;
	private Map<String, ?> data;
	private Map<String, ?> error;
	
	public String getVersion() {
		return this.version;
	}
	
	public boolean isError() {
		// gets set to null during jackson internalize if not an error
		return this.error != null;
	}
	
	public Map<String, ?> getData() {
		Assert.isTrue(!this.isError(), "Response is an error");
		return this.data;
	}
	
	public Map<String, ?> getError() {
        Assert.isTrue(this.isError(), "Response is *not* an error");
		return this.error;
	}

	@Override
	public String toString() {
		return "StuboResponse [version=" + version + ", data=" + data
				+ ", error=" + error + "]";
	}
}
