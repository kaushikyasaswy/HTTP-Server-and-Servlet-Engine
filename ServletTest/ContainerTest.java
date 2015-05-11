package ServletTest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis455.webserver.HttpServer;
import TestHarness.MyContainer;
import TestHarness.MyRequest;
import TestHarness.MyResponse;
import TestHarness.MyServletConfig;
import TestHarness.MyServletContext;
import TestHarness.MySession;

public class ContainerTest {

	static int port;
	static String root;
	ServerSocket servSock;
	Socket sock;
	int i =0;
	MyRequest r = null;


	MyContainer t;

	@Before
	//Start the server
	public void startServer() {
		Logger log = Logger.getLogger(HttpServer.class.getName());
		t = new MyContainer();
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		try {
			t.initialize(args[0], log);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testServletsList() throws Exception {
		assertFalse(t.servlets.isEmpty());
		assertTrue(t.servlets.containsKey("init"));
		assertTrue(t.servlets.containsKey("busy"));
		assertTrue(t.servlets.containsKey("cookie1"));
		assertTrue(t.servlets.containsKey("cookie2"));
		assertTrue(t.servlets.containsKey("cookie3"));
		assertTrue(t.servlets.containsKey("session1"));
		assertTrue(t.servlets.containsKey("session2"));
		assertTrue(t.servlets.containsKey("session3"));
	}

	@Test
	public void testContextParams() {
		Enumeration s = HttpServer.servletContext.getInitParameterNames();
		while(s.hasMoreElements())
			System.out.println(HttpServer.servletContext.getAttribute(s.nextElement().toString()));
	}

	@Test
	public void testServletConfig() {
		Servlet serv = t.servlets.get("init");
		MyServletConfig config = (MyServletConfig) serv.getServletConfig();
		assertEquals(config.getInitParameter("TestParam"),"1776");
	}

	@Test
	public void testNoServletConfig() {
		Servlet serv = t.servlets.get("busy");
		MyServletConfig config = (MyServletConfig) serv.getServletConfig();
		assertEquals(config.getInitParameter("TestParam"),null);
	}

	@Test
	public void testContext() {
		Servlet serv = t.servlets.get("busy");
		MyServletConfig config = (MyServletConfig) serv.getServletConfig();
		MyServletContext context = HttpServer.servletContext;
		assertNotEquals(context,null);
		assertEquals((Object)"kalle@seas.upenn.edu",context.getInitParameter("webmaster"));
	}

	@Test
	public void testServletAbsent() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "testServ";
		try {
			System.out.println(t.doWork(args, sock, new HashMap<String,Object>()));
			assertFalse(t.doWork(args, sock, new HashMap<String,Object>()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testServletPresent() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "init";
		try {
			assertTrue(t.doWork(args, sock, new HashMap<String,Object>()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testShutdown() {
		t.performShutDown();
		for(String s: t.servlets.keySet())
			assert(t.servlets.get(s).getClass().equals(NullPointerException.class));
		assertTrue(t.servlets.isEmpty());	
	}

	@Test
	public void testRequestUrl() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "init/asg/asgh";
		MyContainer c = new MyContainer(){
			public boolean doWork(String[] args, Socket sock, HashMap<String,Object> att)
					throws Exception, IOException, ServletException {
				Socket s;

				s = sock;
				HashMap<String, Object> requestParams = att;

				MySession fs = null;


				MyResponse response = new MyResponse(this);
				MyRequest request = new MyRequest(fs,response);
				for (int i = 1; i < args.length - 1; i += 2) {

					//System.out.println(args[i+1]+ "  "+i);
					String[] strings = args[i+1].split("\\?|&|=");

					/*
					 * Added for getting Servlet Name 
					 * and path info
					 * and requestURL
					 */
					String[] requestURL = args[i+1].split("[?]");
					request.setAttribute("requestURL", requestURL[0]);
					//	System.out.println("requestURL"+requestURL[0]);
					String totalPath = strings[0];
					//	System.out.println("totp"+totalPath);

					String[] urlpath = totalPath.split("/");
					//System.out.println(urlpath[0]);

					if(urlpath.length>=2)
						request.setAttribute("path-info", totalPath.substring(urlpath[0].length()));
					//System.out.println("servlet-name "+urlpath[0]);
					//	System.out.println("path-info "+request.getAttribute("path-info"));
					if(strings.length>=2)
						request.setAttribute("query-string", args[i+1].substring(totalPath.length()+1));
					//	System.out.println("query-string "+request.getAttribute("query-string"));
					//	System.out.println("requestURL "+request.getAttribute("requestURL"));
					//Till here

					HttpServlet servlet = servlets.get(urlpath[0]);
					//There is no servlet for that request
					if (servlet == null) {
						System.err.println("error: cannot find mapping for servlet " + strings[0]);
						//System.exit(-1);
					}
					response.setHeader("requestVersion", requestParams.get("requestVersion").toString());
					r = request;
				}
				return true;			
			}
		};
		String[] arg = new String[2];
		arg[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		Logger log = Logger.getLogger(HttpServer.class.getName());
		try {
			c.initialize(arg[0],log);
			c.doWork(args, sock, new HashMap<String,Object>());
			
			r.getRequestURI();
			r.getRequestURL();
			r.getPathInfo();
			r.getQueryString();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
