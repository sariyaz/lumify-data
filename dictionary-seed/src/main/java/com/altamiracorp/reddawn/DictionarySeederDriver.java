package com.altamiracorp.reddawn;

import org.json.JSONException;

import java.io.IOException;

public class DictionarySeederDriver {
	public static void main(String[] args) throws IOException, JSONException {
		DictionarySearcher searcher = new DictionarySearcher();
		searcher.search(DictionarySearcher.PERSON);
	}
}
