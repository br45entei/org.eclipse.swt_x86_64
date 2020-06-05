package com.gmail.br45entei.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** @author Brian_Entei */
public class LogKeeper extends OutputStream {
	
	public static final void main(String[] args) {
		System.out.println(new LogKeeper(StandardCharsets.ISO_8859_1, true, 20000).print("Hello, world!").getText());
	}
	
	private volatile Charset charset;
	private final PrintStream pr;
	private volatile int textLengthLimit = 20000;
	
	//==============================================================================
	
	/** The buffer where data is stored. */
	protected volatile byte[] buf = new byte[32];
	
	/** The number of valid bytes in the buffer. */
	protected volatile int count;
	
	/** The index of the next character to read from the buffer.
	 * This value should always be nonnegative
	 * and not larger than the value of <code>count</code>.
	 * The next byte to be read from the input stream buffer
	 * will be <code>buf[pos]</code>. */
	protected volatile int pos;
	
	/** The currently marked position in the stream.
	 * ByteArrayInputStream objects are marked at position zero by
	 * default when constructed. They may be marked at another
	 * position within the buffer by the <code>mark()</code> method.
	 * The current buffer position is set to this point by the
	 * <code>reset()</code> method.
	 * <p>
	 * If no mark has been set, then the value of mark is the offset
	 * passed to the constructor (or 0 if the offset was not supplied). */
	protected volatile int mark = 0;
	
	//==============================================================================
	
	public LogKeeper(Charset charset, boolean autoFlush, int textLengthLimit) {
		this.charset = charset == null ? StandardCharsets.ISO_8859_1 : charset;
		try {
			this.pr = new PrintStream(this, autoFlush, this.charset.name()) {
				@Override
				public void flush() {
					super.flush();
					LogKeeper.this.updateText();
				}
			};
		} catch(UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
		this.textLengthLimit = textLengthLimit;
	}
	
	/** The maximum size of array to allocate.
	 * Some VMs reserve some header words in an array.
	 * Attempts to allocate larger arrays may result in
	 * OutOfMemoryError: Requested array size exceeds VM limit */
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	
	/** Increases the capacity to ensure that it can hold at least the
	 * number of elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity */
	private void grow(int minCapacity) {
		// overflow-conscious code
		int oldCapacity = this.buf.length;
		int newCapacity = oldCapacity << 1;
		if(newCapacity - minCapacity < 0) newCapacity = minCapacity;
		if(newCapacity - MAX_ARRAY_SIZE > 0) newCapacity = hugeCapacity(minCapacity);
		this.buf = Arrays.copyOf(this.buf, newCapacity);
	}
	
	private static int hugeCapacity(int minCapacity) {
		if(minCapacity < 0) // overflow
			throw new OutOfMemoryError();
		return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
	}
	
	/** Increases the capacity if necessary to ensure that it can hold
	 * at least the number of elements specified by the minimum
	 * capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 * @throws OutOfMemoryError if {@code minCapacity < 0}. This is
	 *             interpreted as a request for the unsatisfiably large capacity
	 *             {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}. */
	private void ensureCapacity(int minCapacity) {
		// overflow-conscious code
		if(minCapacity - this.buf.length > 0) {
			this.grow(minCapacity);
		}
	}
	
	protected synchronized String updateText() {
		final int textLengthLimit = this.textLengthLimit;
		final Charset charset = this.charset;
		
		String text = new String(this.buf, 0, this.count, charset);
		if(text.length() > textLengthLimit) {
			text = text.substring(text.length() - textLengthLimit, text.length());
			this.buf = text.getBytes(charset);
			this.count = this.buf.length;
		}
		return text;
	}
	
	public synchronized byte[] getData(boolean reset) {
		byte[] data = Arrays.copyOf(this.buf, this.count);
		if(reset) {
			this.count = 0;
		}
		return data;
	}
	
	/** Clears the contents of this LogKeeper's buffer.
	 * 
	 * @return This LogKeeper */
	public synchronized LogKeeper clear() {
		this.pr.flush();
		this.count = this.mark = this.pos = 0;
		return this;
	}
	
	public synchronized LogKeeper setData(byte[] data, int off, int len) throws IndexOutOfBoundsException {
		this.clear();
		this.write(data, 0, len);
		return this;
	}
	
	public synchronized LogKeeper setData(byte[] data) {
		return this.setData(data, 0, data.length);
	}
	
	/** @return The number of valid bytes in this LogKeeper's buffer. */
	public synchronized int size() {
		return this.count;
	}
	
	@Override
	public synchronized void write(int b) {
		this.ensureCapacity(this.count + 1);
		this.buf[this.count] = (byte) b;
		this.count += 1;
	}
	
	@Override
	public synchronized void write(byte[] b) {
		this.write(b, 0, b.length);
	}
	
	@Override
	public synchronized void write(byte[] b, int off, int len) throws IndexOutOfBoundsException {
		if((off < 0) || (off > b.length) || (len < 0) ||//
				((off + len) - b.length > 0)) {//
			throw new IndexOutOfBoundsException();
		}
		this.ensureCapacity(this.count + len);
		System.arraycopy(b, off, this.buf, this.count, len);
		this.count += len;
	}
	
	private volatile boolean flushDebounce = false;
	
	@Override
	public synchronized void flush() {
		if(this.flushDebounce) {
			return;
		}
		this.flushDebounce = true;
		try {
			this.pr.flush();
		} finally {
			this.flushDebounce = false;
		}
	}
	
	/** Does nothing. */
	@Override
	public void close() {
	}
	
	public synchronized String getText(boolean reset) {
		return this.updateText();//new String(this.buf, 0, this.count, this.charset);
	}
	
	public synchronized String getText() {
		return this.getText(false);
	}
	
	public synchronized LogKeeper setText(String text) {
		text = text == null ? "" : text;
		return this.setData(text.getBytes(this.charset));
	}
	
	//====================================================================================================================================================
	
	/** Prints a boolean value. The string produced by <code>{@link
	 * java.lang.String#valueOf(boolean)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param b The <code>boolean</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper print(boolean b) {
		this.pr.print(b);
		return this;
	}
	
	/** Prints a character. The character is translated into one or more bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param c The <code>char</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper print(char c) {
		this.pr.print(c);
		return this;
	}
	
	/** Prints an integer. The string produced by <code>{@link
	 * java.lang.String#valueOf(int)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param i The <code>int</code> to be printed.
	 * @return This LogKeeper
	 * @see java.lang.Integer#toString(int) */
	public synchronized LogKeeper print(int i) {
		this.pr.print(i);
		return this;
	}
	
	/** Prints a long integer. The string produced by <code>{@link
	 * java.lang.String#valueOf(long)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param l The <code>long</code> to be printed.
	 * @return This LogKeeper
	 * @see java.lang.Long#toString(long) */
	public synchronized LogKeeper print(long l) {
		this.pr.print(l);
		return this;
	}
	
	/** Prints a floating-point number. The string produced by <code>{@link
	 * java.lang.String#valueOf(float)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param f The <code>float</code> to be printed.
	 * @return This LogKeeper
	 * @see java.lang.Float#toString(float) */
	public synchronized LogKeeper print(float f) {
		this.pr.print(f);
		return this;
	}
	
	/** Prints a double-precision floating-point number. The string produced by
	 * <code>{@link java.lang.String#valueOf(double)}</code> is translated into
	 * bytes according to the platform's default character encoding, and these
	 * bytes are written in exactly the manner of the <code>{@link
	 * #write(int)}</code> method.
	 *
	 * @param d The <code>double</code> to be printed.
	 * @return This LogKeeper
	 * @see java.lang.Double#toString(double) */
	public synchronized LogKeeper print(double d) {
		this.pr.print(d);
		return this;
	}
	
	/** Prints an array of characters. The characters are converted into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param s The array of chars to be printed.
	 * @return This LogKeeper
	 *
	 * @throws NullPointerException If <code>s</code> is <code>null</code> */
	public synchronized LogKeeper print(char s[]) {
		this.pr.print(s);
		return this;
	}
	
	/** Prints a string. If the argument is <code>null</code> then the string
	 * <code>"null"</code> is printed. Otherwise, the string's characters are
	 * converted into bytes according to the platform's default character
	 * encoding, and these bytes are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param s The <code>String</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper print(String s) {
		this.pr.print(s);
		return this;
	}
	
	/** Prints an object. The string produced by the <code>{@link
	 * java.lang.String#valueOf(Object)}</code> method is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param obj The <code>Object</code> to be printed.
	 * @return This LogKeeper
	 * @see java.lang.Object#toString() */
	public synchronized LogKeeper print(Object obj) {
		this.pr.print(obj);
		return this;
	}
	
	/* Methods that do terminate lines */
	
	/** Terminates the current line by writing the line separator string. The
	 * line separator string is defined by the system property
	 * <code>line.separator</code>, and is not necessarily a single newline
	 * character (<code>'\n'</code>).
	 * 
	 * @return This LogKeeper */
	public synchronized LogKeeper println() {
		this.pr.println();
		return this;
	}
	
	/** Prints a boolean and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(boolean)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param b The <code>boolean</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(boolean b) {
		this.pr.println(b);
		return this;
	}
	
	/** Prints a character and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(char)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param c The <code>char</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(char c) {
		this.pr.println(c);
		return this;
	}
	
	/** Prints an integer and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(int)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param i The <code>int</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(int i) {
		this.pr.println(i);
		return this;
	}
	
	/** Prints a long and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(long)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param l a The <code>long</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(long l) {
		this.pr.println(l);
		return this;
	}
	
	/** Prints a float and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(float)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param f The <code>float</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(float f) {
		this.pr.println(f);
		return this;
	}
	
	/** Prints a double and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(double)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param d The <code>double</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(double d) {
		this.pr.println(d);
		return this;
	}
	
	/** Prints an array of characters and then terminates the line. This method
	 * behaves as though it invokes <code>{@link #print(char[])}</code> and
	 * then <code>{@link #println()}</code>.
	 *
	 * @param s an array of chars to print.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(char s[]) {
		this.pr.println(s);
		return this;
	}
	
	/** Prints a String and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(String)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param s The <code>String</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(String s) {
		this.pr.println(s);
		return this;
	}
	
	/** Prints an Object and then terminates the line. This method calls
	 * at first String.valueOf(x) to get the printed object's string value,
	 * then behaves as
	 * though it invokes <code>{@link #print(String)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param obj The <code>Object</code> to be printed.
	 * @return This LogKeeper */
	public synchronized LogKeeper println(Object obj) {
		this.pr.println(obj);
		return this;
	}
	
	/** A convenience method to write a formatted string to this output stream
	 * using the specified format string and arguments.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.printf(format,
	 * args)</tt> behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.format(format, args)
	 * </pre>
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>
	 * 			
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 * 			
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 * 			
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 * 			
	 * @return This output stream
	 * 			
	 * @since 1.5 */
	public synchronized PrintStream printf(String format, Object... args) {
		return this.pr.printf(format, args);
	}
	
	/** A convenience method to write a formatted string to this output stream
	 * using the specified format string and arguments.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.printf(l, format,
	 * args)</tt> behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.format(l, format, args)
	 * </pre>
	 *
	 * @param l
	 *            The {@linkplain java.util.Locale locale} to apply during
	 *            formatting. If <tt>l</tt> is <tt>null</tt> then no
	 *            localization
	 *            is applied.
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>
	 * 			
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 * 			
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 * 			
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 * 			
	 * @return This output stream
	 * 			
	 * @since 1.5 */
	public PrintStream printf(Locale l, String format, Object... args) {
		return this.pr.printf(l, format, args);
	}
	
	/** Writes a formatted string to this output stream using the specified
	 * format string and arguments.
	 *
	 * <p>
	 * The locale always used is the one returned by {@link
	 * java.util.Locale#getDefault() Locale.getDefault()}, regardless of any
	 * previous invocations of other formatting methods on this object.
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>
	 * 			
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 * 			
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 * 			
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 * 			
	 * @return This output stream
	 * 			
	 * @since 1.5 */
	public PrintStream format(String format, Object... args) {
		return this.pr.format(format, args);
	}
	
	/** Writes a formatted string to this output stream using the specified
	 * format string and arguments.
	 *
	 * @param l
	 *            The {@linkplain java.util.Locale locale} to apply during
	 *            formatting. If <tt>l</tt> is <tt>null</tt> then no
	 *            localization
	 *            is applied.
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>
	 * 			
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 * 			
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 * 			
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 * 			
	 * @return This output stream
	 * 			
	 * @since 1.5 */
	public PrintStream format(Locale l, String format, Object... args) {
		return this.pr.format(l, format, args);
	}
	
	/** Appends the specified character sequence to this output stream.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.append(csq)</tt>
	 * behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.print(csq.toString())
	 * </pre>
	 *
	 * <p>
	 * Depending on the specification of <tt>toString</tt> for the
	 * character sequence <tt>csq</tt>, the entire sequence may not be
	 * appended. For instance, invoking then <tt>toString</tt> method of a
	 * character buffer will return a subsequence whose content depends upon
	 * the buffer's position and limit.
	 *
	 * @param csq
	 *            The character sequence to append. If <tt>csq</tt> is
	 *            <tt>null</tt>, then the four characters <tt>"null"</tt> are
	 *            appended to this output stream.
	 * 			
	 * @return This output stream
	 * 			
	 * @since 1.5 */
	public PrintStream append(CharSequence csq) {
		return this.pr.append(csq);
	}
	
	/** Appends a subsequence of the specified character sequence to this output
	 * stream.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.append(csq, start,
	 * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in
	 * exactly the same way as the invocation
	 *
	 * <pre>
	 * out.print(csq.subSequence(start, end).toString())
	 * </pre>
	 *
	 * @param csq
	 *            The character sequence from which a subsequence will be
	 *            appended. If <tt>csq</tt> is <tt>null</tt>, then characters
	 *            will be appended as if <tt>csq</tt> contained the four
	 *            characters <tt>"null"</tt>.
	 * 			
	 * @param start
	 *            The index of the first character in the subsequence
	 * 			
	 * @param end
	 *            The index of the character following the last character in the
	 *            subsequence
	 *
	 * @return This output stream
	 *
	 * @throws IndexOutOfBoundsException
	 *             If <tt>start</tt> or <tt>end</tt> are negative,
	 *             <tt>start</tt>
	 *             is greater than <tt>end</tt>, or <tt>end</tt> is greater than
	 *             <tt>csq.length()</tt>
	 *
	 * @since 1.5 */
	public PrintStream append(CharSequence csq, int start, int end) {
		return this.pr.append(csq, start, end);
	}
	
	/** Appends the specified character to this output stream.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.append(c)</tt>
	 * behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.print(c)
	 * </pre>
	 *
	 * @param c
	 *            The 16-bit character to append
	 * 			
	 * @return This output stream
	 * 			
	 * @since 1.5 */
	public PrintStream append(char c) {
		return this.pr.append(c);
	}
	
	//====================================================================================================================================================
	
	/** Prints the given throwable and its backtrace to this LogKeeper's
	 * internal {@link PrintStream}
	 * 
	 * @param ex The {@link Throwable} to print
	 * @return This LogKeeper */
	public LogKeeper printStackTrace(Throwable ex) {
		ex.printStackTrace(this.pr);
		return this;
	}
	
	//====================================================================================================================================================
	
	/** Reads the next byte of data from this input stream. The value
	 * byte is returned as an <code>int</code> in the range
	 * <code>0</code> to <code>255</code>. If no byte is available
	 * because the end of the stream has been reached, the value
	 * <code>-1</code> is returned.
	 * <p>
	 * This <code>read</code> method
	 * cannot block.
	 *
	 * @return the next byte of data, or <code>-1</code> if the end of the
	 *         stream has been reached. */
	public synchronized int read() {
		return (this.pos < this.count) ? (this.buf[this.pos++] & 0xff) : -1;
	}
	
	public synchronized int read(byte[] b) {
		return this.read(b, 0, b.length);
	}
	
	/** Reads up to <code>len</code> bytes of data into an array of bytes
	 * from this input stream.
	 * If <code>pos</code> equals <code>count</code>,
	 * then <code>-1</code> is returned to indicate
	 * end of file. Otherwise, the number <code>k</code>
	 * of bytes read is equal to the smaller of
	 * <code>len</code> and <code>count-pos</code>.
	 * If <code>k</code> is positive, then bytes
	 * <code>buf[pos]</code> through <code>buf[pos+k-1]</code>
	 * are copied into <code>b[off]</code> through
	 * <code>b[off+k-1]</code> in the manner performed
	 * by <code>System.arraycopy</code>. The
	 * value <code>k</code> is added into <code>pos</code>
	 * and <code>k</code> is returned.
	 * <p>
	 * This <code>read</code> method cannot block.
	 *
	 * @param b the buffer into which the data is read.
	 * @param off the start offset in the destination array <code>b</code>
	 * @param len the maximum number of bytes read.
	 * @return the total number of bytes read into the buffer, or
	 *         <code>-1</code> if there is no more data because the end of
	 *         the stream has been reached.
	 * @exception NullPointerException If <code>b</code> is <code>null</code>.
	 * @exception IndexOutOfBoundsException If <code>off</code> is negative,
	 *                <code>len</code> is negative, or <code>len</code> is
	 *                greater than
	 *                <code>b.length - off</code> */
	public synchronized int read(byte b[], int off, int len) {
		if(b == null) {
			throw new NullPointerException();
		} else if(off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		}
		
		if(this.pos >= this.count) {
			return -1;
		}
		
		int avail = this.count - this.pos;
		if(len > avail) {
			len = avail;
		}
		if(len <= 0) {
			return 0;
		}
		System.arraycopy(this.buf, this.pos, b, off, len);
		this.pos += len;
		return len;
	}
	
	/** Skips <code>n</code> bytes of input from this input stream. Fewer
	 * bytes might be skipped if the end of the input stream is reached.
	 * The actual number <code>k</code>
	 * of bytes to be skipped is equal to the smaller
	 * of <code>n</code> and <code>count-pos</code>.
	 * The value <code>k</code> is added into <code>pos</code>
	 * and <code>k</code> is returned.
	 *
	 * @param n the number of bytes to be skipped.
	 * @return the actual number of bytes skipped. */
	public synchronized long skip(long n) {
		long k = this.count - this.pos;
		if(n < k) {
			k = n < 0 ? 0 : n;
		}
		
		this.pos += k;
		return k;
	}
	
	/** Sets the buffer to the given position for reading.
	 * 
	 * @param pos The position of the read buffer to set
	 * @return This LogKeeper
	 * @throws IndexOutOfBoundsException If <code>pos</code> is either negative
	 *             or greater than {@link #size()} */
	public synchronized LogKeeper seek(int pos) throws IndexOutOfBoundsException {
		if(pos < 0 || pos >= this.count) {
			throw new IndexOutOfBoundsException();
		}
		this.pos = pos;
		return this;
	}
	
	/** Returns the number of remaining bytes that can be read (or skipped over)
	 * from this input stream.
	 * <p>
	 * The value returned is <code>count&nbsp;- pos</code>,
	 * which is the number of bytes remaining to be read from the input buffer.
	 *
	 * @return the number of remaining bytes that can be read (or skipped
	 *         over) from this input stream without blocking. */
	public synchronized int available() {
		return this.count - this.pos;
	}
	
	/** Tests if this <code>InputStream</code> supports mark/reset. The
	 * <code>markSupported</code> method of <code>LogKeeper</code>
	 * always returns <code>true</code>.
	 * 
	 * @return True */
	public boolean markSupported() {
		return true;
	}
	
	/** Set the current marked position in the stream.
	 * LogKeeper objects are marked at position zero by
	 * default when constructed. They may be marked at another
	 * position within the buffer by this method.
	 * <p>
	 * If no mark has been set, then the value of the mark is 0.
	 *
	 * <p>
	 * Note: The <code>readAheadLimit</code> for this class
	 * has no meaning. */
	public void mark(int readAheadLimit) {
		this.mark = this.pos;
	}
	
	/** Returns the buffer to the beginning for reading.
	 * 
	 * @return This LogKeeper */
	public synchronized LogKeeper rewind() {
		this.pos = 0;
		return this;
	}
	
	public synchronized LogKeeper markBeginning() {
		this.mark = 0;
		return this;
	}
	
	/** Resets the buffer to the marked position. The marked position
	 * is 0 unless another position was marked. */
	public synchronized void reset() {
		this.pos = this.mark;
	}
	
	/** Returns a dummy InputStream whose methods are routed back to this
	 * LogKeeper.<br>
	 * Operations performed on the returned InputStream are performed on this
	 * LogKeeper, and are therefore visible to any further InputStreams returned
	 * by this method.<br>
	 * To reset the InputStream to the beginning, use {@link #seek(int)} with a
	 * position of 0.
	 * 
	 * @return This LogKeeper, represented as an InputStream */
	public InputStream asInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				return LogKeeper.this.read();
			}
			
			@Override
			public int read(byte[] b) throws IOException {
				return LogKeeper.this.read(b);
			}
			
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return LogKeeper.this.read(b, off, len);
			}
			
			@Override
			public long skip(long n) throws IOException {
				return LogKeeper.this.skip(n);
			}
			
			@Override
			public int available() throws IOException {
				return LogKeeper.this.available();
			}
			
			@Override
			public boolean markSupported() {
				return LogKeeper.this.markSupported();
			}
			
			@Override
			public synchronized void mark(int readlimit) {
				LogKeeper.this.mark(readlimit);
			}
			
			@Override
			public synchronized void reset() throws IOException {
				LogKeeper.this.reset();
			}
			
			@Override
			public void close() {
				LogKeeper.this.pos = LogKeeper.this.count;
			}
		};
	}
	
	//====================================================================================================================================================
	
}
