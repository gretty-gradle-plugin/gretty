package org.akhikhl.gretty.internal.integrationTests;

import java.util.Objects;

public class AnyJavaVersion implements Comparable<AnyJavaVersion> {
    private int majorVersion;

    private AnyJavaVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public boolean isJava9Compatible() {
        return majorVersion >= 9;
    }

    public boolean isJava10Compatible() {
        return majorVersion >= 10;
    }

    @Override
    public int compareTo(AnyJavaVersion o) {
        return Integer.compare(this.majorVersion, o.majorVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnyJavaVersion that = (AnyJavaVersion) o;
        return majorVersion == that.majorVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(majorVersion);
    }

    public static AnyJavaVersion of(Integer integer) {
        return new AnyJavaVersion(Objects.requireNonNull(integer));
    }
}