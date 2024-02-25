package com.blogspot.kotlinstudy.lognote

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class RecentFileManager private constructor() {
    companion object {
        private const val RECENTES_LIST_FILE = "lognote_recents.xml"
        val LOGNOTE_HOME: String = System.getenv("LOGNOTE_HOME") ?: ""
        const val ITEM_VERSION = "RECENT_VERSION"

        const val ITEM_PATH = "_PATH"
        const val ITEM_SHOW_LOG = "_SHOW_LOG"
        const val ITEM_TOKEN_FILTER = "_TOKEN_FILTER_"
        const val ITEM_HIGHLIGHT_LOG = "_HIGHLIGHT_LOG"
        const val ITEM_SEARCH_LOG = "_SEARCH_LOG"
        const val ITEM_BOOKMARKS = "_BOOKMARKS"

        const val ITEM_SHOW_LOG_CHECK = "_SHOW_LOG_CHECK"
        const val ITEM_TOKEN_CHECK = "_TOKEN_CHECK_"
        const val ITEM_HIGHLIGHT_LOG_CHECK = "_HIGHLIGHT_LOG_CHECK"
        const val ITEM_SEARCH_MATCH_CASE = "_SEARCH_MATCH_CASE"

        const val MAX_RECENT_FILE = 30
        private val mInstance: RecentFileManager = RecentFileManager()

        fun getInstance(): RecentFileManager {
            return mInstance
        }
    }

    val mRecentList = mutableListOf<RecentItem>()
    val mOpenList = mutableListOf<OpenItem>()
    private val mProperties = Properties()
    private var mRecentListPath = RECENTES_LIST_FILE
    private val mFormatManager = FormatManager.getInstance()

    init {
        if (LOGNOTE_HOME.isNotEmpty()) {
            mRecentListPath = "$LOGNOTE_HOME${File.separator}$RECENTES_LIST_FILE"
        }
        println("Recent list : $mRecentListPath")
        manageVersion()
        loadList()
    }

    class RecentItem() {
        var mPath = ""
        var mShowLog = ""
        var mTokenFilter = Array(FormatManager.MAX_TOKEN_COUNT) { "" }
        var mHighlightLog = ""
        var mSearchLog = ""
        var mBookmarks = ""

        var mShowLogCheck = true
        var mTokenCheck = Array(FormatManager.MAX_TOKEN_COUNT) { true }
        var mHighlightLogCheck = true
        var mSearchMatchCase = true
    }

    data class OpenItem(val mPath: String, var mStartLine: Int, var mEndLine: Int)

    private fun loadXml(): Boolean {
        var ret = true
        var fileInput: FileInputStream? = null

        try {
            fileInput = FileInputStream(mRecentListPath)
            mProperties.loadFromXML(fileInput)
        } catch (ex: Exception) {
            ex.printStackTrace()
            ret = false
        } finally {
            if (null != fileInput) {
                try {
                    fileInput.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }

        return ret
    }

    private fun saveXml(): Boolean {
        var ret = true
        var fileOutput: FileOutputStream? = null
        try {
            fileOutput = FileOutputStream(mRecentListPath)
            mProperties.storeToXML(fileOutput, "")
        } catch (ex: Exception) {
            ex.printStackTrace()
            ret = false
        } finally {
            if (null != fileOutput) {
                try {
                    fileOutput.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
        return ret
    }

    fun loadList() {
        loadXml()

        val formatName = mFormatManager.mCurrFormat.mName
        val tokens = mFormatManager.mCurrFormat.mTokens

        mRecentList.clear()
        for (i in 0 until MAX_RECENT_FILE) {
            val recentItem = RecentItem()
            recentItem.mPath = (mProperties["$i$ITEM_PATH"] ?: "") as String
            if (recentItem.mPath.isEmpty()) {
                break
            }
            recentItem.mShowLog = (mProperties["$i$ITEM_SHOW_LOG"] ?: "") as String
            for (idx in 0 until FormatManager.MAX_TOKEN_COUNT) {
                if (tokens[idx].mToken.isEmpty()) {
                    recentItem.mTokenFilter[idx] = ""
                }
                else {
                    recentItem.mTokenFilter[idx] =
                        (mProperties["$i$ITEM_TOKEN_FILTER${formatName}_${tokens[idx].mToken}"] ?: "") as String
                }
            }
            recentItem.mHighlightLog = (mProperties["$i$ITEM_HIGHLIGHT_LOG"] ?: "") as String
            recentItem.mSearchLog = (mProperties["$i$ITEM_SEARCH_LOG"] ?: "") as String
            recentItem.mBookmarks = (mProperties["$i$ITEM_BOOKMARKS"] ?: "") as String

            var check = (mProperties["$i$ITEM_SHOW_LOG_CHECK"] ?: "false") as String
            recentItem.mShowLogCheck = check.toBoolean()
            for (idx in 0 until FormatManager.MAX_TOKEN_COUNT) {
                if (tokens[idx].mToken.isEmpty()) {
                    recentItem.mTokenCheck[idx] = false
                }
                else {
                    check = (mProperties["$i$ITEM_TOKEN_CHECK${formatName}_${tokens[idx].mToken}"] ?: "false") as String
                    recentItem.mTokenCheck[idx] = check.toBoolean()
                }
            }
            check = (mProperties["$i$ITEM_HIGHLIGHT_LOG_CHECK"] ?: "false") as String
            recentItem.mHighlightLogCheck = check.toBoolean()
            check = (mProperties["$i$ITEM_SEARCH_MATCH_CASE"] ?: "false") as String
            recentItem.mSearchMatchCase = check.toBoolean()

            mRecentList.add(recentItem)
        }
    }

    fun saveList() {
        val formatName = mFormatManager.mCurrFormat.mName
        val tokens = mFormatManager.mCurrFormat.mTokens
        for (i in 0 until MAX_RECENT_FILE) {
            mProperties.remove("$i$ITEM_PATH")
            mProperties.remove("$i$ITEM_SHOW_LOG")
            for (idx in 0 until FormatManager.MAX_TOKEN_COUNT) {
                mProperties.remove("$i$ITEM_TOKEN_FILTER${formatName}_${tokens[idx].mToken}")
            }
            mProperties.remove("$i$ITEM_HIGHLIGHT_LOG")
            mProperties.remove("$i$ITEM_SEARCH_LOG")
            mProperties.remove("$i$ITEM_BOOKMARKS")

            mProperties.remove("$i$ITEM_SHOW_LOG_CHECK")
            for (idx in 0 until FormatManager.MAX_TOKEN_COUNT) {
                mProperties.remove("$i$ITEM_TOKEN_CHECK${formatName}_${tokens[idx].mToken}")
            }
            mProperties.remove("$i$ITEM_HIGHLIGHT_LOG_CHECK")
            mProperties.remove("$i$ITEM_SEARCH_MATCH_CASE")
        }

        val mSaveList = mutableListOf<String>()
        for (i in 0 until MAX_RECENT_FILE) {
            if (i >= mRecentList.size) {
                break
            }
            val recentItem = mRecentList[i]
            if (!mSaveList.contains(recentItem.mPath)) {
                mSaveList.add(recentItem.mPath)
                mProperties["$i$ITEM_PATH"] = recentItem.mPath
                mProperties["$i$ITEM_SHOW_LOG"] = recentItem.mShowLog
                for (idx in 0 until FormatManager.MAX_TOKEN_COUNT) {
                    if (tokens[idx].mToken.isNotEmpty()) {
                        mProperties["$i$ITEM_TOKEN_FILTER${formatName}_${tokens[idx].mToken}"] =
                            recentItem.mTokenFilter[idx]
                    }
                }
                mProperties["$i$ITEM_HIGHLIGHT_LOG"] = recentItem.mHighlightLog
                mProperties["$i$ITEM_SEARCH_LOG"] = recentItem.mSearchLog
                mProperties["$i$ITEM_BOOKMARKS"] = recentItem.mBookmarks

                mProperties["$i$ITEM_SHOW_LOG_CHECK"] = recentItem.mShowLogCheck.toString()
                for (idx in 0 until FormatManager.MAX_TOKEN_COUNT) {
                    if (tokens[idx].mToken.isNotEmpty()) {
                        mProperties["$i$ITEM_TOKEN_CHECK${formatName}_${tokens[idx].mToken}"] =
                            recentItem.mTokenCheck[idx].toString()
                    }
                }
                mProperties["$i$ITEM_HIGHLIGHT_LOG_CHECK"] = recentItem.mHighlightLogCheck.toString()
                mProperties["$i$ITEM_SEARCH_MATCH_CASE"] = recentItem.mSearchMatchCase.toString()
            }
        }

        saveXml()
    }

    fun saveItem(key: String, value: String) {
        loadList()
        setItem(key, value)
        saveList()
    }

    fun saveItems(keys: Array<String>, values: Array<String>) {
        loadList()
        setItems(keys, values)
        saveList()
    }

    fun getItem(key: String): String? {
        return mProperties[key] as String?
    }

    fun setItem(key: String, value: String) {
        mProperties[key] = value
    }

    private fun setItems(keys: Array<String>, values: Array<String>) {
        if (keys.size != values.size) {
            println("saveItem : size not match ${keys.size}, ${values.size}")
            return
        }
        for (idx in keys.indices) {
            mProperties[keys[idx]] = values[idx]
        }
    }

    fun removeItem(key: String) {
        mProperties.remove(key)
    }

    fun addOpenFile(openItem: OpenItem) {
        mOpenList.add(openItem)
    }

    private fun manageVersion() {
        val isLoaded = loadXml()

        if (isLoaded) {
            var confVer: String = (mProperties[ITEM_VERSION] ?: "") as String
            if (confVer.isEmpty()) {
                updateRecentFileFromV0ToV1()
                confVer = (mProperties[ITEM_VERSION] ?: "") as String
                println("RecentFileManager : manageVersion : $confVer applied")
            }

//            if (confVer == "1") {
//                updateConfigFromV1ToV2()
//                confVer = (mProperties[ITEM_VERSION] ?: "") as String
//                println("RecentFileManager : manageVersion : $confVer applied")
//            }
        }
        else {
            mProperties[ITEM_VERSION] = "1"
        }

        saveXml()
    }

    private fun updateRecentFileFromV0ToV1() {
        println("updateRecentFileFromV0ToV1 : tag,pid,tid to token properties ++")
        val formatName = mFormatManager.mCurrFormat.mName
        val tokens = mFormatManager.mCurrFormat.mTokens

        val itemShowTag = "_SHOW_TAG"
        val itemShowPid = "_SHOW_PID"
        val itemShowTid = "_SHOW_TID"

        val itemShowTagCheck = "_SHOW_TAG_CHECK"
        val itemShowPidCheck = "_SHOW_PID_CHECK"
        val itemShowTidCheck = "_SHOW_TID_CHECK"

        for (i in 0 until MAX_RECENT_FILE) {
            if (mProperties["$i$ITEM_PATH"] == null) {
                break
            }

            mProperties["$i$itemShowTag"]?.let { mProperties["$i$ITEM_TOKEN_FILTER${formatName}_${tokens[0].mToken}"] = it }
            mProperties["$i$itemShowPid"]?.let { mProperties["$i$ITEM_TOKEN_FILTER${formatName}_${tokens[1].mToken}"] = it }
            mProperties["$i$itemShowTid"]?.let { mProperties["$i$ITEM_TOKEN_FILTER${formatName}_${tokens[2].mToken}"] = it }

            mProperties["$i$itemShowTagCheck"]?.let { mProperties["$i$ITEM_TOKEN_CHECK${formatName}_${tokens[0].mToken}"] = it }
            mProperties["$i$itemShowPidCheck"]?.let { mProperties["$i$ITEM_TOKEN_CHECK${formatName}_${tokens[1].mToken}"] = it }
            mProperties["$i$itemShowTidCheck"]?.let { mProperties["$i$ITEM_TOKEN_CHECK${formatName}_${tokens[2].mToken}"] = it }

            mProperties.remove("$i$itemShowTag")
            mProperties.remove("$i$itemShowPid")
            mProperties.remove("$i$itemShowTid")

            mProperties.remove("$i$itemShowTagCheck")
            mProperties.remove("$i$itemShowPidCheck")
            mProperties.remove("$i$itemShowTidCheck")
        }

        mProperties[ITEM_VERSION] = "1"
        println("updateRecentFileFromV0ToV1 : --")
    }
}

