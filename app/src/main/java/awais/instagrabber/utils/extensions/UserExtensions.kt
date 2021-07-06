package awais.instagrabber.utils.extensions

import awais.instagrabber.repositories.responses.User

fun User.isReallyPrivate(currentUser: User? = null): Boolean {
    if (currentUser == null) return this.isPrivate
    if (this.pk == currentUser.pk) return false
    return this.friendshipStatus?.following == false && this.isPrivate
}