package com.usetech.rest_machinegun.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MachinegunTaskRequest {
	private String url;
	private MachinegunTaskRequestMethod method;
	private Map<String, String> headers;
	private String body;
	@JsonAlias("volatile")
	private Boolean isVolatile;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public MachinegunTaskRequestMethod getMethod() {
		return method;
	}

	public void setMethod(MachinegunTaskRequestMethod method) {
		this.method = method;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Boolean getVolatile() {
		return isVolatile != null && isVolatile;
	}

	public void setVolatile(Boolean aVolatile) {
		isVolatile = aVolatile;
	}
}
