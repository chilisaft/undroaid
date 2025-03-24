package com.chilisaft.undroaid.data.models

data class Server(
    val owner: Owner?,
    val guid: String?,
    val apiKey: String?,
    val name: String?,
    val wanIp: String?,
    val lanIp: String?,
    val localUrl: String?,
    val remoteUrl: String?
)