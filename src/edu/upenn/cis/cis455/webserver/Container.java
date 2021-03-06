package edu.upenn.cis.cis455.webserver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.print.attribute.standard.Severity;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Container {

	static final Logger logger = Logger.getLogger(Container.class);

	Handler h;
	public static HashMap<String,HttpServlet> servlets = new HashMap<String,HttpServlet>();
	HashMap<String,String> contextParams = new HashMap<String,String>();
	HashMap<String, String> servletMapping = new HashMap<String,String>();
	HashMap<String,HashMap<String,String>> servletParams = new HashMap<String,HashMap<String,String>>();
	Context context;

	static class Handler extends DefaultHandler {

		private int m_state = 0;
		private String m_servletName;
		private String m_paramName;
		private String m_displayName = null;
		HashMap<String,String> m_servlets = new HashMap<String,String>();
		HashMap<String,String> m_contextParams = new HashMap<String,String>();
		HashMap<String, String> m_servletMapping = new HashMap<String,String>();
		HashMap<String,HashMap<String,String>> m_servletParams = new HashMap<String,HashMap<String,String>>();

		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if (qName.compareTo("servlet-name") == 0) {
				m_state = (m_state == 5) ? 6 : 1;
			} else if (qName.compareTo("servlet-class") == 0) {
				m_state = 2;
			} else if (qName.compareTo("context-param") == 0) {
				m_state = 3;
			} else if (qName.compareTo("init-param") == 0) {
				m_state = 4;
			} else if (qName.compareTo("servlet-mapping") == 0) {
				m_state = 5;
			} else if (qName.compareTo("url-pattern") == 0) {
				m_state = 7;
			} else if (qName.compareTo("display-name") == 0) {
				m_state = 8;
			} else if (qName.compareTo("param-name") == 0) {
				m_state = (m_state == 3) ? 10 : 20;
			} else if (qName.compareTo("param-value") == 0) {
				m_state = (m_state == 10) ? 11 : 21;
			}
		}

		public void characters(char[] ch, int start, int length) {
			String value = new String(ch, start, length);
			if (m_state == 1) {
				m_servletName = value;
				m_state = 0;
			} else if (m_state == 2) {
				m_servlets.put(m_servletName, value);
				//m_servletName = null;
				m_state = 0;
			} else if (m_state == 6) {
				m_servletName = value;
				m_state = 0;
			} else if (m_state == 7) {
				if (m_servletName == null) {
					System.err.println("URL pattern value '" + value + "' without servlet name");
					System.exit(-1);
				}
				m_servletMapping.put(value, m_servletName);
				m_state = 0;
			} else if (m_state == 8) {
				m_displayName = value;
				m_state = 0;
			} else if (m_state == 10 || m_state == 20) {
				m_paramName = value;
			} else if (m_state == 11) {
				if (m_paramName == null) {
					System.err.println("Context parameter value '" + value + "' without name");
					System.exit(-1);
				}
				m_contextParams.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			} else if (m_state == 21) {
				if (m_paramName == null) {
					System.err.println("Servlet parameter value '" + value + "' without name");
					System.exit(-1);
				}
				HashMap<String,String> p = m_servletParams.get(m_servletName);
				if (p == null) {
					p = new HashMap<String,String>();
					m_servletParams.put(m_servletName, p);
				}
				p.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			} 
		}
	}

	private static Handler parseWebdotxml(String webdotxml) throws ParserConfigurationException, SAXException, IOException {
		Handler h = new Handler();
		File file = new File(webdotxml);
		if (file.exists() == false) {
			System.err.println("error: cannot find " + file.getPath());
			System.exit(-1);
		}
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(file, h);
		return h;
	}

	private static Context createContext(Handler h) {
		Context context;
		if (h.m_displayName != null) 
			context = new Context(h.m_displayName);
		else
			context = new Context();
		for (String param : h.m_contextParams.keySet()) {
			context.setInitParam(param, h.m_contextParams.get(param));
		}
		return context;
	}

	private static HashMap<String,HttpServlet> createServlets(Handler h, Context context) {
		HashMap<String,HttpServlet> servlets = new HashMap<String,HttpServlet>();
		for (String servletName : h.m_servlets.keySet()) {
			Config config = new Config(servletName, context);
			String className = h.m_servlets.get(servletName);
			try {
				Class servletClass = Class.forName(className);
				HttpServlet servlet = (HttpServlet) servletClass.newInstance();


				HashMap<String,String> servletParams = h.m_servletParams.get(servletName);
				if (servletParams != null) {
					for (String param : servletParams.keySet()) {
						config.setInitParam(param, servletParams.get(param));
					}
				}
				servlet.init(config);
				servlets.put(servletName, servlet);
			}
			catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return servlets;
	}

	public Container() throws ParserConfigurationException, SAXException, IOException {
		h = parseWebdotxml(Server.webdotxml);
		context = createContext(h);
		servlets = createServlets(h, context);		
	}

}