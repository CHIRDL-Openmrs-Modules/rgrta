package org.openmrs.module.rgrta.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rgrta.hibernateBeans.Encounter;
import org.openmrs.module.rgrta.service.EncounterService;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.api.UserService;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class TestCheckinController extends SimpleFormController
{

	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request)
			throws Exception
	{
		return "testing";
	}

	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception
	{
		LocationService locationService = Context.getLocationService();

		Map<String, Object> map = new HashMap<String, Object>();
		String patientIdString = request.getParameter("patientId");
		String providerIdString = request.getParameter("providerId");

		if (patientIdString == null)
		{
			patientIdString = "";
		}
		if (providerIdString == null)
		{
			providerIdString = "";
		}
		if (!(patientIdString.length() == 0)&&!(providerIdString.length() == 0))
		{
			int providerId = Integer.parseInt(providerIdString);
			int patientId = Integer.parseInt(patientIdString);
			SocketHL7ListenerService hl7Service = Context.getService(SocketHL7ListenerService.class);
			EncounterService encounterService = Context.getService(EncounterService.class);
			PatientService patientService = Context.getPatientService();
			UserService userService = Context.getUserService();
			
			Encounter encounter = new Encounter();
			encounter.setEncounterDatetime(new java.util.Date());
			Location location = locationService.getLocation("Unknown Location");
			Patient patient = patientService.getPatient(patientId);
			User provider = userService.getUser(providerId);
		
			encounter.setLocation(location);
			encounter.setProvider(provider);
			encounter.setPatient(patient);
			encounterService.saveEncounter(encounter);
			
			SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
			hl7ListService.messageProcessed(encounter);
		}
		
		map.put("patientId", patientIdString);
		map.put("providerId", providerIdString);

		return map;
	}

}
