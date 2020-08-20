package awais.instagrabber.models.direct_messages;

import androidx.core.util.ObjectsCompat;

import java.io.Serializable;

import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.InboxReadState;

public final class InboxThreadModel implements Serializable {
    private final InboxReadState readState;
    private final String threadId, threadV2Id, threadType, threadTitle, newestCursor, oldestCursor, nextCursor, prevCursor;
    private final ProfileModel inviter;
    private final ProfileModel[] users, leftUsers;
    private final DirectItemModel[] items;
    private final boolean muted, isPin, isSpam, isGroup, named, pending, archived, canonical, hasOlder, hasNewer;
    private final long lastActivityAt;

    public InboxThreadModel(final InboxReadState readState, final String threadId, final String threadV2Id, final String threadType, final String threadTitle,
                            final String newestCursor, final String oldestCursor, final String nextCursor, final String prevCursor,
                            final ProfileModel inviter, final ProfileModel[] users,
                            final ProfileModel[] leftUsers, final DirectItemModel[] items, final boolean muted,
                            final boolean isPin, final boolean named, final boolean canonical, final boolean pending,
                            final boolean hasOlder, final boolean hasNewer, final boolean isSpam, final boolean isGroup,
                            final boolean archived, final long lastActivityAt) {
        this.readState = readState;
        this.threadId = threadId;
        this.threadV2Id = threadV2Id;
        this.threadType = threadType;
        this.threadTitle = threadTitle;
        this.newestCursor = newestCursor;
        this.oldestCursor = oldestCursor;
        this.nextCursor = nextCursor;
        this.prevCursor = prevCursor;
        this.inviter = inviter;
        this.users = users;
        this.leftUsers = leftUsers;
        this.items = items; // todo
        this.muted = muted;
        this.isPin = isPin;
        this.named = named;
        this.canonical = canonical;
        this.pending = pending;
        this.hasOlder = hasOlder;
        this.hasNewer = hasNewer;
        this.isSpam = isSpam;
        this.isGroup = isGroup;
        this.archived = archived;
        this.lastActivityAt = lastActivityAt;
    }

    public InboxReadState getReadState() {
        return readState;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getThreadV2Id() {
        return threadV2Id;
    }

    public String getThreadType() {
        return threadType;
    }

    public String getThreadTitle() {
        return threadTitle;
    }

    public String getNewestCursor() {
        return newestCursor;
    }

    public String getOldestCursor() {
        return oldestCursor;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public String getPrevCursor() {
        return prevCursor;
    }

    public ProfileModel getInviter() {
        return inviter;
    }

    public ProfileModel[] getUsers() {
        return users;
    }

    public ProfileModel[] getLeftUsers() {
        return leftUsers;
    }

    public DirectItemModel[] getItems() {
        return items;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isPin() {
        return isPin;
    }

    public boolean isNamed() {
        return named;
    }

    public boolean isPending() {
        return pending;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public boolean hasOlder() {
        return hasOlder;
    }

    public boolean isHasNewer() {
        return hasNewer;
    }

    public boolean isSpam() {
        return isSpam;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final InboxThreadModel that = (InboxThreadModel) o;
        return ObjectsCompat.equals(threadId, that.threadId) &&
                ObjectsCompat.equals(threadV2Id, that.threadV2Id);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(threadId, threadV2Id);
    }
}