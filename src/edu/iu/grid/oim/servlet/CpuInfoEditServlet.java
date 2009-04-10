package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.webif.divex.DivExRoot;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.db.CpuInfoModel;
import edu.iu.grid.oim.model.db.record.CpuInfoRecord;
import edu.iu.grid.oim.view.divex.form.CpuInfoFormDE;

import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivExWrapper;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.SideContentView;

public class CpuInfoEditServlet extends ServletBase implements Servlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(CpuInfoEditServlet.class);  
	private String current_page = "cpuinfo";	

    public CpuInfoEditServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		setAuth(request);
//		auth.check("edit_measurement");
		auth.check("admin");
		
		CpuInfoRecord rec;
		String title;

		//if cpu_info_id is provided then we are doing update, otherwise do new.
		// AG: Do we need any request parameter-value checks?
		String cpu_info_id_str = request.getParameter("cpu_info_id");
		if(cpu_info_id_str != null) {
			//pull record to update
			int cpu_info_id = Integer.parseInt(cpu_info_id_str);
			CpuInfoModel model = new CpuInfoModel(auth);
			try {
				CpuInfoRecord keyrec = new CpuInfoRecord();
				keyrec.id =cpu_info_id;
				rec = model.get(keyrec);
			} catch (SQLException e) {
				throw new ServletException(e);
			}	
			title = "Update CPU Information";
		} else {
			rec = new CpuInfoRecord();
			title = "New CPU Information record";	
		}
	
		CpuInfoFormDE form;
		String origin_url = BaseURL()+"/"+current_page;
		try {
			form = new CpuInfoFormDE(DivExRoot.getInstance(request), rec, origin_url, auth);
		} catch (SQLException e) {
			throw new ServletException(e);
		}
		
		//put the form in a view and display
		ContentView contentview = new ContentView();
		contentview.add(new HtmlView("<h1>"+title+"</h1>"));	
		contentview.add(new DivExWrapper(form));
		
		Page page = new Page(createMenuView("admin"), contentview, createSideView());	
		// AG: Ensure creating new JS. 
		page.addExternalJS(BaseURL()+"/osggridtypeedit.js");
		page.render(response.getWriter());	
	}
	
	private SideContentView createSideView()
	{
		SideContentView view = new SideContentView();
		view.add("Misc-no-op", new HtmlView("Misc-no-op"));
		return view;
	}
}