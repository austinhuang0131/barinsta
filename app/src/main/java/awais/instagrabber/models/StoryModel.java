package awais.instagrabber.models;

import java.io.Serializable;

import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;

public final class StoryModel implements Serializable {
    private final String storyMediaId, storyUrl, username;
    private final MediaItemType itemType;
    private final long timestamp;
    private String videoUrl, tappableShortCode, tappableId, spotify;
    private PollModel poll;
    private QuestionModel question;
    private QuizModel quiz;
    private String[] mentions;
    private int position;
    private boolean isCurrentSlide = false;

    public StoryModel(final String storyMediaId, final String storyUrl, final MediaItemType itemType, final long timestamp, final String username) {
        this.storyMediaId = storyMediaId;
        this.storyUrl = storyUrl;
        this.itemType = itemType;
        this.timestamp = timestamp;
        this.username = username;
    }

    public String getStoryUrl() {
        return storyUrl;
    }

    public String getStoryMediaId() {
        return storyMediaId;
    }

    public MediaItemType getItemType() {
        return itemType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getTappableShortCode() {
        return tappableShortCode;
    }

    public PollModel getPoll() {
        return poll;
    }

    public QuestionModel getQuestion() {
        return question;
    }

    public QuizModel getQuiz() {
        return quiz;
    }

    public String[] getMentions() {
        return mentions;
    }

    public int getPosition() {
        return position;
    }

    public void setVideoUrl(final String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getSpotify() {
        return spotify;
    }

    public void setSpotify(final String spotify) {
        this.spotify = spotify;
    }

    public void setTappableShortCode(final String tappableShortCode) {
        this.tappableShortCode = tappableShortCode;
    }

    public void setPoll(final PollModel poll) {
        this.poll = poll;
    }

    public void setQuestion(final QuestionModel question) {
        this.question = question;
    }

    public void setQuiz(final QuizModel quiz) {
        this.quiz = quiz;
    }

    public void setMentions(final String[] mentions) {
        this.mentions = mentions;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public void setCurrentSlide(final boolean currentSlide) {
        this.isCurrentSlide = currentSlide;
    }

    public boolean isCurrentSlide() {
        return isCurrentSlide;
    }

    public String getUsername() {
        return username;
    }
}