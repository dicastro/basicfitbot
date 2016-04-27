package es.qopuir.basicfitbot.back;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
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

import net.anthavio.phanbedder.Phanbedder;

@Component
public class BasicFitRest {
    private static final Logger LOG = LoggerFactory.getLogger(BasicFitRest.class);

    private static final String BASICFIT_WEEK_URL = "https://portal.virtuagym.com/classes/week/2016-04-27?event_type=1&coach=0&activity_id=0&member_id_filter=0&embedded=1&planner_type=7&show_personnel_schedule=&in_app=0&pref_club=10560&embedded=1";

    @Autowired
    private ProxyProperties proxyProperties;

    // @Cacheable("idealistaBuildingHtmlRequest")
    public void getBasicFitTimetable(File dest) throws IOException {
        File phantomjs = Phanbedder.unpack();

        DesiredCapabilities cap = new DesiredCapabilities();

        if (proxyProperties.isEnabled()) {
            Proxy proxy = new Proxy();
            proxy.setProxyType(ProxyType.MANUAL).setHttpProxy(proxyProperties.getHostPort()).setSslProxy(proxyProperties.getHostPort())
                    .setNoProxy("localhost, 127.0.0.1");
            cap.setCapability(CapabilityType.PROXY, proxy);
        }

        cap.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
        cap.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjs.getAbsolutePath());

        // WebDriver driver = new FirefoxDriver();
        WebDriver driver = new PhantomJSDriver(cap);
        // driver.manage().window().setSize(new Dimension(1920, 1773));
        driver.manage().window().setSize(new Dimension(350, 1773));

        driver.get(BASICFIT_WEEK_URL);

        if (driver instanceof JavascriptExecutor) {
            final JavascriptExecutor js = (JavascriptExecutor) driver;

            driver.findElement(By.id("head")).findElements(By.tagName("a")).forEach((e) -> {
                js.executeScript("arguments[0].parentNode.removeChild(arguments[0])", e);
            });

            List<Integer> deletedColumnIndex = new ArrayList<Integer>();

            List<WebElement> titleColumns = driver.findElement(By.id("schedule_header")).findElements(By.tagName("div"));

            IntStream.range(0, titleColumns.size()).forEach((i) -> {
                long todaySpans = titleColumns.get(i).findElements(By.tagName("span")).stream().filter((e) -> {
                    return !e.getText().equalsIgnoreCase("Wed 27 Apr") && !e.getText().equalsIgnoreCase("Wednesday 27 Apr");
                }).count();

                if (todaySpans > 1) {
                    js.executeScript("arguments[0].parentNode.removeChild(arguments[0])", titleColumns.get(i));
                    deletedColumnIndex.add(i);
                }
            });

            List<WebElement> contentColumns = driver.findElement(By.id("schedule_content")).findElements(By.className("cal_column"));

            IntStream.range(0, contentColumns.size()).forEach((i) -> {
                if (deletedColumnIndex.contains(i)) {
                    js.executeScript("arguments[0].parentNode.removeChild(arguments[0])", contentColumns.get(i));
                }
            });
        }

        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        // Now you can do whatever you need to do with it, for example copy somewhere
        FileUtils.copyFile(srcFile, dest);
        driver.close();
    }
}