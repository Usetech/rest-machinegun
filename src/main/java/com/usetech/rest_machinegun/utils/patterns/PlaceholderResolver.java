package com.usetech.rest_machinegun.utils.patterns;

import java.util.function.Function;

public interface PlaceholderResolver {
	/**
	 * Compiles the given pattern and prepares for resolving pattern placeholders
	 * @param pattern - text with placeholders (special text sequences bounded with specified delimiters, eg. ${placeholder})
	 */
	void compile(String pattern);

	/**
	 * Resolves specified pattern against specified value resolver. Calls {@link #compile(String)} method before resolving.
	 * @param valueResolver - function for resolving values of placeholders
	 * @return - text with replaced placeholders by its values
	 */
	String resolve(Function<String, String> valueResolver);
}
