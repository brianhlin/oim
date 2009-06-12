package edu.iu.grid.oim.view.divex.form;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.webif.divex.form.FormDEBase;
import com.webif.divex.DivEx;
import com.webif.divex.Event;
import com.webif.divex.EventListener;
import com.webif.divex.StaticDE;
import com.webif.divex.form.CheckBoxFormElementDE;
import com.webif.divex.form.FormElementDEBase;
import com.webif.divex.form.SelectFormElementDE;
import com.webif.divex.form.TextAreaFormElementDE;
import com.webif.divex.form.TextFormElementDE;
import com.webif.divex.form.validator.EmailValidator;
import com.webif.divex.form.validator.UrlValidator;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.Config;
import edu.iu.grid.oim.model.Context;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.DNModel;
import edu.iu.grid.oim.model.db.record.DNRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;

public class ContactFormDE extends FormDEBase 
{
    static Logger log = Logger.getLogger(ContactFormDE.class); 
   
    private Context context;
    private Authorization auth;
	private Integer id;
	
	private TextFormElementDE name;
	private TextFormElementDE primary_email, secondary_email;
	private TextFormElementDE primary_phone, secondary_phone;
	private TextFormElementDE primary_phone_ext, secondary_phone_ext;
	private TextFormElementDE address_line_1, address_line_2;
	private TextFormElementDE city, state, zipcode, country;
	private CheckBoxFormElementDE active;
	private CheckBoxFormElementDE disable;
	private TextFormElementDE im;
	private PhotoDE photo_url;
	private CheckBoxFormElementDE person;
	private TextAreaFormElementDE contact_preference;
	private SelectFormElementDE submitter_dn;
	
	class PhotoDE extends FormElementDEBase<String>
	{
		public PhotoDE(DivEx _parent) {
			super(_parent);
			url = new TextFormElementDE(this);
			url.addEventListener(new EventListener() {
				public void handleEvent(Event e) {
					PhotoDE.this.redraw();
				}});
			url.addValidator(UrlValidator.getInstance());
		}

		private TextFormElementDE url;

		@Override
		protected void onEvent(Event e) {
			// TODO Auto-generated method stub
			
		}
		
		//bypass pretty much everything to url element
		public void setSampleValue(String value) {
			url.setSampleValue(value);
		}
		public void setValue(String value) {
			url.setValue(value);
		}
		public String getValue() {
			return url.getValue();
		}
		public Boolean isValid() { 
			return url.isValid();
		}
		public void setLabel(String label) {
			url.setLabel(label);
		}

		@Override
		public void render(PrintWriter out) {
			String img = url.getValue();
			if(img == null || img.length() == 0) {
				img = Config.getApplicationBase() + "/images/noavatar.gif";
			}
			
			out.print("<div ");
			renderClass(out);
			out.write("id=\""+getNodeID()+"\">");
			if(!hidden) {
				url.render(out);
				out.write("<img class=\"avatar_preview\" src=\""+img+"\"/>");
			}
			out.write("</div>");
		}
	}
	
	public void showHidePersonalDetail()
	{
		Boolean hidden = !person.getValue();
		address_line_1.setHidden(hidden);
		address_line_2.setHidden(hidden);
		city.setHidden(hidden);
		state.setHidden(hidden);
		zipcode.setHidden(hidden);
		country.setHidden(hidden);
		im.setHidden(hidden);
		photo_url.setHidden(hidden);
		ContactFormDE.this.redraw();
		
		Boolean required = person.getValue();
		city.setRequired(required);
		state.setRequired(required);
		zipcode.setRequired(required);
		country.setRequired(required);
	}
	
	public ContactFormDE(Context _context, ContactRecord rec, String origin_url)
	{	
		super(_context.getPageRoot(), origin_url);
		context = _context;
		auth = context.getAuthorization();
		id = rec.id;

		name = new TextFormElementDE(this);
		name.setLabel("Full Name");
		name.setValue(rec.name);
		name.setRequired(true);
		
		primary_email = new TextFormElementDE(this);
		primary_email.setLabel("Primary Email");
		primary_email.setValue(rec.primary_email);
		primary_email.setRequired(true);
		primary_email.addValidator(new EmailValidator());
		
		secondary_email = new TextFormElementDE(this);
		secondary_email.setLabel("Secondary Email");
		secondary_email.setValue(rec.secondary_email);
		secondary_email.addValidator(new EmailValidator());

		primary_phone = new TextFormElementDE(this);
		primary_phone.setLabel("Primary Phone");
		primary_phone.setValue(rec.primary_phone);
		//primary_phone.setRequired(true);

		primary_phone_ext = new TextFormElementDE(this);
		primary_phone_ext.setLabel("Primary Phone Extension");
		primary_phone_ext.setValue(rec.primary_phone_ext);

		secondary_phone = new TextFormElementDE(this);
		secondary_phone.setLabel("Secondary Phone");
		secondary_phone.setValue(rec.secondary_phone);

		secondary_phone_ext = new TextFormElementDE(this);
		secondary_phone_ext.setLabel("Secondary Phone Extension");
		secondary_phone_ext.setValue(rec.secondary_phone_ext);
		
		contact_preference = new TextAreaFormElementDE(this);
		contact_preference.setLabel("Enter Additional Contact Preferences: For example, you as a site admin might prefer to be contacted by phone or by email.");
		contact_preference.setValue(rec.contact_preference);

		new StaticDE(this, "<h2>Personal Information</h2>");
		
		person = new CheckBoxFormElementDE(this);
		person.setLabel("This is a personal contact (not mailing list, group contact, etc...)");
		person.setValue(rec.person);
		person.addEventListener(new EventListener() {
			public void handleEvent(Event e) {
				showHidePersonalDetail();
			}});
		
		new StaticDE(this, "<div class=\"indent\">");
		{
			address_line_1 = new TextFormElementDE(this);
			address_line_1.setLabel("Address Line 1");
			address_line_1.setValue(rec.address_line_1);
	
			address_line_2 = new TextFormElementDE(this);
			address_line_2.setLabel("Address Line 2");
			address_line_2.setValue(rec.address_line_2);
			
			city = new TextFormElementDE(this);
			city.setLabel("City");
			city.setValue(rec.city);
			city.setRequired(true);
	
			state = new TextFormElementDE(this);
			state.setLabel("State");
			state.setValue(rec.state);
			state.setRequired(true);
	
			zipcode = new TextFormElementDE(this);
			zipcode.setLabel("Zipcode");
			zipcode.setValue(rec.zipcode);
			zipcode.setRequired(true);
	
			country = new TextFormElementDE(this);
			country.setLabel("Country");
			country.setValue(rec.country);
			country.setRequired(true);
			
			im = new TextFormElementDE(this);
			im.setLabel("Instant Messaging Information");
			im.setValue(rec.im);
			im.setSampleValue("soichih@gtalk");
			
			photo_url = new PhotoDE(this);
			photo_url.setLabel("Photo URL");
			photo_url.setSampleValue("http://somewhere.com/myphoto.png");
			photo_url.setValue(rec.photo_url);
		}
		new StaticDE(this, "</div>");

		if(auth.allows("admin")) {
			new StaticDE(this, "<h2>Administrative Tasks</h2>");
		}

		if(rec.person == null || rec.person == false) {
			showHidePersonalDetail();
		}
		
		active = new CheckBoxFormElementDE(this);
		active.setLabel("Active");
		active.setValue(rec.active);
		if(!auth.allows("admin")) {
			active.setHidden(true);
		}
		
		disable = new CheckBoxFormElementDE(this);
		disable.setLabel("Disabled");
		disable.setValue(rec.disable);
		if(!auth.allows("admin")) {
			disable.setHidden(true);
		}
		
		//create DN selector
		HashMap<Integer, String> dns = new HashMap();
		try {
			DNModel dnmodel = new DNModel(context);;
			for(DNRecord dnrec : dnmodel.getAll()) {
				dns.put(dnrec.id, dnrec.dn_string);
			}
		} catch (SQLException e) {
			log.error(e);
		}
		submitter_dn = new SelectFormElementDE(this, dns);
		submitter_dn.setLabel("Submitter DN");
		submitter_dn.setValue(rec.submitter_dn_id);
		if(!auth.allows("admin")) {
			submitter_dn.setHidden(true);
		}
	}

	protected Boolean doSubmit() 
	{
		Boolean ret = true;
		ContactRecord rec = new ContactRecord();
		rec.id = id;
	
		rec.name = name.getValue();
		rec.primary_email = primary_email.getValue();
		rec.secondary_email = secondary_email.getValue();
		rec.primary_phone = primary_phone.getValue();
		rec.secondary_phone = secondary_phone.getValue();
		rec.primary_phone_ext = primary_phone_ext.getValue();
		rec.secondary_phone_ext = secondary_phone_ext.getValue();
		rec.address_line_1 = address_line_1.getValue();
		rec.address_line_2 = address_line_2.getValue();
		rec.city = city.getValue();
		rec.state = state.getValue();
		rec.zipcode = zipcode.getValue();
		rec.country = country.getValue();
		rec.active = active.getValue();
		rec.disable = disable.getValue();
		rec.person = person.getValue();
		rec.im = im.getValue();
		rec.photo_url = photo_url.getValue();
		rec.contact_preference = contact_preference.getValue();
		rec.submitter_dn_id = submitter_dn.getValue();
		
		ContactModel model = new ContactModel(context);
		try {
			if(rec.id == null) {
				model.insert(rec);
			} else {
				model.update(model.get(rec), rec);
			}
		} catch (Exception e) {
			alert(e.getMessage());
			ret = false;
		}
		context.close();
		return ret;
	}
}
