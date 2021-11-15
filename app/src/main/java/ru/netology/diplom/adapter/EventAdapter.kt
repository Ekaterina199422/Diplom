package ru.netology.diplom.adapter

import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.content.res.AppCompatResources
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import ru.netology.diplom.R
import ru.netology.diplom.databinding.EventListBinding
import ru.netology.diplom.dto.Event
import ru.netology.diplom.dto.EventType
import ru.netology.diplom.utils.AndroidUtils
import ru.netology.diplom.utils.loadCircleCrop
import ru.netology.diplom.utils.loadImage

interface OnEventButtonInteractionListener {
    fun onEventLike(event: Event)
    fun onEventEdit(event: Event)
    fun onEventRemove(event: Event)
    fun onEventExhibitor(event: Event)
    fun onAvatarClicked(event: Event)
    fun onLinkClicked(url: String)
    fun onSeeExhibitorsClicked(event: Event)
}

class EventAdapter(private val interactionListener: OnEventButtonInteractionListener) :
    PagingDataAdapter<Event, EventViewHolder>(EventDiffCallback) {

    companion object EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean =
            oldItem.id == newItem.id


        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val eventBinding =
            EventListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return EventViewHolder(eventBinding, interactionListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
    }

}


class EventViewHolder(
    private val eventBinding: EventListBinding,
    private val interactionListener: OnEventButtonInteractionListener
) :
    RecyclerView.ViewHolder(eventBinding.root) {


    fun bind(event: Event) {
        with(eventBinding) {
            tvUserName.text = event.author
            tvPublished.text =
                AndroidUtils.formatMillisToDateTimeString(event.published.toEpochMilli())
            tvContents.text = event.content
            BetterLinkMovementMethod.linkify(Linkify.WEB_URLS, tvContents)
                .setOnLinkClickListener { textView, url ->
                    interactionListener.onLinkClicked(url)
                    true
                }

            tvEventDueDate.text =
                AndroidUtils.formatMillisToDateTimeString(event.datetime.toEpochMilli())


            event.authorAvatar?.let {
                ivAvatar.loadCircleCrop(it)
            } ?: ivAvatar.setImageDrawable(
                AppCompatResources.getDrawable(
                    itemView.context,
                    R.drawable.ic_no_avatar_user
                )
            )

            buttonExhibitor.isChecked = event.exhibitorByMe
            buttonExhibitor.text = event.exhibitorsCount.toString()
            buttonExhibitor.setOnClickListener {
                interactionListener.onEventExhibitor(event)
            }


            tvActionSeeExhibitor.setOnClickListener {
                interactionListener.onSeeExhibitorsClicked(event)
            }

            if (event.exhibitorsIds.isEmpty()) tvActionSeeExhibitor.visibility = View.GONE
            else tvActionSeeExhibitor.visibility = View.VISIBLE

            event.attachment?.let {
                imageAttachments.loadImage(it.url)
            }
            if (event.attachment == null) {
                mediaCase.visibility = View.GONE
            } else {
                mediaCase.visibility = View.VISIBLE
            }

            buttonLike.isChecked = event.likedByMe
            buttonLike.text = event.likeCount.toString()
            buttonLike.setOnClickListener {
                interactionListener.onEventLike(event)
            }

            ivEventType.setBackgroundResource(
                when (event.type) {
                    EventType.OFFLINE -> R.drawable.ic_event_type_offline
                    EventType.ONLINE -> R.drawable.ic_event_type_online
                }
            )

            tvEventType.text = when (event.type) {
                EventType.OFFLINE -> itemView.context.getString(R.string.event_type_offline)
                EventType.ONLINE -> itemView.context.getString(R.string.event_type_online)
            }

            ivAvatar.setOnClickListener {
                interactionListener.onAvatarClicked(event)
            }



            if (!event.ownedByMe) {
               buttonEventsOptions.visibility = View.GONE
            } else {
                buttonEventsOptions.visibility = View.VISIBLE
                buttonEventsOptions.setOnClickListener {
                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.list_menu)
                        menu.setGroupVisible(R.id.list_item_modification, event.ownedByMe)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.action_delete -> {
                                    interactionListener.onEventRemove(event)
                                    true
                                }
                                R.id.action_edit -> {
                                    interactionListener.onEventEdit(event)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }
            }
        }
    }
}