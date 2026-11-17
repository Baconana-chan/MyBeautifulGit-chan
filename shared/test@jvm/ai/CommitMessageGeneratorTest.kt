package org.baconana.mybeautifulgit_chan.ai

import org.baconana.mybeautifulgit_chan.platform.AppSettings
import org.baconana.mybeautifulgit_chan.platform.CredentialStore
import java.io.BufferedReader
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CommitMessageGeneratorTest {

    @Test
    fun `sensitive files and obvious secrets never reach the prompt diff`() {
        val diff = """
            diff --git a/.env b/.env
            +API_KEY=super-secret-value
            diff --git a/src/App.kt b/src/App.kt
            @@ -1 +1 @@
            +val password = "hunter2"
            +val token = "abcdefghijkl"
            +Authorization: Bearer abcdefghijklmnop
            +Authorization: Basic dXNlcjpwYXNz
            diff --git a/logo.png b/logo.png
            Binary files a/logo.png and b/logo.png differ
        """.trimIndent()

        val sanitized = CommitMessageGenerator.sanitizeDiff(diff, 5_000)

        assertFalse(sanitized.diff.contains(".env"))
        assertFalse(sanitized.diff.contains("super-secret-value"))
        assertFalse(sanitized.diff.contains("hunter2"))
        assertFalse(sanitized.diff.contains("abcdefghijkl"))
        assertFalse(sanitized.diff.contains("abcdefghijklmnop"))
        assertFalse(sanitized.diff.contains("dXNlcjpwYXNz"))
        assertTrue(sanitized.diff.contains("src/App.kt"))
        assertEquals(2, sanitized.omittedSections)
        assertTrue(sanitized.redacted)
    }

    @Test
    fun `diff is bounded at a line boundary and marked as truncated`() {
        val diff = buildString {
            repeat(200) { appendLine("+line $it with enough text to grow the diff") }
        }

        val sanitized = CommitMessageGenerator.sanitizeDiff(diff, 1_000)

        assertTrue(sanitized.truncated)
        assertTrue(sanitized.diff.length <= 1_000)
        assertTrue(sanitized.diff.endsWith("[... diff truncated ...]"))
    }

    @Test
    fun `prompt includes branch, style references and the untrusted-data instruction`() {
        val prompts = CommitMessageGenerator.buildPrompts(
            config = AiSettings(
                messageLanguage = AiMessageLanguage.English,
                style = AiCommitStyle.Conventional,
            ),
            branch = "feature/ai",
            recentSubjects = listOf("feat: add parser", "fix: handle empty input"),
            diff = "+fun answer() = 42",
        )

        assertTrue(prompts.first.contains("untrusted data"))
        assertTrue(prompts.first.contains("Conventional Commits"))
        assertTrue(prompts.first.contains("English"))
        assertTrue(prompts.second.contains("feature/ai"))
        assertTrue(prompts.second.contains("feat: add parser"))
        assertTrue(prompts.second.contains("+fun answer() = 42"))
    }

    @Test
    fun `response sanitizer removes wrappers and keeps one short line`() {
        assertEquals(
            "feat: add AI commit generation",
            CommitMessageGenerator.sanitizeCommitMessage(
                "```text\nfeat: add AI commit generation\n```",
            ),
        )
        assertEquals(
            "fix: handle empty diff",
            CommitMessageGenerator.sanitizeCommitMessage("Commit message: \"fix: handle empty diff\""),
        )
        assertNull(CommitMessageGenerator.sanitizeCommitMessage("   \n```\n```"))
    }

    @Test
    fun `chat completion parser supports string and text-part content`() {
        assertEquals(
            "feat: add tests",
            CommitMessageGenerator.parseCompletionContent(
                """{"choices":[{"message":{"content":"feat: add tests"}}]}""",
            ),
        )
        assertEquals(
            "fix: parse content",
            CommitMessageGenerator.parseCompletionContent(
                """{"choices":[{"message":{"content":[{"type":"text","text":"fix: parse content"}]}}]}""",
            ),
        )
        assertNull(CommitMessageGenerator.parseCompletionContent("not json"))
        assertNull(CommitMessageGenerator.parseCompletionContent("""{"choices":[]}"""))
    }

    @Test
    fun `endpoints require https except explicitly allowed loopback http`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            CommitMessageGenerator.validateEndpoint(
                "https://api.openai.com/v1/chat/completions",
                allowLocalHttp = false,
            ).toString(),
        )
        assertFailsWith<AiGenerationException> {
            CommitMessageGenerator.validateEndpoint(
                "http://api.example.com/v1/chat/completions",
                allowLocalHttp = true,
            )
        }
        assertEquals(
            "http://127.0.0.1:11434/v1/chat/completions",
            CommitMessageGenerator.validateEndpoint(
                "http://127.0.0.1:11434/v1/chat/completions",
                allowLocalHttp = true,
            ).toString(),
        )
        assertFailsWith<AiGenerationException> {
            CommitMessageGenerator.validateEndpoint(
                "https://user:password@api.example.com/v1/chat/completions",
                allowLocalHttp = false,
            )
        }
    }

    @Test
    fun `local OpenAI-compatible server receives the prepared request`() = runBlocking {
        FakeAiServer(
            response = """{"choices":[{"message":{"content":"feat: wire AI commit messages"}}]}""",
        ).use { server ->
            val fixture = fixture(server.endpoint)
            val prepared = fixture.generator.prepare(
                diff = "diff --git a/a.txt b/a.txt\n+new line",
                branch = "main",
                recentSubjects = listOf("feat: initial"),
                fileCount = 1,
            )

            val result = fixture.generator.generate(prepared)

            assertEquals("feat: wire AI commit messages", result)
            val request = assertNotNull(server.requests.firstOrNull())
            assertEquals("/v1/chat/completions", request.path)
            assertEquals("Bearer test-api-key", request.headers["authorization"])
            assertTrue(request.body.contains("diff --git a/a.txt"))
            assertTrue(request.body.contains("main"))
            assertFalse(request.body.contains("test-api-key"))
        }
    }

    @Test
    fun `request timeout is reported as a generation failure`() = runBlocking {
        FakeAiServer(
            response = """{"choices":[{"message":{"content":"too late"}}]}""",
            delayMs = 1_000,
        ).use { server ->
            val fixture = fixture(server.endpoint, requestTimeout = Duration.ofMillis(200))
            val prepared = fixture.generator.prepare(
                diff = "+line",
                branch = "main",
                recentSubjects = emptyList(),
                fileCount = 1,
            )

            assertFailsWith<AiGenerationException> {
                fixture.generator.generate(prepared)
            }
            Unit
        }
    }

    private fun fixture(endpoint: String, requestTimeout: Duration = Duration.ofSeconds(2)): Fixture {
        val dir = Files.createTempDirectory("git-chan-ai")
        val settings = AppSettings(dir.resolve("settings.properties"))
        settings.saveAiSettings(
            AiSettings(
                enabled = true,
                endpoint = endpoint,
                model = "test-model",
                allowLocalHttp = true,
            ),
        )
        val credentials = CredentialStore(dir.resolve("ai.bin"), dir.resolve("ai.key"))
        credentials.saveToken("test-api-key")
        return Fixture(
            generator = CommitMessageGenerator(settings, credentials, requestTimeout = requestTimeout),
        )
    }

    private data class Fixture(val generator: CommitMessageGenerator)

    private data class RecordedRequest(
        val path: String,
        val headers: Map<String, String>,
        val body: String,
    )

    /** Minimal HTTP/1.1 server used instead of a real provider in tests. */
    private class FakeAiServer(
        private val response: String,
        private val delayMs: Long = 0,
    ) : AutoCloseable {
        private val server = ServerSocket(
            0,
            50,
            InetAddress.getByName("127.0.0.1"),
        )
        val requests = CopyOnWriteArrayList<RecordedRequest>()
        private val worker = thread(name = "fake-ai-server", isDaemon = true) {
            runCatching {
                while (!server.isClosed) {
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val reader: BufferedReader = socket.getInputStream()
                            .bufferedReader(Charsets.UTF_8)
                        val requestLine = reader.readLine() ?: return@use
                        val path = requestLine.split(' ').getOrNull(1).orEmpty()
                        val headers = buildMap {
                            while (true) {
                                val line = reader.readLine() ?: break
                                if (line.isEmpty()) break
                                val colon = line.indexOf(':')
                                if (colon > 0) {
                                    put(
                                        line.substring(0, colon).trim().lowercase(),
                                        line.substring(colon + 1).trim(),
                                    )
                                }
                            }
                        }
                        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                        val body = if (contentLength > 0) {
                            val chars = CharArray(contentLength)
                            var offset = 0
                            while (offset < contentLength) {
                                val read = reader.read(chars, offset, contentLength - offset)
                                if (read < 0) break
                                offset += read
                            }
                            String(chars, 0, offset)
                        } else {
                            ""
                        }
                        requests += RecordedRequest(path, headers, body)
                        if (delayMs > 0) Thread.sleep(delayMs)
                        val bodyBytes = response.toByteArray(Charsets.UTF_8)
                        val head = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: ${bodyBytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                        socket.getOutputStream().apply {
                            write(head.toByteArray(Charsets.US_ASCII))
                            write(bodyBytes)
                            flush()
                        }
                    }
                }
            }
        }

        val endpoint: String
            get() = "http://127.0.0.1:${server.localPort}/v1/chat/completions"

        override fun close() {
            server.close()
            worker.join(1_000)
        }
    }
}
