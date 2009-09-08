package edu.iu.grid.oim.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.divrep.DivRep;
import com.divrep.DivRepEvent;
import com.divrep.DivRepEventListener;
import com.divrep.DivRepRoot;
import com.divrep.common.DivRepButton;
import com.divrep.common.DivRepSelectBox;

import edu.iu.grid.oim.lib.StaticConfig;
import edu.iu.grid.oim.model.db.ActionModel;
import edu.iu.grid.oim.model.db.CpuInfoModel;
import edu.iu.grid.oim.model.db.DNModel;
import edu.iu.grid.oim.model.db.LogModel;
import edu.iu.grid.oim.model.db.record.ActionRecord;
import edu.iu.grid.oim.model.db.record.CpuInfoRecord;
import edu.iu.grid.oim.model.db.record.LogRecord;

import edu.iu.grid.oim.view.BreadCrumbView;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivRepWrapper;
import edu.iu.grid.oim.view.GenericView;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.MenuView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.RecordTableView;
import edu.iu.grid.oim.view.SideContentView;

public class ReportRegistrationServlet extends ServletBase implements Servlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(ReportRegistrationServlet.class);  
	
    public ReportRegistrationServlet() {
        // TODO Auto-generated constructor stub
    }
    
    Integer days = 7;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{	
		if(!auth.isLocal()) {
			//allow cron to access
			auth.check("admin");
		}
		
		try {
			String days_str = request.getParameter("days");
			if(days_str != null) {
				days = Integer.parseInt(days_str);
			}
			
			//construct view
			MenuView menuview = new MenuView(context, "admin");
			ContentView contentview = createContentView();
		
			PrintWriter out = response.getWriter();
			if(request.getParameter("plain") != null) {
				contentview.render(out);
			} else {
				//set crumbs
				BreadCrumbView bread_crumb = new BreadCrumbView();
				bread_crumb.addCrumb("Administration",  "admin");
				bread_crumb.addCrumb("Registration Report",  null);
				contentview.setBreadCrumb(bread_crumb);
				
				Page page = new Page(context, menuview, contentview, createSideView());
				page.render(out);			
			}
		} catch (SQLException e) {
			log.error(e);
			throw new ServletException(e);
		}
	}
	
	protected ContentView createContentView() 
		throws ServletException, SQLException
	{	
		ContentView contentview = new ContentView();	
		contentview.add(new HtmlView("<h1>Registration Report</h1>"));
		
		try {
			//DNModel dmodel = new DNModel(context);
			
			//pull log entries that matches the log type
			LogModel lmodel = new LogModel(context);
			Collection<LogRecord> recs = lmodel.getLatest("%", days);
			
			//sort the records
			Collection<LogRecord> resource_recs = new ArrayList<LogRecord>();
			Collection<LogRecord> sc_recs = new ArrayList<LogRecord>();
			Collection<LogRecord> vo_recs = new ArrayList<LogRecord>();
			Collection<LogRecord> user_recs = new ArrayList<LogRecord>();
			for(LogRecord rec : recs) {
				if(!rec.type.equals("insert")) {
					continue;
				}
				if(rec.model.equals("edu.iu.grid.oim.model.db.ResourceModel")) {
					resource_recs.add(rec);
					continue;
				}
				if(rec.model.equals("edu.iu.grid.oim.model.db.SCModel")) {
					sc_recs.add(rec);
					continue;
				}		
				if(rec.model.equals("edu.iu.grid.oim.model.db.VOModel")) {
					vo_recs.add(rec);
					continue;
				}
				if(rec.model.equals("edu.iu.grid.oim.model.db.ContactModel")) {
					user_recs.add(rec);
					continue;
				}
			}
			
			contentview.add(new HtmlView("<h2>Resource Registration</h2>"));
			contentview.add(buildList(StaticConfig.getApplicationBase() + "/resourceedit", 
					resource_recs, "name", "description"));
			
			contentview.add(new HtmlView("<h2>Support Center Registration</h2>"));
			contentview.add(buildList(StaticConfig.getApplicationBase() + "/scedit", 
					sc_recs, "name", "description"));
			
			contentview.add(new HtmlView("<h2>VO Registration</h2>"));
			contentview.add(buildList(StaticConfig.getApplicationBase() + "/voedit", 
					vo_recs, "name", "description"));
			
			contentview.add(new HtmlView("<h2>User Registration</h2>"));
			contentview.add(buildList(StaticConfig.getApplicationBase() + "/contactedit", 
					user_recs, "name", "primary_email"));
			
		} catch(SQLException e) {
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return contentview;
	}
	
	private GenericView buildList(String pageurl, Collection<LogRecord> recs, String header_key, String detail_key) throws ParserConfigurationException
	{
		GenericView view = new GenericView();
    	
		XPath xpath = XPathFactory.newInstance().newXPath();
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	factory.setNamespaceAware(false);
    	factory.setValidating(false);
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	
    	if(recs.size() == 0) {
	    	view.add(new HtmlView("<p>No registration</p>"));
    	}
    	
		for(LogRecord rec : recs) {
	    	//parse log
			byte[] bArray = rec.xml.getBytes();
			ByteArrayInputStream bais = new ByteArrayInputStream(bArray);
			try {
				Document log = builder.parse(bais);
				
				String header = (String)xpath.evaluate("//Field[Name='"+header_key+"']/Value", log, XPathConstants.STRING);
		    	xpath.reset();
		    	
				String detail = (String)xpath.evaluate("//Field[Name='"+detail_key+"']/Value", log, XPathConstants.STRING);
		    	xpath.reset();

				String id = (String)xpath.evaluate("//Key[Name='id']/Value", log, XPathConstants.STRING);
		    	xpath.reset();
		    	
		    	view.add(new HtmlView("<p class=\"right\">"+rec.timestamp+" UTC</p>"));
				view.add(new HtmlView("<a href=\""+pageurl+"?id="+id+"\"><h3>"+header+"</h3></a>"));
				view.add(new HtmlView("<p>"+detail+"</p>"));
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
		return view;
	}
	
	private SideContentView createSideView()
	{
		SideContentView view = new SideContentView();

    	view.add(new HtmlView("<h3>List Types</h3>"));
    	view.add(new HtmlView("<div class=\"indent\">"));
    	LinkedHashMap<Integer, String> keyvalues = new LinkedHashMap<Integer, String>();
		keyvalues.put(7, "Last 1 Week");
		keyvalues.put(14, "Last 2 Week");
		keyvalues.put(30, "Last 1 Month");
		keyvalues.put(365, "Last 1 Year");
		final DivRepSelectBox select = new DivRepSelectBox(context.getPageRoot(), keyvalues);
		select.setLabel("Show Registration For");
		select.setValue(days);
		select.addEventListener(new DivRepEventListener(){
			public void handleEvent(DivRepEvent e) {
				String days = (String)e.value;
				select.modified(false);
				select.redirect(StaticConfig.getApplicationBase() + "/reportregistration?days=" + days);
			}});
		view.add(new DivRepWrapper(select));		
		view.add(new HtmlView("</div>"));

		
		return view;
	}
}
