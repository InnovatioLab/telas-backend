ALTER TABLE scheduler_job_runs
    ADD COLUMN IF NOT EXISTS result_summary JSONB NULL;
