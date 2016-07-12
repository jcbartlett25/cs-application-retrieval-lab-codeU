package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        
        Map<String, Integer> or = new HashMap<String, Integer>();

        // Add all the values from the previous map
        for (String searchWord : this.map.keySet()) {

        	or.put(searchWord, map.get(searchWord));
        }

        for (String searchWord : that.map.keySet()) {

        	Integer relevance1 = this.getRelevance(searchWord);
        	Integer relevance2 = that.getRelevance(searchWord);
        	or.put(searchWord, totalRelevance(relevance1,relevance2));
        }

         WikiSearch orSearch = new WikiSearch(or);
         return orSearch;
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        
        Map<String, Integer> and = new HashMap<String, Integer>();

        for(String searchWord : this.map.keySet()) {

        	// Needs to exist and be greater than zero
        	if (that.getRelevance(searchWord) != null && !that.getRelevance(searchWord).equals(0)) {

        		int relevance1 = this.getRelevance(searchWord);
	        	int relevance2 = that.getRelevance(searchWord);
	        	and.put(searchWord, totalRelevance(relevance1,relevance2));
        	}
        }

        WikiSearch andSearch = new WikiSearch(and);
        return andSearch;
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        
        Map<String, Integer> minus = new HashMap<String, Integer>();

        for (String searchWord : this.map.keySet()) {

        	if (!that.map.containsKey(searchWord)) {

        		minus.put(searchWord, this.getRelevance(searchWord));
        	}
        }

        WikiSearch minusSearch = new WikiSearch(minus);
        return minusSearch;
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
        
        List<Entry<String, Integer>> sorted = new LinkedList<Entry<String, Integer>>(this.map.entrySet());

        Comparator<Entry<String, Integer>> comparator = getEntryComparator();
        
        Collections.sort(sorted, comparator);

        return sorted;
	}

	private Comparator<Entry<String, Integer>> getEntryComparator() {

		Comparator<Entry<String, Integer>> comparator = new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> thisOne, Entry<String, Integer> thatOne) {

            	if (thisOne.getValue() > thatOne.getValue())
            		return 1;

            	else if (thisOne.getValue() < thatOne.getValue())
            		return -1;

            	else
            		return 0;
            }
        };

        return comparator;
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
