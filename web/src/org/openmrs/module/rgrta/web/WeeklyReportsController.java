package org.openmrs.module.rgrta.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.web.WeeklyReportRow;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class WeeklyReportsController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return "testing";
	}
	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj,
	                                org.springframework.validation.BindException errors) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String locationName = request.getParameter("locationName");
		if (locationName != null && locationName.length() == 0) {
			locationName = null;
		}
		if (locationName != null) {
			map.put("locationName", locationName);
		}	
			ATDService atdService = Context.getService(ATDService.class);
			//# PSF Printed
			List<Object[]> asthmaPrintedByWeek = atdService.getFormsPrintedByWeek("Asthma_Alert", null);
		
			List<WeeklyReportRow> rows = populateRows(asthmaPrintedByWeek);
			LinkedHashMap<String, WeeklyReportRow> asthmaPrintedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaPrintedMap.put(row.getDateRange(), row);
			}
			
			map.put("asthmaPrintedMap", asthmaPrintedMap);
			
			//# PSF scanned
			List<Object[]> asthmaScannedByWeek = atdService.getFormsScannedByWeek("Asthma_Alert", null);
			rows = populateRows(asthmaScannedByWeek);
			LinkedHashMap<String, WeeklyReportRow> asthmaScannedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaScannedMap.put(row.getDateRange(), row);
			}
			map.put("asthmaScannedMap", asthmaScannedMap);
			
			//% of PSF Scanned
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : asthmaPrintedMap.values()) {
				Integer psfPrintedCount = row.getData();
				Integer asthmacannedCount = null;
				if (asthmaScannedMap.get(row.getDateRange()) != null) {
					asthmacannedCount = asthmaScannedMap.get(row.getDateRange()).getData();
				}
				if (asthmacannedCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) asthmacannedCount / psfPrintedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> asthmaPercentScannedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaPercentScannedMap.put(row.getDateRange(), row);
			}
			
			map.put("asthmaPercentScannedMap", asthmaPercentScannedMap);
			
			//# scanned asthma w >=1 Box Chked
			List<Object[]> asthmaScannedAnsweredByWeek = atdService.getFormsScannedAnsweredByWeek("Asthma_Alert", null);
			rows = populateRows(asthmaScannedAnsweredByWeek);
			LinkedHashMap<String, WeeklyReportRow> asthmaScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("asthmaScannedAnsweredMap", asthmaScannedAnsweredMap);
			
			//# scanned asthma w anything marked
			List<Object[]> asthmaScannedAnythingMarkedByWeek = atdService.getFormsScannedAnythingMarkedByWeek("Asthma_Alert",
			    null);
			rows = populateRows(asthmaScannedAnythingMarkedByWeek);
			LinkedHashMap<String, WeeklyReportRow> asthmaScannedAnythingMarkedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaScannedAnythingMarkedMap.put(row.getDateRange(), row);
			}
			map.put("asthmaScannedAnythingMarkedMap", asthmaScannedAnythingMarkedMap);
			
			//% of scanned asthma w >=1 Box Chked
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : asthmaScannedMap.values()) {
				Integer asthmacannedCount = row.getData();
				Integer asthmaScannedAnsweredCount = null;
				if (asthmaScannedAnsweredMap.get(row.getDateRange()) != null) {
					asthmaScannedAnsweredCount = asthmaScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (asthmaScannedAnsweredCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double)asthmaScannedAnsweredCount / asthmacannedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> asthmaPercentScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaPercentScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("asthmaPercentScannedAnsweredMap", asthmaPercentScannedAnsweredMap);
			
			//% of scanned asthma with anything marked
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : asthmaScannedMap.values()) {
				Integer asthmacannedCount = row.getData();
				Integer asthmaScannedAnythingMarkedCount = null;
				if (asthmaScannedAnythingMarkedMap.get(row.getDateRange()) != null) {
					asthmaScannedAnythingMarkedCount = asthmaScannedAnythingMarkedMap.get(row.getDateRange()).getData();
				}
				if (asthmaScannedAnythingMarkedCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) asthmaScannedAnythingMarkedCount / asthmacannedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> asthmaPercentScannedAnythingMarkedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				asthmaPercentScannedAnythingMarkedMap.put(row.getDateRange(), row);
			}
			map.put("asthmaPercentScannedAnythingMarkedMap", asthmaPercentScannedAnythingMarkedMap);
			
			//# PWS Printed
			List<Object[]> diabetesPrintedByWeek = atdService.getFormsPrintedByWeek("Diabetes_Care_Worksheet", null);
			rows = populateRows(diabetesPrintedByWeek);
			LinkedHashMap<String, WeeklyReportRow> diabetesPrintedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesPrintedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesPrintedMap", diabetesPrintedMap);
			
			//# PWS scanned
			List<Object[]> diabetesScannedByWeek = atdService.getFormsScannedByWeek("Diabetes_Care_Worksheet", null);
			rows = populateRows(diabetesScannedByWeek);
			LinkedHashMap<String, WeeklyReportRow> diabetesScannedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesScannedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesScannedMap", diabetesScannedMap);
			
			//% of PWS Scanned
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : diabetesPrintedMap.values()) {
				Integer diabetesPrintedCount = row.getData();
				Integer diabetescannedCount = null;
				if (diabetesScannedMap.get(row.getDateRange()) != null) {
					diabetescannedCount = diabetesScannedMap.get(row.getDateRange()).getData();
				}
				if (diabetescannedCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) diabetescannedCount / diabetesPrintedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> diabetesPercentScannedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesPercentScannedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesPercentScannedMap", diabetesPercentScannedMap);
			
			//# scanned diabetes w >=1 Box Chked
			List<Object[]> diabetesScannedAnsweredByWeek = atdService.getFormsScannedAnsweredByWeek("Diabetes_Care_Worksheet", null);
			rows = populateRows(diabetesScannedAnsweredByWeek);
			LinkedHashMap<String, WeeklyReportRow> diabetesScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("diabetesScannedAnsweredMap", diabetesScannedAnsweredMap);
			
			//# scanned diabetes w anything marked
			List<Object[]> diabetesScannedAnythingMarkedByWeek = atdService.getFormsScannedAnythingMarkedByWeek("Diabetes_Care_Worksheet",
			    locationName);
			rows = populateRows(diabetesScannedAnythingMarkedByWeek);
			LinkedHashMap<String, WeeklyReportRow> diabetesScannedAnythingMarkedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesScannedAnythingMarkedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesScannedAnythingMarkedMap", diabetesScannedAnythingMarkedMap);
			
			//% of scanned diabetes w >=1 Box Chked
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : diabetesScannedMap.values()) {
				Integer diabetescannedCount = row.getData();
				Integer diabetescannedAnsweredCount = null;
				if (diabetesScannedAnsweredMap.get(row.getDateRange()) != null) {
					diabetescannedAnsweredCount = diabetesScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (diabetescannedAnsweredCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) diabetescannedAnsweredCount / diabetescannedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> diabetesPercentScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesPercentScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("diabetesPercentScannedAnsweredMap", diabetesPercentScannedAnsweredMap);
			
			//% of scanned diabetes with anything marked
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : diabetesScannedMap.values()) {
				Integer diabetescannedCount = row.getData();
				Integer diabetescannedAnythingMarkedCount = null;
				if (diabetesScannedAnythingMarkedMap.get(row.getDateRange()) != null) {
					diabetescannedAnythingMarkedCount = diabetesScannedAnythingMarkedMap.get(row.getDateRange()).getData();
				}
				if (diabetescannedAnythingMarkedCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) diabetescannedAnythingMarkedCount / diabetescannedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> diabetesPercentScannedAnythingMarkedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesPercentScannedAnythingMarkedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesPercentScannedAnythingMarkedMap", diabetesPercentScannedAnythingMarkedMap);
			
			//# PSF Questions Printed & Scanned
			List<Object[]> psfQuestionsScannedByWeek = atdService.getQuestionsScanned("Asthma_Alert", null);
			rows = populateRows(psfQuestionsScannedByWeek);
			LinkedHashMap<String, WeeklyReportRow> psfQuestionsScannedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				psfQuestionsScannedMap.put(row.getDateRange(), row);
			}
			map.put("psfQuestionsScannedMap", psfQuestionsScannedMap);
			
			//# PSF Questions w >= 1 Box Chked
			List<Object[]> psfQuestionsScannedAnsweredByWeek = atdService.getQuestionsScannedAnswered("Asthma_Alert", null);
			rows = populateRows(psfQuestionsScannedAnsweredByWeek);
			LinkedHashMap<String, WeeklyReportRow> psfQuestionsScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				psfQuestionsScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("psfQuestionsScannedAnsweredMap", psfQuestionsScannedAnsweredMap);
			
			//% PSF Prompts w Response
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : psfQuestionsScannedMap.values()) {
				Integer psfQuestionsScannedCount = row.getData();
				Integer psfQuestionsScannedAnsweredCount = null;
				if (psfQuestionsScannedAnsweredMap.get(row.getDateRange()) != null) {
					psfQuestionsScannedAnsweredCount = psfQuestionsScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (psfQuestionsScannedAnsweredCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) psfQuestionsScannedAnsweredCount / psfQuestionsScannedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> psfPercentQuestionsScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				psfPercentQuestionsScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("psfPercentQuestionsScannedAnsweredMap", psfPercentQuestionsScannedAnsweredMap);
			
			//% PSF Prompts w Response - adjusted for blanks
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : psfQuestionsScannedMap.values()) {
				Integer psfQuestionsScannedCount = row.getData();
				Integer psfQuestionsScannedAnsweredCount = null;
				Integer asthmaScannedCount = null;
				Integer asthmaScannedAnsweredCount = null;
				
				if (psfQuestionsScannedAnsweredMap.get(row.getDateRange()) != null) {
					psfQuestionsScannedAnsweredCount = psfQuestionsScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (asthmaScannedMap.get(row.getDateRange()) != null) {
					asthmaScannedCount = asthmaScannedMap.get(row.getDateRange()).getData();
				}
				if (asthmaScannedAnsweredMap.get(row.getDateRange()) != null) {
					asthmaScannedAnsweredCount = asthmaScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (psfQuestionsScannedAnsweredCount != null && asthmaScannedCount != null && asthmaScannedAnsweredCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow
					        .setData(org.openmrs.module.chirdlutil.util.Util
					                .round(
					                    ((double) psfQuestionsScannedAnsweredCount / (psfQuestionsScannedCount - ((asthmaScannedCount - asthmaScannedAnsweredCount) * 20))) * 100,
					                    0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> psfPercentQuestionsScannedAnsweredAdjustedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				psfPercentQuestionsScannedAnsweredAdjustedMap.put(row.getDateRange(), row);
			}
			map.put("psfPercentQuestionsScannedAnsweredAdjustedMap", psfPercentQuestionsScannedAnsweredAdjustedMap);
			
			//# PWS Questions Printed & Scanned
			List<Object[]> diabetesQuestionsScannedByWeek = atdService.getQuestionsScanned("Diabetes_Care_Worksheet", null);
			rows = populateRows(diabetesQuestionsScannedByWeek);
			LinkedHashMap<String, WeeklyReportRow> diabetesQuestionsScannedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesQuestionsScannedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesQuestionsScannedMap", diabetesQuestionsScannedMap);
			
			//# PWS Questions w >= 1 Box Chked
			List<Object[]> diabetesQuestionsScannedAnsweredByWeek = atdService.getQuestionsScannedAnswered("Diabetes_Care_Worksheet", null);
			rows = populateRows(diabetesQuestionsScannedAnsweredByWeek);
			LinkedHashMap<String, WeeklyReportRow> diabetesQuestionsScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesQuestionsScannedAnsweredMap.put(row.getDateRange(), row);
			}
			map.put("diabetesQuestionsScannedAnsweredMap", diabetesQuestionsScannedAnsweredMap);
			
			//% PWS Prompts w Response
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : diabetesQuestionsScannedMap.values()) {
				Integer diabetesQuestionsScannedCount = row.getData();
				Integer diabetesQuestionsScannedAnsweredCount = null;
				if (diabetesQuestionsScannedAnsweredMap.get(row.getDateRange()) != null) {
					diabetesQuestionsScannedAnsweredCount = diabetesQuestionsScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (diabetesQuestionsScannedAnsweredCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow.setData(org.openmrs.module.chirdlutil.util.Util.round(
					    ((double) diabetesQuestionsScannedAnsweredCount / diabetesQuestionsScannedCount) * 100, 0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> diabetesPercentQuestionsScannedAnsweredMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesPercentQuestionsScannedAnsweredMap.put(row.getDateRange(), row);
			}
			
			map.put("diabetesPercentQuestionsScannedAnsweredMap", diabetesPercentQuestionsScannedAnsweredMap);
			
			//% PWS Prompts w Response - adjusted for blanks
			rows = new ArrayList<WeeklyReportRow>();
			for (WeeklyReportRow row : diabetesQuestionsScannedMap.values()) {
				Integer diabetesQuestionsScannedCount = row.getData();
				Integer diabetesQuestionsScannedAnsweredCount = null;
				Integer diabetesScannedCount = null;
				Integer diabetesScannedAnsweredCount = null;
				
				if (diabetesQuestionsScannedAnsweredMap.get(row.getDateRange()) != null) {
					diabetesQuestionsScannedAnsweredCount = diabetesQuestionsScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (diabetesScannedMap.get(row.getDateRange()) != null) {
					diabetesScannedCount = diabetesScannedMap.get(row.getDateRange()).getData();
				}
				if (diabetesScannedAnsweredMap.get(row.getDateRange()) != null) {
					diabetesScannedAnsweredCount = diabetesScannedAnsweredMap.get(row.getDateRange()).getData();
				}
				if (diabetesQuestionsScannedAnsweredCount != null && diabetesScannedCount != null && diabetesScannedAnsweredCount != null) {
					WeeklyReportRow percentRow = new WeeklyReportRow();
					percentRow.setDateRange(row.getDateRange());
					percentRow
					        .setData(org.openmrs.module.chirdlutil.util.Util
					                .round(
					                    ((double) diabetesQuestionsScannedAnsweredCount / (diabetesQuestionsScannedCount - ((diabetesScannedCount - diabetesScannedAnsweredCount) * 6))) * 100,
					                    0).intValue());
					rows.add(percentRow);
				}
			}
			LinkedHashMap<String, WeeklyReportRow> diabetesPercentQuestionsScannedAnsweredAdjustedMap = new LinkedHashMap<String, WeeklyReportRow>();
			for (WeeklyReportRow row : rows) {
				diabetesPercentQuestionsScannedAnsweredAdjustedMap.put(row.getDateRange(), row);
			}
			map.put("diabetesPercentQuestionsScannedAnsweredAdjustedMap", diabetesPercentQuestionsScannedAnsweredAdjustedMap);
			
		
		
		
		return showForm(request, response, errors, map);
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException,
	 *      java.util.Map)
	 */
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors,
	                                Map controlModel) throws Exception {
		LocationService locationService = Context.getLocationService();
		List<Location> locations = locationService.getAllLocations();
		
		if (controlModel == null) {
			controlModel = new HashMap<String, Object>();
		}
		
		controlModel.put("locations", locations);
		
		return super.showForm(request, response, errors, controlModel);
	}
	
	private List<WeeklyReportRow> populateRows(List<Object[]> databaseRows) throws ParseException {
		List<WeeklyReportRow> rows = new ArrayList<WeeklyReportRow>();
		
		if (databaseRows == null) {
			return rows;
		}
		
		for (Object[] databaseRow : databaseRows) {
			WeeklyReportRow row = new WeeklyReportRow();
			String startDateString = (String) databaseRow[0];
			String endDateString = (String) databaseRow[1];
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date startDate = formatter.parse(startDateString);
			Date endDate = formatter.parse(endDateString);
			SimpleDateFormat displayFormat = new SimpleDateFormat("MM/dd");
			
			row.setDateRange(displayFormat.format(startDate) + " - " + displayFormat.format(endDate));
			row.setData((java.math.BigInteger) databaseRow[2]);
			rows.add(row);
		}
		return rows;
	}
	
}
