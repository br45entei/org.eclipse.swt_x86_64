/*******************************************************************************
 * 
 * Copyright (C) 2020 Brian_Entei (br45entei@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 *******************************************************************************/
package com.gmail.br45entei.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.gmail.br45entei.data.DisposableByteArrayOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;

/** Utility class used for housing common file-related functions.<br>
 * You can also read file contents up to 2gb, rename files and folders, and log strings of
 * text into files that<br>
 * automatically gzip when log file size reaches 8kb with a configurable root
 * directory for log files.
 * 
 * @author <a href=
 *         "http://redsandbox.ddns.net/about/author.html">Brian_Entei</a>,
 *         <a href="http://www.joapple.ca/fr">Jonathan</a>
 * @see #readFile(File)
 * @see #renameFile(File, String)
 * @see #getRootLogFolder()
 * @see #setRootLogFolder(File)
 * @see #logStr(String, String) */
public class FileUtil {
	
	/** Wraps the given {@link OutputStream} with a new {@link PrintStream} that
	 * uses the given line separator.
	 * 
	 * @param out The output stream to wrap
	 * @param lineSeparator The line separator that the returned PrintStream
	 *            will use. If <tt><b>null</b></tt>, a new {@link PrintStream}
	 *            is simply created and returned.
	 * @return The resulting PrintStream */
	public static final PrintStream wrapOutputStream(final OutputStream out, final String lineSeparator) {
		if(lineSeparator == null) {
			return new PrintStream(out, true);
		}
		final String originalLineSeparator = AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
		try {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					System.setProperty("line.separator", lineSeparator);
					return null;
				}
			});
			return new PrintStream(out, true);
		} finally {
			System.setProperty("line.separator", originalLineSeparator);
		}
	}
	
	/** Reads and returns a single line of text from the given input stream,
	 * using the given charset to convert the read data into a string.
	 * 
	 * @param in The {@link InputStream} to read the text from
	 * @param trim Whether or not the end of the line should have any existing
	 *            (single) carriage return character removed
	 * @param charset The {@link Charset} to use when converting the read data
	 *            into a {@link String}
	 * @return The read line, or <tt><b>null</b></tt> if the end of the stream
	 *         was reached
	 * @throws IOException Thrown if a read error occurs */
	public static final String readLine(InputStream in, boolean trim, Charset charset) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b;
		while((b = in.read()) != -1) {
			if(b == 10) {//LF character '\n' (line feed)
				break;
			}
			baos.write(b);
		}
		if(b == -1 && baos.size() == 0) {
			return null;
		}
		byte[] data = baos.toByteArray();
		String line = new String(data, 0, data.length, charset);
		return trim && line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
	}
	
	/** Reads and returns a single line of text from the given input stream,
	 * using the {@link StandardCharsets#ISO_8859_1 ISO_8859_1} standard charset
	 * to convert the read data into a string.
	 * 
	 * @param in The {@link InputStream} to read the text from
	 * @param trim Whether or not the end of the line should have any existing
	 *            (single) carriage return character removed
	 * @return The read line, or <tt><b>null</b></tt> if the end of the stream
	 *         was reached
	 * @throws IOException Thrown if a read error occurs */
	public static final String readLine(InputStream in, boolean trim) throws IOException {
		return readLine(in, trim, StandardCharsets.ISO_8859_1);
	}
	
	/** Reads and returns a single line of text from the given input stream,
	 * using the {@link StandardCharsets#ISO_8859_1 ISO_8859_1} standard charset
	 * to convert the read data into a string.
	 * 
	 * @param in The {@link InputStream} to read the text from
	 * @return The read line, or <tt><b>null</b></tt> if the end of the stream
	 *         was reached
	 * @throws IOException Thrown if a read error occurs */
	public static final String readLine(InputStream in) throws IOException {
		return readLine(in, true);
	}
	
	/** Test to see ratio of file size between gzipped and plain text files;
	 * this is not API
	 * 
	 * @param args Program command line arguments */
	public static final void main(String[] args) {
		String logName = "test";
		for(int index = 0; index < 5; index++) {
			DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
			try {
				GZIPOutputStream gzip = new GZIPOutputStream(baos);
				while(baos.toByteArray().length < 16103) {//14570) {//17636) {//19169) {//12288) {//8192) {
					String random = StringUtil.nextSessionId();
					byte[] r = random.getBytes(StandardCharsets.UTF_8);
					gzip.write(r);
					logStr(logName, random);
				}
				gzip.flush();
				gzip.close();
			} catch(IOException e) {
				baos.close();
				throw new Error("This should not have happened!", e);
			}
		}
	}
	
	private static volatile File rootLogDir = new File(System.getProperty("user.dir"));
	
	/** @param file The file whose contents will be read
	 * @return The file's contents, in a byte array
	 * @throws IOException Thrown if an I/O exception occurs */
	public static final byte[] readFile(File file) throws IOException {
		if(file == null || !file.isFile()) {
			return null;
		}
		try(FileInputStream in = new FileInputStream(file)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = in.read()) >= 0) {
				baos.write(read);
			}
			return baos.toByteArray();
		}
	}
	
	/** @param file The file whose contents will be read
	 * @return The file's contents, in a byte array */
	public static final byte[] readFileData(File file) {
		DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
		try(FileInputStream fis = new FileInputStream(file)) {
			byte[] buf = new byte[2048];
			int read;
			while((read = fis.read(buf, 0, buf.length)) != -1) {
				baos.write(buf, 0, read);
			}
		} catch(IOException ignored) {
		}
		byte[] bytes = baos.getBytesAndDispose();
		baos.close();
		return bytes;
	}
	
	/** @param file The file whose contents will be read
	 * @return The file's contents as a string, where lines are delimited via
	 *         '{@code \r\n}'. */
	public static final String readFileBR(File file) {
		StringBuilder sb = new StringBuilder();
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\r\n");
			}
		} catch(IOException ignored) {
		}
		return sb.toString();
	}
	
	/** @param file The file whose contents will be read
	 * @param charset The charset to use when reading the file data
	 * @return The file's contents as a string using the specified charset,
	 *         where lines are delimited via '{@code \r\n}'. */
	public static final String readFile(File file, Charset charset) {
		if(charset == null) {
			return readFileBR(file);
		}
		byte[] bytes = readFileData(file);
		String rtrn = new String(bytes, charset);
		bytes = null;
		System.gc();
		return rtrn;
	}
	
	/** @param file The file whose accessibly will be checked
	 * @return True if the file can be written to, false otherwise
	 * @throws InvalidPathException if a {@code Path} object cannot be
	 *             constructed from the abstract path (see
	 *             {@link java.nio.file.FileSystem#getPath
	 *             FileSystem.getPath(String first, String... more)}) */
	public static final boolean isFileAccessible(File file) throws InvalidPathException {
		/*if(file.exists()) {
			try {
				@SuppressWarnings("unused")
				FileInfo unused = new FileInfo(file, null); //(Moved from JavaWebServer FileUtil of same name and package as this)
				return true;
			} catch(IOException ignored) {
			}
		}
		return false;*/
		
		if(file != null) {
			if(file.getAbsolutePath().startsWith("." + File.separator)) {
				file = new File(File.separator + file.getAbsolutePath().substring(("." + File.separator).length()));//remove the freakin dot...
			}
			if(file.getAbsolutePath().contains("." + File.separator)) {
				file = new File(FilenameUtils.normalize(file.getAbsolutePath()));
			}
		}
		return file != null ? Files.isWritable(file.toPath()) : false;
	}
	
	/** @param file The file whose last modified time will be returned
	 * @return The file's last modified attribute
	 * @throws IOException Thrown if an I/O exception occurs */
	public static final long getLastModified(File file) throws IOException {
		return Files.getLastModifiedTime(file.toPath()).toMillis();
	}
	
	/** @param file The file or folder whose size(content length, number of
	 *            bytes, etc....) will be returned
	 * @return The file or folder's size(folders will usually just return
	 *         {@code 0}; to get a folder's size, see
	 *         {@link #getSizeDeep(Path)}.)
	 * @throws IOException if an I/O error occurs */
	public static final long getSize(File file) throws IOException {
		return Files.size(file.toPath());
	}
	
	/** @param startPath The file or folder whose size(content length, number of
	 *            bytes, etc....) will be returned
	 * @return The file or folder's size
	 * @throws IOException if an I/O error is thrown by a visitor method */
	public static final long getSizeDeep(Path startPath) throws IOException {
		final AtomicLong size = new AtomicLong(0);
		Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				size.addAndGet(attrs.size());
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				// Skip folders that can't be traversed
				//System.out.println("skipped: " + file + "e=" + exc);
				return FileVisitResult.CONTINUE;
			}
		});
		return size.get();
	}
	
	/** @param file The file to rename
	 * @param renameTo The new name for the file
	 * @return Whether or not the renaming was successful
	 * @throws IOException Thrown if an I/O exception occurs */
	public static final boolean renameFile(File file, String renameTo) throws IOException {
		Path source = Paths.get(file.toURI());
		boolean success = true;
		if(file.isDirectory() && file.getName().equalsIgnoreCase(renameTo)) {
			File folder = new File(file.getParentFile(), renameTo);
			success = file.renameTo(folder);
		} else {
			Files.move(source, source.resolveSibling(renameTo), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		}
		return success;
	}
	
	private static final ConcurrentHashMap<String, File> logFiles = new ConcurrentHashMap<>();
	
	/** @param logName The base name of the log file. Example: if you want a log
	 *            file named &quot;Commands.log&quot;, then you would only
	 *            supply the string &quot;Commands&quot; here
	 * @param createIfNotExist Whether or not the log file should be created
	 * @return The log file if it already existed or the createIfNotExist
	 *         boolean argument was set to {@code true}. If the file did not
	 *         exist and the argument was {@code false}, then {@code null} is
	 *         returned.
	 * @throws IOException Thrown if an I/O exception occurs */
	private static final File getLogFile(String logName, boolean createIfNotExist) throws IOException {
		if(logName == null) {
			return null;
		}
		File file = logFiles.get(logName);
		boolean fileDidExist = false;
		if(file != null) {
			fileDidExist = file.isFile();//true;
			//If the file's name doesn't match it's original key(file was renamed into an archive?), or the file's parental directory
			//structure has changed(either the root folder was changed, or the file was moved into a different folder somehow), then:
			if(!file.getName().equals(logName) || !FilenameUtils.normalize(file.getParentFile().getAbsolutePath()).equals(FilenameUtils.normalize(getRootLogFolder().getAbsolutePath()))) {
				logFiles.remove(logName);//unregister the now invalid file object
				file = null;//set the variable to null so that it is re-created below if either createIfNotExist or fileDidExist is true
			}
		}
		if(createIfNotExist || fileDidExist) {
			if(file == null) {
				file = new File(getRootLogFolder(), logName + ".log");
			}
			if(!file.exists()) {
				file.createNewFile();
			}
			logFiles.put(logName, file);
			return file;
		}
		return null;
	}
	
	/** @return The parent directory for any log files created */
	public static final File getRootLogFolder() {
		return rootLogDir;
	}
	
	/** @param folder The new parent directory that will contain all future log
	 *            files */
	public static final void setRootLogFolder(File folder) {
		if(folder != null) {
			if(!folder.exists()) {
				folder.mkdirs();
			}
			rootLogDir = folder;
		}
	}
	
	private static final File getArchiveFolder() {
		File logs = new File(getRootLogFolder(), "Logs");
		if(!logs.exists()) {
			logs.mkdirs();
		}
		return logs;
	}
	
	/** @param logName The base name of the log file. Example: if you want a log
	 *            file<br>
	 *            named &quot;Commands.log&quot;, then you would only supply<br>
	 *            the string &quot;Commands&quot; here
	 * @param str The line or lines of text to append to the end of the log
	 *            file */
	public static final void logStr(String logName, String str) {
		try {
			File file = getLogFile(logName, true);
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			final long lastModifiedTime = attr.lastModifiedTime().toMillis();
			final long fileSize = attr.size();
			boolean fileSizeLimitReached = fileSize >= 8192;
			if(fileSizeLimitReached) {//8kb
				/*if(renameFile(file, "Logs" + File.separator + fileName)) {
					file = getLogFile(fileName, true);
				} else {
					System.err.println("File not renamed!");
				}*/
				String baseName = FilenameUtils.getBaseName(file.getName()) + "_" + StringUtil.getTime(lastModifiedTime, false, true, true);
				String ext = ".log.gz";
				String fileName = baseName + ext;
				File archived = new File(getArchiveFolder(), fileName);
				int duplicates = 0;//juuust in case
				while(archived.exists()) {
					archived = new File(getArchiveFolder(), baseName + "_" + (duplicates++) + ext);
				}
				byte[] r = FileUtil.readFile(file);
				GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(archived, false));
				out.write(r);
				out.flush();
				out.close();
				FileDeleteStrategy.FORCE.deleteQuietly(file);
				file = getLogFile(logName, true);
			}
			FileOutputStream out = new FileOutputStream(file, !fileSizeLimitReached);
			PrintWriter pr = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
			pr.println(str);
			pr.flush();
			pr.close();
		} catch(Error e) {
		} catch(RuntimeException e) {
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
}
