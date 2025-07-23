package cn.wkuny

data class RefreshTokenResponse(val token_type: String,
                                val expires_in:Int,
                                val scope:String,
                                val refresh_token:String,
                                val access_token:String
)
