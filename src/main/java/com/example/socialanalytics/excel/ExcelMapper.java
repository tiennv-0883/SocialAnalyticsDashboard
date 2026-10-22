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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.EmptyFileException;
import org.apache.poi.UnsupportedFileFormatException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

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
		return read(input, type, Integer.MAX_VALUE);
	}

	/**
	 * Reads the first sheet of an .xlsx file. Rejects the whole file (before building the
	 * workbook in memory) when it has more than {@code maxRows} data rows, and when a
	 * {@link ExcelColumn#required() required} column is missing from the header.
	 */
	public static <T> List<RowResult<T>> read(InputStream input, Class<T> type, int maxRows) {
		List<Field> fields = columnFields(type);
		Constructor<T> constructor = noArgConstructor(type);
		OPCPackage pkg = null;
		try {
			pkg = OPCPackage.open(input);
			ensureRowLimit(pkg, maxRows);
			Workbook workbook = new XSSFWorkbook(pkg);
			Sheet sheet = workbook.getSheetAt(0);
			Row header = sheet.getRow(sheet.getFirstRowNum());
			if (header == null) {
				throw new ExcelProcessingException("Excel file is empty");
			}
			Map<Field, Integer> columnIndexes = resolveColumns(header, fields);
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

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
							field.set(target, convert(cell, field.getType(), formatter, evaluator));
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
		catch (IOException | OpenXML4JException | SAXException | EmptyFileException | UnsupportedFileFormatException
				| POIXMLException ex) {
			throw new ExcelProcessingException("Cannot read Excel file: " + ex.getMessage(), ex);
		}
		finally {
			if (pkg != null) {
				// read-only use: discard instead of close(), which would try to save the package
				pkg.revert();
			}
		}
	}

	/**
	 * Counts rows of the first sheet with a streaming SAX parse and stops as soon as the
	 * limit is exceeded, so oversized files fail fast without loading the whole sheet.
	 */
	private static void ensureRowLimit(OPCPackage pkg, int maxRows)
			throws IOException, OpenXML4JException, SAXException {
		if (maxRows == Integer.MAX_VALUE) {
			return;
		}
		XSSFReader reader = new XSSFReader(pkg);
		XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
		if (!sheets.hasNext()) {
			throw new ExcelProcessingException("Excel file is empty");
		}
		long maxRowElements = (long) maxRows + 1; // + header row
		try (InputStream sheet = sheets.next()) {
			XMLReader parser = XMLHelper.newXMLReader();
			parser.setContentHandler(new DefaultHandler() {

				private long rows;

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					if ("row".equals(localName) && ++rows > maxRowElements) {
						throw new RowLimitExceededException();
					}
				}

			});
			parser.parse(new InputSource(sheet));
		}
		catch (RowLimitExceededException ex) {
			throw new ExcelProcessingException("Too many rows: at most " + maxRows + " data rows are allowed");
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Cannot create XML parser", ex);
		}
	}

	/** Used only to abort the SAX parse early. */
	private static final class RowLimitExceededException extends SAXException {

		private static final long serialVersionUID = 1L;

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
		List<String> expected = fields.stream().map(field -> field.getAnnotation(ExcelColumn.class).name()).toList();
		if (columnIndexes.isEmpty()) {
			throw new ExcelProcessingException("No known columns found in header row. Expected: " + expected);
		}
		List<String> missing = fields.stream()
			.filter(field -> field.getAnnotation(ExcelColumn.class).required() && !columnIndexes.containsKey(field))
			.map(field -> field.getAnnotation(ExcelColumn.class).name())
			.toList();
		if (!missing.isEmpty()) {
			throw new ExcelProcessingException("Missing required columns: " + missing + ". Expected: " + expected);
		}
		return columnIndexes;
	}

	private static Object convert(Cell cell, Class<?> type, DataFormatter formatter, FormulaEvaluator evaluator) {
		CellType cellType = (cell == null) ? CellType.BLANK : resultType(cell, evaluator);
		if (cellType == CellType.BLANK) {
			if (type.isPrimitive()) {
				throw new IllegalArgumentException("value is required");
			}
			return null;
		}
		if (type == Instant.class || type == LocalDateTime.class || type == LocalDate.class) {
			LocalDateTime dateTime = (cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
					? cell.getLocalDateTimeCellValue()
					: LocalDateTime.parse(formatter.formatCellValue(cell, evaluator).trim());
			if (type == LocalDate.class) {
				return dateTime.toLocalDate();
			}
			return (type == Instant.class) ? dateTime.toInstant(ZoneOffset.UTC) : dateTime;
		}
		String text = formatter.formatCellValue(cell, evaluator).trim();
		if (text.isEmpty()) {
			return convert(null, type, formatter, evaluator);
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

	private static CellType resultType(Cell cell, FormulaEvaluator evaluator) {
		if (cell.getCellType() != CellType.FORMULA) {
			return cell.getCellType();
		}
		CellType result;
		try {
			result = evaluator.evaluateFormulaCell(cell);
		}
		catch (RuntimeException ex) {
			throw new IllegalArgumentException("cannot evaluate formula '=" + cell.getCellFormula() + "'");
		}
		if (result == CellType.ERROR) {
			throw new IllegalArgumentException("formula '=" + cell.getCellFormula() + "' returns an error");
		}
		return result;
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

	static void writeCell(Cell cell, Object value, CellStyle dateStyle) {
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

	static CellStyle headerStyle(Workbook workbook) {
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
