package com.chilisaft.undroaid.data.repository

interface StorageRepository {
    var apiToken: String?
    var address: String?
    var username: String?
    var password: String?
    var lastNotification: String?
    var theme: Int
    var showCoreList: Boolean
    val uuid: String
}