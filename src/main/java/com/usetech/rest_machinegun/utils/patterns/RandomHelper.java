package com.usetech.rest_machinegun.utils.patterns;

import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class RandomHelper {
	private final Random random = new Random();

	private long randomRange(long f, long t) {
		return f + random.nextLong(t - f);
	}
	private long randomLong() {
		return random.nextLong();
	}
	private long randomInteger() {
		return random.nextInt();
	}
	private long randomByte() {
		return (byte) (random.nextInt() & 0xFF);
	}

	private String randomAscii(int len, Predicate<Character> filter) {
		return Stream
			.generate(() -> (char) Integer.valueOf(32 + random.nextInt(128 - 32)).byteValue())
			.filter(filter != null ? filter : x -> true)
			.limit(len)
			.collect(Collector.of(
				StringBuilder::new,
				StringBuilder::append,
				StringBuilder::append,
				StringBuilder::toString
			));
	}

	private static final String ASCII_ALL_CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	private static final int ASCII_ALL_CHARS_COUNT = ASCII_ALL_CHARS.length();
	private void checkRangeRegExp(String regexpRange) {
		if (ASCII_ALL_CHARS.replaceAll("[" + regexpRange + "]", "").length() < ASCII_ALL_CHARS_COUNT) return; // there are matches
		throw new IllegalArgumentException("No one ASCII character doesn't match specified regexp");
	}

	public String random(String... args) {
		if (args.length == 0) return null;
		int offs = "random".equalsIgnoreCase(args[0]) ? 1 : 0;
		if (args.length > offs) {
			switch (args[offs++].toLowerCase()) {
				case "byte":
					return Long.valueOf(randomByte()).toString();
				case "int":
				case "integer":
					return Long.valueOf(randomInteger()).toString();
				case "long":
					return Long.valueOf(randomLong()).toString();
				case "uuid":
					return UUID.randomUUID().toString();
				case "range":
					if (args.length <= offs + 1) return null;
					return Long.valueOf(randomRange(Long.parseLong(args[offs]), Long.parseLong(args[offs + 1]))).toString();
				case "ascii":
					if (args.length <= offs) return null;
					int len = Integer.parseInt(args[offs++]);
					if (args.length >= offs + 1) { // 2 length arg
						long len2 = Long.parseLong(args[offs++]);
						if (len2 < len) {
							throw new IllegalArgumentException("Range max bound must be greater than range min bound");
						}
						if (len2 > len) {
							len = (int) randomRange(len, len2);
						}
					}
					if (args.length >= offs + 1) { // regexp range
						checkRangeRegExp(args[offs]);
						Pattern patt = Pattern.compile("[" + args[offs] + "]");
						return randomAscii(len, ch -> patt.matcher(ch.toString()).matches());
					} else {
						return randomAscii(len, null);
					}
				default:
					return null;
			}
		}
		return Long.valueOf(randomLong()).toString();
	}
}
