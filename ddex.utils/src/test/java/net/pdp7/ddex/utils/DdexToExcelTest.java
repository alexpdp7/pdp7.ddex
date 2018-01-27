package net.pdp7.ddex.utils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.TestCase;

public class DdexToExcelTest extends TestCase {
	public void test() throws Exception {
		List<Map<String, Object>> releaseTable = new DdexToTableConverter()
				.convert(new File("src/test/resources/8421597103035.xml"))
				.collect(Collectors.toList());
		new TableToExcel().convert(releaseTable, "release").write(new File("release.xls"));

	}
}
