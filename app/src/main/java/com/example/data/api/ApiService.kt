package com.example.data.api

import com.example.data.models.AuthResponse
import com.example.data.models.Conversation
import com.example.data.models.LiveStream
import com.example.data.models.Message
import com.example.data.models.User
import com.example.data.models.Video
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): AuthResponse

    @POST("api/users/register")
    suspend fun register(@Body request: Map<String, String>): User

    @GET("api/feed/next")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 10
    ): List<Video>

    @GET("api/feed/following")
    suspend fun getFollowingFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 10
    ): List<Video>

    @POST("api/videos/{id}/like")
    suspend fun likeVideo(@Path("id") videoId: String): Map<String, Boolean>

    @POST("api/videos/{id}/view")
    suspend fun viewVideo(@Path("id") videoId: String)

    @Multipart
    @POST("api/videos/upload")
    suspend fun uploadVideo(
        @Part video: MultipartBody.Part,
        @Part("description") description: RequestBody,
        @Part("is_public") isPublic: RequestBody,
        @Part("audio_title") audioTitle: RequestBody? = null,
        @Part("audio_owner") audioOwner: RequestBody? = null,
        @Part("effect") effect: RequestBody? = null
    ): Video

    @GET("api/users/me")
    suspend fun getMyProfile(): User

    @POST("api/users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Map<String, Any>

    @GET("api/users/{userId}/sounds")
    suspend fun getUserSounds(@Path("userId") userId: String): List<Video> // Videos identified as having original sound

    @POST("api/sounds/{videoId}/save")
    suspend fun saveSound(@Path("videoId") videoId: String): Map<String, Any>

    @GET("api/users/me/saved-sounds")
    suspend fun getSavedSounds(): List<Video>

    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): User

    @GET("api/messages/conversations")
    suspend fun getConversations(): List<Conversation>

    @GET("api/messages/{user_id}")
    suspend fun getMessages(@Path("user_id") userId: String): List<Message>

    @POST("api/messages/send")
    suspend fun sendMessage(@Body request: Map<String, String>): Message

    @GET("api/lives/active")
    suspend fun getActiveLives(): List<LiveStream>

    @GET("api/actfile")
    suspend fun getActFiles(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): List<com.example.data.models.ActFile>

    @POST("api/actfile")
    suspend fun createActFile(@Body request: Map<String, String>): com.example.data.models.ActFile

    @POST("api/actfile/{id}/like")
    suspend fun likeActFile(@Path("id") id: String): Map<String, Boolean>

    @POST("api/users/me/verify")
    suspend fun requestVerification(@Body criteria: com.example.data.models.VerificationCriteria): com.example.data.models.VerificationResult

    @GET("api/users/me/verification-status")
    suspend fun getVerificationStatus(): com.example.data.models.VerificationStatusResponse

    @GET("api/videos/{id}/comments")
    suspend fun getVideoComments(@Path("id") videoId: String): List<com.example.data.models.VideoComment>

    @POST("api/videos/{id}/comments")
    suspend fun addVideoComment(@Path("id") videoId: String, @Body content: Map<String, String>): com.example.data.models.VideoComment

    @GET("api/actfile/{id}/replies")
    suspend fun getActFileReplies(@Path("id") id: String): List<com.example.data.models.ActFileReply>

    @POST("api/actfile/{id}/replies")
    suspend fun addActFileReply(@Path("id") id: String, @Body content: Map<String, String>): com.example.data.models.ActFileReply

    @Multipart
    @POST("api/users/me/avatar")
    suspend fun updateAvatar(@Part avatar: MultipartBody.Part): User

    @GET("api/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("type") type: String = "users",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<User>
    
    @GET("api/search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("type") type: String = "videos",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<Video>

    @GET("api/users")
    suspend fun getUsers(): List<User>

    @GET("api/stories")
    suspend fun getStories(): List<com.example.data.models.Story>

    @Multipart
    @POST("api/stories")
    suspend fun uploadStory(
        @Part file: MultipartBody.Part,
        @Part("effect") effect: RequestBody? = null
    ): Map<String, Any>

    @GET("api/stories/{id}/comments")
    suspend fun getStoryComments(@Path("id") storyId: String): List<com.example.data.models.VideoComment>

    @POST("api/stories/{id}/comments")
    suspend fun addStoryComment(@Path("id") storyId: String, @Body content: Map<String, String>): com.example.data.models.VideoComment

    @GET("api/admin/stats")
    suspend fun getServerStats(): Map<String, Any>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body updates: Map<String, String>): User

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("unread_only") unreadOnly: Boolean = false,
        @Query("limit") limit: Int = 50
    ): List<com.example.data.models.Notification>

    @GET("api/users/{id}/videos")
    suspend fun getUserVideos(@Path("id") userId: String): List<Video>
}
