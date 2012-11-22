package org.gradle.sample;

import java.util.Iterator;
import java.util.Arrays;

public class People implements Iterable<Person> {
    public Iterator<Person> iterator() {
        return Arrays.asList(new Person("free person")).iterator();
    }
}
