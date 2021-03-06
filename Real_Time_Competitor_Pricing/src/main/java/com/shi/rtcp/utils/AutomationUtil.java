package com.shi.rtcp.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.shi.rtcp.locators.LocatorVO;
import com.shi.rtcp.locators.Locators;
import com.shi.rtcp.vos.TestStepExecutionResultVO;

/**
 * @author ddaphal
 *
 */
public class AutomationUtil {


	public static String getLastModifiedFile(String csvDir,long clickTime) throws RTCPException {

		File dir = new File(csvDir);
		FileFilter fileFilter = new WildcardFileFilter("*.csv");
		File[] files = dir.listFiles(fileFilter);

		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

		if(files.length==0)
		{
			throw new RTCPException("File not found, Dir is empty");
		}

		if(files[0].lastModified()>clickTime)
			return files[0].getAbsolutePath();
		else
			throw new RTCPException("File not found");
	}

	public static String roundToNDigits(Double d,int digits)
	{
		String zeros =RTCPConstants.EMPTY_STRING;
		for (int i = 0; i < digits; i++) {
			zeros=zeros+"0";
		}
		DecimalFormat f = new DecimalFormat("##."+zeros);
		return f.format(d);
	}

	public static void checkTempFolder()
	{
		try {
			delete(new File("./Temp"));
			(new File("./Temp")).mkdir();
		} catch (IOException e2) {
			System.out.println("Temp folder not deleted !");
		}
	}

	public static void delete(File file)
			throws IOException{

		if(file.isDirectory()){

			if(file.list().length==0){

				file.delete();
				System.out.println("Directory is deleted : " 
						+ file.getAbsolutePath());

			}else{

				String files[] = file.list();

				for (String temp : files) {
					File fileDelete = new File(file, temp);

					delete(fileDelete);
				}

				if(file.list().length==0){
					file.delete();
					System.out.println("Directory is deleted : " 
							+ file.getAbsolutePath());
				}
			}

		}else{
			file.delete();
		}
	}

	public static void scrollToElement(WebDriver webDriver,WebElement element)
	{
		try {
			((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", element);
		} catch (Exception e) {

			//TODO - Handle this
			System.out.println("Exception in scrollToElement()");
		}
	}
	/**
	 * Return web element for given locator
	 * @param webDriver
	 * @param locator
	 * @return
	 */
	public static WebElement getElement(WebDriver webDriver,LocatorVO locator) throws RTCPException
	{
		if(webDriver==null)
		{
			throw new RTCPException("Browser not instantiated!");
		}
		WebElement element=null;

		WebDriverWait wait = new WebDriverWait(webDriver, 120);

		String locatorType=locator.getType();

		try 
		{
			switch(locatorType)
			{
			case "id":
				element =  wait.until(ExpectedConditions.presenceOfElementLocated(By.id(locator.getLocator())));
				break;
				
			case "name":
				element =  wait.until(ExpectedConditions.presenceOfElementLocated(By.name(locator.getLocator())));
				break;

			case "xpath":
				element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(locator.getLocator())));
				break;

			case "class":
				element = wait.until(ExpectedConditions.presenceOfElementLocated(By.className(locator.getLocator())));
				break;
				
			case "link":
				element = wait.until(ExpectedConditions.presenceOfElementLocated(By.partialLinkText(locator.getLocator())));
				break;

			default:
				throw new RTCPException("Unrec. locator provided "+locator.getLocator());
			}
		} catch (Exception e) {

			throw new RTCPException("Exception getElement() : "+locator.getLocator() +" "+e.getMessage());
		}

		if(element==null)
		{
			throw new RTCPException("Element not found : "+locator.getLocator());
		}
		else
		{
			AutomationUtil.highlightElement(webDriver, element);
		}
		return element;
	}

	/**
	 * Highlight the control
	 * 
	 * @param driver
	 * @param element
	 */
	public static void highlightElement(WebDriver driver,WebElement element) {
		try {

			if(!LoadProperties.HIGHLIGHT_OBJECT)
				return;

			String Scolor = "#00FF00";
			int iBlinkCount = 6;

			JavascriptExecutor js = ((JavascriptExecutor) driver);
			if (element!=null)
			{
				//String bgcolor  = element.getCssValue("backgroundColor");
				String bgcolor  = ((JavascriptExecutor)driver).executeScript("return arguments[0].style.border;",element).toString();
				//System.out.println("bgcolor .............."+bgcolor);
				for (int i = 0; i < iBlinkCount; i++) 
				{
					changeColor(Scolor, element, js);
					changeColor(bgcolor, element, js);
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Changes the color
	 * @param color
	 * @param element
	 * @param js
	 */
	public static void changeColor(String color, WebElement element,  JavascriptExecutor js) {

		js.executeScript("arguments[0].style.border='"+color+"';",  element);
		//js.executeScript("arguments[0].style.backgroundColor = '"+color+"';  arguments[0].style.border='3px solid red'; ",  element);
		try {
			Thread.sleep(10);
		}  catch (InterruptedException e) {
		}
	}

	public static DesiredCapabilities getBrowserCapabilities()
	{
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();

		capabilities.setCapability(CapabilityType.HAS_NATIVE_EVENTS, true);
		capabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
		capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
		capabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT,true);
		capabilities.setCapability(CapabilityType.SUPPORTS_APPLICATION_CACHE,true);
		capabilities.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS,true);
		capabilities.setCapability(CapabilityType.SUPPORTS_LOCATION_CONTEXT,true);
		capabilities.setCapability(CapabilityType.SUPPORTS_WEB_STORAGE,true);
		capabilities.setCapability(CapabilityType.SUPPORTS_ALERTS,true);

		return capabilities;
	}

	public static String getConditionSymbol(String condition)
	{
		switch(condition)
		{

		case "equals":case "==":
			return "==";

		case "not equals":case "!":
			return "!";

		case "less":case "<":
			return "<";

		case "greater":case ">":
			return ">";

		case "less or equal": case "<=":
			return "<=";


		case "greater or equal":case ">=":
			return ">=";
		}

		//default = = 
		return "==";
	}

	public  static String getExecutionTimingString(Calendar calendar) {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		String timeStr = hour + ":" + minute + ":" + second;
		return timeStr;
	}


	public static int getPageCounts(WebDriver webDriver,String l)
	{
		int pageCount=0;
		try {

			LocatorVO loc=new LocatorVO(l);

			WebElement ele = getElement(webDriver, loc);

			String totalAvailablePages = ele.getText();

			totalAvailablePages=totalAvailablePages.replaceAll(",","");

			System.out.println("totalAvailablePages.... "+totalAvailablePages);

			return Integer.parseInt(totalAvailablePages)>=5?5:Integer.parseInt(totalAvailablePages);

		} catch (RTCPException e) {

			e.printStackTrace();
		}

		System.out.println("Got page Count : "+pageCount);

		return pageCount;
	}


	public static LocatorVO getTableLocatorByName(String name)
	{
		switch(name.toUpperCase())
		{
		case RTCPConstants.PRICE_MAIN_DATA_TABLE_HEADER_VARIABLE:
			return Locators.RealTimePriceMainDataTableHeader;

		case RTCPConstants.PRICE_MAIN_DATA_TABLE_VARIABLE:
			return Locators.RealTimePriceMainDataTable;

		case RTCPConstants.PRICE_DATA_TABLE_HEADER_VARIABLE:
			return Locators.RealTimePriceDataTableHeader;

		case RTCPConstants.PRICE_DATA_TABLE_VARIABLE:
			return Locators.RealTimePriceDataTable;
		}

		return null;
	}

	public static void clearText(WebElement input)
	{
		String getLengthfromTextBox = input.getAttribute("value").toString();
		int slength=getLengthfromTextBox.length();
		for (int o=0;o<slength;o++)
		{
			input.sendKeys(Keys.BACK_SPACE);

		}

		input.clear();
	}

	public static String getTableIdByName(String name)
	{
		//System.out.println("getTableIdByName() : "+name);
		switch(name.toUpperCase())
		{
		case RTCPConstants.PRICE_MAIN_DATA_TABLE_VARIABLE:
			return RTCPConstants.PRICE_MAIN_DATA_TABLE_ID;

		case RTCPConstants.PRICE_DATA_TABLE_VARIABLE:
			return RTCPConstants.PRICE_DATA_TABLE_ID;
		}

		return null;
	}


	public static TestStepExecutionResultVO returnExceptionResult(TestStepExecutionResultVO testStepExecutionResultVO,Exception e)
	{
		/*if(e.getMessage().contains("Exception : element not visible"))
		{
			testStepExecutionResultVO.setDefectDesc("Exception : element not visible");
		}
		else*/
		{
			testStepExecutionResultVO.setDefectDesc("Exception : "+e.getMessage());
		}
		return testStepExecutionResultVO;
	}

	public static TestStepExecutionResultVO returnUnderConstructionResult()
	{
		TestStepExecutionResultVO testStepExecutionResultVO = new TestStepExecutionResultVO();

		testStepExecutionResultVO.setStatus(1);

		testStepExecutionResultVO.setDefectDesc("Business definition pending");

		return testStepExecutionResultVO;
	}
	/*
	public static String convertDate2(String s)
	{
		String D= s.substring(0,s.indexOf("/"));

		if(D.length()==1)
		{
			D="0"+D;
		}


		String M= s.substring(s.indexOf("/")+1,s.lastIndexOf("/"));

		if(M.length()==1)
		{
			M="0"+M;
		}

		String Y= s.substring(s.lastIndexOf("/")+1,s.indexOf(" "));

		String T1= s.substring(s.indexOf(" ")+1,s.indexOf(":"));

		if(T1.length()==1)
		{
			T1="0"+T1;
		}



		String T2= s.substring(s.indexOf(":")+1);

		if(T2.length()==1)
		{
			T2="0"+T2;
		}

		return D+"/"+M+"/"+Y+" "+T1+":"+T2;

	}

	public static String convertDate(String s)
	{
		String M= s.substring(s.lastIndexOf("-")+1,s.indexOf(" "));

		String D= s.substring(s.indexOf("-")+1,s.lastIndexOf("-"));

		String Y= s.substring(0,s.indexOf("-"));

		String T= s.substring(s.indexOf(" ")+1,s.lastIndexOf(":"));

		return D+"/"+M+"/"+Y+" "+T;

	}
	 */

	public static String shortenTestName(String sTestFileName)
	{
		String sTestName=sTestFileName;

		if(sTestName.contains("/"))
		{
			sTestName=sTestName.substring(sTestName.lastIndexOf("/")+1);
		}
		else if(sTestName.contains("\\"))
		{
			sTestName=sTestName.substring(sTestName.lastIndexOf("\\")+1);
		}
		
		sTestName=sTestName.replace(".xlsx", "");
		
		return sTestName;
	}
}
