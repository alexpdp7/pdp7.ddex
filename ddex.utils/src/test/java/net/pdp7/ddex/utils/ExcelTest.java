package net.pdp7.ddex.utils;

import java.io.File;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import junit.framework.TestCase;

public class ExcelTest extends TestCase {
	public void test() throws Exception {
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("foo");
		HSSFRow row = sheet.createRow(0);
		row.createCell(0).setCellValue("bar");
		workbook.write(new File("test.xls"));
	}
}
