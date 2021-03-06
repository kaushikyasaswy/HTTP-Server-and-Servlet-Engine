package edu.upenn.cis.cis455.webserver;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.StringUtils;
import org.xml.sax.SAXException;

public class Handler implements Runnable {

	static final Logger logger = Logger.getLogger(Handler.class);
	static Set<String> map = new LinkedHashSet<String>();
	private final Queue<Socket> Q;
	private HashMap<String,String> thread_request_map = new HashMap<String,String>();
	public enum methods {GET, HEAD, DEFAULT};
	public ArrayList<String> validversions = new ArrayList<String>();
	public boolean path_ok = true;
	public boolean file_modified = false;
	Date d = Calendar.getInstance().getTime();
	SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	String date;
	int count = 0;
	HashMap<String,HttpServlet> servlets;
	HashMap<String,String> servlet_mapping;
	public boolean is_servlet = false;
	String new_url = null;
	public Container container;
	public Context context;
	public HashMap<String, String> absolute_urls;
	public HashMap<String, String> path_urls;
	public static HashMap<String, Session> session_map;
	public static Random random = new Random();
	public static HashMap<Socket, BufferedReader> socket_readers = new HashMap<Socket, BufferedReader>();
	public static HashMap<Session, Long> session_last_accessed_times = new HashMap<Session, Long>();

	public Handler(Queue<Socket> Q) throws ParserConfigurationException, SAXException, IOException {
		this.Q = Q; //The queue containing the client sockets
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		date = format.format(d); //The current date in HTTP/1.1 format
		validversions.add("HTTP/1.1");
		validversions.add("HTTP/1.0");
		validversions.add("http/1.0");
		validversions.add("http/1.1");
		session_map = new HashMap<String, Session>();
		container = new Container();
		context = container.context;
		servlets = container.servlets;
		servlet_mapping = container.h.m_servletMapping;
		absolute_urls = new HashMap<String, String>();
		path_urls = new HashMap<String, String>();
		for (String url : servlet_mapping.keySet()) {
			new_url = (!url.startsWith("/")) ? "/"+url : url;
			if (!url.contains("*"))
				absolute_urls.put(new_url, servlet_mapping.get(url));
			else if (url.endsWith("/*"))
				path_urls.put(new_url.replace("/*", ""), servlet_mapping.get(url));
		}
	}

	//Generate appropriate error message
	public void generate_error(int error, HashMap<String,String> hm, Socket clientSocket) {
		try {
			String header = "";
			String content = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			switch(error) {
			case 400:
				header = "HTTP/1.1 400 Bad Request\r\n";
				content = "<html><body>\n<h1>400: Bad Request</h1>\n</body></html>";
				break;

			case 403:
				header = "HTTP/1.1 403 Forbidden\r\n";
				content = "<html><body>\n<h1>403: Forbidden Access</h1>\n</body></html>";
				break;

			case 404:
				header = "HTTP/1.1 404 Not Found\r\n";
				content = "<html><body>\n<h1>404: File Not Found</h1>\n</body></html>";
				break;

			case 415:
				header = "HTTP/1.1 415 Unsupported Media Type\r\n";
				content = "<html><body>\n<h1>415: Unsupported Media Type</h1>\n</body></html>";
				break;

			case 500:
				header = "HTTP/1.1 Internal Server Error\r\n";
				content = "<html><body>\n<h1>500: Internal Server Error</h1>\n</body></html>";
				break;

			case 501:
				header = "HTTP/1.1 501 Not Implemented\r\n";
				content = "<html><body>\n<h1>501: Not Implemented</h1>\n</body></html>";
				break;

			case 505:
				header = "HTTP/1.1 505 HTTP Version Not Supported\r\n";
				content = "<html><body>\n<h1>505: HTTP Version Not Supported</h1>\n</body></html>";
				break;		
			}
			content_length = content.getBytes().length;
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			out.write(content.getBytes());
			out.flush();
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	//Generate the response when a file is requested
	public void generate_response_file(HashMap<String,String> hm, Socket clientSocket, File f) {
		try {
			String header = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			String filepath = f.getAbsolutePath();
			FileInputStream fis = new FileInputStream(filepath);
			header = "HTTP/1.1 200 OK\r\n";
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			if(filepath.endsWith("html") || filepath.endsWith("htm")) {
				header += "Content-Type: text/html\r\n";
			}
			else if(filepath.endsWith("txt")) {
				header += "Content-Type: text/plain\r\n";
			}
			else if(filepath.endsWith("jpg")) {
				header += "Content-Type: image/jpg\r\n";
			}
			else if(filepath.endsWith("jpeg")) {
				header += "Content-Type: image/jpeg\r\n";
			}
			else if(filepath.endsWith("gif")) {
				header += "Content-Type: image/gif\r\n";
			}
			else if(filepath.endsWith("png")) {
				header += "Content-Type: image/png\r\n";
			}
			else {
				generate_error(415, hm, clientSocket); //Unsupported media type
				fis.close();
				return;
			}
			content_length = fis.available();
			byte[] content = new byte[content_length];
			fis.read(content);
			header += "Content-Length: " + content_length + "\r\n";
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			header += "Last Modified: " + sdf.format(f.lastModified()) + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			out.write(content);
			out.flush();
			fis.close();
			//out.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	//Generate response when a directory listing is requested
	public void generate_response_directory(HashMap<String,String> hm, Socket clientSocket, File f) {
		try {
			String header = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			header = "HTTP/1.1 200 OK\r\n";
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			File[] contents = f.listFiles();
			String page_content = "<html>\n<body>\n<h1 style=\"text-align:center\"><b>\"JerryMouse\" Server</b></h1><hr>\n<h2>Contents  of  <a href=\"http://" + hm.get("host") + "/\">ROOT</a> / ";
			String links[] = hm.get("path").split("/");
			for (int i=2; i<links.length+1; i++) {
				String href = hm.get("host");
				for (int j = 1; j<i; j++) {
					href += "/" + links[j];
				}
				page_content += "<a href=\"http://" + href + "\">" + URLDecoder.decode(links[i-1],"UTF-8") + "</a> / ";
			}
			page_content += "</h2>\n<table cellspacing=\"30\"><tr><th>Name</th><th>Type</th><th>Size</th><th>Last Modified</th></tr>";
			//List the contents of the directory
			for(File s: contents) {
				int l = Server.directory.length();
				String abs = s.getAbsolutePath();
				String relative_path = abs.substring(l, abs.length());
				page_content += "<tr><td><a href=\"";
				page_content += "http://"+ hm.get("host") + relative_path;
				page_content += "\">" + s.getName() + "</a></td><td>";
				if (s.isDirectory())
					page_content += "Sub-Directory</td><td>N/A</td><td>";
				else { //List the file type
					if (s.getName().endsWith("jpeg") || s.getName().endsWith("jpg") || s.getName().endsWith("gif") || s.getName().endsWith("png"))
						page_content += "Image</td><td>";
					else if (s.getName().endsWith("html"))
						page_content += "HTML File</td><td>";
					else if (s.getName().endsWith("txt"))
						page_content += "Text File</td><td>";
					else
						page_content += "Unknown</td><td>";
					FileInputStream fis = new FileInputStream(s.getAbsolutePath());
					page_content += fis.available() + " B</td><td>";
					fis.close();
				}
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				page_content += sdf.format(s.lastModified()) + "</td></tr>";
			}
			page_content += "</table></body></html>";
			content_length = page_content.length();
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			out.write(page_content.getBytes());
			out.flush();
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	//Generate response when the error log page is requested
	public void generate_errorlog_page(HashMap<String,String> hm, Socket clientSocket) throws IOException {
		File f = new File("htmlLayout.html");
		String header = "";
		int content_length = 0;
		DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
		String filepath = f.getAbsolutePath();
		FileInputStream fis = new FileInputStream(filepath);
		header = "HTTP/1.1 200 OK\r\n";
		header += "Server: JerryMouse Server/x1.0\r\n";
		header += "Date: " + date + "\r\n";
		header += "Content-Type: text/html\r\n";
		content_length = fis.available();
		byte[] content = new byte[content_length];
		fis.read(content);
		header += "Content-Length: " + content_length + "\r\n";
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		header += "Connection: close\r\n";
		header += "\r\n";
		out.write(header.getBytes());
		out.write(content);
		out.flush();
		fis.close();
		//out.close();
	}

	//Generate response when the control page is requested
	public void generate_control_page(HashMap<String,String> hm, Socket clientSocket) {
		try {
			String header = "";
			String content = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			content = "<html><body>\n<h1 style=\"text-align:center\">Control Panel</h1>\n<hr>\nFull Name: Kaushik Yasaswy Suryanarayana<br>SEAS Login: kaus\n<hr>\n";
			content += "<button type=\"button\" onclick=\"location.href='http://" + hm.get("host") + "/errorlog'\"><b>Error Log</b></button><hr>";
			content += "<table cellspacing = \"20\"><tr><th>Thread</th><th>Status</th></tr>";
			for(Thread t : Server.threadpool) {
				content += "<tr><td align=\"center\">" + t.getName() + "</td><td align=\"center\">";
				if (t.getState() == Thread.State.RUNNABLE) { 
					content += thread_request_map.get(t.getName()); //Get the URL the thread is servicing
				}
				else
					content += t.getState().toString(); //Get the state of the thread otherwise
				content += "</td></tr>";
			}
			content += "</table><hr><button type=\"button\" onclick=\"location.href='http://" + hm.get("host") + "/shutdown'\"><b>Shutdown Server</b></button>";
			content += "</body></html>";
			byte[] b = content.getBytes();
			content_length = b.length;
			header = "HTTP/1.1 200 OK\r\n";
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			out.write(b);
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	//Generate the response when a request for shutdown is received and start the shutdown process
	public void generate_shutdown_page(HashMap<String,String> hm, Socket clientSocket) {
		try {
			String header = "";
			String content = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			Server.alive = false; //Indicate that a request for shutdown was received using this flag
			Server.serverSocket.close(); //Stop accepting further requests
			content = "<html><body>\n<h1 style=\"text-align:center\">The Server is going to shutdown now. Goodbye!</h1>";
			content += "</body></html>";
			content_length = content.getBytes().length;
			header = "HTTP/1.1 200 OK\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			out.write(content.getBytes());
			out.flush();
			out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	//The following are the same methods implemented for HEAD method (message body isnt sent in the response)

	public void generate_error_alt(int error, HashMap<String,String> hm, Socket clientSocket) {
		try {
			String header = "";
			String content = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			switch(error) {
			case 400:
				header = "HTTP/1.1 400 Bad Request\r\n";
				content = "<html><body>\n<h1>400: Bad Request</h1>\n</body></html>";
				break;

			case 403:
				header = "HTTP/1.1 403 Forbidden\r\n";
				content = "<html><body>\n<h1>403: Forbidden Access</h1>\n</body></html>";
				break;

			case 404:
				header = "HTTP/1.1 404 Not Found\r\n";
				content = "<html><body>\n<h1>404: File Not Found</h1>\n</body></html>";
				break;

			case 415:
				header = "HTTP/1.1 415 Unsupported Media Type\r\n";
				content = "<html><body>\n<h1>415: Unsupported Media Type</h1>\n</body></html>";
				break;

			case 501:
				header = "HTTP/1.1 501 Not Implemented\r\n";
				content = "<html><body>\n<h1>501: Not Implemented</h1>\n</body></html>";
				break;

			case 505:
				header = "HTTP/1.1 505 HTTP Version Not Supported\r\n";
				content = "<html><body>\n<h1>505: HTTP Version Not Supported</h1>\n</body></html>";
				break;		
			}
			content_length = content.getBytes().length;
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			//out.write(content.getBytes());
			out.flush();
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void generate_response_file_alt(HashMap<String,String> hm, Socket clientSocket, File f) {
		try {
			String header = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			String filepath = f.getAbsolutePath();
			FileInputStream fis = new FileInputStream(filepath);
			header = "HTTP/1.1 200 OK\r\n";
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			if(filepath.endsWith("html") || filepath.endsWith("htm")) {
				header += "Content-Type: text/html\r\n";
			}
			else if(filepath.endsWith("txt")) {
				header += "Content-Type: text/plain\r\n";
			}
			else if(filepath.endsWith("jpg")) {
				header += "Content-Type: image/jpg\r\n";
			}
			else if(filepath.endsWith("jpeg")) {
				header += "Content-Type: image/jpeg\r\n";
			}
			else if(filepath.endsWith("gif")) {
				header += "Content-Type: image/gif\r\n";
			}
			else if(filepath.endsWith("png")) {
				header += "Content-Type: image/png\r\n";
			}
			else {
				generate_error(415, hm, clientSocket);
				fis.close();
				return;
			}
			content_length = fis.available();
			byte[] content = new byte[content_length];
			fis.read(content);
			header += "Content-Length: " + content_length + "\r\n";
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			header += "Last Modified: " + sdf.format(f.lastModified()) + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			//out.write(content);
			out.flush();
			fis.close();
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void generate_response_directory_alt(HashMap<String,String> hm, Socket clientSocket, File f) {
		try {
			String header = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			header = "HTTP/1.1 200 OK\r\n";
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			File[] contents = f.listFiles();
			String page_content = "<html>\n<body>\n<h1 style=\"text-align:center\"><b>\"JerryMouse\" Server</b></h1><hr>\n<h2>Contents  of  <a href=\"http://" + hm.get("host") + "/\">ROOT</a> / ";
			String links[] = hm.get("path").split("/");
			for (int i=2; i<links.length+1; i++) {
				String href = hm.get("host");
				for (int j = 1; j<i; j++) {
					href += "/" + links[j];
				}
				page_content += "<a href=\"http://" + href + "\">" + URLDecoder.decode(links[i-1],"UTF-8") + "</a> / ";
			}
			page_content += "</h2>\n<table cellspacing=\"30\"><tr><th>Name</th><th>Type</th><th>Size</th><th>Last Modified</th></tr>";
			for(File s: contents) {
				int l = Server.directory.length();
				String abs = s.getAbsolutePath();
				String relative_path = abs.substring(l, abs.length());
				page_content += "<tr><td><a href=\"";
				page_content += "http://"+ hm.get("host") + relative_path;
				page_content += "\">" + s.getName() + "</a></td><td>";
				if (s.isDirectory())
					page_content += "Sub-Directory</td><td>N/A</td><td>";
				else {
					if (s.getName().endsWith("jpeg") || s.getName().endsWith("jpg") || s.getName().endsWith("gif") || s.getName().endsWith("png"))
						page_content += "Image</td><td>";
					else if (s.getName().endsWith("html"))
						page_content += "HTML File</td><td>";
					else if (s.getName().endsWith("txt"))
						page_content += "Text File</td><td>";
					else
						page_content += "Unknown</td><td>";
					FileInputStream fis = new FileInputStream(s.getAbsolutePath());
					page_content += fis.available() + " B</td><td>";
					fis.close();
				}
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				page_content += sdf.format(s.lastModified()) + "</td></tr>";
			}
			page_content += "</table></body></html>";
			content_length = page_content.length();
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			//out.write(page_content.getBytes());
			out.flush();
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void generate_errorlog_page_alt(HashMap<String,String> hm, Socket clientSocket) throws IOException {
		File f = new File("htmlLayout.html");
		String header = "";
		int content_length = 0;
		DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
		String filepath = f.getAbsolutePath();
		FileInputStream fis = new FileInputStream(filepath);
		header = "HTTP/1.1 200 OK\r\n";
		header += "Server: JerryMouse Server/x1.0\r\n";
		header += "Date: " + date + "\r\n";
		header += "Content-Type: text/html\r\n";
		content_length = fis.available();
		byte[] content = new byte[content_length];
		fis.read(content);
		header += "Content-Length: " + content_length + "\r\n";
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		header += "Connection: close\r\n";
		header += "\r\n";
		out.write(header.getBytes());
		//out.write(content);
		out.flush();
		fis.close();
		//out.close();
	}

	public void generate_control_page_alt(HashMap<String,String> hm, Socket clientSocket) {
		try {
			String header = "";
			String content = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			content = "<html><body>\n<h1 style=\"text-align:center\">Control Panel</h1>\n<hr>\nFull Name: Kaushik Yasaswy Suryanarayana<br>SEAS Login: kaus\n<hr>\n";
			content += "<button type=\"button\" onclick=\"location.href='http://" + hm.get("host") + "/errorlog'\"><b>Error Log</b></button><hr>";
			content += "<table cellspacing = \"20\"><tr><th>Thread</th><th>Status</th></tr>";
			for(Thread t : Server.threadpool) {
				content += "<tr><td align=\"center\">" + t.getName() + "</td><td align=\"center\">";
				if (t.getState() == Thread.State.RUNNABLE) { 
					content += thread_request_map.get(t.getName()); //Get the URL the thread is servicing
				}
				else
					content += t.getState().toString(); //Get the state of the thread otherwise
				content += "</td></tr>";
			}
			content += "</table><hr><button type=\"button\" onclick=\"location.href='http://" + hm.get("host") + "/shutdown'\"><b>Shutdown Server</b></button>";
			content += "</body></html>";
			byte[] b = content.getBytes();
			content_length = b.length;
			header = "HTTP/1.1 200 OK\r\n";
			header += "Server: JerryMouse Server/x1.0\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			//out.write(b);
			//out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void generate_shutdown_page_alt(HashMap<String,String> hm, Socket clientSocket) {
		try {
			String header = "";
			String content = "";
			int content_length = 0;
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			Server.alive = false;
			Server.serverSocket.close();
			content = "<html><body>\n<h1 style=\"text-align:center\">The Server is going to shutdown now. Goodbye!</h1>";
			content += "</body></html>";
			content_length = content.getBytes().length;
			header = "HTTP/1.1 200 OK\r\n";
			header += "Date: " + date + "\r\n";
			header += "Content-Type: text/html\r\n";
			header += "Content-Length: " + content_length + "\r\n";
			header += "Connection: close\r\n";
			header += "\r\n";
			out.write(header.getBytes());
			//out.write(content.getBytes());
			out.flush();
			out.close();
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public String check_if_modified(HashMap<String,String> hm, Socket clientSocket, File f, int i) {
		try {
			SimpleDateFormat f1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			SimpleDateFormat f2 = new SimpleDateFormat("E, dd-MMM-yyyy HH:mm:ss zzz");
			SimpleDateFormat f3 = new SimpleDateFormat("E MMM  d HH:mm:ss yyyy");
			String head_date;
			int current_year = Calendar.getInstance().YEAR;
			current_year = current_year % 100;
			if (i==1)
				head_date = hm.get("if-modified-since");
			else
				head_date = hm.get("if-unmodified-since");
			Date header_date;
			if (head_date.contains(","))
				if (head_date.split(",")[0].length() == 3)
					header_date = f1.parse(head_date);
				else {
					String[] date_parts1 = head_date.split("-");
					String[] date_parts2 = date_parts1[2].split(" ");
					int year =Integer.parseInt(date_parts2[0]);
					String new_year = "";
					if (year>(current_year+50))
						new_year = "19" + Integer.toString(year);
					else
						new_year = "20" + Integer.toString(year);
					head_date = date_parts1[0] + "-" + date_parts1[1] + "-" + new_year + " " + date_parts2[1] + " " + date_parts2[2];
					header_date = f2.parse(head_date);
				}
			else
				header_date = f3.parse(head_date);
			String filedate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").format(new Date(f.lastModified()));
			Date file_date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(filedate);
			if (!header_date.before(file_date))
				return date;
			else {
				file_modified = true;
			}
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
		return date;
	}

	//Main method for generating all responses for resource requests
	public void generate_response(HashMap<String,String> hm, Socket clientSocket) {
		try {

			String header = "";
			String content = "";
			int content_length = 0;
			methods method;
			try {
				method = methods.valueOf(hm.get("method"));
			}
			catch (IllegalArgumentException e) {
				method = methods.valueOf("DEFAULT");
			}
			//Error 400: Bad Request - When the version is 1.1 but does not contain a Host field
			if(hm.get("version").toString().equals(new String("HTTP/1.1")) && !hm.containsKey("host")) {
				System.out.println("true");
				generate_error(400, hm, clientSocket);
				return;
			}
			//Handling absolute URLs
			String path = hm.get("path");
			String[] path_parts;
			if (path.startsWith("http")) {
				if(path.contains(clientSocket.getInetAddress().getHostName()))
					path_parts = path.split(clientSocket.getInetAddress().getHostName()+":"+clientSocket.getLocalPort());
				else
					path_parts = path.split(clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getLocalPort());
				hm.put("path",path_parts[1]);
			}
			//Adding the default port 80 if none exists in the Host field
			if (hm.containsKey("host")) {
				if (!hm.get("host").matches(".*\\d+.*"))
					hm.put("host", hm.get("host").trim() + ":80");
			}
			if (hm.containsKey("host")) //Store the URL a thread is currently servicing
				thread_request_map.put(Thread.currentThread().getName(),hm.get("host").toString()+hm.get("path").toString());
			else { //Case when HTTP/1.0 and no Host field request for control page
				String host = clientSocket.getLocalAddress().getHostName() + ":" + clientSocket.getLocalPort();
				thread_request_map.put(Thread.currentThread().getName(),host + hm.get("path").toString());
			}

			//Inspect the method in the request header
			switch (method) {

			case GET:

				//If the request is for the control page
				if (hm.get("path").equals(new String("/control"))) {
					generate_control_page(hm, clientSocket);
					return;	
				}
				//If the request if for the error log page
				if (hm.get("path").equals("/errorlog")) {
					generate_errorlog_page(hm, clientSocket);
					return;
				}
				//If the request is for the shutdown page
				if (hm.get("path").equals(new String("/shutdown"))) {
					//logger.info("Received a request for shutdown");
					generate_shutdown_page(hm, clientSocket);
					return;	
				}
				String filepath = Server.directory + hm.get("path");
				filepath = simplify_path(filepath);
				if (!path_ok) { //Forbidden access
					generate_error(403, hm, clientSocket);
					path_ok = true;
					return;
				}
				File f = new File(filepath);
				//If the resource does not exist
				if(!f.exists()) {
					generate_error(404, hm, clientSocket);
					return;
				}
				//If the resource does not have read permissions
				if(!f.canRead()) {
					generate_error(403, hm, clientSocket);
					return;
				}
				//Both If-Modified-Since and If-Unmodified-Since fields are not allowed accoridng to HTTP specs
				if (hm.containsKey("if-modified-since") && hm.containsKey("if-unmodified-since")) {
					generate_error(400, hm, clientSocket);
					return;
				}
				String new_date = null;
				if (hm.containsKey("if-modified-since")) {
					new_date = check_if_modified(hm, clientSocket, f, 1);
					if (!file_modified) { //If the file has not been modified
						DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
						header = "HTTP/1.1 304 Not Modified\r\n";
						header += "Server: JerryMouse Server/x1.0\r\n";
						header += "Date: "+new_date+"\r\n";
						header += "Connection: close";
						header += "\r\n";
						out.write(header.getBytes());
						//out.close();
						return;
					}
				}
				if (hm.containsKey("if-unmodified-since")) {
					new_date = check_if_modified(hm, clientSocket, f, 2);
					if (file_modified) { //If the file has been modified
						DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
						header = "HTTP/1.1 412 Precondition Failed\r\n";
						header += "Server: JerryMouse Server/x1.0\r\n";
						header += "Connection: close";
						header += "\r\n";
						out.write(header.getBytes());
						//out.close();
						return;
					}
				}
				//If the requested resource is a directory
				if(f.isDirectory()) {
					generate_response_directory(hm, clientSocket, f);
					return;
				}
				//If the requested resource is a file
				if(f.isFile()) {
					generate_response_file(hm, clientSocket, f);
					return;
				}
				break;

			case HEAD:

				//If the request is for the control page
				if (hm.get("path").equals(new String("/control"))) {
					generate_control_page_alt(hm, clientSocket);
					return;	
				}
				//If the request if for the error log page
				if (hm.get("path").equals("/errorlog")) {
					generate_errorlog_page_alt(hm, clientSocket);
					return;
				}
				//If the request is for the shutdown page
				if (hm.get("path").equals(new String("/shutdown"))) {
					//logger.info("Received a request for shutdown");
					generate_shutdown_page_alt(hm, clientSocket);
					return;	
				}
				String filepath_alt = Server.directory + hm.get("path");
				filepath_alt = simplify_path(filepath_alt);
				if (!path_ok) {
					generate_error_alt(403, hm, clientSocket);
					path_ok = true;
					return;
				}
				File f_alt = new File(filepath_alt);
				//If the resource does not exist
				if(!f_alt.exists()) {
					generate_error_alt(404, hm, clientSocket);
					return;
				}
				//If the resource does not have read permissions
				if(!f_alt.canRead()) {
					generate_error_alt(403, hm, clientSocket);
					return;
				}
				String new_date_alt = null;
				if (hm.containsKey("if-modified-since")) {
					generate_error_alt(400, hm, clientSocket);
				}
				if (hm.containsKey("if-unmodified-since")) {
					new_date_alt = check_if_modified(hm, clientSocket, f_alt, 2);
					if (file_modified) {
						DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
						header = "HTTP/1.1 412 Precondition Failed\r\n";
						header += "Server: JerryMouse Server/x1.0\r\n";
						header += "Connection: close";
						header += "\r\n";
						out.write(header.getBytes());
						//out.close();
						return;
					}
				}
				//If the requested resource is a directory
				if(f_alt.isDirectory()) {
					generate_response_directory_alt(hm, clientSocket, f_alt);
					return;
				}
				//If the requested resource is a file
				if(f_alt.isFile()) {
					generate_response_file_alt(hm, clientSocket, f_alt);
					return;
				}
				break;

			case DEFAULT: //Method not implemented in the current version of the server

				generate_error(501, hm, clientSocket);
				return;

			}
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}

	}

	//Validating the path
	public String simplify_path(String s) { 
		try {
			s = URLDecoder.decode(s, "UTF-8");
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
		Path p = Paths.get(s);
		s = p.normalize().toString();
		String[] home_directory_parts = Server.directory.split("/");
		String home = home_directory_parts[home_directory_parts.length-1];
		if (!s.contains(home))
			path_ok = false; //Trying to access files beyond home directory
		if (s.endsWith("/etc/passwd") || s.endsWith("/etc/passwd/"))
			path_ok = false; //Trying to access password
		return s;
	}

	public boolean validate_case_servlets(HashMap<String, String> hm, Socket clientSocket) {
		try {
			if(hm.get("version").toString().equals(new String("HTTP/1.1")) && !hm.containsKey("host")) {
				generate_error(400, hm, clientSocket);
				return false;
			}
			String path = hm.get("path");
			String[] path_parts;
			if (path.startsWith("http")) {
				if(path.contains(clientSocket.getInetAddress().getHostName()))
					path_parts = path.split(clientSocket.getInetAddress().getHostName()+":"+clientSocket.getLocalPort());
				else
					path_parts = path.split(clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getLocalPort());
				hm.put("path",path_parts[1]);
			}
			if (hm.containsKey("host")) {
				if (!hm.get("host").matches(".*\\d+.*"))
					hm.put("host", hm.get("host").trim() + ":80");
			}
			String filepath_alt = Server.directory + hm.get("path");
			filepath_alt = simplify_path(filepath_alt);
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
		if (!path_ok) {
			generate_error_alt(403, hm, clientSocket);
			path_ok = true;
			return false;
		}
		return true;

	}

	public boolean validate_line(String request_line, HashMap<String,String> hm, Socket clientSocket) {
		String reg_exp = "\\w+\\s+\\S+\\s+\\S+";
		if(!request_line.matches(reg_exp)) {
			generate_error(400, hm, clientSocket);
			return false;
		}
		String[] request_parts = request_line.split("\\s+");
		//Allow method and version parts of request to be in lowercase (Be liberal in accepting rule)
		/*
		if(!request_parts[0].toUpperCase().equals(request_parts[0])) {
			generate_error(400, hm, clientSocket);
			return false;
		}
		 */
		hm.put("method", request_parts[0].toUpperCase());
		hm.put("path", request_parts[1]);
		hm.put("version", request_parts[2].trim().toUpperCase()); 
		if (!validversions.contains(request_parts[2])) { //HTTP version not supported
			generate_error(505, hm, clientSocket);
			return false;
		}
		return true;
	}

	public boolean process_header_fields(ArrayList<String> header_lines, HashMap<String, String> hm, Socket clientSocket) {
		/*A few important considerations for the header fields:
			Allow the header fields to be in lower case too
			Allow multiple spaces or tabs between : and value
			Allow multi lined values. In this case, append the next line to the previous value separated by a space
			Allow same header field multiple times. In this case, append all such values separated by a ','
		 */
		String reg_exp1 = "\\S+:{1}\\s+\\S.*"; //Line in the format "key: value"
		String reg_exp2 = "\\s+\\S+"; //Line in the format " value(continued)" -- Allow multi lined values according to spec
		String[] lines = new String[header_lines.size()];
		lines = header_lines.toArray(lines);
		int i = 0;
		String[] line_parts;
		String temp;
		while (i< header_lines.size()) {
			if (lines[i].toString().matches(reg_exp1)) {
				temp = "";
				line_parts = lines[i].split(":{1}\\s+", 2);
				temp = line_parts[1];
				i += 1;
				if (i != header_lines.size()) { //Multi lines attributes
					while (lines[i].toString().matches(reg_exp2)) {
						temp += lines[i].replaceAll("^\\s+", " ");
						i += 1;
						if (i == header_lines.size())
							break;
					}
				}
				if(!hm.containsKey(line_parts[0])) {
					hm.put(line_parts[0].toLowerCase(), temp);
				}
				else {
					hm.put(line_parts[0].toLowerCase(), hm.get(line_parts[0]) + "," + temp); //Same field multiple times
				}
			}
			else { //Not in a supported format
				generate_error(400, hm, clientSocket);
				return false;
			}
		}
		return true;
	}

	public Entry<String, String> servlet_mapping(HashMap<String,String> hm) {
		String path = hm.get("path");
		String servlet = null;
		int max = 0;
		int len = 0;
		String new_url = null;
		String str = path.split("\\?")[0];
		for (String url : absolute_urls.keySet()) {
			if (url.equals(str)) {
				Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(url, absolute_urls.get(url));
				return entry;
			}
		}
		for (String url : path_urls.keySet()) {
			if (str.contains(url)) {
				len = url.split("/").length;
				if (len > max) {
					servlet = path_urls.get(url);
					new_url = url;
					max = len;
				}
				len = 0;
			}
		}
		Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(new_url, servlet);
		return entry;
	}

	public void servlet_handler(Socket clientSocket, HashMap<String, String> hm, Entry<String,String> url_servlet, BufferedReader in) {
		try {
			Session session = null;
			String request_string = null;
			String body = "";
			Response response = new Response(clientSocket, url_servlet);
			Request request = new Request(context, session, response, clientSocket, hm, url_servlet);
			String servlet = url_servlet.getValue();
			HttpServlet servlet_object = servlets.get(servlet);
			String path = hm.get("path");
			String[] strings = path.split("\\?|&|=");
			for (int j = 1; j < strings.length - 1; j += 2) {
				request.setParameter(strings[j], strings[j+1]);
			}
			if ( hm.get("method").toUpperCase().compareTo("GET") == 0 || hm.get("method").toUpperCase().compareTo("POST") == 0 ) {
				request.setMethod(hm.get("method").toUpperCase());
				int j = 0;
				if (hm.get("method").equals("POST")) {
					int content_l = Integer.parseInt(hm.get("content-length"));
					int ch;
					StringBuffer sb = new StringBuffer();
					ArrayList<String> lines = new ArrayList<String>();
					if (content_l != 0) {
						while((ch = in.read()) != -1) {
							if (j >= content_l-1) {
								break;
							}
							++j;
							if ((char)ch == '\n') {
								lines.add(sb.toString());
								sb = new StringBuffer();
								continue;
							}
							sb.append((char)ch);	
						}
						sb.append((char)ch);
						if (sb.length() != 0) {
							lines.add(sb.toString());
						}
						String[] parts;
						for (String string : lines) {
							body += string;
							parts = string.split("=|&");
							for (int i = 0; i < parts.length-1; i+=2) {
								request.setParameter(parts[i], parts[i+1]);
							}
						}
					}
					request.setBody(body);
				}
				servlet_object.service(request, response);
				response.flushBuffer();
			} else {
				generate_error(501, hm, clientSocket);
			}
		}
		catch(Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void run() {
		while(Server.alive) { //Loop until a command for shutdown was received
			HashMap<String,String> hm = new HashMap<String,String>();
			String servlet = null;
			String request = "";
			Socket clientSocket;
			synchronized(Q) {
				while(Q.isEmpty()) {
					try {
						if(Server.alive == false)
							break; //Stop running when a request for shutdown was received
						Q.wait();
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
					} 
				}
				if(Server.alive == false) //Proceed further only if a request for shutdown was not received
					break;
				clientSocket = Q.remove(); //Remove a request from the queue
				try {
					clientSocket.setSoTimeout(500000);
				} catch (SocketException e) {
					logger.error(e.getMessage());
				}
			}
			try {

				clientSocket.setSoTimeout(500000);
				BufferedReader in;
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				request = in.readLine(); //The first line of the request
				if (request == null) { //Handle null requests by skipping them
					continue;
				}
				if(!validate_line(request, hm, clientSocket)) {
					in.close();
					clientSocket.close();
					continue;
				}
				ArrayList<String> header_lines = new ArrayList<String>();
				while(true) {
					request = in.readLine();
					if (request.length() == 0)
						break;
					header_lines.add(request);
				}
				if(!process_header_fields(header_lines, hm, clientSocket)) {
					in.close();
					clientSocket.close();
					continue;
				}
				Entry<String, String> url_servlet = servlet_mapping(hm);
				servlet = url_servlet.getValue();
				if (servlet != null) { //Handle as a servlet
					if (!validate_case_servlets(hm, clientSocket)) {
						in.close();
						clientSocket.close();
						return;
					}
					servlet_handler(clientSocket, hm, url_servlet, in);
				}
				else {
					generate_response(hm, clientSocket); //Handle as a resource and generate the response
				}
				in.close();
				clientSocket.close();
			}
			catch(SocketTimeoutException e) {
				try {
					clientSocket.close();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			catch(Exception e) {
				e.printStackTrace(); 
				try {
					generate_error(500, hm, clientSocket);
				} 
				catch (Exception e1) {
					logger.error(e.getMessage());
				}
			}
		}
	}

}
