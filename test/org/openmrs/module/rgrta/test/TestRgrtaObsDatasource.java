package org.openmrs.module.rgrta.test;

import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.result.Result;

import org.openmrs.module.rgrta.QueryKite;
import org.openmrs.module.dss.hibernateBeans.Rule;
import org.openmrs.module.dss.service.DssService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

/**
 * @author Tammy Dugan
 * 
 */
@SkipBaseSetup
public class TestRgrtaObsDatasource extends BaseModuleContextSensitiveTest
{

	/**
	 * Set up the database with the initial dataset before every test method in
	 * this class.
	 * 
	 * Require authorization before every test method in this class
	 * 
	 */
	@Before
	public void runBeforeEachTest() throws Exception {
		// create the basic user and give it full rights
		initializeInMemoryDatabase();
		executeDataSet(TestUtil.DBUNIT_SETUP_FILE);
		// authenticate to the temp database
		authenticate();
	}

	@Test
	public void testRgrtaObsDatasource()throws Exception
	{
		DssService dssService = Context
				.getService(DssService.class);
		Integer patientId = 30520;
		Patient patient = Context.getPatientService().getPatient(patientId);
		String mrn = "999995";

		QueryKite.mrfQuery(mrn, patientId);// query and add to datasource

		ArrayList<Rule> ruleList = new ArrayList<Rule>();
		Rule rule = new Rule();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("conceptName", "CALCIUM (SMA)");
		rule.setTokenName("testRgrtaObsDatasource");
		rule.setParameters(parameters);

		ruleList.add(rule);
		ArrayList<Result> results = dssService.runRules(patient, ruleList,
				 null, null);

		for (Result result : results)
		{
			if (result != null)
			{
				for (int i = 0; i < result.size(); i++)
				{
					System.out.println(result.get(i).toNumber());
					System.out.println(result.get(i).getResultDate());
				}
			}
		}
	}
}