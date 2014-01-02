/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.util.Date;
import java.util.HashMap;

import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.ATDError;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.State;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;

/**
 * @author tmdugan
 *
 */
public class ConsumeFormInstance implements ProcessStateAction
{

	/* (non-Javadoc)
	 * @see org.openmrs.module.rgrta.action.ProcessStateAction#processAction(org.openmrs.module.atd.hibernateBeans.StateAction, org.openmrs.Patient, org.openmrs.module.atd.hibernateBeans.PatientState, java.util.HashMap)
	 */
	public void processAction(StateAction stateAction, Patient patient,
			PatientState patientState, HashMap<String, Object> parameters)
	{
		ATDService atdService = Context.getService(ATDService.class);
		String formInst = null;
		try {
			long totalTime = System.currentTimeMillis();
			long startTime = System.currentTimeMillis();
			//lookup the patient again to avoid lazy initialization errors
			PatientService patientService = Context.getPatientService();
			Integer patientId = patient.getPatientId();
			patient = patientService.getPatient(patientId);
			
			
			Integer locationTagId = patientState.getLocationTagId();
			Integer locationId = patientState.getLocationId();

			State currState = patientState.getState();
			Integer sessionId = patientState.getSessionId();
			FormInstance formInstance = (FormInstance) parameters.get("formInstance");
			PatientState patientStateWithForm = atdService.getPrevPatientStateByAction(patientState.getSessionId(), patientState.getPatientStateId(), "PRODUCE FORM INSTANCE");

			if (formInstance == null){
				formInstance = patientStateWithForm.getFormInstance();
			}
			
			FormService formService = Context.getFormService();
			Form form = formService.getForm(formInstance.getFormId());
			patientState.setFormInstance(formInstance);
			atdService.updatePatientState(patientState);
			startTime = System.currentTimeMillis();
			RgrtaStateActionHandler.consume(sessionId,formInstance,patient,
					parameters,null,locationTagId);
			startTime = System.currentTimeMillis();
			StateManager.endState(patientState);
			System.out.println("Consume: Total time to consume "+form.getName()+": "+(System.currentTimeMillis()-totalTime));
			RgrtaStateActionHandler.changeState(patient, sessionId, currState,
					stateAction,parameters,locationTagId,locationId);
		} catch (APIException e) {

			String message = e.getMessage();
			ATDError ce = new ATDError("Error", "General Error", 
					"Error consuming form instance " ,Util.getStackTrace(e) , 
					 new Date(), patientState.getSessionId());
			atdService.saveError(ce);
			System.out.println("Error consuming form instance ");
			
		}

	}

	public void changeState(PatientState patientState,
			HashMap<String, Object> parameters) {
		//deliberately empty because processAction changes the state
	}

}
