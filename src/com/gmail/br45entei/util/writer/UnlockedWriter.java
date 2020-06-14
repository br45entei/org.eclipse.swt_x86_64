/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * Modified by Brian_Entei
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.gmail.br45entei.util.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/** @author Brian_Entei */
public abstract class UnlockedWriter extends Writer {
	
	/** @return The underlying OutputStream for this Writer */
	public abstract OutputStream getOutputStream();
	
	private final StreamEncoder se;
	
	protected void setSecondaryOut(OutputStream out) {
		this.se.setSecondaryOut(out);
	}
	
	protected void setTertiaryOut(OutputStream out) {
		this.se.setTertiaryOut(out);
	}
	
	/** Creates a new character-stream writer whose critical sections will
	 * synchronize on the given object.
	 *
	 * @param lock
	 *            Object to synchronize on */
	public UnlockedWriter(Object lock) {
		super(lock);
		this.se = null;
	}
	
	/** Creates an OutputStreamWriter that uses the named charset.
	 *
	 * @param out
	 *            An OutputStream
	 *
	 * @param charsetName
	 *            The name of a supported
	 *            {@link java.nio.charset.Charset charset}
	 *
	 * @exception UnsupportedEncodingException
	 *                If the named encoding is not supported */
	public UnlockedWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException {
		super(out);
		if(charsetName == null) throw new NullPointerException("charsetName");
		this.se = StreamEncoder.forOutputStreamWriter(out, this, charsetName);
	}
	
	/** Creates an OutputStreamWriter that uses the default character encoding.
	 *
	 * @param out An OutputStream */
	public UnlockedWriter(OutputStream out) {
		super(out);
		try {
			this.se = StreamEncoder.forOutputStreamWriter(out, this, (String) null);
		} catch(UnsupportedEncodingException e) {
			throw new Error(e);
		}
	}
	
	/** Creates an OutputStreamWriter that uses the given charset.
	 *
	 * @param out
	 *            An OutputStream
	 *
	 * @param cs
	 *            A charset
	 *
	 * @since 1.4
	 * @spec JSR-51 */
	public UnlockedWriter(OutputStream out, Charset cs) {
		super(out);
		if(cs == null) throw new NullPointerException("charset");
		this.se = StreamEncoder.forOutputStreamWriter(out, this, cs);
	}
	
	/** Creates an OutputStreamWriter that uses the given charset encoder.
	 *
	 * @param out
	 *            An OutputStream
	 *
	 * @param enc
	 *            A charset encoder
	 *
	 * @since 1.4
	 * @spec JSR-51 */
	public UnlockedWriter(OutputStream out, CharsetEncoder enc) {
		super(out);
		if(enc == null) throw new NullPointerException("charset encoder");
		this.se = StreamEncoder.forOutputStreamWriter(out, this, enc);
	}
	
	/** Returns the name of the character encoding being used by this stream.
	 *
	 * <p>
	 * If the encoding has an historical name then that name is returned;
	 * otherwise the encoding's canonical name is returned.
	 *
	 * <p>
	 * If this instance was created with the {@link
	 * #UnlockedWriter(OutputStream, String)} constructor then the returned
	 * name, being unique for the encoding, may differ from the name passed to
	 * the constructor. This method may return <tt>null</tt> if the stream has
	 * been closed.
	 * </p>
	 *
	 * @return The historical name of this encoding, or possibly
	 *         <code>null</code> if the stream has been closed
	 *
	 * @see java.nio.charset.Charset
	 *
	 * @revised 1.4
	 * @spec JSR-51 */
	public String getEncoding() {
		return this.se.getEncoding();
	}
	
	/** Writes a single character.
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void write(int c) throws IOException {
		this.se.write(c);
	}
	
	/** Writes the specified byte to this output stream. The general
	 * contract for <code>write</code> is that one byte is written
	 * to the output stream. The byte to be written is the eight
	 * low-order bits of the argument <code>b</code>. The 24
	 * high-order bits of <code>b</code> are ignored.
	 * <p>
	 * Subclasses of <code>OutputStream</code> must provide an
	 * implementation for this method.
	 *
	 * @param b the <code>byte</code>.
	 * @exception IOException if an I/O error occurs. In particular,
	 *                an <code>IOException</code> may be thrown if the
	 *                output stream has been closed. */
	public final void writeBytes(int b) throws IOException {
		this.getOutputStream().write(b);
	}
	
	/** Writes <code>len</code> bytes from the specified byte array
	 * starting at offset <code>off</code> to this output stream.
	 * The general contract for <code>write(b, off, len)</code> is that
	 * some of the bytes in the array <code>b</code> are written to the
	 * output stream in order; element <code>b[off]</code> is the first
	 * byte written and <code>b[off+len-1]</code> is the last byte written
	 * by this operation.
	 * <p>
	 * The <code>write</code> method of <code>OutputStream</code> calls
	 * the write method of one argument on each of the bytes to be
	 * written out. Subclasses are encouraged to override this method and
	 * provide a more efficient implementation.
	 * <p>
	 * If <code>b</code> is <code>null</code>, a
	 * <code>NullPointerException</code> is thrown.
	 * <p>
	 * If <code>off</code> is negative, or <code>len</code> is negative, or
	 * <code>off+len</code> is greater than the length of the array
	 * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
	 *
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @exception IOException if an I/O error occurs. In particular,
	 *                an <code>IOException</code> is thrown if the output
	 *                stream is closed. */
	public final void writeBytes(byte[] b, int off, int len) throws IOException {
		this.getOutputStream().write(b, off, len);
	}
	
	/** Writes a portion of an array of characters.
	 *
	 * @param cbuf Buffer of characters
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void write(char cbuf[], int off, int len) throws IOException {
		this.se.write(cbuf, off, len);
	}
	
	/** Writes a portion of a string.
	 *
	 * @param str A String
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void write(String str, int off, int len) throws IOException {
		this.se.write(str, off, len);
	}
	
	/** Flushes the stream.
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void flush() throws IOException {
		this.se.flush();
	}
	
	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		this.se.close();
	}
	
}
