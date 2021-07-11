package thoughtbot.expandableadapter;

import java.util.List;

import awais.instagrabber.repositories.responses.User;

public class ExpandableGroup {
    private final String title;
    private final List<User> items;

    public ExpandableGroup(final String title, final List<User> items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public List<User> getItems() {
        return items;
    }

    public int getItemCount() {
        if (items != null) {
            return items.size();
        }
        return 0;
    }
}