package edu.upenn.cis.cis455.webserver;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

class Session implements HttpSession {
	
	private Properties m_props = new Properties();
	private boolean m_valid = true;
	public long creation_time = 0;
	public String session_id = "";
	public long last_accessed_time = 0;
	Context servlet_context;
	int maxinactiveinterval = 0;

	public Session(String id, long last_accessed, Context context) {
		creation_time = new Date().getTime();
		session_id = id;
		last_accessed_time = last_accessed;
		servlet_context = context;
	}
	
	public long getCreationTime() {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		return creation_time;
	}

	public String getId() {
		return session_id;
	}
	
	public long getLastAccessedTime() {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		return Handler.session_last_accessed_times.get(this);
	}

	public ServletContext getServletContext() {
		return servlet_context;
	}

	public void setMaxInactiveInterval(int arg0) {
		maxinactiveinterval = arg0;
	}

	public int getMaxInactiveInterval() {
		return maxinactiveinterval;
	}

	public HttpSessionContext getSessionContext() {
		return null;
	}

	public Object getAttribute(String arg0) {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		return m_props.get(arg0);
	}

	public Object getValue(String arg0) {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		return m_props.get(arg0);
	}

	public Enumeration getAttributeNames() {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		return m_props.keys();
	}

	//Depreciated
	public String[] getValueNames() {
		return null;
	}

	public void setAttribute(String arg0, Object arg1) {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		m_props.put(arg0, arg1);
	}

	public void putValue(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		m_props.remove(arg0);
	}

	//Depreciated
	public void removeValue(String arg0) {
		m_props.remove(arg0);
	}

	public void invalidate() {
		if (m_valid == false) {
			throw new IllegalStateException();
		}
		m_valid = false;
		Handler.session_map.remove(this.getId());
	}

	public boolean isNew() {
		return (boolean)m_props.get("isnew");
	}

	boolean isValid() {
		return m_valid;
	}
}
