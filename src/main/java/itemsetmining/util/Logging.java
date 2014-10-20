package itemsetmining.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

public class Logging {

	/** Set up logging to console */
	public static void setUpConsoleLogger(final Logger logger,
			final Level logLevel) {
		LogManager.getLogManager().reset();
		logger.setLevel(logLevel);
		final Handler handler = setUpConsoleHandler();
		logger.addHandler(handler);
	}

	/** Set up logging to file */
	public static void setUpFileLogger(final Logger logger,
			final Level logLevel, final String path) {
		LogManager.getLogManager().reset();
		logger.setLevel(logLevel);
		final Handler handler = setUpFileHandler(path);
		logger.addHandler(handler);
	}

	/** Set up logging to console and file */
	public static void setUpConsoleAndFileLogger(final Logger logger,
			final Level logLevel, final String path) {
		LogManager.getLogManager().reset();
		logger.setLevel(logLevel);
		final Handler chandler = setUpConsoleHandler();
		final Handler fhandler = setUpFileHandler(path);
		logger.addHandler(chandler);
		logger.addHandler(fhandler);
	}

	/** Set the log file name */
	public static String getLogFileName(final boolean timeStampLog,
			final String logDir, final File dataset) {
		String timeStamp = "";
		if (timeStampLog)
			timeStamp = "-"
					+ new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss")
							.format(new Date());
		return logDir + FilenameUtils.getBaseName(dataset.getName())
				+ timeStamp + ".log";
	}

	/** Set up console handler */
	public static Handler setUpConsoleHandler() {
		final ConsoleHandler handler = new ConsoleHandler() {
			@Override
			protected void setOutputStream(final OutputStream out)
					throws SecurityException {
				super.setOutputStream(System.out);
			}
		};
		handler.setLevel(Level.ALL);
		final Formatter formatter = new Formatter() {
			@Override
			public String format(final LogRecord record) {
				return record.getMessage();
			}
		};
		handler.setFormatter(formatter);
		return handler;
	}

	/** Set up file handler */
	public static Handler setUpFileHandler(final String path) {
		FileHandler handler = null;
		try {
			handler = new FileHandler(path, 104857600, 1);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		handler.setLevel(Level.ALL);
		final Formatter formatter = new Formatter() {
			@Override
			public String format(final LogRecord record) {
				return record.getMessage();
			}
		};
		handler.setFormatter(formatter);
		return handler;
	}

	private Logging() {
	}

}
