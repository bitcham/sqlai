package com.sqlai.domain.datasource

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

/**
 * Unit tests for DatabaseMetadata entity
 * Tests needsResync business logic and sync timestamp management
 */
@SpringBootTest
class DatabaseMetadataTest : FunSpec({

    context("DatabaseMetadata creation") {
        test("should create valid metadata") {
            // given & when
            val metadata = DatabaseMetadata(
                schemaName = "public"
            )

            // then
            metadata.schemaName shouldBe "public"
            metadata.id shouldBe null // Not persisted yet
        }

        test("should set lastSyncedAt to current time by default") {
            // given
            val before = LocalDateTime.now()

            // when
            val metadata = DatabaseMetadata(
                schemaName = "public"
            )

            // then
            val after = LocalDateTime.now()
            (metadata.lastSyncedAt >= before) shouldBe true
            (metadata.lastSyncedAt <= after) shouldBe true
        }

        test("should allow custom lastSyncedAt") {
            // given
            val customTime = LocalDateTime.of(2024, 1, 1, 12, 0)

            // when
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = customTime
            )

            // then
            metadata.lastSyncedAt shouldBe customTime
        }

        test("should throw exception when schemaName is blank") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                DatabaseMetadata(
                    schemaName = "   "
                )
            }
            exception.message shouldContain "Schema name must not be blank"
        }

        test("should throw exception when schemaName is empty") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                DatabaseMetadata(
                    schemaName = ""
                )
            }
            exception.message shouldContain "Schema name must not be blank"
        }
    }

    context("needsResync() method") {
        test("should return false when last synced just now") {
            // given
            val metadata = DatabaseMetadata(
                schemaName = "public"
                // lastSyncedAt defaults to now
            )

            // when
            val needsResync = metadata.needsResync()

            // then
            needsResync shouldBe false
        }

        test("should return false when last synced within threshold (default 24 hours)") {
            // given - Synced 12 hours ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusHours(12)
            )

            // when
            val needsResync = metadata.needsResync()

            // then
            needsResync shouldBe false
        }

        test("should return true when last synced exactly 24 hours ago") {
            // given
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusHours(24)
            )

            // when
            val needsResync = metadata.needsResync()

            // then
            needsResync shouldBe true
        }

        test("should return true when last synced more than 24 hours ago") {
            // given - Synced 48 hours ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusHours(48)
            )

            // when
            val needsResync = metadata.needsResync()

            // then
            needsResync shouldBe true
        }

        test("should return true when last synced days ago") {
            // given - Synced 7 days ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusDays(7)
            )

            // when
            val needsResync = metadata.needsResync()

            // then
            needsResync shouldBe true
        }

        test("should support custom threshold - 1 hour") {
            // given - Synced 30 minutes ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusMinutes(30)
            )

            // when
            val needsResync = metadata.needsResync(thresholdHours = 1)

            // then
            needsResync shouldBe false
        }

        test("should support custom threshold - synced exactly at threshold") {
            // given - Synced exactly 6 hours ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusHours(6)
            )

            // when
            val needsResync = metadata.needsResync(thresholdHours = 6)

            // then
            needsResync shouldBe true
        }

        test("should support custom threshold - synced beyond threshold") {
            // given - Synced 10 hours ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusHours(10)
            )

            // when
            val needsResync = metadata.needsResync(thresholdHours = 6)

            // then
            needsResync shouldBe true
        }

        test("should support very short threshold - 1 minute") {
            // given - Synced 2 minutes ago
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusMinutes(2)
            )

            // when - Threshold is 0 hours (actually checks minutes/seconds)
            // Since 2 minutes > 0 hours, should need resync
            val needsResync = metadata.needsResync(thresholdHours = 0)

            // then
            needsResync shouldBe true
        }

        test("should handle edge case - synced in future (clock skew)") {
            // given - lastSyncedAt is in the future due to clock skew
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().plusHours(1)
            )

            // when
            val needsResync = metadata.needsResync()

            // then - Negative hours, should not need resync
            needsResync shouldBe false
        }
    }

    context("markAsSynced() method") {
        test("should update lastSyncedAt to current time") {
            // given - Old sync time
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusDays(7)
            )
            val oldSyncTime = metadata.lastSyncedAt

            // Verify it needs resync
            metadata.needsResync() shouldBe true

            // when
            val before = LocalDateTime.now()
            metadata.markAsSynced()
            val after = LocalDateTime.now()

            // then
            (metadata.lastSyncedAt >= before) shouldBe true
            (metadata.lastSyncedAt <= after) shouldBe true
            (metadata.lastSyncedAt > oldSyncTime) shouldBe true
            metadata.needsResync() shouldBe false
        }

        test("should allow multiple sync operations") {
            // given
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusDays(1)
            )

            // First sync
            metadata.markAsSynced()
            val firstSyncTime = metadata.lastSyncedAt
            metadata.needsResync() shouldBe false

            // Wait a moment (simulated by manually setting old time)
            metadata.lastSyncedAt = LocalDateTime.now().minusHours(25)
            metadata.needsResync() shouldBe true

            delay(10)

            // Second sync
            metadata.markAsSynced()
            val secondSyncTime = metadata.lastSyncedAt

            // then
            (secondSyncTime > firstSyncTime) shouldBe true
            metadata.needsResync() shouldBe false
        }
    }

    context("Entity identity") {
        test("should have null ID before persistence") {
            // given & when
            val metadata = DatabaseMetadata(
                schemaName = "public"
            )

            // then
            metadata.id shouldBe null
        }

        test("should use default equals (reference equality)") {
            // given
            val metadata1 = DatabaseMetadata(
                schemaName = "public"
            )
            val metadata2 = DatabaseMetadata(
                schemaName = "public"
            )

            // then - Different instances
            (metadata1 === metadata2) shouldBe false
            (metadata1 == metadata2) shouldBe false
        }

        test("should be equal to itself") {
            // given
            val metadata = DatabaseMetadata(
                schemaName = "public"
            )

            // then
            (metadata === metadata) shouldBe true
            (metadata == metadata) shouldBe true
        }
    }

    context("Business scenarios") {
        test("should track sync lifecycle for frequently updated database") {
            // given - New metadata
            val metadata = DatabaseMetadata(
                schemaName = "public"
            )

            // Scenario 1: Just synced, no resync needed
            metadata.needsResync(thresholdHours = 1) shouldBe false

            // Scenario 2: 2 hours pass (simulated)
            metadata.lastSyncedAt = LocalDateTime.now().minusHours(2)
            metadata.needsResync(thresholdHours = 1) shouldBe true

            // Scenario 3: Perform sync
            metadata.markAsSynced()
            metadata.needsResync(thresholdHours = 1) shouldBe false
        }

        test("should handle different schema names") {
            // MySQL
            val mysqlMetadata = DatabaseMetadata(
                schemaName = "production_db"
            )

            // PostgreSQL public schema
            val pgPublicMetadata = DatabaseMetadata(
                schemaName = "public"
            )

            // PostgreSQL custom schema
            val pgCustomMetadata = DatabaseMetadata(
                schemaName = "analytics"
            )

            // All valid
            mysqlMetadata.schemaName shouldBe "production_db"
            pgPublicMetadata.schemaName shouldBe "public"
            pgCustomMetadata.schemaName shouldBe "analytics"
        }

        test("should handle different resync policies") {
            // given
            val metadata = DatabaseMetadata(
                schemaName = "public",
                lastSyncedAt = LocalDateTime.now().minusHours(12)
            )

            // Production: strict 6-hour policy
            metadata.needsResync(thresholdHours = 6) shouldBe true

            // Development: relaxed 24-hour policy
            metadata.needsResync(thresholdHours = 24) shouldBe false

            // Real-time: aggressive 1-hour policy
            metadata.needsResync(thresholdHours = 1) shouldBe true
        }

        test("should support manual sync trigger regardless of threshold") {
            // given - Recently synced
            val metadata = DatabaseMetadata(
                schemaName = "public"
            )
            val initialSyncTime = metadata.lastSyncedAt

            // Even though no resync needed, admin triggers manual sync
            metadata.needsResync() shouldBe false

            // Manual sync
            Thread.sleep(10) // Small delay to ensure time difference
            metadata.markAsSynced()

            // Verify sync occurred
            (metadata.lastSyncedAt > initialSyncTime) shouldBe true
        }
    }

})