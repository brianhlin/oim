package edu.iu.grid.oim.view.divrep.form;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.webif.divrep.DivRep;
import com.webif.divrep.Event;
import com.webif.divrep.EventListener;
import com.webif.divrep.common.Static;
import com.webif.divrep.common.CheckBoxFormElement;
import com.webif.divrep.common.FormBase;
import com.webif.divrep.common.TextArea;
import com.webif.divrep.common.Text;
import com.webif.divrep.validator.DoubleValidator;
import com.webif.divrep.validator.UniqueValidator;
import com.webif.divrep.validator.UrlValidator;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.FileReader;
import edu.iu.grid.oim.lib.Footprint;
import edu.iu.grid.oim.lib.Authorization.AuthorizationException;
import edu.iu.grid.oim.model.Context;
import edu.iu.grid.oim.model.db.ContactTypeModel;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.ResourceAliasModel;
import edu.iu.grid.oim.model.db.ResourceContactModel;
import edu.iu.grid.oim.model.db.ResourceDowntimeModel;
import edu.iu.grid.oim.model.db.ResourceServiceModel;
import edu.iu.grid.oim.model.db.ResourceWLCGModel;
import edu.iu.grid.oim.model.db.ServiceModel;
import edu.iu.grid.oim.model.db.ResourceModel;
import edu.iu.grid.oim.model.db.VOModel;
import edu.iu.grid.oim.model.db.VOResourceOwnershipModel;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.ResourceAliasRecord;
import edu.iu.grid.oim.model.db.record.ResourceContactRecord;
import edu.iu.grid.oim.model.db.record.ResourceDowntimeRecord;
import edu.iu.grid.oim.model.db.record.ResourceRecord;
import edu.iu.grid.oim.model.db.record.ResourceServiceRecord;
import edu.iu.grid.oim.model.db.record.ResourceWLCGRecord;
import edu.iu.grid.oim.model.db.record.VOResourceOwnershipRecord;
import edu.iu.grid.oim.view.divrep.ContactEditor;
import edu.iu.grid.oim.view.divrep.OIMHierarchySelector;
import edu.iu.grid.oim.view.divrep.ResourceAlias;
import edu.iu.grid.oim.view.divrep.ResourceServices;
import edu.iu.grid.oim.view.divrep.ResourceWLCG;
import edu.iu.grid.oim.view.divrep.VOResourceOwnership;

public class ResourceFormDE extends FormBase 
{
    static Logger log = Logger.getLogger(ResourceFormDE.class); 
   
    private Context context;
    
	protected Authorization auth;
	private Integer id;
	
	private Text name;
	private TextArea description;
	private Text fqdn;
	private Text url;
	
	private CheckBoxFormElement active;
	private CheckBoxFormElement disable;
	private OIMHierarchySelector resource_group_id;
	private ResourceAlias aliases;
	private ResourceServices resource_services;
	private VOResourceOwnership owners;

	private CheckBoxFormElement wlcg;
	private ResourceWLCG wlcg_section;
	
	//contact types to edit
	private int contact_types[] = {
		1, //submitter
		3, //admin contact
		2, //security contact
		9, //resource report contact
		5 //misc contact
	};
	private HashMap<Integer, ContactEditor> contact_editors = new HashMap();
	
	public ResourceFormDE(Context _context, ResourceRecord rec, String origin_url) throws AuthorizationException, SQLException
	{	
		super(_context.getPageRoot(), origin_url);
		context = _context;
		auth = context.getAuthorization();
		
		id = rec.id;
		
		new Static(this, "<h2>Basic Resource Information</h2>");
		new Static(this, "<p>Add/modify basic information about this resource.</p>");
		
		//pull vos for unique validator
		HashMap<Integer, String> resources = getResources();
		if(id != null) {
			//if doing update, remove my own name (I can use my own name)
			resources.remove(id);
		}
		
		name = new Text(this);
		name.setLabel("Name");
		name.setValue(rec.name);
		name.addValidator(new UniqueValidator<String>(resources.values()));
		name.setRequired(true);
		name.setSampleValue("Indiana_Sample_CE");
		
		fqdn = new Text(this);
		fqdn.setLabel("Fully Qualified Domain Name (FQDN) of this resource");
		fqdn.setValue(rec.fqdn);
		fqdn.addValidator(new UniqueValidator<String>(resources.values()));
		fqdn.setRequired(true);
		fqdn.setSampleValue("gate01.sample.edu");

		resource_group_id = new OIMHierarchySelector(this, context, OIMHierarchySelector.Type.RESOURCE_GROUP);
		resource_group_id.setLabel("Select Your Facility (Instituition), Site (Department), and Resource Group");
		resource_group_id.setRequired(true);
		if(id != null) {
			resource_group_id.setValue(rec.resource_group_id);
		}

		description = new TextArea(this);
		description.setLabel("Short Description");
		description.setValue(rec.description);
		description.setRequired(true);
		description.setSampleValue("This is a hidden gatekeeper accessible from the FermiGrid site only. It reports to Gratia and RSV directly but offsite jobs can only get to it via the FermiGrid job gateway fermigridosg1");
				
		url = new Text(this);
		url.setLabel("Information URL");
		url.setValue(rec.url);
		url.addValidator(UrlValidator.getInstance());
		url.setRequired(true);
		url.setSampleValue("http://sample.edu/information");
		
		new Static(this, "<h3>Resource FQDN Aliases (If Applicable)</h3>");
		new Static(this, "<p>If you used a DNS alias as their main gatekeeper or SE head node FQDN (as defined above), then you can add real host name(s) here as reverse alias(es).</p>");
		aliases = new ResourceAlias (this);
		ResourceAliasModel ramodel = new ResourceAliasModel(context);
		if(id != null) {
			for(ResourceAliasRecord rarec : ramodel.getAllByResourceID(id)) {
				aliases.addAlias(rarec.resource_alias);
			}
		}
		
		new Static(this, "<h2>Resource Services</h2>");
		new Static(this, "<p>Add, remove, modify services associated with your resource. For example, a CE or an SRM.</p>");
		ServiceModel smodel = new ServiceModel(context);
		resource_services = new ResourceServices(this, context, smodel.getAll());
		ResourceServiceModel rsmodel = new ResourceServiceModel(context);
		if(id != null) {
			for(ResourceServiceRecord rarec : rsmodel.getAllByResourceID(id)) {
				resource_services.addService(rarec);
			}
		}

		// Resource ownership stuff
		new Static(this, "<h2>VO Owners</h2>");
		new Static(this, "<p>Add/modify VO ownership of this resource.</p>");
		VOModel vo_model = new VOModel(context);
		owners = new VOResourceOwnership(this, vo_model.getAll());
		VOResourceOwnershipModel voresowner_model = new VOResourceOwnershipModel(context);
		if(id != null) {
			for(VOResourceOwnershipRecord voresowner_rec : voresowner_model.getAllByResourceID(id)) {
				owners.addOwner(voresowner_rec);
			}
		} else {
			//add new one
			owners.addOwner(new VOResourceOwnershipRecord());
		}

		new Static(this, "<h2>Contact Information</h2>");
		new Static(this, "<p>Add, remove, modify various types of contacts associated with your resource. These contacts have the authorization to modify this resource. Each contact entry field shows you a list of contacts as you type a name.</p>");
		HashMap<Integer/*contact_type_id*/, ArrayList<ResourceContactRecord>> voclist_grouped = null;
		if(id != null) {
			ResourceContactModel vocmodel = new ResourceContactModel(context);
			ArrayList<ResourceContactRecord> voclist = vocmodel.getByResourceID(id);
			voclist_grouped = vocmodel.groupByContactTypeID(voclist);
		} else {
			//set user's contact as submitter
			voclist_grouped = new HashMap<Integer, ArrayList<ResourceContactRecord>>();
			ArrayList<ResourceContactRecord> list = new ArrayList<ResourceContactRecord>();
			ResourceContactRecord submitter = new ResourceContactRecord();
			submitter.contact_id = auth.getContactID();
			submitter.contact_rank_id = 1;//primary
			submitter.contact_type_id = 1;//submitter
			list.add(submitter);
			voclist_grouped.put(1/*submitter*/, list);
		}
		ContactTypeModel ctmodel = new ContactTypeModel(context);
		for(int contact_type_id : contact_types) {
			ContactEditor editor = createContactEditor(voclist_grouped, ctmodel.get(contact_type_id));
			//disable submitter editor if needed
			if(!auth.allows("admin")) {
				if(contact_type_id == 1) { //1 = Submitter Contact
					editor.setDisabled(true);
				}
			}
			contact_editors.put(contact_type_id, editor);
		}
		
		new Static(this, "<h2>WLCG Interoperability Information (If Applicable)</h2>");
		new Static(this, "<p>Enable this section if your resource is part of the WLCG interoperability agreement. " + 
					"You can then provide more interoperability details for this resource, including KSI2K Limits " + 
					" and storage capacity min/max values. If you are not sure about any of these values, " + 
					" ask your Owner VO(s)!</p>");

		wlcg = new CheckBoxFormElement(this);
		wlcg.setLabel("This is a WLCG resource");

		//indent the whole WCLG things
		new Static(this, "<div class=\"indent\">");
		wlcg_section = new ResourceWLCG (this, context, null);
		hideWLCGElement(true);

		wlcg.addEventListener(new EventListener() {
			public void handleEvent(Event e) {	
				if(((String)e.value).compareTo("true") == 0) {
					hideWLCGElement(false);
				} else {
					hideWLCGElement(true);
				}
			}
		});

		if(id != null) {
			ResourceWLCGModel wmodel = new ResourceWLCGModel(context);
			ResourceWLCGRecord wrec = wmodel.get(rec.id);
			if(wrec != null) {
				wlcg.setValue(true);
				wlcg_section.setWlcgRecord(wrec);
				hideWLCGElement(false);				
			}
		}
		new Static(this, "</div>");

		if(auth.allows("admin")) {
			new Static(this, "<h2>Administrative Tasks</h2>");
		}
		active = new CheckBoxFormElement(this);
		active.setLabel("Active");
		active.setValue(rec.active);
		if(!auth.allows("admin")) {
			active.setHidden(true);
		}
		
		disable = new CheckBoxFormElement(this);
		disable.setLabel("Disable");
		disable.setValue(rec.disable);
		if(!auth.allows("admin")) {
			disable.setHidden(true);
		}
	}
	
	private void hideWLCGElement(Boolean b)
	{
		wlcg_section.setHidden(b);
		wlcg_section.redraw();
	}
	
	private ContactEditor createContactEditor(HashMap<Integer, ArrayList<ResourceContactRecord>> voclist, ContactTypeRecord ctrec) throws SQLException
	{
		new Static(this, "<h3>" + StringEscapeUtils.escapeHtml(ctrec.name) + "</h3>");
		ContactModel pmodel = new ContactModel(context);		
		ContactEditor editor = new ContactEditor(this, pmodel, ctrec.allow_secondary, ctrec.allow_tertiary);
		
		//if provided, populate currently selected contacts
		if(voclist != null) {
			ArrayList<ResourceContactRecord> clist = voclist.get(ctrec.id);
			if(clist != null) {
				for(ResourceContactRecord rec : clist) {
					ContactRecord keyrec = new ContactRecord();
					keyrec.id = rec.contact_id;
					ContactRecord person = pmodel.get(keyrec);
					editor.addSelected(person, rec.contact_rank_id);
				}
			}
		}
	
		return editor;
	}
	
	protected Boolean doSubmit() 
	{
		Boolean ret = true;
		
		//Construct VORecord
		ResourceRecord rec = new ResourceRecord();
		rec.id = id;
	
		rec.name = name.getValue();
		rec.description = description.getValue();
		rec.fqdn = fqdn.getValue();
		rec.url = url.getValue();
		rec.active = active.getValue();
		rec.disable = disable.getValue();
		rec.resource_group_id = resource_group_id.getValue();
		
		//If WLCG is on, then create wlcg record
		ResourceWLCGRecord wrec = null;
		if(wlcg.getValue()) {
			wrec = wlcg_section.getWlcgRecord();
		}
		
		ResourceModel model = new ResourceModel(context);
		try {
			if(rec.id == null) {
				model.insertDetail(rec, 
						aliases.getAliases(), 
						getContactRecordsFromEditor(), 
						wrec,
						resource_services.getResourceServiceRecords(),
						owners.getOwners());
				
				//create footprint ticket
				Footprint fp = new Footprint(context);
				fp.createNewResourceTicket(rec.name);
				
			} else {
				model.updateDetail(rec, 
						aliases.getAliases(), 
						getContactRecordsFromEditor(),
						wrec,
						resource_services.getResourceServiceRecords(),
						owners.getOwners());
			}
		} catch (Exception e) {
			alert(e.getMessage());
			ret = false;
		}
		context.close();
		return ret;
	}
	
	//retrieve contact records from the contact editor.
	//be aware that VOContactRecord's vo_id is not populated.. you need to fill it out with
	//appropriate vo_id later
	private ArrayList<ResourceContactRecord> getContactRecordsFromEditor()
	{
		ArrayList<ResourceContactRecord> list = new ArrayList();
		
		for(Integer type_id : contact_editors.keySet()) 
		{
			ContactEditor editor = contact_editors.get(type_id);
			HashMap<ContactRecord, Integer> contacts = editor.getContactRecords();
			for(ContactRecord contact : contacts.keySet()) {
				ResourceContactRecord rec = new ResourceContactRecord();
				Integer rank_id = contacts.get(contact);
				rec.contact_id = contact.id;
				rec.contact_type_id = type_id;
				rec.contact_rank_id = rank_id;
				list.add(rec);
			}
		}
		
		return list;
	}
	
	private HashMap<Integer, String> getResources() throws SQLException
	{
		ResourceModel model = new ResourceModel(context);
		HashMap<Integer, String> resources = new HashMap();
		for(ResourceRecord rec : model.getAll()) {
			resources.put(rec.id, rec.name);
		}
		return resources;
	}
	
	public Boolean isValidResourceFQDN(String url)
	{
		if(fqdn.getValue().equals(url)) return true;
		ArrayList<String> as = aliases.getAliases();
		if(as.contains(url)) return true;
		
		return false;
	}
}