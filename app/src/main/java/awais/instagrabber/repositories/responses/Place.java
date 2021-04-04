package awais.instagrabber.repositories.responses;

public class Place {
    private final Location location;
    // for search
    private final String title; // those are repeated within location
    private final String subtitle; // address
    private final String slug; // browser only; for end of address
    // for location info
    private final String status;

    public Place(final Location location,
                 final String title,
                 final String subtitle,
                 final String slug,
                 final String status) {
        this.location = location;
        this.title = title;
        this.subtitle = subtitle;
        this.slug = slug;
        this.status = status;
    }

    public Location getLocation() {
        return location;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getSlug() {
        return slug;
    }

    public String getStatus() {
        return status;
    }
}
