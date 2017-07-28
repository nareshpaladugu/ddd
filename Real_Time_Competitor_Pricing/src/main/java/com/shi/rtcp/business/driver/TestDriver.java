package com.shi.rtcp.business.driver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.shi.rtcp.business.KeywordInterface;
import com.shi.rtcp.business.handlefailure.HandlerFailures;
import com.shi.rtcp.business.keywords.LaunchBrowser;
import com.shi.rtcp.business.keywords.util.KeywordFactoryImpl;
import com.shi.rtcp.business.testdata.DataSheetReader;
import com.shi.rtcp.email.EmailUtils;
import com.shi.rtcp.reader.TestReader;
import com.shi.rtcp.utils.AutomationUtil;
import com.shi.rtcp.utils.LoadProperties;
import com.shi.rtcp.utils.RTCPConstants;
import com.shi.rtcp.utils.RTCPException;
import com.shi.rtcp.utils.ResultUtil;
import com.shi.rtcp.utils.ResultVO;
import com.shi.rtcp.vos.BusinessProcessVO;
import com.shi.rtcp.vos.SingleTestStepVO;
import com.shi.rtcp.vos.TestStepExecutionResultVO;
import com.shi.rtcp.vos.TestVO;

/**
 * @author Dnyaneshwar Daphal
 *
 */
public class TestDriver {

	@Test(dataProvider="scriptProvider")
	public void driverTest(String sTestFileName)
	{
		System.out.println("Executing test : "+sTestFileName);

		AutomationUtil.checkTempFolder();

		ResultVO resultvo=new ResultVO();

		List<BusinessProcessVO> listOfBusniessProcesses=new LinkedList<BusinessProcessVO>();

		resultvo.setScriptName(sTestFileName);

		int totalFaliedTests = 0;
		int totalPassedTests = 0;

		WebDriver webDriver=null;

		TestVO testvo=null;
		try {
			testvo = TestReader.readTest(sTestFileName);

			LinkedList<BusinessProcessVO> driverRows = testvo.getDriverRows();

			TestStepExecutionResultVO testStepExecutionResultVO= null;

			Map<String, Map<String, Map<String, String>>>dataMap = new HashMap<String, Map<String,Map<String,String>>>();

			Map<String, String> rowNumberConfigurationMap = new HashMap<String,String>();

			boolean exitFlag = false;

			boolean stopFlag = false;

			for (BusinessProcessVO businessProcess : driverRows) {

				if(!businessProcess.getToBeExecuted().equalsIgnoreCase("X") && !businessProcess.getToBeExecuted().equalsIgnoreCase("N"))
				{
					businessProcess.setExecuted(true);

					if(!businessProcess.getTestDataFile().isEmpty())
					{
						try {
							dataMap= DataSheetReader.readDataSheets(businessProcess.getTestDataFile());

							Set<String> sheets = dataMap.keySet();

							for (String sheet : sheets) {

								if(businessProcess.getRowNumber().isEmpty())
								{
									//default row number is 1
									rowNumberConfigurationMap.put(sheet, "1");
								}
								else
								{
									rowNumberConfigurationMap.put(sheet, businessProcess.getRowNumber());
								}
							}

						} catch (RTCPException e) {

							businessProcess.setMessages(e.getMessage());
						}
					}

					LinkedList<TestStepExecutionResultVO> listOfExecutionsSteps=new LinkedList<TestStepExecutionResultVO>();
					Integer passedSoFar = 0;
					Integer failedSoFar = 0;
					Integer skippedSoFar = 0;

					businessProcess.setPassedTests(0);
					businessProcess.setFailedTest(0);
					businessProcess.setSkippedTests(0);

					businessProcess.setStartCalendar(Calendar.getInstance());

					LinkedList<SingleTestStepVO> testSteps =	businessProcess.getTestSteps();

					int startIt = businessProcess.getStartIt();
					int stopIt = businessProcess.getStopIt();

					if(startIt==-1)
					{
						startIt=1;
					}
					startIt--;

					if(stopIt==-1)
					{
						stopIt=businessProcess.getTestSteps().size();
					}

					stopIt--;


					exitFlag = false;

					for (int iCurrentTestStepNumber = startIt; iCurrentTestStepNumber <=stopIt; iCurrentTestStepNumber++) {

						SingleTestStepVO singleTestStepVO = testSteps.get(iCurrentTestStepNumber);

						if(!singleTestStepVO.getToBeExecuted().equalsIgnoreCase("X"))
						{

							Calendar testStartCal= Calendar.getInstance();

							if(singleTestStepVO.getAction().isEmpty())
							{
								continue;
							}

							if(singleTestStepVO.getAction().equalsIgnoreCase("LaunchBrowser"))
							{
								LaunchBrowser launchBrowser = new LaunchBrowser();

								try {
									testStepExecutionResultVO = launchBrowser.execute(null, singleTestStepVO.getParamsList(dataMap, rowNumberConfigurationMap));
								} catch (RTCPException e) {

									testStepExecutionResultVO.setDefectDesc(e.getMessage());
								}

								resultvo.setBrowser(singleTestStepVO.getParam2());

								webDriver = launchBrowser.getWebDriverInstance();
							}
							else
							{
								KeywordInterface keyword = KeywordFactoryImpl.getInstance().get(singleTestStepVO.getAction());

								if(keyword==null)
								{
									testStepExecutionResultVO = new TestStepExecutionResultVO();
									testStepExecutionResultVO.setDefectDesc("Unknown Keyword");
								}
								else
								{
									try {
										testStepExecutionResultVO = keyword.execute(webDriver, singleTestStepVO.getParamsList(dataMap, rowNumberConfigurationMap));
									} catch (RTCPException e) {

										testStepExecutionResultVO.setDefectDesc(e.getMessage());
									}
								}

							}

							switch (testStepExecutionResultVO.getStatus()) 
							{
							case RTCPConstants.PASS:

								passedSoFar++;
								businessProcess.setPassedTests(passedSoFar);
								totalPassedTests++;

								break;

							case RTCPConstants.FAIL:

								failedSoFar++;
								businessProcess.setFailedTest(failedSoFar);
								totalFaliedTests++;

								if(LoadProperties.CaptureBitmapOnFailure)
								{
									HandlerFailures.captureBitmapHandler(webDriver, sTestFileName, businessProcess,
											singleTestStepVO, testStepExecutionResultVO);
								}

								String sActionOnFail  = singleTestStepVO.getActionOnFail();

								if(sActionOnFail.equalsIgnoreCase(RTCPConstants.ACTION_ON_FAIL_EXIT))
								{
									exitFlag = true;
								}
								else if(sActionOnFail.equalsIgnoreCase(RTCPConstants.ACTION_ON_FAIL_STOP))
								{
									stopFlag = true;
								}
								else if(sActionOnFail.equalsIgnoreCase(RTCPConstants.ACTION_ON_FAIL_JUMP))
								{
									String sActionOnFailRow = singleTestStepVO.getActionOnFailParam();

									if(sActionOnFailRow.isEmpty())
									{
										testStepExecutionResultVO.setStatus(0);
										testStepExecutionResultVO.setDefectDesc(testStepExecutionResultVO.getDefectDesc()+" Action On Fail - Jump row not specified");
									}
									else
									{
										try {
											int jumpRow = Integer.parseInt(sActionOnFailRow);

											if(jumpRow>stopIt)
											{
												testStepExecutionResultVO.setStatus(0);
												testStepExecutionResultVO.setDefectDesc(testStepExecutionResultVO.getDefectDesc()
														+" Action On Fail - Invalid Jump row specified :"+sActionOnFailRow+ ", It should be less than stop iteration");
											}
											else
											{
												iCurrentTestStepNumber=jumpRow-2;
											}

										} catch (NumberFormatException e) {


											testStepExecutionResultVO.setStatus(0);
											testStepExecutionResultVO.setDefectDesc(testStepExecutionResultVO.getDefectDesc()
													+" Action On Fail - Invalid Jump row specified :"+sActionOnFailRow);
										}


									}

								}

								break;

							case RTCPConstants.SKIPPED:

								skippedSoFar++;
								businessProcess.setSkippedTests(skippedSoFar);
								//totalSkippedTest++;
								break;
							}

							if(testStepExecutionResultVO.getTestData().isEmpty())
								testStepExecutionResultVO.setTestData(singleTestStepVO.getParamForTestData());

							testStepExecutionResultVO.setTestCaseID(singleTestStepVO.getTeststep());
							testStepExecutionResultVO.setExecutionTimeStr(AutomationUtil.getExecutionTimingString(Calendar.getInstance()));
							testStepExecutionResultVO.setStepDescription(singleTestStepVO.getStepDescription());

							testStepExecutionResultVO.setStartCalendar(testStartCal);
							testStepExecutionResultVO.setStopCalendar(Calendar.getInstance());

							listOfExecutionsSteps.add(testStepExecutionResultVO);

							if(exitFlag)
							{
								break;
							}


							if(stopFlag)
							{
								break;
							}
						}	
					}

					businessProcess.setStopCalendar(Calendar.getInstance());
					businessProcess.setTestCaseExecutionList(listOfExecutionsSteps);

					listOfBusniessProcesses.add(businessProcess);	

					if(stopFlag)
					{
						break;
					}
				}
			}

			resultvo.setListOfBusniessProcesses(listOfBusniessProcesses);

			String sTestName = AutomationUtil.shortenTestName(sTestFileName);

			String sResultFileName ="./Results/"+sTestName+"_"+ResultUtil.getTimeStamp()+".xls";

			ResultUtil.generateExcelResult(resultvo,sResultFileName);

			if(LoadProperties.CaptureBitmapOnFailure && totalFaliedTests!=0)
			{
				HandlerFailures.generatFailureReport(sTestName+"_"+ResultUtil.getTimeStamp());
			}

			if(LoadProperties.EMAIL_FLAG)
			{
				EmailUtils.sendAutoEmail(sTestName, sResultFileName, totalPassedTests, totalFaliedTests, ResultUtil.calculateTotalTime(listOfBusniessProcesses));
			}

		} catch (RTCPException e1) {
			e1.printStackTrace();
		}
		finally
		{
			if(webDriver!=null)
			{
				try {
					//webDriver.quit();
				} catch (Exception e) {
				}
			}
		}
	}


	@DataProvider(name="scriptProvider", parallel=true)
	public Object[][] scriptProvider(){

		String files = LoadProperties.LST_FILES;
		String[] lstFiles= files.split(",");
		Object[][] lstOfFiles = new Object[lstFiles.length][1];
		int i = 0;
		for(String sFileName : lstFiles){
			lstOfFiles[i++] = new Object[]{LoadProperties.LST_FILES_FOLDER+sFileName};
		}
		return lstOfFiles;
	}
}
