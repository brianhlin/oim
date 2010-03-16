package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.divrep.DivRep;
import com.divrep.DivRepEvent;
import com.divrep.DivRepEventListener;
import com.divrep.DivRepRoot;
import com.divrep.common.DivRepButton;
import com.divrep.common.DivRepStaticContent;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.StaticConfig;
import edu.iu.grid.oim.model.Context;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.view.divrep.form.ContactFormDE;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivRepWrapper;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.MenuView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.SideContentView;

public class ProfileEditServlet extends ServletBase implements Servlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(ProfileEditServlet.class);  
	private String parent_page = "home";	
	private ContactFormDE form;
	
    public ProfileEditServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		ContactRecord rec;
		try {
			rec = auth.getContact();
				
			String origin_url = StaticConfig.getApplicationBase()+"/"+parent_page;
			form = new ContactFormDE(context, rec, origin_url, true);
			
			//put the form in a view and display
			ContentView contentview = new ContentView();
			contentview.add(new HtmlView("<h1>Edit Your User Profile</h1>"));	
			if(auth.isDisabledOIMUser()) {
				contentview.add(new HtmlView(auth.getDisabledUserWarning()));
			} 
			contentview.add(new DivRepWrapper(form));
			
			Page page = new Page(context, new MenuView(context, "profileedit"), contentview, createSideView());
			page.render(response.getWriter());	
			
		} catch (SQLException e) {
			throw new ServletException(e);
		}
	}
	
	private SideContentView createSideView() throws SQLException
	{
		SideContentView view = new SideContentView();
		String auth_type_string = "";
		HashSet<String> auth_types = auth.getAuthorizationTypesForCurrentDN();
		for (String auth_type: auth_types) {
			auth_type_string += "<p>" + auth_type + "</p>";
		}
		view.add("About", new HtmlView("This page lets you edit your OIM profil.</p><p>On your OIM profile, you can set contact information like email, phone number and extension, an email address for SMS text messages, and postal address (only applicable to human contacts).</p><p>You can also set your local timezone so other applications like GOCTicket and MyOSG can display timestamps in your local timezone.</p><p>You can also provide a link to an image that you would like to use as your profile picture!"));
		if (auth_type_string == "") 
			auth_type_string = "N/A to de-activated account.";
		view.add("Auth Type(s) For Your Profile", new HtmlView(auth_type_string));		
		return view;
	}
}