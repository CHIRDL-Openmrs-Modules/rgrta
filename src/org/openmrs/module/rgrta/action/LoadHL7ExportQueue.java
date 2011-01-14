/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.Session;
import org.openmrs.module.atd.hibernateBeans.State;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7Export;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportMap;
import org.openmrs.module.rgrta.service.RgrtaService;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationTagAttributeValue;
import org.openmrs.module.chirdlutil.impl.ChirdlUtilServiceImpl;
import org.openmrs.module.chirdlutil.service.ChirdlUtilService;


/**
 * @author tmdugan
 *
 */
public class LoadHL7ExportQueue implements ProcessStateAction
{

	/* (non-Javadoc)
	 * @see org.openmrs.module.rgrta.action.ProcessStateAction#processAction(org.openmrs.module.atd.hibernateBeans.StateAction, org.openmrs.Patient, org.openmrs.module.atd.hibernateBeans.PatientState, java.util.HashMap)
	 */
	public void processAction(StateAction stateAction, Patient patient,
			PatientState patientState, HashMap<String, Object> parameters)
	{
		//lookup the patient again to avoid lazy initialization errors
		PatientService patientService = Context.getPatientService();
		RgrtaService RgrtaService = Context
		.getService(RgrtaService.class);
		ATDService atdService = Context
		.getService(ATDService.class);
		
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);
		Integer locationTagId = patientState.getLocationTagId();
		Integer locationId = patientState.getLocationId();
		

		//Get existing form_id.
		//If not a form, form id will be null in export map table.
		//Default config location will have the format for non-form;
		
		//Integer formId = (Integer) parameters.get("formId");
		FormInstance formInstance = (FormInstance) parameters.get("formInstance");
		Integer formId = formInstance.getFormId();
		String formIdString = String.valueOf(formId);
		State currState = patientState.getState();
		Integer sessionId = patientState.getSessionId();
		Session session = atdService.getSession(sessionId);
		Integer encounterId = session.getEncounterId();

		try {
			
			RgrtaHL7Export export = new RgrtaHL7Export();
			export.setDateInserted(new Date());
			export.setEncounterId(encounterId);
			export.setSessionId(sessionId);
			export.setVoided(false);
			export.setStatus(1);
			RgrtaHL7ExportMap exportMap = new RgrtaHL7ExportMap();
			RgrtaHL7Export insertedExport = RgrtaService.insertEncounterToHL7ExportQueue(export);
			exportMap.setValue(formIdString);
			exportMap.setHl7ExportQueueId(insertedExport.getQueueId());
			exportMap.setDateInserted(new Date());
			exportMap.setVoided(false);
			RgrtaService.saveHL7ExportMap(exportMap);
			
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

}
