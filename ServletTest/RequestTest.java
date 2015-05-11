package ServletTest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import TestHarness.MyContainer;
import TestHarness.MyRequest;
import TestHarness.MyResponse;
import TestHarness.MySession;
import edu.upenn.cis455.webserver.HttpServer;

public class RequestTest {
	MyRequest r = null;
	Socket sock;
	HashMap<String,Object> att = new HashMap<String,Object>();


	MyContainer c;

	@Before
	public void testRequestParams() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "init/asg/asgh";
		c = new MyContainer(){
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
					//		System.out.println("requestURL"+requestURL[0]);
					String totalPath = strings[0];
					System.out.println("totp"+totalPath);

					String[] urlpath = totalPath.split("/");
					//System.out.println(urlpath[0]);
					request.setAttribute("servlet-path", urlpath[0]);

					if(urlpath.length>=2)
						request.setAttribute("path-info", totalPath.substring(urlpath[0].length()));
					//	System.out.println("servlet-name "+urlpath[0]);
					//		System.out.println("path-info "+request.getAttribute("path-info"));
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
					if (args[i].compareTo("GET") == 0 || args[i].compareTo("POST") == 0) {
						System.out.println("here");
						request.setMethod(args[i]);
					}


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
		}
		catch(Exception e){
			System.out.println(e);
		}
	}

	@Test
	public void testOnlyServletPath() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "init";
		try{
			c.doWork(args, sock, new HashMap<String,Object>());

			System.out.println(args[2]);
			System.out.println("RequestURI:"+r.getRequestURI());
			assertEquals("init",r.getRequestURI().toString());
			System.out.println("RequestURL:"+r.getRequestURL().toString());
			assertEquals("http://localhost:7777/init",r.getRequestURL().toString());
			System.out.println("PathInfo:"+r.getPathInfo());
			assertEquals(null,r.getPathInfo());
			System.out.println("QueryString"+r.getQueryString());
			assertEquals(null,r.getQueryString());
			System.out.println("\n");
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testnoQuery() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "init/asg/asg/dh";
		try{
			c.doWork(args, sock, new HashMap<String,Object>());

			System.out.println(args[2]);
			System.out.println("RequestURI:"+r.getRequestURI());
			assertEquals("init/asg/asg/dh",r.getRequestURI());
			System.out.println("RequestURL:"+r.getRequestURL().toString());
			assertEquals("http://localhost:7777/init/asg/asg/dh",r.getRequestURL().toString());
			System.out.println("PathInfo:"+r.getPathInfo());
			assertEquals("/asg/asg/dh",r.getPathInfo());
			System.out.println("QueryString"+r.getQueryString());
			assertEquals(null,r.getQueryString());
			System.out.println("\n");

		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPathandQuery() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "demo/tet/jhj.html?a=10&b=c";
		try{
			c.doWork(args, sock, new HashMap<String,Object>());

			System.out.println(args[2]);
			System.out.println("RequestURI:"+r.getRequestURI());
			assertEquals("demo/tet/jhj.html",r.getRequestURI());
			System.out.println("RequestURL:"+r.getRequestURL());
			assertEquals("http://localhost:7777/demo/tet/jhj.html/a=10&b=c",r.getRequestURL().toString());
			System.out.println("PathInfo:"+r.getPathInfo());
			assertEquals("/tet/jhj.html",r.getPathInfo());
			System.out.println("QueryString"+r.getQueryString());
			assertEquals("a=10&b=c",r.getQueryString());
			System.out.println("\n");

		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testOnlyQueryNoPath() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "demo/?a=10&b=c";
		try{
			c.doWork(args, sock, new HashMap<String,Object>());

			System.out.println(args[2]);
			System.out.println("RequestURI:"+r.getRequestURI());
			assertEquals("demo",r.getRequestURI());
			System.out.println("RequestURL:"+r.getRequestURL().toString());
			assertEquals("http://localhost:7777/demo/a=10&b=c",r.getRequestURL().toString());
			System.out.println("PathInfo:"+r.getPathInfo());
			assertEquals(null,r.getPathInfo());
			System.out.println("QueryString"+r.getQueryString());
			assertEquals("a=10&b=c",r.getQueryString());
			System.out.println("\n");

		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testonlypath() {
		String[] args = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "demo/sdt/sdt/qwr.html";
		try{
			c.doWork(args, sock, new HashMap<String,Object>());

			System.out.println(args[2]);
			System.out.println("RequestURI:"+r.getRequestURI());
			assertEquals("demo/sdt/sdt/qwr.html",r.getRequestURI());
			System.out.println("RequestURL:"+r.getRequestURL());
			assertEquals("http://localhost:7777/demo/sdt/sdt/qwr.html",r.getRequestURL().toString());
			System.out.println("PathInfo:"+r.getPathInfo());
			assertEquals("/sdt/sdt/qwr.html",r.getPathInfo());
			System.out.println("QueryString"+r.getQueryString());
			assertEquals(null,r.getQueryString());
			System.out.println("\n");
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testGetMethod() {
		MyResponse res = new MyResponse(c);
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "/cookie1";

		HashMap<String,Object> att = new HashMap<String,Object>();
		att.put("test", "Fri, Wed, Thur");
		att.put("Test","testvalue1, testValue2");
		try {
			c.doWork(args, sock, new HashMap<String,Object>());
			c.parseRequest(null, r, att);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(r.getMethod(), "GET");
	}

	@Test
	public void testGetMultiValues() {
		MyResponse res = new MyResponse(c);
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "/cookie1";

		att.put("test", "Fri, Wed, Thur");
		att.put("Test","testvalue1, testValue2");
		try {
			c.doWork(args, sock, new HashMap<String,Object>());
			c.parseRequest(null, r, att);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(r.getAttribute("test"), "Fri, Wed, Thur");
		assertEquals(r.getAttribute("Test"), "testvalue1, testValue2");

	}

	@Test
	public void testAuthType() {
		r = new MyRequest(null,null);
		assertEquals(HttpServletRequest.BASIC_AUTH,r.getAuthType());
	}
	

	@Test
	public void testgetCookie() {
		MyResponse res = new MyResponse(c){
			public PrintWriter getWriter() {
				buffer = new StringWriter(4096);
				PrintWriter pw = new PrintWriter(buffer,false);
				return pw;
			}
		};
		r = new MyRequest(null,res);
		Cookie co = new Cookie("a","1");
		co.setMaxAge(25600);
		res.addCookie(co);

		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "/cookie1";

		att.put("Cookie", "a=1");
		try {
			c.parseRequest(null, r, att);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Cookie[] a = r.getCookies();
		for(int i = 0; i<a.length; i++){
			assertEquals(a[i].getName(),co.getName());
			assertEquals(a[i].getValue(),co.getValue());
		}

	}


}
