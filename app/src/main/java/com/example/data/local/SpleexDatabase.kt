package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.R
import com.example.data.media.MediaStorageManager
import com.example.data.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        LikeEntity::class,
        SavedPostEntity::class,
        PollVoteEntity::class,
        CommentEntity::class,
        FollowEntity::class,
        BlockEntity::class,
        StoryEntity::class,
        StoryViewEntity::class,
        ReelEntity::class,
        ReelLikeEntity::class,
        ConversationEntity::class,
        ConversationMemberEntity::class,
        MessageEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpleexDatabase : RoomDatabase() {
    abstract fun spleexDao(): SpleexDao

    companion object {
        @Volatile
        private var INSTANCE: SpleexDatabase? = null

        fun getInstance(context: Context): SpleexDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpleexDatabase::class.java,
                    "spleex_social.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object SpleexSeedInitializer {
    suspend fun ensureSeeded(db: SpleexDatabase, media: MediaStorageManager) =
        withContext(Dispatchers.IO) {
            val dao = db.spleexDao()
            if (dao.countUsers() > 0) return@withContext

            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val day = 24L * hour

            val neonImg = media.drawableUri(R.drawable.img_feed_neon_street)
            val deskImg = media.drawableUri(R.drawable.img_feed_creative_desk)
            val rooftopImg = media.drawableUri(R.drawable.img_feed_rooftop_skyline)
            val bannerImg = media.drawableUri(R.drawable.img_auth_banner)

            val demoPassHash = PasswordHasher.hashPassword("Spleex@2026")

            val users = listOf(
                UserEntity(
                    id = "user_aria",
                    fullName = "Aria Vance",
                    username = "aria_vibe",
                    email = "aria@spleex.social",
                    passwordHash = demoPassHash,
                    bio = "Visual director & midnight street photographer. Building aesthetics in pink & obsidian.",
                    website = "spleex.social/aria",
                    avatarUri = rooftopImg,
                    coverUri = bannerImg,
                    accentHex = "#FF75A0",
                    frameStyle = "NEON_PINK",
                    badgeLabel = "Founder Circle",
                    isVerified = true,
                    isOnline = true,
                    createdAt = now - 10 * day
                ),
                UserEntity(
                    id = "user_kai",
                    fullName = "Kai Takahashi",
                    username = "kai_frames",
                    email = "kai@spleex.social",
                    passwordHash = demoPassHash,
                    bio = "35mm film, rainy neon alleys & ambient synth setups.",
                    website = "kaiframes.studio",
                    avatarUri = neonImg,
                    coverUri = neonImg,
                    accentHex = "#C8B6FF",
                    frameStyle = "VIOLET_AURA",
                    badgeLabel = "Top Creator",
                    isVerified = true,
                    isOnline = true,
                    createdAt = now - 8 * day
                ),
                UserEntity(
                    id = "user_nora",
                    fullName = "Nora Lin",
                    username = "nora_studio",
                    email = "nora@spleex.social",
                    passwordHash = demoPassHash,
                    bio = "Minimalist desk setups, product photography & late-night matcha.",
                    website = "nora.design",
                    avatarUri = deskImg,
                    coverUri = deskImg,
                    accentHex = "#FFB3C6",
                    frameStyle = "SOFT_ROSE",
                    badgeLabel = "Design Pro",
                    isVerified = false,
                    isOnline = true,
                    createdAt = now - 6 * day
                ),
                UserEntity(
                    id = "user_zane",
                    fullName = "Zane Mercer",
                    username = "zane_beats",
                    email = "zane@spleex.social",
                    passwordHash = demoPassHash,
                    bio = "Rooftop sessions & sound design. Always down for collabs.",
                    website = "zanebeats.fm",
                    avatarUri = bannerImg,
                    coverUri = rooftopImg,
                    accentHex = "#FF75A0",
                    frameStyle = "GOLD_SPARK",
                    badgeLabel = "Sound Maker",
                    isVerified = true,
                    isOnline = false,
                    createdAt = now - 5 * day
                )
            )
            dao.insertUsers(users)

            val posts = listOf(
                PostEntity(
                    id = "post_1",
                    authorId = "user_kai",
                    caption = "Rain-washed asphalt turns every street corner into a cinema frame. Shot on 35mm after midnight.",
                    hashtags = "#nightphotography #neon #spleex #35mm",
                    mediaUrisSerialized = "$neonImg|$rooftopImg",
                    postType = "CAROUSEL",
                    createdAt = now - 2 * hour
                ),
                PostEntity(
                    id = "post_2",
                    authorId = "user_nora",
                    caption = "Dialed in the pink rim light on the desk setup tonight. Film camera, espresso, zero distractions.",
                    hashtags = "#desksetup #minimal #creatorlife",
                    mediaUrisSerialized = deskImg,
                    postType = "PHOTO",
                    createdAt = now - 5 * hour
                ),
                PostEntity(
                    id = "post_3",
                    authorId = "user_aria",
                    caption = "Golden-rose twilight from the rooftop lounge. Which vibe should we drop for the weekend meetup?",
                    hashtags = "#rooftop #twilight #community",
                    mediaUrisSerialized = rooftopImg,
                    postType = "POLL",
                    pollQuestion = "Weekend SPLEEX creator meetup theme?",
                    pollOptionsSerialized = "Rooftop Golden Hour|Neon Night Photo Walk|Lo-Fi Studio Jam",
                    createdAt = now - 9 * hour
                ),
                PostEntity(
                    id = "post_4",
                    authorId = "user_zane",
                    caption = "Sometimes the quietest late-night sketches become the tracks you replay all year. Keep shipping your art even when nobody is watching yet.",
                    hashtags = "#confession #creativity #midnight",
                    mediaUrisSerialized = "",
                    postType = "CONFESSION",
                    isAnonymousConfession = true,
                    createdAt = now - 14 * hour
                )
            )
            dao.insertPosts(posts)

            dao.insertComments(
                listOf(
                    CommentEntity(
                        id = "c_1",
                        postId = "post_1",
                        authorId = "user_aria",
                        text = "The reflections in the first frame are unreal.",
                        createdAt = now - 90 * 60_000L
                    ),
                    CommentEntity(
                        id = "c_2",
                        postId = "post_1",
                        authorId = "user_nora",
                        text = "Need the lens details ASAP!",
                        createdAt = now - 45 * 60_000L
                    ),
                    CommentEntity(
                        id = "c_3",
                        postId = "post_2",
                        authorId = "user_kai",
                        text = "That soft pink ambient glow hits different.",
                        createdAt = now - 3 * hour
                    )
                )
            )

            dao.insertFollows(
                listOf(
                    FollowEntity("user_aria", "user_kai", "ACCEPTED", now - 4 * day),
                    FollowEntity("user_aria", "user_nora", "ACCEPTED", now - 3 * day),
                    FollowEntity("user_kai", "user_aria", "ACCEPTED", now - 3 * day),
                    FollowEntity("user_nora", "user_aria", "ACCEPTED", now - 2 * day),
                    FollowEntity("user_zane", "user_aria", "ACCEPTED", now - 1 * day)
                )
            )

            dao.insertLike(LikeEntity("post_1", "user_aria", now - hour))
            dao.insertLike(LikeEntity("post_1", "user_nora", now - hour))
            dao.insertLike(LikeEntity("post_2", "user_aria", now - 2 * hour))
            dao.insertPollVote(PollVoteEntity("post_3", "user_kai", 1, now - 4 * hour))
            dao.insertPollVote(PollVoteEntity("post_3", "user_nora", 0, now - 3 * hour))

            dao.insertStories(
                listOf(
                    StoryEntity(
                        id = "story_kai_1",
                        authorId = "user_kai",
                        mediaUri = neonImg,
                        caption = "Midnight rain walk in Shibuya",
                        isCloseFriends = false,
                        createdAt = now - 2 * hour,
                        expiresAt = now + 22 * hour
                    ),
                    StoryEntity(
                        id = "story_nora_1",
                        authorId = "user_nora",
                        mediaUri = deskImg,
                        caption = "Editing tonight's reel",
                        isCloseFriends = true,
                        createdAt = now - 4 * hour,
                        expiresAt = now + 20 * hour
                    ),
                    StoryEntity(
                        id = "story_aria_1",
                        authorId = "user_aria",
                        mediaUri = rooftopImg,
                        caption = "Pink clouds above the skyline",
                        isCloseFriends = false,
                        createdAt = now - 5 * hour,
                        expiresAt = now + 19 * hour
                    )
                )
            )

            dao.insertReels(
                listOf(
                    ReelEntity(
                        id = "reel_1",
                        authorId = "user_kai",
                        mediaUri = neonImg,
                        title = "Neon Rain Reflections",
                        caption = "Chasing color reflections after a storm.",
                        hashtags = "#reels #cinematic #neon",
                        durationLabel = "0:24",
                        viewsCount = 2480L,
                        createdAt = now - 3 * hour
                    ),
                    ReelEntity(
                        id = "reel_2",
                        authorId = "user_nora",
                        mediaUri = deskImg,
                        title = "Desk Glow Upgrade",
                        caption = "60 seconds to transform your night workflow.",
                        hashtags = "#setup #aesthetic #spleex",
                        durationLabel = "0:19",
                        viewsCount = 1840L,
                        createdAt = now - 7 * hour
                    ),
                    ReelEntity(
                        id = "reel_3",
                        authorId = "user_aria",
                        mediaUri = rooftopImg,
                        title = "Twilight Skyline",
                        caption = "Golden rose hour from 32 floors up.",
                        hashtags = "#skyline #pinkhour #travel",
                        durationLabel = "0:15",
                        viewsCount = 3190L,
                        createdAt = now - 12 * hour
                    )
                )
            )

            dao.insertConversation(
                ConversationEntity(
                    id = "conv_aria_kai",
                    title = "",
                    isGroup = false,
                    lastMessageText = "Just dropped the new 35mm shots on SPLEEX!",
                    lastMessageAt = now - 35 * 60_000L
                )
            )
            dao.insertConversationMembers(
                listOf(
                    ConversationMemberEntity("conv_aria_kai", "user_aria"),
                    ConversationMemberEntity("conv_aria_kai", "user_kai")
                )
            )
            dao.insertMessages(
                listOf(
                    MessageEntity(
                        id = "msg_1",
                        conversationId = "conv_aria_kai",
                        senderId = "user_aria",
                        text = "Hey Kai! How did the night shoot turn out?",
                        isRead = true,
                        createdAt = now - 50 * 60_000L
                    ),
                    MessageEntity(
                        id = "msg_2",
                        conversationId = "conv_aria_kai",
                        senderId = "user_kai",
                        text = "Just dropped the new 35mm shots on SPLEEX!",
                        imageUri = neonImg,
                        reactionEmoji = "🔥",
                        isRead = false,
                        createdAt = now - 35 * 60_000L
                    )
                )
            )

            dao.insertNotifications(
                listOf(
                    NotificationEntity(
                        id = "notif_1",
                        recipientId = "user_aria",
                        actorId = "user_kai",
                        type = "COMMENT",
                        targetId = "post_1",
                        message = "tagged you in a midnight street frame",
                        isRead = false,
                        createdAt = now - 40 * 60_000L
                    ),
                    NotificationEntity(
                        id = "notif_2",
                        recipientId = "user_aria",
                        actorId = "user_nora",
                        type = "FOLLOW",
                        targetId = "user_nora",
                        message = "started following your SPLEEX profile",
                        isRead = false,
                        createdAt = now - 2 * hour
                    )
                )
            )
        }
}
