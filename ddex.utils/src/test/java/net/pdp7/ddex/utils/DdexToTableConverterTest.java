package net.pdp7.ddex.utils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.TestCase;

public class DdexToTableConverterTest extends TestCase {
	public void test() throws Exception {
		List<Map<String, Object>> output = new DdexToTableConverter().convert(new File("src/test/resources/8421597103035.xml")).collect(Collectors.toList());
		System.out.println(output);
	}

	public void testGetProductionYearFrom() {
		assertEquals(2017, new DdexToTableConverter().getProductionYearFrom("NLF711709442"));
	}
}
