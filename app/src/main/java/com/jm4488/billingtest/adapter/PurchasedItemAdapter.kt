package com.jm4488.billingtest.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.Purchase
import com.jm4488.billingtest.databinding.ItemPurchasedProductBinding
import com.jm4488.billingtest.utils.GoogleBillingUtils
import kotlinx.android.synthetic.main.item_purchased_product.view.*

class PurchasedItemAdapter() : RecyclerView.Adapter<BillingItemViewHolder>() {
    var items = arrayListOf<Purchase>()
    lateinit var billingUtils: GoogleBillingUtils

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillingItemViewHolder {
        return BillingItemViewHolder.PurchasedViewHolder(ItemPurchasedProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: BillingItemViewHolder, position: Int) {
        val purchaseItem = items[position]
        holder.onBind(purchaseItem)
        holder.itemView.btn_confirm.visibility = if (billingUtils.isPurchasePending(purchaseItem)) View.VISIBLE else View.GONE
        holder.itemView.btn_confirm.setOnClickListener {
            if (billingUtils.isPurchasePending(purchaseItem)) {
                Log.e("[PURCHASEDADAP]", "btn_confirm / pending item : ${purchaseItem.sku}")
                billingUtils.doConsumeOrAcknowledgePurchaseItem(purchaseItem)
            } else {
                Log.e("[PURCHASEDADAP]", "btn_confirm / is not pending item")
            }
        }
    }

    fun setUtil(utils: GoogleBillingUtils) {
        billingUtils = utils
    }

    fun updateConsumedItem(item: Purchase) {
        items.remove(item)
        notifyDataSetChanged()
    }

    fun updateAcknowledgedItem(view: BillingItemViewHolder) {
        view.itemView.btn_confirm.visibility = View.GONE
    }

    fun getPosition(item: Purchase): Int {
        for (index in 0 until items.size) {
            if (items[index] == item) {
                return index
            }
        }
        return -1
    }
}