package com.altamiracorp.lumify.model.query.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Provides a utility for tokenizing an input value with a Lucene {@link Analyzer}
 */
public class LuceneTokenizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneTokenizer.class);

    /**
     * Extracts the tokens that are generated by the {@link StandardAnalyzer} for the specified value
     * @param value The value to analyze, not null
     * @return A list of tokens generated by the analyzer
     */
    public static List<String> standardTokenize(final String value) {
        return tokenizeString(new StandardAnalyzer(Version.LUCENE_42), value);
    }

    /**
     * Extracts the tokens that are generated by the provided analyzer for the specified value
     * @param analyzer The analyzer used for token generation, not null
     * @param value The value to analyze, not null
     * @return A list of tokens generated by the analyzer
     */
    public static List<String> tokenizeString(final Analyzer analyzer, final String value) {
        Preconditions.checkNotNull(analyzer);
        Preconditions.checkNotNull(value);

        final List<String> tokens = Lists.newArrayList();

        try {
            final TokenStream stream = analyzer.tokenStream(null, new StringReader(value));
            stream.reset();

            while (stream.incrementToken()) {
                tokens.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            LOGGER.error("Error occurred while tokenizing stream", e);
            throw new RuntimeException(e);
        }

        return tokens;
    }
}
