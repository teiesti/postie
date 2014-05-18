package de.teiesti.postie.serializers.matcher;

/**
 * A {@code KnuthMorrisPrattMatcher} is a {@link Matcher} that uses an adjusted version of the Knuth–Morris–Pratt
 * algorithm. This matcher does only use a minimum of resources: If {@code n} is the search pattern length, it does not
 * need more space than {@code O(n)} independent how often {@link #feed(char)} is called. Any method excepts
 * {@link #initialize(String)} works in {@code O(1)}; {@link #initialize(String)} works in {@code O(n)}.
 */
public class KnuthMorrisPrattMatcher implements Matcher {

	private char[] pattern;
	private int patternPos = 0;
	private int[] prefixTable;

	@Override
	public void initialize(String pattern) {
		if (pattern == null)
			throw new IllegalArgumentException("pattern == null");

		this.pattern = pattern.toCharArray();
		analysePattern();
	}

	private void analysePattern() {
		prefixTable = new int[pattern.length + 1];

		int i = 0;
		int j = -1;

		prefixTable[i] = j;

		while (i < pattern.length) {

			while (j >= 0 && pattern[j] != pattern[i])
				j = prefixTable[j];

			i++;
			j++;

			prefixTable[i] = j;
		}
	}

	@Override
	public boolean feed(char c) {
		while (patternPos >= 0 && c != pattern[patternPos])
			patternPos = prefixTable[patternPos];

		patternPos++;

		boolean result = patternPos == pattern.length;
		if (result)
			patternPos = prefixTable[patternPos];

		return result;
	}

	@Override
	public void reset() {
		patternPos = 0;
	}

}
