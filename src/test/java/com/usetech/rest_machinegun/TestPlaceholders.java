package com.usetech.rest_machinegun;

import com.usetech.rest_machinegun.utils.patterns.RandomHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class TestPlaceholders {
	private static final RandomHelper random = new RandomHelper();

	private String randomString(String... args) {
		return random.random(args);
	}
	private List<String> randomStrings(int count, String... args) {
		return Stream.generate(() -> random.random(args))
			.limit(count)
			.toList();
	}

	@Test
	public void testRandomAsciiFixedLength() {
		for (int j = 0; j < 100; j++) {
			String str = randomString("random", "ascii", "10");
			Assertions.assertNotNull(str);
			Assertions.assertEquals(10, str.length());
			byte[] strBuff = str.getBytes();
			Assertions.assertEquals(10, strBuff.length);
			for (int i = 0; i < strBuff.length; i++) {
				Assertions.assertTrue(strBuff[i] >= 32, "Character #%d code not in the range [32, 127]".formatted(i));
			}
		}
	}

	@Test
	public void testRandomAsciiRandomLengthMultiple() {
		for (int j = 0; j < 100; j++) {
			String str = randomString("random", "ascii", "10", "100");
			Assertions.assertNotNull(str);
			Assertions.assertTrue(str.length() >= 10 && str.length() < 100, "String length (%d) not in range [10, 100)".formatted(str.length()));
			byte[] strBuff = str.getBytes();
			Assertions.assertTrue(strBuff.length >= 10 && strBuff.length < 100, "String length (%d) not in range [10, 100)".formatted(strBuff.length));
			for (int i = 0; i < strBuff.length; i++) {
				Assertions.assertTrue(strBuff[i] >= 32, "Character '%s' code (%d) not in the range [32, 127]".formatted(str.substring(i, i + 1), i));
			}
		}
	}

	@Test
	public void testRandomAsciiInvalidLength() {
		Assertions.assertThrows(NumberFormatException.class, () -> randomString("random", "ascii", "1b"));
	}
	@Test
	public void testRandomAsciiInvalidLengthRange() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> randomString("random", "ascii", "10", "5"));
	}
	@Test
	public void testRandomAsciiInvalidRegexp() {
		Assertions.assertThrows(PatternSyntaxException.class, () -> randomString("random", "ascii", "10", "10", "\\"));
	}
	@Test
	public void testRandomAsciiInvalidRegexpRange() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> randomString("random", "ascii", "10", "10", "а-я"));
	}

	@Test
	public void testRandomAsciiRegexpMultiple() {
		String re = "a-zA-Z0-9!&$";
		for (int j = 0; j < 100; j++) {
			String str = randomString("random", "ascii", "10", "10", re);
			Assertions.assertNotNull(str);
			Assertions.assertEquals(10, str.length());
			Assertions.assertTrue(str.matches("^[" + re + "]{10}$"), "%d> String \"%s\" doesn't match regexp range [%s]".formatted(j, str, re));
		}
	}

}
