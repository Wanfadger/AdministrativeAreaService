package com.wanfadger.AdministrativeareaApi.golden;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.fail;

/**
 * Byte-for-byte response comparison against committed golden files.
 *
 * <p>These files are the contract. The Angular frontend — and every service about to integrate —
 * depends on the exact response shape and on the exact {@code detail} string of every error
 * (components render {@code err.error.detail} verbatim). The service is about to be rewritten from
 * six copy-pasted switch branches into one generic code path; these files are the only thing that
 * proves the rewrite changed no observable behaviour.
 *
 * <p>Regenerate with {@code -Dgolden.update=true}. Review the resulting diff carefully: a change
 * here is a change to a published API contract, never an incidental test fixup.
 */
final class GoldenFiles {

    private static final Path DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private GoldenFiles() {
    }

    static void assertMatches(String name, String actualJson) {
        Path file = DIR.resolve(name + ".json");
        try {
            if (UPDATE) {
                Files.createDirectories(DIR);
                Files.writeString(file, pretty(actualJson), StandardCharsets.UTF_8);
                return;
            }
            if (!Files.exists(file)) {
                fail("Missing golden file %s — regenerate with -Dgolden.update=true".formatted(file));
            }
            String expected = Files.readString(file, StandardCharsets.UTF_8);
            JSONAssert.assertEquals(
                    "Response no longer matches the frozen contract in %s".formatted(file),
                    expected, actualJson, JSONCompareMode.STRICT);
        } catch (IOException e) {
            throw new UncheckedGoldenException(file, e);
        } catch (org.json.JSONException e) {
            throw new UncheckedGoldenException(file, e);
        }
    }

    /** Stable 2-space formatting so the committed files stay reviewable in a diff. */
    private static String pretty(String json) throws org.json.JSONException {
        String trimmed = json.trim();
        return trimmed.startsWith("[")
                ? new org.json.JSONArray(trimmed).toString(2)
                : new org.json.JSONObject(trimmed).toString(2);
    }

    static final class UncheckedGoldenException extends RuntimeException {
        UncheckedGoldenException(Path file, Throwable cause) {
            super("Golden file failure for " + file, cause);
        }
    }
}
