/**
 * 
 */
package org.openmrs.module.rgrta.advice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.hibernateBeans.Program;
import org.openmrs.module.atd.hibernateBeans.Session;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.rgrta.hibernateBeans.Encounter;
import org.openmrs.module.rgrta.service.EncounterService;
import org.openmrs.module.rgrta.service.RgrtaService;
import org.openmrs.module.rgrta.util.Util;

/**
 * @author tmdugan
 * 
 */
public class CheckinPatient implements Runnable
{
	private Log log = LogFactory.getLog(this.getClass());
	private org.openmrs.Encounter encounter = null;

	public CheckinPatient(org.openmrs.Encounter encounter)
	{
		this.encounter = encounter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		Context.openSession();
		
		try
		{
			AdministrationService adminService = Context.getAdministrationService();
			Context.authenticate(adminService
					.getGlobalProperty("scheduler.username"), adminService
					.getGlobalProperty("scheduler.password"));
			ATDService atdService = Context
					.getService(ATDService.class);
			LocationService locationService = Context.getLocationService();

			Patient patient = this.encounter.getPatient();
			Hibernate.initialize(patient); //fully initialize the patient to
				//prevent lazy initialization errors

			Integer encounterId = this.encounter.getEncounterId();
			
			//The session is unique because only 1 session exists at checkin
			List<Session> sessions = atdService.getSessionsByEncounter(encounterId);
			Session session = sessions.get(0);
			
			Integer sessionId = session.getSessionId();
			
			Integer locationTagId = null;
			Integer locationId = null;
			
			// lookup location tag id by printer location
			org.openmrs.module.rgrta.service.EncounterService encounterService = Context
					.getService(org.openmrs.module.rgrta.service.EncounterService.class);
			org.openmrs.module.rgrta.hibernateBeans.Encounter RgrtaEncounter = (Encounter) encounterService
					.getEncounter(this.encounter.getEncounterId());
			String printerLocation = RgrtaEncounter.getPrinterLocation();
			Location location = this.encounter.getLocation();
		
			Set<LocationTag> tags = location.getTags();
			for(LocationTag tag:tags){
					locationTagId = tag.getLocationTagId();
					locationId = location.getLocationId();
					break;
			}
		
			saveInsuranceInfo(encounterId, patient,locationTagId);
			EncounterType encType = RgrtaEncounter.getEncounterType();
			Program program = atdService.getProgramByNameVersion("rgrta", "1.0");
			
			StateManager.changeState(patient, sessionId, null,
					program,null,
					locationTagId,locationId,RgrtaStateActionHandler.getInstance());
		} catch (Exception e)
		{
			this.log.error(e.getMessage());
			this.log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
		}finally{
			Context.closeSession();
		}
	}

	private void saveInsuranceInfo(Integer encounterId, Patient patient,Integer locationTagId)
	{
		RgrtaService RgrtaService = Context
				.getService(RgrtaService.class);
		EncounterService encounterService = Context
				.getService(EncounterService.class);
		Encounter encounter = (Encounter) encounterService
				.getEncounter(encounterId);
		ObsService obsService = Context.getObsService();
		List<org.openmrs.Encounter> encounters = new ArrayList<org.openmrs.Encounter>();
		encounters.add(encounter);
		List<Concept> questions = new ArrayList<Concept>();
		ConceptService conceptService = Context.getConceptService();
		Concept concept = conceptService.getConcept("Insurance");
		questions.add(concept);
		List<Obs> obs = obsService.getObservations(null, encounters, questions,
				null, null, null, null, null, null, null, null, false);

		if (obs == null || obs.size() == 0)
		{

			String carrierCode = encounter.getInsuranceCarrierCode();
			String smsCode = encounter.getInsuranceSmsCode();
			String planCode = encounter.getInsurancePlanCode();
			String category = null;

			//for McKesson Pecar messages
			if (carrierCode != null && carrierCode.length() > 0)
			{
				category = RgrtaService.getInsCategoryByCarrier(carrierCode);

			}
			
			if (category == null)
			{
				// for McKesson PCC messages
				if (planCode != null && planCode.length() > 0)
				{
					category = RgrtaService.getInsCategoryByInsCode(planCode);
				}
			}
			
			if (category == null)
			{
				// for SMS PCC messages
				if (smsCode != null && smsCode.length() > 0)
				{
					category = RgrtaService.getInsCategoryBySMS(smsCode);
				}
			}
			
			if (category != null)
			{
				Util.saveObs(patient, concept, encounterId, category, null,
						null,locationTagId);
			}
		}
	}
}
