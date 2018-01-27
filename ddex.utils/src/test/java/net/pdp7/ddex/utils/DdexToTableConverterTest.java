package net.pdp7.ddex.utils;

import java.io.File;
import java.util.Map;

import junit.framework.TestCase;

public class DdexToTableConverterTest extends TestCase {
	public void test() throws Exception {
		Map<String, Object> output = new DdexToTableConverter().convert(new File("src/test/resources/8421597103035.xml"));
		System.out.println(output);
	}
}
