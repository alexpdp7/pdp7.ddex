package net.pdp7.ddex.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import junit.framework.TestCase;

public class DdexZipConverterTest extends TestCase {

	public void test() throws Exception {
		new DdexZipConverter(new DdexToTableConverter(), new TableToExcel())
				.convert(new FileInputStream("src/test/resources/sample.zip"), new FileOutputStream("release_from_zip.xls"));
	}
}
