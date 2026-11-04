package app.yomi.reader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryDirectoryTest {
    @Test fun acceptsGithubIndexUrlsOnly() {
        val url = "https://raw.githubusercontent.com/user/repository/main/index.min.json"
        assertEquals(url, ExtensionRepositoryDirectory.validatedUrl(url))
    }
    @Test fun rejectsNonHttps() {
        assertThrows(IllegalArgumentException::class.java) {
            ExtensionRepositoryDirectory.validatedUrl("http://example.com/index.min.json")
        }
    }
    @Test fun rejectsLocalhostAndCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            ExtensionRepositoryDirectory.validatedUrl("https://localhost/index.min.json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExtensionRepositoryDirectory.validatedUrl("https://name:token@github.com/a/index.json")
        }
    }
    @Test fun builtInRepositoriesHaveUniqueUrls() {
        assertTrue(ExtensionRepositoryDirectory.defaults.size >= 3)
        assertEquals(
            ExtensionRepositoryDirectory.defaults.size,
            ExtensionRepositoryDirectory.defaults.map { it.url }.distinct().size
        )
    }
}
