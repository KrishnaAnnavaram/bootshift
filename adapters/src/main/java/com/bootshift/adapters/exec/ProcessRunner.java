package com.bootshift.adapters.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Isolated execution of repository-controlled tools (spec section 48).
 *
 * <p>Repository code is untrusted: Maven plugins, Gradle scripts, annotation processors and tests all
 * execute arbitrary code. This runner therefore enforces a command allowlist, a timeout, an output
 * cap, and an explicit working directory.
 *
 * <p>Arguments are always passed as a vector, never as a command string, so there is no parsing step
 * an argument can escape from. On Windows a {@code .cmd} or {@code .bat} wrapper cannot be launched
 * directly by {@code ProcessBuilder} and is prefixed with {@code cmd.exe /c} by {@link #shellWrap};
 * the allowlist is checked against the <em>real</em> target before that wrapping happens, and
 * {@code cmd.exe} is deliberately absent from the allowlist so it can never be a caller's target.
 */
public final class ProcessRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessRunner.class);

    /** Executables the harness is permitted to launch. Anything else is refused. */
    private static final Set<String> DEFAULT_ALLOWLIST = Set.of(
            "mvn", "mvn.cmd", "mvnw", "mvnw.cmd", "gradle", "gradle.bat", "gradlew", "gradlew.bat",
            "java", "java.exe", "javap", "javap.exe", "jdeps", "jdeps.exe", "jdeprscan", "jdeprscan.exe",
            "git", "git.exe");
    // cmd.exe is NOT listed. shellWrap adds it after assertAllowed has already checked the real
    // target, so allowlisting it would buy nothing and would let a caller pass cmd.exe /c <anything>
    // as its own command - which is the whole allowlist, undone.

    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    public record Result(int exitCode, List<String> stdout, List<String> stderr, Duration duration,
                         boolean timedOut, String command) {

        public boolean success() {
            return exitCode == 0 && !timedOut;
        }

        public String stdoutText() {
            return String.join(System.lineSeparator(), stdout);
        }

        public String stderrText() {
            return String.join(System.lineSeparator(), stderr);
        }

        public List<String> tail(List<String> lines, int count) {
            return lines.size() <= count ? lines : lines.subList(lines.size() - count, lines.size());
        }
    }

    private final Set<String> allowlist;
    private final int maxOutputLines;

    public ProcessRunner() {
        this(DEFAULT_ALLOWLIST, 20000);
    }

    public ProcessRunner(Set<String> allowlist, int maxOutputLines) {
        this.allowlist = allowlist;
        this.maxOutputLines = maxOutputLines;
    }

    public static Set<String> defaultAllowlist() {
        return DEFAULT_ALLOWLIST;
    }

    /** Refuses any executable outside the allowlist before a process is created. */
    public void assertAllowed(String executable) {
        String name = Path.of(executable).getFileName().toString().toLowerCase(Locale.ROOT);
        if (!allowlist.contains(name)) {
            throw new SecurityException("Execution refused: " + name + " is not on the command allowlist");
        }
    }

    public Result run(List<String> command, Path workingDirectory, Duration timeout,
                      Map<String, String> environment) {
        return run(command, workingDirectory, timeout, environment, null);
    }

    /**
     * Runs a command, streaming output to an optional log sink so a long build leaves evidence even
     * if it is later killed by the timeout.
     */
    public Result run(List<String> command, Path workingDirectory, Duration timeout,
                      Map<String, String> environment, Path logSink) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }
        assertAllowed(command.get(0));

        ProcessBuilder builder = new ProcessBuilder(shellWrap(command));
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(false);
        Map<String, String> env = new LinkedHashMap<>(environment);
        builder.environment().putAll(env);
        // Deterministic locale and timezone: part of the environment equivalence contract.
        builder.environment().put("LANG", "C");
        builder.environment().put("LC_ALL", "C");

        Instant start = Instant.now();
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            return new Result(-1, List.of(), List.of("Cannot start process: " + e.getMessage()),
                    Duration.between(start, Instant.now()), false, String.join(" ", command));
        }

        Thread outThread = drain(process.getInputStream(), stdout);
        Thread errThread = drain(process.getErrorStream(), stderr);

        boolean timedOut = false;
        int exitCode;
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                timedOut = true;
                process.destroyForcibly();
                process.waitFor(15, TimeUnit.SECONDS);
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            exitCode = -1;
        } catch (IllegalThreadStateException e) {
            exitCode = -1;
        }

        join(outThread);
        join(errThread);

        Result result = new Result(exitCode, List.copyOf(stdout), List.copyOf(stderr),
                Duration.between(start, Instant.now()), timedOut, String.join(" ", command));
        if (logSink != null) {
            writeLog(logSink, result);
        }
        if (timedOut) {
            LOG.warn("Command timed out after {}: {}", timeout, result.command());
        }
        return result;
    }

    private Thread drain(java.io.InputStream stream, List<String> sink) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (sink) {
                        if (sink.size() < maxOutputLines) {
                            sink.add(line);
                        } else if (sink.size() == maxOutputLines) {
                            sink.add("... output truncated at " + maxOutputLines + " lines ...");
                        }
                    }
                }
            } catch (IOException ignored) {
                // stream closed by process termination
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void join(Thread thread) {
        try {
            thread.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeLog(Path logSink, Result result) {
        try {
            Files.createDirectories(logSink.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("$ ").append(result.command()).append(System.lineSeparator());
            sb.append("exit=").append(result.exitCode())
                    .append(" timedOut=").append(result.timedOut())
                    .append(" duration=").append(result.duration()).append(System.lineSeparator());
            sb.append("--- stdout ---").append(System.lineSeparator());
            result.stdout().forEach(l -> sb.append(l).append(System.lineSeparator()));
            sb.append("--- stderr ---").append(System.lineSeparator());
            result.stderr().forEach(l -> sb.append(l).append(System.lineSeparator()));
            Files.writeString(logSink, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Cannot persist execution log {}: {}", logSink, e.getMessage());
        }
    }

    /**
     * Windows batch launchers ({@code mvnw.cmd}, {@code gradlew.bat}) are not directly executable by
     * {@code CreateProcess}, so they are dispatched through {@code cmd.exe /c}. The argument vector
     * is preserved rather than flattened into a shell string, so nothing is re-parsed.
     */
    static List<String> shellWrap(List<String> command) {
        if (!WINDOWS) {
            return command;
        }
        String executable = command.get(0).toLowerCase(Locale.ROOT);
        if (!executable.endsWith(".cmd") && !executable.endsWith(".bat")) {
            return command;
        }
        List<String> wrapped = new ArrayList<>();
        wrapped.add("cmd.exe");
        wrapped.add("/c");
        wrapped.addAll(command);
        return wrapped;
    }

    /** Resolves an executable on PATH, honouring Windows extensions. */
    public static String which(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        List<String> candidates = windows
                ? List.of(name + ".cmd", name + ".exe", name + ".bat", name)
                : List.of(name);
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            for (String candidate : candidates) {
                Path p = Path.of(dir).resolve(candidate);
                if (Files.isRegularFile(p)) {
                    return p.toAbsolutePath().toString();
                }
            }
        }
        return null;
    }

    /** Locates the JDK tool of the given name next to the running JVM. */
    public static String jdkTool(String name) {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            Path tool = Path.of(javaHome, "bin", windows ? name + ".exe" : name);
            if (Files.isRegularFile(tool)) {
                return tool.toAbsolutePath().toString();
            }
        }
        return which(name);
    }
}
