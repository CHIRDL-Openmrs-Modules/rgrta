/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.action.ProcessStateAction;
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
		ChirdlUtilService chirdlUtilService = Context.getService(ChirdlUtilService.class);
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);
		
		Integer locationTagId = patientState.getLocationTagId();
		Integer locationId = patientState.getLocationId();
		
		
		
		RgrtaService RgrtaService = Context
				.getService(RgrtaService.class);
		ATDService atdService = Context
				.getService(ATDService.class);
		State currState = patientState.getState();
		Integer sessionId = patientState.getSessionId();
		
		Session session = atdService.getSession(sessionId);
		Integer encounterId = session.getEncounterId();
		
		try {
			if (patientState.getState().getName().equals("Export POC")) {
				//TODO:add encounter to queue
				RgrtaHL7Export pocExport = new RgrtaHL7Export();
				pocExport.setDateInserted(new Date());
				pocExport.setEncounterId(encounterId);
				pocExport.setSessionId(sessionId);
				pocExport.setVoided(false);
				pocExport.setStatus(1);
				RgrtaHL7ExportMap POCmap = new RgrtaHL7ExportMap();
				LocationTagAttributeValue  POCTagValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, "POCConceptMapLocation", 
						locationId);
				if (POCTagValue != null && !POCTagValue.equals("")) {
					POCTagValue.getValue();
					RgrtaHL7Export insertedExport = RgrtaService.insertEncounterToHL7ExportQueue(pocExport);
					POCmap.setValue(String.valueOf(POCTagValue.getLocationTagAttributeValueId()));
					POCmap.setHl7ExportQueueId(insertedExport.getQueueId());
					POCmap.setDateInserted(new Date());
					POCmap.setVoided(false);
					RgrtaService.saveHL7ExportMap(POCmap);
				}
				
				

				RgrtaHL7ExportMap psftiffMap = new RgrtaHL7ExportMap();
				LocationTagAttributeValue  psftiffTagValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, "PSFTiffConceptMapLocation", 
						locationId);
				psftiffTagValue.getValue();
				
				if (psftiffTagValue != null && !psftiffTagValue.equals("")){
					RgrtaHL7Export exportpsf = new RgrtaHL7Export();
					exportpsf.setDateInserted(new Date());
					exportpsf.setEncounterId(encounterId);
					exportpsf.setSessionId(sessionId);
					exportpsf.setVoided(false);
					exportpsf.setStatus(1);
					RgrtaHL7Export insertedExport = RgrtaService.insertEncounterToHL7ExportQueue(exportpsf);
					psftiffMap.setValue(String.valueOf(psftiffTagValue.getLocationTagAttributeValueId()));
					psftiffMap.setHl7ExportQueueId(insertedExport.getQueueId());
					psftiffMap.setDateInserted(new Date());
					psftiffMap.setVoided(false);
					RgrtaService.saveHL7ExportMap(psftiffMap);
					
				}
				
				RgrtaHL7ExportMap pwstiffMap = new RgrtaHL7ExportMap();
				LocationTagAttributeValue  pwstiffTagValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, "PWSTiffConceptMapLocation", 
						locationId);
				pwstiffTagValue.getValue();
				
				if (pwstiffTagValue != null && !pwstiffTagValue.equals("")){
					RgrtaHL7Export exportpws = new RgrtaHL7Export();
					exportpws.setDateInserted(new Date());
					exportpws.setEncounterId(encounterId);
					exportpws.setSessionId(sessionId);
					exportpws.setVoided(false);
					exportpws.setStatus(1);
					RgrtaHL7Export insertedExport = RgrtaService.insertEncounterToHL7ExportQueue(exportpws);
					pwstiffMap.setValue(String.valueOf(pwstiffTagValue.getLocationTagAttributeValueId()));
					pwstiffMap.setHl7ExportQueueId(insertedExport.getQueueId());
					pwstiffMap.setDateInserted(new Date());
					pwstiffMap.setVoided(false);
					RgrtaService.saveHL7ExportMap(pwstiffMap);
					
				}
				
			
				
				
			}
			if (patientState.getState().getName().equals("Export Vitals")){
				
				
				RgrtaHL7ExportMap vitalsMap = new RgrtaHL7ExportMap();
				LocationTagAttributeValue  tagValue = 
					chirdlUtilService.getLocationTagAttributeValue(locationTagId, "VitalsConceptMapLocation", 
						locationId);
				if (tagValue != null && !tagValue.equals("")){
					//TODO:add encounter to queue
					RgrtaHL7Export vitalsExport = new RgrtaHL7Export();
					vitalsExport.setDateInserted(new Date());
					vitalsExport.setEncounterId(encounterId);
					vitalsExport.setSessionId(sessionId);
					vitalsExport.setVoided(false);
					vitalsExport.setStatus(1);
					RgrtaHL7Export insertedExport = RgrtaService.insertEncounterToHL7ExportQueue(vitalsExport);
					tagValue.getValue();
					vitalsMap.setValue(String.valueOf(tagValue.getLocationTagAttributeValueId()));
					vitalsMap.setHl7ExportQueueId(insertedExport.getQueueId());
					vitalsMap.setDateInserted(new Date());
					vitalsMap.setVoided(false);
					RgrtaService.saveHL7ExportMap(vitalsMap);
					
				}
				
			}
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
