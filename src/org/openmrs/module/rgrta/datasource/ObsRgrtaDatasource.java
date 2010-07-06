/**
 * 
 */
package org.openmrs.module.rgrta.datasource;

import java.util.Set;

import org.openmrs.Obs;
import org.openmrs.logic.datasource.ObsDataSource;

/**
 * @author Tammy Dugan
 * 
 */
public class ObsRgrtaDatasource extends ObsDataSource
{
	public void parseHL7ToObs(String hl7Message,Integer patientId,String mrn)
	{
		((LogicRgrtaObsDAO) this.getLogicObsDAO()).parseHL7ToObs(hl7Message,
				patientId,mrn);
	}

	public void deleteRegenObsByPatientId(Integer patientId)
	{
		((LogicRgrtaObsDAO) this.getLogicObsDAO())
				.deleteRegenObsByPatientId(patientId);
	}

	public Set<Obs> getRegenObsByConceptName(Integer patientId,
			String conceptName)
	{
		return ((LogicRgrtaObsDAO) this.getLogicObsDAO())
				.getRegenObsByConceptName(patientId, conceptName);
	}
	
	public void clearRegenObs() {
	    ((LogicRgrtaObsDAO) this.getLogicObsDAO()).clearRegenObs();
	}

}
