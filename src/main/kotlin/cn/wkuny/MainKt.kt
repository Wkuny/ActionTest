package cn.wkuny

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

object MainKt {
    private val gson = Gson()
    @Volatile var loginBeginTime = -114514L


    val verifyCode = StringBuffer()
    val accountInfo: AccountInfo
    init{
        val accountJson = IOUtils.toString(MainKt::class.java.getResourceAsStream("/account.json"),StandardCharsets.UTF_8)
        accountInfo = gson.fromJson(accountJson, AccountInfo::class.java)
    }
    @JvmStatic
    fun main(args: Array<String>) {
        val outlookHandler = OutlookMailHandler(accountInfo.email, accountInfo.clientID, accountInfo.refreshToken)
        val crowdinHandler = CrowdinHandler()

        val crowdinTaskChain = RetryTaskChain()
            .step(crowdinHandler::initialize,"初始化",3)
            .step(crowdinHandler::login,"登录Crowdin",1)
            .step(outlookHandler::getAccessToken, "获取AccessToken",3)
            .step(outlookHandler::interactWithOutlook,"IMAP登录Outlook",3)
            .step(outlookHandler::findVerifyCode,"获取验证码",3)
            .step(crowdinHandler::verifyCode,"验证码验证",3)
            .step(crowdinHandler::rememberMe,"操作记住我页面",1)
            .step(crowdinHandler::getTranslation,"获取翻译",1)
            .step(::finalize, "结束",1)
        val retryTaskFailedException = crowdinTaskChain.run()
        if(retryTaskFailedException!=null) throw retryTaskFailedException
    }
    @JvmStatic
    fun finalize(){

    }
}