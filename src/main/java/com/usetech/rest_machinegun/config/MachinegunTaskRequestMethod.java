package com.usetech.rest_machinegun.config;

import io.vertx.core.http.HttpMethod;

public enum MachinegunTaskRequestMethod {
	GET,
	POST,
	PUT,
	DELETE,
	HEAD;

	public HttpMethod toHttpMethod() {
		return switch (this) {
			case GET -> HttpMethod.GET;
			case POST -> HttpMethod.POST;
			case PUT -> HttpMethod.PUT;
			case DELETE -> HttpMethod.DELETE;
			case HEAD -> HttpMethod.HEAD;
		};
	}
}
