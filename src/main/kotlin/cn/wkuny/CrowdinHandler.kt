package cn.wkuny

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.BufferedWriter
import java.time.Duration
import java.util.concurrent.TimeUnit

class CrowdinHandler(){
    private val url = "https://zh.crowdin.com/editor/hypixel/499/en-zhcn"

    private lateinit var email:String
    private lateinit var crowdinPassword:String
    private lateinit var driver: FirefoxDriver
    private lateinit var wait: WebDriverWait
    private lateinit var detector: WebDriverWait
    fun initialize(){
        driver = FirefoxDriver(FirefoxOptions().addArguments("--headless"))
        driver.get(url)
        email = MainKt.getAccountInfo().email
        crowdinPassword = MainKt.getAccountInfo().crowdinPassword
        wait = WebDriverWait(driver, Duration.ofSeconds(30))
        detector = WebDriverWait(driver, Duration.ofMillis(200))
    }
    fun login(){
        wait.until(ExpectedConditions.urlContains("accounts.crowdin.com/login"))
        val userNameField = detector.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#login_login")))
        val passwordField = detector.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#login_password")))
        val loginButton = detector.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#login-container>button")))
        userNameField.sendKeys(email)
        passwordField.sendKeys(crowdinPassword)
        loginButton.click()
    }
    fun verifyCode(){
        wait.until(ExpectedConditions.urlContains("accounts.crowdin.com/device-verify"))
        val codeField = detector.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#verification_code")))
//        val submitButton = detector.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#submit_verification_code")))

        val code = MainKt.verifyCode.toString()
        println("验证码：$code")
        if(code.isEmpty() || !code.matches("\\d{6}".toRegex())) throw IllegalArgumentException("验证码错误")
        codeField.sendKeys(code, Keys.ENTER)
//        submitButton.click()
    }
    fun rememberMe(){
        wait.until(ExpectedConditions.urlContains("accounts.crowdin.com/remember-me"))
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#remember-me"))).click()
    }
    fun getTranslation(){
        val writer:BufferedWriter = MainKt.writer
        writer.appendLine("批准状态,源字符串,翻译字符串")
        wait.until(ExpectedConditions.urlContains("zh.crowdin.com/editor/hypixel/499/en-zhcn"))
        // button.phrases-displayed -> 页码 "page / bound"
        wait.until(ExpectedConditions.textMatches(By.cssSelector("button.phrases-displayed"), "\\d+ / \\d+".toPattern()))
        val pageTag = driver.findElement(By.cssSelector("button.phrases-displayed"))
        // div#texts_paging_info button#next_page
        // ul#texts_list.texts-to-translate-list div.proofread-string-wrapper []
        val pageBound = pageTag.text.split(" / ")[1].toInt()
        for (page in 1..pageBound){
            wait.until(ExpectedConditions.textToBe(By.cssSelector("button.phrases-displayed"), "$page / $pageBound"))
            println("== Page $page / $pageBound ==")
            val elements = driver.findElements(By.cssSelector("ul#texts_list.texts-to-translate-list div.proofread-string-wrapper"))
            for (element in elements){
                var approved:Boolean
                try{
                    element.findElement(By.cssSelector("div.approved-status")) // 找不到元素时进入catch
                    approved = true
                }catch (e:NoSuchElementException){
                    approved = false
                }
                val source = element.findElement(By.cssSelector("div.phrase-left div.singular.selectable")).text
                val translation = element.findElement(By.cssSelector("div.phrase-right textarea")).text
                writer.appendLine("${if(approved) "T" else "F"},${source},${translation}")
                println("${if(approved) "T" else "F"} ${source} ${translation}")
            }
            if(page < pageBound){
                val nextPage =
                    wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#texts_paging_info button#next_page")))
                nextPage.click()
                TimeUnit.SECONDS.sleep(3)
            }
            writer.flush()
        }
    }
}