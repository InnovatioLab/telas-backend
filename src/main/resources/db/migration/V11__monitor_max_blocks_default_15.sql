UPDATE "monitors"
SET "max_blocks" = 15
WHERE "max_blocks" > 15;

ALTER TABLE "monitors"
    ALTER COLUMN "max_blocks" SET DEFAULT 15;
