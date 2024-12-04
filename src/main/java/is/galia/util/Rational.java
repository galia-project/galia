/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.util;

import java.util.Map;

/**
 * Rational number, i.e. fraction.
 */
public record Rational(long numerator, long denominator) {

    public Rational {
        if (denominator == 0) {
            throw new IllegalArgumentException("Denominator cannot be 0");
        }
    }

    public Rational(short numerator, short denominator) {
        this(numerator, (long) denominator);
    }

    public Rational(int numerator, int denominator) {
        this(numerator, (long) denominator);
    }

    public double doubleValue() {
        return numerator() / (double) denominator();
    }

    public float floatValue() {
        return numerator() / (float) denominator();
    }

    /**
     * @return New instance reduced to lowest terms, or the same instance if
     *         it is already reduced to lowest terms.
     */
    public Rational reduced() {
        long n = numerator, d = denominator;
        while (d != 0) {
            long t = d;
            d = n % d;
            n = t;
        }
        long newNumerator = numerator / n, newDenominator = denominator / n;
        if (newNumerator != numerator) {
            return new Rational(newNumerator, newDenominator);
        }
        return this;
    }

    /**
     * @return Map with {@code numerator} and {@code denominator} keys.
     */
    public Map<String,Long> toMap() {
        return Map.of("numerator", numerator, "denominator", denominator);
    }

    @Override
    public String toString() {
        return numerator + ":" + denominator;
    }

}
