package com.usetech.rest_machinegun.utils.patterns;

import java.util.function.Function;

public class NullResolver implements PlaceholderResolver {
	private String pattern;

	@Override
	public void compile(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String resolve(Function<String, String> valueResolver) {
		return pattern;
	}
}
