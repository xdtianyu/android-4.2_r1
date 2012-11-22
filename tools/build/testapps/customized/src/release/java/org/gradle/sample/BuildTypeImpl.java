package org.gradle.sample;

public class BuildTypeImpl implements BuildType {
    public String getBuildType() {
        return "release";
    }
}
