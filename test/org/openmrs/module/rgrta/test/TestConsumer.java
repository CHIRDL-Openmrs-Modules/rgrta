package org.openmrs.module.rgrta.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
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
import org.openmrs.module.rgrta.service.EncounterService;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.chirdlutil.util.XMLUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

/**
 * @author Tammy Dugan
 * 
 */
@SkipBaseSetup
public class TestConsumer extends BaseModuleContextSensitiveTest
{

	/**
	 * Set up the database with the initial dataset before every test method in
	 * this class.
	 * 
	 * Require authorization before every test method in this class
	 * 
	 */
	@Before
	public void runBeforeEachTest() throws Exception {
		// create the basic user and give it full rights
		initializeInMemoryDatabase();
		executeDataSet(TestUtil.DBUNIT_SETUP_FILE);
		// authenticate to the temp database
		authenticate();
	}

	@Test
	public void testConsume() throws Exception
	{
		LocationService locationService = Context.getLocationService();

		int patientId = 30520;
		int providerId = 9350;
		EncounterService encounterService = Context
				.getService(EncounterService.class);
		PatientService patientService = Context.getPatientService();
		UserService userService = Context.getUserService();
		ChirdlUtilService chirdlUtilService = Context.getService(ChirdlUtilService.class);


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
		ATDService atdService = Context.getService(ATDService.class);
		RgrtaService RgrtaService = Context.getService(RgrtaService.class);
		String filename = "test/testFiles/export_PSF.xml";
		String state = null;
		OutputStream generatedXML = new ByteArrayOutputStream();
		String PWSFilename = "test/testFiles/PWS_based_on_PSF_export.xml";
		String removeCurrentTimeXSLT = "test/testFiles/removeCurrentTime.xslt";
		AdministrationService adminService = Context.getAdministrationService();
		String booleanString = adminService
				.getGlobalProperty("atd.mergeTestCaseXML");
		boolean merge = Boolean.parseBoolean(booleanString);
		Integer formInstanceId = 1;
		Integer formId = null;
		FormService formService = Context.getFormService();
		Integer pwsFormId = formService.getForms("PWS", null, null, false,
				null, null, null).get(0).getFormId();
		Integer psfFormId = formService.getForms("PSF", null, null, false,
				null, null, null).get(0).getFormId();

		String PWSMergeDirectory = null;
		Integer locationTagId = 1;
		Integer locationId = 1;
		FormAttributeValue formAttributeValue = atdService
						.getFormAttributeValue(pwsFormId,
								"defaultMergeDirectory", locationTagId,locationId);

				if (formAttributeValue != null)
				{
					PWSMergeDirectory = formAttributeValue.getValue();
				}

				PatientState patientState = new PatientState();
				patientState.setPatient(patient);

				// create the PSF form
				state = "PSF_create";
				LocationTagAttributeValue locTagAttrValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				
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
				int maxPWSDssElements = 0;

				formAttributeValue = atdService.getFormAttributeValue(
						pwsFormId, "numPrompts", locationTagId,locationId);

				if (formAttributeValue != null)
				{
					maxPWSDssElements = Integer.parseInt(formAttributeValue
							.getValue());
				}

				int maxPSFDssElements = 0;
				int sessionId = 1;
				state = "PWS_create";
				locTagAttrValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				
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
				formAttributeValue = atdService.getFormAttributeValue(
						psfFormId, "numQuestions", locationTagId,locationId);

				if (formAttributeValue != null)
				{
					maxPSFDssElements = Integer.parseInt(formAttributeValue
							.getValue());
				}

				RgrtaService.produce(new ByteArrayOutputStream(), patientState,
						patient, encounterId, "PSF", maxPSFDssElements,sessionId);

				// read in the export xml and store observations
				RgrtaService.consume(new FileInputStream(filename), patient,
						encounterId, formInstance, 1, null,locationTagId);

				// create the PWS form
				state = "PWS_create";
				locTagAttrValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, atdService.getStateByName(state).getFormName(), locationId);
				
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
						encounterId, "PWS", maxPWSDssElements,sessionId);
				ByteArrayOutputStream targetXML = new ByteArrayOutputStream();
				IOUtil.bufferedReadWrite(new FileInputStream(PWSFilename),
						targetXML);
				String generatedOutput = generatedXML.toString();
				if (merge && PWSMergeDirectory != null)
				{
					FileWriter writer = new FileWriter(PWSMergeDirectory
							+ "file5.xml");
					writer.write(generatedOutput);
					writer.close();
				}
				generatedXML = new ByteArrayOutputStream();
				XMLUtil.transformXML(new ByteArrayInputStream(generatedOutput
						.getBytes()), generatedXML, new FileInputStream(
						removeCurrentTimeXSLT), null);
				assertEquals(targetXML.toString(), generatedXML.toString());
	}
}