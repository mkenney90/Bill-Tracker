package com.example.billtracker

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TableRow
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_scrolling.*
import okhttp3.OkHttpClient
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.net.URL

class ScrollingActivity : AppCompatActivity() {

    private var setPayStatus = false
    private val billsArray = ArrayList<TextView>()
    private val paidArray = ArrayList<Int>()
    private val colorPaid = Color.parseColor("#33A000")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(toolbar)

        val max = 14

        for (i in 2 until max) {
            val row = tableBills.getChildAt(i) as TableRow
            val x = (i - 1)
            row.tag = "${x}_bill"
            for (j in 0 until 5) {
                val tvnew = TextView(this)
                tvnew.setBackgroundColor(Color.GRAY)
                tvnew.textSize = 24f
                row.addView(tvnew)
                billsArray.add(tvnew)

                var id = "$x$j".toInt()
                tvnew.id = id

                val params = tvnew.layoutParams as TableRow.LayoutParams
                params.setMargins(3,2,2,2)
                tvnew.layoutParams = params

                tvnew.setOnClickListener{
                    if (setPayStatus) {
                        val paidId = it.id

                        for ((idx, v) in billsArray.withIndex()) {
                            if (v.id == paidId) {
                                if (paidArray[idx] == 1) {
                                    billsArray[idx].setBackgroundColor(Color.GRAY)
                                    paidArray[idx] = 0
                                } else {
                                    billsArray[idx].setBackgroundColor(Color.parseColor("#33A000"))
                                    paidArray[idx] = 1
                                }
                                break
                            }
                        }

                    }
                }
            }
        }

        getNetworkData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.action_setstatus -> {
                setPayStatus = true
                toolbar_layout.setBackgroundColor(Color.parseColor("#DA5800"))
            }
            R.id.action_finish -> {
                setPayStatus = false
                toolbar_layout.setBackgroundColor(Color.parseColor("#8A55DA"))
                writeNetworkData()
            }
            else -> super.onOptionsItemSelected(item)
        }

        return super.onOptionsItemSelected(item)

    }

    fun getNetworkData() : String {
        var data = "BLANK"

        doAsync {
            println("async")

            val response = URL("http://www.staminadamage.com/billtracker/bills.txt").readText()
            val lines = response.lines()

            for ((i, l) in lines.withIndex()) {
                val start = l.indexOf("{")
                var out = l.substring(start + 1, start + 10).replace(",","")
                var list = out.map { it.toString().toInt() }.toIntArray()

                for ((j, n) in list.withIndex()) {
                    if (n == 1) {
                        billsArray[i * 5 + j].setBackgroundColor(colorPaid)
                        paidArray.add(1)
                    } else {
                        paidArray.add(0)
                    }
                }
            }

            uiThread {
                toast("got network data")
            }
        }

        return data
    }

    private fun writeNetworkData() : String {

        println("write")
        var output = ""

        for (x in 1 until 12) {
            output = "$output={"
            for (y in 0 until 5) {
                output = "$output${paidArray[x*5+y]}"
                if (y < 4) {
                    output = "$output,"
                }
            }
            output = "$output}\n"
        }
        val json = "{\"data\":$output}"

        doAsync {
            sendPostRequest(json)
        }
        return output
    }

    val JSON = MediaType.parse("application/json; charset=utf-8")

    private fun sendPostRequest(data:String) : String? {

        val body = RequestBody.create(JSON, data)

        var client = OkHttpClient()
        var request = Request.Builder()
            .url("http://www.staminadamage.com/billtracker/update.php")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        return response.body()?.string()
    }
}
