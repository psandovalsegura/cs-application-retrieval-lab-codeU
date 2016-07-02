package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {

	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 *
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}

	/**
	 * Looks up the relevance of a given URL.
	 *
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}

	/**
	 * Prints the contents in order of term frequency.
	 *
	 * @param map
	 */
	private  void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}

	/**
	 * Computes the union of two search results.
	 *
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        Map<String, Integer> map = new HashMap<String, Integer>();
		//Get the sets of the keys for both
		Set<String> set1 = this.map.keySet();
		Set<String> set2 = that.map.keySet();

		//The key union of the sets will have a sum value, all other keys will retain their values
		for (String url : set1) {
			if (set2.contains(url)) {
				map.put(url, this.getRelevance(url) + that.getRelevance(url));
			} else {
				map.put(url, this.getRelevance(url));
			}
		}

		for (String url: set2) {
			if(!map.containsKey(url)) {
				map.put(url, that.getRelevance(url));
			}
		}

		return new WikiSearch(map);
	}

	/**
	 * Computes the intersection of two search results.
	 *
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		//Get the sets of the keys for both
		Set<String> set1 = this.map.keySet();
		Set<String> set2 = that.map.keySet();

		//The key intersections of the sets will have a sum value
		for (String url : set1) {
			if (set2.contains(url)) {
				map.put(url, this.getRelevance(url) + that.getRelevance(url));
			}
		}

		return new WikiSearch(map);
	}

	/**
	 * Computes the intersection of two search results.
	 *
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		//Get the sets of the keys for both
		Set<String> set1 = this.map.keySet();
		Set<String> set2 = that.map.keySet();

		//The key difference of the sets will have a sum value
		for (String url : set1) {
			if (!set2.contains(url)) {
				map.put(url, this.getRelevance(url));
			}
		}

		return new WikiSearch(map);
	}

	/**
	 * Computes the relevance of a search with multiple terms.
	 *
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 *
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
		//Add map entries to a list to be sorted
		List<Entry<String, Integer>> urlEntries = new ArrayList<Entry<String, Integer>>();
		for (Entry<String, Integer> urlEntry : this.map.entrySet()) {
			urlEntries.add(urlEntry);
		}

		//Define how an entry list is sorted - based on integer values
        Comparator<Entry<String, Integer>> comparator = new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> entry1, Entry<String, Integer> entry2) {
				if (entry1.getValue() > entry2.getValue()) {
					return 1;
				} else if (entry1.getValue() < entry2.getValue()) {
					return -1;
				}

				//Both equal
				return 0;
			}
		};

		//Perform sort
		Collections.sort(urlEntries, comparator);

		return urlEntries;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 *
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException {

		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);

		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();

		// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();

		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
	}
}
