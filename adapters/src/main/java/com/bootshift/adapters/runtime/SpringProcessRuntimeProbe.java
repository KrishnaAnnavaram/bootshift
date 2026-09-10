package com.bootshift.adapters.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.bootshift.adapters.exec.ProcessRunner;
import com.bootshift.core.security.SensitiveValues;
import com.bootshift.core.util.Json;
import com.bootshift.ports.runtime.RuntimeProbePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime probe that actually starts the application and observes it (Agent 04 enrichment, Agent 16).
 *
 * <p>The probe launches the packaged application in a child process, waits for the Spring context to
 * report readiness, then interrogates whichever surfaces are reachable: Actuator endpoints when the
 * application exposes them, and the startup log when it does not.
 *
 * <p>Crucially it does not fake anything. A module that cannot start because MongoDB or a Config
 * Server is unavailable produces {@code started=false} and a populated {@code unobservable} list,
 * which becomes a blind spot in the evidence manifest instead of an assumed pass (R19).
 */
public final class SpringProcessRuntimeProbe implements RuntimeProbePort {

    private static final Logger LOG = LoggerFactory.getLogger(SpringProcessRuntimeProbe.class);

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private final ProcessRunner runner;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();
    private final Path logRoot;

    public SpringProcessRuntimeProbe(Path logRoot) {
        this(new ProcessRunner(), logRoot);
    }

    public SpringProcessRuntimeProbe(ProcessRunner runner, Path logRoot) {
        this.runner = runner;
        this.logRoot = logRoot;
    }

    @Override
    public String name() {
        return "spring-process-probe";
    }

    @Override
    public boolean available(Path moduleRoot) {
        return findArtifact(moduleRoot) != null;
    }

    /** Locates a packaged jar or war produced by a previous build. */
    public static Path findArtifact(Path moduleRoot) {
        Path target = moduleRoot.resolve("target");
        if (!Files.isDirectory(target)) {
            return null;
        }
        try (var stream = Files.list(target)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return (name.endsWith(".jar") || name.endsWith(".war"))
                                && !name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar")
                                && !name.endsWith(".original");
                    })
                    .max(java.util.Comparator.comparingLong(p -> p.toFile().length()))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public ProbeResult probe(Path repositoryRoot, Path moduleRoot, String moduleId,
                             Map<String, String> settings) {
        long start = System.currentTimeMillis();
        List<Observation> observations = new ArrayList<>();
        List<BoundProperty> bound = new ArrayList<>();
        List<String> unobservable = new ArrayList<>();

        Path artifact = findArtifact(moduleRoot);
        if (artifact == null) {
            unobservable.addAll(allDimensionNames(
                    "No packaged artifact for " + moduleId + "; the module was never successfully built"));
            return new ProbeResult(moduleId, false,
                    "No packaged artifact available - build the module before runtime validation",
                    observations, bound, unobservable, settings.getOrDefault("fingerprint", "unknown"),
                    System.currentTimeMillis() - start);
        }

        int port = Integer.parseInt(settings.getOrDefault("server.port", "0"));
        if (port == 0) {
            port = freePort();
        }
        String baseUrl = "http://127.0.0.1:" + port;

        List<String> command = new ArrayList<>();
        String javaExecutable = ProcessRunner.jdkTool("java");
        command.add(javaExecutable == null ? "java" : javaExecutable);
        command.add("-Dserver.port=" + port);
        command.add("-Dmanagement.server.port=" + port);
        command.add("-Dmanagement.endpoints.web.exposure.include=*");
        command.add("-Dmanagement.endpoint.health.show-details=always");
        command.add("-Dmanagement.endpoint.configprops.show-values=NEVER");
        command.add("-Dspring.cloud.config.enabled=false");
        command.add("-Dspring.cloud.config.fail-fast=false");
        command.add("-Dspring.main.banner-mode=off");
        command.add("-Duser.timezone=UTC");
        command.add("-Dfile.encoding=UTF-8");
        settings.forEach((key, value) -> {
            if (key.startsWith("jvm.arg.")) {
                command.add(value);
            } else if (key.startsWith("spring.") || key.startsWith("eureka.") || key.startsWith("management.")) {
                command.add("-D" + key + "=" + value);
            }
        });
        String profile = settings.get("spring.profiles.active");
        if (profile != null) {
            command.add("-Dspring.profiles.active=" + profile);
        }
        command.add("-jar");
        command.add(artifact.toAbsolutePath().toString());

        Path logFile = logRoot.resolve(moduleId.replaceAll("[^A-Za-z0-9._-]", "_") + "-runtime.log");
        ProcessHandleResult handle = launch(command, moduleRoot, logFile);
        if (handle.process() == null) {
            unobservable.addAll(allDimensionNames("Process could not be started: " + handle.failure()));
            return new ProbeResult(moduleId, false, handle.failure(), observations, bound, unobservable,
                    settings.getOrDefault("fingerprint", "unknown"), System.currentTimeMillis() - start);
        }

        boolean started = false;
        String failureReason = null;
        try {
            started = waitForReadiness(baseUrl, handle.process(), logFile);
            if (started) {
                observations.add(new Observation(Dimension.CONTEXT, moduleId,
                        "Spring application context started and responded on " + baseUrl,
                        Map.of("base_url", baseUrl, "artifact", artifact.getFileName().toString()),
                        null, true));
                collect(baseUrl, "/actuator/health", Dimension.HEALTH, moduleId, observations, unobservable);
                collect(baseUrl, "/actuator/beans", Dimension.BEANS, moduleId, observations, unobservable);
                collect(baseUrl, "/actuator/conditions", Dimension.CONDITIONS, moduleId, observations, unobservable);
                collect(baseUrl, "/actuator/env", Dimension.CONFIG_BINDING, moduleId, observations, unobservable);
                collect(baseUrl, "/actuator/mappings", Dimension.REQUEST_MAPPINGS, moduleId, observations, unobservable);
                collectConfigProps(baseUrl, moduleId, observations, bound, unobservable);
            } else {
                failureReason = summarizeFailure(logFile, handle.process());
                unobservable.addAll(allDimensionNames(
                        "Application did not reach readiness: " + failureReason));
            }
        } finally {
            handle.process().destroy();
            try {
                if (!handle.process().waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    handle.process().destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handle.process().destroyForcibly();
            }
        }

        return new ProbeResult(moduleId, started, failureReason, observations, bound, unobservable,
                settings.getOrDefault("fingerprint", "unknown"), System.currentTimeMillis() - start);
    }

    private record ProcessHandleResult(Process process, String failure) {
    }

    private ProcessHandleResult launch(List<String> command, Path workingDirectory, Path logFile) {
        try {
            Files.createDirectories(logFile.getParent());
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());
            builder.environment().put("TZ", "UTC");
            builder.environment().put("LANG", "C");
            return new ProcessHandleResult(builder.start(), null);
        } catch (IOException e) {
            return new ProcessHandleResult(null, e.getMessage());
        }
    }

    private boolean waitForReadiness(String baseUrl, Process process, Path logFile) {
        Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                return false;
            }
            if (get(baseUrl + "/actuator/health").isPresent()) {
                return true;
            }
            if (get(baseUrl + "/").isPresent()) {
                return true;
            }
            if (logIndicatesStarted(logFile)) {
                return get(baseUrl + "/actuator/health").isPresent() || get(baseUrl + "/").isPresent();
            }
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean logIndicatesStarted(Path logFile) {
        try {
            if (!Files.isRegularFile(logFile)) {
                return false;
            }
            String text = Files.readString(logFile, StandardCharsets.UTF_8);
            return text.contains("Started ") && text.contains(" in ") && text.contains(" seconds");
        } catch (IOException e) {
            return false;
        }
    }

    private java.util.Optional<String> get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500
                    ? java.util.Optional.of(response.body()) : java.util.Optional.empty();
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private void collect(String baseUrl, String path, Dimension dimension, String moduleId,
                         List<Observation> observations, List<String> unobservable) {
        java.util.Optional<String> body = get(baseUrl + path);
        if (body.isEmpty()) {
            unobservable.add(dimension.name() + ": " + path + " not exposed by " + moduleId);
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            JsonNode node = Json.parse(body.get());
            data.put("summary", summarize(dimension, node));
            data.put("raw_hash", com.bootshift.core.util.Hashing.sha256(body.get()));
        } catch (RuntimeException e) {
            data.put("raw_hash", com.bootshift.core.util.Hashing.sha256(body.get()));
        }
        observations.add(new Observation(dimension, moduleId, path, data, null, true));
    }

    private void collectConfigProps(String baseUrl, String moduleId, List<Observation> observations,
                                    List<BoundProperty> bound, List<String> unobservable) {
        java.util.Optional<String> body = get(baseUrl + "/actuator/configprops");
        if (body.isEmpty()) {
            unobservable.add(Dimension.CONFIG_BINDING.name()
                    + ": /actuator/configprops not exposed; bound-property provenance is unavailable "
                    + "for " + moduleId + ", so PROPERTY_SILENTLY_IGNORED cannot be confirmed at runtime");
            return;
        }
        JsonNode root = Json.parse(body.get());
        root.path("contexts").fields().forEachRemaining(context ->
                context.getValue().path("beans").fields().forEachRemaining(bean -> {
                    String prefix = bean.getValue().path("prefix").asText("");
                    JsonNode properties = bean.getValue().path("properties");
                    properties.fields().forEachRemaining(property -> {
                        String key = prefix.isEmpty() ? property.getKey() : prefix + "." + property.getKey();
                        boolean sensitive = SensitiveValues.isSensitiveKey(key);
                        bound.add(new BoundProperty(key, null, "CONFIGURATION_PROPERTIES",
                                bean.getValue().path("prefix").asText(null), property.getKey(),
                                true, property.getValue().isNull(), false, null, sensitive));
                    });
                }));
        observations.add(new Observation(Dimension.CONFIG_BINDING, moduleId,
                "/actuator/configprops", Map.of("bound_property_count", bound.size()), null, true));
    }

    private String summarize(Dimension dimension, JsonNode node) {
        return switch (dimension) {
            case BEANS -> "contexts=" + node.path("contexts").size();
            case HEALTH -> "status=" + node.path("status").asText("UNKNOWN");
            case REQUEST_MAPPINGS -> "contexts=" + node.path("contexts").size();
            case CONDITIONS -> "contexts=" + node.path("contexts").size();
            default -> "fields=" + node.size();
        };
    }

    private String summarizeFailure(Path logFile, Process process) {
        try {
            if (!Files.isRegularFile(logFile)) {
                return "no startup log produced (exit=" + exitCodeOf(process) + ")";
            }
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0 && i > lines.size() - 200; i--) {
                String line = lines.get(i);
                if (line.contains("APPLICATION FAILED TO START") || line.contains("Caused by:")
                        || line.contains("Error creating bean") || line.contains("Exception")) {
                    return SensitiveValues.redactLine(null, line.trim());
                }
            }
            return "context did not become ready within " + STARTUP_TIMEOUT
                    + " (exit=" + exitCodeOf(process) + ")";
        } catch (IOException e) {
            return "startup log unreadable: " + e.getMessage();
        }
    }

    private static String exitCodeOf(Process process) {
        try {
            return process.isAlive() ? "running" : String.valueOf(process.exitValue());
        } catch (IllegalThreadStateException e) {
            return "running";
        }
    }

    private static List<String> allDimensionNames(String reason) {
        List<String> result = new ArrayList<>();
        for (Dimension dimension : Dimension.values()) {
            result.add(dimension.name() + ": " + reason);
        }
        return result;
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 18080 + (int) (System.nanoTime() % 1000);
        }
    }
}
