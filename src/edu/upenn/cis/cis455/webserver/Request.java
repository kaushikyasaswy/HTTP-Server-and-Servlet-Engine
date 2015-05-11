package edu.upenn.cis.cis455.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

class Request implements HttpServletRequest {
	
	static final Logger logger = Logger.getLogger(Request.class);

	public Socket clientSocket;
	public HashMap<String,String> hm;
	private Properties m_params = new Properties();
	private Properties m_props = new Properties();
	private Session m_session = null;
	private String m_method;
	private String m_encoding = "ISO-8859-1";
	public String url = null;
	public String servlet = null;
	public Response response;
	public Context context;
	public boolean is_session_from_cookie = false;
	String body = null;
	Locale locale;
	Request() {
	}

	/*Request(Session session) {
		m_session = session;
	}

	Request(Socket client, Response r, HashMap<String, String> hash, Entry<String, String> entry) {
		clientSocket = client;
		hm = hash;
		m_method = hm.get("method");
		url = entry.getKey();
		servlet = entry.getValue();	
		response =r;
	}*/

	Request(Context c, Session session, Response r, Socket client, HashMap<String, String> hash, Entry<String, String> entry) {
		clientSocket = client;
		hm = hash;
		m_session = session;
		m_method = hm.get("method");
		url = entry.getKey();
		servlet = entry.getValue();
		response = r;
		context = c;
	}

	public String getAuthType() {
		return BASIC_AUTH;
	}

	public Cookie[] getCookies() {
		String cookie = hm.get("cookie");
		if (cookie == null) {
			return null;
		}
		String[] cookie_parts = cookie.split(";");
		int length = cookie_parts.length;
		Cookie[] cookies = new Cookie[length];
		for (int i=0; i<length; i++) {
			String[] each_cookie = cookie_parts[i].split("=");
			if (each_cookie[0].contains("jsessionid")) {
				for (String id : Handler.session_map.keySet()) {
					if (each_cookie[1].equals(id)) {
						m_session = Handler.session_map.get(id);
					}
				}
			}
			Cookie cookie_new = new Cookie(each_cookie[0].trim(), each_cookie[1]);
			cookies[i] = cookie_new;
		}
		return cookies;
	}

	public long getDateHeader(String arg0) {
		if (hm.containsKey(arg0.toLowerCase())) {
			String head_date = hm.get(arg0.toLowerCase());
			SimpleDateFormat f1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			SimpleDateFormat f2 = new SimpleDateFormat("E, dd-MMM-yyyy HH:mm:ss zzz");
			SimpleDateFormat f3 = new SimpleDateFormat("E MMM d HH:mm:ss yyyy");
			Date header_date;
			try {
				if (head_date.contains(","))
					if (head_date.split(",")[0].length() == 3)
						header_date = f1.parse(head_date);
					else
						header_date = f2.parse(head_date);
				else
					header_date = f3.parse(head_date);
				return header_date.getTime();
			}
			catch (Exception e) {
				logger.error(e.getMessage());
				throw new IllegalArgumentException();
			}
		}
		else
			return -1;
	}

	public String getHeader(String arg0) {
		if (hm.containsKey(arg0.toLowerCase())) {
			if (hm.get(arg0.toLowerCase()).contains(",")) {
				return hm.get(arg0.toLowerCase()).split("\\,")[0].trim();
			}
			else
				return hm.get(arg0.toLowerCase());
		}
		else
			return null;
	}

	public Enumeration<String> getHeaders(String arg0) {
		ArrayList<String> strings = new ArrayList<String>();
		if (hm.containsKey(arg0.toLowerCase())) {
			for (String str : hm.get(arg0.toLowerCase()).split("\\,")) {
				strings.add(str);
			}
			Enumeration<String> headers = Collections.enumeration(strings);
			return headers;
		}
		else {
			return Collections.emptyEnumeration();
		}
	}

	public Enumeration<String> getHeaderNames() {
		ArrayList<String> strings = new ArrayList<String>();
		if (hm.isEmpty()) {
			return Collections.emptyEnumeration();
		}
		else {
			for (String str : hm.keySet()) 
				strings.add(str);
			Enumeration<String> headers = Collections.enumeration(strings);
			return headers;
		}
	}

	public int getIntHeader(String arg0) {
		if (!hm.containsKey(arg0.toLowerCase()))
			return -1;
		String str = hm.get(arg0.toLowerCase());
		try {
			int header_int = Integer.parseInt(str);
			return header_int;
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			throw new NumberFormatException();
		}
	}

	public String getMethod() {
		return m_method;
	}

	public String getPathInfo() {
		String path = hm.get("path");
		String ret = null;
		if (!path.contains("?") && path.equals(url))
			return null;
		else if ((path.substring(0, path.indexOf("?"))).equals(url)) {
			return null;
		}
		else {
			System.out.println(url);
			ret = path.substring(path.lastIndexOf(url)+url.length(), path.indexOf("?"));
			if (!ret.startsWith("/"))
				ret = "/" + ret;
			return ret;
		}
	}

	//Not Needed
	public String getPathTranslated() {
		/*String extra_path = this.getPathInfo();
		if (extra_path == null)
			return null;
		else
			return (Server.directory + hm.get("path").substring(0, hm.get("path").lastIndexOf(url) + url.length()) + extra_path).replace("//", "/");
		 */
		return null;
	}

	public String getContextPath() {
		return "";
	}

	public String getQueryString() {
		String path = hm.get("path");
		if (!path.contains("?"))
			return null;
		else
			return path.split("\\?")[1];
	}

	//Authentication not needed, hence return null
	public String getRemoteUser() {
		return null;
	}

	//Not needed
	public boolean isUserInRole(String arg0) {
		return false;
	}

	//Not needed
	public Principal getUserPrincipal() {
		return null;
	}

	public String getRequestedSessionId() {
		return m_session.getId();
	}

	public String getRequestURI() {
		String path = hm.get("path");
		if (path.startsWith("http://")) {
			path = path.substring(7);
			path = path.split("/",2)[1];
		}
		return path.split("\\?")[0];
	}

	public StringBuffer getRequestURL() {
		StringBuffer url = new StringBuffer();
		url.append("http://" + clientSocket.getInetAddress().getHostName() + ":" + clientSocket.getPort());
		if(getRequestURI() !=null )
			url.append("/" + getRequestURI());
		if(getQueryString()!=null)
			url.append("?"+getQueryString());
		return url;
	}

	//TODO
	public String getServletPath() {
		return url;
	}

	public HttpSession getSession(boolean arg0) {
		if(response.isCommitted())
			throw new IllegalStateException();
		getCookies();
		if (arg0) {
			if (! hasSession()) {
				is_session_from_cookie = false;
				long last_accessed_time = new Date().getTime();
				String session_id = new Integer(Handler.random.nextInt()).toString();
				m_session = new Session(session_id, last_accessed_time, context);
				m_session.putValue("isnew", "true");
				Handler.session_map.put(session_id, m_session);
				Handler.session_last_accessed_times.put(m_session, new Date().getTime());
			}
			else {
				is_session_from_cookie = true;
				m_session.putValue("isnew", "false");
				Handler.session_last_accessed_times.put(m_session, new Date().getTime());
				return m_session;
			}
			Cookie c = new Cookie("jsessionid", m_session.getId());
			c.setMaxAge(4000);
			response.addCookie(c);
			return m_session;
		} else {
			if (! hasSession()) {
				m_session = null;
			}
		}
		return m_session;
	}

	public HttpSession getSession() {
		return getSession(true);
	}

	public boolean isRequestedSessionIdValid() {
		return m_session.isValid();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return is_session_from_cookie;
	}

	public boolean isRequestedSessionIdFromURL() {
		return !is_session_from_cookie;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return !is_session_from_cookie;
	}

	public Object getAttribute(String arg0) {
		return m_props.get(arg0);
	}

	public Enumeration getAttributeNames() {
		return m_props.keys();
	}

	public String getCharacterEncoding() {
		return m_encoding;
	}

	public void setCharacterEncoding(String arg0) {
		m_encoding = arg0;
	}

	public int getContentLength() {
		if (hm.containsKey("content-length")) {
			return Integer.parseInt(hm.get("content-length"));
		}
		else {
			return -1;
		}
	}

	public String getContentType() {
		if (hm.containsKey("content-type")) {
			return hm.get("content-type");
		}
		else {
			return null;
		}
	}

	//Not Needed
	public ServletInputStream getInputStream() {
		return null;
	}

	public String getParameter(String arg0) {
		return m_params.getProperty(arg0);
	}

	public Enumeration getParameterNames() {
		return m_params.keys();
	}

	public String[] getParameterValues(String arg0) {
		String value = m_params.getProperty(arg0);
		if(value == null)
			return null;
		String values[] = value.split(",");
		return values;
	}

	public Map getParameterMap() {
		Map<String,String> param_map = new HashMap<String,String>();
		Set<Object> params = m_params.keySet();
		for (Object param : params) {
			param_map.put(param.toString(), m_params.getProperty(param.toString()));
		}
		return param_map;
	}

	public String getProtocol() {
		return hm.get("version");
	}

	public String getScheme() {
		return "http";
	}

	public String getServerName() {
		if (hm.containsKey("host"))
			return hm.get("host").split(":")[0];
		else {
			return clientSocket.getInetAddress().getHostName();
		}
	}

	public int getServerPort() {
		if (hm.containsKey("host"))
			return Integer.parseInt(hm.get("host").split(":")[1]);
		else {
			return clientSocket.getPort();
		}
	}

	public BufferedReader getReader() {
		BufferedReader br;
		try {
		br = new BufferedReader(new StringReader(body));
		}
		catch(Exception e) {
			logger.error(e.getMessage());
			return null;
		}
		return br;
	}

	public String getRemoteAddr() {
		return clientSocket.getLocalAddress().getHostAddress();
	}

	public String getRemoteHost() {
		return clientSocket.getLocalAddress().getHostName() + ":" + clientSocket.getLocalPort();
	}

	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		m_props.remove(arg0);
	}

	public Locale getLocale() {
		return locale;
	}

	//Not needed
	public Enumeration getLocales() {
		return null;
	}

	public boolean isSecure() {
		return false;
	}

	//Not Needed
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	//Depreciated
	public String getRealPath(String arg0) {
		return null;
	}

	public int getRemotePort() {
		return clientSocket.getLocalPort();
	}

	public String getLocalName() {
		return clientSocket.getInetAddress().getHostName();
	}

	public String getLocalAddr() {
		return clientSocket.getInetAddress().getHostAddress();
	}

	public int getLocalPort() {
		return clientSocket.getPort();
	}

	void setMethod(String method) {
		m_method = method;
	}

	//For adding the parameters from Handler
	void setParameter(String key, String value) {
		m_params.setProperty(key, value);
	}

	void clearParameters() {
		m_params.clear();
	}

	//Check if a session already exists
	boolean hasSession() {
		return ((m_session != null) && m_session.isValid());
	}
	
	public void setBody(String arg0) {
		body = arg0;
	}

}
