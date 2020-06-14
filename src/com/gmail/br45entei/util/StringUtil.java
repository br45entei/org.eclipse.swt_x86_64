package com.gmail.br45entei.util;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.exception.ConnectionTimeoutException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public final strictfp class StringUtil {
	
	@SuppressWarnings("unchecked")
	public static final <T> T[] clean(T[] a) {
		List<T> list = new ArrayList<>(Arrays.asList(a));
		list.removeAll(Collections.singleton(null));
		return list.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), list.size()));
	}
	
	public static final String newline = System.getProperty("line.separator");
	
	public static final DecimalFormat decimal = new DecimalFormat("#0.00");
	
	public static final String urlPathToFileName(String path) {
		String fileName = StringUtil.decodeHTML(path);
		fileName = (fileName.contains("#") && fileName.lastIndexOf(".") < fileName.lastIndexOf("#")) ? fileName.substring(0, fileName.lastIndexOf("#")) : fileName;
		fileName = fileName.contains("?") ? fileName.substring(0, fileName.indexOf("?")) : fileName;
		return StringUtil.makeStringFilesystemSafe(FilenameUtils.getName(fileName.endsWith("/") && (fileName.indexOf("/") != fileName.lastIndexOf("/")) ? fileName.substring(0, fileName.length() - 1) : fileName));
	}
	
	public static final String undoFilesystemSafeReplaced(String s) {
		return s.replace(";", ":").replace("''", "\"").replace("[ast]", "*").replace("[pipe]", "|").replace("&lt;", "<").replace("&gt;", ">");
	}
	
	public static final String makeStringFilesystemSafeReplaced(String s) {
		int len = s.length();
		StringBuilder sb = new StringBuilder(len);
		for(int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			if(ch < 0x10 || ch >= 0x7F) {
				sb.append('0');
				continue;
			}
			if(ch == ':') {
				sb.append(";");
			} else if(ch == '"') {
				sb.append("''");
			} else if(ch == '*') {
				sb.append("[ast]");
			} else if(ch == '|') {
				sb.append("[pipe]");
			} else if(ch == '<') {
				sb.append("&lt;");
			} else if(ch == '>') {
				sb.append("&gt;");
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	public static final String makeStringFilesystemSafe(String s) {
		char escape = '%'; // ... or some other legal char.
		int len = s.length();
		StringBuilder sb = new StringBuilder(len);
		for(int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			if(ch < ' ' || ch >= 0x7F || ch == '/' || ch == '\\' || ch == '?' || ch == ':' || ch == '"' || ch == '*' || ch == '|' || ch == '<' || ch == '>' || (ch == '.' && i == 0) || ch == escape) {
				sb.append(escape);
				if(ch < 0x10) {
					sb.append('0');
				}
				sb.append(Integer.toHexString(ch));
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	static {
		decimal.setRoundingMode(RoundingMode.HALF_EVEN);
	}
	
	public static final byte[] readFile(File file) {
		if(file == null || !file.isFile()) {
			return null;
		}
		try(FileInputStream in = new FileInputStream(file)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = in.read()) != -1) {
				baos.write(read);
			}
			return baos.toByteArray();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static final String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int read;
		while((read = in.read()) != -1) {
			String s = new String(new byte[] {(byte) read});
			if(s.equals("\n")) {
				break;
			}
			baos.write(read);
		}
		if(baos.size() == 0 && read == -1) {
			return null;
		}
		String rtrn = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		return rtrn.endsWith("\r") ? rtrn.substring(0, rtrn.length() - 1) : rtrn;
	}
	
	public static final String replaceAllExceptFirstIn(String str, String target, String replacement) {
		if(str == null || str.isEmpty() || target == null) {
			return str;
		}
		int index = str.indexOf(target);
		if(index == -1 || index == str.length() - 1) {
			return str;
		}
		//System.err.println("index: " + index + "; str.length(): " + str.length());
		String prefix = str.substring(0, index + 1);//should include target
		//System.err.println("prefix: " + prefix);
		String suffix = str.substring(index + 1);
		//System.err.println("suffix: " + suffix + "; suffix replaced: " + suffix.replace(target, replacement));
		return prefix + suffix.replaceAll(Pattern.quote(target), Matcher.quoteReplacement(replacement == null ? "" : replacement));
	}
	
	public static final String readLine(InputStream in, long timeout) throws ConnectionTimeoutException, IOException {
		final Property<String> data = new Property<>("Data");
		final Property<IOException> exception = new Property<>("IOException");
		Thread readThread = new Thread(new Runnable() {
			@Override
			public final void run() {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					int read;
					while((read = in.read()) != -1) {
						String s = new String(new byte[] {(byte) read});
						if(s.equals("\n")) {
							break;
						}
						baos.write(read);
					}
					if(baos.size() == 0 && read == -1) {
						data.setValue(null);
						return;
					}
					String rtrn = new String(baos.toByteArray(), StandardCharsets.UTF_8);
					data.setValue(rtrn.endsWith("\r") ? rtrn.substring(0, rtrn.length() - 1) : rtrn);
				} catch(IOException e) {
					exception.setValue(e);
				}
			}
		});
		readThread.setDaemon(true);
		readThread.start();
		final long startTime = System.currentTimeMillis();
		if(!readThread.isAlive()) {
			while(!readThread.isAlive()) {
				Functions.sleep(1L);
			}
		}
		boolean timedout = false;
		long elapsedTime = 0L;
		while(readThread.isAlive()) {
			if(exception.getValue() != null) {
				break;
			}
			elapsedTime = System.currentTimeMillis() - startTime;
			if(elapsedTime >= timeout) {
				timedout = true;
				readThread.interrupt();
				break;
			}
			Functions.sleep(1L);
		}
		if(timedout) {
			String time = StringUtil.getElapsedTime(Math.max(elapsedTime, timeout), false);
			if(time.startsWith("0:0")) {
				time = time.substring("0:0".length());
			}
			throw new ConnectionTimeoutException("Connection timed out after " + time + " second" + (Math.max(elapsedTime, timeout) == 1000L ? "" : "s") + ".");
		}
		if(!timedout && exception.getValue() != null) {
			throw exception.getValue();
		}
		return data.getValue();
	}
	
	/** @return Whether or not a 64 bit system was detected */
	public static boolean isJvm64bit() {
		for(String s : new String[] {"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"}) {
			String s1 = System.getProperty(s);
			if((s1 != null) && s1.contains("64")) {
				return true;
			}
		}
		return false;
	}
	
	/** Enum class differentiating types of operating systems
	 * 
	 * @author Brian_Entei */
	public static enum EnumOS {
		/** Linux or other similar Unix-type operating systems */
		LINUX,
		/** Salaries operating systems */
		SOLARIS,
		/** Windows operating systems */
		WINDOWS,
		/** Mac/OSX */
		OSX,
		/** An unknown operating system */
		UNKNOWN;
	}
	
	/** @return The type of operating system that java is currently running
	 *         on */
	public static EnumOS getOSType() {
		String s = System.getProperty("os.name").toLowerCase();
		return s.contains("win") ? EnumOS.WINDOWS : (s.contains("mac") ? EnumOS.OSX : (s.contains("solaris") ? EnumOS.SOLARIS : (s.contains("sunos") ? EnumOS.SOLARIS : (s.contains("linux") ? EnumOS.LINUX : (s.contains("unix") ? EnumOS.LINUX : EnumOS.UNKNOWN)))));
	}
	
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Random random = new Random();
	
	/** @param string The String to hash
	 * @return The resulting hash
	 * @see <a
	 *      href=
	 *      "http://stackoverflow.com/a/1660613/2398263">stackoverflow.com</a> */
	public static final long hash(String string) {
		long h = 1125899906842597L; // prime
		int len = string.length();
		
		for(int i = 0; i < len; i++) {
			h = 31 * h + string.charAt(i);
		}
		return h;
	}
	
	public static final int getRandomIntBetween(int min, int max) {
		return random.nextInt(max - min) + min;
	}
	
	public static final int getSecureRandomIntBetween(int min, int max) {
		return secureRandom.nextInt(max - min) + min;
	}
	
	public static final boolean getSecureRandomBoolean() {
		return getSecureRandomIntBetween(0, 2) == 1;
	}
	
	public static final long shuffle(List<?> list) {
		long rtrn = secureRandom.nextLong();
		Collections.shuffle(list, new Random(rtrn));
		return rtrn;
	}
	
	public static final void shuffle(List<?> list, long seed) {
		Collections.shuffle(list, new Random(seed));
	}
	
	public static final String cacheValidatorTimePattern = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";
	public static final Locale cacheValidatorTimeLocale = Locale.US;
	public static final TimeZone cacheValidatorTimeFormat = TimeZone.getTimeZone("GMT");
	
	public static final String generateRandomChars(int length) {
		return generateRandomChars(length, true);
	}
	
	public static final String generateRandomChars(int length, boolean alphanumeric) {
		final int upperCharMin = 'A' + 0;//'A' is 65.
		final int upperCharMax = 'Z' + 0;//'Z' is 90.
		final int lowerCharMin = 'a' + 0;//'a' is 97.
		final int lowerCharMax = 'z' + 0;//'z' is 122.
		final int symMin = alphanumeric ? 48 : 32;//32 is space, everything below is system characters etc. 48 is '0', 57 is '9'.
		final int symMax = alphanumeric ? 57 : upperCharMin - 1;//this is 64.
		StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i < length; i++) {
			boolean letterOrSymbol = getSecureRandomBoolean();
			boolean lowerCase = letterOrSymbol ? getSecureRandomBoolean() : false;
			sb.append((char) getSecureRandomIntBetween(letterOrSymbol ? (lowerCase ? lowerCharMin : upperCharMin) : symMin, letterOrSymbol ? (lowerCase ? lowerCharMax : upperCharMax) : symMax));
		}
		return sb.toString();
	}
	
	public static final String nextSessionId() {
		return new BigInteger(130, secureRandom).toString(32);
	}
	
	public static final boolean isFileSystemSafe(String fileName) {
		try {
			Paths.get(new File(System.getProperty("user.dir") + File.separator + fileName).toURI());
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	public static final void main(String[] args) {
		/*String test = "<html>\r\n"//
				+ "\t<head>\r\n"//
				+ "\t\t<title>Hai ther!</title>\r\n"//
				+ "\t</head>\r\n"//
				+ "\t<body>\r\n"//
				+ "\t\t<string>This is a webpage! Do you like it?</string>\r\n"//
				+ "\t</body>\r\n"//
				+ "</html>";
		
		System.out.println(test.length());
		System.out.println("\r\n===\r\n");
		try {
			System.out.println(compressString(test, "UTF-8").length);
			System.out.println(compressString("1234567890123456789012345678901234", "UTF-8").length);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		try {
			File out = new File(System.getProperty("user.dir") + File.separatorChar + "temp-" + (new Random()).nextLong() + ".txt");
			PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"), true);
			pr.println("Hello, world!\r\nHow are you?");
			pr.flush();
			pr.close();
			System.out.println("Resulting charset: \"" + getDetectedEncoding(out) + "\"");
			out.deleteOnExit();
		} catch(Throwable e) {
			e.printStackTrace();
		}
		*/
		//long millis = (4 * MILLENNIUM) + (2 * DAY) + (17 * HOUR) + (56 * MINUTE) + (49 * SECOND) + (0 * MILLISECOND);
		//long millis = (4 * MILLENNIUM) + (2 * DAY) + (0 * HOUR) + (0 * MINUTE) + (4 * SECOND) + (0 * MILLISECOND);
		//long millis = -((4 * MILLENNIUM) + (364 * DAY) + (0 * HOUR) + (0 * MINUTE) + (0 * SECOND) + (0 * MILLISECOND));
		//long millis = -1L + YEAR;
		long millis = 245981L;//0L;
		System.out.println(StringUtil.getElapsedTime(millis));
		DisposableByteArrayInputStream in = new DisposableByteArrayInputStream("Hello,\r\nWorld!\r\nThis\r\nString\r\nHas\r\nMany\r\nLines!".getBytes(StandardCharsets.UTF_8));
		try {
			long startTime = System.currentTimeMillis();
			String line = readLine(in);
			System.out.println(getElapsedTime(System.currentTimeMillis() - startTime, true) + ": " + line);
			startTime = System.currentTimeMillis();
			line = readLine(in, 2000);
			System.out.println(getElapsedTime(System.currentTimeMillis() - startTime, true) + ": " + line);
		} catch(IOException e) {
			e.printStackTrace();
		}
		in.close();
		System.out.println("\r\n\r\n\r\nTest: " + StringUtil.replaceAllExceptFirstIn("1021.", ".", "_"));
		System.out.println("\r\nTest_2: " + StringUtil.replaceAllExceptFirstIn("s p", " ", null));
		String check = StringUtil.generateRandomChars(6, false);
		System.out.println("generated random string with specifyable length: " + check + "\r\nlength: " + check.length());
		System.out.println("\r\n\r\nReplaced ip \"107.219.113.9:54687.txt\": " + makeStringFilesystemSafeReplaced("107.219.113.9:54687.txt"));
	}
	
	public static final int getLengthOfLongestLineInStr(String str) {
		int count = 0;
		if(str == null) {
			return -1;
		}
		if(str.contains("\n")) {
			for(String s : str.split(Pattern.quote("\n"))) {
				final int length = s.length();
				if(length > count) {
					count = length;
				}
			}
		}
		return count + 1;
	}
	
	public static final String getSpecificLineInStr(String str, int line) {
		String[] split = str.split(Pattern.quote("\n"));
		if(split.length > line) {
			return split[line];
		}
		return null;
	}
	
	public static final int getNumOfLinesInStr(String str) {
		int count = 0;
		if(str == null) {
			return -1;
		}
		if(str.contains("\n")) {
			count = str.split("\\n").length;
		}
		return count + 1;
	}
	
	public static final String requestArgumentsToString(HashMap<String, String> requestArguments, String... argumentsToIgnore) {
		String rtrn = "?";
		final HashMap<String, String> reqArgs = new HashMap<>(requestArguments);
		for(Entry<String, String> entry : requestArguments.entrySet()) {
			if(entry.getKey() != null) {
				for(String argToIgnore : argumentsToIgnore) {
					if(entry.getKey().equals(argToIgnore)) {
						reqArgs.remove(entry.getKey());
					}
				}
			}
		}
		boolean addedAnyArguments = false;
		for(Entry<String, String> entry : reqArgs.entrySet()) {
			rtrn += (addedAnyArguments ? "&" : "") + entry.getKey() + "=" + entry.getValue();
			addedAnyArguments = true;
		}
		return rtrn.equals("?") ? "" : rtrn;
	}
	
	/** @param getTimeOnly Whether or not time should be included but not date
	 *            as
	 *            well
	 * @param fileSystemSafe Whether or not the returned string will be used in
	 *            the making of a folder or file
	 * @param milliseconds Whether or not the milliseconds should be included
	 * @return The resulting string */
	public static String getSystemTime(boolean getTimeOnly, boolean fileSystemSafe, boolean milliseconds) {
		return new SimpleDateFormat(getTimeOnly ? (fileSystemSafe ? "HH.mm.ss" + (milliseconds ? ".SSS" : "") : "HH:mm:ss" + (milliseconds ? ":SSS" : "")) : (fileSystemSafe ? "MM-dd-yyyy_HH.mm.ss" + (milliseconds ? ".SSS" : "") : "MM/dd/yyyy_HH:mm:ss" + (milliseconds ? ":SSS" : ""))).format(new Date());
	}
	
	/** @param str The string to limit
	 * @param limit The number of chars to limit to
	 * @return The limited string */
	public static String limitStringToNumOfChars(String str, int limit) {
		return(str != null ? (str.length() >= 1 ? (str.substring(0, (str.length() >= limit ? limit : str.length()))) : "") : "");
	}
	
	/** @param stackTraceElements The elements to convert
	 * @return The resulting string */
	public static final String stackTraceElementsToStr(StackTraceElement[] stackTraceElements) {
		return stackTraceElementsToStr(stackTraceElements, "\r\n");
	}
	
	/** @param stackTraceElements The elements to convert
	 * @param lineSeparator The line separator to use
	 * @return The resulting string */
	public static final String stackTraceElementsToStr(StackTraceElement[] stackTraceElements, String lineSeparator) {
		String str = "";
		if(stackTraceElements != null) {
			for(StackTraceElement stackTrace : stackTraceElements) {
				str += (!stackTrace.toString().startsWith("Caused By") ? "     at " : "") + stackTrace.toString() + lineSeparator;
			}
		}
		return str;
	}
	
	/** @param stackTraceElements The elements to convert
	 * @return The resulting string */
	public static final String stackTraceCausedByElementsOnlyToStr(StackTraceElement[] stackTraceElements) {
		String str = "";
		if(stackTraceElements != null) {
			for(StackTraceElement stackTrace : stackTraceElements) {
				str += (!stackTrace.toString().startsWith("Caused By") ? "" : stackTrace.toString() + "\r\n");
			}
		}
		return str;
	}
	
	/** @param stackTraceElements The elements to convert
	 * @return The resulting string */
	public static final String stackTraceCausedByElementsOnlyToStr(StackTraceElement[] stackTraceElements, String lineSeparator) {
		String str = "";
		if(stackTraceElements != null) {
			for(StackTraceElement stackTrace : stackTraceElements) {
				str += (!stackTrace.toString().startsWith("Caused By") ? "" : stackTrace.toString() + lineSeparator);
			}
		}
		return str;
	}
	
	/** @param e The {@link Throwable} to convert
	 * @return The resulting String */
	public static final String throwableToStrNoStackTraces(Throwable e) {
		if(e == null) {
			return "null";
		}
		String str = e.getClass().getName() + ": ";
		if((e.getMessage() != null) && !e.getMessage().isEmpty()) {
			str += e.getMessage() + "\r\n";
		} else {
			str += "\r\n";
		}
		str += stackTraceCausedByElementsOnlyToStr(e.getStackTrace());
		if(e.getCause() != null) {
			str += "Caused by:\r\n" + throwableToStrNoStackTraces(e.getCause());
		}
		return str;
	}
	
	/** @param e The {@link Throwable} to convert
	 * @return The resulting String */
	public static final String throwableToStrNoStackTraces(Throwable e, String lineSeparator) {
		if(e == null) {
			return "null";
		}
		String str = e.getClass().getName() + ": ";
		if((e.getMessage() != null) && !e.getMessage().isEmpty()) {
			str += e.getMessage() + lineSeparator;
		} else {
			str += lineSeparator;
		}
		str += stackTraceCausedByElementsOnlyToStr(e.getStackTrace(), lineSeparator);
		if(e.getCause() != null) {
			str += "Caused by:" + lineSeparator + throwableToStrNoStackTraces(e.getCause(), lineSeparator);
		}
		return str;
	}
	
	/** @param e The {@link Throwable} to convert
	 * @return The resulting String */
	public static final String throwableToStr(Throwable e) {
		return throwableToStr(e, "\r\n");
	}
	
	/** @param e The {@link Throwable} to convert
	 * @param lineSeparator The line separator to use
	 * @return The resulting String */
	public static final String throwableToStr(Throwable e, String lineSeparator) {
		if(e == null) {
			return "null";
		}
		String str = e.getClass().getName() + ": ";
		if((e.getMessage() != null) && !e.getMessage().isEmpty()) {
			str += e.getMessage() + lineSeparator;
		} else {
			str += lineSeparator;
		}
		str += stackTraceElementsToStr(e.getStackTrace(), lineSeparator);
		if(e.getCause() != null) {
			str += "Caused by:" + lineSeparator + throwableToStr(e.getCause());
		}
		return str;
	}
	
	/** @param arrays The array or arrays to check
	 * @return The Class type of the array */
	@SuppressWarnings("unchecked")
	public static final <T> Class<T> getClassFromArray(T[]... arrays) {
		Class<T> clazz = null;
		for(T[] array : arrays) {
			if(clazz != null) {
				break;
			}
			if(array != null) {
				for(T element : array) {
					if(element != null) {
						clazz = (Class<T>) element.getClass();
						break;
					}
				}
			}
		}
		return clazz;
	}
	
	/** @param <T> The arrays' class
	 * @param arrays The String arrays to combine
	 * @return The combined arrays as one String[] array, or null if none of the
	 *         arrays contained any instances or if null was given */
	@SuppressWarnings("unchecked")
	public static final <T> T[] combine(T[]... arrays) {
		if(arrays == null) {
			return null;
		}
		Class<T> clazz = null;
		int newLength = 0;
		for(T[] array : arrays) {
			newLength += array != null ? array.length : 0;
			if(clazz == null && array != null) {
				for(T element : array) {
					if(element != null) {
						clazz = (Class<T>) element.getClass();
						break;
					}
				}
			}
		}
		if(clazz == null) {
			return null;
		}
		T[] rtrn = (T[]) Array.newInstance(clazz, newLength);//new T[newLength];
		int index = 0;
		for(T[] array : arrays) {
			if(array != null) {
				for(T element : array) {
					rtrn[index++] = element;
				}
			}
		}
		return rtrn;
	}
	
	/** @param array The list to read from
	 * @param c The character to use as a separator
	 * @return The resulting string */
	public static final String stringArrayToString(char c, Collection<String> array) {
		return stringArrayToString(String.valueOf(c), array);
	}
	
	/** @param array The list to read from
	 * @param c The string to use as a separator
	 * @return The resulting string */
	public static final String stringArrayToString(String c, Collection<String> array) {
		if(array == null) {
			return "null";
		}
		String rtrn = "";
		int index = 0;
		for(String element : array) {
			rtrn += element + ((++index) == array.size() ? "" : c);
		}
		return rtrn.trim();
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @return The resulting string */
	public static final String stringArrayToString(char c, String... array) {
		if(array == null) {
			return "null";
		}
		String rtrn = "";
		for(String element : array) {
			rtrn += element + c;
		}
		return rtrn.length() >= 2 ? rtrn.substring(0, rtrn.length() - 1) : rtrn;
	}
	
	/** @param array The String[] array to convert
	 * @param c The separator character to use
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, char c) {
		return stringArrayToString(array, c, 0);
	}
	
	/** @param array The String[] array to convert
	 * @param c The separator character to use
	 * @param startIndex The index to start at
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, char c, int startIndex) {
		return stringArrayToString(array, String.valueOf(c), startIndex);
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, String c) {
		return stringArrayToString(array, c, 0);
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @param startIndex The index to start at
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, String c, int startIndex) {
		if(array == null || startIndex >= array.length) {
			return "null";
		}
		String rtrn = "";
		int i = 0;
		for(String element : array) {
			if(i >= startIndex) {
				rtrn += element + (i + 1 == array.length ? "" : c);
			}
			i++;
		}
		return rtrn;
	}
	
	/** @param array The String[] array to convert
	 * @param c The separator character to use
	 * @param startIndex The index to start at
	 * @param endIndex The index to stop short at
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, char c, int startIndex, int endIndex) {
		return stringArrayToString(array, c + "", startIndex, endIndex);
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @param startIndex The index to start at
	 * @param endIndex The index to stop short at
	 * @return The resulting string. If startIndex is greater than or equal to
	 *         the array's size, endIndex is greater than the array's size,
	 *         startIndex is greater than or equal to endIndex, and/or either
	 *         startIndex or endIndex are negative, "null" is returned. */
	public static final String stringArrayToString(String[] array, String c, int startIndex, int endIndex) {
		if(array == null || startIndex >= array.length || endIndex > array.length || startIndex >= endIndex || startIndex < 0 || endIndex < 0) {
			return "null";
		}
		String rtrn = "";
		int i = 0;
		for(String element : array) {
			if(i >= startIndex && i < endIndex) {
				rtrn += element + (i + 1 == endIndex ? "" : c);
			}
			i++;
		}
		return rtrn;
	}
	
	public static final String fixConcatenatedString(String str, String delimiter, String unwantedPrefix, String unwantedSuffix, String unwantedContents, String unwantedContentsReplacement) {
		if(str != null && delimiter != null && unwantedPrefix != null && unwantedSuffix != null && unwantedContents != null) {
			if(unwantedContentsReplacement == null) {
				unwantedContentsReplacement = "";
			}
			String result = "";
			final String[] split = str.split(Pattern.quote(delimiter));
			for(int i = 0; i < split.length; i++) {
				String s = split[i];
				if(s.isEmpty()) {
					continue;
				}
				if(s.length() > 1) {
					if(s.startsWith(unwantedPrefix) && s.length() > unwantedPrefix.length()) {
						s = s.substring(unwantedPrefix.length());
					} else if(s.equals(unwantedPrefix)) {
						s = "";
					}
					if(s.endsWith(unwantedSuffix) && s.length() > unwantedSuffix.length()) {
						s = s.substring(0, s.length() - unwantedSuffix.length());
					} else if(s.equals(unwantedSuffix)) {
						s = "";
					}
				} else {
					if(s.equals(unwantedPrefix)) {
						s = "";
					}
					if(s.equals(unwantedSuffix)) {
						s = "";
					}
				}
				if(s.contains(unwantedContents)) {
					s = s.replaceAll(Pattern.quote(unwantedContents), Matcher.quoteReplacement(unwantedContentsReplacement));
					if(s.replaceAll(Pattern.quote(delimiter), Matcher.quoteReplacement("")).isEmpty()) {
						s = "";
					}
				}
				if(s.isEmpty()) {
					continue;
				}
				result += s + ((i + 1) == split.length ? "" : delimiter);
			}
			return result;
		}
		return null;//merciless "you give me null I give you null" behaviour is intended. No lazy coding!!!1
	}
	
	/** Resizes an array. Can be used to copy arrays as well.
	 * 
	 * @param clazz The class type of the array
	 * @param array The array to resize
	 * @param startIndex The index at which the resizing will start
	 * @param endIndex The index at which the resizing will stop short at
	 * @return The resized array */
	public static final <T> T[] resizeArray(Class<T> clazz, T[] array, int startIndex, int endIndex) {
		if(array == null || startIndex >= array.length || endIndex > array.length || startIndex >= endIndex || startIndex < 0 || endIndex < 0) {
			return array;
		}
		@SuppressWarnings("unchecked")
		T[] rtrn = (T[]) Array.newInstance(clazz, (endIndex - startIndex));
		int i = 0;
		for(int j = startIndex; j < endIndex; j++) {
			if(j >= array.length) {
				break;
			}
			rtrn[i] = array[j];
			i++;
		}
		return rtrn;
	}
	
	public static final boolean isStrUUID(String uuid) {
		if(uuid == null) {
			return false;
		}
		try {
			return UUID.fromString(uuid) != null;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param str The string to convert
	 * @return The resulting long value
	 * @throws NumberFormatException Thrown if the given String does not
	 *             represent a valid long value */
	public static final Long getLongFromStr(String str) throws NumberFormatException {
		return Long.valueOf(str);
	}
	
	/** @param time The time to convert
	 * @return The result */
	public static final String getTime(long time) {
		return getTime(time, true);
	}
	
	/** @param time The time to convert
	 * @param getTimeOnly Whether or not the result should exclude the date
	 * @return The result */
	public static String getTime(long time, boolean getTimeOnly) {
		return getTime(time, getTimeOnly, false);
	}
	
	/** @param time The time to convert
	 * @param getTimeOnly Whether or not the result should exclude the date
	 * @param fileSystemSafe Whether or not the date should be file system
	 *            safe(does nothing if the date is excluded)
	 * @return The result */
	public static final String getTime(long time, boolean getTimeOnly, boolean fileSystemSafe) {
		return getTime(time, getTimeOnly, fileSystemSafe, false);
	}
	
	/** @param time The time to convert
	 * @param getTimeOnly Whether or not the result should exclude the date
	 * @param fileSystemSafe Whether or not the date should be file system
	 *            safe(does nothing if the date is excluded)
	 * @param showMilliseconds Whether or not the results should include
	 *            milliseconds
	 * @return The result */
	public static String getTime(long time, boolean getTimeOnly, boolean fileSystemSafe, boolean showMilliseconds) {
		return new SimpleDateFormat(getTimeOnly ? (fileSystemSafe ? "HH.mm.ss" + (showMilliseconds ? ".SSS" : "") : "HH:mm:ss" + (showMilliseconds ? ":SSS" : "")) : (fileSystemSafe ? "MM-dd-yyyy_HH.mm.ss" + (showMilliseconds ? ".SSS" : "") : "MM/dd/yyyy_HH:mm:ss" + (showMilliseconds ? ":SSS" : ""))).format(new Date(time));
		//final String millis = showMilliseconds ? (fileSystemSafe ? "." : ":") + "SSS" : "";
		//return new SimpleDateFormat(getTimeOnly ? "HH-mm-ss" + millis : fileSystemSafe ? "MM-dd-yyyy_HH.mm.ss" + millis : "MM/dd/yyyy'\t'h:mm:ss" + millis + " a").format(new Date(time));
	}
	
	public static final String getCacheTimeFormatWith(long millis, String pattern, Locale locale, TimeZone zone) {
		SimpleDateFormat rtrn = new SimpleDateFormat(pattern, locale);
		rtrn.setTimeZone(zone);
		return rtrn.format(new Date(millis));
	}
	
	public static final SimpleDateFormat getCacheValidatorTimeFormat() {
		SimpleDateFormat rtrn = new SimpleDateFormat(cacheValidatorTimePattern, cacheValidatorTimeLocale);
		rtrn.setTimeZone(cacheValidatorTimeFormat);
		return rtrn;
	}
	
	/** @param millis The amount of milliseconds that have passed since
	 *            midnight,
	 *            January 1, 1970
	 * @return The resulting string in cache format */
	public static final String getCacheTime(long millis) {
		return getCacheValidatorTimeFormat().format(new Date(millis));
	}
	
	/** @return The current time in cache format */
	public static final String getCurrentCacheTime() {
		return getCacheValidatorTimeFormat().format(new Date());
	}
	
	public static final byte[] compressString(String str, Charset charset) {
		return compressString(str, charset != null ? charset.name() : null);
	}
	
	public static final byte[] compressString(String str, String charsetName) {
		if(str == null || str.length() <= 33) {
			try {
				return str == null ? new byte[0] : str.getBytes(charsetName);
			} catch(UnsupportedEncodingException ignored) {
				return str.getBytes();
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(str.length());
		try(GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(str.getBytes(charsetName));
			gzip.flush();
		} catch(Throwable ignored) {
		}
		return out.toByteArray();
	}
	
	public static final String decompressString(byte[] data) {
		return readGZippedStringFromBuffer(data);
	}
	
	public static final String readGZippedStringFromBuffer(byte[] buf) {
		String rtrn = null;
		if(buf != null) {
			try(GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(buf))) {
				rtrn = "";
				byte[] b = new byte[4096];
				int read;
				while((read = in.read(b, 0, b.length)) != -1) {
					rtrn += new String(b, 0, read, StandardCharsets.UTF_8);
				}
			} catch(IOException e) {
				return rtrn;
			}
		}
		return rtrn;
	}
	
	public static final byte[] removeTrailingNewLineCharsFrom(byte[] data) {
		//long startTime = System.currentTimeMillis();
		String check = new String(data, StandardCharsets.UTF_8);
		while(check.endsWith("\n") || check.endsWith("\r")) {
			//if(check.endsWith("\n") || check.endsWith("\r")) {
			//check = check.trim();
			check = check.substring(0, check.length() - 1);
			data = check.getBytes(StandardCharsets.UTF_8);
			//}
			check = new String(data, StandardCharsets.UTF_8);
		}
		//System.err.println(StringUtil.getElapsedTimeTraditional(System.currentTimeMillis() - startTime, true));//0:00:001 ms!
		return data;
	}
	
	public static final boolean containsIgnoreCase(String str, String... list) {
		if(str != null && list != null && list.length != 0) {
			for(String s : Arrays.asList(list)) {
				if(s != null && s.equalsIgnoreCase(str)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static final boolean containsIgnoreCase(Collection<String> list, String str) {
		if(str != null && list != null && !list.isEmpty()) {
			for(String s : new ArrayList<>(list)) {
				if(str.equalsIgnoreCase(s)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String getStringInList(Set<String> list, String str) {
		if(list != null && !list.isEmpty()) {
			for(String s : new ArrayList<>(list)) {
				if(s != null && s.equalsIgnoreCase(str)) {
					return s;
				}
			}
		}
		return null;
	}
	
	public static final String getUrlLinkFromFile(File urlFile) throws IOException {
		String line = "";
		if(urlFile == null || !urlFile.exists() || urlFile.isDirectory()) {
			return line;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(urlFile));
			while(br.ready()) {
				line = br.readLine();
				if(line.startsWith("URL=")) {
					line = line.substring(4);
					break;
				}
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		return line;
	}
	
	public static final String makeFilePathURLSafe(String filePath) {
		return filePath.replace("%", "%25").replace("+", "%2b").replace("#", "%23").replace(" ", "%20");
	}
	
	public static final String encodeHTML(String str) {
		str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25").replace("+", "%2b").replace("#", "%23").replace(" ", "%20");
		StringEscapeUtils.escapeHtml3("");
		return StringEscapeUtils.escapeHtml4(str).replace(" ", "%20");//.replaceAll("(?i)&mdash;", "�").replaceAll("(?i)&ndash;", "�").replaceAll("(?i)&micro;", "?").replaceAll("(?i)&omega;", "?");
	}
	
	public static final String decodeHTML(String s) {
		final String str = s;
		s = s.replaceAll("%(?![0-9a-fA-F]{2})", "%25").replaceAll("\\+", "%2B");
		try {
			s = URLDecoder.decode(s, "UTF-8");
			return StringEscapeUtils.unescapeHtml4(s);
		} catch(Throwable ignored) {
			return StringEscapeUtils.unescapeHtml4(str);
		}
	}
	
	@SuppressWarnings("unused")
	private static final boolean isCharIllegal(String str) {
		return(str.equals("\n") || str.equals("\r") || str.equals("\t") || str.equals("\0") || str.equals("\f") || str.equals("`") || str.equals("'") || str.equals("?") || str.equals("*") || str.equals("<") || str.equals(">") || str.equals("|") || str.equals("\""));
	}
	
	public static final String longToString(long l) {
		return longToString(Long.valueOf(l));
	}
	
	public static final String longToString(Long l) {
		return doubleToString(l.doubleValue());
	}
	
	public static final String doubleToString(double d) {
		return doubleToString(Double.valueOf(d));
	}
	
	public static final String doubleToString(Double d) {
		if(d == null) {
			return null;
		}
		if(d.isNaN() || d.isInfinite()) {
			return d.toString();
		}
		// pre java 8, a value of 0 would yield "0.0" below
		if(d.doubleValue() == 0) {
			return "0";
		}
		return new BigDecimal(d.toString()).stripTrailingZeros().toPlainString();
	}
	
	public static final String intToString(int i) {
		return intToString(Integer.valueOf(i));
	}
	
	public static final String intToString(Integer i) {
		return floatToString(i.floatValue());
	}
	
	public static final String floatToString(float f) {
		return floatToString(Float.valueOf(f));
	}
	
	public static final String floatToString(Float f) {
		if(f == null) {
			return null;
		}
		if(f.isNaN() || f.isInfinite()) {
			return f.toString();
		}
		// pre java 8, a value of 0 would yield "0.0" below
		if(f.floatValue() == 0) {
			return "0";
		}
		return new BigDecimal(f.toString()).stripTrailingZeros().toPlainString();
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid long value */
	public static final boolean isStrLong(String value) {
		try {
			Long.parseLong(value);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid int value */
	public static final boolean isStrInt(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid double value */
	public static final boolean isStrDouble(String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid float value */
	public static final boolean isStrFloat(String value) {
		try {
			Float.parseFloat(value);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** Compare Strings in alphabetical order */
	public static final Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
		@Override
		public int compare(String str1, String str2) {
			if(str1 == null || str2 == null) {
				return Integer.MAX_VALUE;
			}
			int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
			if(res == 0) {
				res = str1.compareTo(str2);
			}
			return res;
		}
	};
	
	//==========================
	protected static final long MILLISECOND = 1L;
	protected static final long SECOND = 1000L;
	protected static final long MINUTE = 60 * SECOND;
	protected static final long HOUR = 60 * MINUTE;
	protected static final long DAY = 24 * HOUR;
	protected static final long WEEK = 7 * DAY;
	protected static final long YEAR = 365 * DAY; //(long) (365.2395 * DAY);
	protected static final long DECADE = 10 * YEAR;
	protected static final long CENTURY = 10 * DECADE;
	protected static final long MILLENNIUM = 10 * CENTURY;
	
	/** @param millis The time in milliseconds
	 * @return The time, in String format */
	public static final String getElapsedTime(long millis) {
		return getElapsedTime(millis, true);
	}
	
	public static final String getElapsedTime(long millis, boolean bukkitFormat) {
		return bukkitFormat ? getElapsedTimeInBukkitFormat(millis) : getElapsedTimeTraditional(millis, false);
	}
	
	/** @param millis The time in milliseconds
	 * @return The time, in String format */
	public static String getElapsedTimeInBukkitFormat(long millis) {
		//final long total = millis;
		boolean negative = millis < 0;
		if(negative) {
			millis = Math.abs(millis);
		}
		String rtrn = "";
		if(millis >= MILLENNIUM) {
			long millenniums = millis / MILLENNIUM;
			millis %= MILLENNIUM;
			rtrn += millenniums + " Millennium" + (millenniums == 1 ? "" : "s") + " ";
		}
		if(millis >= CENTURY) {
			long centuries = millis / CENTURY;
			millis %= CENTURY;
			rtrn += centuries + " Centur" + (centuries == 1 ? "y" : "ies") + " ";
		}
		if(millis >= YEAR) {
			long years = millis / YEAR;
			millis %= YEAR;
			rtrn += years + " Year" + (years == 1 ? "" : "s") + " ";
		}
		if(millis >= WEEK) {
			long weeks = millis / WEEK;
			millis %= WEEK;
			rtrn += weeks + " Week" + (weeks == 1 ? "" : "s") + " ";
		}
		if(millis >= DAY) {
			long days = millis / DAY;
			millis %= DAY;
			rtrn += days + " Day" + (days == 1 ? "" : "s") + " and ";
		}
		long hours = 0L;
		if(millis >= HOUR) {
			hours = millis / HOUR;
			millis %= HOUR;
		}
		long minutes = 0L;
		if(millis >= MINUTE) {
			minutes = millis / MINUTE;
			millis %= MINUTE;
		}
		long seconds = 0L;
		if(millis >= SECOND) {
			seconds = millis / SECOND;
			millis %= SECOND;
		}
		final String hourStr = (hours == 0 ? "" : hours + "h ");
		final String minuteStr = (minutes == 0 ? "" : minutes + "m ");
		final String secondStr = (seconds == 0 ? "" : seconds + "s");
		rtrn += hourStr + minuteStr + secondStr;
		rtrn += (negative ? " Remaining" : "");
		rtrn = rtrn.replace("  ", " ").trim();
		return rtrn.trim().isEmpty() ? "0s" : rtrn;
	}
	
	/** @param millis The time in milliseconds
	 * @param showMilliseconds Whether or not to show milliseconds(...:000)
	 * @return The time, in String format */
	public static String getElapsedTimeTraditional(long millis, boolean showMilliseconds) {
		final long total = millis;
		boolean negative = millis < 0;
		if(negative) {
			millis = Math.abs(millis);
		}
		String rtrn = "";
		if(millis >= MILLENNIUM) {
			long millenniums = millis / MILLENNIUM;
			millis %= MILLENNIUM;
			rtrn += millenniums + " Millennium" + (millenniums == 1 ? "" : "s") + " ";
		}
		if(millis >= CENTURY) {
			long centuries = millis / CENTURY;
			millis %= CENTURY;
			rtrn += centuries + " Centur" + (centuries == 1 ? "y" : "ies") + " ";
		}
		if(millis >= YEAR) {
			long years = millis / YEAR;
			millis %= YEAR;
			rtrn += years + " Year" + (years == 1 ? "" : "s") + " ";
		}
		if(millis >= WEEK) {
			long weeks = millis / WEEK;
			millis %= WEEK;
			rtrn += weeks + " Week" + (weeks == 1 ? "" : "s") + " ";
		}
		if(millis >= DAY) {
			long days = millis / DAY;
			millis %= DAY;
			rtrn += days + " Day" + (days == 1 ? "" : "s") + " and ";
		}
		long hours = 0L;
		if(millis >= HOUR) {
			hours = millis / HOUR;
			millis %= HOUR;
		}
		long minutes = 0L;
		if(millis >= MINUTE) {
			minutes = millis / MINUTE;
			millis %= MINUTE;
		}
		long seconds = 0L;
		if(millis >= SECOND) {
			seconds = millis / SECOND;
			millis %= SECOND;
		}
		long milliseconds = 0L;
		if(millis >= MILLISECOND && showMilliseconds) {
			milliseconds = millis / MILLISECOND;
			//millis %= milliseconds;
		} else if(!showMilliseconds) {
			long check = millis / MILLISECOND;
			if(check >= 500L && check <= 999L) {
				return getElapsedTime((total + 1000L) - check, false);
			}
		}
		final String hourStr = (hours == 0 ? "" : hours + ":");
		final String minuteStr = (minutes == 0 ? (hours != 0 ? "00:" : (seconds != 0 || milliseconds != 0 ? "0:" : "")) : (minutes < 10 ? "0" : "") + minutes + ":");
		final String secondStr = (hours == 0 && minutes == 0 && seconds == 0 && milliseconds == 0 ? "" : (seconds < 10 ? "0" : "") + seconds);
		rtrn += hourStr + minuteStr + secondStr + (milliseconds != 0 ? ":" + (milliseconds < 100 ? (milliseconds < 10 ? "00" : "0") : "") + milliseconds : "");
		rtrn = rtrn.endsWith("and ") ? rtrn.substring(0, rtrn.length() - 4).trim() : rtrn;
		rtrn += (negative ? " Remaining" : "");
		rtrn = rtrn.replace("  ", " ").trim();
		return rtrn.trim().isEmpty() ? "0:00" + (showMilliseconds ? ":000" : "") : rtrn;
	}
	
	public static final String capitalizeFirstLetterOfEachWordIn(String str) {
		if(str.contains(" ")) {
			StringBuilder sb = new StringBuilder(str.length());
			int workingIndex = 0;
			while(true) {
				int nextIndex = str.substring(workingIndex).indexOf(" ");
				if(nextIndex <= workingIndex) {
					nextIndex = -1;
				}
				String current = capitalizeFirstLetter((nextIndex == -1 ? str.substring(workingIndex + 1) : str.substring(workingIndex, nextIndex)).toLowerCase());
				sb.append(current);
				sb.append(nextIndex == -1 ? "" : " ");
				workingIndex += current.length();
				if(nextIndex == -1) {
					break;
				}
			}
			return sb.toString();
		}
		return capitalizeFirstLetter(str.toLowerCase());
	}
	
	/** @param str The text to edit
	 * @return The given text with its first letter capitalized */
	public static final String capitalizeFirstLetter(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
	
	public static final <T> T findClosestMatch(Collection<T> collection, T target) {
		int distance = Integer.MAX_VALUE;
		T closest = null;
		for(T compareObject : collection) {
			int currentDistance = StringUtils.getLevenshteinDistance(compareObject.toString(), target.toString());
			if(currentDistance < distance) {
				distance = currentDistance;
				closest = compareObject;
			}
		}
		return closest;
	}
	
}
