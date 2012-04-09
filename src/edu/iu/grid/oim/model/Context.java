package edu.iu.grid.oim.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import org.apache.log4j.Logger;

import com.divrep.DivRepPage;
import com.divrep.DivRepRoot;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.AuthorizationException;

public class Context {
    static Logger log = Logger.getLogger(Context.class);  
    
    private DivRepRoot divrep_root;
	private DivRepPage divrep_pageroot;
	
	private Authorization auth = new Authorization();
	private String request_url;
	private HttpSession session;
	private String remote_addr;
	
	//DB connection that we will reuse everywhere... it's OIM DB 100% of the time..
	private Connection connection = null;

	//stores the reason for current transaction (used for log table)
	//why should this work? because *usually* all update within a session occurs under common purpose.
	private String comment;
	public void setComment(String _comment) { comment = _comment; }
	public String getComment() { return comment; }
	public String getRemoteAddr() { return remote_addr; }
	
	public Context(HttpServletRequest request) throws AuthorizationException
	{	
		//don't store request object because it can get stale really fast... (was causing issue when divrep tries to get session from it)
		//request = _request;	
		
		session = request.getSession();
		auth = new Authorization(request);
		setRequestURL(request);
		divrep_root = DivRepRoot.getInstance(request.getSession());
		divrep_pageroot = divrep_root.initPage(request.getRequestURI() + request.getQueryString());
		remote_addr = request.getRemoteAddr();
	}
	
	protected void finalize() throws Throwable {
	    try {
	        close(); 
	    } finally {
	        super.finalize();
	    }
	}


	public static Context getGuestContext()
	{
		return new Context();
	}
	private Context()
	{
	}

	public void close()
	{
		try {
			if(connection != null) {
				if (!connection.isClosed()) {
					log.info("Closing connection: " + connection.toString());
					connection.close();
					
				}
			}
		} catch (SQLException e) {
			log.info("Failed to reset session for divrep (it's okay if this caused by invalidated session)", e);
		} 
		
		try {
			if(divrep_root != null) {	
				divrep_root.setSession(session);
			}
		} catch (IllegalStateException e) {
			log.info("Failed to reset session for divrep (it's okay if this caused by invalidated session)", e);
		}
	}
	
	/*
	I don't know why, but JDBC's isValid() function doesn't work.. When I use it I get following exception
	
	java.lang.AbstractMethodError: org.apache.tomcat.dbcp.dbcp.PoolingDataSource$PoolGuardConnectionWrapper.isValid(I)Z
	at edu.iu.grid.oim.model.Context.isConnectionValid(Context.java:95)

	 */
	public boolean isConnectionValid() {
		if (connection == null)
			return false;
		
		//my version of isValid()... since JDBC.isValid() doesn't work!
		try {
			if (connection.isClosed()) {
				//log.warn("OIM connection is closed...");
				return false;
			}
			connection.getMetaData();
		} catch (Exception e) {
			//log.warn("OIM connection is stale...");
			return false;
		}
		return true;
		
	}
	
	
	//make sure to close the connection as soon as you are done (inside the same function that you call connectOIM)
	public Connection connect(String ctx) throws SQLException
	{	
		if(isConnectionValid()) {
			return connection;
		}
		
		//reconnect
		try {
			javax.naming.Context initContext = new InitialContext();
			javax.naming.Context envContext = (javax.naming.Context)initContext.lookup("java:/comp/env");
			DataSource ds = (DataSource)envContext.lookup(ctx);
			connection = ds.getConnection();
			log.info("Requesting new db connection: context=" + ctx + " connection=" + connection.toString() + " for " + auth.getUserDN());
			initContext.close();
			
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			
			return connection;
		} catch (NamingException e) {
			log.error(e);
		}
		return null;
	}
	
	public Authorization getAuthorization()
	{
		return auth;
	}
	public DivRepPage getPageRoot()
	{
		return divrep_pageroot;
	}
	
	public String getRequestURL()
	{
		return request_url;
	}

	
	private void setRequestURL(HttpServletRequest request) {
		request_url = "";
		if(request != null) {
			request_url += request.getRequestURI();
			if(request.getQueryString() != null) {
				request_url += "?" + request.getQueryString();
			}
		}
	}
}
