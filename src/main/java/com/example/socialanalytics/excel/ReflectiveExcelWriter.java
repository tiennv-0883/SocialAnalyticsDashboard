package com.example.socialanalytics.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.util.StringUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

public final class ReflectiveExcelWriter<T> implements AutoCloseable {

	public record Column(String header, Method getter, boolean reference) {
	}

	private final List<Column> columns;

	private final SXSSFWorkbook workbook = new SXSSFWorkbook(200);

	private final Sheet sheet;

	private final CellStyle dateStyle;

	private int rowCount;

	public ReflectiveExcelWriter(Class<T> type, String sheetName) {
		this.columns = columns(type);
		this.sheet = workbook.createSheet(sheetName);
		this.dateStyle = workbook.createCellStyle();
		this.dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
		CellStyle headerStyle = ExcelMapper.headerStyle(workbook);
		Row header = sheet.createRow(0);
		for (int c = 0; c < columns.size(); c++) {
			Cell cell = header.createCell(c);
			cell.setCellValue(columns.get(c).header());
			cell.setCellStyle(headerStyle);
		}
	}

	public static List<Column> columns(Class<?> type) {
		Deque<Class<?>> hierarchy = new ArrayDeque<>();
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			hierarchy.push(current);
		}
		List<Column> columns = new ArrayList<>();
		for (Class<?> declaring : hierarchy) {
			for (Field field : declaring.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())
						|| field.isAnnotationPresent(Transient.class)) {
					continue;
				}
				Method getter = findGetter(type, field);
				if (getter == null) {
					continue;
				}
				if (isSimple(field.getType())) {
					columns.add(new Column(field.getName(), getter, false));
				}
				else if (field.getType().isAnnotationPresent(Entity.class)) {
					columns.add(new Column(field.getName() + "Id", getter, true));
				}
			}
		}
		if (columns.isEmpty()) {
			throw new IllegalArgumentException(type.getName() + " has no exportable properties");
		}
		return columns;
	}

	public void append(Collection<? extends T> items) {
		for (T item : items) {
			Row row = sheet.createRow(++rowCount);
			for (int c = 0; c < columns.size(); c++) {
				ExcelMapper.writeCell(row.createCell(c), value(columns.get(c), item), dateStyle);
			}
		}
	}

	public int rowCount() {
		return rowCount;
	}

	public byte[] toBytes() {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			workbook.write(out);
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new ExcelProcessingException("Cannot write Excel file", ex);
		}
	}

	@Override
	public void close() {
		try {
			workbook.close();
		}
		catch (IOException ex) {
			throw new ExcelProcessingException("Cannot close Excel workbook", ex);
		}
	}

	private static Object value(Column column, Object item) {
		Object value = invoke(column.getter(), item);
		if (value == null || !column.reference()) {
			return value;
		}
		try {
			return invoke(value.getClass().getMethod("getId"), value);
		}
		catch (NoSuchMethodException ex) {
			return value.toString();
		}
	}

	private static Object invoke(Method method, Object target) {
		try {
			return method.invoke(target);
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			throw new ExcelProcessingException("Cannot read '" + method.getName() + "' of " + target.getClass().getSimpleName(), ex);
		}
	}

	private static Method findGetter(Class<?> type, Field field) {
		String suffix = StringUtils.capitalize(field.getName());
		for (String prefix : List.of("get", "is")) {
			try {
				Method method = type.getMethod(prefix + suffix);
				if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
					return method;
				}
			}
			catch (NoSuchMethodException ex) {
				continue;
			}
		}
		return null;
	}

	private static boolean isSimple(Class<?> type) {
		return type.isPrimitive() || type.isEnum() || CharSequence.class.isAssignableFrom(type)
				|| Number.class.isAssignableFrom(type) || type == Boolean.class || Temporal.class.isAssignableFrom(type)
				|| type == BigDecimal.class;
	}

}
