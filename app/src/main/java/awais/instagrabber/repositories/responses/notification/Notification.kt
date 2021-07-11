package awais.instagrabber.repositories.responses.notification

import awais.instagrabber.models.enums.NotificationType
import awais.instagrabber.models.enums.NotificationType.Companion.valueOfType

class Notification(val args: NotificationArgs,
                   private val storyType: Int,
                   val pk: String) {
    val type: NotificationType?
        get() = valueOfType(storyType)
}