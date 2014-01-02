/**
 * 
 */
package org.openmrs.module.rgrta.randomizer;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.rgrta.hibernateBeans.Study;

/**
 * @author tmdugan
 *
 */
public interface Randomizer
{
	public void randomize(Study study,Patient patient,Encounter encounter);
}
