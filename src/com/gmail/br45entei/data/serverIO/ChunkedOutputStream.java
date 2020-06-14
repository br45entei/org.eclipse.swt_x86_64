package com.gmail.br45entei.data.serverIO;

import java.io.IOException;
import java.io.OutputStream;

/** If you use this to write data to an HTTP client, be sure to send a
 * "<b>{@code Transfer-Encoding: chunked}</b>" header to the client!
 * 
 * @author <a href="https://stackoverflow.com/a/2395224/2398263">BalusC</a> on
 *         <a href="https://stackoverflow.com/">stackoverflow.com</a> */
public class ChunkedOutputStream extends OutputStream {
	
	private static final byte[] CRLF = "\r\n".getBytes();
	private OutputStream output = null;
	
	/** @param output The output stream to use */
	public ChunkedOutputStream(OutputStream output) {
		this.output = output;
	}
	
	@Override
	public void write(int i) throws IOException {
		write(new byte[] {(byte) i}, 0, 1);
	}
	
	@Override
	public void write(byte[] b, int offset, int length) throws IOException {
		writeHeader(length);
		this.output.write(CRLF, 0, CRLF.length);
		this.output.write(b, offset, length);
		this.output.write(CRLF, 0, CRLF.length);
	}
	
	@Override
	public void flush() throws IOException {
		this.output.flush();
	}
	
	@Override
	public void close() throws IOException {
		writeHeader(0);
		this.output.write(CRLF, 0, CRLF.length);
		this.output.write(CRLF, 0, CRLF.length);
		this.output.close();
	}
	
	private void writeHeader(int length) throws IOException {
		byte[] header = Integer.toHexString(length).getBytes();
		this.output.write(header, 0, header.length);
	}
	
}
