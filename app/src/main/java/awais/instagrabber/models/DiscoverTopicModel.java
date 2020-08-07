package awais.instagrabber.models;

import java.io.Serializable;

public final class DiscoverTopicModel implements Serializable {
    private final String[] id, name;

    public DiscoverTopicModel(final String[] id, final String[] name) {
        this.id = id;
        this.name = name;
    }

    public String[] getIds() {
        return id;
    }

    public String[] getNames() {
        return name;
    }
}