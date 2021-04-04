package awais.instagrabber.repositories.responses.search;

import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Place;
import awais.instagrabber.repositories.responses.User;

public class SearchItem {
    private final User user;
    private final Place place;
    private final Hashtag hashtag;
    private final int position;

    public SearchItem(final User user,
                      final Place place,
                      final Hashtag hashtag,
                      final int position) {
        this.user = user;
        this.place = place;
        this.hashtag = hashtag;
        this.position = position;
    }

    public User getUser() {
        return user;
    }

    public Place getPlace() {
        return place;
    }

    public Hashtag getHashtag() {
        return hashtag;
    }

    public int getPosition() {
        return position;
    }
}
