package org.openmrs.module.rgrta.extension.html;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.Extension;
import org.openmrs.module.web.extension.AdministrationSectionExt;

/**
 * @author Tammy Dugan
 *
 */
public class AdminList extends AdministrationSectionExt {

	@Override
	public Extension.MEDIA_TYPE getMediaType() {
		return Extension.MEDIA_TYPE.html;
	}
	
	@Override
	public String getTitle() {
		return "Rgrta.title";
	}
	
	@Override
	public Map<String, String> getLinks() {
		
		Map<String, String> map = new HashMap<String, String>();
		
		map.put("module/Rgrta/testCheckin.form", "Test checkin through AOP");
		map.put("module/Rgrta/parseDictionary.form", "Parse dictionary file");
		map.put("module/Rgrta/loadObs.form", "Load old obs");
		map.put("module/Rgrta/fillOutPSF.form?formName=PSF", "Scan PSF");
		map.put("module/Rgrta/fillOutPWS.form?formName=PWS", "Scan PWS");
		map.put("module/Rgrta/greaseBoard.form", "Grease Board");
		map.put("module/Rgrta/viewPatient.form", "View Encounters");
		map.put("module/Rgrta/ruleTester.form", "Rule Tester");
		map.put("module/Rgrta/weeklyReports.form", "Weekly Reports");
		return map;
	}
	
}
