package org.gradle.sample;

import java.util.Arrays;
import java.util.Iterator;

public class People implements Iterable<Person> {
    public Iterator<Person> iterator() {
        return Arrays.asList(new Person("paid for person")).iterator();
    }
}
