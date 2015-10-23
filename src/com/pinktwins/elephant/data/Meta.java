package com.pinktwins.elephant.data;

import java.io.File;
import java.util.List;

/**
 * Created by Kamil Nadłonek on 23.10.15.
 * email:kamilnadlonek@gmail.com
 */
public interface Meta {
    String title();

    long created();

    void title(String newTitle);

    void setCreatedTime();

    int getAttachmentPosition(File attachment);

    void setAttachmentPosition(File attachment, int position);

    List<String> tags();

    void setTags(List<String> tagIds, List<String> tagNames);
}
