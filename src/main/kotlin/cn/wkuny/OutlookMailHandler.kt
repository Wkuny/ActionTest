package cn.wkuny

import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMultipart

class OutlookMailHandler(val user:String, val clientID:String, val refreshToken:String) {
    private val client = HttpClient.newHttpClient()
    private lateinit var store: Store
    private lateinit var accessToken: String

    private var verifyCode:String? = null

    fun getAccessToken(){
        val requestRefreshToken = HttpRequest.newBuilder()
            .uri(URI.create("https://login.microsoftonline.com/common/oauth2/v2.0/token"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=refresh_token&client_id=${clientID}&refresh_token=${refreshToken}"))
            .build()
        val response = client.send(requestRefreshToken, HttpResponse.BodyHandlers.ofString())
        val body = response.body()
        this.accessToken = gson.fromJson(body, RefreshTokenResponse::class.java).access_token
    }
    fun interactWithOutlook(){
        val prop = Properties()
        prop.setProperty("mail.store.protocol", "imap")
        prop.setProperty("mail.imap.host", "outlook.office365.com")
        prop.setProperty("mail.imap.port", "993")
        prop.setProperty("mail.imap.ssl.enable", "true")
        prop.setProperty("mail.imap.auth.mechanisms", "XOAUTH2")
        store = Session.getInstance(prop).getStore("imap")
        store.connect("outlook.office365.com",993, user, accessToken)
    }
    private fun getInbox(): Array<Message>{
        val folder = store.defaultFolder.getFolder("INBOX")
        folder.open(Folder.READ_ONLY)
        return folder.messages
    }
    fun findVerifyCode(){
        val messages = getInbox()
        for(index in messages.size-1 downTo 0){
            val from = messages[index].from[0] as InternetAddress
            if(!from.personal.equals("Crowdin")) continue
            if(messages[index].receivedDate.time + 15*60*1000 < System.currentTimeMillis()) continue
            val content = messages[index].content as MimeMultipart
            this.verifyCode = getVerifyCodeFrom(content.getBodyPart(0).content.toString())
            Main.verifyCode = this.verifyCode
            return
        }
    }
    companion object{
        @JvmStatic
        val gson = Gson();
        @JvmStatic
        private val pattern = java.util.regex.Pattern.compile("(\\d{6})$", java.util.regex.Pattern.MULTILINE)
        @JvmStatic
        fun getVerifyCodeFrom(content:String):String{
            val matcher = pattern.matcher(content)
            if(matcher.find()) return matcher.group(1)
            else throw IllegalArgumentException("No matching verify code.")     //应当触发重试补偿
        }
    }
}
