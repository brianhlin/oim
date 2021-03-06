package edu.iu.grid.oim.model.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.ContactRank;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.RecordBase;
import edu.iu.grid.oim.model.db.record.VOReportContactRecord;
import edu.iu.grid.oim.model.db.record.VOReportNameRecord;

public class VOReportContactModel extends SmallTableModelBase<VOReportContactRecord> {
    static Logger log = Logger.getLogger(VOReportContactModel.class); 
	
	public VOReportContactModel(UserContext context) {
		super(context, "vo_report_contact");
		// TODO Auto-generated constructor stub
	}
	VOReportContactRecord createRecord() throws SQLException
	{
		return new VOReportContactRecord();
	}

	public Collection<VOReportContactRecord> getAllByVOReportNameID(int vo_report_name_id) throws SQLException
	{ 
		ArrayList<VOReportContactRecord> list = new ArrayList<VOReportContactRecord>();
		for(RecordBase record : getCache()) {
			VOReportContactRecord vorc_record = (VOReportContactRecord)record;
			if(vorc_record.vo_report_name_id == vo_report_name_id) list.add(vorc_record);
		}
		return list;
	}	
    public String getName()
    {
    	return "Virtual Organization Report Contact";
    }
	public String getHumanValue(String field_name, String value) throws NumberFormatException, SQLException
	{
		if(field_name.equals("vo_report_name_id")) {
			VOReportNameModel model = new VOReportNameModel(context);
			VOReportNameRecord rec = model.get(Integer.parseInt(value));
			return value + " (" + rec.name + ")";
		} else if(field_name.equals("contact_type_id")) {
			ContactTypeModel model = new ContactTypeModel(context);
			ContactTypeRecord rec = model.get(Integer.parseInt(value));
			return value + " (" + rec.name + ")";
		} else if(field_name.equals("contact_rank_id")) {
			ContactRank rank = ContactRank.get(Integer.parseInt(value));
			return value + " (" + rank + ")";
		} else if(field_name.equals("contact_id")) {
			ContactModel model = new ContactModel(context);
			ContactRecord rec = model.get(Integer.parseInt(value));
			return value + " (" + rec.name + ")";
		}
		return value;
	}
	public Boolean hasLogAccess(XPath xpath, Document doc) throws XPathExpressionException
	{
		Integer report_name_id = Integer.parseInt((String)xpath.evaluate("//Keys/Key[Name='vo_report_name_id']/Value", doc, XPathConstants.STRING));
		VOReportNameModel vornmodel = new VOReportNameModel(context);
		VOReportNameRecord vornrec;
		try {
			vornrec = vornmodel.get(report_name_id);
			VOModel model = new VOModel(context);
			return model.canEdit(vornrec.vo_id);
		} catch (SQLException e) {
			log.error(e);
		}
		return false;
	}
}
