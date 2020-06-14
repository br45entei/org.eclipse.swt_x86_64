package com.gmail.br45entei.swt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class Functions {
	
	private static final char[] ILLEGAL_CHARACTERS = {'\n', '\r', '\t', '\0', '\f', '`', '?', '*', '<', '>', '|', '\"'};
	private static final String[] ILLEGAL_CHARACTER_REPLACEMENTS = {"", "", "", "", "", "&#96;", "", "", "&lt;", "&gt;", "", "&quot;"};
	
	private static final SecureRandom random = new SecureRandom();
	private static final DecimalFormat decimal = new DecimalFormat("#0.00");
	
	static {
		Functions.decimal.setRoundingMode(RoundingMode.HALF_EVEN);
	}
	
	public static final String roundToStr(double d) {
		return decimal.format(d);
	}
	
	public static final double roundD(double d) {
		return Double.valueOf(decimal.format(d)).doubleValue();
	}
	
	public static final float roundF(double d) {
		return Double.valueOf(decimal.format(d)).floatValue();
	}
	
	public static final String nextSessionId() {
		return new BigInteger(130, random).toString(32);
	}
	
	/** @param str The string to convert
	 * @return The string with HTML characters converted into normal
	 *         characters */
	public static String htmlToText(String str) {
		String rtrn;
		try {
			rtrn = StringEscapeUtils.unescapeHtml4(URLDecoder.decode(str, "UTF-8"));
		} catch(UnsupportedEncodingException e) {
			rtrn = StringEscapeUtils.unescapeHtml4(str);
		}
		int i = 0;
		for(char illegalChar : ILLEGAL_CHARACTERS) {
			rtrn = rtrn.replace(illegalChar + "", ILLEGAL_CHARACTER_REPLACEMENTS[i]);
			i++;
		}
		return rtrn;
	}
	
	public static final void minimizeShell(Shell shell) {
		OS.SendMessage(shell.handle, OS.WM_SYSCOMMAND, OS.SC_MINIMIZE, 0);
		/*Windows only!*/OS.UpdateWindow(shell.handle);/**/
	}
	
	public static final void setBoundsFor(Control control, Rectangle bounds) {
		if(control == null || bounds == null) {
			return;
		}
		if(!control.getBounds().equals(bounds)) {
			control.setBounds(bounds);
		}
	}
	
	public static final void setLocationFor(Control control, Point loc) {
		if(control == null || loc == null) {
			return;
		}
		if(!control.getLocation().equals(loc)) {
			control.setLocation(loc);
		}
	}
	
	public static final void setSizeFor(Control control, Point size) {
		if(control == null || size == null) {
			return;
		}
		if(!control.getSize().equals(size)) {
			control.setSize(size);
		}
	}
	
	public static final void setSelectionFor(ProgressBar progressBar, int value) {
		if(progressBar == null) {
			return;
		}
		if(progressBar.getSelection() != value) {
			progressBar.setSelection(value);
		}
	}
	
	public static final void setSelectionFor(Button button, boolean value) {
		if(button == null) {
			return;
		}
		if(button.getSelection() != value) {
			button.setSelection(value);
		}
	}
	
	/** @param shell The shell to center */
	public static final void centerShellOnPrimaryMonitor(Shell shell) {
		Monitor primary = shell.getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		setLocationFor(shell, new Point(x, y));//shell.setLocation(x, y);
	}
	
	public static final void centerShell2OnShell1(Shell shell1, Shell shell2) {
		Rectangle bounds = shell1.getBounds();
		Rectangle rect = shell2.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		setLocationFor(shell2, new Point(x, y));//shell2.setLocation(x, y);
	}
	
	public static final Image[] getDefaultShellImages() {
		return new Image[] {SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-16x16.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-32x32.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-64x64.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-128x128.png")};
	}
	
	public static final void main(String[] args) {
		long test = 157483213;
		String humanReadable = humanReadableByteCount(test, true, 6);
		String result = new BigDecimal(fromHumanReadableByteCount(humanReadable, true)).toPlainString();
		System.out.println("test long: " + test + "; readable: " + humanReadable + "; converted back to double: " + result);
	}
	
	/** @param bytes The humanReadableByteCount generated from
	 *            {@link #humanReadableByteCount(long, boolean, int)}
	 * @param si Whether or not si was used
	 * @return The resulting double */
	public static double fromHumanReadableByteCount(String bytes, boolean si) {
		String[] split = bytes.split(Pattern.quote(" "));
		if(split.length != 2) {
			return -1.0D;
		}
		double b = Double.valueOf(split[0]).doubleValue();
		if(split[1].equalsIgnoreCase("b")) {
			return b;
		}
		final char pre = split[1].toUpperCase().charAt(0);
		double unit = si ? 10.00D : 10.24D;//si ? 1000 : 1024;
		int exp = 0;
		switch(pre) {
		case 'K':
			exp = 3;
			break;
		case 'M':
			exp = 6;
			break;
		case 'G':
			exp = 9;
			break;
		case 'T':
			exp = 12;
			break;
		case 'P':
			exp = 15;
			break;
		case 'E':
			exp = 18;
			break;
		default:
			break;
		}
		return(b * Math.pow(unit, exp));// / 1000000000000.0D;
	}
	
	/** @param bytes The amount of bytes
	 * @param si Whether or not the bytes are in SI format
	 * @return The readable string
	 * @author aioobe from <a href=
	 *         "http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java/3758880#3758880"
	 *         >stackoverflow.com</a>
	 * @param decimalPlaces The number of decimal places */
	public static String humanReadableByteCount(long bytes, boolean si, int decimalPlaces) {
		int unit = si ? 1000 : 1024;
		if(bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		String[] split = String.format("%." + decimalPlaces + "f %sB", new Double(bytes / Math.pow(unit, exp)), pre).split(Pattern.quote(" "));
		split[0] = split[0].indexOf(".") < 0 ? split[0] : split[0].replaceAll("0*$", "").replaceAll("\\.$", "");
		return split[0] + " " + split[1];
	}
	
	/** @param file
	 * @return
	 * @throws IOException */
	public static final OutputStream openOutputStream(File file) throws IOException {
		if(file.exists()) {
			if(file.isDirectory()) {
				throw new IOException("The file \"" + file.getAbsolutePath() + "\" is a directory, not a file.\r\nCannot write to folders.");
			}
			if(file.canWrite() == false) {
				throw new IOException("The file \"" + file.getAbsolutePath() + "\" could not be accessed!");
			}
		} else {
			File parent = file.getParentFile();
			if(parent != null) {
				if(!parent.mkdirs() && !parent.isDirectory()) {
					throw new IOException("Parent folder \"" + parent.getAbsolutePath() + "\" could not be created!");
				}
			}
		}
		return new FileOutputStream(file, false);
	}
	
	/** @param url The URL to get the file size of
	 * @return The file size, or -1 if unsuccessful. */
	public static final long getFileSize(URL url) {
		URLConnection conn = null;
		try {
			conn = url.openConnection();
			if(conn instanceof HttpURLConnection) {
				HttpURLConnection conn1 = (HttpURLConnection) conn;
				conn1.setRequestMethod("GET");
				conn1.getInputStream();
				return conn1.getContentLengthLong();
			}
			conn.getInputStream();
			return conn.getContentLengthLong();
		} catch(IOException e) {
			//e.printStackTrace();
			return -1L;
		} finally {
			if(conn != null) {
				if(conn instanceof HttpURLConnection) {
					((HttpURLConnection) conn).disconnect();
				} else {
					//XXX ?
				}
			}
		}
	}
	
	public static final boolean isStrUUID(String uuid) {
		try {
			UUID.fromString(uuid);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	public static final String getElementsFromStringArrayAtIndexesAsString(String[] array, int startIndex, int endIndex) {
		return getElementsFromStringArrayAtIndexesAsString(array, startIndex, endIndex, ' ');
	}
	
	public static final String getElementsFromStringArrayAtIndexesAsString(String[] array, int startIndex, int endIndex, char seperatorChar) {
		if(array == null || array.length == 0) {
			return null;
		}
		if(startIndex < 0 || startIndex > array.length || endIndex < 0 || endIndex > array.length) {
			return null;
		}
		String rtrn = "";
		if(startIndex > endIndex) {
			for(int i = startIndex; i < endIndex; i--) {
				rtrn += seperatorChar + array[i];
			}
		} else {
			for(int i = startIndex; i < endIndex; i++) {
				rtrn += seperatorChar + array[i];
			}
		}
		if(rtrn.startsWith(seperatorChar + "")) {
			rtrn = rtrn.substring(1);
		}
		return rtrn.trim();
	}
	
	public static final String getElementsFromStringArrayAtIndexAsString(String[] array, int index) {
		return getElementsFromStringArrayAtIndexAsString(array, index, ' ');
	}
	
	public static final String getElementsFromStringArrayAtIndexAsString(String[] array, int index, char seperatorChar) {
		if(array == null || index >= array.length) {
			return "";
		}
		String mkArgs = "";
		for(int i = index; i < array.length; i++) {
			mkArgs += array[i] + seperatorChar;
		}
		return mkArgs.trim();
	}
	
	public static final boolean doesArrayContainAnyNullObjects(Object[] array) {
		for(int i = 0; i < array.length; i++) {
			if(array[i] == null) {
				return true;
			}
		}
		return false;
	}
	
	public static final int getNextFreeIndexInArray(Object[] array) {
		if(array == null || !doesArrayContainAnyNullObjects(array)) {
			return -1;
		}
		for(int i = 0; i < array.length; i++) {
			if(array[i] == null) {
				return i;
			}
		}
		return -1;
	}
	
	public static final boolean isWindowsAeroActive() {
		return false;
	}
	
	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}
	
	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s);
	}
	
	public static final void sleep() {
		sleep(12.5D);//15L is actually better but running code in the thread causes time delay etc. so 12.5 is a nice compromise etc(20L is too slow and 10L is too fast).
	}
	
	public static final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();//(see http://stackoverflow.com/a/4906814/2398263 )
		}
	}
	
	public static final void sleep(double millis) {
		long wholeNum = (long) millis;
		int nanos = (int) Math.floor((millis - wholeNum) * 1000000);//(int) Math.floor((millis - wholeNum) / 1000000);
		try {
			Thread.sleep(wholeNum, nanos);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();//(see http://stackoverflow.com/a/4906814/2398263 )
		}
	}
	
	public static final String stackTraceElementsToStr(StackTraceElement[] stackTraceElements) {
		String str = "";
		for(StackTraceElement stackTrace : stackTraceElements) {
			str += (!stackTrace.toString().startsWith("Caused By") ? "     at " : "") + stackTrace.toString() + "\r\n";
		}
		return str;
	}
	
	public static String throwableToStr(Throwable t) {
		if(t == null) {
			return "null";
		}
		String str = t.getClass().getName() + ": ";
		if((t.getMessage() != null) && !t.getMessage().isEmpty()) {
			str += t.getMessage() + "\r\n";
		} else {
			str += "\r\n";
		}
		str += stackTraceElementsToStr(t.getStackTrace());
		if(t.getCause() != null) {
			str += "Caused by:\r\n" + throwableToStr(t.getCause());
		}
		return str;
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
		if(baos.size() == 0) {
			return null;
		}
		String rtrn = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		return rtrn.endsWith("\r") ? rtrn.substring(0, rtrn.length() - 1) : rtrn;
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
	
	public static final boolean equals(Color color1, Color color2) {
		if(color1 == null || color2 == null) {
			if(color1 == null && color2 == null) {
				return true;
			}
			return false;
		}
		return color1.getBlue() == color2.getBlue() && color1.getGreen() == color2.getGreen() && color1.getRed() == color2.getRed();
	}
	
	public static void overrideBackgroundColorFor(Control obj, Color backgroundColor) {
		obj.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				obj.setBackground(backgroundColor);//obj.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				org.eclipse.swt.graphics.Pattern pattern = new org.eclipse.swt.graphics.Pattern(event.gc.getDevice(), 0, 0, 0, 100, backgroundColor, 230, backgroundColor, 230);//org.eclipse.swt.graphics.Pattern pattern = new org.eclipse.swt.graphics.Pattern(event.gc.getDevice(), 0, 0, 0, 100, event.gc.getDevice().getSystemColor(SWT.COLOR_GRAY), 230, event.gc.getDevice().getSystemColor(SWT.COLOR_BLACK), 230);
				event.gc.setBackgroundPattern(pattern);
				event.gc.fillGradientRectangle(0, 0, obj.getBounds().width, obj.getBounds().height, true);
			}
		});
	}
	
	/** @param shell The shell whose text will be set
	 * @param text The text to set
	 * @return True if the text was changed */
	public static boolean setTextFor(Shell shell, String text) {
		if(shell != null && !shell.isDisposed() && text != null) {
			if(!text.equals(shell.getText())) {
				shell.setText(text);
				return true;
			}
		}
		return false;
	}
	
	/** @param label The label whose text will be set
	 * @param text The text to set
	 * @return True if the text was changed */
	public static boolean setTextFor(Label label, String text) {
		if(label != null && !label.isDisposed() && text != null) {
			if(!text.equals(label.getText())) {
				label.setText(text);
				return true;
			}
		}
		return false;
	}
	
	/** @param text The text whose text will be set
	 * @param txt The string of text to set
	 * @return True if the text's text was changed */
	public static boolean setTextFor(Text text, String txt) {
		if(text != null && !text.isDisposed() && txt != null) {
			if(!txt.equals(text.getText())) {
				text.setText(txt);
				return true;
			}
		}
		return false;
	}
	
	/** @param button The button whose text will be set
	 * @param text The text to set
	 * @return True if the text was changed */
	public static boolean setTextFor(Button button, String text) {
		if(button != null && !button.isDisposed() && text != null) {
			if(!text.equals(button.getText())) {
				button.setText(text);
				return true;
			}
		}
		return false;
	}
	
	/** @param control The control whose visible state will be set
	 * @param visible The visible state to set
	 * @return True if the control's visible state was changed */
	public static boolean setVisibleFor(Control control, boolean visible) {
		if(control != null && !control.isDisposed()) {
			if(control.isVisible() != visible) {
				control.setVisible(visible);
				return true;
			}
		}
		return false;
	}
	
	/** @param control The control whose enabled state will be set
	 * @param enabled The enable state to set
	 * @return True if the control's enabled state was changed */
	public static boolean setEnabledFor(Control control, boolean enabled) {
		if(control != null && !control.isDisposed()) {
			if(control.getEnabled() != enabled) {
				control.setEnabled(enabled);
				return true;
			}
		}
		return false;
	}
	
	/** @param shell The shell whose images will be set
	 * @param images The images to set
	 * @return True if the shell's images were changed */
	public static final boolean setShellImages(Shell shell, Image[] images) {
		if(shell == null || images == null || shell.isDisposed()) {
			return false;
		}
		if(!equals(shell.getImages(), images)) {
			if(images.length > 0) {
				shell.setImage(images[0]);
			}
			shell.setImages(images);
			return true;
		}
		return false;
	}
	
	public static final void toggleAlwaysOnTop(Shell shell, boolean isOnTop) {
		Point location = shell.getLocation();
		Point dimension = shell.getSize();
		OS.SetWindowPos(shell.handle, isOnTop ? OS.HWND_TOPMOST : OS.HWND_NOTOPMOST, location.x, location.y, dimension.x, dimension.y, 0);
	}
	
	/** @param images1 The first {@link Image}[] array
	 * @param images2 The second {@link Image}[] array
	 * @return Whether or not the arrays are identical */
	public static final boolean equals(Image[] images1, Image[] images2) {
		if(images1 == images2) {
			return true;
		}
		if(images1 != null && images2 != null && images1.length == images2.length) {
			for(int i = 0; i < images1.length; i++) {
				Image image1 = images1[i];
				Image image2 = images2[i];
				if(image1 == null && image2 == null) {
					continue;
				}
				if(image1 != null && image2 != null) {
					if(image1 != image2 && !image1.equals(image2)) {
						return false;
					}
				} else {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
}
