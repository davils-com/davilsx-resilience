package com.davils.resilience.ktor.timelimiter

import com.davils.resilience.timelimiter.hardTimeLimiter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class TimeLimiterPluginTest : FunSpec({
    test("allows fast requests within the configured timeout") {
        testApplication {
            val limiter = hardTimeLimiter(500.milliseconds)

            application {
                install(TimeLimiterPlugin) {
                    timeLimiter = limiter
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
        }
    }

    test("returns 504 when request handling exceeds the timeout") {
        testApplication {
            val limiter = hardTimeLimiter(50.milliseconds)

            application {
                install(TimeLimiterPlugin) {
                    timeLimiter = limiter
                }
                routing {
                    get("/slow") {
                        delay(200)
                        call.respond("ok")
                    }
                }
            }

            client.get("/slow").apply {
                status shouldBe HttpStatusCode.GatewayTimeout
                bodyAsText() shouldBe "Request timed out"
            }
        }
    }

    test("fails installation when timeLimiter is not configured") {
        shouldThrow<IllegalStateException> {
            testApplication {
                application {
                    install(TimeLimiterPlugin) { }
                }
            }
        }
    }
})
