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

package is.galia.status;

/**
 * <p>Application health, generally obtained from the {@link HealthChecker}.</p>
 *
 * <p>This class is thread-safe.</p>
 */
public final class Health {

    public enum Color {
        GREEN, YELLOW, RED
    }

    private Color color = Color.GREEN;
    private String message;

    public synchronized Color getColor() {
        return color;
    }

    public synchronized String getMessage() {
        return message;
    }

    /**
     * For Jackson JSON serialization.
     */
    public Color[] getPossibleColors() {
        return Color.values();
    }

    synchronized void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the minimum color to the given value. Subsequent calls cannot set
     * the color to any lower value.
     */
    public synchronized void setMinColor(Color minColor) {
        if (minColor.ordinal() > color.ordinal()) {
            this.color = minColor;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getColor());
        if (getMessage() != null) {
            builder.append(": ");
            builder.append(getMessage());
        }
        return builder.toString();
    }

}
