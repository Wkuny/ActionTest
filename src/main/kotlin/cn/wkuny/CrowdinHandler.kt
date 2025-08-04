package cn.wkuny

import cn.hutool.core.io.FileUtil
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*

class CrowdinHandler(){
    private val urlBase = "https://zh.crowdin.com/editor/hypixel/499/en-"

    private lateinit var email:String
    private lateinit var crowdinPassword:String
    private lateinit var driver: FirefoxDriver
    private lateinit var wait: WebDriverWait
    private lateinit var detector: WebDriverWait
    private lateinit var windowMap:TreeMap<String, String>   // lang -> windowHandle
    private lateinit var writerMap:TreeMap<String, BufferedWriter>   // lang -> writer
    fun initialize(){
        windowMap = TreeMap<String, String>()
        writerMap = TreeMap<String, BufferedWriter>()
        driver = FirefoxDriver(FirefoxOptions().addArguments("--headless"))
        driver.get("https://zh.crowdin.com/editor/hypixel/499/en-zhcn")
        email = MainKt.accountInfo.email
        crowdinPassword = MainKt.accountInfo.crowdinPassword
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
        MainKt.loginBeginTime = System.currentTimeMillis()

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
        for(lang in MainKt.accountInfo.langList){
            driver.executeScript("window.open(\"${urlBase + lang}\")")
        }
        driver.switchTo().window(driver.windowHandles.elementAt(0))
        driver.close()
        assert(driver.windowHandles.size == MainKt.accountInfo.langList.size)
        driver.windowHandles.forEachIndexed { index, window ->
            driver.switchTo().window(window)
            wait.until(ExpectedConditions.urlContains("?view"))
            val currentURL = driver.currentUrl
            assert(currentURL!=null)
            assert(currentURL!!.startsWith("https://zh.crowdin.com/editor/hypixel/499/en-"))
            val langRegex = "^https://zh.crowdin.com/editor/hypixel/499/en-(\\w+)\\??.*$".toPattern()
            val lang = langRegex.matcher(currentURL).apply{ find() }.group(1)
            val filename = "./result-${lang}.csv"
            windowMap[lang] = window
            writerMap[lang] = FileUtil.getWriter("./result-${lang}.csv", StandardCharsets.UTF_8, false)
            writerMap[lang]!!.appendLine("批准状态,源字符串,翻译字符串")
        }
        var finished: Boolean
        do{
            finished = true
            windowMap.forEach { (lang, window) ->
                driver.switchTo().window(window)
                val result = getTranslationOf(lang)
                finished = finished && result
            }
        }while(!finished)
    }
    fun getTranslationOf(lang:String): Boolean{
        val writer:BufferedWriter = writerMap[lang]!!
        wait.until(ExpectedConditions.urlContains("zh.crowdin.com/editor/hypixel/499/en-${lang}"))
        // button.phrases-displayed -> 页码 "page / bound"
        wait.until(ExpectedConditions.textMatches(By.cssSelector("button.phrases-displayed"), "\\d+ / \\d+".toPattern()))
        val pageTag = driver.findElement(By.cssSelector("button.phrases-displayed"))
        // div#texts_paging_info button#next_page
        // ul#texts_list.texts-to-translate-list div.proofread-string-wrapper []
        val (page, pageBound) = pageTag.text.split(" / ").map{ it -> it.toInt()}
        wait.until(ExpectedConditions.textToBe(By.cssSelector("button.phrases-displayed"), "$page / $pageBound"))
        println("Page(${lang}) $page / $pageBound")
        val elements = driver.findElements(By.cssSelector("ul#texts_list.texts-to-translate-list div.proofread-string-wrapper"))
        for (element in elements){
            var approved:Boolean
            try{
                element.findElement(By.cssSelector("div.approved-status")) // 找不到元素时进入catch
                approved = true
            }catch (e: NoSuchElementException){
                approved = false
            }
            RetryTaskChain.run({
                val source = element.findElement(By.cssSelector("div.phrase-left div.singular.selectable")).text
                val translation = element.findElement(By.cssSelector("div.phrase-right textarea")).text
                writer.appendLine("${if(approved) "T" else "F"},${source},${translation}")
            }, 3, null)
        }
        writer.flush()
        if(page < pageBound){
            val nextPage =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#texts_paging_info button#next_page")))
            nextPage.click()
            return false
        } else return true

    }
}