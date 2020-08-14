package awais.instagrabber.models;

import java.io.Serializable;

public final class DiscoverTopicModel implements Serializable {
    private final String[] id, name;
    private final String rankToken;

    public DiscoverTopicModel(final String[] id, final String[] name, final String rankToken) {
        this.id = id;
        this.name = name;
        this.rankToken = rankToken;
    }

    public String[] getIds() {
        return id;
    }

    public String[] getNames() {
        return name;
    }

    public String getToken() {
        return rankToken;
    }
}