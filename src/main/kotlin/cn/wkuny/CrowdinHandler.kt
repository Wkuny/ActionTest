package cn.wkuny

import org.openqa.selenium.By
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.util.concurrent.TimeUnit

class CrowdinHandler(private val email:String, private val crowdinPassword:String){
    private val driver = FirefoxDriver()
    private val url = "https://zh.crowdin.com/editor/hypixel/499/en-zhcn"
    private val wait = WebDriverWait(driver, Duration.ofSeconds(10))
    init{
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
        driver.get(url)
    }
    fun login(){
        wait.until(ExpectedConditions.urlContains("accounts.crowdin.com/login"))
        val userNameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#login_login")))
        val passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#login_password")))
        val loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#login-container>button")))
        userNameField.sendKeys(email)
        passwordField.sendKeys(crowdinPassword)
        loginButton.click()
    }
    fun verifyCode(){
        wait.until(ExpectedConditions.urlContains("accounts.crowdin.com/device-verify"))
        val codeField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#verification_code")))
        val submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#submit_verification_code")))

        Main.lock.lock()
        val code = Main.verifyCode
        Main.lock.unlock()
        println("验证码：$code")
        if(code==null || code.isEmpty() || !code.matches("\\d{6}".toRegex())) throw IllegalArgumentException("验证码错误")
        codeField.sendKeys(code)
        submitButton.click()
    }
    fun rememberMe(){
        wait.until(ExpectedConditions.urlContains("https://accounts.crowdin.com/remember-me"))
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#remember-me"))).click()

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#remember-me")))
    }
    fun getTranslation(){
        // ul#texts_list.texts-to-translate-list div.proofread-string-wrapper
    }
}