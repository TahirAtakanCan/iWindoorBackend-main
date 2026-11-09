package com.atablood.iWindoor_api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Spring bağlamı olmadan uygulama paketindeki tüm @RestController sınıflarını yansıma ile tarar; docs/api-routes.txt ile karşılaştırır.
 * Güncellemek için: mvnw test -Dtest=ApiRoutesTest -Dapi.routes.update=true
 */
class ApiRoutesTest {

    private static final String PACKAGE = "com.atablood.iWindoor_api"; // tüm alt paketler (auth dahil)
    private static final Path ROUTES_FILE = Paths.get("docs", "api-routes.txt");

    @Test
    void apiRoutesMatchDocs() throws Exception {
        List<String> actual = collectRoutes();
        boolean update = Boolean.parseBoolean(System.getProperty("api.routes.update", "false"));
        if (update) {
            Files.createDirectories(ROUTES_FILE.getParent());
            Files.writeString(ROUTES_FILE, String.join("\n", actual) + "\n", StandardCharsets.UTF_8);
            return;
        }
        if (!Files.exists(ROUTES_FILE)) {
            fail("docs/api-routes.txt yok. Oluşturmak için: .\\mvnw.cmd test -Dtest=ApiRoutesTest -Dapi.routes.update=true");
        }
        List<String> expected = Files.readAllLines(ROUTES_FILE, StandardCharsets.UTF_8).stream()
                .map(s -> s.replace("\uFEFF", "").trim()) // Windows editörlerinin eklediği BOM'u yok say
                .filter(s -> !s.isEmpty())
                .toList();
        if (new TreeSet<>(expected).equals(new TreeSet<>(actual))) return; // sıra önemli değil

        Set<String> exp = new LinkedHashSet<>(expected);
        Set<String> act = new LinkedHashSet<>(actual);
        List<String> added = act.stream().filter(r -> !exp.contains(r)).toList();
        List<String> removed = exp.stream().filter(r -> !act.contains(r)).toList();
        StringBuilder msg = new StringBuilder("API rota listesi docs/api-routes.txt ile uyuşmuyor.\n");
        if (!added.isEmpty()) {
            msg.append("Eklenen:\n");
            added.forEach(r -> msg.append("  + ").append(r).append('\n'));
        }
        if (!removed.isEmpty()) {
            msg.append("Çıkan:\n");
            removed.forEach(r -> msg.append("  - ").append(r).append('\n'));
        }
        msg.append("Güncellemek için: .\\mvnw.cmd test -Dtest=ApiRoutesTest -Dapi.routes.update=true");
        fail(msg.toString());
    }

    static List<String> collectRoutes() throws Exception {
        Set<String> routes = new TreeSet<>();
        for (Class<?> cls : findControllerClasses()) {
            if (cls.getAnnotation(RestController.class) == null) continue;
            String prefix = joinPaths(classMappings(cls));
            for (Method method : cls.getDeclaredMethods()) {
                for (String[] entry : methodMappings(method)) {
                    String http = entry[0];
                    String path = joinPaths(prefix, entry[1]);
                    routes.add(http + " " + normalize(path));
                }
            }
        }
        return new ArrayList<>(routes);
    }

    private static List<Class<?>> findControllerClasses() throws IOException, ClassNotFoundException {
        String path = PACKAGE.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if (!"file".equals(url.getProtocol())) continue;
            Path dir = Paths.get(URI.create(url.toString()));
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> stream = Files.walk(dir)) {
                for (Path p : stream.filter(f -> f.getFileName().toString().endsWith(".class")).toList()) {
                    String name = p.getFileName().toString();
                    if (name.contains("$")) continue;
                    String rel = dir.relativize(p).toString().replace('\\', '.').replace('/', '.');
                    String fqcn = PACKAGE + "." + rel.substring(0, rel.length() - 6);
                    try {
                        classes.add(Class.forName(fqcn, false, Thread.currentThread().getContextClassLoader()));
                    } catch (Throwable ignored) {
                        // yüklenemeyen yardımcı sınıflar rota içermez
                    }
                }
            }
        }
        return classes;
    }

    private static String[] classMappings(Class<?> cls) {
        RequestMapping rm = cls.getAnnotation(RequestMapping.class);
        if (rm == null) return new String[]{""};
        String[] paths = firstNonEmpty(rm.value(), rm.path());
        return paths.length == 0 ? new String[]{""} : paths;
    }

    private static List<String[]> methodMappings(Method method) {
        List<String[]> out = new ArrayList<>();
        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null) add(out, "GET", firstNonEmpty(get.value(), get.path()));
        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null) add(out, "POST", firstNonEmpty(post.value(), post.path()));
        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null) add(out, "PUT", firstNonEmpty(put.value(), put.path()));
        DeleteMapping del = method.getAnnotation(DeleteMapping.class);
        if (del != null) add(out, "DELETE", firstNonEmpty(del.value(), del.path()));
        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        if (patch != null) add(out, "PATCH", firstNonEmpty(patch.value(), patch.path()));
        RequestMapping rm = method.getAnnotation(RequestMapping.class);
        if (rm != null) {
            String[] paths = firstNonEmpty(rm.value(), rm.path());
            RequestMethod[] methods = rm.method();
            if (methods.length == 0) {
                add(out, "GET", paths); // nadir; varsayılan
            } else {
                for (RequestMethod m : methods) {
                    add(out, m.name(), paths);
                }
            }
        }
        return out;
    }

    private static void add(List<String[]> out, String http, String[] paths) {
        if (paths.length == 0) {
            out.add(new String[]{http, ""});
        } else {
            for (String p : paths) out.add(new String[]{http, p == null ? "" : p});
        }
    }

    private static String[] firstNonEmpty(String[] a, String[] b) {
        if (a != null && a.length > 0) return a;
        if (b != null && b.length > 0) return b;
        return new String[0];
    }

    private static String joinPaths(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            if (!part.startsWith("/")) {
                if (sb.isEmpty() || sb.charAt(sb.length() - 1) != '/') sb.append('/');
            }
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '/' && part.startsWith("/")) {
                sb.append(part.substring(1));
            } else {
                sb.append(part);
            }
        }
        if (sb.isEmpty()) return "/";
        String s = sb.toString();
        return s.startsWith("/") ? s : "/" + s;
    }

    /** /api/v1/projects/{id}/versions/{versionId} → /api/v1/projects/{}/versions/{} */
    static String normalize(String path) {
        String p = path.replaceAll("\\{[^/}]+}", "{}");
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }
}
