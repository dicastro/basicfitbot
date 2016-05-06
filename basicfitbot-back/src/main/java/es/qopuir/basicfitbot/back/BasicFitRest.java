package es.qopuir.basicfitbot.back;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import es.qopuir.basicfitbot.back.PhantomjsDownloader.Version;

@Component
public class BasicFitRest {
	private static final Logger LOG = LoggerFactory.getLogger(BasicFitRest.class);

	private static final String BASICFIT_TIMETABLE_BASEURL = "https://portal.virtuagym.com/classes/week/{day}";

	private static final MultiValueMap<String, String> BASICFIT_TIMETABLE_PARAMS = new LinkedMultiValueMap<String, String>();

	static {
		BASICFIT_TIMETABLE_PARAMS.add("event_type", "1");
		BASICFIT_TIMETABLE_PARAMS.add("coach", "0");
		BASICFIT_TIMETABLE_PARAMS.add("activity_id", "0");
		BASICFIT_TIMETABLE_PARAMS.add("member_id_filter", "0");
		BASICFIT_TIMETABLE_PARAMS.add("embedded", "1");
		BASICFIT_TIMETABLE_PARAMS.add("planner_type", "7");
		BASICFIT_TIMETABLE_PARAMS.add("show_personnel_schedule", "");
		BASICFIT_TIMETABLE_PARAMS.add("in_app", "0");
		BASICFIT_TIMETABLE_PARAMS.add("pref_club", "10560");
		BASICFIT_TIMETABLE_PARAMS.add("embedded", "1");
	}

	private final ProxyProperties proxyProperties;

	@Autowired
	public BasicFitRest(ProxyProperties proxyProperties) {
		this.proxyProperties = proxyProperties;
	}

	public enum Mode {
		TODAY, WEEK;
	}

	private String getUrl() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		LocalDate today = LocalDate.now();

		UriComponents uri = UriComponentsBuilder.fromHttpUrl(BASICFIT_TIMETABLE_BASEURL)
				.queryParams(BASICFIT_TIMETABLE_PARAMS).buildAndExpand(today.format(formatter));

		return uri.toUriString();
	}
	
	/**
	 * Returns today in short format (ex: Wed 27 Apr)
	 * 
	 * @return today in short format (ex: Wed 27 Apr)
	 */
	private String getTodayShort() {
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd MMM").withLocale(Locale.ENGLISH);
	    
	    LocalDate today = LocalDate.now();
	    
	    return today.format(formatter);
    }
	
	/**
     * Returns today in long format (ex: Wednesday 27 Apr)
     * 
     * @return today in long format (ex: Wednesday 27 Apr)
     */
	private String getTodayLong() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMM").withLocale(Locale.ENGLISH);
        
        LocalDate today = LocalDate.now();
        
        return today.format(formatter);
    }

	public byte[] getBasicFitTimetable() throws IOException {
		return getBasicFitTimetable(Mode.TODAY);
	}

	public DesiredCapabilities getDriverCapabilities() {
		DesiredCapabilities capabilities = new DesiredCapabilities();

		if (proxyProperties.isEnabled()) {
			Proxy proxy = new Proxy();
			proxy.setProxyType(ProxyType.MANUAL).setHttpProxy(proxyProperties.getHostPort())
					.setSslProxy(proxyProperties.getHostPort()).setNoProxy("localhost, 127.0.0.1");
			capabilities.setCapability(CapabilityType.PROXY, proxy);
		}

		capabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
		
		capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
		        PhantomjsDownloader.download(Version.V_2_1_1, proxyProperties).getAbsolutePath());

		return capabilities;
	}

	// @Cacheable("idealistaBuildingHtmlRequest")
	public byte[] getBasicFitTimetable(Mode mode) throws IOException {
		WebDriver driver = new PhantomJSDriver(getDriverCapabilities());

		// driver.manage().window().setSize(new Dimension(1920, 1773));
		driver.manage().window().setSize(new Dimension(350, 1773));

		driver.get(getUrl());

		if (driver instanceof JavascriptExecutor) {
			final JavascriptExecutor js = (JavascriptExecutor) driver;

			driver.findElement(By.id("head")).findElements(By.tagName("a")).forEach((e) -> {
				js.executeScript("arguments[0].parentNode.removeChild(arguments[0])", e);
			});

			List<Integer> deletedColumnIndex = new ArrayList<Integer>();

			List<WebElement> titleColumns = driver.findElement(By.id("schedule_header"))
					.findElements(By.tagName("div"));

			IntStream.range(0, titleColumns.size()).forEach((i) -> {
				long todaySpans = titleColumns.get(i).findElements(By.tagName("span")).stream().filter((e) -> {
					return !e.getText().equalsIgnoreCase(getTodayShort())
							&& !e.getText().equalsIgnoreCase(getTodayLong());
				}).count();

				if (todaySpans > 1) {
					js.executeScript("arguments[0].parentNode.removeChild(arguments[0])", titleColumns.get(i));
					deletedColumnIndex.add(i);
				}
			});

			List<WebElement> contentColumns = driver.findElement(By.id("schedule_content"))
					.findElements(By.className("cal_column"));

			IntStream.range(0, contentColumns.size()).forEach((i) -> {
				if (deletedColumnIndex.contains(i)) {
					js.executeScript("arguments[0].parentNode.removeChild(arguments[0])", contentColumns.get(i));
				}
			});
		}

		byte[] screenshot = takeScreenshot(driver);

		driver.close();

		return screenshot;
	}

	private byte[] takeScreenshot(WebDriver driver) throws IOException {
		return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
	}
}