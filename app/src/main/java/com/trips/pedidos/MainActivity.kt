package com.trips.pedidos

import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import java.text.NumberFormat
import java.util.*

data class Product(val name:String,val price:Int,val emoji:String)
data class Line(val p:Product,var qty:Int)

class MainActivity: android.app.Activity() {
    private val products = listOf(
        Product("Brownie",12000,"🍫"), Product("Cookie",12000,"🍪"),
        Product("Trufa",5000,"🍬"), Product("Los Malditos",20000,"💀"),
        Product("6 Trufas",24000,"📦"), Product("12 Trufas",42000,"📦"),
        Product("Combo 2+2+2",48000,"🎁"), Product("Combo 3+3+4",78000,"🎁")
    )
    private val cart = mutableListOf<Line>()
    private lateinit var cartBox: LinearLayout
    private lateinit var totalText: TextView
    private lateinit var client: EditText
    private lateinit var payment: Spinner
    private var orderNo:Int = 1

    override fun onCreate(b:Bundle?) {
        super.onCreate(b)
        orderNo = getPreferences(0).getInt("order",1)
        showOrder()
    }

    private fun money(n:Int) = NumberFormat.getNumberInstance(Locale("es","AR")).format(n).let{"$$it"}

    private fun tv(s:String,size:Float=16f,bold:Boolean=false):TextView =
        TextView(this).apply { text=s; textSize=size; setTextColor(Color.rgb(20,20,20)); if(bold) setTypeface(null,Typeface.BOLD); setPadding(10,8,10,8) }

    private fun button(s:String,on:()->Unit):Button =
        Button(this).apply { text=s; textSize=14f; setOnClickListener{on()} }

    private fun showOrder() {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(245,245,245))}
        val head=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(18,14,18,14);setBackgroundColor(Color.rgb(17,17,17))}
        head.addView(tv("TRIPS",25f,true).apply{setTextColor(Color.WHITE);letterSpacing=.15f},LinearLayout.LayoutParams(0,-2,1f))
        head.addView(tv("PEDIDO #"+"%03d".format(orderNo),15f,true).apply{setTextColor(Color.WHITE)})
        root.addView(head)

        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,10,12,20)}
        body.addView(tv("DATOS DEL CLIENTE",18f,true))
        client=EditText(this).apply{hint="Nombre (opcional)";textSize=16f}
        body.addView(client)
        payment=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Mercado Pago","Efectivo","Transferencia","Otro"))}
        body.addView(payment)

        body.addView(tv("PRODUCTOS",18f,true).apply{setPadding(10,18,10,8)})
        val grid=GridLayout(this).apply{columnCount=2;setPadding(2,2,2,2)}
        products.forEachIndexed { i,p ->
            val b=Button(this).apply{text="${p.emoji} ${p.name}\n${money(p.price)}";textSize=15f;setOnClickListener{add(i)}}
            grid.addView(b,GridLayout.LayoutParams().apply{width=0;height=125;columnSpec=GridLayout.spec(i%2,1f);setMargins(5,5,5,5)})
        }
        body.addView(grid)
        body.addView(tv("PEDIDO",18f,true).apply{setPadding(10,18,10,8)})
        cartBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        body.addView(cartBox)
        totalText=tv("TOTAL  $0",23f,true).apply{gravity=Gravity.END}
        body.addView(totalText)
        val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        actions.addView(button("VACIAR"){cart.clear();renderCart()},LinearLayout.LayoutParams(0,-2,1f))
        actions.addView(button("CONFIRMAR PEDIDO"){confirm()},LinearLayout.LayoutParams(0,-2,1f))
        body.addView(actions)
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root);renderCart()
    }

    private fun add(i:Int){val x=cart.find{it.p==products[i]};if(x==null)cart.add(Line(products[i],1))else x.qty++;renderCart()}
    private fun renderCart(){
        cartBox.removeAllViews();var total=0
        if(cart.isEmpty()) cartBox.addView(tv("Todavía no agregaste productos.",14f))
        cart.forEachIndexed{idx,l->
            total+=l.p.price*l.qty
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            row.addView(tv("${l.qty} × ${l.p.name}\n${money(l.p.price*l.qty)}",15f,true),LinearLayout.LayoutParams(0,-2,1f))
            row.addView(button("−"){l.qty--;if(l.qty<=0)cart.removeAt(idx);renderCart()})
            row.addView(tv("${l.qty}",16f,true))
            row.addView(button("+"){l.qty++;renderCart()})
            cartBox.addView(row)
        }
        totalText.text="TOTAL  ${money(total)}"
    }

    private fun confirm(){
        if(cart.isEmpty()){Toast.makeText(this,"Agregá al menos un producto.",Toast.LENGTH_SHORT).show();return}
        val total=cart.sumOf{it.p.price*it.qty}
        val lines=cart.joinToString("\n"){ "${it.qty} x ${it.p.name}    ${money(it.p.price*it.qty)}" }
        val name=client.text.toString().trim()
        val pay=payment.selectedItem.toString()
        val ticket="""TRIPS
PEDIDO #${"%03d".format(orderNo)}
${if(name.isNotEmpty())"$name\n" else ""}--------------------------
$lines
--------------------------
TOTAL: ${money(total)}
PAGO: $pay

¡GRACIAS! ♡"""
        saveOrder(ticket)
        shareTicket(ticket)
        orderNo++;getPreferences(0).edit().putInt("order",orderNo).apply()
        cart.clear();renderCart()
    }

    private fun saveOrder(ticket:String){getPreferences(0).edit().putString("last_ticket",ticket).apply()}

    private fun shareTicket(text:String){
        val i=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text);putExtra(Intent.EXTRA_TITLE,"TRIPS — Ticket")}
        startActivity(Intent.createChooser(i,"Enviar ticket con…"))
    }
}
