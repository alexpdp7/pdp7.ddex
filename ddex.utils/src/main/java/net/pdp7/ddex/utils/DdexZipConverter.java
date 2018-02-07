package net.pdp7.ddex.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdexZipConverter {
	protected final Logger logger = LoggerFactory.getLogger(DdexZipConverter.class);
	protected final DdexToTableConverter tableConverter;
	protected final TableToExcel tableToExcel;

	public DdexZipConverter(DdexToTableConverter tableConverter, TableToExcel tableToExcel) {
		this.tableConverter = tableConverter;
		this.tableToExcel = tableToExcel;
	}

	public void convert(InputStream input, OutputStream out) {
		ZipInputStream zipStream = new ZipInputStream(input);
		List<Map<String, Object>> table = new ArrayList<>();
		try {
			ZipEntry entry;
			while ((entry = zipStream.getNextEntry()) != null) {
				try {
					table.addAll(tableConverter.convert(new CloseShieldInputStream(zipStream)).collect(Collectors.toList()));
				}
				catch(Exception e) {
					throw new DdexZipConverterException.GenericErrorOnEntry(entry, e);
				}
			}
			tableToExcel.convert(table, "release").write(out);
		} catch (IOException e) {
			throw new DdexZipConverterException.IOProblem(e);
		}
	}

	public static class DdexZipConverterException extends RuntimeException {
		protected DdexZipConverterException(String message, Throwable cause) {
			super(message, cause);
		}

		protected DdexZipConverterException(String message) {
			super(message);
		}

		public static class IOProblem extends DdexZipConverterException {
			protected IOProblem(IOException e) {
				super("IO Problem", e);
			}
		}

		public static class GenericErrorOnEntry extends DdexZipConverterException {
			protected final ZipEntry entry;

			protected GenericErrorOnEntry(ZipEntry entry, Exception e) {
				super("Error on entry " + entry, e);
				this.entry = entry;
			}
		}
	}
}
