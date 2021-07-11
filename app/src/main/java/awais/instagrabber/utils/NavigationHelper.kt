package awais.instagrabber.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.ArrayRes
import awais.instagrabber.R
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.models.Tab

var tabOrderString: String? = null

private val NON_REMOVABLE_NAV_ROOT_IDS: List<Int> = listOf(R.id.profile_nav_graph, R.id.more_nav_graph)


fun getLoggedInNavTabs(context: Context): Pair<List<Tab>, List<Tab>> {
    val navRootIds = getArrayResIds(context.resources, R.array.logged_in_nav_root_ids)
    return getTabs(context, navRootIds)
}

fun getAnonNavTabs(context: Context): List<Tab> {
    val navRootIds = getArrayResIds(context.resources, R.array.anon_nav_root_ids)
    val (tabs, _) = getTabs(context, navRootIds, true)
    return tabs
}

private fun getTabs(
    context: Context,
    navRootIds: IntArray,
    isAnon: Boolean = false,
): Pair<List<Tab>, MutableList<Tab>> {
    val navGraphNames = getResIdsForNavRootIds(navRootIds, ::getNavGraphNameForNavRootId)
    val navGraphResIds = getResIdsForNavRootIds(navRootIds, ::getNavGraphResIdForNavRootId)
    val titleArray = getResIdsForNavRootIds(navRootIds, ::getTitleResIdForNavRootId)
    val iconIds = getResIdsForNavRootIds(navRootIds, ::getIconResIdForNavRootId)
    val startDestFragIds = getResIdsForNavRootIds(navRootIds, ::getStartDestFragIdForNavRootId)
    val (orderedGraphNames, orderedNavRootIds) = if (isAnon) navGraphNames to navRootIds.toList() else getOrderedNavRootIdsFromPref(navGraphNames)
    val tabs = mutableListOf<Tab>()
    val otherTabs = mutableListOf<Tab>() // Will contain tabs not in current list
    for (i in navRootIds.indices) {
        val navRootId = navRootIds[i]
        val tab = Tab(
            iconIds[i],
            context.getString(titleArray[i]),
            if (isAnon) false else !NON_REMOVABLE_NAV_ROOT_IDS.contains(navRootId),
            navGraphResIds[i],
            navRootId,
            startDestFragIds[i]
        )
        if (!isAnon && !orderedGraphNames.contains(navGraphNames[i])) {
            otherTabs.add(tab)
            continue
        }
        tabs.add(tab)
    }
    val associateBy = tabs.associateBy { it.navigationRootId }
    val orderedTabs = orderedNavRootIds.mapNotNull { associateBy[it] }
    return orderedTabs to otherTabs
}

private fun getArrayResIds(resources: Resources, @ArrayRes arrayRes: Int): IntArray {
    val typedArray = resources.obtainTypedArray(arrayRes)
    val length = typedArray.length()
    val navRootIds = IntArray(length)
    for (i in 0 until length) {
        val resourceId = typedArray.getResourceId(i, 0)
        if (resourceId == 0) continue
        navRootIds[i] = resourceId
    }
    typedArray.recycle()
    return navRootIds
}

private fun <T> getResIdsForNavRootIds(navRootIds: IntArray, resMapper: Function1<Int, T>): List<T> = navRootIds
    .asSequence()
    .filterNot { it == 0 }
    .map(resMapper)
    .filterNot { it == 0 }
    .toList()

private fun getTitleResIdForNavRootId(id: Int): Int = when (id) {
    R.id.direct_messages_nav_graph -> R.string.title_dm
    R.id.feed_nav_graph -> R.string.feed
    R.id.profile_nav_graph -> R.string.profile
    R.id.discover_nav_graph -> R.string.title_discover
    R.id.more_nav_graph -> R.string.more
    R.id.favorites_nav_graph -> R.string.title_favorites
    R.id.notification_viewer_nav_graph -> R.string.title_notifications
    else -> 0
}

private fun getIconResIdForNavRootId(id: Int): Int = when (id) {
    R.id.direct_messages_nav_graph -> R.drawable.ic_message_24
    R.id.feed_nav_graph -> R.drawable.ic_home_24
    R.id.profile_nav_graph -> R.drawable.ic_person_24
    R.id.discover_nav_graph -> R.drawable.ic_explore_24
    R.id.more_nav_graph -> R.drawable.ic_more_horiz_24
    R.id.favorites_nav_graph -> R.drawable.ic_star_24
    R.id.notification_viewer_nav_graph -> R.drawable.ic_not_liked
    else -> 0
}

private fun getStartDestFragIdForNavRootId(id: Int): Int = when (id) {
    R.id.direct_messages_nav_graph -> R.id.directMessagesInboxFragment
    R.id.feed_nav_graph -> R.id.feedFragment
    R.id.profile_nav_graph -> R.id.profileFragment
    R.id.discover_nav_graph -> R.id.discoverFragment
    R.id.more_nav_graph -> R.id.morePreferencesFragment
    R.id.favorites_nav_graph -> R.id.favoritesFragment
    R.id.notification_viewer_nav_graph -> R.id.notificationsViewer
    else -> 0
}

fun getNavGraphNameForNavRootId(id: Int): String = when (id) {
    R.id.direct_messages_nav_graph -> "direct_messages_nav_graph"
    R.id.feed_nav_graph -> "feed_nav_graph"
    R.id.profile_nav_graph -> "profile_nav_graph"
    R.id.discover_nav_graph -> "discover_nav_graph"
    R.id.more_nav_graph -> "more_nav_graph"
    R.id.favorites_nav_graph -> "favorites_nav_graph"
    R.id.notification_viewer_nav_graph -> "notification_viewer_nav_graph"
    else -> ""
}

fun getNavGraphResIdForNavRootId(id: Int): Int = when (id) {
    R.id.direct_messages_nav_graph -> R.navigation.direct_messages_nav_graph
    R.id.feed_nav_graph -> R.navigation.feed_nav_graph
    R.id.profile_nav_graph -> R.navigation.profile_nav_graph
    R.id.discover_nav_graph -> R.navigation.discover_nav_graph
    R.id.more_nav_graph -> R.navigation.more_nav_graph
    R.id.favorites_nav_graph -> R.navigation.favorites_nav_graph
    R.id.notification_viewer_nav_graph -> R.navigation.notification_viewer_nav_graph
    else -> 0
}

private fun getNavRootIdForGraphName(navGraphName: String): Int = when (navGraphName) {
    "direct_messages_nav_graph" -> R.id.direct_messages_nav_graph
    "feed_nav_graph" -> R.id.feed_nav_graph
    "profile_nav_graph" -> R.id.profile_nav_graph
    "discover_nav_graph" -> R.id.discover_nav_graph
    "more_nav_graph" -> R.id.more_nav_graph
    "favorites_nav_graph" -> R.id.favorites_nav_graph
    "notification_viewer_nav_graph" -> R.id.notification_viewer_nav_graph
    else -> 0
}

private fun getOrderedNavRootIdsFromPref(navGraphNames: List<String>): Pair<List<String>, List<Int>> {
    tabOrderString = Utils.settingsHelper.getString(PreferenceKeys.PREF_TAB_ORDER)
    if (tabOrderString.isNullOrBlank()) {
        // Use top 5 entries for default list
        val top5navGraphNames: List<String> = navGraphNames.subList(0, 5)
        val newOrderString = top5navGraphNames.joinToString(",")
        Utils.settingsHelper.putString(PreferenceKeys.PREF_TAB_ORDER, newOrderString)
        tabOrderString = newOrderString
        return top5navGraphNames to top5navGraphNames.map(::getNavRootIdForGraphName)
    }
    val orderString = tabOrderString ?: return navGraphNames to navGraphNames.subList(0, 5).map(::getNavRootIdForGraphName)
    // Make sure that the list from preference does not contain any invalid values
    val orderGraphNames = orderString
        .split(",")
        .asSequence()
        .filter(String::isNotBlank)
        .filter(navGraphNames::contains)
        .toList()
    val graphNames = if (orderGraphNames.isEmpty()) {
        // Use top 5 entries for default list
        navGraphNames.subList(0, 5)
    } else orderGraphNames
    return graphNames to graphNames.map(::getNavRootIdForGraphName)
}

fun isNavRootInCurrentTabs(navRootString: String?): Boolean {
    val navRoot = navRootString ?: return false
    return tabOrderString?.contains(navRoot) ?: false
}