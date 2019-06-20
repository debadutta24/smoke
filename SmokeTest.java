package com.nexant;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.*;

import java.util.Set;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.WebDriverRunner.url;

public class SmokeTest {
    String url, version, build, username, password, activitiBuild;

    @BeforeSuite
    public void setup() {
        System.setProperty("selenide.browser", "chrome");
        System.setProperty("selenide.headless", "true");
        Configuration.timeout = 65000;
        url = System.getProperty("url");
        version = System.getProperty("version");
        build = System.getProperty("build");
        username = System.getProperty("username");
        if (url.contains("stag") || url.contains("build") || url.contains("demo")) {
            password = System.getProperty("passwordStage");
        }
        else if (url.contains("8080")) {
            password = System.getProperty("passwordQA");
        }
        else if (url.contains("nexant-dev")) {
            password = System.getProperty("passwordQAAWS");
        }
        else {
            password = System.getProperty("passwordProd");
        }

        activitiBuild = System.getProperty("activitiBuild");
        if (username == null) username = "username";
        if (password == null) password = "password";
        if (version == null) version = "0.0.0";
        if (build == null) build = "0";
        if (activitiBuild == null) activitiBuild = "0";
    }

    @Test
    public void smokeTest() throws InterruptedException {
        System.out.println("Navigate to: " + url);
        open(url);
        System.out.println("Verify that the bottom of the login page shows the " +
                "correct version and build #: " + version + " " + build);
        $(By.className("footer")).scrollTo().shouldHave(text("Version " + version + " Build " + build));
        System.out.println("Enter username and password, then press enter.");
        $("#j_username").scrollTo().setValue(username);
        $("#j_password").setValue(password).pressEnter();
        System.out.println("Verify that homepage loads, and take a screenshot.");
        $("#mainNav").shouldBe(visible);
        screenshot("main application");

        checkReports();
        checkActiviti();
        checkPUX(url);
    }

    private void checkReports() throws InterruptedException {
        if (!url.contains("nexant-dev")) {
            WebDriver driver = WebDriverRunner.getWebDriver();
            String firstWindowHandle = driver.getWindowHandle();
            System.out.println("Click 'Reports' link.");
            $$(".welcome").findBy(text("Reports")).scrollTo().click();
            System.out.println("Switch to reporting tab");
            switchToNextTab(driver);
            closePopovers();
            System.out.println("Verify that reports homepage shows.");
            $(".homeMain").shouldBe(visible);
            screenshot("reports");
            System.out.println("Close reports tab.");
            driver.close();
            driver.switchTo().window(firstWindowHandle);
        }
    }

    private void checkActiviti() throws InterruptedException {
        //skip checking activiti for environments that don't have any programs
        if (noPrograms(url)) {
            System.out.println("Skipping activiti check because this environment has no programs");
            return;
        }
        WebDriver driver = WebDriverRunner.getWebDriver();
        String firstWindowHandle = driver.getWindowHandle();
        System.out.println("Click on Programs tab.");
        $("#tabPrograms").click();
        System.out.println("Hover over first program and select 'Program Editor' option.");
        $(".x-grid-row", 0).hover();
        $(By.linkText("Program Editor")).click();
        System.out.println("Switch to activiti tab.");
        switchToNextTab(driver);

        closePopovers();
        System.out.println("Verify that 'Processes' link is visible and take screenshot.");
        $(By.linkText("Processes")).shouldBe(visible);
        screenshot("activiti");
        String[] urlParts = url().split("/activiti-app/");
        String activitiBuildNumberURL = urlParts[0] + "/activiti-app/app/dsmc/public/buildnumber";
        System.out.println("Open " + activitiBuildNumberURL + " , verify that build # is " + activitiBuild);
        open(activitiBuildNumberURL);
        $(By.xpath("/html/body")).shouldHave(text(activitiBuild));
        screenshot("activitiBuildNumber");

        System.out.println("Close activiti tab");
        driver.close();
        driver.switchTo().window(firstWindowHandle);
    }

    private void checkPUX(String url) {
        if (hasPUX(url)) {
            String[] urlParts = url().split("/traksmart4/");
            String PUXURL = urlParts[0] + "/traksmart4/html/pux/commercial/auth/login";
            System.out.println("Open PUX at: " + PUXURL + " and check that login page is visible.");
            open(PUXURL);
            $("#username").shouldBe(visible);
            $("#password").shouldBe(visible);
            screenshot("PUX");
        }
        else {
            System.out.println("Skipping PUX check, as this environment does not have PUX enabled.");
        }
    }

    private boolean hasPUX(String url) {
        return url.toLowerCase().contains("172.20.19.126") ||
                url.toLowerCase().contains("scedemo") ||
                url.toLowerCase().contains("sce-build") ||
                url.toLowerCase().contains("sce-staging") ||
                url.toLowerCase().contains("efficiencyworks") ||
                url.toLowerCase().contains("psegren") ||
                url.toLowerCase().contains("pse-staging") ||
                url.toLowerCase().contains("srp5-staging") ||
                url.toLowerCase().contains("psegdsmc-staging") ||
                url.toLowerCase().contains("questar");
    }

    private boolean noPrograms(String url) {
        return url.toLowerCase().contains("georgiapower") ||
                url.toLowerCase().contains("sce-build") ||
                url.toLowerCase().contains("ameren") ||
                url.toLowerCase().contains("mactri") ||
                //ouc does have programs, but nexantsupport user doesn't have rights to view them.
                url.toLowerCase().contains("ouc") ||
                url.toLowerCase().contains("siliconvalleypower")||
                url.toLowerCase().contains("qa05")||
                url.toLowerCase().contains("qa04");
    }

    private void switchToNextTab(WebDriver driver) {
        Wait().until(ExpectedConditions.numberOfWindowsToBe(2));
        driver.switchTo().window(nextWindowHandle(driver));
    }

    private void closePopovers() throws InterruptedException {
        System.out.println("Close tutorial pop-overs if present.");
        Thread.sleep(1000);
        while ($(".popover-navigation button").isDisplayed()) {
            $(".popover-navigation button").scrollTo().click();
            Thread.sleep(3500);
        }
    }

    private String nextWindowHandle(WebDriver driver) {
        String windowHandle = driver.getWindowHandle();
        Set<String> windowHandles = driver.getWindowHandles();
        windowHandles.remove(windowHandle);

        return windowHandles.iterator().next();
    }
}
