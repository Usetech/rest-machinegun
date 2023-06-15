package com.usetech.rest_machinegun.utils.patterns;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;

public class PlaceholdersResolverImpl implements PlaceholderResolver {
	private final String beginPlaceholderSeq;
	private final String endPlaceholderSeq;

	private Function<String, String> valueResolver;
	private Function<String, String> placeholderMapper;

	record PlaceholderPos (
		int startPos,
		int endPos,
		Placeholder placeholder
	) {}

	record Placeholder (
		String placeholder,
		List<PlaceholderPos> positions
	) {}

	static class ResolvingResult {
		StringBuilder builder;
		int lastPatternPos;

		public ResolvingResult() {
			builder = new StringBuilder();
			lastPatternPos = 0;
		}

		public void setLastPatternPos(int lastPatternPos) {
			this.lastPatternPos = lastPatternPos;
		}
	}

	private String pattern;
	private final Map<String, Placeholder> placeholders = new HashMap<>();

	public PlaceholdersResolverImpl(String beginPlaceholderSeq, String endPlaceholderSeq) {
		this.beginPlaceholderSeq = beginPlaceholderSeq;
		this.endPlaceholderSeq = endPlaceholderSeq;
	}

	/**
	 * Register placeholder value resolver
	 * @param valueResolver - function for resolving values of placeholders,
	 *                        consumes placeholder inner text (without braces),
	 *                        returns String result value associated with the placeholder
	 */
	public void setValueResolver(Function<String, String> valueResolver) {
		this.valueResolver = valueResolver;
	}

	/**
	 * Register optional placeholder mapping function applied before resolving value.
	 * Result of this function will be used as argument for value resolver.
	 * If omitted then used original placeholders.
	 * @param placeholderMapper - Placeholder converting function
	 */
	public void setPlaceholderMapper(Function<String, String> placeholderMapper) {
		this.placeholderMapper = placeholderMapper;
	}

	public Function<String, String> getValueResolver() {
		return valueResolver != null ? valueResolver : s -> "";
	}

	public Function<String, String> getPlaceholderMapper() {
		return placeholderMapper != null ? placeholderMapper : s -> s;
	}

	private void pushPlaceholder(String placeholder, int startPos, int endPos) {
		placeholders
			.computeIfAbsent(placeholder, p -> new Placeholder(placeholder, new ArrayList<>()))
			.positions()
			.add(new PlaceholderPos(startPos, endPos, null));
	}

	@Override
	public void compile(String pattern) {
		// clear
		placeholders.values().forEach(p -> p.positions().clear());
		placeholders.clear();
		// lookup
		this.pattern = pattern;
		int pb, pe, pb_, pe_;
		pb = pattern.indexOf(beginPlaceholderSeq);
		while (pb >= 0) {
			pb_ = pb + beginPlaceholderSeq.length();
			pe = pattern.indexOf(endPlaceholderSeq, pb_);
			pe_ = pe + endPlaceholderSeq.length();
			if (pe == -1) { // not found - done
				break;
			}
			String placeholder = pattern.substring(pb_, pe);
			placeholder = getPlaceholderMapper().apply(placeholder);
			pushPlaceholder(placeholder, pb, pe_);
			// next tag
			pb = pattern.indexOf(beginPlaceholderSeq, pe_);
		}
	}

	@Override
	public String resolve(Function<String, String> valueResolver) {
		if (placeholders.size() == 0) return pattern; // degenerated case

		Set<String> keys = placeholders.keySet();
		Map<String, String> values = new HashMap<>(keys.size());
		keys.parallelStream().forEach(k -> values.put(k, valueResolver.apply(k))); // resolve all placeholders
		// sort placeholders
		List<PlaceholderPos> positions = placeholders.entrySet().stream()
			.flatMap(ent -> ent.getValue().positions().stream().map(p -> new PlaceholderPos(p.startPos(), p.endPos(), ent.getValue())))
			.sorted(Comparator.comparingInt(PlaceholderPos::startPos))
			.toList()
			;
		// resolve sequentially
		ResolvingResult result = positions.stream()
			.collect(Collector.of(
				ResolvingResult::new,
				(res, pos) -> {
					res.builder.append(pattern, res.lastPatternPos, pos.startPos());
					res.builder.append(values.get(pos.placeholder().placeholder()));
					res.setLastPatternPos(pos.endPos());
				},
				(res1, res2) -> res1
			));
		// append pattern tail
		result.builder.append(pattern.substring(result.lastPatternPos));
		//
		return result.builder.toString();
	}

	/**
	 * Resolves specified pattern against specified value resolver. Calls {@link #compile(String)} method before resolving.
	 * @param pattern             - text with placeholders (special text sequences bounded with specified delimiters, eg. ${placeholder})
	 * @param customValueResolver - function for resolving values of placeholders,
	 *                              will be used for resolving placeholders instead of function
	 *                              registered with {@link #setValueResolver(Function)}
	 * @return - text with replaced placeholders by its values
	 */
	public String resolve(String pattern, Function<String, String> customValueResolver) {
		compile(pattern);
		return resolve(customValueResolver);
	}

	/**
	 * Resolves specified pattern. Calls {@link #compile(String)} method before resolving.
	 * @param pattern - text with placeholders (special text sequences bounded with specified delimiters, eg. ${placeholder})
	 * @return - text with replaced placeholders by its values
	 */
	public String resolve(String pattern) {
		compile(pattern);
		return resolve(valueResolver);
	}

	/**
	 * Resolves compiled pattern with registered value resolver
	 * @return - text with replaced placeholders by its values
	 */
	public String resolve() {
		return resolve(valueResolver);
	}
}
