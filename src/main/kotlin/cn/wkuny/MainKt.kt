package cn.wkuny

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

object MainKt {
    private val gson = Gson()
    private val accountInfo: AccountInfo
    init{
        val accountJson = IOUtils.toString(MainKt::class.java.getResourceAsStream("/account.json"),StandardCharsets.UTF_8)
        accountInfo = gson.fromJson(accountJson, AccountInfo::class.java)
    }
    @JvmStatic
    fun main(args: Array<String>) {
        val outlookHandler = OutlookMailHandler(accountInfo.email, accountInfo.clientID, accountInfo.refreshToken)
        val crowdinHandler = CrowdinHandler(accountInfo.email, accountInfo.crowdinPassword)
        val outlookTaskChain = RetryTaskChain()
            .step(outlookHandler::getAccessToken, "获取AccessToken",3)
            .step(outlookHandler::interactWithOutlook,"IMAP登录Outlook",3)
            .step(Main.lock::lock,"获取锁",1)          // 获取到锁 = 需要获取验证码，触发收取邮件
            .step(outlookHandler::findVerifyCode,"获取验证码",3)
            .step(Main.lock::unlock,"释放锁",1)

        val crowdinTaskChain = RetryTaskChain()
            .step(Main.lock::lock,"获取锁",1)          // 获取锁，暂时阻止收取邮件获取验证码
            .step(crowdinHandler::login,"登录Crowdin",1)
            .step(Main.lock::unlock,"释放锁",1)        // 释放锁，允许收取邮件获取验证码
            .step(crowdinHandler::verifyCode,"验证码验证",1)
            .step(crowdinHandler::rememberMe,"操作记住我页面",1)
            .step(crowdinHandler::getTranslation,"获取翻译",1)
        val outlookThread = Thread(outlookTaskChain::run,"Outlook")
        val crowdinThread = Thread(crowdinTaskChain::run,"Crowdin")

        crowdinThread.start()
        outlookThread.start()
    }
}