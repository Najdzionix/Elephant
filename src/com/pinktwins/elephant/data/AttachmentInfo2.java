package com.pinktwins.elephant.data;

import java.io.File;

/**
 * Created by Kamil Nadłonek on 26.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class AttachmentInfo2 implements Comparable<AttachmentInfo2> {
    public File f;
    public int position;

    public AttachmentInfo2(File f, int position) {
        this.f = f;
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int compareTo(AttachmentInfo2 o) {
        return position - o.position;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
