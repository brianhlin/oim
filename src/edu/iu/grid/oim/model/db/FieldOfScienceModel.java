package edu.iu.grid.oim.model.db;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.record.FieldOfScienceRecord;
import edu.iu.grid.oim.model.db.record.RecordBase;
import edu.iu.grid.oim.model.db.record.ResourceRecord;

public class FieldOfScienceModel extends SmallTableModelBase<FieldOfScienceRecord> {
    static Logger log = Logger.getLogger(FieldOfScienceModel.class);  
	
    public FieldOfScienceModel(UserContext context) 
    {
    	super(context, "field_of_science");
    }
    FieldOfScienceRecord createRecord() throws SQLException
	{
		return new FieldOfScienceRecord();
	}
	public ArrayList<FieldOfScienceRecord> getAll() throws SQLException
	{
		ArrayList<FieldOfScienceRecord> list = new ArrayList<FieldOfScienceRecord>();
		for(RecordBase it : getCache()) {
			list.add((FieldOfScienceRecord)it);
		}
		return list;
	}

	public FieldOfScienceRecord get(int id) throws SQLException {
		FieldOfScienceRecord keyrec = new FieldOfScienceRecord();
		keyrec.id = id;
		return get(keyrec);
	}
	
    public String getName()
    {
    	return "Field Of Science";
    }
	public Boolean hasLogAccess(XPath xpath, Document doc) throws XPathExpressionException
	{
		//Integer id = Integer.parseInt((String)xpath.evaluate("//Keys/Key[Name='id']/Value", doc, XPathConstants.STRING));
		if(auth.allows("admin")) {
			return true;
		}
		return false;
	}
}
