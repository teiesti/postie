package de.teiesti.postie.serializers.matcher;

/**
 * A {@code Matcher} is an automaton which is fed with characters. For each given character the automaton returns
 * weather the string that can be built by concatenating the characters in the order they were given ends with a
 * previously given search pattern.<br>
 * In other words, a {@code Matcher} solves the string matching problem for string that is given character by character.
 * Moreover, a {@code Matcher} must return a result for an already given substring before the next character is hand
 * over.
 */
public interface Matcher {

	/**
	 * Initializes this {@code Matcher} with the given {@link String} as search pattern. Henceforth
	 * {@link #feed(char)} returns {@code true} if and only if the search pattern is equal to the end of the already
	 * given character sequence.<br>
	 * In case this method is called after any character was fed to {@link #feed(char)},
	 * the fed characters will be discarded: This means the {@code Matcher} behaves as if these characters were
	 * never passed.<br>
	 * This method must be called at least once before any character can be fed to this {@code Matcher} using
	 * {@link #feed(char)}. In case this method is called twice, the second call overrides the first one.
	 *
	 * @param pattern the new search pattern
	 */
	public void initialize(String pattern);

	/**
	 * Feeds the given character to this {@code Matcher}. This method returns weather the string that can be built by
	 * concatenating the given characters in the order they were passed to this method ends with a previously given
	 * search pattern. In detail, the characters passed to this method can be interpreted as a string: Before the
	 * first method call the string is empty. Each time this method is called the given character is appended to the
	 * existing string. This method returns if the string ends with a search pattern that was given during
	 * initialization (see {@link #initialize(String)}.<br>
	 * You may find this method very helpful for solving the string matching problem. Simply pass your string
	 * (character by character) to this method until it returns {@code true} for the first time. If this happens you
	 * have found the end of the first occurrence of the search pattern that was given to {@link #initialize(String)}
	 * in your string. This does also work for string of infinite (or not yet known) length.
	 *
	 * @param c the character to feed to this {@code Matcher}
	 * @return weather the search pattern was found
	 */
	public boolean feed(char c);

	/**
	 * Resets this {@code Matcher}. After calling this method this {@code Matcher} behaves as if {@link #feed(char)}
	 * has never been called before. Any character that was fed to {@link #feed(char)} will be discarded; the "string"
	 * (see {@link #feed(char)} becomes empty again. This method does not affect the search pattern given to
	 * {@link #initialize(String)}.
	 */
	public void reset();

}
