package com.att.aro.core.packetanalysis.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Data;

public class VideoStreamConstructorTest {

	@Test
	public void testFindMagic() throws Exception {
//		VideoStreamConstructor videoStreamConstructor = new VideoStreamConstructor();
		
		Map<String, MyClass> classMap = new HashMap<>();
		classMap.put("one", new MyClass("Me", 1));
		classMap.put("two", new MyClass("Bayley", 2));
		classMap.put("three", new MyClass("Artimus", 3));
		classMap.put("four", new MyClass("Apollo", 4));
		
		System.out.println(this.findMagic(classMap, "two"));
	}

	@Test
	public void anotherExercise() {

		Map<String, Double> productPrice = new HashMap<>();
		// add value
		productPrice.put("Rice", 6.9);
		productPrice.put("Flour", 3.9);
		productPrice.put("Sugar", 4.9);
		productPrice.put("Milk", 3.9);
		productPrice.put("Egg", 1.9);

		// Set<String> keys = productPrice.keySet();
		// keys.forEach(key -> System.out.println(key));

		Collection<Double> values = productPrice.values();
		values.forEach(value -> System.out.println(value));

		productPrice.forEach((key, value) -> {
			System.out.print("\nkey: " + key +", Value: " + value);
		});
	}
	
	@Data
	@AllArgsConstructor
	public class MyClass{
		String name;
		int id;
	}
	
	public String findMagic(Map<String, MyClass> classMap, String findThis) {
		
//		Map<String, MyClass> result =
//				classMap.entrySet().stream().collect(Collectors.toMap(MyClass::getName, c -> c));
//		
//		Optional<Entry<String, MyClass>> result = classMap
//				.entrySet()
//				.stream()
//				.filter(x -> MyClass::getName(), findThis)
//				.findFirst()
////				.orElse(null) != null)
//				;
		
		return null;
	}

}
