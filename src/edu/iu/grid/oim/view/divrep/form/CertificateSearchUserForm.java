package edu.iu.grid.oim.view.divrep.form;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.divrep.DivRep;
import com.divrep.DivRepEvent;
import com.divrep.DivRepEventListener;
import com.divrep.common.DivRepButton;
import com.divrep.common.DivRepDate;
import com.divrep.common.DivRepSelectBox;
import com.divrep.common.DivRepStaticContent;
import com.divrep.common.DivRepTextBox;

import edu.iu.grid.oim.model.CertificateRequestStatus;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.CertificateRequestUserModel;
import edu.iu.grid.oim.model.db.VOModel;
import edu.iu.grid.oim.model.db.record.CertificateRequestUserRecord;
import edu.iu.grid.oim.model.db.record.VORecord;
import edu.iu.grid.oim.view.divrep.VOSelector;

public class CertificateSearchUserForm extends DivRep
{
	private static final long serialVersionUID = -7040235927943722402L;
	static Logger log = Logger.getLogger(CertificateSearchUserForm.class);
	private UserContext context;
	//private Authorization auth;
	private String base_url;
	
	private DivRepStaticContent message;
	private DivRepTextBox request_id;
	private DivRepTextBox serial_id;
	private DivRepTextBox dn_contains;
	private DivRepSelectBox status;
	private VOSelector vo;
	private DivRepDate request_after;
	private DivRepDate request_before;
	
	private DivRepButton submit;
	
	private Tab active_tab;
	private ArrayList<Tab> tabs;
	
	private abstract class Tab {
		String id;
		String name;
		public Tab(String id, String name) {
			this.id = id;
			this.name = name;
		}
		public String getID() { return id; }
		public void renderTab(PrintWriter out) {
			if(active_tab == this) {
				out.write("<li class=\"active\">");
			} else {
				out.write("<li>");
			}
			out.write("<a href=\"#"+id+"\" data-toggle=\"tab\">"+name+"</a>");
			out.write("</li>");
		}
		public void renderPane(PrintWriter out) {
			if(active_tab == this) {
				out.write("<div class=\"tab-pane active\" id=\""+id+"\">");
			} else {
				out.write("<div class=\"tab-pane\" id=\""+id+"\">");
			}
			render(out);
			out.write("</div>");
		}
		abstract void render(PrintWriter out);
		abstract ArrayList<CertificateRequestUserRecord> search(CertificateRequestUserModel model);
	}
	
	class OtherTab extends Tab {

		public OtherTab() {
			super("other", "Others");
		}

		@Override
		void render(PrintWriter out) {
			out.write("<div class=\"row-fluid\">");
			
			out.write("<div class=\"span4\">");
			dn_contains.render(out);
			status.render(out);
			vo.render(out);
			out.write("</div>"); //span4
			
			out.write("<div class=\"span4 duration\">");
			request_after.render(out);
			out.write("</div>");//span4
			
			out.write("<div class=\"span4 duration\">");
			request_before.render(out);
			out.write("</div>"); //span4
			
			out.write("</div>"); //row-fluid
		}
		
		ArrayList<CertificateRequestUserRecord> search(CertificateRequestUserModel model) {
			ArrayList<CertificateRequestUserRecord> recs = new ArrayList<CertificateRequestUserRecord>();
			try {
				String status_str = null;
				if(status.getValue() != null) {
					status_str = CertificateRequestStatus.toStatus(status.getValue());
				}
				String dn_str = null;
				if(dn_contains.getValue() != null && !dn_contains.getValue().trim().isEmpty()) {
					dn_str = dn_contains.getValue();
				}

				recs = model.search(dn_str, status_str, vo.getValue(), request_after.getValue(), request_before.getValue());
				if(recs.isEmpty()) {	
					message.setHtml("<p class=\"alert\">No matching user certificates.</p>");
				}
				
			} catch (SQLException e) {
				log.error("Failed to search by dn_contains", e);
			}
			return recs;
		}
	}
	class RequestTab extends Tab {
		public RequestTab() {
			super("request", "Request ID");
		}

		@Override
		void render(PrintWriter out) {
			request_id.render(out);
		}
		
		@Override
		ArrayList<CertificateRequestUserRecord> search(CertificateRequestUserModel model) {
			ArrayList<CertificateRequestUserRecord> recs = new ArrayList<CertificateRequestUserRecord>();
			if(request_id.getValue() != null && !request_id.getValue().isEmpty()) {
				try {
					Integer int_id = Integer.parseInt(request_id.getValue());
					//if id is speciied, it takes precedence
					CertificateRequestUserRecord rec = model.get(int_id);
					if(rec != null) {	
						recs.add(rec);	
					} else {
						message.setHtml("<p class=\"alert\">No matching user certificate with request ID: " + request_id.getValue() + "</p>");
					}
				} catch (NumberFormatException e) {
					//maybe not number
					message.setHtml("<p class=\"alert\">Please specify an integer:  " + request_id.getValue() + "</p>");
				} catch (SQLException e) {
					log.error("Failed to search by reques_id", e);
				}
			}
			return recs;
		}
	}
	
	class SerialTab extends Tab {
		public SerialTab() {
			super("serial", "Serial Number");
		}

		@Override
		void render(PrintWriter out) {
			serial_id.render(out);
		}

		@Override
		ArrayList<CertificateRequestUserRecord> search(CertificateRequestUserModel model) {
			ArrayList<CertificateRequestUserRecord> recs = new ArrayList<CertificateRequestUserRecord>();
			if(serial_id.getValue() != null && !serial_id.getValue().isEmpty()) {
				try {
					//if id is speciied, it takes precedence
					CertificateRequestUserRecord rec = model.getBySerialID(serial_id.getValue());
					if(rec != null) {	
						recs.add(rec);	
					} else {
						message.setHtml("<p class=\"alert\">No matching user certificate with serial number: " + serial_id.getValue() + "</p>");
					}
				} catch (SQLException e) {
					log.error("Failed to search by serial_id", e);
				}
			}
			return recs;
		}
	}
	
	public CertificateSearchUserForm(final HttpServletRequest request, final UserContext context) {
		super(context.getPageRoot());
		this.context = context;
		base_url = request.getRequestURI();
		
		message = new DivRepStaticContent(this, "");
		
		request_id = new DivRepTextBox(this);
		request_id.setLabel("Request ID");
		//request_id.setWidth(200);
		
		serial_id = new DivRepTextBox(this);
		serial_id.setLabel("Serial Number");
		
		dn_contains = new DivRepTextBox(this);
		dn_contains.setLabel("DN Contains");
		//dn_contains.setWidth(210);
		
		status = new DivRepSelectBox(this);
		status.setLabel("Status");
		status.setNullLabel("(Any)");
		LinkedHashMap<Integer, String> keyvalues = new LinkedHashMap();
		int i = 0;
		while(true) {
			String st = CertificateRequestStatus.toStatus(i);
			if(st == null) break;
			keyvalues.put(i, st);
			++i;
		}
		status.setValues(keyvalues);
		
		vo = new VOSelector(this, context);
		vo.setLabel("VO");
		vo.setNullLabel("(Any)");
		
		request_after = new DivRepDate(this);
		//Calendar today = new GregorianCalendar();
		Calendar last_year = new GregorianCalendar();
		last_year.add(Calendar.MONTH, -6);
		request_after.setValue(last_year.getTime());
		request_after.setLabel("Requested After");
		request_before = new DivRepDate(this);
		request_before.setLabel("Requested Before");
		
		/* doesn't work
		//set min date
		try {
			String string = "January 1, 2012";
			Date date = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse(string);
			request_after.setMinDate(date);
			request_before.setMinDate(date);
		} catch (ParseException e1) {
			log.error("Failed to set min date");
		}
		*/
		
		submit = new DivRepButton(this, "Search");
		submit.addClass("btn");
		submit.addClass("btn-primary");
		//submit.addClass("pull-right");
		submit.addEventListener(new DivRepEventListener() {
			@Override
			public void handleEvent(DivRepEvent e) {
				//construct parameter and refresh
				StringBuffer url = new StringBuffer(base_url);
				url.append("?");
				if(request_id.getValue() != null) {
					url.append("request_id="+request_id.getValue()+"&");
				}
				if(serial_id.getValue() != null) {
					url.append("serial_id="+serial_id.getValue()+"&");
				}
				if(dn_contains.getValue() != null) {
					url.append("dn_contains="+StringEscapeUtils.escapeHtml(dn_contains.getValue())+"&");
				}
				if(status.getValue() != null) {
					url.append("status="+status.getValue()+"&");
				}
				if(vo.getValue() != null) {
					url.append("vo="+vo.getValue()+"&");
				}
				if(request_after.getValue() != null) {
					url.append("request_after="+request_after.getValue().getTime()+"&");
				}
				if(request_before.getValue() != null) {
					url.append("request_before="+request_before.getValue().getTime()+"&");
				}
				url.append("active="+active_tab.getID());
				submit.redirect(url.toString());
			}
		});
		
		tabs = new ArrayList<Tab>();
		tabs.add(new RequestTab());
		tabs.add(new SerialTab());
		tabs.add(new OtherTab());
		
		//set current values
		request_id.setValue(request.getParameter("request_id"));
		serial_id.setValue(request.getParameter("serial_id"));
		dn_contains.setValue(request.getParameter("dn_contains"));
		if(request.getParameter("vo") != null) {
			vo.setValue(Integer.parseInt(request.getParameter("vo")));
		}
		//DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
		if(request.getParameter("request_after") != null) {
			long time = Long.parseLong(request.getParameter("request_after"));
			Date d = new Date(time);
			request_after.setValue(d);
		}
		if(request.getParameter("request_before") != null) {
			long time = Long.parseLong(request.getParameter("request_before"));
			Date d = new Date(time);
			request_before.setValue(d);
		}
		if(request.getParameter("status") != null) {
			status.setValue(Integer.parseInt(request.getParameter("status")));
		}
		if(request.getParameter("active") != null) {
			active_tab = findTab(request.getParameter("active"));
		} else {
			active_tab = tabs.get(0);
		}
	}

	protected void onEvent(DivRepEvent e) {
		if(e.action.equals("shown")) {
			active_tab = findTab(e.value.substring(1));//remove # from #tabid 
		}
		log.debug("selecting tab:" + e.value);
	}
	
	private Tab findTab(String id) {
		for(Tab tab : tabs) {
			if(tab.getID().equals(id)) {
				return tab;
			}
		}
		return null;
	}

	public ArrayList<CertificateRequestUserRecord> search() {
		//ArrayList<CertificateRequestUserRecord> recs = new ArrayList<CertificateRequestUserRecord>();
		CertificateRequestUserModel model = new CertificateRequestUserModel(context);
		return active_tab.search(model);
	}

	@Override
	public void render(PrintWriter out) {
		out.write("<div id=\""+getNodeID()+"\">");
		
		//form
		out.write("<div class=\"tabbable\">");
	
		//tabs
		out.write("<ul class=\"nav nav-tabs\">");
		for(Tab tab : tabs) {
			tab.renderTab(out);
		}
		out.write("</ul>");//nav-tabs
		
		//tab panes
		out.write("<div class=\"tab-content\">");
		for(Tab tab : tabs) {
			tab.renderPane(out);
		}
		out.write("</div>");//tab-content
		
		out.write("</div>");//tabbable

		out.write("<div class=\"form-actions\">");
		submit.render(out);
		out.write("</div>");
		
		message.render(out);
		
		out.write("<script>\n");
		out.write("$('#"+getNodeID()+" a[data-toggle=\"tab\"]').on('shown', function (e) {\n");
		out.write("divrep(\""+getNodeID()+"\", e, e.target.hash);");
		out.write("})\n");
		out.write("</script>\n");
		
		out.write("</div>");
		
	}
}
