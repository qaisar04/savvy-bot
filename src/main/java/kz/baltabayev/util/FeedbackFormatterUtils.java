package kz.baltabayev.util;

import kz.baltabayev.entity.Feedback;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class FeedbackFormatterUtils {

    public static String formatFeedbackList(List<Feedback> feedbackList) {

        StringBuilder formattedList = new StringBuilder();
        for (Feedback feedback : feedbackList) {
            formattedList.append("ID: ").append(feedback.getId())
                    .append(", Username: ").append(feedback.getUsername())
                    .append(", Created At: ").append(feedback.getCreatedAt())
                    .append(", Description: ").append(feedback.getDescription())
                    .append("\n");
        }

        return formattedList.toString();
    }

}
