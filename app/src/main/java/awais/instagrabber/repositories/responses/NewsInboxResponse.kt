package awais.instagrabber.repositories.responses

import awais.instagrabber.repositories.responses.notification.Notification
import awais.instagrabber.repositories.responses.notification.NotificationCounts

data class NewsInboxResponse(
    val counts: NotificationCounts,
    val newStories: List<Notification>,
    val oldStories: List<Notification>
)