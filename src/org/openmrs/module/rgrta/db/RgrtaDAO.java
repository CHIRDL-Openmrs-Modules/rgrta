package org.openmrs.module.rgrta.db;


import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.rgrta.Percentile;
import org.openmrs.module.rgrta.hibernateBeans.Bmiage;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7Export;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportMap;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportStatus;
import org.openmrs.module.rgrta.hibernateBeans.Family;
import org.openmrs.module.rgrta.hibernateBeans.Hcageinf;
import org.openmrs.module.rgrta.hibernateBeans.Lenageinf;
import org.openmrs.module.rgrta.hibernateBeans.OldRule;
import org.openmrs.module.rgrta.hibernateBeans.PatientFamily;
import org.openmrs.module.rgrta.hibernateBeans.Statistics;
import org.openmrs.module.rgrta.hibernateBeans.Study;
import org.openmrs.module.rgrta.hibernateBeans.StudyAttributeValue;


/**
 * Rgrta-related database functions
 * 
 * @author Tammy Dugan
 * @version 1.0
 */
public interface RgrtaDAO {

	
		/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Percentile getWtageinf(double ageMos,int sex);
	
	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Bmiage getBmiage(double ageMos,int sex);
	
	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Hcageinf getHcageinf(double ageMos,int sex);
	
	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Lenageinf getLenageinf(double ageMos,int sex);
	
	public void addStatistics(Statistics statistics);
	
	public void updateStatistics(Statistics statistics);
	
	public List<Study> getActiveStudies();
	
	public List<Statistics> getStatByFormInstance(int formInstanceId,String formName, Integer locationId);

	public List<Statistics> getStatByIdAndRule(int formInstanceId,int ruleId,String formName, Integer locationId);
		
	public StudyAttributeValue getStudyAttributeValue(Study study,String studyAttributeName);

	public List<OldRule> getAllOldRules();

	public PatientFamily getPatientFamily(Integer patientId);
	
	public Family getFamilyByAddress(String address);
	
	public Family getFamilyByPhone(String phone);
	
	public void savePatientFamily(PatientFamily patientFamily);
	
	public void saveFamily(Family family);
	
	public void updateFamily(Family family);
	
	public Obs getStudyArmObs(Integer familyId,Concept studyConcept);

	public List<String> getInsCategories();
	
	public String getObsvNameByObsvId(String obsvId);
	
	public String getInsCategoryByCarrier(String carrierCode);
	
	public String getInsCategoryBySMS(String smsCode);
	
	public String getInsCategoryByInsCode(String insCode);
	
	public Integer getHighBP(Integer ageInYears, String sex,
			Integer bpPercentile, String bpType, Integer heightPercentile);

	public String getDDSTLeaf(String category, Integer ageInDays);
	
	public List<Statistics> getStatsByEncounterForm(Integer encounterId,String formName);

	public List<Statistics> getStatsByEncounterFormNotPrioritized(Integer encounterId,String formName);
	
	public RgrtaHL7Export insertEncounterToHL7ExportQueue(RgrtaHL7Export export);

	public List<RgrtaHL7Export> getPendingHL7Exports();
	
	public void saveRgrtaHL7Export(RgrtaHL7Export export);
	
	public List<RgrtaHL7Export> getPendingHL7ExportsByEncounterId(Integer encounterId);
	
	public List<PatientState> getReprintRescanStatesByEncounter(Integer encounterId, Date optionalDateRestriction, 
			Integer locationTagId,Integer locationId);
		
	/** Insert queued hl7 export to map table
	 * @param map
	 * @return
	 */
	public void  saveHL7ExportMap (RgrtaHL7ExportMap map);
	
	public RgrtaHL7ExportMap getRgrtaExportMapByQueueId(Integer queue_id);
	
	public RgrtaHL7ExportStatus getRgrtaExportStatusByName (String name);
	
	public RgrtaHL7ExportStatus getRgrtaExportStatusById (Integer id);

	public List<Object[]> getFormsPrintedByWeek(String formName, String locationName);
	
	public List<Object[]> getFormsScannedByWeek(String formName, String locationName);
	
	public List<Object[]> getFormsScannedAnsweredByWeek(String formName, String locationName);
	
	public List<Object[]> getFormsScannedAnythingMarkedByWeek(String formName, String locationName);
	
	public List<Object[]> getQuestionsScanned(String formName, String locationName);

	public List<Object[]> getQuestionsScannedAnswered(String formName, String locationName);
}
