package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.LogModel;
import edu.iu.grid.oim.model.db.SCModel;
import edu.iu.grid.oim.model.db.record.LogRecord;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.view.divrep.form.SCFormDE;
import edu.iu.grid.oim.view.BootBreadCrumbView;
import edu.iu.grid.oim.view.BootMenuView;
import edu.iu.grid.oim.view.BootPage;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivRepWrapper;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.LogView;
import edu.iu.grid.oim.view.SideContentView;

public class SCEditServlet extends ServletBase implements Servlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(SCEditServlet.class);  
	private String parent_page = "sc";	

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		UserContext context = new UserContext(request);
		Authorization auth = context.getAuthorization();
		auth.check("edit_my_sc");
		
		SCRecord rec;
		ArrayList<LogRecord> logs = null;

		//if sc_id is provided then we are doing update, otherwise do new.
		String sc_id_str = request.getParameter("id");
		if(sc_id_str != null) {
			//check authorization
			int sc_id = Integer.parseInt(sc_id_str);
			SCModel model = new SCModel(context);			
			if(!model.canEdit(sc_id)) {
				response.sendRedirect("sc?id="+sc_id);
			}
			
			try {
				SCRecord keyrec = new SCRecord();
				keyrec.id = sc_id;
				rec = model.get(keyrec);
				
				//pull logs
				LogModel logmodel = new LogModel(context);
				logs = logmodel.search("edu.iu.grid.oim.model.db.SC%", String.valueOf(sc_id));
			} catch (SQLException e) {
				throw new ServletException(e);
			}	
		} else {
			rec = new SCRecord();
		}
	
		SCFormDE form;
		try {
			form = new SCFormDE(context, rec, parent_page);
		} catch (SQLException e) {
			throw new ServletException(e);
		}
		
		//put the form in a view and display
		ContentView contentview = new ContentView(context);
		if(rec.active != null && rec.active == false) {
			contentview.add(new HtmlView("<div class=\"alert\">This Support Center is currently inactive.</div>"));
		}
		if(rec.disable != null && rec.disable == true) {
			contentview.add(new HtmlView("<div class=\"alert\">This Support Center is currently disabled.</div>"));
		}
		contentview.add(new DivRepWrapper(form));
		
		//setup crumbs
		BootBreadCrumbView bread_crumb = new BootBreadCrumbView();
		bread_crumb.addCrumb("Support Center",  parent_page);
		bread_crumb.addCrumb(rec.name,  null);
		contentview.setBreadCrumb(bread_crumb);
		
		BootPage page = new BootPage(context, new BootMenuView(context, parent_page), contentview, createSideView(logs, rec));		
		page.render(response.getWriter());	
	}
	
	private SideContentView createSideView(ArrayList<LogRecord> logs, SCRecord rec)
	{
		SideContentView view = new SideContentView();
		if(rec.id != null) {
			view.add(new HtmlView("<p><a class=\"btn\" href=\"sc?id="+rec.id+"\">Show Readonly View</a></p>"));
		}
		view.addContactNote();		
		if(logs != null) {
			view.add(new LogView(logs));	
		}
		return view;
	}
}