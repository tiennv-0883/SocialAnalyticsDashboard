package com.example.socialanalytics.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Builds small .xlsx files for tests; numbers become numeric cells, null leaves the cell empty. */
public final class ExcelTestFiles {

	private ExcelTestFiles() {
	}

	public static byte[] workbook(Object[][] rows) throws IOException {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet();
			for (int r = 0; r < rows.length; r++) {
				Row row = sheet.createRow(r);
				for (int c = 0; c < rows[r].length; c++) {
					Object value = rows[r][c];
					if (value instanceof Number number) {
						row.createCell(c).setCellValue(number.doubleValue());
					}
					else if (value != null) {
						row.createCell(c).setCellValue(value.toString());
					}
				}
			}
			workbook.write(out);
			return out.toByteArray();
		}
	}

}
