package org.openmrs.module.rgrta.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
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
import org.openmrs.module.atd.hibernateBeans.Statistics;
import org.openmrs.module.rgrta.hibernateBeans.Study;
import org.openmrs.module.rgrta.hibernateBeans.StudyAttributeValue;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface RgrtaService
{
	public void consume(InputStream input, Patient patient,
			Integer encounterId,FormInstance formInstance,Integer sessionId,
			List<FormField> fieldsToConsume,Integer locationTagId);

	public void produce(OutputStream output, PatientState state,
			Patient patient,Integer encounterId,String dssType,
			int maxDssElements,Integer sessionId);

	
	
	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Percentile getWtageinf(double ageMos, int sex);

	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Bmiage getBmiage(double ageMos, int sex);
	
	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Hcageinf getHcageinf(double ageMos, int sex);

	/**
	 * @param ageMos
	 * @param sex
	 * @return
	 */
	public Lenageinf getLenageinf(double ageMos, int sex);

	public List<Study> getActiveStudies();

	public List<Statistics> getStatByFormInstance(int formInstanceId,String formName,
			Integer locationId);

	public StudyAttributeValue getStudyAttributeValue(Study study,
			String studyAttributeName);

	public List<Statistics> getStatByIdAndRule(int formInstanceId,int ruleId,String formName,
			Integer locationId);

	
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
	
	public PatientState getPrevProducePatientState(Integer sessionId, Integer patientStateId );
	
	public Double getHighBP(Patient patient, Integer bpPercentile, String bpType, org.openmrs.Encounter encounter);
	
	public Double getHighBP(Patient patient, Integer bpPercentile, String bpType, 
			Double heightPercentile, Date onDate);
		
	public String getDDSTLeaf(String category, Integer ageInDays);
	
	public List<Statistics> getStatsByEncounterForm(Integer encounterId,String formName);

	public List<Statistics> getStatsByEncounterFormNotPrioritized(Integer encounterId,String formName);
	
	public RgrtaHL7Export insertEncounterToHL7ExportQueue(RgrtaHL7Export export);

	public List<RgrtaHL7Export> getPendingHL7Exports();
	
	public RgrtaHL7Export getNextPendingHL7Export(String optionalResend, String resendNoAck);


	
	public void saveRgrtaHL7Export(RgrtaHL7Export export);
	
	public List<RgrtaHL7Export> getPendingHL7ExportsByEncounterId(Integer encounterId);
	
	/**
	 * @param patientId
	 * @param optionalDateRestrictio
	 * 
	 * Search patient states to determine if a reprint has ever been performed during that
	 * session.
	 * 
	 * @return
	 */
	public List<PatientState> getReprintRescanStatesByEncounter(Integer encounterId, Date optionalDateRestriction, Integer locationTagId, Integer locationId);
	
	/**
	 * Gets a list of the printer stations for PSF
	 * @return List of form attributes
	 */
	public List<String> getPrinterStations(Location location);
	
	public void  saveHL7ExportMap (RgrtaHL7ExportMap map);
	
	public RgrtaHL7ExportMap getRgrtaExportMapByQueueId(Integer queue_id);
	
	public RgrtaHL7ExportStatus getRgrtaExportStatusByName (String name);
	
	public RgrtaHL7ExportStatus getRgrtaExportStatusById (Integer id);
	
}