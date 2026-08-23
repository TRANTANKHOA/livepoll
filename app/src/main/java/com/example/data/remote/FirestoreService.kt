package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.local.entity.VoteEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreService {

    private val tag = "FirestoreService"

    private val db: FirebaseFirestore?
        get() = try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase not initialized yet or config missing: ${e.message}")
            null
        }

    fun isCloudAvailable(): Boolean {
        return db != null
    }

    /**
     * Publishes a poll and its options to Cloud Firestore collection 'polls'.
     */
    suspend fun publishPollToCloud(
        poll: PollEntity,
        options: List<PollOptionEntity>
    ): Boolean {
        val firestore = db ?: return false
        return try {
            val optionsList = options.map { opt ->
                mapOf(
                    "id" to opt.id,
                    "pollId" to opt.pollId,
                    "text" to opt.text,
                    "subtitle" to (opt.subtitle ?: ""),
                    "dateTimeSlot" to (opt.dateTimeSlot ?: ""),
                    "venueAddress" to (opt.venueAddress ?: ""),
                    "duration" to (opt.duration ?: ""),
                    "priceRating" to (opt.priceRating ?: ""),
                    "addedBy" to (opt.addedBy ?: ""),
                    "displayOrder" to opt.displayOrder
                )
            }

            val pollData = hashMapOf(
                "id" to poll.id,
                "code" to poll.code,
                "title" to poll.title,
                "description" to poll.description,
                "category" to poll.category,
                "categoryIcon" to poll.categoryIcon,
                "creatorName" to poll.creatorName,
                "createdAt" to poll.createdAt,
                "deadlineTimestamp" to (poll.deadlineTimestamp ?: 0L),
                "allowMultipleChoices" to poll.allowMultipleChoices,
                "allowCustomOptions" to poll.allowCustomOptions,
                "isAnonymous" to poll.isAnonymous,
                "isClosed" to poll.isClosed,
                "targetHeadcount" to (poll.targetHeadcount ?: 0),
                "location" to (poll.location ?: ""),
                "groupId" to (poll.groupId ?: ""),
                "groupName" to (poll.groupName ?: ""),
                "options" to optionsList,
                "cloudSyncedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("polls")
                .document(poll.id)
                .set(pollData, SetOptions.merge())
                .await()

            Log.d(tag, "Successfully published poll ${poll.id} (${poll.code}) to Cloud Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error publishing poll to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Look up a poll by its 6-character short code in Cloud Firestore.
     */
    suspend fun findPollByCode(code: String): Pair<PollEntity, List<PollOptionEntity>>? {
        val firestore = db ?: return null
        return try {
            val querySnapshot = firestore.collection("polls")
                .whereEqualTo("code", code.trim().uppercase())
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.d(tag, "No cloud poll found with code $code")
                return null
            }

            val doc = querySnapshot.documents.first()
            val pollId = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: "Untitled Poll"
            val description = doc.getString("description") ?: ""
            val category = doc.getString("category") ?: "GENERAL"
            val categoryIcon = doc.getString("categoryIcon") ?: "📊"
            val creatorName = doc.getString("creatorName") ?: "Organizer"
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val deadlineTimestamp = doc.getLong("deadlineTimestamp")?.takeIf { it > 0L }
            val allowMultipleChoices = doc.getBoolean("allowMultipleChoices") ?: false
            val allowCustomOptions = doc.getBoolean("allowCustomOptions") ?: true
            val isAnonymous = doc.getBoolean("isAnonymous") ?: false
            val isClosed = doc.getBoolean("isClosed") ?: false
            val targetHeadcount = doc.getLong("targetHeadcount")?.toInt()?.takeIf { it > 0 }
            val location = doc.getString("location").takeIf { !it.isNullOrBlank() }
            val groupId = doc.getString("groupId").takeIf { !it.isNullOrBlank() }
            val groupName = doc.getString("groupName").takeIf { !it.isNullOrBlank() }

            val pollEntity = PollEntity(
                id = pollId,
                code = code.trim().uppercase(),
                title = title,
                description = description,
                category = category,
                categoryIcon = categoryIcon,
                creatorName = creatorName,
                createdAt = createdAt,
                deadlineTimestamp = deadlineTimestamp,
                allowMultipleChoices = allowMultipleChoices,
                allowCustomOptions = allowCustomOptions,
                isAnonymous = isAnonymous,
                isClosed = isClosed,
                targetHeadcount = targetHeadcount,
                location = location,
                groupId = groupId,
                groupName = groupName
            )

            @Suppress("UNCHECKED_CAST")
            val rawOptions = doc.get("options") as? List<Map<String, Any>> ?: emptyList()
            val optionEntities = rawOptions.mapIndexed { idx, map ->
                PollOptionEntity(
                    id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                    pollId = pollId,
                    text = (map["text"] as? String) ?: "Option ${idx + 1}",
                    subtitle = (map["subtitle"] as? String)?.takeIf { it.isNotBlank() },
                    dateTimeSlot = (map["dateTimeSlot"] as? String)?.takeIf { it.isNotBlank() },
                    venueAddress = (map["venueAddress"] as? String)?.takeIf { it.isNotBlank() },
                    duration = (map["duration"] as? String)?.takeIf { it.isNotBlank() },
                    priceRating = (map["priceRating"] as? String)?.takeIf { it.isNotBlank() },
                    addedBy = (map["addedBy"] as? String)?.takeIf { it.isNotBlank() },
                    displayOrder = (map["displayOrder"] as? Long)?.toInt() ?: idx
                )
            }

            Pair(pollEntity, optionEntities)
        } catch (e: Exception) {
            Log.e(tag, "Error searching cloud poll by code: ${e.message}", e)
            null
        }
    }

    /**
     * Records a vote into Firestore collection `polls/{pollId}/votes`.
     */
    suspend fun recordVoteToCloud(vote: VoteEntity): Boolean {
        val firestore = db ?: return false
        return try {
            val voteData = hashMapOf(
                "id" to vote.id,
                "pollId" to vote.pollId,
                "optionId" to vote.optionId,
                "voterId" to vote.voterId,
                "voterName" to vote.voterName,
                "timestamp" to vote.timestamp,
                "rsvpStatus" to vote.rsvpStatus,
                "plusGuests" to vote.plusGuests,
                "ratingValue" to (vote.ratingValue ?: 0),
                "feedbackComment" to (vote.feedbackComment ?: "")
            )

            firestore.collection("polls")
                .document(vote.pollId)
                .collection("votes")
                .document(vote.id)
                .set(voteData, SetOptions.merge())
                .await()

            Log.d(tag, "Successfully uploaded vote ${vote.id} to Cloud Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error recording vote to Cloud Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches all cloud votes for a given poll from Cloud Firestore.
     */
    suspend fun fetchCloudVotes(pollId: String): List<VoteEntity> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection("polls")
                .document(pollId)
                .collection("votes")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val optionId = doc.getString("optionId") ?: return@mapNotNull null
                val voterId = doc.getString("voterId") ?: return@mapNotNull null
                val voterName = doc.getString("voterName") ?: "Voter"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val rsvpStatus = doc.getString("rsvpStatus") ?: "GOING"
                val plusGuests = doc.getLong("plusGuests")?.toInt() ?: 0
                val ratingValue = doc.getLong("ratingValue")?.toInt()?.takeIf { it > 0 }
                val feedbackComment = doc.getString("feedbackComment").takeIf { !it.isNullOrBlank() }

                VoteEntity(
                    id = id,
                    pollId = pollId,
                    optionId = optionId,
                    voterId = voterId,
                    voterName = voterName,
                    timestamp = timestamp,
                    rsvpStatus = rsvpStatus,
                    plusGuests = plusGuests,
                    ratingValue = ratingValue,
                    feedbackComment = feedbackComment
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching cloud votes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Attaches a real-time Firestore Snapshot listener to sync multiplayer votes instantly.
     */
    fun attachLiveVotesListener(
        pollId: String,
        onVotesUpdated: (List<VoteEntity>) -> Unit
    ): ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            firestore.collection("polls")
                .document(pollId)
                .collection("votes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Snapshot listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val votes = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val optionId = doc.getString("optionId") ?: return@mapNotNull null
                            val voterId = doc.getString("voterId") ?: return@mapNotNull null
                            val voterName = doc.getString("voterName") ?: "Voter"
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val rsvpStatus = doc.getString("rsvpStatus") ?: "GOING"
                            val plusGuests = doc.getLong("plusGuests")?.toInt() ?: 0
                            val ratingValue = doc.getLong("ratingValue")?.toInt()?.takeIf { it > 0 }
                            val feedbackComment = doc.getString("feedbackComment").takeIf { !it.isNullOrBlank() }

                            VoteEntity(
                                id = id,
                                pollId = pollId,
                                optionId = optionId,
                                voterId = voterId,
                                voterName = voterName,
                                timestamp = timestamp,
                                rsvpStatus = rsvpStatus,
                                plusGuests = plusGuests,
                                ratingValue = ratingValue,
                                feedbackComment = feedbackComment
                            )
                        }
                        onVotesUpdated(votes)
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach snapshot listener: ${e.message}")
            null
        }
    }
}
