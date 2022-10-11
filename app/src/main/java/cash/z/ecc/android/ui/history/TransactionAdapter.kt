package cash.z.ecc.android.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import cash.z.ecc.android.R
import cash.z.ecc.android.sdk.model.TransactionOverview

class TransactionAdapter :
    PagedListAdapter<TransactionOverview, TransactionViewHolder>(
        object : DiffUtil.ItemCallback<TransactionOverview>() {
            override fun areItemsTheSame(
                oldItem: TransactionOverview,
                newItem: TransactionOverview
            ) = oldItem.minedHeight == newItem.minedHeight && oldItem.rawId == newItem.rawId &&
                    // bugfix: distinguish between self-transactions so they don't overwrite each other in the UI // TODO confirm that this is working, as intended
                    oldItem.raw?.byteArray.contentEquals(newItem.raw?.byteArray)

            override fun areContentsTheSame(
                oldItem: TransactionOverview,
                newItem: TransactionOverview
            ) = oldItem == newItem
        }
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = TransactionViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }
}
