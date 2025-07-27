package cn.wkuny

data class AccountInfo(val email:String,
                       val clientID:String,
                       val refreshToken:String,
                       val emailPassword:String? = null,
                       val crowdinPassword:String
)