package org.openmrs.module.rgrta.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.hibernateBeans.FormAttributeValue;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationTagAttributeValue;
import org.openmrs.module.chirdlutil.service.ChirdlUtilService;
import org.openmrs.module.rgrta.service.RgrtaService;
import org.openmrs.module.rgrta.test.TestUtil;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.chirdlutil.util.XMLUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

/**
 * This Class tests the RgrtaServiceImpl class.
 * 
 * IMPORTANT: This test class needs more than eclipse's default amount of JVM
 * memory to run since it is compiling java files. Please increase you JVM
 * memory to 256 MB or higher.
 * 
 * @author tmdugan
 * 
 */
@SkipBaseSetup
public class RgrtaServiceImplTest extends BaseModuleContextSensitiveTest
{
	/**
	 * @verifies {@link RgrtaServiceImpl#getHighBP(Patient,Integer,String,Encounter)}
	 *           test = should checkHighBP
	 */
	@Test
	public void getHighBP_shouldCheckHighBP() throws Exception
	{
		Calendar onDate = Calendar.getInstance();
		onDate.set(2008, Calendar.DECEMBER, 22);
		RgrtaService RgrtaService = Context.getService(RgrtaService.class);
		Patient patient = new Patient();
		EncounterService encounterService = Context.getEncounterService();
		Encounter encounter = encounterService.getEncounter(3);
		Calendar birthDate = null;
		Calendar baseBirthDate = Calendar.getInstance();
		baseBirthDate.set(2006, Calendar.DECEMBER, 22);
		patient.setBirthdate(baseBirthDate.getTime());
		patient.setGender("F"); // female
		Integer bpPercentile = 90; // high is 90th percentile
		String bpType = "systolic"; // systolic
		Double heightPercentile = 5.0;

		// test gender
		// test F
		patient.setGender("F");
		Double highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// test M
		patient.setGender("M");
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(98, highBP, 2);

		// test age
		// 0 years
		patient.setGender("F"); // female
		bpPercentile = 90; // high is 90th percentile
		bpType = "systolic"; // systolic
		heightPercentile = 5.0;

		birthDate = onDate;
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(97, highBP, 0);

		// 9 months
		birthDate = Calendar.getInstance();
		birthDate.set(2008, Calendar.MARCH, 22);
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(97, highBP, 0);

		// 1 year
		birthDate = Calendar.getInstance();
		birthDate.set(2007, Calendar.DECEMBER, 22);
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(97, highBP, 0);

		// 15 months
		birthDate = Calendar.getInstance();
		birthDate.set(2007, Calendar.SEPTEMBER, 22);
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(97.5, highBP, 0);

		// 16.5 years
		birthDate = Calendar.getInstance();
		birthDate.set(1992, Calendar.JUNE, 22);
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(122, highBP, 0);

		// 17 years
		birthDate = Calendar.getInstance();
		birthDate.set(1991, Calendar.DECEMBER, 22);
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(122, highBP, 0);

		// 18 years
		birthDate = Calendar.getInstance();
		birthDate.set(1990, Calendar.DECEMBER, 22);
		patient.setBirthdate(birthDate.getTime());
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(null, highBP);

		// test systolic/diastolic
		// systolic
		patient.setBirthdate(baseBirthDate.getTime());
		patient.setGender("F"); // female
		bpPercentile = 90; // high is 90th percentile
		heightPercentile = 5.0;

		bpType = "systolic";
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// diastolic
		bpType = "diastolic";
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(57, highBP, 0);

		// test height percentile
		patient.setBirthdate(baseBirthDate.getTime());
		patient.setGender("F"); // female
		bpPercentile = 90; // high is 90th percentile
		bpType = "systolic"; // systolic

		// 0 height percentile
		heightPercentile = 0.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// 3 height percentile
		heightPercentile = 3.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// 5 height percentile
		heightPercentile = 5.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// 7 height percentile
		heightPercentile = 7.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// 10 height percentile
		heightPercentile = 10.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// 95 height percentile
		heightPercentile = 95.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(105, highBP, 0);

		// 97 height percentile
		heightPercentile = 97.0;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(105, highBP, 0);

		// test BP percentile
		// 90
		patient.setBirthdate(baseBirthDate.getTime());
		patient.setGender("F"); // female
		bpType = "systolic"; // systolic
		heightPercentile = 5.0;

		bpPercentile = 90;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(99, highBP, 0);

		// 95
		bpPercentile = 95;
		highBP = RgrtaService.getHighBP(patient, bpPercentile, bpType,
				heightPercentile, onDate.getTime());
		Assert.assertEquals(102, highBP, 0);
	}

	@Before
	public void runBeforeEachTest() throws Exception
	{
		// create the basic user and give it full rights
		initializeInMemoryDatabase();
		executeDataSet(TestUtil.DBUNIT_SETUP_FILE);
		// authenticate to the temp database
		authenticate();
	}

	/**
	 * @verifies {@link RgrtaServiceImpl#produce(OutputStream,PatientState,Patient,Integer,String,int)}
	 *           test = should testPSFProduce
	 */
	@Test
	public void produce_shouldTestPSFProduce() throws Exception
	{
		LocationService locationService = Context.getLocationService();
		File dir1 = new File("ruleLibrary");

		AdministrationService adminService = Context.getAdministrationService();
	    adminService.setGlobalProperty("dss.javaRuleDirectory", dir1.getCanonicalPath());
	    adminService.setGlobalProperty("dss.classRuleDirectory", dir1.getCanonicalPath());
	    adminService.setGlobalProperty("dss.mlmRuleDirectory", dir1.getCanonicalPath());

		int patientId = 30520;
		int providerId = 30515;
		Integer formInstanceId = 1;
		Integer sessionId = 1;

		EncounterService encounterService = Context
				.getService(EncounterService.class);
		PatientService patientService = Context.getPatientService();
		UserService userService = Context.getUserService();

		org.openmrs.module.rgrta.hibernateBeans.Encounter encounter = new org.openmrs.module.rgrta.hibernateBeans.Encounter();
		encounter.setEncounterDatetime(new java.util.Date());
		Patient patient = patientService.getPatient(patientId);
		User provider = userService.getUser(providerId);

		encounter.setLocation(locationService.getLocation("Unknown Location"));
		encounter.setProvider(provider);
		encounter.setPatient(patient);
		Calendar scheduledTime = Calendar.getInstance();
		scheduledTime.set(2007, Calendar.NOVEMBER, 20, 8, 12);
		encounter.setScheduledTime(scheduledTime.getTime());
		encounterService.saveEncounter(encounter);
		Integer encounterId = encounter.getEncounterId();
		String generatedOutput = null;
		String booleanString = adminService
				.getGlobalProperty("atd.mergeTestCaseXML");
		boolean merge = Boolean.parseBoolean(booleanString);
		ATDService atdService = Context.getService(ATDService.class);

		String PSFMergeDirectory = null;
		FormService formService = Context.getFormService();

		Integer psfFormId = formService.getForms("PSF", null, null, false,
				null, null, null).get(0).getFormId();
		Integer locationTagId = 1;
		Integer locationId = 1;
		
		try
		{
				FormAttributeValue formAttributeValue = atdService
						.getFormAttributeValue(psfFormId,
								"defaultMergeDirectory", locationTagId,locationId);

				if (formAttributeValue != null)
				{
					PSFMergeDirectory = formAttributeValue.getValue();
				}

				String PSFFilename = "test/testFiles/PSF.xml";
				String removeCurrentTimeXSLT = "test/testFiles/removeCurrentTime.xslt";

				ChirdlUtilService chirdlutilService = Context.getService(ChirdlUtilService.class);
				RgrtaService RgrtaService = Context.getService(RgrtaService.class);
				PatientState patientState = new PatientState();
				patientState.setPatient(patient);

				// test create PSF merge file
				String state = "PSF_create";
				LocationTagAttributeValue locTagAttrValue = 
					chirdlutilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				
				Integer formId = null;
				
				if(locTagAttrValue != null){
					String value = locTagAttrValue.getValue();
					if(value != null){
						try
						{
							formId = Integer.parseInt(value);
						} catch (Exception e)
						{
						}
					}
				}
				FormInstance formInstance = new FormInstance();
				formInstance.setFormInstanceId(formInstanceId);

				formInstance.setFormId(formId);
				formInstance.setLocationId(locationId);
				patientState.setSessionId(sessionId);
				OutputStream generatedXML = new ByteArrayOutputStream();
				formAttributeValue = atdService.getFormAttributeValue(
						psfFormId, "numQuestions", locationTagId,locationId);
				int maxDssElements = 0;

				if (formAttributeValue != null)
				{
					maxDssElements = Integer.parseInt(formAttributeValue.getValue());
				}

				RgrtaService.produce(generatedXML, patientState, patient,
						encounterId, "PSF", maxDssElements,sessionId);
				OutputStream targetXML = new ByteArrayOutputStream();
				IOUtil.bufferedReadWrite(new FileInputStream(PSFFilename),
						targetXML);
				generatedOutput = generatedXML.toString();
				if (merge && PSFMergeDirectory != null)
				{
					FileWriter writer = new FileWriter(PSFMergeDirectory
							+ "file1.xml");
					writer.write(generatedOutput);
					writer.close();
				}
				generatedXML = new ByteArrayOutputStream();
				XMLUtil.transformXML(new ByteArrayInputStream(generatedOutput
						.getBytes()), generatedXML, new FileInputStream(
						removeCurrentTimeXSLT), null);
				assertEquals(targetXML.toString(), generatedXML.toString());

				// test forms with younger child
				Calendar calendar = Calendar.getInstance();
				calendar.set(2007, Calendar.JANUARY, 1);
				patient.setBirthdate(calendar.getTime());
				PSFFilename = "test/testFiles/PSF_younger.xml";

				// test create PSF merge file
				state = "PSF_create";
				locTagAttrValue = 
					chirdlutilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				
				if(locTagAttrValue != null){
					String value = locTagAttrValue.getValue();
					if(value != null){
						try
						{
							formId = Integer.parseInt(value);
						} catch (Exception e)
						{
						}
					}
				}
				formInstance = new FormInstance();
				formInstance.setFormInstanceId(formInstanceId);

				formInstance.setFormId(formId);
				formInstance.setLocationId(locationId);
				generatedXML = new ByteArrayOutputStream();
				RgrtaService.produce(generatedXML, patientState, patient,
						encounterId, "PSF", maxDssElements,sessionId);
				targetXML = new ByteArrayOutputStream();
				IOUtil.bufferedReadWrite(new FileInputStream(PSFFilename),
						targetXML);
				generatedOutput = generatedXML.toString();
				if (merge && PSFMergeDirectory != null)
				{
					FileWriter writer = new FileWriter(PSFMergeDirectory
							+ "file2.xml");
					writer.write(generatedOutput);
			writer.flush();
					writer.close();
				}
				generatedXML = new ByteArrayOutputStream();
				XMLUtil.transformXML(new ByteArrayInputStream(generatedOutput
						.getBytes()), generatedXML, new FileInputStream(
						removeCurrentTimeXSLT), null);
				assertEquals(targetXML.toString(), generatedXML.toString());
			
		} catch (Exception e)
		{

		}
	}

	/**
	 * @verifies {@link RgrtaServiceImpl#produce(OutputStream,PatientState,Patient,Integer,String,int)}
	 *           test = should testPWSProduce
	 */
	@Test
	public void produce_shouldTestPWSProduce() throws Exception
	{
		LocationService locationService = Context.getLocationService();
		int patientId = 30520;
		int providerId = 30515;
		Integer formInstanceId = 1;

		EncounterService encounterService = Context
				.getService(EncounterService.class);
		PatientService patientService = Context.getPatientService();
		UserService userService = Context.getUserService();

		org.openmrs.module.rgrta.hibernateBeans.Encounter encounter = new org.openmrs.module.rgrta.hibernateBeans.Encounter();
		encounter.setEncounterDatetime(new java.util.Date());
		Patient patient = patientService.getPatient(patientId);
		User provider = userService.getUser(providerId);

		encounter.setLocation(locationService.getLocation("Unknown Location"));
		encounter.setProvider(provider);
		encounter.setPatient(patient);
		Calendar scheduledTime = Calendar.getInstance();
		scheduledTime.set(2007, Calendar.NOVEMBER, 20, 8, 12);
		encounter.setScheduledTime(scheduledTime.getTime());
		encounterService.saveEncounter(encounter);
		Integer encounterId = encounter.getEncounterId();
		String generatedOutput = null;
		AdministrationService adminService = Context.getAdministrationService();
		String booleanString = adminService
				.getGlobalProperty("atd.mergeTestCaseXML");
		boolean merge = Boolean.parseBoolean(booleanString);
		ATDService atdService = Context.getService(ATDService.class);

		String PWSMergeDirectory = null;
		FormService formService = Context.getFormService();

		Integer pwsFormId = formService.getForms("PWS", null, null, false,
				null, null, null).get(0).getFormId();
		Integer locationTagId = 1;
		Integer locationId = 1;
		
		try
		{
				FormAttributeValue formAttributeValue = atdService
						.getFormAttributeValue(pwsFormId,
								"defaultMergeDirectory", locationTagId,locationId);

				if (formAttributeValue != null)
				{
					PWSMergeDirectory = formAttributeValue.getValue();
				}

				String PWSFilename = "test/testFiles/PWS.xml";
				String removeCurrentTimeXSLT = "test/testFiles/removeCurrentTime.xslt";

				RgrtaService RgrtaService = Context
						.getService(RgrtaService.class);
				
				ChirdlUtilService chirdlutilService = Context.getService(ChirdlUtilService.class);
				PatientState patientState = new PatientState();
				patientState.setPatient(patient);

				// test create PWS merge file
				String state = "PWS_create";
				LocationTagAttributeValue locTagAttrValue = 
					chirdlutilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				Integer formId = null;
				if(locTagAttrValue != null){
					String value = locTagAttrValue.getValue();
					if(value != null){
						try
						{
							formId = Integer.parseInt(value);
						} catch (Exception e)
						{
						}
					}
				}
				FormInstance formInstance = new FormInstance();
				formInstance.setFormId(formId);
				formInstance.setFormInstanceId(formInstanceId);
				formInstance.setLocationId(locationId);
				patientState.setFormInstance(formInstance);

				OutputStream generatedXML = new ByteArrayOutputStream();
				formAttributeValue = atdService.getFormAttributeValue(
						pwsFormId, "numQuestions", locationTagId,locationId);
				int maxDssElements = 0;
				int sessionId = 1;

				if (formAttributeValue != null)
				{
					maxDssElements = Integer.parseInt(formAttributeValue
							.getValue());
				}

				RgrtaService.produce(generatedXML, patientState, patient,
						encounterId, "PWS", maxDssElements,sessionId);
				OutputStream targetXML = new ByteArrayOutputStream();
				IOUtil.bufferedReadWrite(new FileInputStream(PWSFilename),
						targetXML);
				generatedOutput = generatedXML.toString();
				if (merge && PWSMergeDirectory != null)
				{
					FileWriter writer = new FileWriter(PWSMergeDirectory
							+ "file1.xml");
					writer.write(generatedOutput);
			writer.flush();
					writer.close();
				}
				generatedXML = new ByteArrayOutputStream();
				XMLUtil.transformXML(new ByteArrayInputStream(generatedOutput
						.getBytes()), generatedXML, new FileInputStream(
						removeCurrentTimeXSLT), null);
				assertEquals(targetXML.toString(), generatedXML.toString());

				// test forms with younger child
				Calendar calendar = Calendar.getInstance();
				calendar.set(2007, Calendar.JANUARY, 1);
				patient.setBirthdate(calendar.getTime());
				PWSFilename = "test/testFiles/PWS_younger.xml";

				// test create PWS merge file
				state = "PWS_create";
				locTagAttrValue = 
					chirdlutilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				
				if(locTagAttrValue != null){
					String value = locTagAttrValue.getValue();
					if(value != null){
						try
						{
							formId = Integer.parseInt(value);
						} catch (Exception e)
						{
						}
					}
				}
				formInstance = new FormInstance();
				formInstance.setFormInstanceId(formInstanceId);

				formInstance.setFormId(formId);
				formInstance.setLocationId(locationId);
				generatedXML = new ByteArrayOutputStream();
				RgrtaService.produce(generatedXML, patientState, patient,
						encounterId, "PWS", maxDssElements,sessionId);
				targetXML = new ByteArrayOutputStream();
				IOUtil.bufferedReadWrite(new FileInputStream(PWSFilename),
						targetXML);
				generatedOutput = generatedXML.toString();
				if (merge && PWSMergeDirectory != null)
				{
					FileWriter writer = new FileWriter(PWSMergeDirectory
							+ "file2.xml");
					writer.write(generatedOutput);
					writer.close();
				}
				generatedXML = new ByteArrayOutputStream();
				XMLUtil.transformXML(new ByteArrayInputStream(generatedOutput
						.getBytes()), generatedXML, new FileInputStream(
						removeCurrentTimeXSLT), null);
				assertEquals(targetXML.toString(), generatedXML.toString());
		
		} catch (Exception e)
		{

		}
	}

	/**
	 * @verifies {@link RgrtaServiceImpl#consume(InputStream,Patient,Integer,Integer,Integer,Integer)}
	 *           test = should testPSFConsume
	 */
	@Test
	public void consume_shouldTestPSFConsume() throws Exception
	{
		// TODO auto-generated
		Assert.fail("Not yet implemented");
	}

	/**
	 * @verifies {@link RgrtaServiceImpl#consume(InputStream,Patient,Integer,Integer,Integer,Integer)}
	 *           test = should testPWSConsume
	 */
	@Test
	public void consume_shouldTestPWSConsume() throws Exception
	{
		// TODO auto-generated
		Assert.fail("Not yet implemented");
	}
}