/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.ATDError;
import org.openmrs.module.atd.hibernateBeans.FormAttributeValue;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.Session;
import org.openmrs.module.atd.hibernateBeans.State;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.rgrta.hibernateBeans.Encounter;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7Export;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportMap;
import org.openmrs.module.rgrta.service.EncounterService;
import org.openmrs.module.rgrta.service.RgrtaService;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationTagAttributeValue;
import org.openmrs.module.chirdlutil.impl.ChirdlUtilServiceImpl;
import org.openmrs.module.chirdlutil.service.ChirdlUtilService;
import org.openmrs.module.chirdlutil.util.IOUtil;


/**
 * @author tmdugan
 *
 */
public class ExportObs implements ProcessStateAction
{

	/* (non-Javadoc)
	 * @see org.openmrs.module.rgrta.action.ProcessStateAction#processAction(org.openmrs.module.atd.hibernateBeans.StateAction, org.openmrs.Patient, org.openmrs.module.atd.hibernateBeans.PatientState, java.util.HashMap)
	 */
	
	private static Log log = LogFactory.getLog(RgrtaStateActionHandler.class);
	
	public void processAction(StateAction stateAction, Patient patient,
			PatientState patientState, HashMap<String, Object> parameters)
	{
	
		
		ATDService atdService = Context.getService(ATDService.class);
		RgrtaService RgrtaService = Context.getService(RgrtaService.class);
		EncounterService encounterService = Context.getService(EncounterService.class);
		AdministrationService adminService = Context.getAdministrationService();
		LocationService locService = Context.getLocationService();
		ConceptService conceptService = Context.getConceptService();
		
		
		//lookup the patient again to avoid lazy initialization errors
		PatientService patientService = Context.getPatientService();
		
		
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);
		Integer locationTagId = patientState.getLocationTagId();
		Integer locationId = patientState.getLocationId();
		Integer orderRep = 0;
		

		FormInstance formInstance = (FormInstance) parameters.get("formInstance");
		PatientState patientStateWithForm = atdService.getPrevPatientStateByAction(patientState.getSessionId(), patientState.getPatientStateId(), "PRODUCE FORM INSTANCE");

		if (formInstance == null){
			formInstance = patientStateWithForm.getFormInstance();
		}
		State currState = patientState.getState();
		Integer sessionId = patientState.getSessionId();
		Session session = atdService.getSession(sessionId);
		Integer encounterId = session.getEncounterId();

		try {
			
			//get the location to put the file
			//get the config file
			String configFileName = getFileLocation( formInstance.getFormId(), "HL7ConfigFile");
			if (configFileName == null || configFileName.equalsIgnoreCase("") ) {
				//ATDError ce = new ATDError("Error", "Hl7 Export", 
				//		"Hl7 export config file not found: " + configFileName, 
				//		null ,
				//		 new Date(), sessionId);
				//atdService.saveError(ce);
				return;
			}
			
			Properties hl7Properties = org.openmrs.module.rgrta.util.Util.getProps(configFileName);
			Encounter openmrsEncounter = (Encounter) encounterService.getEncounter(encounterId);
			org.openmrs.module.rgrta.hl7.mckesson.HL7MessageConstructor constructor = 
				new org.openmrs.module.rgrta.hl7.mckesson.HL7MessageConstructor(configFileName, null );
			
			constructor.AddSegmentMSH(openmrsEncounter);
			constructor.AddSegmentPID(patient);
			constructor.AddSegmentPV1(openmrsEncounter);
			String univServiceId = hl7Properties.getProperty("univ_serv_id", "001");
			String univServiceIdName = hl7Properties.getProperty("univ_serv_id_name", "");
			constructor.AddSegmentOBR(openmrsEncounter, univServiceId, univServiceIdName, orderRep);
			String message = constructor.getMessage();
			saveMessageFile(message, formInstance);
			
			
		} finally{

			StateManager.endState(patientState);
			RgrtaStateActionHandler.changeState(patient, sessionId, currState,stateAction,
					parameters,locationTagId,locationId);
		}
	}

	public void changeState(PatientState patientState,
			HashMap<String, Object> parameters) {
		//deliberately empty because processAction changes the state
	}
	
private String getFileLocation( Integer formId, String fileType){
		
		
		String filename = null;
		Integer locId = null;
		Integer locTagId = null;
		
		LocationService locService = Context.getLocationService();
		
		
		ATDService atdService = Context.getService(ATDService.class);
		Location loc= locService.getLocation("Default Location");
		LocationTag  locTag = locService.getLocationTagByName("Default Location Tag");
		locId = loc.getLocationId();
		locTagId = locTag.getLocationTagId();
		
		FormAttributeValue formAttrValue = atdService.getFormAttributeValue(formId, fileType, locTagId, locId);
		if (formAttrValue != null){
			filename = formAttrValue.getValue();
		}
		return filename;
	}


public void saveMessageFile( String message, FormInstance formInstance){
	
	AdministrationService adminService = Context.getAdministrationService();
	EncounterService es = Context.getService(EncounterService.class);
	String filename =  org.openmrs.module.chirdlutil.util.Util.archiveStamp() + ".hl7";
	String exportDir = getFileLocation( formInstance.getFormId(), "HL7ExportObsDir");
	FileOutputStream exportFile = null;
	
	try
	{
		 exportFile = new FileOutputStream(
				exportDir + "/" + filename);
	} catch (FileNotFoundException e1)
	{
		log.error("Couldn't find file: "+ exportFile + "/" + filename);
	}
	if (exportFile != null && exportFile != null)
	{
		try
		{

			ByteArrayInputStream messageStream = new ByteArrayInputStream(
					message.getBytes());
			IOUtil.bufferedReadWrite(messageStream, exportFile);
			exportFile.flush();
			exportFile.close();
		} catch (Exception e)
		{
			try
			{
				exportFile.flush();
				exportFile.close();
			} catch (Exception e1)
			{
			}
			log.error("There was an error writing the hl7 export obs file");
			log.error(e.getMessage());
			log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
		}
	}
	return;
	
}
}
