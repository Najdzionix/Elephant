package com.pinktwins.elephant.model;

/**
 * Created by Kamil Nadonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class AttachmentInfo {
    Object object;
    int startPosition;
    int endPosition;

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
}
