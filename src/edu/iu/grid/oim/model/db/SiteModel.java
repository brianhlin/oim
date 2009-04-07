package edu.iu.grid.oim.model.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.db.record.RecordBase;
import edu.iu.grid.oim.model.db.record.SiteRecord;

public class SiteModel extends SmallTableModelBase<SiteRecord> {
    static Logger log = Logger.getLogger(SiteModel.class); 

	public SiteModel(Authorization _auth) {
		super(_auth, "sc_contact");
	}
	SiteRecord createRecord(ResultSet rs) throws SQLException
	{
		return new SiteRecord(rs);
	}
	public ArrayList<SiteRecord> getAll() throws SQLException
	{
		ArrayList<SiteRecord> list = new ArrayList<SiteRecord>();
		for(RecordBase it : getCache()) {
			list.add((SiteRecord)it);
		}
		return list;
	}
}