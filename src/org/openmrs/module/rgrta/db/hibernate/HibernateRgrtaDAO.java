package org.openmrs.module.rgrta.db.hibernate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.Drug;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.rgrta.Percentile;
import org.openmrs.module.rgrta.db.RgrtaDAO;
import org.openmrs.module.rgrta.hibernateBeans.Bmiage;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7Export;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportMap;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportStatus;
import org.openmrs.module.rgrta.hibernateBeans.DDST_Milestone;
import org.openmrs.module.rgrta.hibernateBeans.Family;
import org.openmrs.module.rgrta.hibernateBeans.Hcageinf;
import org.openmrs.module.rgrta.hibernateBeans.Lenageinf;
import org.openmrs.module.rgrta.hibernateBeans.OldRule;
import org.openmrs.module.rgrta.hibernateBeans.PatientFamily;
import org.openmrs.module.atd.hibernateBeans.Statistics;
import org.openmrs.module.rgrta.hibernateBeans.Study;
import org.openmrs.module.rgrta.hibernateBeans.StudyAttribute;
import org.openmrs.module.rgrta.hibernateBeans.StudyAttributeValue;
import org.openmrs.module.rgrta.hibernateBeans.Wtageinf;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationTagAttributeValue;
import org.openmrs.module.chirdlutil.util.Util;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of Rgrta database methods.
 * 
 * @author Tammy Dugan
 * 
 */
@Transactional
public class HibernateRgrtaDAO implements RgrtaDAO
{

	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;

	/**
	 * 
	 */
	public HibernateRgrtaDAO()
	{
	}

	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory)
	{
		this.sessionFactory = sessionFactory;
	}

	public void addStatistics(Statistics statistics)
	{
		try
		{
			this.sessionFactory.getCurrentSession().save(statistics);
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
	}

	public void updateStatistics(Statistics statistics)
	{
		try
		{
			this.sessionFactory.getCurrentSession().update(statistics);
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
	}

	public List<Statistics> getStatByFormInstance(int formInstanceId,
			String formName, Integer locationId)
	{
		try
		{
			String sql = "select * from atd_statistics where form_instance_id=? and form_name=? "+
			"and location_id=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, formInstanceId);
			qry.setString(1, formName);
			qry.setInteger(2,locationId);
			qry.addEntity(Statistics.class);
			return qry.list();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public List<OldRule> getAllOldRules()
	{
		try
		{
			String sql = "select * from Rgrta_old_rule";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.addEntity(OldRule.class);
			return qry.list();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public List<Statistics> getStatByIdAndRule(int formInstanceId, int ruleId,
			String formName, Integer locationId)
	{
		try
		{
			String sql = "select * from atd_statistics where form_instance_id=? "+
			"and rule_id=? and form_name=? and location_id=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, formInstanceId);
			qry.setInteger(1, ruleId);
			qry.setString(2, formName);
			qry.setInteger(3,locationId);
			qry.addEntity(Statistics.class);
			return qry.list();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Percentile getWtageinf(double ageMos, int sex)
	{
		try
		{
			String sql = "select * from Rgrta_wtageinf where agemos=? and sex=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setDouble(0, ageMos);
			qry.setInteger(1, sex);
			qry.addEntity(Wtageinf.class);
			return (Percentile) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Bmiage getBmiage(double ageMos, int sex)
	{
		try
		{
			String sql = "select * from Rgrta_bmiage where agemos=? and sex=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setDouble(0, ageMos);
			qry.setInteger(1, sex);
			qry.addEntity(Bmiage.class);
			return (Bmiage) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Hcageinf getHcageinf(double ageMos, int sex)
	{
		try
		{
			String sql = "select * from Rgrta_hcageinf where agemos=? and sex=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setDouble(0, ageMos);
			qry.setInteger(1, sex);
			qry.addEntity(Hcageinf.class);
			return (Hcageinf) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Lenageinf getLenageinf(double ageMos, int sex)
	{
		try
		{
			String sql = "select * from Rgrta_lenageinf where agemos=? and sex=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setDouble(0, ageMos);
			qry.setInteger(1, sex);
			qry.addEntity(Lenageinf.class);
			return (Lenageinf) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public List<Study> getActiveStudies()
	{
		try
		{
			String sql = "select * from Rgrta_study where status=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, 1);
			qry.addEntity(Study.class);
			return qry.list();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	private StudyAttribute getStudyAttributeByName(String studyAttributeName)
	{
		try
		{
			String sql = "select * from Rgrta_study_attribute "
					+ "where name=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setString(0, studyAttributeName);
			qry.addEntity(StudyAttribute.class);

			List<StudyAttribute> list = qry.list();

			if (list != null && list.size() > 0)
			{
				return list.get(0);
			}
			return null;
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public StudyAttributeValue getStudyAttributeValue(Study study,
			String studyAttributeName)
	{
		try
		{
			StudyAttribute studyAttribute = this
					.getStudyAttributeByName(studyAttributeName);

			if (study != null && studyAttribute != null)
			{
				Integer studyId = study.getStudyId();
				Integer studyAttributeId = studyAttribute.getStudyAttributeId();

				String sql = "select * from Rgrta_study_attribute_value where study_id=? and study_attribute_id=?";
				SQLQuery qry = this.sessionFactory.getCurrentSession()
						.createSQLQuery(sql);

				qry.setInteger(0, studyId);
				qry.setInteger(1, studyAttributeId);
				qry.addEntity(StudyAttributeValue.class);

				List<StudyAttributeValue> list = qry.list();

				if (list != null && list.size() > 0)
				{
					return list.get(0);
				}

			}
			return null;
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public String getInsCategoryByCarrier(String carrierCode)
	{
		try
		{
			String sql = "select distinct category from Rgrta_insurance_category where star_carrier_code=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setString(0, carrierCode);
			qry.addScalar("category");
			return (String) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public String getInsCategoryByInsCode(String insCode)
	{
		try
		{
			String sql = "select distinct category from Rgrta_insurance_category where ins_code=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.addScalar("category");
			qry.setString(0, insCode);

			return (String) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public String getInsCategoryBySMS(String smsCode)
	{
		try
		{
			String sql = "select distinct category from Rgrta_insurance_category where sms_code=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.addScalar("category");
			qry.setString(0, smsCode);

			return (String) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public List<String> getInsCategories()
	{
		try
		{
			String sql = "select distinct category from Rgrta_insurance_category " +
			"where category is not null and category <> '' order by category";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.addScalar("category");

			List<String> list = qry.list();
			ArrayList<String> categories = new ArrayList<String>();
			for (String currResult : list)
			{
				categories.add(currResult);
			}

			return categories;
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public PatientFamily getPatientFamily(Integer patientId)
	{
		try
		{
			String sql = "select * from Rgrta_patient_family where patient_id=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, patientId);
			qry.addEntity(PatientFamily.class);
			return (PatientFamily) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Obs getStudyArmObs(Integer familyId, Concept studyConcept)
	{
		try
		{
			String sql = "select * from Rgrta_patient_family where family_id=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, familyId);
			qry.addEntity(PatientFamily.class);
			List<PatientFamily> patientFamilies = qry.list();
			ObsService obsService = Context.getObsService();
			PatientService patientService = Context.getPatientService();

			if (patientFamilies != null && patientFamilies.size() > 0)
			{
				PatientFamily patientFamily = patientFamilies.get(0);
				Integer patientId = patientFamily.getPatientId();
				Patient patient = patientService.getPatient(patientId);
				List<Person> persons = new ArrayList<Person>();
				persons.add(patient);
				List<Concept> questions = new ArrayList<Concept>();
				questions.add(studyConcept);
				List<Obs> obs = obsService.getObservations(persons, null,
						questions, null, null, null, null, null, null, null,
						null, false);

				if (obs != null && obs.size() > 0)
				{
					return obs.get(0);
				}
			}
			return null;
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Family getFamilyByAddress(String address)
	{
		try
		{
			String sql = "select * from Rgrta_family where street_address=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setString(0, address);
			qry.addEntity(Family.class);
			return (Family) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Family getFamilyByPhone(String phone)
	{
		try
		{
			String sql = "select * from Rgrta_family where phone_num=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setString(0, phone);
			qry.addEntity(Family.class);
			return (Family) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public void savePatientFamily(PatientFamily patientFamily)
	{
		try
		{
			this.sessionFactory.getCurrentSession().save(patientFamily);
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
	}

	public void saveFamily(Family family)
	{
		try
		{
			this.sessionFactory.getCurrentSession().save(family);
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
	}

	public void updateFamily(Family family)
	{
		try
		{
			this.sessionFactory.getCurrentSession().update(family);
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
	}


	public String getObsvNameByObsvId(String obsvId)
	{
		try
		{
			Connection con = this.sessionFactory.getCurrentSession()
					.connection();
			String sql = "select obsv_name from Rgrta1_obsv_dictionary where obsv_id = ?";

			try
			{
				PreparedStatement stmt = con.prepareStatement(sql);
				stmt.setString(1, obsvId);

				ResultSet rs = stmt.executeQuery();
				if (rs.next())
				{
					return rs.getString(1);
				}
				stmt.close();
			} catch (Exception e)
			{
				this.log.error(e.getMessage());
				this.log.error(Util.getStackTrace(e));
			}
			return null;
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public Integer getHighBP(Integer ageInYears, String sex,
			Integer bpPercentile, String bpType, Integer heightPercentile)
	{
		try
		{
			String bpColumn = "Systolic";

			if (bpType.equalsIgnoreCase("diastolic"))
			{
				bpColumn = "Diastolic";
			}

			String sql = "select " + bpColumn + "_HT" + heightPercentile
					+ " from Rgrta_high_bp where Age=? and Sex=?"
					+ " and BP_Percentile=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, ageInYears);
			qry.setString(1, sex);
			qry.setInteger(2, bpPercentile);
			qry.addScalar(bpColumn + "_HT" + heightPercentile);
			return (Integer) qry.uniqueResult();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}

	public String getDDSTLeaf(String category, Integer ageInDays)
	{
		try
		{
			String sql = "select * from Rgrta_ddst where category=? and cutoff_age <= ? order by cutoff_age desc";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setString(0, category);
			qry.setInteger(1, ageInDays);
			qry.addEntity(DDST_Milestone.class);
			if (qry.list().size() > 0)
			{
				DDST_Milestone milestone = (DDST_Milestone) qry.list().get(0);
				return milestone.getMilestone();
			}
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}
	
	public List<Statistics> getStatsByEncounterForm(Integer encounterId,String formName)
	{
		try
		{
			String sql = "select * from atd_statistics where obsv_id is not null and encounter_id=? and form_name=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, encounterId);
			qry.setString(1, formName);
			qry.addEntity(Statistics.class);
			return qry.list();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}
	
	public List<Statistics> getStatsByEncounterFormNotPrioritized(Integer encounterId,String formName)
	{
		try
		{
			String sql = "select * from atd_statistics where rule_id is null and obsv_id is not null and encounter_id=? and form_name=?";
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			qry.setInteger(0, encounterId);
			qry.setString(1, formName);
			qry.addEntity(Statistics.class);
			return qry.list();
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
		return null;
	}
	
	public RgrtaHL7Export insertEncounterToHL7ExportQueue(RgrtaHL7Export export){
		sessionFactory.getCurrentSession().saveOrUpdate(export);
		return export;
	}
	
	public List <RgrtaHL7Export> getPendingHL7Exports(){
		
		// pending messages in queue and messages with status of
		//open_socket_failed
		SQLQuery qry = this.sessionFactory.getCurrentSession()
			.createSQLQuery("select * from Rgrta_hl7_export " +
		" where (date_processed is null and voided = 0 and status = 1) " +
		" || status = 8 ");

		qry.addEntity(RgrtaHL7Export.class);
		List <RgrtaHL7Export> exports = qry.list();
		return exports;
	}
	
	public RgrtaHL7Export getNextPendingHL7Export(String resendImagesNotFound, String resendNoAck){
		
		try {
			String resend = "";
			String resendNoAckExtension = "";
			if (resendImagesNotFound != null && (resendImagesNotFound.equalsIgnoreCase("yes")||
					resendImagesNotFound.equalsIgnoreCase("true")))
			{
				RgrtaHL7ExportStatus status = getRgrtaExportStatusByName("image_not_found");
				if (status != null ){
					resend = " or status = " + status.getHl7ExportStatusId() ;
				}
			} 
			
			if (resendNoAck != null && (resendNoAck.equalsIgnoreCase("yes")||
					resendNoAck.equalsIgnoreCase("true")))
			{
				RgrtaHL7ExportStatus status = getRgrtaExportStatusByName("ACK_not_received");
				RgrtaHL7ExportStatus statusSocket = getRgrtaExportStatusByName("open_socket_failed");
				if (status != null ){
					resendNoAckExtension = " or status = " + status.getHl7ExportStatusId()
					 +  " or status = " + statusSocket.getHl7ExportStatusId();
				}
			} 
			
			SQLQuery qry = this.sessionFactory.getCurrentSession()
				.createSQLQuery("select * from Rgrta_hl7_export " +
			" where voided = 0 and ((status = 1 and date_processed is null) " 
						+ resend + resendNoAckExtension + " ) order by date_inserted ");

			
			qry.addEntity(RgrtaHL7Export.class);
			List <RgrtaHL7Export> exports = qry.list();
			if (exports != null && exports.size() > 0) {
				return exports.get(0);
			}
		} catch (HibernateException e) {
			log.error(e);
		}
		
		return null;
	}

	
	public void saveRgrtaHL7Export(RgrtaHL7Export export) {
		this.sessionFactory.getCurrentSession().saveOrUpdate(export);
		return;
	}
	
	public List<RgrtaHL7Export> getPendingHL7ExportsByEncounterId(Integer encounterId){
		SQLQuery qry = this.sessionFactory.getCurrentSession()
		.createSQLQuery("select * from Rgrta_hl7_export where encounter_id = ? " + 
				" and date_processed is null and voided = 0 order by date_inserted desc");
		qry.setInteger(0, encounterId);
		qry.addEntity(RgrtaHL7Export.class);
		List <RgrtaHL7Export> exports = qry.list();
		return exports;
	}
	
	public List<PatientState> getReprintRescanStatesByEncounter(Integer encounterId, Date optionalDateRestriction, 
			Integer locationTagId,Integer locationId){
		
		try
		{
			
			String dateRestriction = "";
			if (optionalDateRestriction != null)
			{
				dateRestriction = " and start_time >= ?";
			} 
			
			String sql = "select * from atd_patient_state a "+
						"inner join atd_session b on a.session_id=b.session_id where state in ("+
						"select state_id from atd_state where state_action_id in ("+
						"select state_action_id from atd_state_action where action_name in ('RESCAN','REPRINT')) "+
						") "+
						"and encounter_id=? and retired=? and location_tag_id=? and location_id=? "+dateRestriction;
			
			SQLQuery qry = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sql);
			
			qry.setInteger(0, encounterId);
			qry.setBoolean(1, false);
			qry.setInteger(2, locationTagId);
			qry.setInteger(3, locationId);
			
			if (optionalDateRestriction != null)
			{
				qry.setDate(4, optionalDateRestriction);
			}
			
			qry.addEntity(PatientState.class);
			return qry.list();
		} catch (Exception e)
		{
			log.error(Util.getStackTrace(e));
		}
		return null;
	}
	
	public void  saveHL7ExportMap (RgrtaHL7ExportMap map){
		try
		{
			this.sessionFactory.getCurrentSession().save(map);
		} catch (Exception e)
		{
			this.log.error(Util.getStackTrace(e));
		}
	}
	
	public RgrtaHL7ExportMap getRgrtaExportMapByQueueId(Integer queueId){
		try {
			SQLQuery qry = this.sessionFactory.getCurrentSession()
			.createSQLQuery("select * from Rgrta_hl7_export_map " +
			" where hl7_export_queue_id = ?");
			qry.setInteger(0, queueId);
			qry.addEntity(RgrtaHL7ExportMap.class);
			List<RgrtaHL7ExportMap> list = qry.list();
			if (list != null && list.size() > 0) {
				return list.get(0);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public RgrtaHL7ExportStatus getRgrtaExportStatusByName (String name){
		/*try {
			SQLQuery qry = this.sessionFactory.getCurrentSession()
			.createSQLQuery("select * from Rgrta_hl7_export_status " +
			" where name = ?");
			qry.setString(0, name);
			qry.addEntity(RgrtaHL7ExportStatus.class);
			List<RgrtaHL7ExportStatus> list = qry.list();
			if (list != null && list.size() > 0) {
				return list.get(0);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;*/
		
		
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(RgrtaHL7ExportStatus.class).add(
			    Expression.eq("name", name));
			try {
				if (crit.list().size() < 1) {
					log.warn("No export status found with name: " + name);
					return null;
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			return (RgrtaHL7ExportStatus) crit.list().get(0);
	}
	
	public RgrtaHL7ExportStatus getRgrtaExportStatusById (Integer id){
		
		try {
			SQLQuery qry = this.sessionFactory.getCurrentSession()
			.createSQLQuery("select * from Rgrta_hl7_export_status " +
			" where hl7_export_status_id = ?");
			qry.setInteger(0, id);
			qry.addEntity(RgrtaHL7ExportStatus.class);
			List<RgrtaHL7ExportStatus> list = qry.list();
			if (list != null && list.size() > 0) {
				return list.get(0);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	
}
