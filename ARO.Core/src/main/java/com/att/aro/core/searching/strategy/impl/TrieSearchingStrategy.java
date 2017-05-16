/*
 *  Copyright 2017 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.searching.strategy.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingPattern;
import com.att.aro.core.searching.pojo.SearchingResult;
import com.att.aro.core.searching.pojo.SearchingResultBuilder;
import com.att.aro.core.searching.strategy.ISearchingStrategy;

public class TrieSearchingStrategy implements ISearchingStrategy {
	
	/**
	 * TrieNode
	 * 
	 *
	 *
	 */
	private class TrieNode {
		private Map<Character, TrieNode> children;
		private boolean isEnd;
		private String pattern;
		private String type;
		
		public TrieNode() {
			children = new HashMap<Character, TrieNode>();
		}
	}
	
	/**
	 * keyword Trie
	 * 
	 *
	 *
	 */
	private class KeywordTrie {
		
		private TrieNode root;
		
		public KeywordTrie() {
			root = new TrieNode();
		}
		
		// insert a pattern
		public void insert(String pattern, String type) {
			insertHelper(pattern.toCharArray(), type, 0, root);
		}
		
		private void insertHelper(char[] chars, String type, int index, TrieNode root) {
			if (index == chars.length) {
				root.isEnd = true;
				root.pattern = new String(chars);
				root.type = type;
				return;
			}
			
			char next = chars[index];
			if (!root.children.containsKey(next)) {
				root.children.put(next, new TrieNode());
			}
			insertHelper(chars, type, index + 1, root.children.get(next));
		}
		
		// peek trie to get next trie node
		public TrieNode peek(TrieNode current, char next) {
			if (current.children.containsKey(next)) {
				return current.children.get(next);
			} else {
				return null;
			}
		}
		
		// check if the upcoming character is a valid start
		public boolean isCandidateStart(char character) {
			return root.children.containsKey(character);
		}
	}
	
	@Override
	public SearchingResult applySearch(SearchingPattern pattern, SearchingContent content) {
		SearchingResultBuilder resultBuilder = new SearchingResultBuilder();

		List<String> words = pattern.getWords();
		List<String> types = pattern.getTypes();
		String text = content.get();

		KeywordTrie trie = buildTrie(words, types);
		
		Map<String, String> wordsFound = search(trie, text);
		
		Set<String> keySet = wordsFound.keySet();
		for(String key : keySet) {
			resultBuilder.add(key, wordsFound.get(key));
		}

		return resultBuilder.build();
	}
	
	private Map<String, String> search(KeywordTrie trie, String text) {
		Map<String, String> result = new HashMap<String, String>();
		
		Queue<Integer> candidateStartQueue = new LinkedList<Integer>();
		
		int index = 0;
		while(index < text.length()) {
			
			while(index < text.length() && !trie.isCandidateStart(text.charAt(index))) {
				index++;
			}
			
			if (index == text.length()) {
				return result;
			}
			candidateStartQueue.add(index);
			
			int nextStart = index + 1;
			
			while(!candidateStartQueue.isEmpty()) {
				int candidate = candidateStartQueue.poll();
				
				TrieNode current = trie.root;	// check from root
				int runIndex = candidate;	// candidate start point
				while(runIndex < text.length() && trie.peek(current, text.charAt(runIndex)) != null) {
					current = trie.peek(current, text.charAt(runIndex++));
					
					if (current.isEnd && !result.containsKey(current.pattern)) {
						result.put(current.pattern, current.type);
					}
					
					if (runIndex < text.length() && trie.isCandidateStart(text.charAt(runIndex))) {
						candidateStartQueue.add(runIndex);
					}
				}
				
				nextStart = Math.max(nextStart, candidate + 1);
			}
			
			index = nextStart;
		}
		
		return result;
	}

	/**
	 * build trie from list of keywords
	 * 
	 * @param keywords
	 * @return
	 */
	private KeywordTrie buildTrie(List<String> keywords, List<String> types) {
		KeywordTrie trie = new KeywordTrie();
		
		for(int i = 0; i < keywords.size(); i++) {
			trie.insert(keywords.get(i), types.get(i));
		}
		
		return trie;
	}
}
