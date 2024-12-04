package is.galia.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Software version supporting {@literal n.n.n-qualifier} syntax.
 *
 * @param major     Major version.
 * @param minor     Minor version.
 * @param patch     Patch version.
 * @param qualifier String following the version, such as {@code rc1}, {@code
 *                  -SNAPSHOT}, etc.
 */
public record SoftwareVersion(int major, int minor, int patch,
                              String qualifier) implements Comparable<SoftwareVersion> {

    /**
     * @param version Version string.
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static SoftwareVersion parse(String version) {
        // Parse major, minor, patch
        Pattern pattern = Pattern.compile("^(\\d+).?(\\d+)?.?(\\d+)?");
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            int major, minor = 0, patch = 0;
            String qualifier = null;
            major = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                minor = Integer.parseInt(matcher.group(2));
                if (matcher.group(3) != null) {
                    patch = Integer.parseInt(matcher.group(3));
                }
            }
            // Parse qualifier
            pattern = Pattern.compile("(-.*)$");
            matcher = pattern.matcher(version);
            if (matcher.find()) {
                qualifier = matcher.group(1);
            }
            return new SoftwareVersion(major, minor, patch, qualifier);
        }
        throw new IllegalArgumentException("Unable to parse version: " + version);
    }

    /**
     * Creates an {@literal n.0} instance.
     *
     * @param major Major version.
     */
    public SoftwareVersion(int major) {
        this(major, 0, 0, null);
    }

    /**
     * Creates an {@literal n.n} instance.
     *
     * @param major Major version.
     * @param minor Minor version.
     */
    public SoftwareVersion(int major, int minor) {
        this(major, minor, 0, null);
    }

    /**
     * Creates an {@literal n.n.n} instance.
     *
     * @param major Major version.
     * @param minor Minor version.
     * @param patch Patch version.
     */
    public SoftwareVersion(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }

    /**
     * Behaves similarly to {@link #isGreaterThan(SoftwareVersion)} except
     * returns {@code 0} for equal instances.
     *
     * @param other The object to be compared.
     * @return Whether the instance is greater than, equal to, or less than the
     *         given instance.
     */
    @Override
    public int compareTo(SoftwareVersion other) {
        if (this.equals(other)) {
            return 0;
        }
        return isGreaterThan(other) ? 1 : -1;
    }

    /**
     * @param other Instance to compare against.
     * @return Whether the instance is greater than the given version.
     */
    public boolean isGreaterThan(SoftwareVersion other) {
        if (major > other.major) {
            return true;
        } else if (major == other.major) {
            if (minor > other.minor) {
                return true;
            } else if (minor == other.minor) {
                if (patch > other.patch) {
                    return true;
                } else if (patch == other.patch) {
                    if (qualifier == null && other.qualifier != null) {
                        return true;
                    } else if (qualifier != null && other.qualifier != null) {
                        Pattern pattern     = Pattern.compile("(\\d+)");
                        Matcher thisMatcher = pattern.matcher(qualifier);
                        Matcher thatMatcher = pattern.matcher(other.qualifier);
                        if (thisMatcher.find() && thatMatcher.find()) {
                            int thisNum = Integer.parseInt(thisMatcher.group(1));
                            int thatNum = Integer.parseInt(thatMatcher.group(1));
                            return thisNum > thatNum;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(major);
        builder.append(".");
        builder.append(minor);
        if (patch > 0) {
            builder.append(".");
            builder.append(patch);
        }
        if (qualifier != null) {
            builder.append(qualifier);
        }
        return builder.toString();
    }

}
