-- Migration: V3__add_performance_indexes.sql
-- Description: Add composite and sorting indexes for user contributions and saved issues

-- 1. Composite index for filtering user contributions by status (PLANNING, IN_PROGRESS, PR_OPENED, COMPLETED)
CREATE INDEX IF NOT EXISTS idx_contributions_user_status ON contributions (user_id, status);

-- 2. Composite index for sorting user contributions by recent activity
CREATE INDEX IF NOT EXISTS idx_contributions_user_updated ON contributions (user_id, updated_at DESC);

-- 3. Composite index for listing saved issues in chronological crate order
CREATE INDEX IF NOT EXISTS idx_saved_issues_user_saved_at ON saved_issues (user_id, saved_at DESC);
