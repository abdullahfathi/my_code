package com.medallia.word2vec;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/** Provides search functionality */
public interface Searcher {
	/** Represents a match to a search word */
	public interface Match {
		/** {@link Ordering} which compares {@link Match#distance()} */
		Ordering<Match> ORDERING = Ordering.natural().onResultOf(new Function<Match, Double>() {
			@Override public Double apply(Match match) {
				return match.distance();
			}
		});
		/** {@link Function} which forwards to {@link #match()} */
		Function<Match, String> TO_WORD = new Function<Match, String>() {
			@Override public String apply(Match result) {
				return result.match();
			}
		};
		/** @return Cosine distance of the match */
		double distance();
		/** @return Matching word */
		String match();
	}
	
	/** Represents the similarity between two words */
	public interface SemanticDifference {
		/** @return Top matches to the given word which share this semantic relationship */
		List<Match> getMatches(String word, int maxMatches) throws UnknownWordException;
	}
	
	/** Exception when a word is unknown to the {@link Word2VecModel}'s vocabulary */
	public static class UnknownWordException extends Exception {
		UnknownWordException(String word) {
			super(String.format("Unknown search word '%s'", word));
		}
	}
	
	/** @return true if a word is inside the model's vocabulary. */
	boolean contains(String word);
	
	/** @return cosine similarity between two words. */
	double cosineDistance(String s1, String s2) throws UnknownWordException;
	
	/** @return Top matches to the given vector */
	List<Match> getMatches(final double[] vec, int maxNumMatches);

	/** @return Top matches to the given word */
	List<Match> getMatches(String word, int maxMatches) throws UnknownWordException;
	
	/** @return Raw word vector */
	ImmutableList<Double> getRawVector(String word) throws UnknownWordException;
	
	/** @return {@link SemanticDifference} between the word vectors for the given */
	SemanticDifference similarity(String s1, String s2) throws UnknownWordException;
}
