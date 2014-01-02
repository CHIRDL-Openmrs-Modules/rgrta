package org.openmrs.module.rgrta.impl;

import org.openmrs.module.rgrta.service.EncounterService;

/**
 * Encounter-related services
 * 
 * @author Tammy Dugan
 * @version 1.0
 */
public class EncounterServiceImpl extends org.openmrs.api.impl.EncounterServiceImpl implements EncounterService
{
	org.openmrs.module.rgrta.db.EncounterDAO dao = null;
	
	/**
	 * 
	 */
	public EncounterServiceImpl()
	{
	}

	/**
	 * @param dao
	 */
	public void setRgrtaEncounterDAO(org.openmrs.module.rgrta.db.EncounterDAO dao)
	{
		this.dao = dao;
		super.setEncounterDAO(this.dao);
	}
	
}
