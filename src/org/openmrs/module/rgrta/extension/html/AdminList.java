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
		map.put("module/rgrta/weeklyReports.form", "Weekly Reports");
		return map;
	}
	
}
