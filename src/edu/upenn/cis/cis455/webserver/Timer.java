package edu.upenn.cis.cis455.webserver;

import java.util.Date;
import org.apache.log4j.Logger;

public class Timer implements Runnable{
	
	static final Logger logger = Logger.getLogger(Timer.class);
	
	public long max_inactive_time;
	public long last_accessed_time;
	long current_time = new Date().getTime();
	
	public void run() {
		try {
			Thread.sleep(10000);
			for (String session_id : Handler.session_map.keySet()) {
				Session session = Handler.session_map.get(session_id);
				max_inactive_time = session.maxinactiveinterval*1000;
				last_accessed_time = session.last_accessed_time;
				if ((current_time - last_accessed_time) > max_inactive_time) {
					session.invalidate();
					Handler.session_map.remove(session_id);
				}
			}
		} 
		catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}
	
}
