/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.TeleformFileMonitor;
import org.openmrs.module.atd.TeleformFileState;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.Session;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.atd.hibernateBeans.Statistics;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.rgrta.service.RgrtaService;

/**
 * @author tmdugan
 * 
 */
public class WaitForScan implements ProcessStateAction {

	private static Log log = LogFactory.getLog(RgrtaStateActionHandler.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.module.rgrta.action.ProcessStateAction#processAction(org.openmrs.module.atd.hibernateBeans.StateAction,
	 *      org.openmrs.Patient,
	 *      org.openmrs.module.atd.hibernateBeans.PatientState,
	 *      java.util.HashMap)
	 */
	public void processAction(StateAction stateAction, Patient patient,
			PatientState patientState, HashMap<String, Object> parameters){
		// lookup the patient again to avoid lazy initialization errors
		PatientService patientService = Context.getPatientService();
		ATDService atdService = Context.getService(ATDService.class);
		LocationService locationService = Context.getLocationService();
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);
		PatientState stateWithFormId  = null;
		Integer sessionId = patientState.getSessionId();

		FormInstance formInstance = (FormInstance) parameters.get("formInstance");
		if(formInstance == null){
			RgrtaService rgrtaService = Context.getService(RgrtaService.class);
			stateWithFormId = atdService.getPrevPatientStateByAction(sessionId, patientState.getPatientStateId(), "PRODUCE FORM INSTANCE");

		    formInstance = patientState.getFormInstance();
	
			if (formInstance == null && stateWithFormId != null) {
				formInstance = stateWithFormId.getFormInstance();
			}
		}
		
		//Save obs for atd sent to provider
		ConceptService conceptService = Context.getConceptService();
		FormService formService = Context.getFormService();

		Integer formId = (Integer) parameters.get("formId");
		String formName = "";
		if (formId == null){
			if (stateWithFormId != null) {
				formId = stateWithFormId.getFormId();
			}
		}
		
		if (formId != null) {
			Form form = formService.getForm(formId);
			formName = "";
			if (form != null){
				formName = formService.getForm(formId).getName();
			}
		}
		Concept concept = conceptService.getConcept("atd_sent_to_provider");
		Session session = atdService.getSession(sessionId);
		Integer encounterId = session.getEncounterId();
		Util.saveObs(patient, concept, encounterId, formName);
		
		patientState.setFormInstance(formInstance);
		atdService.updatePatientState(patientState);
		TeleformFileState teleformFileState = TeleformFileMonitor
				.addToPendingStatesWithoutFilename(formInstance);
		
		Location defaultLocation = locationService.getLocation("Default Location");
		Integer defaultLocationId = 1;
		if (defaultLocation != null){
			defaultLocationId = defaultLocation.getLocationId();
		} 
		patientState.setLocationId(defaultLocationId);
		teleformFileState.addParameter("patientState", patientState);
	}

	public void changeState(PatientState patientState,
			HashMap<String, Object> parameters) {
		RgrtaService rgrtaService = Context.getService(RgrtaService.class);

		StateManager.endState(patientState);

		try {

			FormInstance formInstance = patientState.getFormInstance();
			
			if(formInstance == null){
			Integer sessionId = patientState.getSessionId();
			PatientState stateWithFormId = rgrtaService
					.getPrevProducePatientState(sessionId, patientState
							.getPatientStateId());

			if (stateWithFormId != null) {
				formInstance = stateWithFormId.getFormInstance();
			}
			}
			Integer formId = formInstance.getFormId();
			FormService formService = Context.getFormService();
			Form form = formService.getForm(formId);
			String formName = form.getName();
			ATDService atdService = Context.getService(ATDService.class);
			List<Statistics> statistics = atdService.getStatByFormInstance(formInstance.getFormInstanceId(), formName, patientState.getLocationId());
			 
			for (Statistics currStat : statistics) {
				currStat.setScannedTimestamp(patientState.getEndTime());
				atdService.updateStatistics(currStat);
			}
			

			RgrtaStateActionHandler.changeState(patientState.getPatient(), patientState.getSessionId(),
					patientState.getState(), patientState.getState()
							.getAction(), parameters, patientState
							.getLocationTagId(), patientState.getLocationId());
		} catch (Exception e) {
			log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
		}
	}

}
