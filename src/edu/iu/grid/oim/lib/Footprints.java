package edu.iu.grid.oim.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.ConfigModel;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.view.HtmlFileView;

public class Footprints 
{
    static Logger log = Logger.getLogger(Footprints.class); 
    
	Authorization auth;
	UserContext context;

	public Footprints(UserContext context)
	{
		auth = context.getAuthorization();
		this.context = context;
	}
		
	public class FPTicket {
		public String title;
		public String description;
		public String nextaction;
		public Date nad;
		public String name;
		public String phone;
		public String email;
		public String status;
		public ArrayList<String> ccs = new ArrayList<String>();
		public ArrayList<String> assignees = new ArrayList<String>();
		public HashMap<String, String> metadata = new HashMap<String, String>();
		
		public Boolean mail_suppression_assignees = false;
		public Boolean mail_suppression_submitter = false;
		public Boolean mail_suppression_ccs = false;
		
		public FPTicket() {
			Calendar cal = Calendar.getInstance();
			//cal.add(Calendar.DATE, 7); 
			nad = cal.getTime();
		}
	}
	
	private Document parseXML(InputStream in) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(in);
	}
	
	private HttpClient createHttpClient() {
		HttpClient cl = new HttpClient();
		cl.getParams().setParameter("http.protocol.single-cookie-header", true);
		cl.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		//cl.getHttpConnectionManager().getParams().setConnectionTimeout(1000*10);
	    cl.getParams().setParameter("http.useragent", "OIM (OSG Information Management System)");
	    //cl.getParams().setParameter("http.contenttype", "application/x-www-form-urlencoded")
	    
	    return cl;
	}
	
	//returns ticket id if successful. null if not
	public String open(FPTicket ticket) {
		HttpClient cl = createHttpClient();
		PostMethod post = new PostMethod(StaticConfig.conf.getProperty("api.gocticket")+"/rest/open");

		post.addParameter("title", ticket.title);
		post.setParameter("description", ticket.description);
		post.setParameter("name", ticket.name);
		
		if(ticket.phone == null) {
			ticket.phone = ""; //empty works just fine
		}
		post.setParameter("phone", ticket.phone);
		
		post.setParameter("email", ticket.email);
		post.setParameter("nextaction", ticket.nextaction);
		post.setParameter("nextactiontime", String.valueOf(ticket.nad.getTime()/1000));
	
		for(int i = 0; i < ticket.ccs.size(); ++i) {
			post.setParameter("cc["+i+"]", ticket.ccs.get(i));
		}
		for(int i = 0; i < ticket.assignees.size(); ++i) {
			post.setParameter("assignee["+i+"]", ticket.assignees.get(i));
		}
		int i = 0;
		for(String key : ticket.metadata.keySet()) {
			String value = ticket.metadata.get(key);
			post.setParameter("metadata["+i+"]", key + "=" + value);
			i++;
		}
		
		if(ticket.mail_suppression_assignees) {
			post.setParameter("mail_suppression_assignees", "true");
		}
		if(ticket.mail_suppression_submitter) {
			post.setParameter("mail_suppression_submitter", "true");
		}
		if(ticket.mail_suppression_ccs) {
			post.setParameter("mail_suppression_ccs", "true");
		}
		
		try {
			log.debug("executing post");
			cl.executeMethod(post);
			log.debug("parsing xml");
			Document ret = parseXML(post.getResponseBodyAsStream());
			log.debug("getting element status");
			NodeList status_nl = ret.getElementsByTagName("Status");
			log.debug("getting first item");
			Element status = (Element)status_nl.item(0);
			if(status != null && status.getTextContent().equals("success")) {
				log.debug("getting ticketid");
				Element ticket_id_e = (Element) ret.getElementsByTagName("TicketID").item(0);
				return ticket_id_e.getTextContent();
			}
			log.error("Unknown return code from goc ticket");
		} catch (IOException e) {
			log.error("Failed to make open request" ,e);
		} catch (ParserConfigurationException e) {
			log.error("Failed to parse returned message" ,e);
		} catch (SAXException e) {
			log.error("Failed to parse returned message" ,e);
		}
		log.debug("returning null");
		return null;
	}
	
	//return true if successful
	public Boolean update(FPTicket ticket, String ticket_id) {
		/*
		if(StaticConfig.isDebug()) {
			log.debug("skipping ticket update on ticket: " + ticket_id + " since debug flag is set");
			log.debug(ticket.description);
			return false;
		}
		*/
		
		log.info("updated goc ticket : " + ticket_id);
		log.debug(ticket.description);
		
		HttpClient cl = new HttpClient();
		cl.getParams().setParameter("http.protocol.single-cookie-header", true);
		cl.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		PostMethod post = new PostMethod(StaticConfig.conf.getProperty("api.gocticket")+"/rest/update?id="+ticket_id);
		post.setParameter("description", ticket.description);
		if(ticket.nextaction != null) {
			post.setParameter("nextaction", ticket.nextaction);
		}
		post.setParameter("nextactiontime", String.valueOf(ticket.nad.getTime()/1000));
		if(ticket.status != null) {
			post.setParameter("status", ticket.status);
		}
		//only update if new list is provided (this means we can't *empty* cclist..)
		if(ticket.ccs.size() > 0) {
			for(int i = 0; i < ticket.ccs.size(); ++i) {
				post.setParameter("cc["+i+"]", ticket.ccs.get(i));
			}
		}
		//only update if new list is provided (this means we can't *empty* assignees..)
		if(ticket.assignees.size() > 0) {
			for(int i = 0; i < ticket.assignees.size(); ++i) {
				post.setParameter("assignee["+i+"]", ticket.assignees.get(i));
			}
		}
		
		if(ticket.mail_suppression_assignees) {
			post.setParameter("mail_suppression_assignees", "true");
		}
		if(ticket.mail_suppression_submitter) {
			post.setParameter("mail_suppression_submitter", "true");
		}
		if(ticket.mail_suppression_ccs) {
			post.setParameter("mail_suppression_ccs", "true");
		}
		
		try {
			cl.executeMethod(post);
			Document ret = parseXML(post.getResponseBodyAsStream());
			NodeList status_nl = ret.getElementsByTagName("Status");
			if(status_nl.getLength() != 0) {
				Element status = (Element)status_nl.item(0);
				if(status.getTextContent().equals("success")) {
					return true;
				}
				log.error("Unknown return code from goc ticket");
			} else {
				log.error("Failed to receive status code.");
			}
		} catch (IOException e) {
			log.error("Failed to make open request" ,e);
		} catch (ParserConfigurationException e) {
			log.error("Failed to parse returned message" ,e);
		} catch (SAXException e) {
			log.error("Failed to parse returned message" ,e);
		}
		
		return false;
	}
	
	public void createNewResourceTicket(String resource_name, SCRecord sc)
	{
		FPTicket ticket = new FPTicket();
		
		//create description
		ConfigModel config = new ConfigModel(context);
		HtmlFileView description = new HtmlFileView(config.ResourceFPTemplate.getString());
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("##RESOURCE_NAME##", resource_name);
		description.applyParams(params);
		ticket.description = description.toString();
		
        ContactRecord contact = auth.getContact();
        ticket.name = contact.name;
        ticket.phone = contact.primary_phone;
        ticket.email = contact.primary_email;
        ticket.nextaction = "Verify Information and Add to Ops Meeting Agenda";
        ticket.title = "OIM Resource Registration - " + resource_name;
		ticket.metadata.put("SUBMITTER_NAME", contact.getFirstName() + " " + contact.getLastName());
		ticket.metadata.put("SUBMITTER_DN", auth.getUserDN());
		ticket.metadata.put("SUBMITTED_VIA", "OIM/registration");
		
		if(StaticConfig.isDebug()) {
			ticket.assignees.add("hayashis");
		} else {
			ticket.assignees.add("echism");
			ticket.assignees.add("kagross");
            ticket.ccs.add("rquick@iu.edu");
            ticket.ccs.add("fkw@ucsd.edu");
            if(sc != null && sc.footprints_id != null) {
            	ticket.assignees.add(sc.footprints_id);
            	ticket.metadata.put("SUPPORTING_SC_ID", sc.id.toString());
            	ticket.metadata.put("SUPPORTING_SC_NAME", sc.name);
            }
		}
		
		String id = open(ticket);
		if(id != null) {
			log.info("Created GOC ticket with ID: " + id);
		}
	}
	
	public void createNewSCTicket(String sc_name)
	{
		FPTicket ticket = new FPTicket();
		
		//create description
		ConfigModel config = new ConfigModel(context);
		HtmlFileView description = new HtmlFileView(config.SCFPTemplate.getString());
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("##SC_NAME##", sc_name);
		description.applyParams(params);
		ticket.description = description.toString();
		
        ContactRecord contact = auth.getContact();
        ticket.name = contact.name;
        ticket.phone = contact.primary_phone;
        ticket.email = contact.primary_email;
        ticket.nextaction = "Verify Information and Add to Ops Meeting Agenda";
        ticket.title = "OIM Support Center Registration - " + sc_name;
		ticket.metadata.put("SUBMITTER_NAME", contact.getFirstName() + " " + contact.getLastName());
		ticket.metadata.put("SUBMITTER_DN", auth.getUserDN());
		ticket.metadata.put("SUBMITTED_VIA", "OIM/registration");
		
		if(StaticConfig.isDebug()) {
			ticket.assignees.add("mvkrenz");
            ticket.ccs.add("mvkrenz@iu.edu");
		} else {
			ticket.assignees.add("echism");
			ticket.assignees.add("kagross");
            ticket.ccs.add("rquick@iu.edu");
            ticket.ccs.add("fkw@ucsd.edu");
		}
		
		String id = open(ticket);
		if(id != null) {
			log.info("Created GOC ticket with ID: " + id);
		}
	}
	
	public void createNewVOTicket(String vo_name, SCRecord sc) {
		FPTicket ticket = new FPTicket();
		
		//create description
		ConfigModel config = new ConfigModel(context);
		HtmlFileView description = new HtmlFileView(config.VOFPTemplate.getString());	
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("##VO_NAME##", vo_name);
		description.applyParams(params);
		ticket.description = description.toString();
		
        ContactRecord contact = auth.getContact();
        ticket.name = contact.name;
        ticket.phone = contact.primary_phone;
        ticket.email = contact.primary_email;
        ticket.nextaction = "Verify Information and Add to Ops Meeting Agenda";
        ticket.title = "OIM Virtual Organization Registration - " + vo_name;
		ticket.metadata.put("SUBMITTER_NAME", contact.getFirstName() + " " + contact.getLastName());
		ticket.metadata.put("SUBMITTER_DN", auth.getUserDN());
		ticket.metadata.put("SUBMITTED_VIA", "OIM/registration");
		
		if(StaticConfig.isDebug()) {
			ticket.assignees.add("hayashis");
		} else {
			ticket.assignees.add("echism");
			ticket.assignees.add("kagross");
            ticket.ccs.add("rquick@iu.edu");
            ticket.ccs.add("fkw@ucsd.edu");
            ticket.ccs.add("osg-security-team@OPENSCIENCEGRID.ORG");
            if(sc != null && sc.footprints_id != null) {
            	ticket.assignees.add(sc.footprints_id);
            	ticket.metadata.put("SUPPORTING_SC_ID", sc.id.toString());
            	ticket.metadata.put("SUPPORTING_SC_NAME", sc.name);
            }
		}
		
		String id = open(ticket);
		if(id != null) {
			log.info("Created GOC ticket with ID: " + id);
		}		
	}
}
