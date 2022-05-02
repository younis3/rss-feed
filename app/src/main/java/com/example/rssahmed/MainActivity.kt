package com.example.rssahmed

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*

import kotlinx.coroutines.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList


import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import javax.xml.parsers.DocumentBuilderFactory




class MainActivity : AppCompatActivity() {

    val BOIURL: String by lazy { getString(R.string.BOIURL) }

    val progrsBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
    val lvItems: ListView by lazy { findViewById(R.id.lvItems) }

    val tvTop: TextView by lazy { findViewById(R.id.textView) }
    val tvSec: TextView by lazy { findViewById(R.id.textView2) }
    val pbCycle: ProgressBar by lazy { findViewById(R.id.progressBarCycle) }


    val ItemsList = ArrayList<RssItem>()
    val adptr: ArrayAdapter<RssItem> by lazy { ArrayAdapter(this, android.R.layout.simple_list_item_1, ItemsList) }




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progrsBar.max = 100

        if (!isInternetCurrentlyAllowedByUser()) {
            tvTop.text = getString(R.string.conError)
            lvItems.visibility = View.INVISIBLE
            progrsBar.visibility = View.INVISIBLE
            pbCycle.visibility = View.INVISIBLE
        }
        else{
            var finishStatus:String
            MainScope().launch {
                val res = async(Dispatchers.Main) { getItemsBg() }
                finishStatus=res.await()// we can use the result only when finished
                if (finishStatus!="Ok")// this line must write in launce block as the next command in onCreate continue to execute in parallel
                    Toast.makeText(baseContext,"this Error occurred while coroutine executed: ${finishStatus}",Toast.LENGTH_LONG).show()
            }//launch
        }

        lvItems.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long ) {
                if (lvItems.choiceMode != ListView.CHOICE_MODE_MULTIPLE) {//show the Toast only when the listview is in  single selection mode

                    //val itemValue = "" + lvItems.getItemAtPosition(position)//this automatically execute the toString method!
                    //Toast.makeText(baseContext,"Position:$position\nListItem: $itemValue",Toast.LENGTH_LONG).show();

                    var openURL = Intent(android.content.Intent.ACTION_VIEW)
                    //get rssItem by position in array list
                    val itemSelected: RssItem = ItemsList.elementAt(position)
                    val urlString : String? = itemSelected.getLink()
                    openURL.data = Uri.parse(urlString)
                    //Toast.makeText(baseContext,urlString ,Toast.LENGTH_LONG).show();
                    startActivity(openURL)
                }//if
            }//onItemClick
        })//OnItemClickListener

    }


    fun isInternetCurrentlyAllowedByUser(): Boolean {
        val cnctvtyMngr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetWork = cnctvtyMngr.activeNetwork
        val networkCapabilities = cnctvtyMngr.getNetworkCapabilities(activeNetWork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } //isInternetCurrentlyAllowedByUser


    suspend fun getItemsBg(): String { // all this fun is in execute in parallel at main thread except the network part

        fun openHttpConnection(strUrl: String?): InputStream? {
            var inStrm: InputStream? = null
            val srcURL: URL
            val connection: URLConnection
            var errDsc: String = "Bad url structure"
            try {
                srcURL = URL(strUrl)
                errDsc = "Failing while creating the URL or HttpURL connection object"
                connection = srcURL.openConnection()
                val httpcnctn: HttpURLConnection = connection as HttpURLConnection
                httpcnctn.allowUserInteraction = false
                httpcnctn.instanceFollowRedirects = true
                httpcnctn.requestMethod = "GET"
                errDsc = "Failing while Establishing the Connection"
                httpcnctn.connect()
                val rqstRC = httpcnctn.responseCode
                if (rqstRC == HttpURLConnection.HTTP_OK)
                    inStrm = httpcnctn.inputStream
            }
            catch (prblm: Exception) {
                Log.d("openHttpConnection=====", errDsc)
            }
            return inStrm// it will return null if problem occurred
        } //OpenHttpConnection


        suspend fun parseingXml(siteUrl: String?): String {
            var xmldoc: Document
            var articleItems: NodeList
            var rc:String
            try {
                progrsBar.progress = 0
                withContext(Dispatchers.IO) {//this context if for io, db and net access operations
                    val dataSource: InputStream? = openHttpConnection(siteUrl)//open stream to BOI site
                    xmldoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dataSource?: throw Exception("Open fail see logcat"))//get from boi  site the xml content using input stream
                    articleItems = xmldoc.getElementsByTagName("item") // get all  CURRENCY elements
                }
                progrsBar.progress = 10
                val progressCounter: Int = 90 / articleItems.length    // 90/15
                for (i in 0 until articleItems.length) {
                    val tnode: Node = articleItems.item(i)
                    val telement: Element = tnode as Element

                    val rssTitle: String = telement.getElementsByTagName("title").item(0).getTextContent()
                    val rssLink: String = telement.getElementsByTagName("link").item(0).getTextContent()
                    val rssAuthor: String = telement.getElementsByTagName("author").item(0).getTextContent()
                    val rssPubDate: String = telement.getElementsByTagName("pubDate").item(0).getTextContent()

                    tvSec.text = rssTitle

                    val rssItem: RssItem = RssItem(rssTitle, rssAuthor, rssLink, rssPubDate)
                    ItemsList.add(rssItem)

                    progrsBar.progress +=  progressCounter

                    delay(600)
                }
                rc="Ok"
            }//try
            catch (prblm: Exception) {
                rc=prblm.message!!
            }
            return rc;
        } //parseingXml


        val result = parseingXml(BOIURL)
        progrsBar.visibility = View.INVISIBLE
        tvTop.visibility = View.INVISIBLE
        tvSec.visibility = View.INVISIBLE
        pbCycle.visibility = View.INVISIBLE


        if (result == "Ok") {
            lvItems.setAdapter(adptr) // now as the list if full with data attach it to list view
            lvItems.visibility = View.VISIBLE // allow viewing  the list view
        } else {
            tvTop.text = "Can\'t Downloading Or Parsing the Xml Data\n from Globes Site"
        }

        return result
    }//doInBg


}
