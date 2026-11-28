package com.plutusoft.livepulse.data.remote

import android.util.Log
import com.plutusoft.livepulse.BuildConfig
import com.plutusoft.livepulse.data.local.entity.PollEntity
import com.plutusoft.livepulse.data.local.entity.PollOptionEntity
import com.plutusoft.livepulse.data.local.entity.VoteEntity
import com.plutusoft.livepulse.util.SecurityDefenseHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreService {

    private val tag = "FirestoreService"

    // Set once before first use of the instance; useEmulator must not be called twice.
    @Volatile private var emulatorConfigured = false

    private val db: FirebaseFirestore?
        get() = try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance().also { configureEmulatorIfNeeded(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase not initialized yet or config missing: ${e.message}")
            null
        }

    private fun configureEmulatorIfNeeded(firestore: FirebaseFirestore) {
        if (!BuildConfig.FIRESTORE_EMULATOR || emulatorConfigured) return
        synchronized(this) {
            if (emulatorConfigured) return
            // 10.0.2.2 is the host loopback as seen from the Android emulator.
            firestore.useEmulator("10.0.2.2", 8080)
            emulatorConfigured = true
            Log.d(tag, "Firestore pointed at local emulator 10.0.2.2:8080")
        }
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
    /**
     * Appends a suggested option to the cloud poll doc's options array (KB-07):
     * the web client renders options from the poll doc, so app-side suggestions
     * must land there too — Room-only inserts are invisible to web joiners.
     */
    suspend fun publishOptionToCloud(pollId: String, option: PollOptionEntity): Boolean {
        val firestore = db ?: return false
        return try {
            val optionData = mapOf(
                "id" to option.id,
                "pollId" to option.pollId,
                "text" to option.text,
                "subtitle" to (option.subtitle ?: ""),
                "dateTimeSlot" to (option.dateTimeSlot ?: ""),
                "venueAddress" to (option.venueAddress ?: ""),
                "duration" to (option.duration ?: ""),
                "priceRating" to (option.priceRating ?: ""),
                "addedBy" to (option.addedBy ?: ""),
                "displayOrder" to option.displayOrder
            )
            firestore.collection("polls")
                .document(pollId)
                .update("options", FieldValue.arrayUnion(optionData))
                .await()
            Log.d(tag, "Appended option ${option.id} to cloud poll $pollId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error appending option to cloud: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches the current poll doc as a PollEntity (inbound poll-doc sync):
     * the app otherwise never learns cloud-side poll changes (isClosed,
     * isAnonymous, deadline, title edits from other clients) — its Room row
     * goes stale forever. Caller updates Room via PollDao.updatePoll (a plain
     * row UPDATE — never REPLACE, which would cascade-delete votes/options).
     */
    suspend fun fetchPollFromCloud(pollId: String): PollEntity? {
        val firestore = db ?: return null
        return try {
            val doc = firestore.collection("polls").document(pollId).get().await()
            if (!doc.exists()) return null
            val deadlineTimestamp = doc.getLong("deadlineTimestamp")?.takeIf { it > 0L }
            PollEntity(
                id = pollId,
                code = (doc.getString("code") ?: "").trim().uppercase(),
                title = doc.getString("title") ?: "Untitled Poll",
                description = doc.getString("description") ?: "",
                category = doc.getString("category") ?: "GENERAL",
                categoryIcon = doc.getString("categoryIcon") ?: "📊",
                creatorName = doc.getString("creatorName") ?: "Organizer",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                deadlineTimestamp = deadlineTimestamp,
                allowMultipleChoices = doc.getBoolean("allowMultipleChoices") ?: false,
                allowCustomOptions = doc.getBoolean("allowCustomOptions") ?: true,
                isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                isClosed = doc.getBoolean("isClosed") ?: false,
                targetHeadcount = doc.getLong("targetHeadcount")?.toInt()?.takeIf { it > 0 },
                location = doc.getString("location").takeIf { !it.isNullOrBlank() },
                groupId = doc.getString("groupId").takeIf { !it.isNullOrBlank() },
                groupName = doc.getString("groupName").takeIf { !it.isNullOrBlank() }
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching poll from cloud: ${e.message}", e)
            null
        }
    }

    /**
     * Fetches the cloud poll doc's current options (KB-07): the live-votes
     * listener reconciles unknown optionIds against this before writing votes,
     * so web-suggested options reach Room instead of tripping the FK constraint.
     */
    suspend fun fetchPollOptionsFromCloud(pollId: String): List<PollOptionEntity> {
        val firestore = db ?: return emptyList()
        return try {
            val doc = firestore.collection("polls").document(pollId).get().await()
            val raw = doc.get("options") as? List<*> ?: return emptyList()
            raw.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val id = map["id"] as? String ?: return@mapNotNull null
                val text = map["text"] as? String ?: return@mapNotNull null
                PollOptionEntity(
                    id = id,
                    pollId = pollId,
                    text = text,
                    subtitle = map["subtitle"] as? String ?: "",
                    dateTimeSlot = map["dateTimeSlot"] as? String ?: "",
                    venueAddress = map["venueAddress"] as? String ?: "",
                    duration = map["duration"] as? String ?: "",
                    priceRating = map["priceRating"] as? String ?: "",
                    addedBy = map["addedBy"] as? String ?: "",
                    displayOrder = (map["displayOrder"] as? Long)?.toInt() ?: 99
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching cloud poll options: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Syncs a poll's open/closed state to the cloud doc (KB-06): other devices
     * and the web client learn about manual close/re-open via onSnapshot.
     * Uses merge so only isClosed/cloudSyncedAt change.
     */
    suspend fun publishPollStatusToCloud(pollId: String, isClosed: Boolean): Boolean {
        val firestore = db ?: return false
        return try {
            firestore.collection("polls")
                .document(pollId)
                .set(
                    hashMapOf(
                        "isClosed" to isClosed,
                        "cloudSyncedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
            Log.d(tag, "Synced poll $pollId status to cloud (isClosed=$isClosed)")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error syncing poll status to cloud: ${e.message}", e)
            false
        }
    }

    /**
     * Publishes the voter's ballot as a single per-voter document
     * (votes/{voterId}) holding a `selections` array. Overwriting replaces the
     * previous selection, so changed votes never leave stale docs behind (KB-04).
     * All votes in [votes] must come from the same castVotes call: same poll and voter.
     */
    suspend fun publishVoterBallot(votes: List<VoteEntity>): Boolean {
        val first = votes.firstOrNull() ?: return false
        val firestore = db ?: return false
        return try {
            val ballot = hashMapOf<String, Any?>(
                "pollId" to first.pollId,
                "voterId" to first.voterId,
                "voterName" to first.voterName,
                "timestamp" to System.currentTimeMillis(),
                "rsvpStatus" to first.rsvpStatus,
                "plusGuests" to first.plusGuests,
                "ratingValue" to (first.ratingValue ?: 0),
                "feedbackComment" to (first.feedbackComment ?: ""),
                "selections" to votes.map { mapOf("optionId" to it.optionId) }
            )

            firestore.collection("polls")
                .document(first.pollId)
                .collection("votes")
                .document(first.voterId)
                .set(ballot)
                .await()

            Log.d(tag, "Successfully uploaded ballot ${first.voterId} (${votes.size} selections) to Cloud Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error publishing ballot to Cloud Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Maps vote documents to VoteEntities. Two schemas coexist (KB-04 migration):
     * - ballot docs (votes/{voterId} with a `selections` list) — expanded so each
     *   selected option becomes one vote;
     * - legacy per-option docs (votes/{voterId}_{optionId}) — counted only when the
     *   voter has no ballot doc; a ballot supersedes their stale legacy votes.
     */
    private fun mapVoteDocuments(pollId: String, documents: List<DocumentSnapshot>): List<VoteEntity> {
        val ballotVoters = documents
            .filter { it.get("selections") is List<*> }
            .mapNotNull { it.getString("voterId") }
            .toSet()

        val votes = mutableListOf<VoteEntity>()
        for (doc in documents) {
            val voterId = doc.getString("voterId") ?: continue
            val voterName = doc.getString("voterName") ?: "Voter"
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val rsvpStatus = doc.getString("rsvpStatus") ?: "GOING"
            val plusGuests = doc.getLong("plusGuests")?.toInt() ?: 0
            val ratingValue = doc.getLong("ratingValue")?.toInt()?.takeIf { it > 0 }
            val feedbackComment = doc.getString("feedbackComment").takeIf { !it.isNullOrBlank() }

            val selections = doc.get("selections")
            if (selections is List<*>) {
                for (selection in selections) {
                    val optionId = (selection as? Map<*, *>)?.get("optionId") as? String ?: continue
                    votes += VoteEntity(
                        id = SecurityDefenseHelper.createIdempotentVoteKey(pollId, voterId, optionId),
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
            } else {
                val optionId = doc.getString("optionId") ?: continue
                if (voterId in ballotVoters) continue
                votes += VoteEntity(
                    id = doc.getString("id") ?: doc.id,
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
        }
        return votes
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

            snapshot.documents.let { mapVoteDocuments(pollId, it) }
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
                        onVotesUpdated(mapVoteDocuments(pollId, snapshot.documents))
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach snapshot listener: ${e.message}")
            null
        }
    }
}
