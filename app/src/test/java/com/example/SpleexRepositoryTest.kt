package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SpleexDatabase
import com.example.data.media.MediaStorageManager
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.SpleexRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpleexRepositoryTest {

    private lateinit var db: SpleexDatabase
    private lateinit var repo: SpleexRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpleexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SpleexRepository(
            dao = db.spleexDao(),
            mediaStorage = MediaStorageManager(context),
            sessionPrefs = SessionPreferences(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `register validates unique username and hashes password`() = runBlocking {
        val first = repo.register(
            fullName = "Sora Ray",
            username = "sora_ray",
            email = "sora@spleex.social",
            password = "password123",
            confirmPassword = "password123",
            avatarUri = null
        )
        assertTrue(first.isSuccess)
        val user = first.getOrThrow()
        assertNotEquals("password123", user.passwordHash)

        val duplicateUsername = repo.register(
            fullName = "Another Sora",
            username = "sora_ray",
            email = "sora2@spleex.social",
            password = "password123",
            confirmPassword = "password123",
            avatarUri = null
        )
        assertTrue(duplicateUsername.isFailure)

        val loginOk = repo.login("sora_ray", "password123")
        assertTrue(loginOk.isSuccess)
    }

    @Test
    fun `create post like comment and follow update reactive feed`() = runBlocking {
        val alice = repo.register(
            fullName = "Alice",
            username = "alice_test",
            email = "alice@spleex.social",
            password = "secret12",
            confirmPassword = "secret12",
            avatarUri = null
        ).getOrThrow()

        val bob = repo.register(
            fullName = "Bob",
            username = "bob_test",
            email = "bob@spleex.social",
            password = "secret12",
            confirmPassword = "secret12",
            avatarUri = null
        ).getOrThrow()

        val post = repo.createPost(
            authorId = alice.id,
            caption = "Hello SPLEEX #firstpost",
            hashtagsInput = "",
            selectedImageUris = emptyList(),
            isAnonymousConfession = false,
            isPrivateCircle = false,
            pollQuestion = "",
            pollOptions = emptyList()
        ).getOrThrow()

        repo.toggleFollow(bob.id, alice.id)
        repo.toggleLike(post.id, bob.id)
        repo.addComment(post.id, bob.id, "Welcome to SPLEEX!")

        val bobFeed = repo.observePostsFeed(bob.id).first()
        val feedPost = bobFeed.first { it.id == post.id }
        assertEquals(1, feedPost.likesCount)
        assertEquals(1, feedPost.commentsCount)
        assertTrue(feedPost.isLikedByMe)
        assertTrue(feedPost.isFollowingAuthor)
    }
}
