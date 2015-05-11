package ServletTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import TestHarness.MyResponse;
import TestHarness.MyContainer;
import TestHarness.MyRequest;
import static org.junit.Assert.*;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;

public class ResponseTest {
	static int port;
	static String root;
	ServerSocket servSock;
	Socket sock;
	int i =0;
	StringWriter buf;
	HashMap<String,Object> att = new HashMap<String,Object>();

	MyResponse r;
	MyContainer c;
	@Before
	//Start the server
	public void startServer() {
		c = new MyContainer(){
			public void writeHeader(MyResponse r) {
				System.out.println("Writing to head");
			}
			public void writeBody(MyResponse r) {
				System.out.println("Writing to body");
			}
			public void parseRequest(InputStream i,MyRequest n, HashMap<String,Object> requestParams){
				System.out.println("Parsed Request");
			}
		};

		r = new MyResponse(c){
			public PrintWriter getWriter() {
				buffer = new StringWriter(4096);
				PrintWriter pw = new PrintWriter(buf,false);
				return pw;
			}

		};

	}

	@Test
	public void testreset() {
		MyContainer t = new MyContainer();
		String args[] = new String[3];
		args[0]="/Users/karthikalle/Desktop/CIS 555/Homeworks/ms2/src/hw1/WEB-INF/web.xml";
		args[1] = "GET";
		args[2] = "cookie1";
		att.put("requestVersion", "HTTP/1.1");
		try {
			t.initialize(args[0],null);
			t.doWork(args, sock, att);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		r.buffer = new StringWriter();
		r.reset();
		try {
			r.flushBuffer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testgetheader() {
		MyResponse r = new MyResponse(c);
		r.addHeader("test", "testval");
		assertTrue(r.containsHeader("test"));
	}

	@Test
	public void testAddDateHeaders() {
		MyResponse r = new MyResponse(c);
		Long l1 = System.currentTimeMillis();
		Long l2 = System.currentTimeMillis() - 100000;
		r.setDateHeader("d", l1);
		r.addDateHeader("d", l2);
		assertEquals(r.m_props.get("d"),l1+","+l2);
	}

	@Test
	public void testStatus() {
		MyResponse r = new MyResponse(c);
		r.setStatus(500);
		assertEquals(r.statuscodes.get(500),"Internal Server Error");
		r.setStatus(404);
		assertEquals(r.statuscodes.get(404), "Not Found");
		r.setStatus(403);
		assertEquals(r.statuscodes.get(403),"Forbidden");
		r.setStatus(200);
		assertEquals(r.statuscodes.get(200),"OK");
		r.setStatus(101);
		assertEquals(r.statuscodes.get(101),null);
	}

	@Test
	public void testCharEncoding() {
		MyResponse r = new MyResponse(c);
		assertEquals("ISO-8859-1",r.getCharacterEncoding());
	}

	@Test
	public void testContentType() {
		MyResponse r = new MyResponse(c);
		r.setContentType("text/html");
		assertEquals(r.getContentType(),"text/html");
	}

	@Test
	public void testContentlength() {
		MyResponse r = new MyResponse(c);
		r.setContentLength(1000);
		assertEquals(r.m_props.get("Content-Length"),1000);
	}

	@Test
	public void testResetBufferWhenCommitted() {
		MyResponse r = new MyResponse(c);
		r.isCommitted = true;
		boolean flag = false;
		try {
			r.resetBuffer();
		}
		catch(IllegalStateException e)
		{
			assertTrue(true);
			flag = true;
		}
		if(!flag)
			assertFalse(true);
	}

	@Test
	public void testIsCommitted() throws IOException {
		MyResponse r = new MyResponse(c);
		r.flushBuffer();
		assertTrue(r.isCommitted());
	}

	@Test 
	public void testResetWhenCommitted() {
		MyResponse r = new MyResponse(c);
		r.isCommitted = true;
		boolean flag = false;
		try {
			r.reset();
		}
		catch(IllegalStateException e)
		{
			assertTrue(true);
			flag = true;
		}
		if(!flag)
			assertFalse(true);
	}

	@Test 
	public void testResetWhenNotCommitted() {
		MyResponse r = new MyResponse(c);
		r.buffer = new StringWriter();
		r.isCommitted = false;
		r.m_props.put("1", "1");
		r.setStatus(200);
		boolean flag = false;
		try {
			r.reset();
			assertTrue(r.statuscodes.isEmpty());
			assertTrue(r.m_props.isEmpty());
		}
		catch(IllegalStateException e)
		{
			assertFalse(true);
			flag = true;
		}
		if(!flag)
			assertTrue(true);
	}
	
	@Test
	public void testExpirationDate() {
		MyResponse r = new MyResponse(c);
		Cookie co = new Cookie("1","1");
		System.out.println(r.getExpirationDate(co));
		Calendar now = Calendar.getInstance(); 
		SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US); 
		df.setTimeZone(TimeZone.getTimeZone("GMT")); 
		String date1 = df.format(now.getTimeInMillis());	
		assertEquals(r.getExpirationDate(co),date1);

	}
	
	
	@Test
	public void testAfterFlushBufferIfBufferEmpty() throws IOException {
		MyResponse r = new MyResponse(c);
		r.buffer = new StringWriter();
		PrintWriter p = r.getWriter();
		p.write("asg");
		r.flushBuffer();
		StringWriter s = new StringWriter();
		String str1 = new String();
		String str2 = new String();
		r.buffer.write(str1);
		s.write(str2);
		assertEquals(str1,str2);
		assertTrue(r.isCommitted());
	}
}

