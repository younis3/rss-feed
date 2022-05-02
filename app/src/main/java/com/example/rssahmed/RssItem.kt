package com.example.rssahmed

class RssItem (){

    private var title: String? = null
    private var author: String? = null
    private var link: String? = null
    private var pubdate: String? = null

    constructor(title: String, author: String, link: String, pubdate: String): this(){
        this.title = title
        this.author = author
        this.link = link
        this.pubdate = pubdate
    }

    override fun toString(): String {
        return "$pubdate  |  $author \n $title"
    }


    fun getLink(): String?{
        return this.link
    }


}
