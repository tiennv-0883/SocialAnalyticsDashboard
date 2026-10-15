package com.example.socialanalytics.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelMapper {

	private static final String DATE_FORMAT = "yyyy-mm-dd hh:mm:ss";

	private ExcelMapper() {
	}

	public record RowResult<T>(int rowNumber, T value, String error) {

		public boolean isSuccess() {
			return error == null;
		}

	}

	public static <T> List<RowResult<T>> read(InputStream input, Class<T> type) {
		List<Field> fields = columnFields(type);
		Constructor<T> constructor = noArgConstructor(type);
		try (Workbook workbook = WorkbookFactory.create(input)) {
			Sheet sheet = workbook.getSheetAt(0);
			Row header = sheet.getRow(sheet.getFirstRowNum());
			if (header == null) {
				throw new ExcelProcessingException("Excel file is empty");
			}
			Map<Field, Integer> columnIndexes = resolveColumns(header, fields);
			DataFormatter formatter = new DataFormatter();

			List<RowResult<T>> results = new ArrayList<>();
			for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (isBlank(row, formatter)) {
					continue;
				}
				int rowNumber = i + 1;
				try {
					T target = constructor.newInstance();
					for (Map.Entry<Field, Integer> entry : columnIndexes.entrySet()) {
						Field field = entry.getKey();
						Cell cell = row.getCell(entry.getValue());
						try {
							field.set(target, convert(cell, field.getType(), formatter));
						}
						catch (IllegalArgumentException ex) {
							throw new ExcelProcessingException(
									"Column '" + field.getAnnotation(ExcelColumn.class).name() + "': " + ex.getMessage());
						}
					}
					results.add(new RowResult<>(rowNumber, target, null));
				}
				catch (ExcelProcessingException ex) {
					results.add(new RowResult<>(rowNumber, null, ex.getMessage()));
				}
				catch (ReflectiveOperationException ex) {
					throw new ExcelProcessingException("Cannot map row to " + type.getSimpleName(), ex);
				}
			}
			return results;
		}
		catch (IOException | org.apache.poi.EmptyFileException | org.apache.poi.UnsupportedFileFormatException ex) {
			throw new ExcelProcessingException("Cannot read Excel file: " + ex.getMessage(), ex);
		}
	}

	public static <T> byte[] write(List<T> items, Class<T> type, String sheetName) {
		List<Field> fields = columnFields(type);
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(sheetName);
			CellStyle headerStyle = headerStyle(workbook);
			CellStyle dateStyle = workbook.createCellStyle();
			dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(DATE_FORMAT));

			Row header = sheet.createRow(0);
			for (int c = 0; c < fields.size(); c++) {
				Cell cell = header.createCell(c);
				cell.setCellValue(fields.get(c).getAnnotation(ExcelColumn.class).name());
				cell.setCellStyle(headerStyle);
			}
			for (int r = 0; r < items.size(); r++) {
				Row row = sheet.createRow(r + 1);
				for (int c = 0; c < fields.size(); c++) {
					writeCell(row.createCell(c), fields.get(c).get(items.get(r)), dateStyle);
				}
			}
			for (int c = 0; c < fields.size(); c++) {
				sheet.autoSizeColumn(c);
			}
			workbook.write(out);
			return out.toByteArray();
		}
		catch (IOException | IllegalAccessException ex) {
			throw new ExcelProcessingException("Cannot write Excel file", ex);
		}
	}

	static List<Field> columnFields(Class<?> type) {
		List<Field> fields = Arrays.stream(type.getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(ExcelColumn.class))
			.sorted(Comparator.comparingInt(field -> field.getAnnotation(ExcelColumn.class).order()))
			.toList();
		if (fields.isEmpty()) {
			throw new IllegalArgumentException(type.getName() + " has no @ExcelColumn fields");
		}
		fields.forEach(field -> field.setAccessible(true));
		return fields;
	}

	private static <T> Constructor<T> noArgConstructor(Class<T> type) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor;
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(type.getName() + " needs a no-arg constructor", ex);
		}
	}

	private static Map<Field, Integer> resolveColumns(Row header, List<Field> fields) {
		Map<String, Integer> headerIndexes = new HashMap<>();
		for (Cell cell : header) {
			if (cell.getCellType() == CellType.STRING) {
				headerIndexes.put(normalize(cell.getStringCellValue()), cell.getColumnIndex());
			}
		}
		Map<Field, Integer> columnIndexes = new HashMap<>();
		for (Field field : fields) {
			Integer index = headerIndexes.get(normalize(field.getAnnotation(ExcelColumn.class).name()));
			if (index != null) {
				columnIndexes.put(field, index);
			}
		}
		if (columnIndexes.isEmpty()) {
			throw new ExcelProcessingException("No known columns found in header row. Expected: "
					+ fields.stream().map(field -> field.getAnnotation(ExcelColumn.class).name()).toList());
		}
		return columnIndexes;
	}

	private static Object convert(Cell cell, Class<?> type, DataFormatter formatter) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			if (type.isPrimitive()) {
				throw new IllegalArgumentException("value is required");
			}
			return null;
		}
		if (type == Instant.class || type == LocalDateTime.class || type == LocalDate.class) {
			LocalDateTime dateTime = (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
					? cell.getLocalDateTimeCellValue() : LocalDateTime.parse(formatter.formatCellValue(cell).trim());
			if (type == LocalDate.class) {
				return dateTime.toLocalDate();
			}
			return (type == Instant.class) ? dateTime.toInstant(ZoneOffset.UTC) : dateTime;
		}
		String text = formatter.formatCellValue(cell).trim();
		if (text.isEmpty()) {
			return convert(null, type, formatter);
		}
		try {
			if (type == String.class) {
				return text;
			}
			if (type == Long.class || type == long.class) {
				return new BigDecimal(text).longValueExact();
			}
			if (type == Integer.class || type == int.class) {
				return new BigDecimal(text).intValueExact();
			}
			if (type == Double.class || type == double.class) {
				return Double.valueOf(text);
			}
			if (type == Boolean.class || type == boolean.class) {
				return Boolean.valueOf(text);
			}
			if (type.isEnum()) {
				return enumValue(type, text);
			}
		}
		catch (ArithmeticException | NumberFormatException ex) {
			throw new IllegalArgumentException("'" + text + "' is not a valid " + type.getSimpleName());
		}
		throw new IllegalArgumentException("unsupported field type " + type.getSimpleName());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object enumValue(Class<?> type, String text) {
		try {
			return Enum.valueOf((Class<Enum>) type, text.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("'" + text + "' must be one of " + Arrays.toString(type.getEnumConstants()));
		}
	}

	private static void writeCell(Cell cell, Object value, CellStyle dateStyle) {
		if (value == null) {
			cell.setBlank();
		}
		else if (value instanceof Number number) {
			cell.setCellValue(number.doubleValue());
		}
		else if (value instanceof Boolean bool) {
			cell.setCellValue(bool);
		}
		else if (value instanceof Instant instant) {
			// Instants are written (and read back) as UTC wall-clock time
			cell.setCellValue(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
			cell.setCellStyle(dateStyle);
		}
		else if (value instanceof LocalDateTime dateTime) {
			cell.setCellValue(dateTime);
			cell.setCellStyle(dateStyle);
		}
		else if (value instanceof LocalDate date) {
			cell.setCellValue(date);
			cell.setCellStyle(dateStyle);
		}
		else {
			cell.setCellValue(value.toString());
		}
	}

	private static CellStyle headerStyle(Workbook workbook) {
		Font font = workbook.createFont();
		font.setBold(true);
		CellStyle style = workbook.createCellStyle();
		style.setFont(font);
		return style;
	}

	private static boolean isBlank(Row row, DataFormatter formatter) {
		if (row == null) {
			return true;
		}
		for (Cell cell : row) {
			if (!formatter.formatCellValue(cell).isBlank()) {
				return false;
			}
		}
		return true;
	}

	private static String normalize(String header) {
		return header.trim().toLowerCase(Locale.ROOT);
	}

}
