package edu.iu.grid.oim.model.db;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.Context;
import edu.iu.grid.oim.model.db.record.RecordBase;

public abstract class ModelBase<T extends RecordBase> {
    static Logger log = Logger.getLogger(ModelBase.class); 
    
    protected Context context;
	protected Authorization auth;
    protected String table_name;
    
	protected ModelBase(Context _context, String _table_name)
	{
		context = _context;
		auth = _context.getAuthorization();
		
    	table_name = _table_name;
	}
	protected Connection connectOIM() throws SQLException
	{
		return context.connect("jdbc/oim");
	}
	
	//override this to provide human readable value
	public String getHumanValue(String field_name, String value) throws NumberFormatException, SQLException
	{
		return value;
	}
	
    //abstract T createRecord() throws SQLException;
    
	public String getName()
	{
		return getClass().getName();
	}
	
    public void remove(RecordBase rec) throws SQLException
    {
		//auth.check("write_"+table_name);
		
    	try {
			//remove all current contacts
	    	String keysql = "";
	    	for(Field key : rec.getRecordKeys()) {
	    		if(keysql.length() != 0) keysql += " and ";
	    		keysql += "`"+key.getName()+"`" + "=?";
	    	}
			String sql = "DELETE FROM "+table_name+" where " + keysql;
			Connection conn = connectOIM();
			PreparedStatement stmt = conn.prepareStatement(sql);
			int count = 1;
			for(Field key : rec.getRecordKeys()) {
	       		Object value = key.get(rec);
	       		stmt.setObject(count, value);
	    		++count;
			}
			stmt.executeUpdate();
			stmt.close();
		} catch (IllegalArgumentException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (SecurityException e) {
			throw new SQLException(e);
		}
		
		logRemove(rec);
    }
    
    //generated keys are inserted back to rec
    //returns *one of* last inserted record's key. If primary key consists of multiple column, then don't use this
    public Integer insert(RecordBase rec) throws SQLException
    { 	
		//insert new contact records in batch
    	String fields = "";
    	String values = "";
    	for(Field field : rec.getClass().getFields()) {
    		if(fields.length() != 0) {
    			fields += ", ";
    			values += ", ";
    		}
    		fields += "`"+field.getName()+"`";
    		values += "?";
    	}
		String sql = "INSERT INTO "+table_name+" ("+fields+") VALUES ("+values+")";
		Connection conn = connectOIM();
		PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); 
    	try {
	    	//set field values
	    	int count = 1;
	       	for(Field f : rec.getClass().getFields()) {
	       		Object value = f.get(rec);
	       		stmt.setObject(count, value);
	    		++count;
	    	}         
			stmt.executeUpdate(); 
		} catch (IllegalArgumentException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (SecurityException e) {
			throw new SQLException(e);
		}
		
		//attempt to update rec's key fields with newly inserted table keys (if exists) in the order of the key fields.
		//this should work *most of the time*, but if the key fields are not "auto_increment" or if the key is out-of-order
		//(if it's even possible), then this wouldn't work. we could add a new annotation to the record table and
		//do this in more reliable way..
    	Integer a_id = null;
		ResultSet ids = stmt.getGeneratedKeys();  
		if(ids.next()) {
			int count = 1;
	    	for(Field key : rec.getRecordKeys()) {
	    		try {
	    			Integer value = ids.getInt(count);
	    			if(a_id == null) a_id = value;
					key.set(rec, value);
				} catch (IllegalArgumentException e) {
					log.error(e);
				} catch (IllegalAccessException e) {
					log.error(e);
				}
				++count;
	    	}
		}
		
		logInsert(rec);
		stmt.close();
		return a_id;
    }
    
    //find out which fields are changed and do SQL update on those fields
    public void update(RecordBase oldrec, RecordBase newrec) throws SQLException
    {
		//auth.check("write_"+table_name);
    	
    	ArrayList<Field> changed_fields = oldrec.diff(newrec);

		//if nothing has being changed, don't update
    	if(changed_fields.size() == 0) return;
    
    	//construct sql
    	String values = ""; 	
    	for(Field f : changed_fields) {
    		if(values.length() != 0) values += ", ";
    		values += f.getName() + "=?";
    	}
    	String keysql = "";
    	for(Field key : oldrec.getRecordKeys()) {
    		if(keysql.length() != 0) keysql += " and ";
    		keysql += "`"+key.getName()+"`" + "=?";
    	}
    	String sql = "UPDATE " + table_name + " SET " + values + " WHERE " + keysql;
    	PreparedStatement stmt;
    	for(Field f : changed_fields) {
    		if(values.length() != 0) values += ", ";
    		values += f.getName() + "=?";
    	}  	
    	Connection conn = connectOIM();
    	stmt = conn.prepareStatement(sql);
    	try {
	    	//set field values
	    	int count = 1;
	       	for(Field f : changed_fields) {
	       		Object value;
				
				value = f.get(newrec);
	       		stmt.setObject(count, value);
	    		++count;
	    	}    
	       	
	       	//set key values
	    	for(Field key : oldrec.getRecordKeys()) {
	    		Object value = (Object) key.get(oldrec);
	    		stmt.setObject(count, value);
	    		++count;
	    	}
	      
			stmt.executeUpdate(); 
			stmt.close(); 	
		} catch (IllegalArgumentException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (SecurityException e) {
			throw new SQLException(e);
		}
		
		logUpdate(oldrec, newrec);
    }
    
    protected void logInsert(RecordBase rec) throws SQLException 
    {
    	try {
        	String xml = "<Log>\n";
        	xml += "<Type>Insert</Type>\n";
	    	
    		//show key fields
	    	xml += "<Keys>\n";
	    	ArrayList<Field> keys = rec.getRecordKeys();
	    	for(Field key : keys) {
	    		String name = key.getName();
	    		Object value = (Object) key.get(rec);   		
	    		xml += "<Key>\n";
	    		xml += "\t<Name>" + StringEscapeUtils.escapeXml(name) + "</Name>\n";
	    		xml += "\t<Value>" + formatValue(value) + "</Value>\n";
	    		xml += "</Key>\n";
	    	}
	    	xml += "</Keys>\n";
	    	xml += "<Fields>\n";
	    	for(Field f : rec.getClass().getFields()) {
	    		if(keys.contains(f)) continue;	//don't show key field    		
	    		String name = f.getName();
	    		Object value = f.get(rec);
	    		
	    		xml += "<Field>\n";
	    		xml += "\t<Name>" + StringEscapeUtils.escapeXml(name) + "</Name>\n";
	    		xml += "\t<Value>" + formatValue(value) + "</Value>\n";
	    		xml += "</Field>\n";
	    	}
	    	xml += "</Fields>\n";
	    	xml += "</Log>";
	    	
			LogModel lmodel = new LogModel(context);
			int logid = lmodel.insert("insert", getClass().getName(), xml);	    

		} catch (IllegalArgumentException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (SecurityException e) {
			throw new SQLException(e);
		}   	
    }
    
    protected void logRemove(RecordBase rec) throws SQLException 
    {
    	try {
	    	//String plog = "By " + auth.getContact().name;
        	String xml = "<Log>\n";
        	xml += "<Type>Remove</Type>\n";
	    	
    		//show key fields
	    	//plog += "<table width=\"100%\">";
	    	xml += "<Keys>\n";
	    	ArrayList<Field> keys = rec.getRecordKeys();
	    	for(Field key : keys) {
	    		String name = key.getName();
	    		Object value = (Object) key.get(rec);
	    		/*
	    		plog += "<tr><th>"+StringEscapeUtils.escapeHtml(name)+"</th>"+
	    			"<td>"+StringEscapeUtils.escapeHtml(rec.toString(key, auth))+"</td></tr>";
	    		*/
	    		xml += "<Key>\n";
	    		xml += "\t<Name>" + StringEscapeUtils.escapeXml(name) + "</Name>\n";
	    		xml += "\t<Value>" + formatValue(value) + "</Value>\n";
	    		xml += "</Key>\n";
	    	}
	    	xml += "</Keys>\n";
	    	xml += "<Fields>\n";
	    	for(Field f : rec.getClass().getFields()) {
	    		if(keys.contains(f)) continue;	//don't show key field    		
	    		String name = f.getName();
	    		Object value = f.get(rec);

	    		xml += "<Field>\n";
	    		xml += "\t<Name>" + StringEscapeUtils.escapeXml(name) + "</Name>\n";
	    		xml += "\t<Value>" + formatValue(value) + "</Value>\n";
	    		xml += "</Field>\n";
	    		/*
	    		if(rec.isRestricted(f)) {
	    			value = NonPublicInformation;
	    		}
	    		*/
	    		/*
    			plog += "<tr><th>"+StringEscapeUtils.escapeHtml(name)+"</th>"+
    			"<td>"+StringEscapeUtils.escapeHtml(rec.toString(f, auth))+"</td></tr>";
    			*/
	    	}
	    	//plog += "</table>";
	    	xml += "</Fields>\n";
	    	xml += "</Log>";
			LogModel lmodel = new LogModel(context);
			int logid = lmodel.insert("remove", getClass().getName(), xml);	  
			/*
			plog += "Log ID " + logid;
			
			try {
				PublicNotification.publish("Removed " + rec.getTitle(), plog, rec.getLables());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		} catch (IllegalArgumentException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (SecurityException e) {
			throw new SQLException(e);
		}
    }
    
    protected void logUpdate(RecordBase oldrec, RecordBase newrec) throws SQLException 
    {   	
    	try {
	    	//String plog = "By " + auth.getContact().name;
        	String xml = "<Log>\n";
        	xml += "<Type>Update</Type>\n";
	    	
    		//show key fields
        	//plog += "<table width=\"100%\"><tr><th>Updated Field</th><th>Old Value</th><th>New Value</th></tr>";
	    	xml += "<Keys>\n";
	    	for(Field key : oldrec.getRecordKeys()) {
	    		String name = key.getName();
	    		Object value = (Object) key.get(oldrec);
	    		
	    		//don't need to show the record key for updates
	    		/*	
	    		plog += "<tr>";
	    		plog += "<th>"+StringEscapeUtils.escapeHtml(name)+"</th>"+
    			"<td>"+StringEscapeUtils.escapeHtml(oldrec.toString(value, auth))+"</td>";
	    		plog += "<td></td></tr>";
	    		*/
	    		
	    		xml += "<Key>\n";
	    		xml += "\t<Name>" + StringEscapeUtils.escapeXml(name) + "</Name>\n";
	    		xml += "\t<Value>" + formatValue(value) + "</Value>\n";
	    		xml += "</Key>\n";
	    	}
	    	xml += "</Keys>\n";
	    	xml += "<Fields>\n";
	    	for(Field f : oldrec.diff(newrec)) {
	    		String name = f.getName();
	    		Object oldvalue = f.get(oldrec);
	    		Object newvalue = f.get(newrec);
	    		   		
	    		xml += "<Field>\n";
	    		xml += "\t<Name>" + StringEscapeUtils.escapeXml(name) + "</Name>\n";
	    		xml += "\t<OldValue>" + formatValue(oldvalue) + "</OldValue>\n";
	    		xml += "\t<NewValue>" + formatValue(newvalue) + "</NewValue>\n";
	    		xml += "</Field>\n";
	    		/*
	    		if(oldrec.isRestricted(f)) {
	    			oldvalue = NonPublicInformation;
	    			newvalue = NonPublicInformation;
	    		}
	    		*/
	    		/*
	    		plog += "<tr><th>"+StringEscapeUtils.escapeHtml(name)+"</th>"+
    			"<td>"+StringEscapeUtils.escapeHtml(oldrec.toString(f, auth))+"</td>" +
    			"<td>"+StringEscapeUtils.escapeHtml(newrec.toString(f, auth))+"</td></tr>";
    			*/
	    	}
	    	//plog += "</table>";
	    	xml += "</Fields>\n";
	    	xml += "</Log>";
			LogModel lmodel = new LogModel(context);
			int logid = lmodel.insert("update", getClass().getName(), xml);
			/*
			plog += "Log ID " + logid;
			try {
				PublicNotification.publish("Updated " + oldrec.getTitle(), plog, oldrec.getLables());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		} catch (IllegalArgumentException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (SecurityException e) {
			throw new SQLException(e);
		} 
    }
    
    private String formatValue(Object obj)
    {
    	if(obj == null) return LogModel.NULL_TOKEN;
    	return StringEscapeUtils.escapeXml(obj.toString());
    }
    

	//override this to reveal the log to particular user
	abstract public Boolean hasLogAccess(XPath xpath, Document doc) throws XPathExpressionException;
}
