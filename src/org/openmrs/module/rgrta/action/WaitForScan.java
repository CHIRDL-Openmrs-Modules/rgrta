/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.TeleformFileMonitor;
import org.openmrs.module.atd.TeleformFileState;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.rgrta.hibernateBeans.Statistics;
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
		LocationService locationService = Context.getLocationService();
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);

		FormInstance formInstance = (FormInstance) parameters.get("formInstance");
		if(formInstance == null){
		RgrtaService rgrtaService = Context.getService(RgrtaService.class);

		Integer sessionId = patientState.getSessionId();
		PatientState stateWithFormId = rgrtaService.getPrevProducePatientState(
				sessionId, patientState.getPatientStateId());

		formInstance = patientState.getFormInstance();

		if (formInstance == null && stateWithFormId != null) {
			formInstance = stateWithFormId.getFormInstance();
		}
		}
		patientState.setFormInstance(formInstance);
		ATDService atdService = Context.getService(ATDService.class);
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
			
			/*List<Statistics> statistics = rgrtaService.getStatByFormInstance(
				formInstance.getFormInstanceId(), formName, patientState.getLocationId());
			 
			for (Statistics currStat : statistics) {
				currStat.setScannedTimestamp(patientState.getEndTime());
				rgrtaService.updateStatistics(currStat);
			}
			*/

			RgrtaStateActionHandler.changeState(patientState.getPatient(), patientState.getSessionId(),
					patientState.getState(), patientState.getState()
							.getAction(), parameters, patientState
							.getLocationTagId(), patientState.getLocationId());
		} catch (Exception e) {
			log.error("",e);
			log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
		}
	}

}
