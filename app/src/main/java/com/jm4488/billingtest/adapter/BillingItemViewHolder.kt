package com.jm4488.billingtest.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.jm4488.billingtest.databinding.ItemBillingProductBinding
import com.jm4488.billingtest.databinding.ItemPurchasedProductBinding

open class BillingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // product item
    open fun onBind(data: SkuDetails) {}
    class ProductViewHolder(private val binding: ItemBillingProductBinding) : BillingItemViewHolder(binding.root) {
        override fun onBind(data: SkuDetails) {
            binding.data = data
        }
    }

    // already purchased item
    open fun onBind(data: Purchase) {}
    class PurchasedViewHolder(private val binding: ItemPurchasedProductBinding) : BillingItemViewHolder(binding.root) {
        override fun onBind(data: Purchase) {
            binding.data = data
        }
    }
}