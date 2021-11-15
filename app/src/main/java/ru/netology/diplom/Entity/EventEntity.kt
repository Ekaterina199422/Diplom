package ru.netology.diplom.Entity

import androidx.annotation.Nullable
import androidx.room.*
import ru.netology.diplom.dao.InstantDateConverter
import ru.netology.diplom.dao.LongSetDataConverter
import ru.netology.diplom.dto.Event
import ru.netology.diplom.dto.EventType
import java.time.Instant

@Entity
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val authorId: Long,
    val author: String,
    @Nullable
    val authorAvatar: String?,
    val content: String,

    @TypeConverters(InstantDateConverter::class)
    val published: Instant,
    @TypeConverters(InstantDateConverter::class)
    val dateTime: Instant,

    @ColumnInfo(name = "event_type")
    val type: EventType,
    val isLikedByMe: Boolean,
    val likeCount: Int,
    val exhibitorByMe: Boolean,
    val exhibitorsCount: Int,
    @TypeConverters(LongSetDataConverter::class)
    val exhibitorsIds: Set<Long>,
    @Embedded
    val coords: CoordsEmbeddable?,
    @Embedded
    val attachment: MediaAttachmentEmbeddable?,
    val ownedByMe: Boolean = false,
) {

    fun toDto() = Event(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        datetime = dateTime,
        type = type,
        likedByMe = isLikedByMe,
        likeCount = likeCount,
        exhibitorsCount = exhibitorsCount,
        exhibitorByMe = exhibitorByMe,
        exhibitorsIds = exhibitorsIds,
        coords = coords?.toDto(),
        attachment = attachment?.toDto(),

        )

    companion object {
        fun fromDto(eventDto: Event) =
            EventEntity(
                id = eventDto.id,
                authorId = eventDto.authorId,
                author = eventDto.author,
                authorAvatar = eventDto.authorAvatar,
                content = eventDto.content,
                published = eventDto.published,
                dateTime = eventDto.datetime,
                type = eventDto.type,
                isLikedByMe = eventDto.likedByMe,
                likeCount = eventDto.likeCount,
                exhibitorByMe = eventDto.exhibitorByMe,
                exhibitorsCount = eventDto.exhibitorsCount,
                exhibitorsIds = eventDto.exhibitorsIds,
                coords = CoordsEmbeddable.fromDto(eventDto.coords),
                attachment = MediaAttachmentEmbeddable.fromDto(eventDto.attachment),
            )
    }

}

fun List<EventEntity>.toDto(): List<Event> = map(EventEntity::toDto)
fun List<Event>.toEntity(): List<EventEntity> = map(EventEntity::fromDto)