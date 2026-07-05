package com.davils.resilience.ktor.ratelimiter

import com.davils.resilience.ratelimiter.RateLimiterStrategy
import com.davils.resilience.ratelimiter.fixedRateLimiter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RateLimiterPluginTest : FunSpec({
    test("allows requests within the configured limit") {
        testApplication {
            val limiter = fixedRateLimiter(limit = 2, period = 1.seconds) {
                timeoutDuration = Duration.ZERO
                strategy = RateLimiterStrategy.FAIL_FAST
            }

            application {
                install(RateLimiterPlugin) {
                    rateLimiter = limiter
                }
                routing {
                    get("/api") {
                        call.respond("ok")
                    }
                }
            }

            client.get("/api").apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "ok"
            }
            client.get("/api").status shouldBe HttpStatusCode.OK
        }
    }

    test("returns 429 when the limit is exceeded") {
        testApplication {
            val limiter = fixedRateLimiter(limit = 2, period = 1.seconds) {
                timeoutDuration = Duration.ZERO
                strategy = RateLimiterStrategy.FAIL_FAST
            }

            application {
                install(RateLimiterPlugin) {
                    rateLimiter = limiter
                }
                routing {
                    get("/api") {
                        call.respond("ok")
                    }
                }
            }

            client.get("/api").status shouldBe HttpStatusCode.OK
            client.get("/api").status shouldBe HttpStatusCode.OK

            client.get("/api").apply {
                status shouldBe HttpStatusCode.TooManyRequests
                bodyAsText() shouldBe "Rate limit exceeded"
            }
        }
    }

    test("includes Retry-After header when rejecting a request") {
        testApplication {
            val limiter = fixedRateLimiter(limit = 1, period = 500.seconds) {
                timeoutDuration = Duration.ZERO
                strategy = RateLimiterStrategy.FAIL_FAST
            }

            application {
                install(RateLimiterPlugin) {
                    rateLimiter = limiter
                }
                routing {
                    get("/api") {
                        call.respond("ok")
                    }
                }
            }

            client.get("/api").status shouldBe HttpStatusCode.OK

            client.get("/api").apply {
                status shouldBe HttpStatusCode.TooManyRequests
                headers["Retry-After"].shouldNotBeNull()
            }
        }
    }

    test("consumes multiple permits per request when configured") {
        testApplication {
            val limiter = fixedRateLimiter(limit = 3, period = 1.seconds) {
                timeoutDuration = Duration.ZERO
                strategy = RateLimiterStrategy.FAIL_FAST
            }

            application {
                install(RateLimiterPlugin) {
                    rateLimiter = limiter
                    permitsPerRequest = 2
                }
                routing {
                    get("/api") {
                        call.respond("ok")
                    }
                }
            }

            client.get("/api").status shouldBe HttpStatusCode.OK
            client.get("/api").status shouldBe HttpStatusCode.TooManyRequests
        }
    }

    test("fails installation when rateLimiter is not configured") {
        shouldThrow<IllegalStateException> {
            testApplication {
                application {
                    install(RateLimiterPlugin) { }
                }
            }
        }
    }
})
