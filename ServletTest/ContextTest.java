package ServletTest;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import TestHarness.MyServletContext;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ContextTest {
	MyServletContext myContext;
	@Before
	public void initialize() {
		myContext = new MyServletContext();
	}
	@Test
	public void testGetAttributeNull() {
		assertNull(myContext.getAttribute("a"));
	}
	
	@Test
	public void testGetAttribute() {
		myContext.setAttribute("a", "aaa");
		assertEquals("aaa",myContext.getAttribute("a"));
	}
	
	@Test
	public void testGetAttributeNames() {
		myContext.setAttribute("b", "bbb");
		myContext.setAttribute("c", "ccc");
		myContext.setAttribute("d", "ddd");
		Enumeration a = myContext.getAttributeNames();
		while(a.hasMoreElements()){
			String t = (String) a.nextElement();
			assertTrue(t.equals("b")||t.equals("c")||t.equals("d"));
		}
	}
	
	@Test
	public void testRemoveAttributes() {
		myContext.setAttribute("a", "A");
		myContext.setAttribute("b", "A");
		myContext.removeAttribute("a");
		myContext.removeAttribute("b");
		assertFalse(myContext.getAttributeNames().hasMoreElements());
	}
	
	@Test
	public void testParams() {
		myContext.setInitParam("a", "10");
		assertEquals(myContext.getInitParameter("a"),"10");
	}
}

