package de.teiesti.postie.serializer;

import com.google.gson.Gson;

public class GsonSerializer<Letter> extends AbstractSerializer<Letter> {

	private Gson gson = new Gson();
	private Class<Letter> letterClass;

	private GsonSerializer(Class<Letter> letterClass) {
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

		this.letterClass = letterClass;
	}

	@Override
	public String encode(Letter letter) {
		return gson.toJson(letter);
	}

	@Override
	public Letter decode(String letter) {
		return gson.fromJson(letter, letterClass);
	}

	@Override
	public String separator() {
		return System.lineSeparator();
	}

}
