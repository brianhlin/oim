package edu.iu.grid.oim.model.db;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.record.ContactRecord;

public class CertificateQuotaModel {
	static Logger log = Logger.getLogger(CertificateRequestModelBase.class);  
	private UserContext context;
	
    public CertificateQuotaModel(UserContext context) {
		this.context = context;
	}
    
    public boolean canRequestUserCert() {
		//reached global max?
		ConfigModel config = new ConfigModel(context);
		Integer global_max = config.QuotaGlobalUserCertYearMax.getInteger();
		Integer global_count = config.QuotaGlobalUserCertYearCount.getInteger();		
		if(global_count >= global_max) return false;
		
		//reached personal max?
		Authorization auth = context.getAuthorization();
		if(auth.isUser()) {
			ContactRecord user = auth.getContact();
			Integer user_max = config.QuotaUserCertYearMax.getInteger();
			if(user.count_usercert_year >= user_max) return false;
		}
		return true;
    }
    
    public void incrementUserCertRequest() throws SQLException {
		//increment global count
		ConfigModel config = new ConfigModel(context);
		Integer global_count = config.QuotaGlobalUserCertYearCount.getInteger();		
		global_count++;
		config.QuotaGlobalUserCertYearCount.set(global_count.toString());
		
		//increment user count
		Authorization auth = context.getAuthorization();
		if(auth.isUser()) {
			ContactRecord user = auth.getContact();
			user.count_usercert_year++;
			ContactModel cmodel = new ContactModel(context);
			cmodel.emptyCache();//force next get() to pull from the DB instead of cache - which I just updated..
			cmodel.update(cmodel.get(user), user);
		}
    }
    
    public boolean canApproveHostCert(int count) {
		//reached global max?
		ConfigModel config = new ConfigModel(context);
		Integer global_max = config.QuotaGlobalHostCertYearMax.getInteger();
		Integer global_count = config.QuotaGlobalHostCertYearCount.getInteger();		
		if(global_count + count >= global_max) return false;   	
		
		//reached personal day max?
		Authorization auth = context.getAuthorization();
		ContactRecord user = auth.getContact();
		Integer day_max = config.QuotaUserHostDayMax.getInteger();
		if(user.count_hostcert_day + count >= day_max) return false;
		
		//reached personal year max?
		Integer year_max = config.QuotaUserHostYearMax.getInteger();
		if(user.count_hostcert_year + count >= year_max) return false;
		
		return true;
    }
    
    public void incrementHostCertApproval(int inc) throws SQLException {
		//increment global count
		ConfigModel config = new ConfigModel(context);
		Integer global_count = config.QuotaGlobalHostCertYearCount.getInteger();		
		global_count+=inc;
		config.QuotaGlobalHostCertYearCount.set(global_count.toString());
		
		//increment user count
		Authorization auth = context.getAuthorization();
		if(auth.isUser()) {
			ContactRecord user = auth.getContact();
			user.count_hostcert_year++;
			user.count_hostcert_day++;
			ContactModel cmodel = new ContactModel(context);
			cmodel.emptyCache();//force next get() to pull from the DB instead of cache - which I just updated..
			cmodel.update(cmodel.get(user), user);
		}
    }
}
