/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicService;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.Session;
import org.openmrs.module.atd.hibernateBeans.State;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.hibernateBeans.Statistics;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationTagAttributeValue;
import org.openmrs.module.chirdlutil.service.ChirdlUtilService;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.rgrta.datasource.ObsRgrtaDatasource;
import org.openmrs.module.rgrta.service.RgrtaService;

/**
 * @author tmdugan
 * 
 */
public class ProduceFormInstance implements ProcessStateAction
{
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
			PatientState patientState, HashMap<String, Object> parameters)
	{
		long totalTime = System.currentTimeMillis();
		long startTime = System.currentTimeMillis();
		//lookup the patient again to avoid lazy initialization errors
		PatientService patientService = Context.getPatientService();
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);
		
		Integer locationTagId = patientState.getLocationTagId();
		Integer locationId = patientState.getLocationId();
		
		RgrtaService rgrtaService = Context
				.getService(RgrtaService.class);
		ATDService atdService = Context.getService(ATDService.class);
		ChirdlUtilService chirdlUtilService = Context.getService(ChirdlUtilService.class);

		State currState = patientState.getState();
		Integer sessionId = patientState.getSessionId();
		
		Session session = atdService.getSession(sessionId);
		Integer encounterId = session.getEncounterId();
		String formName = null;
		if(parameters != null){
			formName = (String) parameters.get("formName");
		}
		if(formName == null){
			formName = currState.getFormName();
			String formInstanceString =  (String) parameters.get("formInstance");
			
			if (formInstanceString != null){
				FormService formService = Context.getFormService();
				Form form = formService.getForm(formInstanceString);
				formName = form.getName();
			}
		}
		
		//form attributes for default locations
		LocationService locationService = Context.getLocationService();
		Location defaultlocation = locationService.getLocation("Default Location");
		LocationTag defaultLocationTag = locationService.getLocationTagByName("Default Location Tag");
		Integer defaultLocationId = null;
		Integer defaultLocationTagId = null;
		if (defaultlocation != null){
			defaultLocationId = defaultlocation.getLocationId();
		}
		if (defaultLocationTag != null){
			defaultLocationTagId = defaultLocationTag.getLocationTagId();
		}
		LocationTagAttributeValue locTagAttrValue = 
			chirdlUtilService.getLocationTagAttributeValue(defaultLocationTagId, formName, defaultLocationId);
		
		Integer formId = null;
		
		if(locTagAttrValue != null){
			String value = locTagAttrValue.getValue();
			if(value != null){
				try
				{
					formId = Integer.parseInt(value);
				} catch (Exception e)
				{
					log.error("Form id does not contain a parsible integer " +
							" when searched from the location value attribute. Form id value was " +
							value);
				}
			}
		}
		
		if(formId == null){
			//open an error state
			currState = atdService.getStateByName("ErrorState");
			atdService.addPatientState(patient,
					currState, sessionId,locationTagId,locationId);
			log.error(formName+
					" locationTagAttribute does not exist for locationTagId: "+
					locationTagId+" locationId: "+locationId);
			return;
		}
		
		FormService formService = Context.getFormService();
		Form form = formService.getForm(formId);
		startTime = System.currentTimeMillis();
		
		startTime = System.currentTimeMillis();
		
		// write the form
		FormInstance formInstance = atdService.addFormInstance(formId,
				defaultLocationId);
		
		if(parameters == null){
			parameters = new HashMap<String,Object>();
		}
		parameters.put("formInstance", formInstance);
		parameters.put("formId", formId);
		patientState.setFormInstance(formInstance);
		patientState.setFormId(formId);
		atdService.updatePatientState(patientState);
		Integer formInstanceId = formInstance.getFormInstanceId();
		String mergeDirectory = IOUtil
				.formatDirectoryName(org.openmrs.module.atd.util.Util
						.getFormAttributeValue(formId, "pendingMergeDirectory",
								defaultLocationTagId, defaultLocationId));

		String mergeFilename = mergeDirectory + formInstance.toString() + ".xml";
		int maxDssElements = org.openmrs.module.rgrta.util.Util
				.getMaxDssElements(formId, defaultLocationTagId, defaultLocationId);
		
		try {
			FileOutputStream output = new FileOutputStream(mergeFilename);
			startTime = System.currentTimeMillis();
			rgrtaService.produce(output, patientState, patient, encounterId, formName, maxDssElements, sessionId);
			startTime = System.currentTimeMillis();
			output.flush();
			output.close();
		}
		catch (IOException e) {
			log.error("Could not produce merge xml for file: "+mergeFilename, e);
		}
		LogicService logicService = Context.getLogicService();

		ObsRgrtaDatasource xmlDatasource = (ObsRgrtaDatasource) logicService
				.getLogicDataSource("RMRS");

		
		StateManager.endState(patientState);
		System.out.println("Produce: Total time to produce "+form.getName()+": "+(System.currentTimeMillis()-totalTime));
		RgrtaStateActionHandler.changeState(patient, sessionId, currState, stateAction, parameters,
				locationTagId, locationId);
		startTime = System.currentTimeMillis();
		// update statistics
		List<Statistics> statistics = atdService.getStatByFormInstance(formInstanceId, formName, locationId);

		for (Statistics currStat : statistics)
		{
			currStat.setPrintedTimestamp(patientState.getEndTime());
			atdService.updateStatistics(currStat);
		}
		startTime = System.currentTimeMillis();
		
		
	}

	public void changeState(PatientState patientState,
			HashMap<String, Object> parameters) {
		//deliberately empty because processAction changes the state
	}

}
