package com.usetech.rest_machinegun.utils.patterns;

import java.util.function.Function;

public class MustacheResolver implements PlaceholderResolver {
	public static final String MUSTACHE_BEGIN = "{{";
	public static final String MUSTACHE_END = "}}";

	private final PlaceholdersResolverImpl resolver = new PlaceholdersResolverImpl(MUSTACHE_BEGIN, MUSTACHE_END);

	public MustacheResolver() {
		resolver.setPlaceholderMapper(s -> s != null ? s.strip() : null);
	}

	@Override
	public void compile(String pattern) {
		resolver.compile(pattern);
	}

	@Override
	public String resolve(Function<String, String> valueResolver) {
		return resolver.resolve(valueResolver);
	}

	public static boolean hasPlaceholder(String pattern) {
		return pattern.contains(MUSTACHE_BEGIN);
	}
}
