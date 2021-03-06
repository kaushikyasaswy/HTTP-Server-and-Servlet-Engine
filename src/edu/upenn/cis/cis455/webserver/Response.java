package edu.upenn.cis.cis455.webserver;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class Response implements HttpServletResponse {
	
	static final Logger logger = Logger.getLogger(Response.class);

	public Socket clientSocket;
	public HashMap<String, String> headers = new HashMap<String,String>();
	public HashMap<String, String> status_map = new HashMap<String, String>();
	public Entry<Integer, String> error = new AbstractMap.SimpleEntry<Integer, String>(0, "");
	StringBuffer buffer;
	int buffer_size;
	MyWriter writer;
	boolean committed;
	BufferedWriter bw;
	String url;
	String servlet;
	Locale locale;

	Response() {

	}

	Response(Socket client, Entry<String, String> entry) {
		url = entry.getKey();
		servlet = entry.getValue();
		clientSocket = client;
		status_map.put("100", "Continue");
		status_map.put("200", "OK");
		status_map.put("204", "No Content");
		status_map.put("301", "Moved Permanently");
		status_map.put("302", "Found");
		status_map.put("304", "Not Modified");
		status_map.put("400", "Bad Request");
		status_map.put("401", "Unauthorized");
		status_map.put("403", "Forbidden");
		status_map.put("404", "Not Found");
		status_map.put("408", "Request Timeout");
		status_map.put("412", "Precondition Failed");
		status_map.put("500", "Internal Server Error");
		status_map.put("501", "Not Implemented");
		status_map.put("505", "HTTP Version Not Supported");
		locale = null;
		committed = false;
		buffer_size = 1000;
		buffer = new StringBuffer(buffer_size);
		try {
		bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
		writer = new MyWriter(bw);
	}

	public class MyWriter extends PrintWriter {


		MyWriter(BufferedWriter bw) {
			super(bw);
			buffer = new StringBuffer(buffer_size);
		}

		public void write(String data)
		{
			int leftover = 0;
			while (buffer.length() + data.length() >= buffer_size)
			{
				leftover = buffer_size - buffer.length();
				buffer.append(data.substring(0,leftover));
				try{
					flushBuffer();
				}catch(Exception e)
				{
					logger.error(e.getMessage());
				}
				buffer = new StringBuffer(buffer_size);
				data = data.substring(leftover);
			}
			buffer.append(data);
		}
		
		public void println(String data)
		{
			int leftover = 0;
			while (buffer.length() + data.length() >= buffer_size)
			{
				leftover = buffer_size - buffer.length();
				buffer.append(data.substring(0,leftover));
				try{
					flushBuffer();
				}catch(Exception e)
				{
					logger.error(e.getMessage());
				}
				buffer = new StringBuffer(buffer_size);
				data = data.substring(leftover);
			}
			buffer.append(data);
		}
	}

	private void writeHeader()
	{
		if (isCommitted())
			throw new IllegalStateException();	
		String status;
		String header = "";
		header = "HTTP/1.1 ";
		if (headers.containsKey("status")) {
			status = headers.get("status");
		}
		else
			status = "200";		
		header += status + " " + status_map.get(status) + "\r\n";
		for (String key : headers.keySet())
		{
			if (key.equals("status"))
				continue;
			header += key + ": " +headers.get(key) + "\r\n";
		}
		header += "Connection: close\r\n\r\n";
		try{
			bw.write(header);
			bw.flush();
			committed = true;
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	}

	void generateError(String status, String statusMessage) 
	{
		String body = "<html><head><title>"+status+": "+statusMessage+"</title></head><body>"+status+": "+statusMessage+"</body></body>" ;
		int size = body.length();
		setContentLength(size);
		setContentType("text/html");
		setStatus(Integer.parseInt(status));
		writeHeader();
		buffer = new StringBuffer(buffer_size);
		writer.write(body);
		try {
			flushBuffer();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void addCookie(Cookie arg0) {
		if(!headers.containsKey("Set-Cookie")){
			headers.put("Set-Cookie", arg0.getName() + "=" + arg0.getValue() + "; " + "Expires" + "=" + getExpirationDate(arg0));
			return;
		}
		String c = (String) headers.get("Set-Cookie");
		headers.put("Set-Cookie", c + "\r\nSet-Cookie:" + arg0.getName() + "=" + arg0.getValue() + "; " + "Expires" + "=" + getExpirationDate(arg0));
	}

	public String getExpirationDate(Cookie arg0) {
		String date;
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		date = format.format(Calendar.getInstance().getTimeInMillis() + arg0.getMaxAge());
		return date;
	}

	public boolean containsHeader(String arg0) {
		if (headers.containsKey(arg0))
			return true;
		return false;
	}

	public String encodeURL(String arg0) {
		return arg0;
	}

	public String encodeRedirectURL(String arg0) {
		return arg0;
	}

	//Depreciated
	public String encodeUrl(String arg0) {
		return null;
	}

	//Depreciated
	public String encodeRedirectUrl(String arg0) {
		return null;
	}

	public void sendError(int arg0, String arg1) {
		if (isCommitted())
		{
			throw new IllegalStateException();
		}
		else
		{
			headers.clear();
			if (status_map.get(arg0)!=null)
				arg1 = status_map.get(arg0);
			generateError(new Integer(arg0).toString(), arg1);
			committed = true;
		}
	}

	public void sendError(int arg0) {
		if (isCommitted())
		{
			throw new IllegalStateException();
		}
		else
		{
			headers.clear();
			generateError(new Integer(arg0).toString(), status_map.get(new Integer(arg0).toString()));
			committed = true;
		}
	}

	public void sendRedirect(String arg0) {
		if (isCommitted())
			throw new IllegalStateException();
		String host = clientSocket.getLocalAddress().getHostName();
		int port = clientSocket.getLocalPort();
		String location = "";
		if (arg0.contains("http://"))
			location = arg0;
		else if (arg0.startsWith("/"))
			location = "http://" + host + ":" + port + location + arg0;
		else
			location = "http://" + host + ":" + port + url.substring(0, url.indexOf(arg0))+arg0;
		headers.put("Content-Location", location);
		writeHeader();
		buffer = new StringBuffer(buffer_size);
		flushBuffer();
	}

	public void setDateHeader(String arg0, long arg1) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		Date d = new Date(arg1);
		String date = format.format(d);
		headers.put(arg0, date);
	}

	public void addDateHeader(String arg0, long arg1) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		Date d = new Date(arg1);
		String date = format.format(d);
		headers.put(arg0, date);
	}

	public void setHeader(String arg0, String arg1) {
		headers.put(arg0, arg1);
	}

	public void addHeader(String arg0, String arg1) {
		headers.put(arg0, arg1);
	}

	public void setIntHeader(String arg0, int arg1) {
		headers.put(arg0, new Integer(arg1).toString());
	}

	public void addIntHeader(String arg0, int arg1) {
		headers.put(arg0, new Integer(arg1).toString());
	}

	public void setStatus(int arg0) {
		headers.put("status", new Integer(arg0).toString());
	}

	//Depreciated
	public void setStatus(int arg0, String arg1) {

	}

	public String getCharacterEncoding() {
		return "ISO-8859-1";
	}

	public String getContentType() {
		String reply = headers.get("Content-Type");
		if (reply == null)
			return "text/html";
		return reply;
	}

	//Not required
	public ServletOutputStream getOutputStream() {
		return null;
	}

	public PrintWriter getWriter() {
		return writer;
	}

	public void setCharacterEncoding(String arg0) {
		headers.put("Character-Encoding", arg0);
	}

	public void setContentLength(int arg0) {
		headers.put("Content-Length", new Integer(arg0).toString());
	}

	public void setContentType(String arg0) {
		headers.put("Content-Type", arg0);
	}

	public void setBufferSize(int arg0) {
		if (committed) {
			throw new IllegalStateException();
		}
		buffer_size = arg0;
		try {
			writer.write(new String(buffer));
		} 
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public int getBufferSize() {
		return buffer_size;
	}

	public void flushBuffer() {
		if (!isCommitted())
		{
			writeHeader();
			committed = true;
		}
		try{
			bw.write(new String(buffer));
			bw.flush();
			buffer = new StringBuffer(buffer_size);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}

	}
	
	public void resetBuffer() {
		if (isCommitted())
		{
			throw new IllegalStateException();
		}
		buffer = new StringBuffer(buffer_size);
	}

	public boolean isCommitted() {
		return committed;
	}

	public void reset() {
		if (isCommitted())
		{
			throw new IllegalStateException();
		}
		headers.clear();
		buffer = new StringBuffer(buffer_size);
	}

	public void setLocale(Locale arg0) {
		locale = arg0;
	}

	public Locale getLocale() {
		return locale;
	}
}
