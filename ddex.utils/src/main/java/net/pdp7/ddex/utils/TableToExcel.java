package net.pdp7.ddex.utils;

import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class TableToExcel {

	public HSSFWorkbook convert(List<Map<String, Object>> table, String sheetName) {
		HSSFWorkbook workbook = new HSSFWorkbook();
		String[] columns = table.get(0).keySet().toArray(new String[0]);
		HSSFSheet sheet = workbook.createSheet(sheetName);
		HSSFRow headerRow = sheet.createRow(0);
		for (int i = 0; i < columns.length; i++) {
			headerRow.createCell(i).setCellValue(columns[i]);
		}
		int rowNumber = 1;
		for(Map<String, Object> tableRow : table) {
			HSSFRow row = sheet.createRow(rowNumber++);
			for (int i = 0; i < columns.length; i++) {
				row.createCell(i).setCellValue(tableRow.get(columns[i]).toString());
			}
		}
		return workbook;
	}

}
