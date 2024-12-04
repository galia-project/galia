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

package is.galia;

import is.galia.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class CommandLineArgumentParser {

    private static final String NEWLINE = System.lineSeparator();

    private final List<CommandLineArgument> allowedArgs;

    /**
     * @param allowedArgs
     */
    CommandLineArgumentParser(CommandLineArgument... allowedArgs) {
        this.allowedArgs = List.of(allowedArgs);
    }

    /**
     * <p>Parses command-line arguments as they would be supplied to a {@code
     * main()} method.</p>
     *
     * <p>The case of user-supplied argument names does not have to match that
     * used in the {@link CommandLineArgument#name() names of the arguments} passed to the
     * {@link #CommandLineArgumentParser constructor}.</p>
     *
     * <p>{@link CommandLineArgument.Type#FLAG_ONLY Flag-only arguments} receive {@code
     * null} values.</p>
     *
     * <p>The following cases cause an {@link CommandLineArgumentException} to be
     * thrown:</p>
     *
     * <ul>
     *     <li>Unrecognized arguments</li>
     *     <li>Multiple same-named arguments</li>
     *     <li>Values supplied to flag-only arguments</li>
     *     <li>Multiple values supplied for the same argument</li>
     *     <li>Values that cannot be coerced into their corresponding {@link
     *     CommandLineArgument#type() argument's data type}</li>
     *     <li>Missing arguments that are {@link CommandLineArgument#required()
     *     required}</li>
     * </ul>
     *
     * @param userArgs Command-line arguments.
     * @return         Parsed arguments.
     * @throws CommandLineArgumentException if the given arguments are invalid.
     */
    Map<String,Object> parse(String... userArgs) {
        final Set<CommandLineArgument> requiredArgs        = Set.copyOf(requiredArgs()); // immutable
        final Set<CommandLineArgument> missingRequiredArgs = requiredArgs();
        final Map<String,Object> parsedArgs     = new HashMap<>(userArgs.length);

        CommandLineArgument currentUserArg = null;
        for (String tmpUserArg : userArgs) {
            final String userArg     = tmpUserArg.strip();
            final String flaglessArg = userArg.replaceAll("^-*", "");
            if (userArg.startsWith("-")) {
                // We are dealing with an argument name.
                // Check whether we recognize this argument.
                currentUserArg = allowedArgs.stream()
                        .filter(a -> a.name().equalsIgnoreCase(flaglessArg))
                        .findFirst()
                        .orElseThrow(() -> new CommandLineArgumentException(
                                "Unrecognized argument: -" + flaglessArg));
                // Check whether the argument has been supplied multiple times.
                if (parsedArgs.containsKey(currentUserArg.name()) &&
                        parsedArgs.get(currentUserArg.name()) != null) {
                    throw new CommandLineArgumentException(
                            "Multiple arguments supplied for " +
                                    currentUserArg.name());
                } else {
                    parsedArgs.put(currentUserArg.name(), null);
                }
            } else {
                // We are dealing with an argument value.
                final CommandLineArgument fCurrentUserArg = currentUserArg;
                allowedArgs.stream()
                        .filter(a -> a.equals(fCurrentUserArg))
                        .findFirst()
                        .ifPresent((a) -> {
                            Object normalizedValue;
                            switch (a.type()) {
                                case FLAG_ONLY ->
                                        throw new CommandLineArgumentException(
                                                "A value was provided for a flag-only argument: " +
                                                        fCurrentUserArg.name());
                                case BOOLEAN ->
                                        normalizedValue = parseBoolean(userArg);
                                case INTEGER ->
                                        normalizedValue = parseInt(userArg);
                                default ->
                                        normalizedValue = userArg;
                            }
                            if (parsedArgs.containsKey(fCurrentUserArg.name()) &&
                                    parsedArgs.get(fCurrentUserArg.name()) != null) {
                                throw new CommandLineArgumentException(
                                        "Multiple values supplied for argument: " +
                                                fCurrentUserArg.name());
                            }
                            parsedArgs.put(fCurrentUserArg.name(), normalizedValue);
                        });
            }
            missingRequiredArgs.removeIf(a -> a.name().equalsIgnoreCase(flaglessArg));
        }
        // Check that values have been supplied for all required arguments.
        requiredArgs.stream()
                .filter(a -> !CommandLineArgument.Type.FLAG_ONLY.equals(a.type()))
                .forEach(requiredArg -> {
            if (parsedArgs.get(requiredArg.name()) == null &&
                    allowedArgs.stream()
                            .filter(CommandLineArgument::independent)
                            .map(CommandLineArgument::name)
                            .noneMatch(parsedArgs::containsKey)) {
                throw new CommandLineArgumentException(
                        "No value supplied for argument -" + requiredArg.name());
            }
        });
        if (!missingRequiredArgs.isEmpty()) {
            if (!independentArgs().contains(currentUserArg)) {
                throw new CommandLineArgumentException("Missing required arguments: " +
                        missingRequiredArgs.stream().
                                map(CommandLineArgument::name).
                                collect(Collectors.joining(", ")));
            }
        }
        return parsedArgs;
    }

    /**
     * @return Help string suitable for printing to a console.
     */
    String usage() {
        final List<String> flags = new ArrayList<>(allowedArgs.size());
        final int maxNameLength = allowedArgs.stream()
                .mapToInt(a -> a.name().length())
                .max()
                .orElse(0);
        allowedArgs.forEach(arg -> {
            String name = String.format("%1$-" + (maxNameLength + 1) + "s",
                    "-" + arg.name());
            String help = arg.help();
            if (arg.required()) {
                help += " (REQUIRED)";
            }
            flags.add(name + "  " + help);
        });
        Collections.sort(flags);

        List<String> lines = new ArrayList<>(3 + flags.size());
        lines.add("Usage: java <VM args> -jar " +
                Application.getJARFile().getFileName().toString() +
                " <command args>");
        lines.add("");
        lines.add("Command arguments:");
        lines.addAll(flags);

        return String.join(NEWLINE, lines) + NEWLINE;
    }

    private boolean parseBoolean(String userArg) {
        try {
            return StringUtils.toBoolean(userArg);
        } catch (NumberFormatException e) {
            throw new CommandLineArgumentException("Not a boolean: " + userArg);
        }
    }

    private int parseInt(String userArg) {
        try {
            return Integer.parseInt(userArg);
        } catch (NumberFormatException e) {
            throw new CommandLineArgumentException("Not an integer: " + userArg);
        }
    }

    private Set<CommandLineArgument> independentArgs() {
        return allowedArgs.stream()
                .filter(CommandLineArgument::independent)
                .collect(Collectors.toSet());
    }

    private Set<CommandLineArgument> requiredArgs() {
        return allowedArgs.stream()
                .filter(CommandLineArgument::required)
                .collect(Collectors.toSet());
    }

}
