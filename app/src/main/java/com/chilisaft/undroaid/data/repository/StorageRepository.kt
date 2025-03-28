package com.chilisaft.undroaid.data.repository

interface StorageRepository {
    var apiToken: String?
    var serverUrl: String?
    var lastNotification: String?
    var theme: Int
    var showCoreList: Boolean
    val uuid: String
}